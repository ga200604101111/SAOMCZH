#!/usr/bin/env python3
"""Validate and sign a new hook bundle. Signing key remains on the owner's computer.
Use --install-local for an immediate local trial; --repo + --publish to release via GitHub.
No game restart, agent replacement, or player chat upload is performed.
"""
from pathlib import Path
import argparse,json,time,hashlib,subprocess,os,base64
from cryptography.hazmat.primitives import serialization

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('rules',type=Path);p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[2]);p.add_argument('--key',type=Path);p.add_argument('--repo',type=Path);p.add_argument('--install-local',action='store_true');p.add_argument('--publish',action='store_true');p.add_argument('--java',default='java');p.add_argument('--mod',type=Path,action='append',default=[]);a=p.parse_args()
 workspace=Path(__file__).resolve().parents[1];artifacts=workspace/'artifacts';raw=a.rules.read_bytes();assert len(raw)<2*1024*1024
 rules=json.loads(raw);assert isinstance(rules,dict);assert 'sequence' in rules and 'version' in rules
 rules.setdefault('expires',int(time.time())+180*86400)
 body=(json.dumps(rules,ensure_ascii=False,indent=2)+'\n').encode('utf8')
 out=workspace/'publication';out.mkdir(exist_ok=True);candidate=out/'unsigned-candidate.json';candidate.write_bytes(body)
 cp=os.pathsep.join([str(artifacts/'SAO_ZH_Runtime_1.0.0.jar'),str(artifacts/'SAO_ZH_Agent_v16.jar')])
 r=subprocess.run([a.java,'-cp',cp,'sao.zh.engine.ValidateRules',str(candidate)]+[str(f) for f in a.mod],capture_output=True,timeout=45)
 print((r.stdout+r.stderr).decode('utf8','replace'));assert r.returncode==0,'Rules were not signed because validation failed'
 keyfile=a.key or a.root/'.sao_zh_signing/extension-ed25519.pk8';key=serialization.load_der_private_key(keyfile.read_bytes(),password=None)
 public=key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)
 expected=base64.b64decode((workspace/'PUBLIC_KEY.txt').read_text('ascii').strip());assert public==expected,'Wrong maintainer signing key'
 signature=key.sign(body);name='rules-'+rules['version'];digest=hashlib.sha256(body).hexdigest()
 if a.repo:
  dest=a.repo/'extensions/v1';dest.mkdir(parents=True,exist_ok=True)
  existing=dest/(name+'.json')
  if existing.exists() and existing.read_bytes()!=body:raise RuntimeError('Never overwrite an already-published rule version; increment version and sequence')
 else:dest=out
 (dest/(name+'.json')).write_bytes(body);(dest/(name+'.sig')).write_bytes(signature)
 index=('api=1\nversion='+rules['version']+'\nexpires='+str(rules['expires'])+'\nsha256='+digest+'\nbytes='+str(len(body))+'\nurl=https://raw.githubusercontent.com/ga200604101111/SAOMCZH/main/extensions/v1/'+name+'.json\nsignature_url=https://raw.githubusercontent.com/ga200604101111/SAOMCZH/main/extensions/v1/'+name+'.sig\n').encode()
 (dest/'stable.properties').write_bytes(index);(dest/'stable.sig').write_bytes(key.sign(index))
 if a.install_local:
  local=a.root/'SAO_ZH_Extensions/rules';local.mkdir(parents=True,exist_ok=True)
  for n,b in [('candidate.sig',signature),('candidate.json',body)]:
   temp=local/(n+'.pending');temp.write_bytes(b);os.replace(temp,local/n)
  print('LOCAL_SIGNED_CANDIDATE_INSTALLED',local)
 if a.publish:
  assert a.repo and (a.repo/'.git').exists(),'--publish requires --repo'
  files=['extensions/v1/'+x for x in [name+'.json',name+'.sig','stable.properties','stable.sig']]
  env=dict(os.environ,GIT_TERMINAL_PROMPT='0',GCM_INTERACTIVE='Never')
  def git(*args):
   x=subprocess.run(['git',*args],cwd=a.repo,env=env,capture_output=True,timeout=60)
   if x.returncode:raise RuntimeError((x.stdout+x.stderr).decode('utf8','replace'))
   return x.stdout.decode('utf8','replace')
  git('add','--',*files);staged=git('diff','--cached','--name-only').splitlines();assert not set(staged)-set(files),'Other staged files: refusing unrelated commit'
  if staged:git('commit','-m','extensions: publish signed hooks '+rules['version'])
  git('push','origin','main');print('SIGNED_RULES_PUBLISHED',rules['version'],digest)
 else:print('SIGNED_RULES_READY',rules['version'],digest,'not pushed')
if __name__=='__main__':main()
