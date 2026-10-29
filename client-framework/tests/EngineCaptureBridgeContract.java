package sao.zh.engine;
import java.nio.file.*;import java.util.*;import java.lang.reflect.*;import java.nio.charset.StandardCharsets;
/** Proves the real Engine translation path queues unknown domain text, and never records mapped text. */
public final class EngineCaptureBridgeContract {
 static int checks;static void yes(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 public static void main(String[]args)throws Exception{
  Path root=Files.createTempDirectory("sao-zh-bridge-");
  Constructor<Engine> ctor=Engine.class.getDeclaredConstructor(java.lang.instrument.Instrumentation.class,Path.class,Path.class,boolean.class);
  ctor.setAccessible(true);Engine runtime=ctor.newInstance(null,root,root.resolve("runtime.jar"),true);
  String json="{\"schema_version\":2,\"version\":\"1.6.0\",\"sequence\":108,\"expires\":2100000000,\"hooks\":[{\"id\":\"quests-objectives:title\",\"class\":\"fr/sao/questtracker/screen/QuestMenuScreen\",\"method\":\"heading\",\"descriptor\":\"(Ljava/lang/String;F)V\",\"kind\":\"argument\",\"argument\":0,\"locale\":\"zh_tw\",\"section\":\"names\",\"capture\":\"quest\"}]}";
  Script.Hook hook=Script.parse(json.getBytes(StandardCharsets.UTF_8)).byId.get("quests-objectives:title");
  Method translate=Engine.class.getDeclaredMethod("translate",Script.Hook.class,String.class);translate.setAccessible(true);
  Field dictionary=Engine.class.getDeclaredField("dictionary");dictionary.setAccessible(true);
  dictionary.set(runtime,Map.of("names",Map.of("Known Quest","已知任務")));
  yes("已知任務".equals(translate.invoke(runtime,hook,"Known Quest")),"Known translation lost");
  yes("The Chamber Beneath the Fields".equals(translate.invoke(runtime,hook,"The Chamber Beneath the Fields")),"Unknown text modified");
  yes("Untranslated quest reminder".equals(translate.invoke(runtime,hook,"Untranslated quest reminder")),"Unknown text changed");
  translate.invoke(runtime,hook,"YingHuang09");
  Field f=Engine.class.getDeclaredField("unknownCollector");f.setAccessible(true);
  UnknownCollector collector=(UnknownCollector)f.get(runtime);collector.close();
  Path out=root.resolve("SAO_TW_Dynamic/review/hook_missing_zh_tw.jsonl");
  yes(Files.exists(out),"Actual engine failed to capture");
  String content=Files.readString(out);
  yes(content.contains("Untranslated quest reminder"),"Missing quest original");
  yes(!content.contains("Known Quest"),"Known text should not be captured");
  yes(!content.contains("YingHuang09"),"Private name captured");
  yes(content.contains("\"origin\":\"hook.quest\""),"No source provenance");
  System.out.println("REAL_ENGINE_UNKNOWN_TEXT_CAPTURE_PASS checks="+checks);
 }
}
