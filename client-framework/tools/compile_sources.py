#!/usr/bin/env python3
"""Compile the checked-in sources with JDK 17+ and an explicitly supplied ASM JAR.
Produces unsigned development artifacts only, never uses a maintainer private key.
"""
from pathlib import Path
import argparse,subprocess,zipfile,io,os,shutil

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--jdk',type=Path,required=True);p.add_argument('--asm',type=Path,required=True);p.add_argument('--out',type=Path);a=p.parse_args()
 root=Path(__file__).resolve().parents[1];build=a.out or root/'build-from-source';classes=build/'classes';compat=build/'compat';out=build/'artifacts'
 for d in (classes,compat,out):d.mkdir(parents=True,exist_ok=True)
 javac=a.jdk/'bin'/('javac.exe' if os.name=='nt' else 'javac');assert javac.is_file() and a.asm.is_file()
 def compile(target,files):
  result=subprocess.run([str(javac),'--release','17','-encoding','UTF-8','-cp',str(a.asm),'-d',str(target)]+[str(f) for f in files],capture_output=True,timeout=90)
  print((result.stdout+result.stderr).decode('utf8','replace'));assert result.returncode==0
 compile(classes,(root/'src').rglob('*.java'));compile(compat,(root/'compat-src').rglob('*.java'))
 allclasses={f.relative_to(classes).as_posix():f.read_bytes() for f in classes.rglob('*.class')}
 def jar(path,entries,manifest=None):
  with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as z:
   if manifest:z.writestr('META-INF/MANIFEST.MF',manifest)
   for name,data in entries.items():z.writestr(name,data)
  with zipfile.ZipFile(path) as z:assert z.testzip() is None
 manifest='Manifest-Version: 1.0\r\nPremain-Class: sao.zh.boot.Agent\r\nCan-Retransform-Classes: true\r\nCan-Redefine-Classes: false\r\n\r\n'
 jar(out/'SAO_ZH_Agent_v16.jar',{n:b for n,b in allclasses.items() if n.startswith('sao/zh/boot/')},manifest)
 api=io.BytesIO()
 with zipfile.ZipFile(api,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in allclasses.items():
   if n.startswith('sao/zh/api/'):z.writestr(n,b)
 engine={n:b for n,b in allclasses.items() if n.startswith('sao/zh/engine/')};engine['embedded/api.jar']=api.getvalue()
 with zipfile.ZipFile(a.asm) as z:
  for n in z.namelist():
   if n.startswith('org/objectweb/asm/') and n.endswith('.class'):engine[n]=z.read(n)
 engine['META-INF/ASM-LICENSE.txt']=(root/'licenses/ASM-BSD-3-Clause.txt').read_bytes();jar(out/'SAO_ZH_Runtime_1.2.0.jar',engine)
 cc={f.relative_to(compat).as_posix():f.read_bytes() for f in compat.rglob('*.class')}
 prefixes=tuple('sao/zh/compat16/'+v for v in ['SplashBridge','SplashAssets','PackLifecycleManager','GitHubTranslationClient','CjkLayout','PackBridge','RuntimeBridge','MiniJson','FontColorPolicy','LiteralGlyphs','PerformanceBridge','GlyphPageStore','GlyphCacheBridge'])
 buf=io.BytesIO()
 with zipfile.ZipFile(buf,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in cc.items():
   if n.startswith(prefixes):z.writestr(n,b)
 entries={n:b for n,b in cc.items() if not n.startswith(prefixes)};entries['embedded/SAO_ZH_compat16_bridge.jar']=buf.getvalue();entries['META-INF/Noto-OFL-1.1.txt']=(root/'licenses/Noto-OFL-1.1.txt').read_bytes()
 jar(out/'SAO_ZH_Compatibility_1.0.1.jar',entries)
 print('SOURCE_COMPILE_UNSIGNED_PASS',out)
 print('Official bootstrap only trusts signed official modules. Modified builds need an independent trust key and signed descriptors; do not overwrite published releases.')
if __name__=='__main__':main()
