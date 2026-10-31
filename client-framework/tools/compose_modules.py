#!/usr/bin/env python3
"""Compose functional modules into a bounded signed-runtime-compatible hook bundle.
No game access or signing keys. Run ValidateRules against target JARs before signing.
"""
from __future__ import annotations
import argparse,copy,hashlib,json,os,re,tempfile,time
from pathlib import Path
from typing import Any

class ModuleError(ValueError):pass
ID=re.compile(r'^[a-z][a-z0-9.-]{0,39}$')
SEMVER=re.compile(r'^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)$')
CLASS=re.compile(r'^[A-Za-z_$][A-Za-z0-9_$/]*$')
METHOD=re.compile(r'^[A-Za-z_$][A-Za-z0-9_$]*$')
TOKEN=re.compile(r'%(?:[0-9]+\$)?[-+#0 ,(]*[0-9]*(?:\.[0-9]+)?[a-zA-Z]|%%|\{[^{}\r\n]{1,80}\}')
SECTIONS=('names','items','lines','messages')
PROTECTED=('java/','javax/','jdk/','sun/','sao/zh/','org/objectweb/','net/fabricmc/','org/spongepowered/')
HOOK_FIELDS={'id','class','method','descriptor','kind','argument','sites','member_class','member','member_descriptor','locale','section','exact','operations','measure','context_fields','cache_fields','capture'}
MAX_BYTES=2*1024*1024

def load_json(text:str)->Any:
 def unique(pairs):
  obj={}
  for k,v in pairs:
   if k in obj:raise ModuleError('Duplicate JSON key: '+k)
   obj[k]=v
  return obj
 try:return json.loads(text,object_pairs_hook=unique,parse_constant=lambda x:(_ for _ in ()).throw(ModuleError('Nonfinite JSON number')))
 except (ValueError,TypeError) as e:raise ModuleError(str(e)) from e

def version(v:Any)->tuple[int,int,int]:
 if not isinstance(v,str) or not SEMVER.fullmatch(v):raise ModuleError('Expected plain semantic version')
 parts=tuple(map(int,v.split('.')))
 if any(n>2147483647 for n in parts):raise ModuleError('Version component too large')
 return parts

def entries(obj:Any)->dict[str,str]:
 if not isinstance(obj,dict) or len(obj)>10000:raise ModuleError('Invalid dictionary size/type')
 out={}
 for a,b in obj.items():
  if not isinstance(a,str) or not isinstance(b,str) or not a or not b or len(a)>8192 or len(b)>16384 or '\x00' in a+b:raise ModuleError('Invalid translation text')
  if sorted(TOKEN.findall(a))!=sorted(TOKEN.findall(b)):raise ModuleError('Placeholder mismatch: '+repr(a[:80]))
  out[a]=b
 return out

def glossary(obj:Any)->dict[str,dict[str,str]]:
 if not isinstance(obj,dict) or set(obj)-set(SECTIONS):raise ModuleError('Unknown dictionary section')
 return {key:entries(value) for key,value in obj.items()}

def class_name(s:Any)->str:
 if not isinstance(s,str) or len(s)>240 or not CLASS.fullmatch(s) or '/' not in s or '//' in s or s.startswith(PROTECTED):raise ModuleError('Invalid/protected target class')
 return s

