package sao.zh.engine;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import net.minecraft.*;
public final class RenderFastPathProbe {
 private static int checks;
 private static void ok(boolean b,String what){checks++;if(!b)throw new AssertionError(what);}
 private static Object hook(String id,String section)throws Exception{
  Class<?> c=Class.forName("sao.zh.engine.Script$Hook");
  Constructor<?> con=c.getDeclaredConstructor(Map.class);con.setAccessible(true);
  Map<String,Object> m=new LinkedHashMap<>();
  m.put("id",id);m.put("class","fixture/Renderer");m.put("method","getLabel");
  m.put("descriptor","()Ljava/lang/String;");m.put("kind","return");m.put("section",section);
  return con.newInstance(m);
 }
 private static Object translate(Object engine,Object hook,String raw)throws Exception{
  Class<?> h=Class.forName("sao.zh.engine.Script$Hook");
  Method m=Engine.class.getDeclaredMethod("translate",h,String.class);m.setAccessible(true);return m.invoke(engine,hook,raw);
 }
 public static void main(String[] args)throws Exception{
  ok(TextProjection.locale(RenderFastPathProbe.class,"zh_tw"),"locale gate");
  ok(!TextProjection.locale(RenderFastPathProbe.class,"en_us"),"wrong locale");
  class_5250 root=new class_5250(new Object());
  class_5250 initial=class_2561.method_43470("名稱：");
  class_5250 item=class_2561.method_43470("Healing Shard");
  class_2583 gold=new class_2583(0xffb800);
  item.method_10862(gold);root.method_10852(initial).method_10852(item);
  String before=root.getString();Function<String,String> identity=s->s;
  Object noChange=TextProjection.apply(root,RenderFastPathProbe.class,identity);
  ok(noChange==root,"unchanged Text must preserve identity");
  Object out=TextProjection.apply(root,RenderFastPathProbe.class,s->s.equals("Healing Shard")?"治療碎片":s);
  ok(out!=root,"translated Text must clone");
  class_5250 localized=(class_5250)out;
  ok(localized.getString().equals("名稱：治療碎片"),"no siblings duplicated");
  ok(root.getString().equals(before),"original Text mutated");
  ok(localized.method_10855().size()==2,"siblings count changed");
  ok(localized.method_10855().get(0)==initial,"unchanged first sibling copied");
  ok(localized.method_10855().get(1)!=item,"changed second sibling not copied");
  ok(localized.method_10855().get(1).method_10866()==gold,"color/style lost");
  class_5250 plain=class_2561.method_43470("Healing Shard");plain.method_10862(gold);
  Object copy=TextProjection.apply(plain,RenderFastPathProbe.class,s->"Healing Shard".equals(s)?"治療碎片":s);
  ok(((class_5250)copy).getString().equals("治療碎片"),"plain leaf not translated");
  ok(((class_5250)copy).method_10866()==gold,"plain leaf style lost");
  Constructor<Engine> constructor=Engine.class.getDeclaredConstructor(Instrumentation.class,Path.class,Path.class,boolean.class);
  constructor.setAccessible(true);Path temp=Files.createTempDirectory("saozh-fastpath-");
  Engine runtime=constructor.newInstance(null,temp,null,true);
  Field dictionary=Engine.class.getDeclaredField("dictionary");dictionary.setAccessible(true);
  Field epoch=Engine.class.getDeclaredField("epoch");epoch.setAccessible(true);
  AtomicLong gen=(AtomicLong)epoch.get(runtime);
  Object h=hook("source:one","lines"),other=hook("source:two","messages");
  dictionary.set(runtime,Map.of("lines",Map.of("Quest title","任務標題"),"messages",Map.of("Quest title","系統標題")));
  ok("任務標題".equals(translate(runtime,h,"Quest title")),"dictionary lookup");
  ok("任務標題".equals(translate(runtime,h,"Quest title")),"repeated lookup");
  ok("系統標題".equals(translate(runtime,other,"Quest title")),"hook collision");
  dictionary.set(runtime,Map.of("lines",Map.of("Quest title","更新標題"),"messages",Map.of("Quest title","系統標題")));
  gen.incrementAndGet();
  ok("更新標題".equals(translate(runtime,h,"Quest title")),"cache not invalidated after epoch");
  ok("untranslated".equals(translate(runtime,h,"untranslated")),"unknown string changed");
  dictionary.set(runtime,Map.of("lines",Map.of("untranslated","已補譯"),"messages",Map.of()));
  gen.incrementAndGet();
  ok("已補譯".equals(translate(runtime,h,"untranslated")),"negative memo survived epoch");
  Class<?> cls;
  try{Engine.class.getDeclaredField("translationMemo");cls=Class.forName("sao.zh.engine.Engine$TranslationMemo");}
  catch(NoSuchFieldException absent){cls=null;}
  if(cls!=null){
   Constructor<?> ctor=cls.getDeclaredConstructor();ctor.setAccessible(true);Object memo=ctor.newInstance();
   Class<?> hc=Class.forName("sao.zh.engine.Script$Hook");
   Method put=cls.getDeclaredMethod("put",hc,String.class,String.class,long.class);
   Method get=cls.getDeclaredMethod("get",hc,String.class,long.class);
   put.setAccessible(true);get.setAccessible(true);
   for(int i=0;i<70;i++)put.invoke(memo,h,"key-"+i,"value-"+i,7L);
   ok(get.invoke(memo,h,"key-0",7L)==null,"memo does not evict old entries");
   ok("value-69".equals(get.invoke(memo,h,"key-69",7L)),"memo evicted recent entry");
   ok(get.invoke(memo,h,"key-69",8L)==null,"memo did not invalidate generation");
  }
  Field collector=Engine.class.getDeclaredField("unknownCollector");collector.setAccessible(true);
  ((UnknownCollector)collector.get(runtime)).close();
  for(int i=0;i<25000;i++)TextProjection.apply(root,RenderFastPathProbe.class,identity);
  int N=120000;long nanos=System.nanoTime();long allocBefore=-1;com.sun.management.ThreadMXBean bean=null;
  try{bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
   if(bean.isThreadAllocatedMemorySupported()){bean.setThreadAllocatedMemoryEnabled(true);allocBefore=bean.getThreadAllocatedBytes(Thread.currentThread().getId());}
  }catch(Exception ignored){allocBefore=-1;}
  for(int i=0;i<N;i++){
   if(TextProjection.apply(root,RenderFastPathProbe.class,identity)!=root)throw new AssertionError("warm pass unexpectedly cloned");
  }
  long elapsed=System.nanoTime()-nanos;
  long allocAfter=bean!=null&&allocBefore>=0?bean.getThreadAllocatedBytes(Thread.currentThread().getId()):-1;
  System.out.printf(Locale.ROOT,"RENDER_FASTPATH_MOCK_PASS checks=%d calls=%d elapsed_ms=%.2f allocated_bytes_per_call=%.2f optimized=%s%n",
   checks,N,elapsed/1e6,allocBefore>=0?(allocAfter-allocBefore)/(double)N:-1,cls!=null);
 }
}
