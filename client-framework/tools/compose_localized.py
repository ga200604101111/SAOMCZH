#!/usr/bin/env python3
"""Compile hook-only modules and per-locale text rules to a signed-runtime-compatible rules bundle.

Translations remain authoritative in languages/<locale> alongside scoped rule sets.
Only exact (zero/one ASCII padding) matching is expanded at compile time; no wildcard
execution or Runtime update is necessary to preserve V16 clients.
"""
from pathlib import Path
import argparse,copy,hashlib,json,re,time,os,tempfile
from compose_modules import compose,encode,entries,glossary,load_json,ModuleError
class LocaleError(ModuleError):pass
LOCALE=re.compile(r'^[a-z]{2,3}_[a-z]{2,4}$')
def checked_json(p:Path,limit:int=8*1024*1024):
    if p.is_symlink() or not p.is_file() or p.stat().st_size>limit:raise LocaleError('Missing/oversized local source: '+str(p))
    return load_json(p.read_text('utf8'))

# Explicit anchored text matching: padding is exactly zero/one ASCII space on each side.
# Not a regex or wildcard. Both compiler and engine still use the existing exact-match ABI.
def expand_space_exact(mapping:dict[str,str])->dict[str,str]:
    checked=entries(mapping)
    expanded={}
    for src,dst in checked.items():
        if src.strip(' ')!=src or any(char in src for char in '*?[]'):
            raise LocaleError('Padded keys must be plain exact text without wildcards or surrounding spaces: '+repr(src))
        if dst.strip(' ')!=dst:
            raise LocaleError('Padded translations must not contain surrounding spaces')
        for left,right in (('',''),(' ',''),('',' '),(' ',' ')):
            key=left+src+right
            value=left+dst+right
            existing=expanded.get(key)
            if existing is not None and existing!=value:raise LocaleError('Ambiguous padded exact key: '+repr(key))
            expanded[key]=value
    return expanded

def compile_scoped_text(spec:dict,rule_sets:dict,hook_id:str,stack:tuple[str,...]=())->dict:
    if not isinstance(spec,dict) or set(spec)-{'use','exact','padded_exact','operations'}:
        raise LocaleError('Invalid hook-local rule declaration: '+hook_id)
    if 'use' in spec:
        if len(spec)!=1:raise LocaleError('A shared rule reference must be the only rule declaration: '+hook_id)
        ref=spec['use']
        if not isinstance(ref,str) or not re.fullmatch('[a-z][a-z0-9_-]{0,47}',ref):
            raise LocaleError('Invalid rule-set reference')
        if ref in stack:raise LocaleError('Rule-set cycle: '+' -> '.join(stack+(ref,)))
        value=rule_sets.get(ref)
        if value is None:raise LocaleError('Unknown shared rule set: '+ref)
        return compile_scoped_text(value,rule_sets,hook_id,stack+(ref,))
    exact=entries(spec.get('exact',{}))
    padded=expand_space_exact(spec.get('padded_exact',{}))
    for key,value in padded.items():
        if key in exact and exact[key]!=value:
            raise LocaleError('Conflicting exact and space-preserving translation: '+hook_id+' '+repr(key))
        exact[key]=value
    operations=spec.get('operations',[])
    if not isinstance(operations,list) or len(operations)>8:raise LocaleError('Instruction budget exceeded in '+hook_id)
    for op in operations:
        if not isinstance(op,dict) or set(op)-{'op','from','to'} or op.get('op') not in ('prefix','suffix','replace_literal'):
            raise LocaleError('Invalid literal operation in '+hook_id)
        if not isinstance(op.get('from'),str) or not 0<len(op['from'])<=256 or not isinstance(op.get('to',''),str) or len(op.get('to',''))>256:
            raise LocaleError('Invalid operation operand in '+hook_id)
    result={}
    if exact:result['exact']=dict(sorted(exact.items()))
    if operations:result['operations']=copy.deepcopy(operations)
    return result

