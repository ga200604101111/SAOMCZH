package sao.zh.compat16;
import java.io.*;import java.lang.instrument.Instrumentation;import java.lang.management.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.time.*;import java.time.format.DateTimeFormatter;import java.util.*;import java.util.concurrent.*;import jdk.jfr.*;
/** System-loader management/JFR worker. Disabled means no timing transformer or sampler. */
public final class PerformanceService implements AutoCloseable {
 private static volatile PerformanceService instance;
 private final Path directory,control;private final int seconds;private final long start=System.nanoTime();
 private final ScheduledExecutorService worker;private final boolean deep;
 private Recording recording;private String jfrStatus="off";private volatile boolean closed;private int samples;
 private long previousGcMs=-1,previousGcCount=-1;
 private PerformanceService(Path root,Path control,int seconds,boolean deep)throws IOException{
  this.control=control;this.seconds=seconds;this.deep=deep;
  Path reports=root.resolve("SAO_ZH_Performance/reports");Files.createDirectories(reports);
  if(Files.isSymbolicLink(reports))throw new IOException("Symlinked report directory rejected");
  try(var existing=Files.list(reports)){if(existing.filter(x->x.getFileName().toString().matches("session-[0-9]{8}-[0-9]{6}-[0-9a-f]{8}")).count()>=10)throw new IOException("Archive previous performance sessions before recording more; max 10");}
  String tag=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())+"-"+UUID.randomUUID().toString().substring(0,8);
  directory=reports.resolve("session-"+tag);Files.createDirectory(directory);
  worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"SAO-Performance-Writer");t.setDaemon(true);return t;});
 }
 public static synchronized boolean install(Instrumentation instrumentation,Path root){
  if(instance!=null)return true;Path control=root.resolve("SAO_ZH_Performance/performance.properties");
  try{
   Properties p=readControl(control);if(!Boolean.parseBoolean(p.getProperty("enabled","false")))return false;
   int seconds=bounded(p.getProperty("durationSeconds","120"),10,1800);
   PerformanceService service=new PerformanceService(root,control,seconds,Boolean.parseBoolean(p.getProperty("deepJfr","false")));
   try{
    instrumentation.addTransformer(new PerformanceTransformer(),true);
    // During normal startup these targets are still unloaded. Support already-loaded
    // targets without touching unrelated classes; a failure skips only that target.
    for(Class<?> c:instrumentation.getAllLoadedClasses())if(PerformanceTransformer.owners().contains(c.getName().replace('.','/'))&&instrumentation.isModifiableClass(c)){
     try{instrumentation.retransformClasses(c);}catch(Exception|LinkageError ignored){System.err.println("[SAO-Perf] ALREADY_LOADED_TARGET_SKIPPED "+c.getName());}
    }
    instance=service;PerformanceBridge.setEnabled(true);service.worker.scheduleWithFixedDelay(service::sampleSafely,0,1,TimeUnit.SECONDS);
    System.out.println("[SAO-Perf] ENABLED durationSeconds="+seconds+" deepJfr="+service.deep+" reports=SAO_ZH_Performance/reports");return true;
   }catch(Exception|LinkageError e){service.close();throw e;}
  }catch(Exception|LinkageError e){System.err.println("[SAO-Perf] DISABLED_KEEP_GAME "+e.getClass().getSimpleName());return false;}
 }
 private static int bounded(String s,int min,int max){int n=Integer.parseInt(s);if(n<min||n>max)throw new IllegalArgumentException("Duration budget");return n;}
 private static Properties readControl(Path p)throws IOException{
  Properties result=new Properties();if(!Files.isRegularFile(p)||Files.isSymbolicLink(p))return result;
  if(Files.size(p)>4096)throw new IOException("Oversized performance settings");try(Reader r=Files.newBufferedReader(p,StandardCharsets.UTF_8)){result.load(r);}return result;
 }
 private void startJfr(){
  if(!deep||recording!=null)return;
  Recording pending=null;
  try{
   if(!FlightRecorder.isAvailable()){jfrStatus="unavailable";return;}
   Recording r=new Recording();pending=r;r.setName("SAO-local-performance");r.setToDisk(true);r.setMaxSize(64L*1024*1024);r.setMaxAge(Duration.ofSeconds(60));r.setDuration(Duration.ofSeconds(Math.min(seconds,60)));
   Set<String> events=new HashSet<>();for(EventType e:FlightRecorder.getFlightRecorder().getEventTypes())events.add(e.getName());
   for(String event:List.of("jdk.ExecutionSample","jdk.NativeMethodSample"))if(events.contains(event))r.enable(event).withPeriod(Duration.ofMillis(20));
   for(String event:List.of("jdk.GCPhasePause","jdk.JavaMonitorEnter","jdk.ThreadPark","jdk.FileRead","jdk.FileWrite"))if(events.contains(event))r.enable(event).withThreshold(Duration.ofMillis(event.equals("jdk.GCPhasePause")?0:10)).withStackTrace();
   for(String event:List.of("jdk.CPULoad","jdk.ThreadCPULoad"))if(events.contains(event))r.enable(event).withPeriod(Duration.ofSeconds(1));
   r.start();recording=r;jfrStatus="recording_local_only";
  }catch(Exception|LinkageError e){if(pending!=null)pending.close();jfrStatus="failed_"+e.getClass().getSimpleName();}
 }
 private void sampleSafely(){
  if(closed)return;
  try{
   if(samples==0)startJfr();
   boolean stop=System.nanoTime()-start>=TimeUnit.SECONDS.toNanos(seconds)||!Boolean.parseBoolean(readControl(control).getProperty("enabled","false"));
   finishJfr(stop);
   Map<String,Object> report=collect();report.put("finished",stop);report.put("sample_number",++samples);
   Files.writeString(directory.resolve("timeline.jsonl"),json(report)+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
   writeAtomic(directory.resolve("summary.json"),json(report)+"\n");
   if(stop)close();
  }catch(Exception|LinkageError e){System.err.println("[SAO-Perf] SAMPLE_STOPPED "+e.getClass().getSimpleName());close();}
 }
 private Map<String,Object> collect(){
  Map<String,Object> m=new LinkedHashMap<>();m.put("schema_version",1);m.put("elapsed_seconds",(System.nanoTime()-start)/1e9);m.put("timings",PerformanceBridge.snapshot());
  MemoryUsage heap=ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();m.put("heap_used_bytes",heap.getUsed());m.put("heap_committed_bytes",heap.getCommitted());m.put("heap_max_bytes",heap.getMax());
  long ms=0,count=0;for(GarbageCollectorMXBean g:ManagementFactory.getGarbageCollectorMXBeans()){if(g.getCollectionTime()>=0)ms+=g.getCollectionTime();if(g.getCollectionCount()>=0)count+=g.getCollectionCount();}
  m.put("gc_collection_time_delta_ms",previousGcMs<0?null:Math.max(0,ms-previousGcMs));m.put("gc_count_delta",previousGcCount<0?null:Math.max(0,count-previousGcCount));previousGcMs=ms;previousGcCount=count;
  m.put("gc_semantics","MXBean collection time is aggregate; not proof that every millisecond stopped render thread");
  OperatingSystemMXBean os=ManagementFactory.getOperatingSystemMXBean();m.put("logical_processors",os.getAvailableProcessors());
  if(os instanceof com.sun.management.OperatingSystemMXBean bean){double cpu=bean.getProcessCpuLoad();m.put("process_cpu_fraction",cpu<0?null:cpu);m.put("physical_memory_free_bytes",bean.getFreeMemorySize());}
  m.put("jvm_threads",ManagementFactory.getThreadMXBean().getThreadCount());m.put("jfr_status",jfrStatus);
  m.put("gpu_execution_ms",null);m.put("gpu_utilization",null);m.put("remote_server_tps",null);m.put("network_rtt_ms",null);
  m.put("limitations",List.of("unmeasured fields are null, not zero","frame loop includes client tick, window waits and FPS limiting","timing scopes overlap; do not add category totals","percentiles use bounded recent samples","profiling overhead requires A/B measurement","raw JFR may contain local file paths; no automatic upload"));return m;
 }
 private static void writeAtomic(Path path,String text)throws IOException{
  Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,text,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
  try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
 }
 private synchronized void finishJfr(boolean force){
  Recording r=recording;if(r==null)return;
  if(!force&&r.getState()!=RecordingState.STOPPED)return;
  try{
   if(r.getState()==RecordingState.RUNNING)r.stop();
   if(r.getState()==RecordingState.STOPPED){
    Path pending=directory.resolve("deep-local-only.jfr.pending"),complete=directory.resolve("deep-local-only.jfr");r.dump(pending);
    try{Files.move(pending,complete,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(pending,complete);}
    jfrStatus="saved_local_only";
   }
  }catch(Exception e){jfrStatus="save_failed_"+e.getClass().getSimpleName();}
  finally{r.close();recording=null;}
 }
 public synchronized void close(){
  if(closed)return;closed=true;PerformanceBridge.setEnabled(false);worker.shutdown();
  finishJfr(true);
 }
 static String json(Object value){
  if(value==null)return "null";if(value instanceof Boolean)return value.toString();if(value instanceof Number){double d=((Number)value).doubleValue();return Double.isFinite(d)?value.toString():"null";}
  if(value instanceof Map<?,?> map){StringJoiner j=new StringJoiner(",","{","}");for(var e:map.entrySet())j.add(json(String.valueOf(e.getKey()))+":"+json(e.getValue()));return j.toString();}
  if(value instanceof Iterable<?> list){StringJoiner j=new StringJoiner(",","[","]");for(Object x:list)j.add(json(x));return j.toString();}
  String s=String.valueOf(value);StringBuilder b=new StringBuilder("\"");for(int i=0;i<s.length();i++){char c=s.charAt(i);switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();
 }
}
