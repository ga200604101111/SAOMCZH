package sao.zh.engine;
import java.util.*;

/** Client display-only regressions for actual screenshot text and shared glossary. */
public final class CrossSurfaceTextContract {
 private static int count;
 private static void equal(String label,Object got,Object expected){
  count++;if(!Objects.equals(got,expected))
   throw new AssertionError(label+" expected="+expected+" got="+got);
 }
 private static Script.Hook hook(String id,String owner,String method,String descriptor,String kind,String section,int argument){
  Map<String,Object> attrs=new LinkedHashMap<>();
  attrs.put("id",id);attrs.put("class",owner);attrs.put("method",method);
  attrs.put("descriptor",descriptor);attrs.put("kind",kind);
  attrs.put("section",section);
  if(kind.equals("argument"))attrs.put("argument",argument);
  if(kind.equals("field")&&id.equals("named-container:visible-title")){
   attrs.put("member_class","net/minecraft/class_465");
   attrs.put("member","field_22785");
   attrs.put("member_descriptor","Lnet/minecraft/class_2561;");
  }
  if(kind.equals("field")&&id.equals("items-economy:detail-visible-title")){
   attrs.put("member_class","fr/sao/iteminfo/ItemInfoOverlay$Layout");
   attrs.put("member","upperName");
   attrs.put("member_descriptor","Ljava/lang/String;");
  }
  return new Script.Hook(attrs);
 }
 private static final Script.Hook SYSTEM=hook("dynamic-translation:game-actionbar-display",
  "net/minecraft/class_329","method_1758","(Lnet/minecraft/class_2561;Z)V","argument","messages",0);
 private static final Script.Hook CHAT=hook("dynamic-translation:game-system-display",
  "net/minecraft/class_7594","method_44736","(Lnet/minecraft/class_2561;Z)V","system_text","messages",-1);
 private static final Script.Hook ITEM=hook("items-economy:tooltip-display",
  "fr/sao/tooltip/ItemTip","text","(Ljava/lang/String;)V","argument","items",0);
 private static final Script.Hook UPPER=hook("items-economy:detail-visible-title",
  "fr/sao/iteminfo/ItemInfoOverlay","render","(Lnet/minecraft/class_332;II)V","field","items",-1);
 private static final Script.Hook TITLE=hook("named-container:visible-title",
  "net/minecraft/class_465","method_2388","(Lnet/minecraft/class_332;II)V","field","names",-1);
 private static final Map<String,Map<String,String>> DATA=Map.of(
  "items",Map.of("Iron Axe","鐵斧","IRON AXE","鐵斧","Tactical Boots","戰術長靴",
     "Coal Ore","煤礦石","Katana du Chasseur","獵人武士刀","Lingot de Fer Chaud","灼熱鐵錠",
     "Anvil - Place item","鐵砧 - 放置物品","Anvil - Done","鐵砧 - 完成",
     "Sneak + right-click the station with an empty hand to cancel the craft.","空手潛行並右鍵點擊工作站，即可取消製作。"),
  "names",Map.of("Forge - Categories","鍛造 - 分類",
     "Forge — Categories","鍛造 - 分類","Forge – Categories","鍛造 - 分類"),
  "messages",Map.of("Craft fully complete!","製作已全部完成！",
     "Wrong tool: axe required.","工具不符: 需要斧頭。",
     "Wrong tool: pickaxe required.","工具不符: 需要鎬子。",
     "New objective: speak to Bob, in front of the door.","新目標: 與門前的 Bob 交談。"),
  "lines",Map.of());
 private static String show(Script.Hook hook,String text){return RenderTextPipeline.resolve(hook,text,DATA);}
 public static void main(String[]args){
  equal("known named container",show(TITLE,"Forge - Categories"),"鍛造 - 分類");
  equal("dash variant",show(TITLE," Forge — Categories "), " 鍛造 - 分類 ");
  equal("named player chest unchanged",show(TITLE,"YingHuang09's private chest"),"YingHuang09's private chest");
  equal("unknown named container unchanged",show(TITLE,"My Forge - Categories"),"My Forge - Categories");
  equal("non-container text unchanged",show(TITLE,"Iron Axe"),"Iron Axe");
  equal("shared anvil title",show(TITLE,"Anvil - Place item"),"鐵砧 - 放置物品");
  equal("shared anvil completion",show(TITLE,"Anvil - Done"),"鐵砧 - 完成");
  equal("shared forge instruction",show(CHAT,
      "Sneak + right-click the station with an empty hand to cancel the craft."),
      "空手潛行並右鍵點擊工作站，即可取消製作。");
  equal("bracketed item title",show(UPPER,"[Iron Axe]"),"[鐵斧]");
  equal("uppercase item title",show(UPPER,"[IRON AXE]"),"[鐵斧]");
  equal("plain exact item title",show(UPPER,"Iron Axe"),"鐵斧");
  equal("item amount title stripped",show(UPPER,"Iron Axe x2"),"鐵斧");
  equal("material quantity",show(ITEM,"Coal Ore: 5/10"),"煤礦石: 5/10");
  equal("material count changes",show(ITEM,"Coal Ore: 2/10"),"煤礦石: 2/10");
  equal("number without known item",show(ITEM,"Server Resource: 2/10"),"Server Resource: 2/10");
  equal("protect numeric item properties",show(ITEM,"Durability: 444/448"),"Durability: 444/448");
  equal("woodcutting bar",show(SYSTEM,"Woodcutting |||||||||||||||||||| 0%"),
     "伐木 |||||||||||||||||||| 0%");
  equal("woodcutting variable",show(SYSTEM,"Woodcutting |||||||||||||||||||| 97%"),
     "伐木 |||||||||||||||||||| 97%");
  equal("forge progress",show(SYSTEM,"You start forging: Katana du Chasseur"),
     "開始鍛造: 獵人武士刀");
  equal("forge score",show(SYSTEM,"Forging done! Score: 96/100"),
     "鍛造完成！得分: 96/100");
  equal("forge overall quality",show(SYSTEM,"Overall quality: 100% (100 | 100)"),
     "整體品質: 100% (100 | 100)");
  equal("forge rhythm",show(SYSTEM,"Hit 1/3: 98 pts (+199 ms)"),
     "敲擊 1/3: 98 分 (+199 ms)");
  equal("forge rhythm with spacing",show(SYSTEM,"Hit 3/3 : 93 pts (+246 ms)"),
     "敲擊 3/3: 93 分 (+246 ms)");
  equal("receive previously translated material",show(CHAT,"You received  煤礦石"),"已獲得  煤礦石");
  equal("receive exact bilingual item",show(CHAT,"You received  Coal Ore"),"已獲得  煤礦石");
  equal("add shared translated item",show(CHAT,"You added 1x Lingot de Fer Chaud"),
     "已加入 1x 灼熱鐵錠");
  equal("interlink pending",show(CHAT,"[Interlink] ✔ Tactical Boots is on its way to Aincrad. Available there in 5 s of play."),
     "[Interlink] ✔ 戰術長靴 正在運往艾恩葛朗特，遊玩 5 s 後可領取。");
  equal("interlink arrived",show(CHAT,"[Interlink] Tactical Boots has arrived in Aincrad: collect it from the Interlink there."),
     "[Interlink] 戰術長靴 已抵達艾恩葛朗特，請在 Interlink 領取。");
  equal("interlink delivered",show(CHAT,"[Interlink] Transfer complete: Tactical Boots is in your inventory."),
     "[Interlink] 傳送完成: 戰術長靴 已放入背包。");
  equal("queue estimates preserve min",show(CHAT,"Queue for the server: place 1/2 · < 1 min (estimated)"),
     "伺服器排隊: 第 1/2 · < 1 min（預估）");
  equal("known message word translation",show(SYSTEM,"Wrong tool: axe required."),"工具不符: 需要斧頭。");
  equal("known fixed objective",show(SYSTEM,"New objective: speak to Bob, in front of the door."),
     "新目標: 與門前的 Bob 交談。");
  equal("private player chat excluded",show(CHAT,"YingHuang09: You received Coal Ore"),
     "YingHuang09: You received Coal Ore");
  equal("unrecognized chat excluded",show(CHAT,"My Custom Chest: Hello"),
     "My Custom Chest: Hello");
  equal("player name never translated",show(TITLE,"Shepherd Ivo"),"Shepherd Ivo");
  equal("safe NPC name capture",UnknownCollector.safe("Shepherd Ivo","npc_name"),true);
  equal("safe speaker owner",UnknownCollector.permitted("fr/zapdialogue/client/gui/BubbleRenderer","argument","npc_name"),true);
  equal("never arbitrary network name",UnknownCollector.permitted("fr/zapdialogue/client/network/ZapClientPackets","argument","npc_name"),false);
  equal("single player ID excluded",UnknownCollector.safe("YingHuang09","npc_name"),false);
  equal("single name excluded",UnknownCollector.safe("Bob","npc_name"),false);
  equal("private key excluded",UnknownCollector.safe("secret=123456789","npc_name"),false);
  System.out.println("CROSS_SURFACE_TEXT_PASS assertions="+count+" source_reuse=true untrusted_titles_preserved=true");
 }
}
