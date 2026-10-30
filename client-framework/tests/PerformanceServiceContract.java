package sao.zh.compat16;
import java.nio.file.*;import java.lang.instrument.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;import java.util.zip.*;import jdk.jfr.consumer.*;
public final class PerformanceServiceContract {
 static int checks;static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args)throws Exception{
  Path root=Files.createTempDirectory("sao-perf-service-test-");Path folder=root.resolve("SAO_ZH_Performance");Files.createDirectories(folder);Path config=folder.resolve("performance.properties");
  AtomicInteger installed=new AtomicInteger();
  Instrumentation inst=(Instrumentation)Proxy.newProxyInstance(PerformanceServiceContract.class.getClassLoader(),new Class<?>[]{Instrumentation.class},(p,m,a)->{
   if(m.getName().equals("addTransformer")){ok(a[0] instanceof PerformanceTransformer,"Wrong diagnostic transformer");installed.incrementAndGet();return null;}
   if(m.getName().equals("getAllLoadedClasses"))return new Class<?>[0];
   if(m.getReturnType()==boolean.class)return true;if(m.getReturnType()==long.class)return 0L;return null;
  });
  Files.writeString(config,"enabled=false\ndurationSeconds=120\n");ok(!PerformanceService.install(inst,root),"Disabled module started");ok(installed.get()==0,"Disabled module installed bytecode");
  Files.writeString(config,"enabled=true\ndurationSeconds=10\ndeepJfr=true\n");ok(PerformanceService.install(inst,root),"Opt-in module failed");
  for(int i=0;i<100;i++)PerformanceBridge.record(0,16000000L);
  long deadline=System.nanoTime()+20_000_000_000L;Path session=null;
  while(System.nanoTime()<deadline){try(var dirs=Files.list(folder.resolve("reports"))){session=dirs.findFirst().orElse(null);}if(session!=null&&Files.isRegularFile(session.resolve("summary.json")))break;Thread.sleep(40);}
  ok(session!=null&&Files.isRegularFile(session.resolve("summary.json")),"No report was written");String report=Files.readString(session.resolve("summary.json"));
  ok(report.contains("\"remote_server_tps\":null")&&report.contains("\"gpu_execution_ms\":null"),"Unmeasured values fabricated");
  ok(!report.contains(System.getProperty("user.home")),"Summary includes user profile path");
  Files.writeString(config,"enabled=false\n");while(PerformanceBridge.enabled()&&System.nanoTime()<deadline)Thread.sleep(50);
  ok(!PerformanceBridge.enabled(),"Stop control ignored");
  Path jfr=session.resolve("deep-local-only.jfr");while(!Files.isRegularFile(jfr)&&System.nanoTime()<deadline)Thread.sleep(50);ok(Files.isRegularFile(jfr),"Optional JFR was not persisted");
  try(RecordingFile rf=new RecordingFile(jfr)){while(rf.hasMoreEvents()){String name=rf.readEvent().getEventType().getName();ok(!name.equals("jdk.InitialEnvironmentVariable")&&!name.equals("jdk.InitialSystemProperty"),"Sensitive startup metadata enabled");}}
  Map<String,Object> sanitized=PerformanceJfrReport.summarize(jfr);String safe=PerformanceService.json(sanitized);ok(sanitized.containsKey("top_sampled_locations"),"JFR summary missing");ok(!safe.contains(root.toString())&&!safe.contains("commandLine"),"JFR summary leaked private data");
  ok(installed.get()==1,"Duplicate transformers");ok(PerformanceBridge.class.getClassLoader()==null,"Probe must be loaded by bootstrap loader");
  System.out.println("PERFORMANCE_SERVICE_JFR_PASS checks="+checks+" bootstrap=true raw_jfr_local=true no_game=true");
 }
}
