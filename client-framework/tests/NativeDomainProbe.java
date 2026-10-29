package fixture;
import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
public final class NativeDomainProbe {
 public static void verify()throws Exception{
  Path f=Paths.get(System.getProperty("sao.zh.domain.fixtures"));
  String[] targets={"fr.sao.translate.TranslateClient","fr.sao.itemlang.ItemLangClient","fr.sao.daily.DailyModel$Reward","fr.sao.equipment.EquipmentClient$Stat","fr.sao.interlink.InterlinkScreen","fr.sao.questtracker.screen.QuestMenuScreen","fr.sao.questtracker.hud.QuestTrackerRenderer","fr.sao.codex.CodexScreen","com.eighteen.saojob.skills.SkillDetailPopup","fr.sao.teleport.TeleportScreen"};
  ClassLoader loader=NativeDomainProbe.class.getClassLoader();List<Class<?>>loaded=new ArrayList<>();
  for(String name:targets){Class<?> c=Class.forName(name,false,loader);c.getDeclaredMethods();loaded.add(c);}
  Object engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);Method apply=engine.getClass().getMethod("applySigned",byte[].class,byte[].class);
  Object state=apply.invoke(engine,Files.readAllBytes(f.resolve("rules-1.1.0.json")),Files.readAllBytes(f.resolve("rules-1.1.0.sig")));
  if(!state.toString().startsWith("APPLIED"))throw new AssertionError("Production modules not applied");
  for(Class<?> c:loaded)c.getDeclaredMethods();
  Class<?>items=Class.forName("fr.sao.itemlang.ItemLangClient");
  if(items.getMethod("plain",String.class).invoke(null,(Object)null)!=null)throw new AssertionError("Null item changed");
  if(items.getMethod("translate",String.class).invoke(null,(Object)null)!=null)throw new AssertionError("Null item translation changed");
  System.out.println("REAL_SAO_DOMAIN_MODULES_38_HOOKS_10_ALREADY_LOADED_CLASSES_PASS "+state);
  apply.invoke(engine,Files.readAllBytes(f.resolve("native-remove.json")),Files.readAllBytes(f.resolve("native-remove.sig")));
  if(items.getMethod("plain",String.class).invoke(null,(Object)null)!=null)throw new AssertionError("Recovery null changed");
  System.out.println("REAL_SAO_DOMAIN_MODULES_REMOVE_KEEP_COMPATIBILITY_PASS");
 }
}
