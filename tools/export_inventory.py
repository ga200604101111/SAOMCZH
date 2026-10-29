#!/usr/bin/env python3
"""Inventory all *available, explicitly sourced* game text. Local only; no upload or game mutation."""
from __future__ import annotations
import argparse,collections,datetime,hashlib,json,os,re,shutil,tempfile,unicodedata,zipfile
from pathlib import Path
SECTIONS=('names','items','lines','messages')
ORIGINS={'equipment_stat','skill_name','skill_desc','skill_stat','skill_lore','mob_name','bossbar_title','scoreboard_title','actionbar_system','item_name','item_lore','quest_title','quest_description','quest_objective','codex_name','codex_description','map_label','ui_label'}
OBSERVATIONS={'missing_zh_tw.jsonl':ORIGINS,'item_seen.jsonl':{'SAOUI.ItemLangClient'},'ui_seen.jsonl':{'saojob.MapNode'},'daily_seen.jsonl':{'SAOUI.DailyModel.Reward.desc'}}
FORMAT=re.compile(r'(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])')
CJK=re.compile(r'[\u3400-\u9fff]');LATIN=re.compile(r'[A-Za-zÀ-ÿ]{2,}')
PRIVATE=re.compile(r'(?:[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}|\b(?:\d{1,3}\.){3}\d{1,3}\b|\b[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\b|https?://|(?:[A-Za-z]:\\|/Users/|/home/))',re.I)
CHAT=re.compile(r'^\s*(?:<[^>]{1,32}>|\[[^]]{1,32}\]\s*[A-Za-z0-9_]{3,16}\s*[:：»›])')
TOKEN=re.compile(r'%(?:[0-9]+\$)?[-+#0 ,(]*[0-9]*(?:\.[0-9]+)?[a-zA-Z]|%%|\{[^{}\r\n]{1,80}\}')
MAX_FILE=64*1024*1024;MAX_ENTRIES=150000;MAX_TEXT=65536

def normalize(s:str)->str:
 s=s.replace('\ue900','並').replace('\ue901','中').replace('\ue902','串')
 return re.sub(r'\s+',' ',unicodedata.normalize('NFC',FORMAT.sub('',s))).strip()
def tokens(s):return sorted(TOKEN.findall(s))
def stable(s):return hashlib.sha256(s.encode('utf8')).hexdigest()[:24]
def unique(pairs):
 result={}
 for k,v in pairs:
  if k in result:raise ValueError('Duplicate JSON key')
  result[k]=v
 return result
def loads(raw):return json.loads(raw.decode('utf-8-sig') if isinstance(raw,bytes) else raw,object_pairs_hook=unique)
def load(path,limit=MAX_FILE):
 if path.is_symlink() or path.stat().st_size>limit:raise ValueError('Unsafe/oversized input')
 return loads(path.read_bytes())
def acceptable(text):return isinstance(text,str) and 0<len(text)<=MAX_TEXT and '\x00' not in text and not PRIVATE.search(text) and not CHAT.match(text) and any(c.isalpha() for c in text)
def group_key(text):
 # Grouping is review-only. No replacement is installed into the game.
 return re.sub(r'(?<![A-Za-z_])[-+]?\d+(?:[.,]\d+)*', '<number>',text)

