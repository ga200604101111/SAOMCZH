#!/usr/bin/env python3
"""Run actual 1.21.4/SAOJob bytecode + mock Text/Style/cache tests, no Minecraft launch."""
import argparse,os,subprocess,tempfile
from pathlib import Path

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for key in ('jdk','asm','minecraft-jar','saojob-jar'):
  p.add_argument('--'+key,required=True,type=Path)
 p.add_argument('--java',type=Path,help='Optional Java 21 runtime executable')
 args=p.parse_args()
 root=Path(__file__).resolve().parents[3]
 source=Path(__file__).resolve().parent
 javac=args.jdk/'bin'/('javac.exe' if os.name=='nt' else 'javac')
 java=args.java or (args.jdk/'bin'/('java.exe' if os.name=='nt' else 'java'))
 for filepath in [javac,java,args.asm,args.minecraft_jar,args.saojob_jar]:
  if not filepath.is_file():raise SystemExit('File does not exist: '+str(filepath))
 def run(argv,name):
  result=subprocess.run([str(v) for v in argv],capture_output=True,timeout=90)
  output=(result.stdout+result.stderr).decode('utf8','replace')
  print(name+' '+str(result.returncode)+' '+output[-2400:],flush=True)
  if result.returncode:raise SystemExit('FAILED '+name)
 with tempfile.TemporaryDirectory(prefix='sao-world-test-') as temp:
  base=Path(temp);runtime=base/'runtime';fixture=base/'fixture'
  runtime.mkdir();fixture.mkdir()
  run([javac,'--release','17','-encoding','UTF-8','-cp',args.asm,
       '-d',runtime,*sorted((root/'client-framework/src').rglob('*.java'))],'RUNTIME_COMPILE')
  run([javac,'--release','17','-encoding','UTF-8',
       '-cp',os.pathsep.join(map(str,[runtime,args.asm])), '-d',fixture,
       *sorted((source/'stubs').rglob('*.java')),
       *sorted((source/'src').rglob('*.java'))],'ISOLATED_PROBE_COMPILE')
  run([java,'-Xverify:all','-cp',os.pathsep.join(map(str,[fixture,runtime,args.asm])),
       'sao.zh.engine.WorldTextNativeProbe',args.minecraft_jar,args.saojob_jar],
       'WORLD_RENDER_NATIVE_PROBE')
 print('WORLD_TEXT_RENDER_TESTS_ALL_PASS (no game, no network, no private source)',flush=True)

if __name__=='__main__':main()