def compose_bundle(root:Path,locale:str,version:str,sequence:int,expires:int)->dict:
    root=Path(root).resolve()
    if not LOCALE.fullmatch(locale):raise LocaleError('Unsafe locale identifier')
    locales=checked_json(root/'languages/locales.json',65536)
    if locales.get('schema_version')!=1 or locale not in locales.get('available',[]):raise LocaleError('Unsupported/unregistered locale')
    directory=root/'languages'/locale
    spec=checked_json(directory/('zh-tw-version.json' if locale=='zh_tw' else 'version.json'),65536)
    if spec.get('schema_version')!=1 or spec.get('locale')!=locale:raise LocaleError('Locale manifest mismatch')
    data=directory/'translations.json';raw=data.read_bytes()
    desc=spec.get('translation',{})
    if desc.get('path')!=f'languages/{locale}/translations.json' or desc.get('sha256')!=hashlib.sha256(raw).hexdigest() or desc.get('size_bytes')!=len(raw):raise LocaleError('Locale translation hash/path mismatch')
    translations=checked_json(data); 
    if translations.get('locale')!=locale or translations.get('schema_version')!=1:raise LocaleError('Translation locale mismatch')
    glossary({k:translations[k] for k in ['names','items','lines','messages']})
    override_path=directory/'hook_text_rules.json';override_bytes=override_path.read_bytes()
    overdesc=spec.get('hook_text_rules',{})
    if overdesc.get('path')!=f'languages/{locale}/hook_text_rules.json' or overdesc.get('sha256')!=hashlib.sha256(override_bytes).hexdigest() or overdesc.get('size_bytes')!=len(override_bytes):raise LocaleError('Locale hook text rules hash mismatch')
    overrides=checked_json(override_path,2*1024*1024)
    if overrides.get('schema_version') not in (1,2) or overrides.get('locale')!=locale or not isinstance(overrides.get('hooks'),dict):raise LocaleError('Invalid scoped text rules')
    if set(overrides)-{'schema_version','locale','rule_sets','hooks'}:raise LocaleError('Unknown localized text rules field')
    rule_sets=overrides.get('rule_sets',{})
    if not isinstance(rule_sets,dict) or len(rule_sets)>64:raise LocaleError('Invalid localized rule set catalog')
    for rule_name,rule_spec in rule_sets.items():
        if not isinstance(rule_name,str) or not re.fullmatch('[a-z][a-z0-9_-]{0,47}',rule_name):
            raise LocaleError('Unsafe localized rule-set name')
        compile_scoped_text(rule_spec,rule_sets,'rule-set:'+rule_name)
    modules=[];names=set()
    files=sorted((root/'extensions/modules').glob('*/module.json'))
    if not files:raise LocaleError('No functional modules')
    for file in files:
        m=checked_json(file,2*1024*1024)
        if file.parent.name!=m.get('id') or m.get('id') in names:raise LocaleError('Invalid module directory/id')
        names.add(m['id'])
        if 'dictionary' in m:raise LocaleError('Translations must not reside in functional modules: '+m['id'])
        for h in m.get('hooks',[]):
            if any(k in h for k in ('dictionary','exact','operations','locale')):raise LocaleError('Localized data in functional hook: '+m['id']+':'+h['id'])
            key=m['id']+':'+h['id']
            text=compile_scoped_text(overrides['hooks'].get(key,{}),rule_sets,key)
            for k,v in text.items():h[k]=v
            h['locale']=locale
        modules.append(m)
    known={m['id']+':'+h['id'] for m in modules for h in m['hooks']}
    if set(overrides['hooks'])-known:raise LocaleError('Orphan scoped text hook: '+str(sorted(set(overrides['hooks'])-known)[:3]))
    bundle=compose(modules,version=version,sequence=sequence,expires=expires)
    bundle['translation_locale']=locale
    bundle['translation_version']=spec['version']
    return bundle
def main()->int:
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[2])
    p.add_argument('--locale',default='zh_tw')
    p.add_argument('--version',required=True)
    p.add_argument('--sequence',type=int,required=True)
    p.add_argument('--expires',type=int,default=0)
    p.add_argument('--out',type=Path,required=True)
    a=p.parse_args()
    b=compose_bundle(a.root,a.locale,a.version,a.sequence,a.expires or int(time.time())+15552000)
    raw=encode(b)
    if a.out.exists() and a.out.read_bytes()!=raw:raise LocaleError('Refusing to overwrite different rule version')
    a.out.parent.mkdir(parents=True,exist_ok=True)
    fd,tmp=tempfile.mkstemp(prefix='.locale-',suffix='.pending',dir=a.out.parent)
    try:
        with os.fdopen(fd,'wb') as f:f.write(raw);f.flush();os.fsync(f.fileno())
        os.replace(tmp,a.out)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('LOCALE_RULES_READY',a.locale,a.version,'hooks',len(b['hooks']),'bytes',len(raw),'sha256',hashlib.sha256(raw).hexdigest())
    return 0
if __name__=='__main__':
    try:raise SystemExit(main())
    except (LocaleError,ModuleError,OSError) as e:raise SystemExit('LOCALE_RULES_REJECTED: '+str(e))
