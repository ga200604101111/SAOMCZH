package sao.zh.engine;
import sao.zh.boot.*;import sao.zh.api.Dispatch;
import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.lang.instrument.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.jar.*;
/** External runtime. The installed kernel never names Minecraft classes or implements translation. */
public final class Engine {
 private final LayoutSupport layout=new LayoutSupport();private volatile Map<String,Script.Hook>refreshGuards=Map.of();
 private final Instrumentation inst;private final Path root,store;private final RuleTransformer transformer=new RuleTransformer();
 private volatile Script current=Script.empty(),previous;private volatile byte[]currentBytes,currentSig,previousBytes,previousSig;
 private final AtomicLong epoch=new AtomicLong();private volatile Map<String,Map<String,String>>dictionary=Map.of();
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"SAO-ZH-Extensions");t.setDaemon(true);return t;});
 private final ExecutorService downloads=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"SAO-ZH-Download");t.setDaemon(true);return t;});private final AtomicBoolean fetching=new AtomicBoolean();private volatile long controlEpoch;private long nextBinaryCheck;
 private final List<JarFile>held=new ArrayList<>();private final Set<String>logged=ConcurrentHashMap.newKeySet();private volatile long highWater;private long nextNetwork;
 private final Path engineJar;private long candidateStamp=-1,dictStamp=-1,controlStamp=-1;private volatile boolean updatesPaused;private String remoteDigest="";private final boolean fixture;private boolean nativeTest;
 private Engine(Instrumentation i,Path root,Path jar,boolean fixture){inst=i;this.root=root;store=root.resolve("SAO_ZH_Extensions");engineJar=jar;this.fixture=fixture;}
 public static Engine start(Instrumentation inst,Path root,Path jar,String args)throws Exception{
  Engine e=new Engine(inst,root,jar,args.equals("fixture")||args.equals("watch-test"));e.nativeTest=args.equals("native-test")||args.equals("watch-test");e.installApi();
  if(!e.fixture)e.installCompatibility();
  inst.addTransformer(e.transformer,true);
  Path state=e.store.resolve("rules/sequence.txt");if(Files.isRegularFile(state))try{e.highWater=Long.parseLong(Files.readString(state).trim());}catch(Exception bad){Agent.log("SEQUENCE_STATE_INVALID_NETWORK_DISABLED");e.nextNetwork=Long.MAX_VALUE;}
  e.reloadDictionary();e.restoreLastGood();e.restoreWatcherState();e.bind();
  if((!e.fixture&&!e.nativeTest)||args.equals("watch-test"))e.worker.scheduleWithFixedDelay(e::poll,2,2,TimeUnit.SECONDS);
  Agent.log("MODULAR_RULE_ENGINE_READY external=true hooks="+e.current.byId.size());return e;
 }
 private void installApi()throws Exception{
  byte[]data;try(JarFile j=new JarFile(engineJar.toFile());InputStream in=j.getInputStream(j.getJarEntry("embedded/api.jar"))){if(in==null)throw new IOException("Missing runtime API");data=in.readAllBytes();}
  Path p=store.resolve("api/"+Verified.hash(data)+".jar");if(!Files.exists(p))Verified.atomic(p,data);JarFile api=new JarFile(p.toFile());inst.appendToBootstrapClassLoaderSearch(api);held.add(api);
  Class<?>abi=Class.forName("sao.zh.api.Dispatch",true,null);if(abi.getClassLoader()!=null)throw new IllegalStateException("Bootstrap ABI not isolated");
 }
 private void installCompatibility(){
  try{
   Path d=store.resolve("compatibility");Path f=d.resolve("current.properties"),s=d.resolve("current.sig");
   if(!Files.isRegularFile(f)||!Files.isRegularFile(s)){
    String base="https://raw.githubusercontent.com/ga200604101111/SAOMCZH/main/extensions/v1/compatibility";
    byte[]b=Verified.fetch(base+".properties",65536),sig=Verified.fetch(base+".sig",4096);Verified.descriptor(b,sig);Verified.atomic(s,sig);Verified.atomic(f,b);
   }
   Map<String,String>spec=Verified.selectModule(d);Path module=Verified.module(d,spec);
   JarFile jar=new JarFile(module.toFile());inst.appendToSystemClassLoaderSearch(jar);held.add(jar);
   System.setProperty("sao.zh.extension.root",root.toString());
   Class<?>legacy=Class.forName("sao.zh.compat16.DirectAgent",true,ClassLoader.getSystemClassLoader());
   legacy.getMethod("premain",String.class,Instrumentation.class).invoke(null,nativeTest?"test":"",inst);
   Agent.log("EXTERNAL_SAO_COMPATIBILITY_LOADED version="+spec.get("version"));
  }catch(Throwable bad){Agent.log("COMPATIBILITY_SKIPPED_KEEP_GAME "+bad.getClass().getSimpleName()+" "+String.valueOf(bad.getMessage()));}
 }
 private void bind(){Dispatch.bind((id,value,owner)->{
  Script snapshot=current;Script.Hook h=snapshot.byId.get(id);if(h==null||h.failures.get()>=3||!TextProjection.locale(owner,h.locale))return value;
  try{return TextProjection.apply(value,owner,text->translate(h,text));}
  catch(Exception|LinkageError error){if(h.failures.incrementAndGet()<=3)once("hook-fault-"+snapshot.sequence+"-"+id,"HOOK_FAIL_OPEN "+id+" "+error.getClass().getSimpleName());return value;}
 },epoch.get());
 Dispatch.bindSupport((id,receiver,owner,text,size,bold,width,limit,original)->{
  if(text==null||text.length()>8192||!Float.isFinite(size)||size<=0||size>1024||!Float.isFinite(width)||width<=0||width>1000000||limit<1||limit>64)return null;
  Script.Hook h=current.byId.get(id);if(h==null||!h.kind.equals("wrap")||h.failures.get()>=3||!TextProjection.locale(owner,h.locale))return null;
  try{return layout.wrap(h,receiver,owner,translate(h,text),size,bold,width,limit,epoch.get());}
  catch(Exception|LinkageError error){if(h.failures.incrementAndGet()<=3)once("wrap-fault-"+current.sequence+"-"+id,"WRAP_KEEP_ORIGINAL "+id+" "+error.getClass().getSimpleName());return null;}
 },(id,receiver,owner)->{
  Script.Hook h=refreshGuards.get(id);if(h==null||h.failures.get()>=3)return;
  try{layout.refresh(h,receiver,owner,epoch.get());}
  catch(Exception|LinkageError error){if(h.failures.incrementAndGet()<=3)once("refresh-fault-"+current.sequence+"-"+id,"REFRESH_KEEP_SCREEN "+id+" "+error.getClass().getSimpleName());}
 });}
 private String translate(Script.Hook h,String raw){
  if(raw==null||raw.length()>8192)return raw;
  String result=h.literal(raw);if(!result.equals(raw))return result;
  Map<String,Map<String,String>>data=dictionary;String next=data.getOrDefault(h.section,Map.of()).get(raw);
  if(next==null&&h.section.equals("items"))for(String section:new String[]{"names","lines"}){next=data.getOrDefault(section,Map.of()).get(raw);if(next!=null)break;}
  return next==null||next.length()>16384?raw:next;
 }
 public synchronized String applySigned(byte[]body,byte[]sig)throws Exception{
  Verified.signature(body,sig);Script next=Script.parse(body);
  if(next.sequence<highWater)throw new SecurityException("Remote rollback rejected; use explicit local rollback");
  if(currentBytes!=null&&Arrays.equals(currentBytes,body))return "UNCHANGED";
  if(next.sequence==current.sequence&&currentBytes!=null)throw new SecurityException("Sequence reused with different content");
  transition(next);previousBytes=currentBytes;previousSig=currentSig;currentBytes=body.clone();currentSig=sig.clone();highWater=Math.max(highWater,next.sequence);
  persist();return "APPLIED "+next.version;
 }
 private void transition(Script next)throws Exception{
  Script old=current,oldPlan=transformer.plan;
  Map<String,Script.Hook>guards=new TreeMap<>(refreshGuards);for(Script.Hook h:next.byId.values())if(h.kind.equals("refresh"))guards.put(h.id,h);
  if(guards.size()>128)throw new IllegalArgumentException("Retired refresh guard budget");
  Map<String,Script.Hook>physical=new TreeMap<>(next.byId);
  for(Script.Hook h:guards.values()){if(physical.containsKey(h.id)&&!physical.get(h.id).kind.equals("refresh"))throw new IllegalArgumentException("Refresh guard id cannot be reused");physical.putIfAbsent(h.id,h);}
  Script plan=new Script(next.version,next.sequence,physical);boolean shapeChanged=!oldPlan.shape.equals(plan.shape);
  if(shapeChanged){
   Set<String>owners=new HashSet<>(oldPlan.byClass.keySet());owners.addAll(plan.byClass.keySet());List<Class<?>>loaded=new ArrayList<>();
   for(Class<?>c:inst.getAllLoadedClasses())if(owners.contains(c.getName().replace('.','/'))){if(!inst.isModifiableClass(c))throw new IllegalArgumentException("Unmodifiable target "+c.getName());loaded.add(c);}
   transformer.errors.clear();transformer.plan=plan;
   try{
    if(!loaded.isEmpty())inst.retransformClasses(loaded.toArray(new Class<?>[0]));
    if(!transformer.errors.isEmpty())throw new IllegalArgumentException(String.join("; ",transformer.errors));
   }catch(Throwable failure){
    transformer.plan=oldPlan;transformer.errors.clear();try{if(!loaded.isEmpty())inst.retransformClasses(loaded.toArray(new Class<?>[0]));}catch(Throwable restore){Agent.log("RETRANSFORM_RECOVERY_FAILED_NEW_HANDLERS_DISABLED "+restore.getClass().getSimpleName());}
    if(failure instanceof Exception)throw (Exception)failure;throw new IllegalArgumentException("Transformer update rejected",failure);
   }
  }
  previous=old;current=next;refreshGuards=Map.copyOf(guards);epoch.incrementAndGet();bind();
  Agent.log("EXTENSION_GENERATION_ACTIVE version="+next.version+" sequence="+next.sequence+" hooks="+next.byId.size()+" retransform="+shapeChanged);
 }
 public synchronized String rollback()throws Exception{
  if(previous==null)throw new IllegalStateException("No prior generation");Script target=previous;byte[]b=previousBytes,s=previousSig;byte[]old=currentBytes,oldSig=currentSig;
  transition(target);currentBytes=b;currentSig=s;previousBytes=old;previousSig=oldSig;persist();return "ROLLED_BACK "+current.version;
 }
 public void checkRemoteNow(){remoteUpdate(controlEpoch);}
 public long transformCount(){return transformer.count.get();}
 public String status(){return "version="+current.version+" sequence="+current.sequence+" hooks="+current.byId.size()+" epoch="+epoch.get()+" wrap_hits="+layout.wrapHits.get()+" cache_refreshes="+layout.refreshes.get()+" transforms="+transformer.count.get()+" fail_open="+Dispatch.faults();}
 private void persist(){
  try{
   Path dir=store.resolve("rules");if(currentBytes!=null){String h=Verified.hash(currentBytes);Verified.atomic(dir.resolve(h+".json"),currentBytes);Verified.atomic(dir.resolve(h+".sig"),currentSig);Verified.atomic(dir.resolve("active.txt"),h.getBytes(StandardCharsets.US_ASCII));}
   else Files.deleteIfExists(dir.resolve("active.txt"));
   if(previousBytes!=null){String h=Verified.hash(previousBytes);Verified.atomic(dir.resolve(h+".json"),previousBytes);Verified.atomic(dir.resolve(h+".sig"),previousSig);Verified.atomic(dir.resolve("previous.txt"),h.getBytes(StandardCharsets.US_ASCII));}
   Verified.atomic(dir.resolve("sequence.txt"),Long.toString(highWater).getBytes(StandardCharsets.US_ASCII));Verified.atomic(store.resolve("status.txt"),status().getBytes(StandardCharsets.UTF_8));
   Set<String>keep=new HashSet<>();if(currentBytes!=null)keep.add(Verified.hash(currentBytes));if(previousBytes!=null)keep.add(Verified.hash(previousBytes));
   try(DirectoryStream<Path>files=Files.newDirectoryStream(dir)){
    for(Path file:files){String n=file.getFileName().toString();if(n.matches("[a-f0-9]{64}\\.(json|sig)")&&!keep.contains(n.substring(0,64))&&Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))try{Files.deleteIfExists(file);}catch(IOException ignored){}}
   }

  }catch(Exception e){Agent.log("STATE_PERSIST_FAILED_RUNNING_SNAPSHOT_RETAINED "+e.getClass().getSimpleName());}
 }
 private void restoreLastGood(){
  Path active=store.resolve("rules/active.txt");try{if(!Files.isRegularFile(active))return;String hash=Files.readString(active).trim();if(!hash.matches("[0-9a-f]{64}"))throw new SecurityException("Bad active pointer");byte[]b=Verified.read(active.resolveSibling(hash+".json"),Script.LIMIT),s=Verified.read(active.resolveSibling(hash+".sig"),4096);Verified.signature(b,s);Script script=Script.parse(b);if(!Verified.hash(b).equals(hash))throw new SecurityException("Bad local cache");transition(script);currentBytes=b;currentSig=s;
   try{Path prior=active.resolveSibling("previous.txt");if(Files.isRegularFile(prior)){String old=Files.readString(prior).trim();if(old.matches("[a-f0-9]{64}")&&!old.equals(hash)){byte[]pb=Verified.read(prior.resolveSibling(old+".json"),Script.LIMIT),ps=Verified.read(prior.resolveSibling(old+".sig"),4096);Verified.signature(pb,ps);if(!Verified.hash(pb).equals(old))throw new SecurityException("Bad recovery cache");previous=Script.parse(pb);previousBytes=pb;previousSig=ps;}}}catch(Exception recovery){Agent.log("PREVIOUS_RULE_CACHE_SKIPPED "+recovery.getClass().getSimpleName());}
   Agent.log("LAST_GOOD_RULES_RESTORED");}catch(Exception e){Agent.log("LAST_GOOD_RULES_UNAVAILABLE_KEEP_GAME "+e.getClass().getSimpleName());}
 }
 private void reloadDictionary(){
  try{
   Path local=root.resolve("SAO_TW_Dynamic/zh_tw.json");if(!Files.isRegularFile(local))return;long stamp=Files.getLastModifiedTime(local).toMillis();Path cloud=root.resolve("SAO_TW_Dynamic/github-cache/zh_tw.state.json");if(Files.isRegularFile(cloud))stamp+=Files.getLastModifiedTime(cloud).toMillis();Path overrides=root.resolve("SAO_TW_Dynamic/local-overrides.json");if(Files.isRegularFile(overrides))stamp+=Files.getLastModifiedTime(overrides).toMillis();if(stamp==dictStamp)return;
   Map<String,Map<String,String>>result=new HashMap<>();merge(result,Verified.read(local,8*1024*1024));
   if(Files.isRegularFile(cloud)){Map<?,?>state=Script.obj(MiniJson.parse(new String(Verified.read(cloud,65536),StandardCharsets.UTF_8)));String hash=String.valueOf(state.get("sha256"));if(hash.matches("[a-f0-9]{64}")){Path blob=cloud.resolveSibling("zh_tw."+hash+".json");byte[]bytes=Verified.read(blob,8*1024*1024);if(Verified.hash(bytes).equals(hash))merge(result,bytes);}}
   if(Files.isRegularFile(overrides))merge(result,Verified.read(overrides,2*1024*1024));Map<String,Map<String,String>>immutable=new HashMap<>();result.forEach((k,v)->immutable.put(k,Map.copyOf(v)));dictionary=Map.copyOf(immutable);dictStamp=stamp;epoch.incrementAndGet();
  }catch(Exception e){once("dict-failed","EXTERNAL_DICTIONARY_KEEP_PREVIOUS "+e.getClass().getSimpleName());}
 }
 private static void merge(Map<String,Map<String,String>>dst,byte[]bytes){Map<?,?>m=Script.obj(MiniJson.parse(new String(bytes,StandardCharsets.UTF_8)));for(String section:new String[]{"names","messages","lines","items"})if(m.get(section) instanceof Map){Map<String,String>out=dst.computeIfAbsent(section,k->new HashMap<>());for(Map.Entry<?,?>e:((Map<?,?>)m.get(section)).entrySet())if(e.getKey() instanceof String&&e.getValue() instanceof String&&((String)e.getKey()).length()<=8192&&((String)e.getValue()).length()<=16384)out.put((String)e.getKey(),(String)e.getValue());}}
 private void once(String key,String message){if(logged.size()<512&&logged.add(key))try{worker.execute(()->Agent.log(message));}catch(RejectedExecutionException ignored){}}
 private void restoreWatcherState(){
  try{Path p=store.resolve("watch-state.json");if(!Files.isRegularFile(p))return;Map<?,?>m=Script.obj(MiniJson.parse(new String(Verified.read(p,4096),StandardCharsets.UTF_8)));candidateStamp=((Number)m.get("candidate")).longValue();controlStamp=((Number)m.get("control")).longValue();updatesPaused=Boolean.TRUE.equals(m.get("paused"));}catch(Exception bad){updatesPaused=true;Agent.log("WATCH_STATE_INVALID_UPDATES_PAUSED");}
 }
 private void saveWatcherState(){
  try{Map<String,Object>m=new LinkedHashMap<>();m.put("candidate",candidateStamp);m.put("control",controlStamp);m.put("paused",updatesPaused);Verified.atomic(store.resolve("watch-state.json"),(MiniJson.stringify(m)+"\n").getBytes(StandardCharsets.UTF_8));}catch(Exception e){Agent.log("WATCH_STATE_SAVE_FAILED "+e.getClass().getSimpleName());}
 }
 private synchronized void poll(){
  try{
   reloadDictionary();Path candidate=store.resolve("rules/candidate.json"),sig=candidate.resolveSibling("candidate.sig");
   if(Files.isRegularFile(candidate)&&Files.isRegularFile(sig)){long stamp=Files.getLastModifiedTime(candidate).toMillis()+Files.getLastModifiedTime(sig).toMillis();if(stamp!=candidateStamp){candidateStamp=stamp;saveWatcherState();try{applySigned(Verified.read(candidate,Script.LIMIT),Verified.read(sig,4096));}catch(Exception bad){Agent.log("LOCAL_CANDIDATE_REJECTED_OLD_RULES_RETAINED "+bad.getClass().getSimpleName()+" "+bad.getMessage());}}}
   Path control=store.resolve("control.json");if(Files.isRegularFile(control)){long stamp=Files.getLastModifiedTime(control).toMillis();if(stamp!=controlStamp){controlStamp=stamp;Map<?,?>command=Script.obj(MiniJson.parse(new String(Verified.read(control,4096),StandardCharsets.UTF_8)));String action=String.valueOf(command.get("action"));if(action.equals("refresh")){controlEpoch++;updatesPaused=false;nextNetwork=0;dictStamp=-1;}else if(action.equals("rollback")){controlEpoch++;updatesPaused=true;rollback();nextNetwork=Long.MAX_VALUE;}else if(action.equals("disable")){controlEpoch++;updatesPaused=true;transition(Script.empty());currentBytes=null;currentSig=null;persist();nextNetwork=Long.MAX_VALUE;}saveWatcherState();}}
   if(!nativeTest&&!updatesPaused&&System.currentTimeMillis()>=nextNetwork&&fetching.compareAndSet(false,true)){nextNetwork=System.currentTimeMillis()+60000;final long token=controlEpoch;downloads.execute(()->{try{remoteUpdate(token);}finally{fetching.set(false);}});}
  }catch(Throwable problem){once("poll-"+problem.getClass().getSimpleName(),"EXTENSION_POLL_KEEP_CURRENT "+problem.getClass().getSimpleName());}
 }
 private void remoteUpdate(long token){
  try{
   String base="https://raw.githubusercontent.com/ga200604101111/SAOMCZH/main/extensions/v2/stable";
   byte[]body=Verified.fetch(base+".properties",65536),sig=Verified.fetch(base+".sig",4096);Map<String,String>m=Verified.descriptor(body,sig);
   String hash=m.get("sha256");if(hash==null||!hash.matches("[a-f0-9]{64}"))throw new SecurityException("Invalid remote rule digest");if(hash.equals(remoteDigest))return;
   int size=Integer.parseInt(m.get("bytes"));if(size<1||size>Script.LIMIT)throw new SecurityException("Rule size exceeds budget");byte[]rules=Verified.fetch(m.get("url"),size),signature=Verified.fetch(m.get("signature_url"),4096);
   if(rules.length!=size||!Verified.hash(rules).equals(hash))throw new SecurityException("Rule digest mismatch");synchronized(this){if(token!=controlEpoch)return;String status=applySigned(rules,signature);remoteDigest=hash;Agent.log("REMOTE_RULE_UPDATE "+status);}
  }catch(Exception error){once("net-"+error.getClass().getSimpleName(),"REMOTE_RULES_KEEP_LAST_GOOD "+error.getClass().getSimpleName());}
  finally{if(!updatesPaused&&token==controlEpoch&&System.currentTimeMillis()>=nextBinaryCheck){nextBinaryCheck=System.currentTimeMillis()+3600000;for(String kind:new String[]{"runtime","compatibility"})stageBinary(kind);}}
 }
 private void stageBinary(String kind){
  try{
   String base="https://raw.githubusercontent.com/ga200604101111/SAOMCZH/main/extensions/v1/"+kind;
   byte[]body=Verified.fetch(base+".properties",65536),sig=Verified.fetch(base+".sig",4096);Map<String,String>next=Verified.descriptor(body,sig);Path directory=store.resolve(kind);
   Path current=directory.resolve("current.properties"),currentSig=directory.resolve("current.sig");
   if(Files.isRegularFile(current)&&Files.isRegularFile(currentSig)){
    Map<String,String>old=Verified.descriptor(Verified.read(current,65536),Verified.read(currentSig,4096));
    if(Objects.equals(old.get("sha256"),next.get("sha256")))return;
    if(versionCompare(next.get("version"),old.get("version"))<=0)throw new SecurityException("Binary downgrade/reused version rejected");
   }
   Verified.module(directory,next);Verified.atomic(directory.resolve("pending.sig"),sig);Verified.atomic(directory.resolve("pending.properties"),body);
   Agent.log("EXTERNAL_BINARY_READY_NEXT_START kind="+kind+" version="+next.get("version"));
  }catch(Exception e){once("binary-"+kind+"-"+e.getClass().getSimpleName(),"EXTERNAL_BINARY_KEEP_LAST_GOOD "+kind+" "+e.getClass().getSimpleName());}
 }
 private static int versionCompare(String a,String b){
  if(a==null||b==null||!a.matches("[0-9]+\\.[0-9]+\\.[0-9]+")||!b.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IllegalArgumentException("Invalid binary version");
  String[]aa=a.split("\\."),bb=b.split("\\.");for(int i=0;i<3;i++){int n=Long.compare(Long.parseLong(aa[i]),Long.parseLong(bb[i]));if(n!=0)return n;}return 0;
 }
}
