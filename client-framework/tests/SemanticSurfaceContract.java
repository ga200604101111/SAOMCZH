package sao.zh.engine;
import java.util.*;
/** Pure, scope-qualified text tests; no Minecraft launch or live data. */
public final class SemanticSurfaceContract {
 static int checks,failures;
 static void eq(Object a,Object b,String label){checks++;if(!Objects.equals(a,b)){failures++;System.out.println("FAIL "+label+" expected="+b+" actual="+a);}}
 static Script.Hook hook(String id,String owner,String section){return new Script.Hook(Map.of("id",id,"class",owner,"method","display","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section",section));}
 static final Map<String,Map<String,String>> DICT=Map.of("names",Map.of("Mini Treant","迷你樹人","Treant","樹人","Local Lumberjack","當地伐木工","OAK STUMP","橡木樹樁","Marécage Putride","腐敗沼澤","Calme Mortel","致命沉靜"),"items",Map.of("Training Pavise","訓練用大盾","Spider String","蜘蛛絲"),"lines",Map.of("Se rendre là où résident les brigands","前往盜匪的居所"),"messages",Map.of("Calme Mortel déclenché !","致命沉靜已觸發！","You are at the spawn.","你已在重生點。"));
 static String call(Script.Hook h,String s){return RenderTextPipeline.resolve(h,s,DICT);}
 public static void main(String[]args){
  Script.Hook mob=hook("codex-creatures:mob-shown-name","fr/sao/saoui/mob/MobBarRenderer","names");
  Script.Hook item=hook("items-economy:custom-tooltip-name","fr/sao/tooltip/TipModel","items");
  Script.Hook compass=hook("quests-objectives:compass-top-line","fr/sao/questtracker/hud/QuestCompassRenderer","lines");
  Script.Hook holo=hook("worlds-locations:saojob-hologram-preformat","com/eighteen/saojob/hologram/HologramRenderer","lines");
  Script.Hook npc=hook("worlds-locations:npc-tagged-label","net/minecraft/class_897","names");
  Script.Hook system=hook("dynamic-translation:game-system-display","net/minecraft/class_7594","messages");
  Script.Hook generic=hook("ui-text-layout:canvas-mid-text","fr/sao/pass/screen/PassCanvas","messages");
  eq(call(mob,"Mini Treant Lv. 3"),"迷你樹人 Lv. 3","mob suffix");
  eq(call(mob,"§fMini Treant §eLv ° 3"),"§f迷你樹人 §eLv ° 3","mob colors/level");
  eq(call(mob,"Mini Treant [Lv. 9999+]"),"迷你樹人 [Lv. 9999+]","large level not separate entry");
  eq(call(mob,"[Lv. 12] Mini Treant"),"[Lv. 12] 迷你樹人","prefix level");
  eq(call(mob,"Mini Treant"),"迷你樹人","canonical name");
  eq(call(compass,"Se rendre là où résident les brigands | 2773m"),"前往盜匪的居所 | 2773m","quest distance");
  eq(call(compass,"§7Se rendre là où résident les brigands §e| 1.2km"),"§7前往盜匪的居所 §e| 1.2km","quest format");
  eq(call(compass,"Marécage Putride"),"腐敗沼澤","region shared names");
  eq(call(item,"§aTraining Pavise§r"),"§a訓練用大盾§r","item formatting");
  eq(call(item,"4x Training Pavise"),"4x 訓練用大盾","item count prefix");
  eq(call(item,"Training Pavise ×4"),"訓練用大盾 ×4","item count suffix");
  eq(call(item,"Training Pavise"),"訓練用大盾","item canonical");
  eq(call(holo,"§6OAK STUMP"),"§6橡木樹樁","harvest title");
  eq(call(holo,"OAK STUMP\nLv ° 1+"),"橡木樹樁\nLv ° 1+","harvest multiline");
  eq(call(npc,"§6[NPC] §fLocal Lumberjack"),"§6[NPC] §f當地伐木工","NPC prefix preserve");
  eq(call(npc,"Local Lumberjack"),"Local Lumberjack","untagged native name untouched");
  eq(call(system,"§cCalme Mortel déclenché !"),"§c致命沉靜已觸發！","system full event");
  eq(call(system,"✔ You are at the spawn."),"✔ 你已在重生點。","system tick icon");
  for(String privateLine:List.of("Player_One: Calme Mortel déclenché !","[Floor 1] User » You are at the spawn.","<Player> Training Pavise","User: Mini Treant Lv. 3"))eq(call(system,privateLine),privateLine,"player chat rejected");
  for(String name:List.of("Unknown Mob Lv. 3","Mini Treantor Lv. 3","Treant_123","https://example.invalid/Treant"))eq(call(mob,name),name,"unknown/partial unchanged");
  eq(call(generic,"Mini Treant Lv. 3"),"Mini Treant Lv. 3","no generic name replacement");
  eq(call(generic,"Marécage Putride"),"Marécage Putride","no cross-domain fallback");
  eq(call(compass,"Se rendre là où résident les brigands | User_123"),"Se rendre là où résident les brigands | User_123","non-numeric suffix unchanged");
  eq(call(item,"Training Pavise sword"),"Training Pavise sword","no substring matching");
  eq(SemanticText.captureKey(mob,"§fUnknown Beast §eLv ° 3"),"Unknown Beast","capture mob canonical");
  eq(SemanticText.captureKey(compass,"Unknown quest destination | 2773m"),"Unknown quest destination","capture distance not a dictionary key");
  eq(SemanticText.captureKey(item,"12x Unknown sword"),"Unknown sword","capture count not a dictionary key");
  eq(SemanticText.captureKey(compass,"Unknown quest | User_123"),"Unknown quest | User_123","capture does not strip arbitrary metadata");
  if(failures!=0)throw new AssertionError("Semantic span failures="+failures+"/"+checks);
  System.out.println("SEMANTIC_SURFACE_CONTRACT_PASS checks="+checks+" numbers_and_scopes_preserved=true");
 }
}
