#!/usr/bin/env python3
"""Build a corpus-wide CJK completion + literal/icon isolation pack, offline only.
Original icon providers, Latin glyphs, existing textures, and live packs are immutable.
Requires the owner's existing OFL font sources and generated MSDF master atlases.
"""
from pathlib import Path
from PIL import Image
from fontTools.ttLib import TTFont
from fontTools import subset
import argparse,collections,copy,hashlib,io,json,math,os,subprocess,zipfile

LEGACY={ord('並'):0xe900,ord('中'):0xe901,ord('串'):0xe902}
MANIFEST='assets/saotw/glyph-routing.json'

def han(cp):return 0x3400<=cp<=0x4dbf or 0x4e00<=cp<=0x9fff or 0xf900<=cp<=0xfaff or 0x20000<=cp<=0x323af

def dump(v):return (json.dumps(v,ensure_ascii=False,separators=(',',':'))+'\n').encode('utf8')
def sha(data):return hashlib.sha256(data).hexdigest()
def strings(v):
 if isinstance(v,str):yield v
 elif isinstance(v,dict):
  for x in v.values():yield from strings(x)
 elif isinstance(v,list):
  for x in v:yield from strings(x)
from display_typography import normalize_fields
def encode(s,aliases):return ''.join(chr(aliases.get(ord(c),ord(c))) for c in normalize_fields(s.replace('\uff1a',':')))
# Exact UI keys verified against the original SAOUI/companionsmod en_us resources.
# Do not turn Chinese sentence punctuation, numeric values or arbitrary strings into ASCII.
LEVEL_LABELS={'saoui.node.level':'Lv. %s+','saoui.mob.level':'LV. %s','companionsmod.ui.level':'Lv. %s'}
# English compact time units are smaller and belong to native numeric UI slots.
# Deliberately *not* a blanket replacement of Chinese story/prose time expressions.
_COMPACT_UNITS={
 'assets/triggersmod/lang':{
  'saoui.equipment.playtime':'Playtime:',
  'friends.ago.minutes':'%s min',
  'friends.ago.hours':'%s h',
  'bundle.timer_text':'剩餘時間: {days}d {hours}h {minutes}m',
 },
 'assets/saoui/lang':{
  'saoui.equipment.duration.hours':'%sh %s',
  'saoui.equipment.duration.minutes':'%s min',
 },
}
def compact_time_locale(filename, data):
 if not isinstance(data,dict) or not filename.endswith(('/zh_tw.json','/zh_hk.json')):
  return data
 root=filename.rsplit('/',1)[0]
 override=_COMPACT_UNITS.get(root)
 if override is None:return data
 result=dict(data)
 for key,value in override.items():
  if key in result:result[key]=value
 return result

def level_label(key,value):
 target=LEVEL_LABELS.get(key)
 if target is not None and isinstance(value,str):
  wrong={s.replace('.',mark) for s in (target,target.replace('Lv.','LV.')) for mark in ('\u3002','\uff0e')}
  if value in wrong:return target
 return value

def encode_values(v,aliases):
 if isinstance(v,str):return encode(v,aliases)
 if isinstance(v,dict):return {k:encode_values(level_label(k,x),aliases) for k,x in v.items()}
 if isinstance(v,list):return [encode_values(x,aliases) for x in v]
 return v

def scan(archives,repo):
 occupied=set();conflicts=collections.defaultdict(set);values=[]
 for path in archives:
  with zipfile.ZipFile(path) as z:
   for n in z.namelist():
    if not n.startswith('assets/') or not n.endswith('.json'):continue
    try:d=json.loads(z.read(n))
    except (ValueError,UnicodeError):continue
    for s in strings(d):occupied.update(ord(c) for c in s if 0xe000<=ord(c)<=0xf8ff)
    if isinstance(d,dict):
     for g in d.get('glyphs',[]):
      if isinstance(g,dict) and isinstance(g.get('unicode'),int) and 0xe000<=g['unicode']<=0xf8ff:occupied.add(g['unicode'])
     for p in d.get('providers',[]):
      if p.get('type') not in ('bitmap','minecraft:bitmap'):continue
      for row in p.get('chars',[]):
       for c in row:
        if han(ord(c)):conflicts[ord(c)].add(p.get('file',''))
    if n.endswith(('/zh_tw.json','/zh_hk.json')) and '/lang/' in n:values.extend(strings(d))
 table=json.loads((repo/'languages/zh_tw/translations.json').read_text('utf8'))
 for sec in ('names','items','lines','messages'):values.extend(table[sec].values())
 rules=json.loads((repo/'languages/zh_tw/hook_text_rules.json').read_text('utf8'))
 for spec in list(rules.get('rule_sets',{}).values())+list(rules.get('hooks',{}).values()):
  for key in ('exact','padded_exact'):values.extend(spec.get(key,{}).values())
  values.extend(op.get('to','') for op in spec.get('operations',[]))
 return occupied,conflicts,{ord(c) for s in values for c in s if han(ord(c))}

