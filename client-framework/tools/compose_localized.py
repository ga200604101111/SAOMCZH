#!/usr/bin/env python3
"""Compile hook-only modules and per-locale text rules to a signed-runtime-compatible rules bundle.

Only extensions/<locale> is authoritative. Existing protocol-1 language mirrors are
maintained by migration/release tools until installed V16 clients can read canonical paths.
"""
from pathlib import Path
import argparse,copy,hashlib,json,re,time,os,tempfile
from compose_modules import compose,encode,entries,glossary,load_json,ModuleError
class LocaleError(ModuleError):pass
LOCALE=re.compile(r'^[a-z]{2,3}_[a-z]{2,4}$')
def checked_json(p:Path,limit:int=8*1024*1024):
    if p.is_symlink() or not p.is_file() or p.stat().st_size>limit:raise LocaleError('Missing/oversized local source: '+str(p))
    return load_json(p.read_text('utf8'))
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
    if overrides.get('schema_version')!=1 or overrides.get('locale')!=locale or not isinstance(overrides.get('hooks'),dict):raise LocaleError('Invalid scoped text rules')
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
            key=m['id']+':'+h['id'];text=overrides['hooks'].get(key,{})
            if not isinstance(text,dict) or set(text)-{'exact','operations'}:raise LocaleError('Unknown scoped text operation')
            for k,v in text.items():h[k]=copy.deepcopy(v)
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
