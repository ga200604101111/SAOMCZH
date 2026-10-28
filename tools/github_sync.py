#!/usr/bin/env python3
"""Fetch versioned language tables from a public GitHub repository; never fetch executable code."""
from __future__ import annotations
import argparse,hashlib,json,os,re,sys,tempfile,time,shutil
from datetime import datetime,timezone
from pathlib import Path
from urllib.error import HTTPError,URLError
from urllib.parse import quote,urlsplit
from urllib.request import HTTPRedirectHandler,Request,build_opener
from validate import (ValidationError,loads_bytes,validate_index,validate_version,validate_translation,relative_path,LOCALE,SECTIONS)
MAX_JSON=8*1024*1024
IDENT=re.compile(r'^[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}$')
REF=re.compile(r'^[a-zA-Z0-9][a-zA-Z0-9_./-]{0,120}$')
class NetworkError(RuntimeError):pass
class SafeRedirect(HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,newurl):
  host=urlsplit(newurl).hostname
  if host!='raw.githubusercontent.com' or urlsplit(newurl).scheme!='https':
   raise NetworkError('Refused redirect to non-GitHub raw host')
  return super().redirect_request(req,fp,code,msg,headers,newurl)
def checked_repo(config:dict)->tuple[str,str,str,str]:
 owner=config.get('owner','');repo=config.get('repo','');ref=config.get('ref','main');locale=config.get('locale','zh_tw')
 if not isinstance(owner,str) or not IDENT.fullmatch(owner) or owner in ('.','..'):raise ValidationError('Invalid repository owner')
 if not isinstance(repo,str) or not IDENT.fullmatch(repo) or repo in ('.','..') or repo.endswith('.git'):raise ValidationError('Invalid repository name')
 if not isinstance(ref,str) or not REF.fullmatch(ref) or ref.startswith(('.', '/')) or '..' in ref or '//' in ref:raise ValidationError('Invalid Git branch/tag')
 if not isinstance(locale,str) or not LOCALE.fullmatch(locale):raise ValidationError('Invalid Minecraft locale')
 return owner,repo,ref,locale
def atomic(path:Path,data:bytes)->None:
 path.parent.mkdir(parents=True,exist_ok=True)
 fd,name=tempfile.mkstemp(prefix='.sao-l10n-',suffix='.pending',dir=str(path.parent))
 try:
  with os.fdopen(fd,'wb') as f:f.write(data);f.flush();os.fsync(f.fileno())
  os.replace(name,path)
 finally:
  try:os.unlink(name)
  except FileNotFoundError:pass
def request_raw(owner:str,repo:str,ref:str,path:str,cache_dir:Path,limit:int=MAX_JSON)->bytes:
 url=f"https://raw.githubusercontent.com/{quote(owner)}/{quote(repo)}/{quote(ref,safe='/')}/{quote(path,safe='/')}"
 key=hashlib.sha256(url.encode()).hexdigest()
 meta_file=cache_dir/(key+'.meta.json');body_file=cache_dir/(key+'.body')
 meta={}
 if meta_file.exists() and body_file.exists():
  try:meta=json.loads(meta_file.read_text('utf8'))
  except (OSError,ValueError):pass
 headers={'Accept':'application/json','User-Agent':'SAO-Community-L10n/1'}
 if isinstance(meta.get('etag'),str):headers['If-None-Match']=meta['etag']
 opener=build_opener(SafeRedirect())
 try:
  with opener.open(Request(url,headers=headers),timeout=8) as response:
   if response.geturl().split('/')[2]!='raw.githubusercontent.com':raise NetworkError('Untrusted response host')
   body=response.read(limit+1)
   if len(body)>limit:raise ValidationError('Remote JSON too large')
   etag=response.headers.get('ETag')
 except HTTPError as e:
  if e.code==304 and body_file.exists() and body_file.stat().st_size<=limit:return body_file.read_bytes()
  raise NetworkError(f'GitHub HTTP {e.code}: {path}') from e
 except (URLError,OSError,TimeoutError) as e:raise NetworkError(f'GitHub unavailable: {e.__class__.__name__}') from e
 cache_dir.mkdir(parents=True,exist_ok=True)
 atomic(body_file,body)
 atomic(meta_file,json.dumps({'etag':etag,'url':url},ensure_ascii=False).encode('utf8'))
 return body
