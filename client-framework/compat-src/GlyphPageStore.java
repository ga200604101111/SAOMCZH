package sao.zh.compat16;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/** Local, reconstructible grayscale glyph pages. Render callers never perform disk IO.
 * Page groups contain only observed glyphs, not every glyph in the group.
 * Native pointers, OpenGL IDs, source text and Java serialization are never persisted. */
public final class GlyphPageStore implements AutoCloseable {
 private static final int MAGIC=0x53474331,MAX_PAGE=4*1024*1024,MAX_GLYPH=256*1024,MAX_FILES=4096;
 private static final long QUEUE_LIMIT=8L*1024*1024;
 private static final Pattern NS=Pattern.compile("[0-9a-f]{64}"),FILE=Pattern.compile("[0-9a-f]{64}-[0-9a-f]{8}\\.gpage");
 private record Entry(int width,int height,byte[] pixels){}
 private record Page(String name,Map<Integer,Entry> entries,long charge){}
 private final Path root;
 private final long memoryLimit,diskLimit;
 private final ReentrantLock lock=new ReentrantLock();
 private final LinkedHashMap<String,Page> memory=new LinkedHashMap<>(32,.75f,true),dirty=new LinkedHashMap<>();
 private final LinkedHashSet<String> reads=new LinkedHashSet<>(),warms=new LinkedHashSet<>();
 private final ScheduledExecutorService worker;
 private final FileChannel lockChannel;private final FileLock processLock;
 private volatile boolean active=true;
 private long memoryBytes,dirtyBytes,lastStatus;
 private final AtomicLong hits=new AtomicLong(),misses=new AtomicLong(),diskReads=new AtomicLong(),diskWrites=new AtomicLong(),corrupt=new AtomicLong(),drops=new AtomicLong(),errors=new AtomicLong();
 private volatile long diskBytes;
 public GlyphPageStore(Path directory,long memoryLimit,long diskLimit)throws IOException{
  if(memoryLimit<4096||memoryLimit>256L*1024*1024||diskLimit<4096||diskLimit>2L*1024*1024*1024)throw new IOException("Glyph cache budget invalid");
  root=directory.toAbsolutePath().normalize();this.memoryLimit=memoryLimit;this.diskLimit=diskLimit;
  for(Path p=root;p!=null;p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Cache path cannot contain symlinks");
  Files.createDirectories(root);
  Path marker=root.resolve("writer.lock");if(Files.isSymbolicLink(marker))throw new IOException("Unsafe cache lock");
  lockChannel=FileChannel.open(marker,StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
  FileLock acquired;
  try{acquired=lockChannel.tryLock();if(acquired==null)throw new IOException("Glyph cache already in use");}
  catch(Exception e){lockChannel.close();throw new IOException("Glyph cache already in use",e);}
  processLock=acquired;
  worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"SAO-GlyphCache-IO");t.setDaemon(true);return t;});
  worker.scheduleWithFixedDelay(this::cycle,20,60,TimeUnit.MILLISECONDS);
 }
 private static boolean valid(String ns,int glyph,int w,int h){return ns!=null&&NS.matcher(ns).matches()&&glyph>=0&&glyph<1048576&&w>0&&h>0&&w<=2048&&h<=2048&&(long)w*h<=MAX_GLYPH;}
 private static String name(String ns,int glyph){return ns+'-'+String.format(Locale.ROOT,"%08x",glyph>>>6)+".gpage";}
 private static Page page(String name,Map<Integer,Entry> values){long bytes=256;for(Entry e:values.values())bytes+=128+e.pixels.length;return new Page(name,Map.copyOf(values),bytes);}
 public byte[] get(String ns,int glyph,int width,int height){
  if(!active||!valid(ns,glyph,width,height))return null;
  if(!lock.tryLock()){drops.incrementAndGet();return null;}
  try{
   Page p=memory.get(name(ns,glyph));Entry e=p==null?null:p.entries.get(glyph);
   if(e!=null&&e.width==width&&e.height==height){hits.incrementAndGet();return e.pixels.clone();}
   misses.incrementAndGet();if(reads.size()<128)reads.add(name(ns,glyph));else drops.incrementAndGet();return null;
  }finally{lock.unlock();}
 }
 public void put(String ns,int glyph,int width,int height,byte[]pixels){
  if(!active||!valid(ns,glyph,width,height)||pixels==null||pixels.length!=(long)width*height)return;
  if(!lock.tryLock()){drops.incrementAndGet();return;}
  try{
   String n=name(ns,glyph);Page old=memory.get(n);Map<Integer,Entry> values=new HashMap<>(old==null?Map.of():old.entries);
   values.put(glyph,new Entry(width,height,pixels.clone()));Page p=page(n,values);
   if(p.charge>MAX_PAGE||p.charge>memoryLimit){drops.incrementAndGet();return;}
   cache(p);
   Page previous=dirty.get(n);long next=dirtyBytes-(previous==null?0:previous.charge)+p.charge;
   if(next<=QUEUE_LIMIT&&(dirty.size()<64||previous!=null)){dirty.put(n,p);dirtyBytes=next;}else drops.incrementAndGet();
  }finally{lock.unlock();}
 }
 public void warm(String ns){
  if(!active||ns==null||!NS.matcher(ns).matches())return;
  if(!lock.tryLock()){drops.incrementAndGet();return;}
  try{if(warms.size()<64)warms.add(ns);}finally{lock.unlock();}
 }
 private void cache(Page p){
  if(p.charge>memoryLimit)return;Page old=memory.put(p.name,p);if(old!=null)memoryBytes-=old.charge;memoryBytes+=p.charge;
  while(memoryBytes>memoryLimit||memory.size()>512){var i=memory.entrySet().iterator();Page removed=i.next().getValue();i.remove();memoryBytes-=removed.charge;}
 }
 private void cycle(){
  try{
   for(int i=0;i<4;i++){Page p=takeDirty();if(p==null)break;write(p);}
   String ns;lock.lock();try{ns=pop(warms);}finally{lock.unlock();}
   if(ns!=null){int count=0;for(Path p:files()){if(p.getFileName().toString().startsWith(ns+'-')){load(p.getFileName().toString());if(++count>=128)break;}}}
   for(int i=0;i<8;i++){String n;lock.lock();try{n=pop(reads);}finally{lock.unlock();}if(n==null)break;load(n);}
   if(System.nanoTime()-lastStatus>5_000_000_000L){status();lastStatus=System.nanoTime();}
  }catch(Exception|LinkageError e){errors.incrementAndGet();}
 }
 private static String pop(LinkedHashSet<String> set){var it=set.iterator();if(!it.hasNext())return null;String s=it.next();it.remove();return s;}
 private Page takeDirty(){lock.lock();try{var it=dirty.entrySet().iterator();if(!it.hasNext())return null;Page p=it.next().getValue();it.remove();dirtyBytes-=p.charge;return p;}finally{lock.unlock();}}
 private static byte[] digest(byte[] data){try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
 private Page read(String n)throws IOException{
  if(!FILE.matcher(n).matches())return null;Path path=root.resolve(n);
  if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(path))return null;
  long size=Files.size(path);if(size<100||size>MAX_PAGE)throw new IOException("Invalid page size");
  byte[] all;try(InputStream stream=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){all=stream.readNBytes(MAX_PAGE+1);}if(all.length!=size)throw new IOException("Page changed during read");
  byte[] body=Arrays.copyOf(all,all.length-32);if(!MessageDigest.isEqual(digest(body),Arrays.copyOfRange(all,body.length,all.length)))throw new IOException("Glyph checksum mismatch");
  try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(body))){
   if(in.readInt()!=MAGIC)throw new IOException("Wrong glyph page format");String ns=in.readUTF();int group=in.readInt(),count=in.readInt();
   if(!NS.matcher(ns).matches()||group<0||group>=16384||!n.equals(name(ns,group<<6))||count<1||count>64)throw new IOException("Invalid page identity");
   Map<Integer,Entry> values=new HashMap<>();for(int i=0;i<count;i++){
    int idx=in.readInt(),w=in.readInt(),h=in.readInt(),length=in.readInt();
    if(!valid(ns,idx,w,h)||idx>>>6!=group||length!=(long)w*h||length>in.available()||values.containsKey(idx))throw new IOException("Invalid glyph record");
    values.put(idx,new Entry(w,h,in.readNBytes(length)));
   }
   if(in.available()!=0)throw new IOException("Trailing page data");return page(n,values);
  }
 }
 private void load(String n){
  long start=System.nanoTime();try{
   Page p=read(n);if(p==null)return;diskReads.incrementAndGet();
   lock.lock();try{Page current=memory.get(n);if(current!=null){Map<Integer,Entry> joined=new HashMap<>(p.entries);joined.putAll(current.entries);p=page(n,joined);}cache(p);}finally{lock.unlock();}
  }catch(IOException|RuntimeException e){corrupt.incrementAndGet();}
  finally{timing(8,System.nanoTime()-start);}
 }
 private List<Path> files()throws IOException{
  List<Path> found=new ArrayList<>();try(DirectoryStream<Path> stream=Files.newDirectoryStream(root,"*.gpage")){
   for(Path p:stream){if(FILE.matcher(p.getFileName().toString()).matches()&&Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)){found.add(p);if(found.size()>MAX_FILES)break;}}
  }
  long present=0;for(Path p:found)present+=Files.size(p);diskBytes=present;
  found.sort(Comparator.comparingLong(p->{try{return -Files.getLastModifiedTime(p,LinkOption.NOFOLLOW_LINKS).toMillis();}catch(IOException e){return Long.MAX_VALUE;}}));return found;
 }
 private boolean makeRoom(String name,long size)throws IOException{
  List<Path> files=files();if(files.size()>MAX_FILES)return false;long total=0;for(Path p:files)total+=Files.size(p);
  Path dest=root.resolve(name);long old=Files.isRegularFile(dest,LinkOption.NOFOLLOW_LINKS)?Files.size(dest):0;
  if(size>diskLimit)return false;
  int remaining=files.size();for(int i=files.size()-1;i>=0&&(total-old+size>diskLimit||remaining>=MAX_FILES);i--){Path p=files.get(i);if(p.equals(dest))continue;long b=Files.size(p);Files.delete(p);total-=b;remaining--;}
  diskBytes=total-old+size;return diskBytes<=diskLimit;
 }
 private void write(Page fresh){
  long start=System.nanoTime();try{
   Page old;try{old=read(fresh.name);}catch(IOException bad){old=null;corrupt.incrementAndGet();}
   Map<Integer,Entry> merged=new TreeMap<>(old==null?Map.of():old.entries);merged.putAll(fresh.entries);
   try(ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes)){
    String ns=fresh.name.substring(0,64);int group=Integer.parseUnsignedInt(fresh.name.substring(65,73),16);
    out.writeInt(MAGIC);out.writeUTF(ns);out.writeInt(group);out.writeInt(merged.size());
    for(var e:merged.entrySet()){Entry g=e.getValue();out.writeInt(e.getKey());out.writeInt(g.width);out.writeInt(g.height);out.writeInt(g.pixels.length);out.write(g.pixels);}
    out.flush();byte[] data=bytes.toByteArray();if(data.length+32>MAX_PAGE||!makeRoom(fresh.name,data.length+32)){drops.incrementAndGet();return;}
    ByteArrayOutputStream complete=new ByteArrayOutputStream();complete.write(data);complete.write(digest(data));atomic(root.resolve(fresh.name),complete.toByteArray());diskWrites.incrementAndGet();
   }
  }catch(IOException|RuntimeException e){errors.incrementAndGet();}finally{timing(9,System.nanoTime()-start);}
 }
 private static void timing(int channel,long nanos){try{PerformanceBridge.record(channel,nanos);}catch(LinkageError ignored){}}
 private void atomic(Path destination,byte[] bytes)throws IOException{
  if(Files.isSymbolicLink(destination))throw new IOException("Unsafe cache destination");Path tmp=Files.createTempFile(root,".glyph-",".pending");
  try{Files.write(tmp,bytes);try{Files.move(tmp,destination,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,destination,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
 }
 public Map<String,Long> stats(){
  Map<String,Long> s=new TreeMap<>();s.put("memory_hits",hits.get());s.put("misses",misses.get());s.put("disk_reads",diskReads.get());s.put("disk_writes",diskWrites.get());s.put("corrupt_pages",corrupt.get());s.put("dropped",drops.get());s.put("io_errors",errors.get());s.put("disk_bytes",diskBytes);s.put("enabled",active?1L:0L);
  lock.lock();try{s.put("memory_bytes",memoryBytes);s.put("pending_write_bytes",dirtyBytes);s.put("memory_pages",(long)memory.size());}finally{lock.unlock();}return s;
 }
 private void status()throws IOException{StringBuilder text=new StringBuilder("format=1\nmode=native-ttf-raster-pages\n");stats().forEach((k,v)->text.append(k).append('=').append(v).append('\n'));atomic(root.resolve("status.properties"),text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 public synchronized void close(){
  if(!active)return;active=false;
  worker.execute(()->{try{Page p;while((p=takeDirty())!=null)write(p);status();}catch(Exception e){errors.incrementAndGet();}finally{try{processLock.release();lockChannel.close();}catch(IOException ignored){}}});worker.shutdown();
  try{worker.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
  lock.lock();try{memory.clear();memoryBytes=0;reads.clear();warms.clear();}finally{lock.unlock();}
 }
}
