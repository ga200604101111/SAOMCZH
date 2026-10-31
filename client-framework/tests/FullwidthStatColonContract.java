package sao.zh.engine;
import java.util.*;
/** Reproduce a Chinese U+FF1A colon that arrives from native equipment labels. */
public final class FullwidthStatColonContract {
 private static int checks;
 static Script.Hook hook(String id,String owner,String section){
  return new Script.Hook(Map.of("id",id,"class",owner,"method","name",
    "descriptor","()Ljava/lang/String;","kind","return","section",section));
 }
 static void check(String label,String actual,String expected){
  checks++;
  if(!expected.equals(actual))throw new AssertionError(label+" expected="+expected+" actual="+actual);
 }
 public static void main(String[] args){
  Map<String,Map<String,String>> data=Map.of("names",Map.of(),"items",Map.of(),"lines",Map.of(),"messages",Map.of());
  var equipment=hook("items-economy:equipment-stat","fr/sao/equipment/EquipmentClient$Stat","names");
  var tooltip=hook("items-economy:tooltip-stat","fr/sao/tooltip/TipModel$Stat","items");
  var other=hook("mock:world","fr/sao/location/WorldName","names");
  var chat=hook("mock:chat","net/minecraft/class_338","messages");
  check("equipment-fullwidth",RenderTextPipeline.resolve(equipment,"攻擊傷害：7",data),"攻擊傷害: 7");
  check("equipment-between",RenderTextPipeline.resolve(equipment,"攻擊速度：1.2/s",data),"攻擊速度: 1.2/s");
  check("equipment-dodge",RenderTextPipeline.resolve(equipment,"閃避：+5%",data),"閃避: +5%");
  check("equipment-already-ascii",RenderTextPipeline.resolve(equipment,"吸血: +5%",data),"吸血: +5%");
  check("tooltip-fullwidth",RenderTextPipeline.resolve(tooltip,"移動速度：+15%",data),"移動速度: +15%");
  check("tooltip-prefixed-icon",RenderTextPipeline.resolve(tooltip,"\u1392 移動速度：+15%",data),"\u1392 移動速度: +15%");
  check("equipment-formatting",RenderTextPipeline.resolve(equipment,"§7攻擊傷害§f：§a7",data),"§7攻擊傷害§f:§a 7");
  check("equipment-keep-time",RenderTextPipeline.resolve(equipment,"12:30",data),"12:30");
  check("equipment-keep-level",RenderTextPipeline.resolve(equipment,"Lv. 3+",data),"Lv. 3+");
  check("unrelated-region",RenderTextPipeline.resolve(other,"地點：測試",data),"地點：測試");
  check("never-player-chat",RenderTextPipeline.resolve(chat,"玩家：你好",data),"玩家：你好");
  System.out.println("FULLWIDTH_STAT_COLON_PASS checks="+checks+" U+FF1A_to_U+003A=true scoped=true");
 }
}
