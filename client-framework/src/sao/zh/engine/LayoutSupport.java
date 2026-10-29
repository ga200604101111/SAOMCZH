package sao.zh.engine;
import java.lang.ref.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.AtomicLong;
/** Shared external adapter. Only signed, bytecode-verified display cache fields are touched. */
final class LayoutSupport {
 private static final class Identity extends WeakReference<Object>{final int hash;Identity(Object o,ReferenceQueue<Object>q){super(o,q);hash=System.identityHashCode(o);}public int hashCode(){return hash;}public boolean equals(Object o){return this==o||o instanceof Identity&&get()!=null&&get()==((Identity)o).get();}}
 private record Stamp(long generation,String environment){}
 private record Key(String hook,Identity receiver,long generation,String environment,String context,String text,int size,boolean bold,int width,int limit){}
 private static final class Access {
  final Method measure;final List<Field>context=new ArrayList<>(),caches=new ArrayList<>();
  Access(Class<?>owner,Script.Hook hook)throws Exception{
   if(hook.kind.equals("wrap")){measure=owner.getMethod(hook.measure,String.class,float.class,boolean.class);if(measure.getReturnType()!=float.class||Modifier.isStatic(measure.getModifiers()))throw new IllegalArgumentException("Invalid measure method");}
   else measure=null;
   for(Map.Entry<String,String>e:hook.contextFields.entrySet())context.add(field(owner,e.getKey(),e.getValue(),false));
   for(Script.CacheField f:hook.cacheFields)caches.add(field(owner,f.name,f.descriptor,f.action.equals("reset_key")));
  }
  static Field field(Class<?>c,String name,String desc,boolean mutable)throws Exception{
   Field f=c.getDeclaredField(name);if(Modifier.isStatic(f.getModifiers())||mutable&&Modifier.isFinal(f.getModifiers())||!org.objectweb.asm.Type.getDescriptor(f.getType()).equals(desc))throw new IllegalArgumentException("Cache/context field type mismatch");f.setAccessible(true);return f;
  }
 }
 private final ClassValue<Map<String,Access>>access=new ClassValue<>(){protected Map<String,Access>computeValue(Class<?>c){return new HashMap<>();}};
 private final LinkedHashMap<Key,List<String>>memo=new LinkedHashMap<>(32,.75f,true);
 private final ReferenceQueue<Object>queue=new ReferenceQueue<>();private final LinkedHashMap<Identity,Map<String,Stamp>>seen=new LinkedHashMap<>();
 final AtomicLong wrapHits=new AtomicLong(),refreshes=new AtomicLong();
 private long memoGeneration=-1;
 private Access access(Class<?>owner,Script.Hook hook)throws Exception{Map<String,Access>m=access.get(owner);String shape=hook.shape();synchronized(m){Access a=m.get(shape);if(a==null){if(m.size()>=128)m.clear();a=new Access(owner,hook);m.put(shape,a);}return a;}}
 List<String> wrap(Script.Hook hook,Object receiver,Class<?>owner,String text,float size,boolean bold,float width,int limit,long generation)throws Exception{
  if(receiver==null||text==null||text.length()>8192||!Float.isFinite(size)||size<=0||size>1024)throw new IllegalArgumentException("Invalid wrap input");
  Access a=access(owner,hook);StringBuilder ctx=new StringBuilder();for(Field f:a.context){Object v=f.get(receiver);if(v instanceof String&&((String)v).length()>256)throw new IllegalArgumentException("Invalid font key");ctx.append(v).append('|');}
  Key key=new Key(hook.id,new Identity(receiver,null),generation,environment(owner),ctx.toString(),text,Float.floatToIntBits(size),bold,Float.floatToIntBits(width),limit);
  synchronized(memo){if(memoGeneration!=generation){memo.clear();memoGeneration=generation;}List<String>cached=memo.get(key);if(cached!=null){wrapHits.incrementAndGet();return new ArrayList<>(cached);}}
  List<String>result=CjkWrap.wrap(text,width,limit,part->{try{return ((Number)a.measure.invoke(receiver,part,size,bold)).doubleValue();}catch(InvocationTargetException e){if(e.getCause() instanceof VirtualMachineError)throw (VirtualMachineError)e.getCause();throw new IllegalArgumentException("Renderer measurement failed",e.getCause());}catch(ReflectiveOperationException e){throw new IllegalArgumentException("Renderer measurement unavailable",e);}});
  synchronized(memo){if(memoGeneration==generation){memo.put(key,result);while(memo.size()>128)memo.remove(memo.keySet().iterator().next());}}
  return new ArrayList<>(result);
 }
 void refresh(Script.Hook hook,Object receiver,Class<?>owner,long generation)throws Exception{
  if(receiver==null)return;Stamp next=new Stamp(generation,environment(owner));Identity lookup=new Identity(receiver,null);
  synchronized(seen){for(Reference<?>r;(r=queue.poll())!=null;)seen.remove(r);Map<String,Stamp>states=seen.get(lookup);if(states!=null&&next.equals(states.get(hook.id)))return;}
  Access a=access(owner,hook);List<Object>values=new ArrayList<>();
  for(int i=0;i<a.caches.size();i++){Object v=a.caches.get(i).get(receiver);Script.CacheField spec=hook.cacheFields.get(i);if(spec.action.equals("clear_map")&&v!=null){if(!(v instanceof Map)||((Map<?,?>)v).size()>65536||!v.getClass().getName().startsWith("java.util."))throw new IllegalArgumentException("Display cache guard failed");}values.add(v);}
  // Called from the instrumented render method itself, never the download worker.
  for(int i=0;i<a.caches.size();i++){Script.CacheField spec=hook.cacheFields.get(i);Object v=values.get(i);if(spec.action.equals("clear_map")){if(v!=null)((Map<?,?>)v).clear();}else a.caches.get(i).set(receiver,null);}
  synchronized(seen){Map<String,Stamp>states=seen.get(lookup);if(states==null){states=new HashMap<>();seen.put(new Identity(receiver,queue),states);}states.put(hook.id,next);/* World TextDisplays may outnumber open GUI objects: keep bounded weak per-entity stamps. */while(seen.size()>2048)seen.remove(seen.keySet().iterator().next());}
  refreshes.incrementAndGet();
 }
 private static final class Env {Method lang,font;Env(Class<?>o){try{lang=o.getClassLoader().loadClass("fr.sao.util.LanguageWatch").getMethod("current");}catch(Exception ignored){}try{font=o.getClassLoader().loadClass("fr.sao.font.FontChoice").getMethod("get");}catch(Exception ignored){}}}
 private static final ClassValue<Env>envs=new ClassValue<>(){protected Env computeValue(Class<?>c){return new Env(c);}};
 static String environment(Class<?>owner){if(owner==null)return "";Env e=envs.get(owner);try{return (e.lang==null?"":String.valueOf(e.lang.invoke(null)))+'|'+(e.font==null?"":String.valueOf(e.font.invoke(null)));}catch(Exception ex){return "unavailable";}}
}
