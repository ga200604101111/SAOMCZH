package fixture;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import java.nio.charset.StandardCharsets;
/** Actual signed V16 runtime capture path using real SAOUI hook metadata, no GL or game instance. */
public final class NativeCaptureProbe {
 static int checks;
 static void ok(boolean yes,String msg){checks++;if(!yes)throw new AssertionError(msg);}
 public static void verify()throws Exception{
  Class<?> agent=Class.forName("sao.zh.boot.Agent");
  Object engine=agent.getMethod("runtime").invoke(null);
  Path root=(Path)agent.getMethod("root").invoke(null);
  Class<?> engineClass=engine.getClass();
  Field current=engineClass.getDeclaredField("current");current.setAccessible(true);
  Object script=current.get(engine);
  Field lookup=script.getClass().getDeclaredField("byId");lookup.setAccessible(true);
  Map<?,?> hooks=(Map<?,?>)lookup.get(script);
  Object hook=hooks.get("quests-objectives:quest-row-title");
  ok(hook!=null,"Quest Hook was not active");
  Field family=hook.getClass().getDeclaredField("capture");family.setAccessible(true);
  ok("quest".equals(family.get(hook)),"Signed capture metadata missing");
  Method tr=engineClass.getDeclaredMethod("translate",hook.getClass(),String.class);tr.setAccessible(true);
  String phrase="An unheard challenge beneath the stone cavern.";
  ok(phrase.equals(tr.invoke(engine,hook,phrase)),"Untranslated text was modified");
  tr.invoke(engine,hook,phrase); // de-dup capture
  tr.invoke(engine,hook,"Example_123: hello"); // explicit privacy gate
  tr.invoke(engine,hook,"YingHuang09"); // player identity gate
  Field collector=engineClass.getDeclaredField("unknownCollector");collector.setAccessible(true);
  Object writer=collector.get(engine);
  Method close=writer.getClass().getMethod("close");close.setAccessible(true);close.invoke(writer);
  Path file=root.resolve("SAO_TW_Dynamic/review/hook_missing_zh_tw.jsonl");
  ok(Files.exists(file),"No local missing text artifact from real runtime");
  String actual=Files.readString(file,StandardCharsets.UTF_8);
  ok(actual.contains(phrase),"Quest original absent");
  ok(actual.contains("\"origin\":\"hook.quest\""),"Source family missing");
  ok(!actual.contains("Example_123: hello")&&!actual.contains("YingHuang09"),"Private user text leaked");
  ok(actual.lines().filter(line->line.contains(phrase)).count()==1,"Duplicate captured despite session de-dup");
  System.out.println("NATIVE_CAPTURE_HOOK_PRIVACY_AND_DEDUP_PASS checks="+checks+" no_GL=true");
 }
}