def sync(root:Path,config:dict,fetch=None)->dict:
 if config.get('enabled') is not True:raise ValidationError('GitHub translation sync is disabled in github-source.json')
 owner,repo,ref,locale=checked_repo(config)
 folder=root/'SAO_TW_Dynamic'
 cache=folder/'github-http-cache'
 fetch=fetch or (lambda path,max_bytes:request_raw(owner,repo,ref,path,cache,max_bytes))
 manifest=loads_bytes(fetch('manifest.json',1024*1024),1024*1024)
 available=validate_index(manifest)
 if locale not in available:raise ValidationError(f'Locale {locale} is not offered by this repository')
 version_path=available[locale]['version_manifest']
 relative_path(root,version_path,locale)
 version=loads_bytes(fetch(version_path,1024*1024),1024*1024)
 source=validate_version(version,locale)
 relative_path(root,source['path'],locale)
 remote=folder/'github-cache'/(locale+'.json')
 if remote.exists() and remote.stat().st_size==source['size_bytes']:
  existing=remote.read_bytes()
  if hashlib.sha256(existing).hexdigest()==source['sha256']:raw=existing
  else:raw=fetch(source['path'],MAX_JSON)
 else:raw=fetch(source['path'],MAX_JSON)
 translation=loads_bytes(raw,MAX_JSON)
 count=validate_translation(raw,translation,locale,source)
 if not remote.exists() or remote.read_bytes()!=raw:atomic(remote,raw)
 state={'locale':locale,'version':version['version'],'repo':owner+'/'+repo,'ref':ref,'sha256':source['sha256'],
        'entries':count,'updated_at':datetime.now(timezone.utc).isoformat(),'cached_at':str(remote),'applied_v9':False}
 if config.get('apply_to_v9') is True:
  if locale!='zh_tw':raise ValidationError('V9 only supports zh_tw; other locales are cache-only until the multi-language client is installed')
  current=folder/'zh_tw.json'
  if not current.exists():raise ValidationError('V9 local dictionary does not exist')
  before=current.read_bytes()
  local=loads_bytes(before,MAX_JSON)
  if local.get('locale')!='zh_tw':raise ValidationError('Unexpected V9 locale')
  result=dict(local)
  for sec in SECTIONS:result[sec]=dict(local.get(sec,{}))
  # Legacy values remain as fallback; reviewed GitHub values replace corresponding keys.
  for sec in SECTIONS:result[sec].update(translation[sec])
  override_path=folder/'local-overrides.json'
  if override_path.exists():
   override=loads_bytes(override_path.read_bytes(),MAX_JSON)
   if override.get('locale')!='zh_tw':raise ValidationError('Local overrides have wrong locale')
   for sec in SECTIONS:
    for k,v in override.get(sec,{}).items():
     if not isinstance(k,str) or not isinstance(v,str):raise ValidationError('Invalid local override')
     result[sec][k]=v
  dest=(json.dumps(result,ensure_ascii=False,indent=2)+'\n').encode('utf8')
  if dest!=before:
   backup=folder/'backups'/('zh_tw_before_github_'+datetime.now(timezone.utc).strftime('%Y%m%d_%H%M%S_%f')+'.json')
   atomic(backup,before);atomic(current,dest)
   state['backup']=str(backup)
  state['applied_v9']=True
 state_dir=folder/'github-cache'
 atomic(state_dir/(locale+'.state.json'),(json.dumps(state,ensure_ascii=False,indent=2)+'\n').encode('utf8'))
 return state
def main()->int:
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--root',type=Path,default=Path('D:/翻譯'))
 p.add_argument('--config',type=Path,default=None)
 p.add_argument('--watch',action='store_true',help='Check repeatedly while this process is running (optional)')
 args=p.parse_args()
 cf=args.config or args.root/'SAO_TW_Dynamic/github-source.json'
 if not cf.exists():
  print('CONFIG_NOT_FOUND: copy github-source.example.json to '+str(cf)+' and set owner/repo/enabled')
  return 2
 try:config=json.loads(cf.read_text('utf8'))
 except (ValueError,OSError) as e:print('CONFIG_INVALID '+str(e));return 2
 minutes=config.get('check_interval_minutes',60)
 if not isinstance(minutes,int) or not 15<=minutes<=1440:print('INVALID_CHECK_INTERVAL 15..1440 minutes');return 2
 while True:
  try:print('GITHUB_SYNC',json.dumps(sync(args.root,config),ensure_ascii=False))
  except (ValidationError,NetworkError,OSError) as e:
   print('GITHUB_SYNC_SKIPPED_KEEP_LAST_GOOD',str(e),file=sys.stderr)
   if not args.watch:return 1
  if not args.watch:break
  time.sleep(minutes*60)
 return 0
if __name__=='__main__':sys.exit(main())
