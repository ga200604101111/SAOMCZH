package sao.zh.compat16;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import jdk.jfr.consumer.*;
/** Offline JFR aggregation: omit file/socket paths, thread names, process arguments and event payloads. */
public final class PerformanceJfrReport {
 private PerformanceJfrReport(){}
 public static Map<String,Object> summarize(Path recording)throws Exception{
  if(!Files.isRegularFile(recording)||Files.size(recording)>256L*1024*1024)throw new IllegalArgumentException("JFR file budget exceeded");
  Map<String,Long> counts=new TreeMap<>(),hot=new HashMap<>();Map<String,Double> duration=new TreeMap<>(),max=new TreeMap<>();long events=0,renderSamples=0;
  try(RecordingFile reader=new RecordingFile(recording)){
   while(reader.hasMoreEvents()){
    if(++events>2_000_000)throw new IllegalArgumentException("JFR event budget exceeded");
    RecordedEvent event=reader.readEvent();String type=event.getEventType().getName();counts.merge(type,1L,Long::sum);
    double ms=event.getDuration().toNanos()/1e6;if(ms>0){duration.merge(type,ms,Double::sum);max.merge(type,ms,Math::max);}
    if(!type.equals("jdk.ExecutionSample")&&!type.equals("jdk.NativeMethodSample"))continue;
    RecordedThread thread=event.hasField("sampledThread")?event.getThread("sampledThread"):event.getThread();
    boolean render=thread!=null&&"Render thread".equals(thread.getJavaName());if(render)renderSamples++;
    RecordedStackTrace trace=event.getStackTrace();if(trace==null||trace.getFrames().isEmpty())continue;
    // The first frame is a sampled stack location, not a proven mod culprit or wall-time fraction.
    RecordedMethod method=trace.getFrames().get(0).getMethod();String location=(render?"render:":"other:")+method.getType().getName()+"::"+method.getName();
    if(hot.size()<4096||hot.containsKey(location))hot.merge(location,1L,Long::sum);else hot.merge("other:aggregation-budget",1L,Long::sum);
   }
  }
  List<Object> top=new ArrayList<>();hot.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(40).forEach(e->top.add(Map.of("location",e.getKey(),"samples",e.getValue())));
  Map<String,Object> result=new LinkedHashMap<>();result.put("schema_version",1);result.put("events",events);result.put("event_counts",counts);result.put("aggregate_duration_ms",duration);result.put("max_duration_ms",max);result.put("render_thread_execution_samples",renderSamples);result.put("top_sampled_locations",top);
  result.put("limitations",List.of("sample counts are not exact elapsed time","no GPU execution timing or remote server TPS","aggregate event durations may overlap","file paths and payloads intentionally omitted"));return result;
 }
 public static void main(String[]args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("Usage: PerformanceJfrReport input.jfr output.json");
  Path input=Path.of(args[0]).toAbsolutePath().normalize(),output=Path.of(args[1]).toAbsolutePath().normalize();
  if(input.equals(output)||Files.exists(output))throw new IllegalArgumentException("Refusing to overwrite an existing file");
  Files.writeString(output,PerformanceService.json(summarize(input))+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);System.out.println("LOCAL_JFR_SUMMARY_READY "+output);
 }
}
