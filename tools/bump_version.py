#!/usr/bin/env python3
'''Bump an already indexed locale version and update translation digest.'''
import hashlib,json,re,sys
from pathlib import Path
from validate import validate_repo,loads_bytes,validate_translation,ValidationError,relative_path
ROOT=Path(__file__).resolve().parents[1]
def main():
 if len(sys.argv)!=3:
  print('Usage: python tools/bump_version.py zh_tw 1.0.1');return 2
 locale,new=sys.argv[1:]
 if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+(?:[-+][0-9A-Za-z.-]+)?',new):
  print('Version must be SemVer, e.g. 1.0.1');return 2
 manifest=loads_bytes((ROOT/'manifest.json').read_bytes(),1024*1024)
 if locale not in manifest.get('locales',{}):
  print('Unknown locale');return 2
 entry=manifest['locales'][locale]
 version_path=relative_path(ROOT,entry['version_manifest'],locale)
 version=loads_bytes(version_path.read_bytes(),1024*1024)
 file=relative_path(ROOT,version['translation']['path'],locale)
 raw=file.read_bytes()
 digest=hashlib.sha256(raw).hexdigest()
 meta={'path':version['translation']['path'],'size_bytes':len(raw),'sha256':digest}
 validate_translation(raw,loads_bytes(raw,8*1024*1024),locale,meta)
 version['translation']=meta;version['version']=new
 version_path.write_text(json.dumps(version,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 print('VERSION_BUMPED',locale,new,'sha256',digest)
 print(validate_repo(ROOT))
 return 0
if __name__=='__main__':
 try:sys.exit(main())
 except (ValidationError,FileNotFoundError) as e:print('ERROR',e,file=sys.stderr);sys.exit(1)