def checked_hook(h:Any,module_id:str,terms:dict[str,dict[str,str]])->dict[str,Any]:
 if not isinstance(h,dict) or set(h)-HOOK_FIELDS:raise ModuleError('Unsupported hook field')
 h=copy.deepcopy(h)
 id=h.get('id')
 if not isinstance(id,str) or not re.fullmatch('[A-Za-z0-9_.-]{1,55}',id):raise ModuleError('Invalid local hook id')
 h['id']=module_id+':'+id
 class_name(h.get('class'))
 if not isinstance(h.get('method'),str) or not METHOD.fullmatch(h['method']):raise ModuleError('Invalid/hot-unsafe method')
 desc=h.get('descriptor')
 if not isinstance(desc,str) or len(desc)>1024 or not desc.startswith('(') or ')' not in desc:raise ModuleError('Invalid method descriptor')
 kind=h.get('kind')
 if kind not in ('return','argument','field','call','wrap','refresh','armorstand_nameplate','mob_name','system_text','description_list','web_extract'):raise ModuleError('Unsupported hook kind')
 if module_id=='npc-dialogue' or h['class'].startswith('fr/zapdialogue/'):
  permitted={
   ('npc-dialogue','speaker','fr/zapdialogue/client/gui/BubbleRenderer','drawSpeakerName',
    '(Lnet/minecraft/class_332;Lfr/zapdialogue/client/gui/BubbleRenderer$Bubble;Lnet/minecraft/class_2561;Lnet/minecraft/class_327;)V',2,'names','npc_name'),
   ('npc-dialogue','text-wrap','fr/zapdialogue/client/text/StyledText','wrap',
    '(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;',0,'lines','dialogue'),
   ('npc-dialogue','text-trim','fr/zapdialogue/client/text/StyledText','trim',
    '(Lnet/minecraft/class_5348;I)Lnet/minecraft/class_5250;',0,'lines',None),
   ('npc-dialogue','text-length','fr/zapdialogue/client/text/StyledText','length',
    '(Lnet/minecraft/class_5348;)I',0,'lines',None),
  }
  candidate=(module_id,id,h['class'],h['method'],desc,h.get('argument'),h.get('section'),h.get('capture'))
  if candidate not in permitted or kind!='argument' or h.get('sites',1)!=1 or h.get('locale','zh_tw')!='zh_tw' or any(k in h for k in ('member_class','member','member_descriptor','measure','context_fields','cache_fields')):
   raise ModuleError('Zap Dialogue permits only audited display argument projections')
 if module_id=='named-container' or id=='visible-title' and h['class']=='net/minecraft/class_465':
  if (module_id,id,h['class'],h['method'],desc,kind,h.get('section'),h.get('sites',1),
      h.get('member_class'),h.get('member'),h.get('member_descriptor'))!=(
      'named-container','visible-title','net/minecraft/class_465','method_2388',
      '(Lnet/minecraft/class_332;II)V','field','names',1,
      'net/minecraft/class_465','field_22785','Lnet/minecraft/class_2561;'
     ) or h.get('locale','zh_tw')!='zh_tw' or h.get('capture') not in (None,'') or any(k in h for k in ('argument','measure','context_fields','cache_fields')):
   raise ModuleError('Container title hook must target the exact vanilla display-only field')
 if id=='game-actionbar-display' or h['class']=='net/minecraft/class_329' and h['method']=='method_1758':
  if (module_id,id,h['class'],h['method'],desc,kind,h.get('section'),h.get('argument'),h.get('sites',1))!=(
      'dynamic-translation','game-actionbar-display','net/minecraft/class_329',
      'method_1758','(Lnet/minecraft/class_2561;Z)V','argument','messages',0,1
     ) or h.get('locale','zh_tw')!='zh_tw' or h.get('capture') not in (None,'') or any(k in h for k in ('member','member_class','member_descriptor','measure','context_fields','cache_fields')):
   raise ModuleError('Only native HUD Actionbar Text argument is allowed')
 if module_id=='worlds-locations' and id=='vanilla-bossbar-title' or h['class']=='net/minecraft/class_337':
  if (module_id,id,h['class'],h['method'],desc,kind,h.get('section'),h.get('sites',1),
    h.get('member_class'),h.get('member'),h.get('member_descriptor'))!=(
    'worlds-locations','vanilla-bossbar-title','net/minecraft/class_337','method_1796',
    '(Lnet/minecraft/class_332;)V','call','names',1,
    'net/minecraft/class_345','method_5414','()Lnet/minecraft/class_2561;'
  ) or h.get('capture') not in (None,'') or h.get('locale','zh_tw')!='zh_tw' or any(k in h for k in ('argument','measure','context_fields','cache_fields')):
   raise ModuleError('BossBar may only translate the exact rendered name, not boss data')
 if kind=='web_extract':
  if (module_id,id,h['class'],h['method'],desc,h.get('section'))!=(
      'zapaudio-webui','extracted-html','fr/saofr/zapaudio/client/web/WebClientExtractor',
      'extract','()Ljava/nio/file/Path;','lines'
     ) or h.get('capture') not in (None,'') or h.get('sites',1)!=1 or any(k in h for k in ('argument','member_class','member','member_descriptor','context_fields','cache_fields','measure')):
   raise ModuleError('Web HTML extraction may only use the exact Sound of Aincrad return Path')
 if kind=='description_list':
  if (module_id,id,h['class'],h['method'],desc,h.get('section'))!=('items-economy','tooltip-description-paragraphs','fr/sao/tooltip/TipModel','description','()Ljava/util/List;','items') or h.get('capture') not in (None,'item'):raise ModuleError('Only the approved description list is supported')
 if kind in ('mob_name','system_text'):
  expected={'mob_name':('codex-creatures','mob-shown-name','fr/sao/saoui/mob/MobBarRenderer','shownName','(Lfr/sao/saoui/mob/MobBarEntry;)Ljava/lang/String;','names'),
            'system_text':('dynamic-translation','game-system-display','net/minecraft/class_7594','method_44736','(Lnet/minecraft/class_2561;Z)V','messages')}[kind]
  if (module_id,id,h['class'],h['method'],desc,h.get('section'))!=expected or (h.get('capture') and not (kind=='mob_name' and h.get('capture')=='mob')):
   raise ModuleError('Unsafe semantic rendering hook')
 if kind=='armorstand_nameplate':
  if module_id!='worlds-locations' or h['class']!='net/minecraft/class_877' or h['method']!='method_62375' or desc!='(Lnet/minecraft/class_1531;Lnet/minecraft/class_9998;F)V' or h.get('section')!='lines' or h.get('capture'):
   raise ModuleError('ArmorStand hook only permits client render-state label projection')
 if kind=='wrap':
  if desc!='(Ljava/lang/String;FZFI)Ljava/util/List;' or not isinstance(h.get('measure'),str) or not METHOD.fullmatch(h['measure']):raise ModuleError('Invalid bounded wrap contract')
  context=h.get('context_fields',{})
  if not isinstance(context,dict) or len(context)>4 or any(not isinstance(k,str) or not METHOD.fullmatch(k) or v not in ('Ljava/lang/String;','F','I','Z') for k,v in context.items()):raise ModuleError('Invalid layout context')
 if kind=='refresh':
  fields=h.get('cache_fields')
  display_cache=(h['class']=='net/minecraft/class_8113$class_8123'
                 and h['method']=='method_48905'
                 and desc=='(Lnet/minecraft/class_8113$class_8123$class_8127;)Lnet/minecraft/class_8113$class_8123$class_8125;')
  if not (desc.endswith(')V') or display_cache) or not isinstance(fields,list) or not 1<=len(fields)<=8:raise ModuleError('Invalid cache refresh contract')
  seen=set()
  for f in fields:
   if not isinstance(f,dict) or set(f)!={'name','descriptor','action'} or not isinstance(f['name'],str) or not METHOD.fullmatch(f['name']):raise ModuleError('Invalid cache field')
   safe=(f['descriptor'],f['action']) in (('Ljava/util/Map;','clear_map'),('Ljava/lang/String;','reset_key'))
   display_safe=(display_cache and f=={'name':'field_42442','descriptor':'Lnet/minecraft/class_8113$class_8123$class_8125;','action':'reset_display_lines'})
   if f['name'] in seen or not (safe or display_safe):raise ModuleError('Unsafe cache field/type')
   seen.add(f['name'])
  if display_cache and (len(fields)!=1 or not display_safe):raise ModuleError('World TextDisplay cache must be the sole refresh field')
 texttypes=('Ljava/lang/String;','Lnet/minecraft/class_2561;')
 if kind=='return' and desc.split(')',1)[1] not in texttypes:raise ModuleError('Only String/Text return projection is supported')
 if kind=='argument' and (type(h.get('argument')) is not int or not 0<=h['argument']<=254):raise ModuleError('Invalid argument index')
 if kind in ('field','call'):
  class_name(h.get('member_class'))
  if not isinstance(h.get('member'),str) or not METHOD.fullmatch(h['member']):raise ModuleError('Invalid member')
  target=h.get('member_descriptor')
  if not isinstance(target,str) or (target not in texttypes if kind=='field' else not any(target.endswith(')'+t) for t in texttypes)):raise ModuleError('Member is not String/Text')
 sites=h.get('sites',1)
 if type(sites) is not int or not 1<=sites<=32:raise ModuleError('Invalid match-site count')
 locale=h.get('locale','zh_tw')
 if not isinstance(locale,str) or not (locale=='*' or re.fullmatch('[a-z]{2,3}_[a-z]{2,4}',locale)):raise ModuleError('Invalid locale')
 h['locale']=locale;section=h.get('section','names')
 if section not in SECTIONS:raise ModuleError('Invalid section')
 h['section']=section
 capture=h.get('capture')
 if capture is not None:
  families={
   'mob':('codex-creatures',('fr/sao/saoui/mob/MobBarRenderer',)),
   'quest':('quests-objectives',('fr/sao/questtracker/screen/','fr/sao/questtracker/hud/')),
   'codex':('codex-creatures',('fr/sao/codex/',)),
   'skill':('skills-progression',('com/eighteen/saojob/skills/','com/eighteen/saojob/skilltree/')),
   'item':('items-economy',('fr/sao/iteminfo/','fr/sao/tooltip/','fr/sao/daily/','fr/sao/itemlang/')),
   'world':('worlds-locations',('fr/sao/teleport/','fr/sao/interlink/','fr/sao/saoui/mob/BossHud')),
   'system':('combat-feedback',('fr/sao/saoui/hud/actionbar/ActionBarTranslator',)),
   'dialogue':('npc-dialogue',('fr/zapdialogue/client/text/StyledText',)),
   'npc_name':('npc-dialogue',('fr/zapdialogue/client/gui/BubbleRenderer',))
  }
  if not isinstance(capture,str) or capture not in families:raise ModuleError('Unknown capture origin')
  module,owners=families[capture]
  if module_id!=module or (kind not in ('return','argument','call','field') and not (capture=='mob' and kind=='mob_name') and not (capture=='item' and kind=='description_list')) or not any(h['class'].startswith(o) for o in owners):
   raise ModuleError('Capture target not allowlisted for its domain')
 mapped=dict(terms.get(section,{}));mapped.update(entries(h.get('exact',{})));h['exact']=dict(sorted(mapped.items()))
 ops=h.get('operations',[])
 if not isinstance(ops,list) or len(ops)>8:raise ModuleError('Instruction budget exceeded')
 for op in ops:
  if not isinstance(op,dict) or set(op)-{'op','from','to'} or op.get('op') not in ('prefix','suffix','replace_literal'):raise ModuleError('Unsupported/unbounded instruction')
  if not isinstance(op.get('from'),str) or not 0<len(op['from'])<=256 or not isinstance(op.get('to',''),str) or len(op.get('to',''))>256:raise ModuleError('Invalid operation operand')
 return h

