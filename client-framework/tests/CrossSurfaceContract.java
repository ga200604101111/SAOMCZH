package sao.zh.engine;
import java.util.*;
/** Regression for the actual SAOUI native level-marker grammar, not a guessed screenshot string. */
public final class CrossSurfaceContract {
 static int count;
 static Script.Hook h(String id,String owner,String section){return new Script.Hook(Map.of("id",id,"class",owner,"method","display","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section",section));}
 static void equal(Object actual,Object expected){count++;if(!Objects.equals(actual,expected))throw new AssertionError("expected="+expected+" actual="+actual);}
 public static void main(String[]args){
  var d=Map.of("names",Map.of("Mini Treant","迷你樹人","Hanaka","哈納卡"),"items",Map.of("Copper Katana","銅製武士刀"),"lines",Map.of("Se rendre là où résident les brigands","前往盜匪的居所"));
  var mob=h("codex-creatures:mob-shown-name","fr/sao/saoui/mob/MobBarRenderer","names");
  var world=h("worlds-locations:text-display-before-native-wrap","net/minecraft/class_8138$class_8141","lines");
  for(var hook:List.of(mob,world)){
   for(String[]p:List.of(new String[]{"Mini Treant ✦ Lv.3 »","迷你樹人 ✦ Lv.3 »"},new String[]{"✦ Lv.3 » Mini Treant","✦ Lv.3 » 迷你樹人"},new String[]{"§fMini Treant §e✦ Lv.3 »","§f迷你樹人 §e✦ Lv.3 »"},new String[]{"Mini Treant [Lv. 3+]","迷你樹人 [Lv. 3+]"},new String[]{"Mini Treant Lv ° 3","迷你樹人 Lv ° 3"})){
    equal(RenderTextPipeline.resolve(hook,p[0],d),p[1]);
    equal(SemanticText.captureKey(hook,p[0]),"Mini Treant");
   }
  }
  equal(RenderTextPipeline.resolve(mob,"Player_One » Mini Treant",d),"Player_One » Mini Treant");
  var system=h("dynamic-translation:game-system-display","net/minecraft/class_7594","messages");
  equal(RenderTextPipeline.resolve(system,"Player_One » Mini Treant ✦ Lv.3 »",d),"Player_One » Mini Treant ✦ Lv.3 »");
  var item=h("items-economy:tooltip-name","fr/sao/tooltip/TipModel","items");
  equal(RenderTextPipeline.resolve(item,"Copper Katana",d),"銅製武士刀");
  equal(RenderTextPipeline.resolve(world,"Copper Katana",d),"銅製武士刀");
  var hud=h("quests-objectives:compass-flat-info","fr/sao/questtracker/hud/QuestCompassRenderer","lines");
  equal(RenderTextPipeline.resolve(hud,"Hanaka",d),"哈納卡");
  equal(RenderTextPipeline.resolve(hud,"Se rendre là où résident les brigands | 2718m",d),"前往盜匪的居所 | 2718m");
  for(String s:List.of("Lv. 3+","LV. 1","✦ Lv.3 »","[Lv. 3]"))equal(RenderTextPipeline.resolve(mob,s,d),s);
  System.out.println("CROSS_SURFACE_CONTRACT_PASS checks="+count+" native_level_preserved=true player_chat_unchanged=true shared=true");
 }
}
