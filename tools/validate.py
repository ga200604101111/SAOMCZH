#!/usr/bin/env python3
"""Validate GitHub-managed community translations without external dependencies."""
from __future__ import annotations
import hashlib,json,re,sys
from pathlib import Path, PurePosixPath
ROOT=Path(__file__).resolve().parents[1]
LOCALE=re.compile(r'^[a-z]{2,3}_[a-z]{2,4}$')
SHA256=re.compile(r'^[0-9a-f]{64}$')
VERSION=re.compile(r'^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$')
SECTIONS=('names','messages','lines','items')
TOKEN=re.compile(r'%(?:[0-9]+\$)?[-+#0 ,(]*[0-9]*(?:\.[0-9]+)?[a-zA-Z]|%%|\{[^{}\r\n]{1,80}\}')
class ValidationError(ValueError): pass
def no_dupes(pairs):
 result={}
 for k,v in pairs:
  if k in result:raise ValidationError(f'duplicate JSON key: {k}')
  result[k]=v
 return result
def loads_bytes(raw:bytes,limit:int):
 if len(raw)>limit:raise ValidationError('JSON file too large')
 try:return json.loads(raw.decode('utf-8-sig'),object_pairs_hook=no_dupes)
 except (UnicodeError,ValueError) as e:raise ValidationError(str(e)) from e
def relative_path(base:Path,path:str,locale:str)->Path:
 if not isinstance(path,str) or '\\' in path or '?' in path or '#' in path:raise ValidationError('Invalid path')
 parts=PurePosixPath(path).parts
 if not parts or any(x in ('','.','..') for x in parts) or not path.startswith(f'languages/{locale}/'):raise ValidationError('Path outside language directory')
 resolved=base.joinpath(*parts).resolve()
 if not resolved.is_relative_to(base.resolve()):raise ValidationError('Path escapes repository')
 return resolved
def validate_index(data):
 if not isinstance(data,dict) or data.get('schema_version')!=1 or data.get('client_protocol')!=1:raise ValidationError('Unsupported manifest schema/protocol')
 langs=data.get('locales')
 if not isinstance(langs,dict) or not langs:raise ValidationError('No languages')
 for locale,entry in langs.items():
  if not isinstance(locale,str) or not LOCALE.fullmatch(locale):raise ValidationError('Invalid locale')
  if not isinstance(entry,dict) or not isinstance(entry.get('version_manifest'),str):raise ValidationError('Invalid locale entry')
 return langs
def validate_version(version,locale):
 if not isinstance(version,dict) or version.get('schema_version')!=1 or version.get('locale')!=locale:raise ValidationError('Incorrect version locale/schema')
 if not isinstance(version.get('version'),str) or not VERSION.fullmatch(version['version']):raise ValidationError('Invalid version')
 if version.get('min_client_protocol')!=1:raise ValidationError('Unsupported client protocol')
 trans=version.get('translation')
 if not isinstance(trans,dict):raise ValidationError('No translation data metadata')
 if not isinstance(trans.get('size_bytes'),int) or not 0<trans['size_bytes']<=8*1024*1024:raise ValidationError('Translation file size out of range')
 if not isinstance(trans.get('sha256'),str) or not SHA256.fullmatch(trans['sha256']):raise ValidationError('Invalid SHA256')
 return trans
def validate_translation(raw:bytes,translation,locale,meta):
 if len(raw)!=meta['size_bytes'] or hashlib.sha256(raw).hexdigest()!=meta['sha256']:raise ValidationError('Checksum/size mismatch')
 if not isinstance(translation,dict) or translation.get('schema_version')!=1 or translation.get('locale')!=locale:raise ValidationError('Translation locale mismatch')
 n=0
 for section in SECTIONS:
  rows=translation.get(section)
  if not isinstance(rows,dict):raise ValidationError('Missing section: '+section)
  if len(rows)>30000:raise ValidationError('Too many entries')
  for source,target in rows.items():
   if not isinstance(source,str) or not isinstance(target,str) or not source or not target:raise ValidationError('Empty/nonstring entry')
   if len(source)>16384 or len(target)>16384:raise ValidationError('Entry exceeds maximum length')
   if sorted(TOKEN.findall(source))!=sorted(TOKEN.findall(target)):raise ValidationError('Placeholder mismatch: '+repr(source[:50]))
   if '\x00' in source or '\x00' in target:raise ValidationError('NUL in entry')
   n+=1
 return n
def validate_repo(root:Path=ROOT)->dict:
 manifest=loads_bytes((root/'manifest.json').read_bytes(),1024*1024)
 langs=validate_index(manifest)
 out={}
 for locale,entry in langs.items():
  ver_path=relative_path(root,entry['version_manifest'],locale)
  version=loads_bytes(ver_path.read_bytes(),1024*1024)
  meta=validate_version(version,locale)
  filepath=relative_path(root,meta['path'],locale)
  raw=filepath.read_bytes()
  out[locale]={'version':version['version'],'entries':validate_translation(raw,loads_bytes(raw,8*1024*1024),locale,meta),'bytes':len(raw)}
 return out
if __name__=='__main__':
 try:
  report=validate_repo(ROOT)
  print('GITHUB_TRANSLATION_VALIDATE_PASS',json.dumps(report,ensure_ascii=False))
 except (ValidationError,FileNotFoundError,PermissionError) as e:
  print('GITHUB_TRANSLATION_VALIDATE_FAIL',str(e),file=sys.stderr);sys.exit(1)
