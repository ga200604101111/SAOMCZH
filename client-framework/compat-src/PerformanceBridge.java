package sao.zh.compat16;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;import java.util.concurrent.locks.ReentrantLock;
/** Optional bounded timing only. Bootstrap-safe: java.base, no file/network/GL APIs. */
public final class PerformanceBridge {
 private static final String[] LABELS={"client_frame_loop","client_tick","game_render","window_update_wait","ttf_rasterize","msdf_measure","msdf_draw","frame_start_interval","glyph_disk_read","glyph_disk_write"};
 private static final int CAPACITY=4096,N=LABELS.length;
 private static final ReentrantLock LOCK=new ReentrantLock();
 private static final long[][] RING=new long[N][CAPACITY];
 private static final long[] COUNTS=new long[N],TOTALS=new long[N],MAX=new long[N],SLOW=new long[N];
 private static final AtomicLong DROPPED=new AtomicLong(),ABANDONED=new AtomicLong();
 private static volatile boolean enabled;private static volatile long generation;
 private static final class Scope {long gen=Long.MIN_VALUE,lastFrame;final long[] starts=new long[N];}
 private static final ThreadLocal<Scope> LOCAL=ThreadLocal.withInitial(Scope::new);
 private PerformanceBridge(){}
 public static boolean enabled(){return enabled;}
 public static void setEnabled(boolean value){
  LOCK.lock();try{
   enabled=false;generation++;
   if(value){Arrays.fill(COUNTS,0);Arrays.fill(TOTALS,0);Arrays.fill(MAX,0);Arrays.fill(SLOW,0);DROPPED.set(0);ABANDONED.set(0);}
   enabled=value;
  }finally{LOCK.unlock();}
 }
 private static Scope scope(){Scope s=LOCAL.get();long g=generation;if(s.gen!=g){Arrays.fill(s.starts,0);s.lastFrame=0;s.gen=g;}return s;}
 public static void begin(int channel){
  if(!enabled||channel<0||channel>=N)return;
  Scope s=scope();long now=System.nanoTime();if(s.starts[channel]!=0)ABANDONED.incrementAndGet();s.starts[channel]=now;
  if(channel==0){if(s.lastFrame!=0)record(7,now-s.lastFrame);s.lastFrame=now;}
 }
 public static void end(int channel){
  if(!enabled||channel<0||channel>=N)return;
  Scope s=scope();long start=s.starts[channel];s.starts[channel]=0;if(start!=0)record(channel,System.nanoTime()-start);
 }
 /** Also used by glyph disk workers. Never blocks render threads waiting for report serialization. */
 public static void record(int channel,long nanos){
  if(!enabled||channel<0||channel>=N||nanos<0)return;
  if(!LOCK.tryLock()){DROPPED.incrementAndGet();return;}
  try{
   if(!enabled)return;long index=COUNTS[channel]++;RING[channel][(int)(index%CAPACITY)]=nanos;
   TOTALS[channel]+=nanos;MAX[channel]=Math.max(MAX[channel],nanos);if(nanos>=50000000L)SLOW[channel]++;
  }finally{LOCK.unlock();}
 }
 /** Worker-only snapshot. Percentiles describe the last 4096 samples, not entire session. */
 public static Map<String,Object> snapshot(){
  long[][] samples=new long[N][];long[] counts,totals,max,slow;long dropped,abandoned;boolean active;
  LOCK.lock();try{
   counts=COUNTS.clone();totals=TOTALS.clone();max=MAX.clone();slow=SLOW.clone();dropped=DROPPED.get();abandoned=ABANDONED.get();active=enabled;
   for(int i=0;i<N;i++)samples[i]=Arrays.copyOf(RING[i],(int)Math.min(counts[i],CAPACITY));
  }finally{LOCK.unlock();}
  List<Object>channels=new ArrayList<>(N);
  for(int i=0;i<N;i++){
   long[] s=samples[i];Arrays.sort(s);Map<String,Object> m=new LinkedHashMap<>();m.put("name",LABELS[i]);m.put("count",counts[i]);m.put("window_samples",s.length);
   m.put("mean_ms",counts[i]==0?null:totals[i]/(double)counts[i]/1e6);m.put("p50_ms",percentile(s,.5));m.put("p95_ms",percentile(s,.95));m.put("p99_ms",percentile(s,.99));
   m.put("max_ms",counts[i]==0?null:max[i]/1e6);m.put("over_50ms_count",slow[i]);channels.add(m);
  }
  Map<String,Object> result=new LinkedHashMap<>();result.put("enabled",active);result.put("channels",channels);result.put("dropped_samples",dropped);result.put("abandoned_scopes",abandoned);
  result.put("timing_semantics","wall-clock; scopes overlap; window update includes waits, not GPU execution time");return result;
 }
 private static Double percentile(long[] s,double fraction){return s.length==0?null:s[Math.max(0,(int)Math.ceil(s.length*fraction)-1)]/1e6;}
}