def build_alias_font(source,dest,aliases,weight):
 font=TTFont(source);cmap=font.getBestCmap();missing=set(aliases)-set(cmap)
 if missing:raise ValueError('Unrenderable source characters: '+str(sorted(missing)))
 outlines={dst:cmap[src] for src,dst in aliases.items()}
 opt=subset.Options();opt.recalc_timestamp=False;opt.name_IDs=['*'];opt.name_legacy=True;opt.name_languages=['*']
 sub=subset.Subsetter(options=opt);sub.populate(unicodes=list(aliases));sub.subset(font)
 for table in font['cmap'].tables:
  if table.isUnicode():table.cmap=dict(outlines)
 for record in font['name'].names:
  if record.nameID in (1,4,6,16):
   name='SAO Literal CJK '+weight.title() if record.nameID!=6 else 'SAOLiteralCJK-'+weight.title()
   record.string=name.encode(record.getEncoding(),errors='replace')
 font.save(dest);font.close()
 with TTFont(dest) as check:assert set(check.getBestCmap())==set(aliases.values())

def font_weight(name):return 'bold' if any(x in name for x in ('bold','semibold','extrabold')) else 'regular'
def texture_for(name):
 stem=name.rsplit('/',1)[1][:-5]
 return ('assets/saoui/textures/font/msdf/' if name.startswith('assets/saoui/') else 'assets/saojob/textures/font/msdf/')+stem+'.png'

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for key in ('repo','main-pack','support-pack','font-root','master-root','generator','out'):p.add_argument('--'+key,required=True,type=Path)
 p.add_argument('--scan-jar',action='append',default=[],type=Path)
 args=p.parse_args();out=args.out;out.mkdir(parents=True,exist_ok=True)
 if (out/'main-glyph-contract.zip').exists() or (out/'support-glyph-contract.zip').exists():raise SystemExit('Refusing to overwrite an existing candidate')
 # Always rebuild from immutable original assets, never stack alias generations.
 # This keeps existing literal codepoints stable across future dictionary updates.
 with zipfile.ZipFile(args.main_pack) as baseline:
  if MANIFEST in baseline.namelist():raise ValueError('Use the original pre-routing baseline pack, not an already patched candidate')
 archives=[args.main_pack,args.support_pack]+args.scan_jar
 for f in archives+[args.generator]:
  if not f.is_file():raise FileNotFoundError(f)
 occupied,conflicts,required=scan(archives,args.repo)
 fontpaths={w:args.font_root/f'SAO-Free-CJK-FULL-{w}.ttf' for w in ('regular','bold')}
 with TTFont(fontpaths['regular'],lazy=True) as font:font_cmap=set(font.getBestCmap())
 unsupported=sorted(set(conflicts)-font_cmap)
 if set(unsupported)&required:raise ValueError('A required translated Han glyph has no licensed font source')
 aliases={k:v for k,v in LEGACY.items() if k in conflicts};free=(cp for cp in range(0xe910,0xf900) if cp not in occupied)
 for src in sorted(set(conflicts)-set(aliases)-set(unsupported)):aliases[src]=next(free)
 aliases=dict(sorted(aliases.items()));alias_reverse={v:k for k,v in aliases.items()}
 print('GLYPH_CONTRACT_PLAN',json.dumps({'conflicts':len(conflicts),'safe_aliases':len(aliases),'unsupported_original_icons':len(unsupported),'corpus_han':len(required)},ensure_ascii=False),flush=True)
 generated={};masters={}
 for weight in ('regular','bold'):
  folder=out/weight;folder.mkdir(exist_ok=True)
  fontfile=folder/'literal.ttf';build_alias_font(fontpaths[weight],fontfile,aliases,weight)
  generated[f'assets/saotw/font/literal-alias-{weight}.ttf']=fontfile.read_bytes()
  masterdir=args.master_root/('full_'+weight)
  m=json.loads((masterdir/'atlas.json').read_text('utf8'))
  needed=({aliases.get(cp,cp) for cp in required}-{g['unicode'] for g in m['glyphs']})|set(aliases.values())
  # A dictionary may contain characters absent from older master atlases. Generate
  # these from the full licensed source, rather than silently keeping old coverage.
  fullfont=TTFont(fontpaths[weight]);cmap=fullfont.getBestCmap()
  if (needed-set(aliases.values()))-set(cmap):raise ValueError('Full licensed font lacks required characters')
  for table in fullfont['cmap'].tables:
   if table.isUnicode():
    for src,dst in aliases.items():table.cmap[dst]=cmap[src]
  generatorfont=folder/'generator-source.ttf';fullfont.save(generatorfont);fullfont.close()
  chars=folder/'charset.txt';chars.write_text(' '.join(f'0x{cp:X}' for cp in sorted(needed)),'ascii')
  cmd=[args.generator,'-font',generatorfont,'-charset',chars,'-type','msdf','-format','png','-imageout',folder/'atlas.png','-json',folder/'atlas.json','-size','48','-pxrange','16','-yorigin','top','-dimensions','1024','1024','-threads','2','-nokerning']
  run=subprocess.run(list(map(str,cmd)),capture_output=True,timeout=180)
  (folder/'generator.log').write_bytes(run.stdout+run.stderr)
  if run.returncode:raise RuntimeError((run.stdout+run.stderr).decode('utf8','replace'))
  a=json.loads((folder/'atlas.json').read_text('utf8'));assert {g['unicode'] for g in a['glyphs']}==needed
  masters[weight]=({'glyphs':{g['unicode']:g for g in m['glyphs']},'image':Image.open(masterdir/'atlas.png').convert('RGB')},{'glyphs':{g['unicode']:g for g in a['glyphs']},'image':Image.open(folder/'atlas.png').convert('RGB')})
  print('ALIAS_FONT_AND_MSDF_READY',weight,len(aliases),flush=True)
 required_display={aliases.get(cp,cp) for cp in required}|set(aliases.values())|set(LEGACY.values())
 atlas_report=[];locale_changed={};original_crcs={}
 with zipfile.ZipFile(args.main_pack) as z:
  for name in z.namelist():
   if not name.endswith('.json') or '/msdf/' not in name:continue
   doc=json.loads(z.read(name));by={g['unicode']:g for g in doc['glyphs']};missing=sorted(required_display-set(by))
   image_name=texture_for(name);weight=font_weight(name);full,extra=masters[weight]
   with Image.open(io.BytesIO(z.read(image_name))) as image:base=image.convert('RGB')
   cell=80;columns=base.width//cell;rows=math.ceil(len(missing)/columns)
   height=base.height+(rows*cell+8 if missing else 0)
   if max(base.width,height)>8192:raise ValueError('Atlas texture budget exceeded: '+name)
   canvas=Image.new('RGB',(base.width,height),(0,0,0));canvas.paste(base,(0,0))
   for i,cp in enumerate(missing):
    source=extra if cp in extra['glyphs'] else full
    if cp not in source['glyphs']:raise ValueError(f'Master lacks corpus glyph U+{cp:X} in {name}; regenerate master from licensed full font')
    glyph=copy.deepcopy(source['glyphs'][cp]);bounds=glyph.get('atlasBounds')
    if bounds:
     x0=math.floor(bounds['left']);y0=math.floor(bounds['top']);x1=math.ceil(bounds['right']);y1=math.ceil(bounds['bottom'])
     if x1-x0>cell-4 or y1-y0>cell-4:raise ValueError('Glyph tile exceeds cell budget')
     x=(i%columns)*cell+2;y=base.height+4+(i//columns)*cell+2
     tile=source['image'].crop((x0,y0,x1,y1));canvas.paste(tile,(x,y));tile.close()
     for key,delta in [('left',x-x0),('right',x-x0),('top',y-y0),('bottom',y-y0)]:bounds[key]+=delta
    plane=glyph.get('planeBounds')
    if plane and plane['top']<plane['bottom']:plane['top']=-plane['top'];plane['bottom']=-plane['bottom']
    doc['glyphs'].append(glyph)
   assert canvas.crop((0,0,base.width,base.height)).tobytes()==base.tobytes()
   doc['atlas']['height']=height
   png=io.BytesIO();canvas.save(png,format='PNG',compress_level=3)
   generated[image_name]=png.getvalue();generated[name]=dump(doc)
   atlas_report.append({'font':name,'added':len(missing),'width':base.width,'height':height,'weight':weight})
   base.close();canvas.close()
   print('CORPUS_ATLAS_READY',len(atlas_report),name,'added',len(missing),'height',height,flush=True)
  if len(atlas_report)!=26:raise ValueError('Unexpected atlas matrix')
  for n in z.namelist():
   if n.endswith(('/zh_tw.json','/zh_hk.json')) and '/lang/' in n:
    old=json.loads(z.read(n));new=encode_values(compact_time_locale(n,old),aliases)
    if new!=old:generated[n]=dump(new);locale_changed[n]=sum(a!=b for a,b in zip(strings(old),strings(new)))
  # Add exact, alias-only TTF fallbacks to every font family. No ordinary Han can be
  # captured by this provider, so original server bitmap icons retain their identity.
  font_json={n:json.loads(z.read(n)) for n in z.namelist() if n.endswith('.json') and '/font/' in n and '/msdf/' not in n}
 with zipfile.ZipFile(args.support_pack) as support:
  support_fonts={n:json.loads(support.read(n)) for n in support.namelist() if n.endswith('.json') and '/font/' in n and '/msdf/' not in n}
 def patched_font(name,doc):
  if not isinstance(doc,dict) or 'providers' not in doc:return doc
  doc=copy.deepcopy(doc);providers=doc['providers'];first=next((x for x in providers if x.get('type') in ('ttf','minecraft:ttf')),None)
  weight=font_weight(name+' '+str(first.get('file','') if first else ''))
  size=first.get('size',11) if first else 11;shift=first.get('shift',[0,0]) if first else [0,0]
  spec={'type':'ttf','file':f'saotw:literal-alias-{weight}.ttf','size':size,'shift':shift,'oversample':4.0}
  key=sha(dump(spec))[:24];ref='saotw:literal_alias/'+key
  generated['assets/saotw/font/literal_alias/'+key+'.json']=dump({'providers':[spec]})
  providers.insert(0,{'type':'reference','id':ref})
  if name in ('assets/minecraft/font/default.json','assets/minecraft/font/uniform.json'):
   # Keep all bitmap icons ahead of the complete CJK fallback, but ahead of Unifont.
   safe={'type':'ttf','file':'minecraft:sao_cjk_regular.ttf','size':11,'shift':[0,0],'oversample':4.0,'skip':''.join(chr(c) for c in sorted(conflicts))}
   ckey=sha(dump(safe))[:24];generated['assets/saotw/font/literal_cjk/'+ckey+'.json']=dump({'providers':[safe]})
   index=next((i for i,x in enumerate(providers) if x.get('id')=='minecraft:include/unifont'),len(providers))
   providers.insert(index,{'type':'reference','id':'saotw:literal_cjk/'+ckey})
  return doc
 for n,d in font_json.items():generated[n]=dump(patched_font(n,d))
 support_updates={n:dump(patched_font(n,d)) for n,d in support_fonts.items()}
 assets_hashes={n:sha(b) for n,b in generated.items() if '/lang/' not in n}
 manifest={'schema_version':1,'locale':'zh_tw','aliases':{str(k):v for k,v in aliases.items()},'unsupported_icon_codepoints':unsupported,'corpus_codepoints':sorted(required),'atlas_count':len(atlas_report),'assets_sha256':assets_hashes,'notes':'Generated from every current translation and localized resource; original bitmap icons unchanged. Enable only with matching compatibility/runtime.'}
 generated[MANIFEST]=dump(manifest)
 def archive(source,destination,updates):
  temporary=destination.with_suffix('.pending');addition=dict(updates)
  with zipfile.ZipFile(source) as src,zipfile.ZipFile(temporary,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=3,allowZip64=True) as dst:
   for info in src.infolist():
    payload=addition.pop(info.filename,None)
    if info.filename=='pack.mcmeta':
     meta=json.loads(src.read(info.filename));meta['pack']['description']='SAO 繁中・全詞庫字形與圖示隔離候選';payload=dump(meta)
    dst.writestr(info,src.read(info.filename) if payload is None else payload,compress_type=info.compress_type,compresslevel=3)
   for n,b in addition.items():dst.writestr(n,b)
  with zipfile.ZipFile(temporary) as check:
   assert check.testzip() is None;assert len(check.namelist())==len(set(check.namelist()))
  os.replace(temporary,destination)
 main_out=out/'main-glyph-contract.zip';support_out=out/'support-glyph-contract.zip'
 archive(args.main_pack,main_out,generated);archive(args.support_pack,support_out,support_updates)
 for pair in masters.values():
  for item in pair:item['image'].close()
 report={'status':'CANDIDATE_NOT_INSTALLED','main':str(main_out),'support':str(support_out),'main_bytes':main_out.stat().st_size,'support_bytes':support_out.stat().st_size,'safe_collision_aliases':len(aliases),'detected_icon_collisions':len(conflicts),'unsupported_original_icons':unsupported,'corpus_han':len(required),'atlases':atlas_report,'localized_files':locale_changed,'source_pack':str(args.main_pack),'original_icon_providers_preserved':True,'game_files_changed':False}
 (out/'BUILD_REPORT.json').write_bytes(dump(report));(out/'glyph-routing.json').write_bytes(dump(manifest))
 print('SYSTEMATIC_GLYPH_BUILD_PASS',json.dumps({k:v for k,v in report.items() if k not in ('atlases','localized_files')},ensure_ascii=False),flush=True)

if __name__=='__main__':main()
