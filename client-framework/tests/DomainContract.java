package fixture;
import java.lang.reflect.*;import java.nio.file.*;
/** Multiple UI consumers per domain, created before any hook is installed. */
public final class DomainContract {
 public static final class Items {
  public String raw="Stamina Shard";public String search="my typed auction search";public int page=4;public Object dragging=new Object();
  public String tooltip(){return raw;}public String dailyReward(){return raw;}
 }
 public static final class Quests {
  public String raw="Explore the tunnels";public String input="quest search";public int scroll=88;public Object selected=new Object();
  public String journal(){return raw;}public String trackingHud(){return raw;}
 }
 static Object engine;static int checks;
 static void ok(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 static Object call(String method,Class<?>[]types,Object...args)throws Exception{
  try{return engine.getClass().getMethod(method,types).invoke(engine,args);}
  catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}
 }
 static void apply(Path f,String id)throws Exception{call("applySigned",new Class<?>[]{byte[].class,byte[].class},Files.readAllBytes(f.resolve(id+".json")),Files.readAllBytes(f.resolve(id+".sig")));}
 public static void main(String[]args)throws Exception{
  Items items=new Items();Quests quests=new Quests();Object drag=items.dragging,selected=quests.selected;
  ok(items.tooltip().equals("Stamina Shard")&&quests.journal().equals("Explore the tunnels"),"Start with no hooks");
  engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);ok(engine!=null,"External engine unavailable");Path f=Paths.get(args[0]);
  apply(f,"modules-a");
  ok(items.tooltip().equals("耐力碎片")&&items.dailyReward().equals("耐力碎片"),"One item module must translate multiple UIs");
  ok(quests.journal().equals("探索地道")&&quests.trackingHud().equals("探索地道"),"One quest module must translate journal and HUD");
  ok(items.raw.equals("Stamina Shard")&&quests.raw.equals("Explore the tunnels"),"Original models must not be mutated");
  long count=((Number)call("transformCount",new Class<?>[0])).longValue();
  apply(f,"modules-b");
  ok(items.tooltip().equals("耐力碎片・修訂")&&items.dailyReward().equals("耐力碎片・修訂"),"Module A update invisible");
  ok(quests.journal().equals("探索地道")&&quests.trackingHud().equals("探索地道"),"Module B changed with unrelated A update");
  ok(((Number)call("transformCount",new Class<?>[0])).longValue()==count,"Text-only module update retransformed bytecode");
  apply(f,"modules-c");
  ok(items.tooltip().equals("耐力碎片"),"Module A selective rollback failed");
  ok(quests.journal().equals("深入探索地道"),"Rollback A lost independently updated B");
  try{apply(f,"modules-badsite");throw new AssertionError("Unknown method accepted");}catch(IllegalArgumentException expected){checks++;}
  ok(items.tooltip().equals("耐力碎片")&&quests.journal().equals("深入探索地道"),"Failed update damaged known good snapshot");
  apply(f,"modules-items-disabled");
  ok(items.tooltip().equals("Stamina Shard")&&items.dailyReward().equals("Stamina Shard"),"Disabled item module did not restore original");
  ok(quests.journal().equals("深入探索地道"),"Disable item module disabled quest module");
  call("rollback",new Class<?>[0]);
  ok(items.tooltip().equals("耐力碎片")&&quests.trackingHud().equals("深入探索地道"),"Whole-generation recovery failed");
  ok(items.search.equals("my typed auction search")&&items.page==4&&items.dragging==drag,"Item UI interaction changed");
  ok(quests.input.equals("quest search")&&quests.scroll==88&&quests.selected==selected,"Quest UI interaction changed");
  System.out.println("DOMAIN_MODULE_HOT_UPDATE_SELECTIVE_ROLLBACK_PASS checks="+checks+" pid="+ProcessHandle.current().pid()+" one_jvm=true one_kernel=true original_models_untouched=true");
 }
}
