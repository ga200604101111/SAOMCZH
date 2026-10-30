package sao.zh.engine;
import java.util.*;import java.nio.file.*;
/** Actual pipeline contract: name-only UI slots, independent count labels, bounded item stats. */
public final class ItemTitleAndStatsContract {
 static int checks,failures;
 static void eq(String label,String expected,String actual){checks++;if(!Objects.equals(expected,actual)){failures++;System.out.println("FAIL "+label+" expected="+expected+" actual="+actual);}}
 public static void main(String[] args)throws Exception{
  Script script=Script.parse(Files.readAllBytes(Path.of(args[0])));
  Map<String,String> items=Map.of("Mana Shard","魔力碎片","Healing Shard","治療碎片","Stamina Shard","耐力碎片","Halloween Key","萬聖節鑰匙","Quality Health Potion III","優質生命藥水 III","Unknown New Reward","新的獎勵","Training Pavise","訓練用大盾","Healing Shard x2","治療碎片 x2");
  Map<String,Map<String,String>> dict=Map.of("items",items,"names",items,"lines",items,"messages",Map.of());
  String[] titles={"items-economy:reward-name","items-economy:reward-cached-name","items-economy:tooltip-name","items-economy:native-item-name","items-economy:detail-visible-title"};
  for(String id:titles){
   Script.Hook h=script.byId.get(id);if(h==null)throw new AssertionError("Missing hook "+id);
   for(String marker:new String[]{" x2"," x64"," × 12"," X 3"}){
    eq(id+marker,"魔力碎片",RenderTextPipeline.resolve(h,"Mana Shard"+marker,dict));
    eq(id+" translated "+marker,"魔力碎片",RenderTextPipeline.resolve(h,"魔力碎片"+marker,dict));
   }
   eq(id+" prefix","魔力碎片",RenderTextPipeline.resolve(h,"12x Mana Shard",dict));
   eq(id+" formatting","§b魔力碎片§r",RenderTextPipeline.resolve(h,"§bMana Shard x2§r",dict));
   eq(id+" cached translated formatting","§b魔力碎片§r",RenderTextPipeline.resolve(h,"§b魔力碎片 x2§r",dict));
   eq(id+" spaces"," 魔力碎片 ",RenderTextPipeline.resolve(h," Mana Shard x2 ",dict));
   eq(id+" potion tier","優質生命藥水 III",RenderTextPipeline.resolve(h,"Quality Health Potion III x5",dict));
   eq(id+" legacy full-count exact","治療碎片",RenderTextPipeline.resolve(h,"Healing Shard x2",dict));
   eq(id+" new item","新的獎勵",RenderTextPipeline.resolve(h,"Unknown New Reward x12",dict));
   for(String s:new String[]{"2","x2","× 2","Tier 2","Sword +2","Mana Shardx2","Mana Shard x0","Mana Shard x2.5"})eq(id+" keep "+s,s,RenderTextPipeline.resolve(h,s,dict));
  }
  Script.Hook lore=script.byId.get("items-economy:tooltip-display");
  eq("lore count preserved","12x 訓練用大盾",RenderTextPipeline.resolve(lore,"12x Training Pavise",dict));
  eq("durability FR","⛏ 耐久度: 123/448",RenderTextPipeline.resolve(lore,"⛏ Durabilité: 123/448",dict));
  eq("durability EN","⛏ 耐久度: 123/448",RenderTextPipeline.resolve(lore,"⛏ Durability: 123/448",dict));
  eq("quantity status","持有: 17/15 ✔",RenderTextPipeline.resolve(lore,"You have: 17/15 ✔",dict));
  eq("xp status","獎勵: +25 經驗值",RenderTextPipeline.resolve(lore,"Reward: +25 XP",dict));
  eq("harvest stat","⛏ 採集能力: 2",RenderTextPipeline.resolve(lore,"⛏ Harvest Power: 2",dict));
  for(String s:new String[]{"You have: 17/15 ✔ extra","⛏ Durability: invalid","Reward: +25 XP and a sword","Hello x2"})eq("unrecognized lore "+s,s,RenderTextPipeline.resolve(lore,s,dict));
  Script.Hook chat=script.byId.get("dynamic-translation:game-system-display");
  eq("not global stats","You have: 17/15 ✔",RenderTextPipeline.resolve(chat,"You have: 17/15 ✔",dict));
  eq("not global name cleanup","Mana Shard x2",RenderTextPipeline.resolve(chat,"Mana Shard x2",dict));
  if(failures!=0)throw new AssertionError("Item-title/stat failures="+failures+"/"+checks);
  System.out.println("ITEM_TITLE_AND_STATS_CONTRACT_PASS checks="+checks+" native_count_untouched=true lore_numbers_preserved=true");
 }
}