class Builder:
 def __init__(self,root):
  self.root=root;self.rows={};self.maps={s:{} for s in SECTIONS};self.sources=[];self.warnings=[];self.filtered=collections.Counter();self.static_maps={};self.bytes_read=0
 def warning(self,source,error):
  if len(self.warnings)<100:self.warnings.append({'source':str(source),'reason':str(error)[:200]})
 def add(self,section,original,server=None,source='',origin='',historical=False,translation=None,static_key=None):
  if section not in SECTIONS:return
  text=server if isinstance(server,str) and server.strip() else original
  if not acceptable(text) or not acceptable(original):self.filtered['private-empty-or-oversized']+=1;return
  clean=normalize(text)
  if not clean:return
  k=(section,clean)
  if k not in self.rows:
   if len(self.rows)>=MAX_ENTRIES:raise ValueError('Inventory entry limit reached')
   self.rows[k]={'id':stable(section+'\0'+clean),'section':section,'text':clean,'source_keys':set(),'variants':set(),'sources':set(),'origins':set(),'historical_only':True,'observations':0,'static_translations':set(),'static_keys':set()}
  row=self.rows[k];row['historical_only'] &= historical;row['observations']+=1
  for target,value,cap in [('source_keys',original,32),('variants',text,32),('sources',source,32),('origins',origin,32)]:
   if len(row[target])<cap:row[target].add(value)
  if translation and isinstance(translation,str) and normalize(translation)!=clean and CJK.search(normalize(translation)):row['static_translations'].add(normalize(translation))
  if static_key and len(row['static_keys'])<32:row['static_keys'].add(static_key)
 def dictionaries(self):
  base=self.root/'SAO_TW_Dynamic';docs=[]
  for path in [base/'zh_tw.json']:
   if path.is_file():
    try:docs.append(load(path,8*1024*1024))
    except Exception as e:self.warning('local dictionary',type(e).__name__)
  state=base/'github-cache/zh_tw.state.json'
  if state.is_file():
   try:
    doc=load(state,65536);digest=doc['sha256']
    if not re.fullmatch('[a-f0-9]{64}',digest):raise ValueError('Invalid digest')
    path=state.with_name('zh_tw.'+digest+'.json');raw=path.read_bytes()
    if len(raw)>8*1024*1024 or hashlib.sha256(raw).hexdigest()!=digest:raise ValueError('Cloud checksum mismatch')
    docs.append(loads(raw))
   except Exception as e:self.warning('cloud dictionary',type(e).__name__)
  path=base/'local-overrides.json'
  if path.is_file():
   try:docs.append(load(path,2*1024*1024))
   except Exception as e:self.warning('local overrides',type(e).__name__)
  exact={s:{} for s in SECTIONS}
  for doc in docs:
   for sec in SECTIONS:
    values=doc.get(sec,{})
    if isinstance(values,dict):exact[sec].update({k:v for k,v in values.items() if isinstance(k,str) and isinstance(v,str)})
  for sec,rows in exact.items():
   for key,value in rows.items():
    if normalize(value)!=normalize(key) and CJK.search(normalize(value)):self.maps[sec].setdefault(normalize(key),set()).add(normalize(value))
  folder=self.root/'SAO_ZH_Extensions/rules';pointer=folder/'active.txt'
  if pointer.is_file():
   try:
    digest=pointer.read_text('ascii').strip()
    if not re.fullmatch('[a-f0-9]{64}',digest):raise ValueError('Invalid active pointer')
    raw=(folder/(digest+'.json')).read_bytes()
    if len(raw)>2*1024*1024 or hashlib.sha256(raw).hexdigest()!=digest:raise ValueError('Active rules checksum mismatch')
    for hook in loads(raw).get('hooks',[]):
     sec=hook.get('section','names')
     if sec not in SECTIONS:continue
     for key,value in hook.get('exact',{}).items():
      if isinstance(key,str) and isinstance(value,str) and normalize(value)!=normalize(key) and CJK.search(normalize(value)):self.maps[sec].setdefault(normalize(key),set()).add(normalize(value))
   except Exception as e:self.warning('active module mappings',type(e).__name__)
 def tables(self):
  directory=self.root/'SAO_TW_Dynamic/raw';tables=[]
  paths=sorted(directory.glob('*.json'));total=0
  if len(paths)>128:self.warning('raw snapshots','128-file limit; additional snapshots not scanned')
  for path in paths[:128]:
   try:
    total+=path.stat().st_size
    if total>128*1024*1024:raise ValueError('Combined raw snapshot byte budget exceeded')
    doc=load(path,20*1024*1024)
    if not isinstance(doc,dict) or doc.get('origin')!='SAOUI.LangTable' or not isinstance(doc.get('sections'),dict):raise ValueError('Unsupported raw source')
    tables.append((path.name,doc))
   except Exception as e:self.warning('raw/'+path.name,type(e).__name__)
  latest={}
  for name,d in tables:
   lang=d.get('served_language','unknown');stamp=d.get('captured_at','');latest[lang]=max(stamp,latest.get(lang,''))
  for name,d in tables:
   n=0;historical=d.get('captured_at','')!=latest[d.get('served_language','unknown')]
   for sec,rows in d['sections'].items():
    if sec not in SECTIONS or not isinstance(rows,dict):continue
    for original,value in rows.items():
     if isinstance(original,str) and isinstance(value,str):self.add(sec,original,value,'raw/'+name,'SAOUI.LangTable',historical);n+=1
   self.sources.append({'source':'raw/'+name,'kind':'received-table','records':n,'historical':historical})
 def observations(self):
  directory=self.root/'SAO_TW_Dynamic/review'
  for name,allowed in OBSERVATIONS.items():
   p=directory/name
   if not p.is_file():self.sources.append({'source':'review/'+name,'kind':'observation','status':'not-present','records':0});continue
   if p.is_symlink() or p.stat().st_size>MAX_FILE:self.warning(name,'Unsafe/oversized observation file');continue
   n=0;bad=0
   with p.open('r',encoding='utf-8-sig') as f:
    for i,line in enumerate(f):
     if i>=150000:self.warning(name,'Observation line budget reached');break
     if len(line)>512*1024:bad+=1;continue
     try:d=loads(line)
     except (ValueError,TypeError):bad+=1;continue
     if not isinstance(d,dict) or d.get('origin') not in allowed:self.filtered['unapproved-origin']+=1;continue
     self.add(d.get('section'),d.get('source_key'),d.get('server_text'), 'review/'+name,d.get('origin'));n+=1
   self.sources.append({'source':'review/'+name,'kind':'observation','records':n})
   if bad:self.warning(name,'Skipped '+str(bad)+' malformed/incomplete append rows')
 def static_assets(self,game):
  if game is None:return
  game=Path(game);archives=[];folders=[];options=game/'options.txt';enabled=[]
  if options.is_file():
   try:
    row=next((s for s in options.read_text('utf8').splitlines() if s.startswith('resourcePacks:')),'resourcePacks:[]');enabled=loads(row.split(':',1)[1])
   except Exception as e:self.warning('enabled packs',type(e).__name__)
  archives.extend((p,'mod:'+p.name) for p in sorted((game/'mods').glob('*.jar')) if p.is_file() and not p.is_symlink())
  for item in enabled if isinstance(enabled,list) else []:
   if not isinstance(item,str) or not item.startswith('file/'):continue
   rel=item[5:]
   if not rel or '..' in Path(rel).parts or Path(rel).is_absolute() or '\\' in rel:continue
   p=game/'resourcepacks'/rel
   if p.is_symlink():continue
   if p.is_file() and p.suffix.lower()=='.zip':archives.append((p,'pack:'+p.name))
   elif p.is_dir():folders.append((p,'pack:'+p.name))
  marker=self.root/'SAO_ZH_Cache/active.json'
  if marker.is_file():
   try:
    active=load(marker,65536)
    if active.get('owner')!='SAO_ZH_Cache':raise ValueError('Unknown cache owner')
    for kind,digest in active.get('packs',{}).items():
     if kind in ('main','support') and isinstance(digest,str) and re.fullmatch('[a-f0-9]{64}',digest):
      p=self.root/'SAO_ZH_Cache/packs'/(kind+'-'+digest+'.zip')
      if p.is_file() and not p.is_symlink():archives.append((p,'active-pack:'+kind))
   except Exception as e:self.warning('active resource pack marker',type(e).__name__)
  languages={};asset_counts=collections.Counter();seen=set();files=0
  pattern=re.compile(r'^assets/([a-z0-9_.-]+)/lang/(en_us|fr_fr|zh_tw)\.json$')
  def consume(name,raw,source):
   nonlocal files
   match=pattern.fullmatch(name)
   if not match:return
   if len(raw)>8*1024*1024:raise ValueError('Language asset exceeds limit')
   files+=1;self.bytes_read+=len(raw)
   if self.bytes_read>128*1024*1024:raise ValueError('Combined language asset byte budget exceeded')
   if files>4096:raise ValueError('Language asset count limit reached')
   doc=loads(raw)
   if not isinstance(doc,dict):raise ValueError('Unexpected language object')
   ns,locale=match.groups();values=languages.setdefault((ns,locale),{})
   for key,value in doc.items():
    if isinstance(key,str) and isinstance(value,str) and len(key)<4096:values[key]=(value,source);asset_counts[source]+=1
  if len(archives)>512:self.warning('local archives','512-archive limit; additional archives not scanned')
  for p,source in archives[:512]:
   if str(p) in seen:continue
   seen.add(str(p))
   try:
    with zipfile.ZipFile(p) as z:
     for info in z.infolist():
      if pattern.fullmatch(info.filename):
       if info.file_size>8*1024*1024:raise ValueError('Inflated language asset exceeds limit')
       consume(info.filename,z.read(info),source)
   except (OSError,ValueError,zipfile.BadZipFile) as e:self.warning(source,type(e).__name__)
  for p,source in folders:
   for f in p.glob('assets/*/lang/*.json'):
    rel=f.relative_to(p).as_posix()
    if not pattern.fullmatch(rel) or f.is_symlink():continue
    try:
     if f.stat().st_size>8*1024*1024:raise ValueError('Oversized language asset')
     consume(rel,f.read_bytes(),source)
    except Exception as e:self.warning(source,type(e).__name__)
  for (ns,locale),values in languages.items():
   if locale=='zh_tw':continue
   zh=languages.get((ns,'zh_tw'),{})
   for key,(text,source) in values.items():self.add('lines',text,text,source,'language-asset',translation=zh.get(key,(None,None))[0],static_key=ns+':'+key)
  for source,count in sorted(asset_counts.items()):self.sources.append({'source':source,'kind':'language-assets','records':count})
 def finish(self):
  entries=[];groups={};statuses=collections.Counter();origins=collections.Counter()
  for row in sorted(self.rows.values(),key=lambda x:(x['section'],x['text'])):
   candidates=set(row['static_translations']);aliases={row['text']}|{normalize(x) for x in row['source_keys']}|{normalize(x) for x in row['variants']}
   sections=('items','names','lines') if row['section']=='items' else (row['section'],)
   for sec in sections:
    for alias in aliases:candidates.update(self.maps[sec].get(alias,set()))
   if len(candidates)>1:status='conflicting-mappings'
   elif candidates:status='mapped-needs-display-check'
   elif CJK.search(row['text']):status='partial' if LATIN.search(row['text']) else 'already-chinese'
   else:status='missing'
   record={k:sorted(v) if isinstance(v,set) else v for k,v in row.items() if k!='static_translations'}
   record.update(status=status,translations=sorted(candidates),placeholders=tokens(row['text']),review_group=stable(row['section']+'\0'+group_key(row['text'])))
   entries.append(record);statuses[status]+=1;origins.update(record['origins'])
   if status in ('missing','partial','conflicting-mappings'):
    key=record['review_group'];g=groups.setdefault(key,{'id':key,'section':row['section'],'template_for_review_only':group_key(row['text']),'count':0,'examples':[],'statuses':set()});g['count']+=1;g['statuses'].add(status)
    if len(g['examples'])<5:g['examples'].append({'id':row['id'],'text':row['text']})
  queue=[]
  for g in sorted(groups.values(),key=lambda x:(x['section'],x['template_for_review_only'])):g['statuses']=sorted(g['statuses']);queue.append(g)
  summary={'schema_version':1,'locale':'zh_tw','scope':'received-tables-explicit-observations-local-language-assets','entry_count':len(entries),'statuses':dict(statuses),'origin_counts':dict(origins),'review_group_count':len(queue),'historical_only':sum(x['historical_only'] for x in entries),'sources':self.sources,'filtered':dict(self.filtered),'warnings':self.warnings,'coverage_limits':['No capture of server text not yet sent to this client.','No generic screen/heap/network/chat/whisper scan.','No OCR of pictures or texture text.','Mapped status is dictionary evidence, not proof the hook displayed Chinese.','Existing observation collectors cover only their reported origins; unseen runtime-only fields require an adapter.'],'privacy':'Local review inventory only. Not automatically uploaded. Custom item names can still require manual review before sharing.'}
  return {'summary':summary,'entries':entries,'groups':queue}