def compose(modules:list[dict],*,version:str,sequence:int,expires:int)->dict:
 globals()['version'](version)
 if type(sequence) is not int or not 1<=sequence<=2147483647:raise ModuleError('Invalid release sequence')
 if type(expires) is not int or expires<=int(time.time()):raise ModuleError('Expired/invalid bundle')
 if not isinstance(modules,list) or len(modules)>64:raise ModuleError('Module count exceeds limit')
 by_id={};catalog=[]
 for m in modules:
  if not isinstance(m,dict) or type(m.get('schema_version')) is not int or m.get('schema_version')!=1:raise ModuleError('Invalid module schema')
  name=m.get('id')
  if not isinstance(name,str) or not ID.fullmatch(name) or name in by_id:raise ModuleError('Invalid/duplicate module id')
  globals()['version'](m.get('version'))
  if type(m.get('enabled',True)) is not bool:raise ModuleError('enabled must be boolean')
  by_id[name]=copy.deepcopy(m)
 active={k:v for k,v in by_id.items() if v.get('enabled',True)}
 resolved={};stack=[]
 def resolve(id):
  if id in resolved:return resolved[id]
  if id in stack:raise ModuleError('Module dependency cycle: '+' -> '.join(stack+[id]))
  m=active.get(id)
  if m is None:raise ModuleError('Missing/disabled dependency: '+id)
  deps=m.get('dependencies',{})
  if not isinstance(deps,dict) or len(deps)>32 or any(not isinstance(k,str) or not ID.fullmatch(k) for k in deps):raise ModuleError('Invalid dependency list')
  stack.append(id);inherited={}
  own=glossary(m.get('dictionary',{}))
  for dependency,minimum in sorted(deps.items()):
   if dependency not in active:raise ModuleError('Missing/disabled dependency: '+dependency)
   if globals()['version'](active[dependency]['version'])<globals()['version'](minimum):raise ModuleError('Dependency version too old: '+dependency)
   values=resolve(dependency)
   for sec,rows in values.items():
    dest=inherited.setdefault(sec,{})
    for key,value in rows.items():
     if key in dest and dest[key]!=value and key not in own.get(sec,{}):raise ModuleError('Conflicting dependency terms: '+key)
     dest[key]=value
  for sec,rows in own.items():inherited.setdefault(sec,{}).update(rows)
  stack.pop();resolved[id]=inherited;return inherited
 hooks=[];all_ids=set();sites=set();tests=[]
 for id,m in sorted(active.items()):
  terms=resolve(id);rows=m.get('hooks',[])
  if not isinstance(rows,list) or len(rows)>128:raise ModuleError('Invalid module hooks')
  checked=[]
  for row in rows:
   h=checked_hook(row,id,terms)
   if h['id'] in all_ids:raise ModuleError('Duplicate hook id')
   all_ids.add(h['id'])
   site=tuple(h.get(k,0 if k=='argument' else '') for k in ('class','method','descriptor','kind','argument','member_class','member','member_descriptor'))
   if site in sites:raise ModuleError('Ambiguous duplicate hook site: '+h['id'])
   sites.add(site);checked.append(h)
  hooks.extend(sorted(checked,key=lambda h:h['id']))
  digest=hashlib.sha256(json.dumps(m,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf8')).hexdigest()
  coverage=m.get('coverage',[])
  if not isinstance(coverage,list) or len(coverage)>50 or any(not isinstance(c,str) or len(c)>250 for c in coverage):raise ModuleError('Invalid coverage description')
  catalog.append({'id':id,'version':m['version'],'source_sha256':digest,'dependencies':m.get('dependencies',{}),'hook_count':len(checked),'coverage':coverage})
  samples=m.get('tests',[])
  if not isinstance(samples,list) or len(samples)>512:raise ModuleError('Invalid self-test list/budget')
  for test in samples:
   if not isinstance(test,dict) or set(test)!={'hook','input','expect'} or any(not isinstance(v,str) or not v for v in test.values()):raise ModuleError('Invalid module self-test')
   name=id+':'+test['hook']
   if name not in all_ids or len(test['input'])>8192 or len(test['expect'])>16384:raise ModuleError('Invalid self-test target/text')
   tests.append(dict(test,hook=name))
 if len(hooks)>160 or len(tests)>512:raise ModuleError('Compiled bundle exceeds runtime budget')
 result={'schema_version':2 if any(h['kind'] in ('wrap','refresh') for h in hooks) else 1,'version':version,'sequence':sequence,'expires':expires,'module_catalog':catalog,'hooks':hooks,'tests':tests}
 if len(json.dumps(result,ensure_ascii=False).encode('utf8'))>MAX_BYTES:raise ModuleError('Compiled bundle exceeds 2 MiB')
 return result

def encode(bundle:dict)->bytes:return (json.dumps(bundle,ensure_ascii=False,indent=2)+'\n').encode('utf8')
def main()->int:
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('modules',type=Path);p.add_argument('--version',required=True);p.add_argument('--sequence',type=int,required=True);p.add_argument('--expires',type=int,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 if (a.modules.parent.parent/'languages/locales.json').is_file():raise ModuleError('Locale-first repository: use compose_localized.py --locale <language> to avoid dropping scoped translations')
 files=sorted(a.modules.glob('*/module.json'))
 if not files:raise ModuleError('No module.json under supplied root')
 rows=[]
 for file in files:
  if file.is_symlink() or file.stat().st_size>MAX_BYTES:raise ModuleError('Invalid source file')
  rows.append(load_json(file.read_text('utf8')))
 result=compose(rows,version=a.version,sequence=a.sequence,expires=a.expires);body=encode(result)
 if a.out.exists() and a.out.read_bytes()!=body:raise ModuleError('Refusing to overwrite another built bundle; use a new version/path')
 a.out.parent.mkdir(parents=True,exist_ok=True)
 fd,tmp=tempfile.mkstemp(prefix='.bundle-',suffix='.pending',dir=a.out.parent)
 try:
  with os.fdopen(fd,'wb') as f:f.write(body);f.flush();os.fsync(f.fileno())
  os.replace(tmp,a.out)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
 print('MODULES_COMPOSED',len(result['module_catalog']),'hooks',len(result['hooks']),'bytes',len(body),'sha256',hashlib.sha256(body).hexdigest())
 print('Unsigned: validate bytecode sites against the supported game/mod JARs, then sign and publish.')
 return 0
if __name__=='__main__':
 try:raise SystemExit(main())
 except (ModuleError,OSError) as e:raise SystemExit('MODULE_BUNDLE_REJECTED: '+str(e))
