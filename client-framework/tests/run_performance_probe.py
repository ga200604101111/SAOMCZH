#!/usr/bin/env python3
"""Run timing, exact installed bytecode and real JFR contracts without launching Minecraft."""
from pathlib import Path
import argparse,os,subprocess,tempfile,zipfile,json

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for name in ('jdk','asm','minecraft-jar','saoui-jar'):p.add_argument('--'+name,required=True,type=Path)
 p.add_argument('--java',type=Path);p.add_argument('--report',type=Path);a=p.parse_args()
 root=Path(__file__).resolve().parents[1];javac=a.jdk/'bin'/('javac.exe' if os.name=='nt' else 'javac');java=a.java or a.jdk/'bin'/('java.exe' if os.name=='nt' else 'java')
 for file in [javac,java,a.asm,a.minecraft_jar,a.saoui_jar]:
  if not file.is_file():raise SystemExit('Missing input: '+str(file))
 results={}
 def run(args,label):
  proc=subprocess.run(list(map(str,args)),capture_output=True,timeout=90);out=(proc.stdout+proc.stderr).decode('utf8','replace');print(label,proc.returncode,out[-4000:],flush=True)
  if proc.returncode:raise SystemExit('FAILED '+label)
  results[label]=out.strip()
 with tempfile.TemporaryDirectory(prefix='sao-perf-verify-') as tmp:
  work=Path(tmp);classes=work/'classes';classes.mkdir()
  source=[root/'compat-src'/f'{name}.java' for name in ('PerformanceBridge','PerformanceTransformer','PerformanceService','PerformanceJfrReport')]
  tests=[root/'tests'/f'{name}.java' for name in ('PerformanceContract','PerformanceNativeContract','PerformanceServiceContract')]
  run([javac,'--release','17','-encoding','UTF-8','-cp',a.asm,'-d',classes,*source,*tests],'COMPILE')
  bridge=work/'probe-bootstrap.jar'
  with zipfile.ZipFile(bridge,'w',zipfile.ZIP_DEFLATED) as z:
   for f in (classes/'sao/zh/compat16').glob('PerformanceBridge*.class'):z.write(f,f.relative_to(classes).as_posix())
  cp=os.pathsep.join(map(str,[classes,a.asm]));base=[java,'-Xverify:all','-Xbootclasspath/a:'+str(bridge),'-cp',cp]
  run([*base,'sao.zh.compat16.PerformanceContract'],'BOUNDED_TIMING')
  run([*base,'sao.zh.compat16.PerformanceNativeContract',a.minecraft_jar,a.saoui_jar],'ACTUAL_TARGETS_AND_VM_FIXTURE')
  run([*base,'sao.zh.compat16.PerformanceServiceContract'],'BOOTSTRAP_WRITER_AND_REAL_JFR')
 results['game_launched']=False;results['live_game_performance_measured']=False
 if a.report:
  a.report.parent.mkdir(parents=True,exist_ok=True);a.report.write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n','utf8')
 print('PERFORMANCE_EXTENSION_OFFLINE_ALL_PASS')
if __name__=='__main__':main()