def collect(root:Path,game:Path|None=None)->dict:
 b=Builder(Path(root));b.dictionaries();b.tables();b.observations();b.static_assets(game);return b.finish()
def export(root:Path,game:Path|None=None)->Path:
 root=Path(root);report=collect(root,game);raw=json.dumps(report,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf8');digest=hashlib.sha256(raw).hexdigest()
 folder=root/'SAO_TW_Dynamic/inventory';folder.mkdir(parents=True,exist_ok=True);dest=folder/('inventory-'+digest[:20])
 if not dest.exists():
  temp=Path(tempfile.mkdtemp(prefix='.inventory-',dir=folder))
  try:
   def jsonl(name,rows):
    with (temp/name).open('w',encoding='utf8',newline='\n') as out:
     for row in rows:out.write(json.dumps(row,ensure_ascii=False,separators=(',',':'))+'\n')
   jsonl('all_text.jsonl',report['entries']);jsonl('missing_text.jsonl',[r for r in report['entries'] if r['status'] in ('missing','partial')]);jsonl('display_check.jsonl',[r for r in report['entries'] if r['status'] in ('mapped-needs-display-check','conflicting-mappings')]);jsonl('review_groups.jsonl',report['groups'])
   summary=dict(report['summary'],inventory_sha256=digest,generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat())
   (temp/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n','utf8')
   os.replace(temp,dest)
  finally:
   if temp.exists():shutil.rmtree(temp)
 pointer=folder/'current.json';pending=folder/('.current-'+str(os.getpid())+'.pending');pending.write_text(json.dumps({'schema_version':1,'directory':dest.name,'inventory_sha256':digest},indent=2)+'\n','utf8');os.replace(pending,pointer)
 return dest

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--root',type=Path,default=Path('D:/翻譯'));p.add_argument('--game',type=Path);p.add_argument('--no-static',action='store_true');a=p.parse_args()
 game=None if a.no_static else a.game or Path.home()/'AppData/Roaming/.Triggers Client/instances/Sword Art Online'
 try:
  target=export(a.root,game if game and game.is_dir() else None);summary=load(target/'summary.json');print('ALL_AVAILABLE_TEXT_INVENTORY_READY',json.dumps({'path':str(target),'entries':summary['entry_count'],'statuses':summary['statuses'],'review_groups':summary['review_group_count'],'warnings':len(summary['warnings']),'uploaded':False},ensure_ascii=False));return 0
 except (OSError,ValueError) as e:print('TEXT_INVENTORY_FAILED_KEEP_PREVIOUS',str(e));return 1
if __name__=='__main__':raise SystemExit(main())
