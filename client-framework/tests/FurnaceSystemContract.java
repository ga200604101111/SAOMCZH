package sao.zh.engine;
import java.util.*;
/** Screenshot and real log sources: translate only scoped system-rendered messages. */
public final class FurnaceSystemContract {
 private static int checks;
 private static final Map<String,Map<String,String>> DATA=Map.of(
   "messages",Map.ofEntries(
     Map.entry("Fuel ready! Right-click to start heating.","燃料已備妥！右鍵開始加熱。"),
     Map.entry("Heating started! Right-click again to stop at the right temperature.","開始加熱！在適當溫度時再次右鍵停止。"),
     Map.entry("Cooking started! Right-click to retrieve at the optimal moment.","開始加熱處理！在最佳時機右鍵取出。"),
     Map.entry("Quenching in progress...","淬火進行中……"),
     Map.entry("Process complete! Right-click to retrieve the item.","處理完成！右鍵取出物品。"),
     Map.entry("Hey! Sorry, but you can't change that here.","嘿！抱歉，你不能在這裡更改。")
   ),
   "items",Map.of("Coal Ore","煤礦石","Creuset de Forge","鍛造坩堝","Lingot de Fer Chaud","灼熱鐵錠","Minerai de Fer","鐵礦石"),
   "names",Map.of("Eugeo","尤吉歐","Cardinal Relay","卡迪納爾中繼裝置"),
   "lines",Map.of()
 );
 private static Script.Hook h(String id,String owner,String section){
   return new Script.Hook(Map.of("id",id,"class",owner,"method","display","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section",section));
 }
 private static void check(String label,String actual,String expected){
   checks++;if(!Objects.equals(expected,actual))throw new AssertionError(label+" expected="+expected+" actual="+actual);
 }
 public static void main(String[] args){
  Script.Hook system=h("dynamic-translation:game-system-display","net/minecraft/class_7594","messages");
  Script.Hook generic=h("ui-text-layout:canvas-mid-text","fr/sao/pass/screen/PassCanvas","messages");
  Script.Hook world=h("worlds-locations:saojob-hologram-preformat","com/eighteen/saojob/hologram/HologramRenderer","lines");
  for(var e:DATA.get("messages").entrySet())check("fixed "+e.getKey(),RenderTextPipeline.resolve(system,e.getKey(),DATA),e.getValue());
  check("degrees",RenderTextPipeline.resolve(system,"Heating stopped at 108°C. Right-click with the item to insert it.",DATA),"已在 108°C 停止加熱。手持物品右鍵放入。");
  check("degrees variable",RenderTextPipeline.resolve(system,"Heating stopped at 24°C. Right-click with the item to insert it.",DATA),"已在 24°C 停止加熱。手持物品右鍵放入。");
  check("degrees colored",RenderTextPipeline.resolve(system,"§6Heating stopped at §e108°C.§7 Right-click with the item to insert it.",DATA),"§6已在 §e108°C§7 停止加熱。手持物品右鍵放入。");
  check("insert",RenderTextPipeline.resolve(system,"Item inserted: 1x Creuset de Forge",DATA),"已放入: 1x 鍛造坩堝");
  check("add",RenderTextPipeline.resolve(system,"You added 4x Lingot de Fer Chaud",DATA),"已加入 4x 灼熱鐵錠");
  check("add unknown",RenderTextPipeline.resolve(system,"You added 1x New Forged Sword",DATA),"已加入 1x New Forged Sword");
  check("receive",RenderTextPipeline.resolve(system,"You received  Coal Ore",DATA),"已獲得  煤礦石");
  check("receive localized",RenderTextPipeline.resolve(system,"You received  煤礦石",DATA),"已獲得  煤礦石");
  check("receive colored",RenderTextPipeline.resolve(system,"§aYou received  §7Coal Ore",DATA),"§a已獲得  §7煤礦石");
  check("receive icon",RenderTextPipeline.resolve(system,"ⓘ You received  Coal Ore",DATA),"ⓘ 已獲得  煤礦石");
  check("furnace score",RenderTextPipeline.resolve(system,"Furnace — temperature 100 pts | time 100 pts | precision 100%",DATA),"熔爐 — 溫度 100 分 | 時間 100 分 | 精準度 100%");
  check("furnace score variant",RenderTextPipeline.resolve(system,"Furnace — temperature 88 pts | time 100 pts | precision 94%",DATA),"熔爐 — 溫度 88 分 | 時間 100 分 | 精準度 94%");
  check("furnace score colored",RenderTextPipeline.resolve(system,"§7Furnace — temperature §a100 pts §7| time §a100 pts §7| precision §a100%",DATA),"§7熔爐 — 溫度 §a100 分 §7| 時間 §a100 分 §7| 精準度 §a100%");
  check("received prefix leaf",RenderTextPipeline.resolve(system,"§aYou received  ",DATA),"§a已獲得  ");
  check("received item leaf",RenderTextPipeline.resolve(system,"§7Coal Ore",DATA),"§7煤礦石");
  check("count prefix leaf",RenderTextPipeline.resolve(system,"§aYou added 4x ",DATA),"§a已加入 4x ");
  check("furnace labels leaf",RenderTextPipeline.resolve(system,"§7Furnace — temperature ",DATA),"§7熔爐 — 溫度 ");
  check("furnace labels 2 leaf",RenderTextPipeline.resolve(system,"§7 pts | time ",DATA),"§7 分 | 時間 ");
  check("furnace labels 3 leaf",RenderTextPipeline.resolve(system,"§7 pts | precision ",DATA),"§7 分 | 精準度 ");
  check("Hey prefix leaf",RenderTextPipeline.resolve(system,"§cHey!",DATA),"§c嘿！");
  check("Hey body leaf",RenderTextPipeline.resolve(system,"§7 Sorry, but you can't change that here.",DATA),"§7 抱歉，你不能在這裡更改。");
  check("whole source accepted",String.valueOf(SemanticText.permitTree(system,"§aYou received  §7Coal Ore",DATA)),"true");
  check("player chat source rejected",String.valueOf(SemanticText.permitTree(system,"Player_One: You added 1x Lingot de Fer Chaud",DATA)),"false");
  check("player chat",RenderTextPipeline.resolve(system,"Player_One: You added 1x Lingot de Fer Chaud",DATA),"Player_One: You added 1x Lingot de Fer Chaud");
  check("private chat",RenderTextPipeline.resolve(system,"[Floor 1] Player_One » Fuel ready! Right-click to start heating.",DATA),"[Floor 1] Player_One » Fuel ready! Right-click to start heating.");
  check("generic unaffected",RenderTextPipeline.resolve(generic,"You received  Coal Ore",DATA),"You received  Coal Ore");
  check("world NPC name",RenderTextPipeline.resolve(world,"Eugeo",DATA),"尤吉歐");
  check("world waypoint",RenderTextPipeline.resolve(world,"Cardinal Relay",DATA),"卡迪納爾中繼裝置");
  check("empty unnamed",RenderTextPipeline.resolve(world," ",DATA)," ");
  check("blank name",RenderTextPipeline.resolve(world,"Unnamed_123",DATA),"Unnamed_123");
  check("invalid quantity",RenderTextPipeline.resolve(system,"You added 0x Lingot de Fer Chaud",DATA),"You added 0x Lingot de Fer Chaud");
  check("wrong syntax",RenderTextPipeline.resolve(system,"Heating stopped at somewhere. Right-click with the item to insert it.",DATA),"Heating stopped at somewhere. Right-click with the item to insert it.");
  System.out.println("FURNACE_SYSTEM_CONTRACT_PASS checks="+checks+" scoped=true counts=true colors=true");
 }
}
