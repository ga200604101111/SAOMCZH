package sao.zh.engine;
import java.lang.reflect.*;import java.lang.instrument.Instrumentation;import java.nio.file.*;import java.util.*;
public final class ItemCaptureContract {
 public static void main(String[]args)throws Exception{
  Path dir=Files.createTempDirectory("sao-item-capture-");
  Constructor<Engine> c=Engine.class.getDeclaredConstructor(Instrumentation.class,Path.class,Path.class,boolean.class);c.setAccessible(true);Engine engine=c.newInstance(null,dir,null,true);
  Field f=Engine.class.getDeclaredField("dictionary");f.setAccessible(true);f.set(engine,Map.of("items",Map.of("Mana Shard","魔力碎片")));
  Method translate=Engine.class.getDeclaredMethod("translate",Script.Hook.class,String.class);translate.setAccessible(true);
  Script.Hook h=Script.parse(Files.readAllBytes(Path.of(args[0]))).byId.get("items-economy:reward-name");
  Field cf=Engine.class.getDeclaredField("unknownCollector");cf.setAccessible(true);UnknownCollector collector=(UnknownCollector)cf.get(engine);
  try{
   Object a=translate.invoke(engine,h,"New Unknown Reward x2");if(!"New Unknown Reward".equals(a))throw new AssertionError("Unknown count title not separated");
   if(collector.accepted()!=1)throw new AssertionError("Cleaned unknown title was not collected");
   Field tf=Engine.class.getDeclaredField("translationMemo");tf.setAccessible(true);Object memo=((ThreadLocal<?>)tf.get(engine)).get();Field hf=memo.getClass().getDeclaredField("hooks");hf.setAccessible(true);
   for(Object value:((Map<?,?>)hf.get(memo)).values())if(((Map<?,?>)value).containsKey("New Unknown Reward x2"))throw new AssertionError("Untranslated count cleanup must not become a positive translation cache hit");
   if(!"魔力碎片".equals(translate.invoke(engine,h,"Mana Shard x2")))throw new AssertionError("Known title failed");
  }finally{collector.close();}
  Path log=dir.resolve("SAO_TW_Dynamic/review/hook_missing_zh_tw.jsonl");String text=Files.readString(log);
  if(!text.contains("New Unknown Reward")||text.contains("New Unknown Reward x2"))throw new AssertionError("Raw count leaked into capture key");
  try(var files=Files.walk(dir)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
  System.out.println("ITEM_CAPTURE_CONTRACT_PASS canonical_name=true bounded_queue_retry=true no_duplicate_count_key=true");
 }
}
