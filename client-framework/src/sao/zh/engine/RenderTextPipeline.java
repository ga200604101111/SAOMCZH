package sao.zh.engine;
import java.util.*;
import java.util.regex.*;
/** Source -> bounded canonical text -> the existing native layout/render path.
 * This code never scans the screen, modifies packets or measures glyphs.
 * Domain gating ensures numeric messages never rewrite player chat. */
final class RenderTextPipeline {
 private static final Pattern DODGE=Pattern.compile("^You dodged ([0-9]{1,9}(?:\\.[0-9]{1,6})?) damage!$");
 private static final Pattern SHIELD=Pattern.compile("^🛡 Bouclier de Mana \\[([0-9]{1,9})/([0-9]{1,9})\\]$");
 private static final Pattern HEAL=Pattern.compile("^❤ Soin \\+([0-9]{1,9}(?:\\.[0-9]{1,6})?) \\| total soigne ([0-9]{1,9}(?:\\.[0-9]{1,6})?)$");
 private RenderTextPipeline(){}
 static boolean styledName(Script.Hook h){return SemanticText.world(h)||SemanticText.mob(h)||SemanticText.boss(h)||SemanticText.npc(h)||SemanticText.location(h)||ItemPresentation.title(h);}
 static boolean wholeFirst(Script.Hook h){
  String owner=h.owner;
  // Never assemble player chat/nameplate/scoreboard nodes in a generic renderer.
  return SemanticText.boss(h)||NamedContainerTitles.hook(h)||owner.startsWith("fr/zapdialogue/client/")||owner.equals("net/minecraft/class_897")||SemanticText.system(h)||SemanticText.location(h)
   ||owner.equals("net/minecraft/class_877")
   ||owner.equals("net/minecraft/class_8138$class_8141")
   ||owner.equals("com/eighteen/saojob/hologram/HologramRenderer")
   ||owner.startsWith("fr/sao/questtracker/")
   ||owner.startsWith("fr/sao/codex/")
   ||owner.startsWith("fr/sao/iteminfo/")
   ||owner.startsWith("com/eighteen/saojob/skills/")
   ||owner.startsWith("com/eighteen/saojob/skilltree/");
 }
 static String resolve(Script.Hook hook,String raw,Map<String,Map<String,String>> dictionaries){
  String output=resolveSource(hook,raw,dictionaries);
  // Equipment stat labels are in the `names` section, not `items`.
  // Apply the same U+FF1A → ASCII colon normalization to that exact item-stat hook;
  // never globally rewrite names, locations, times, or player chat.
  boolean itemField=hook.section.equals("items")&&!ItemPresentation.title(hook);
  boolean equipmentField=hook.id.equals("items-economy:equipment-stat")
    &&hook.owner.equals("fr/sao/equipment/EquipmentClient$Stat");
  return itemField||equipmentField?FieldTypography.normalize(output):output;
 }
 private static String resolveSource(Script.Hook hook,String raw,Map<String,Map<String,String>> dictionaries){
  if(raw==null||raw.length()>8192||!SemanticText.permitted(hook,raw))return raw;
  // Some SAO TextDisplays concatenate an NPC name and its instruction with
  // a private-use hard separator. The ordinary exact lookup cannot match the
  // whole combined string, but each reviewed fragment can be reused safely.
  if(SemanticText.world(hook)&&raw.indexOf('\uE001')>=0){
   String split=WorldTextSegments.resolve(hook,raw,dictionaries);
   if(!split.equals(raw))return split;
  }
  if(NamedContainerTitles.hook(hook))return NamedContainerTitles.resolve(raw,dictionaries);
  if(ItemPresentation.title(hook))return ItemPresentation.title(hook,raw,dictionaries);
  if(hook.section.equals("items")){
   String resource=ItemPresentation.resourceCount(hook,raw,dictionaries);
   if(!resource.equals(raw))return resource;
  }
  String stat=ItemPresentation.stats(hook,raw);if(!raw.equals(stat))return stat;
  // Already-authorized full-line numeric formats only, before fragment replacement.
  if(hook.id.startsWith("combat-feedback:")){
   String dynamic=combat(raw);if(dynamic!=null)return dynamic;
  }
  String mapped=hook.literal(raw);if(!raw.equals(mapped))return mapped;
  String next=SharedDictionary.lookup(hook,raw,dictionaries);
  return next==null||next.length()>16384?SemanticText.resolve(hook,raw,dictionaries):next;
 }
 private static String combat(String raw){
  if(raw.length()>92||raw.indexOf('§')>=0)return null;
  Matcher m=DODGE.matcher(raw);
  if(m.matches())return "你閃避了 "+m.group(1)+" 點傷害！";
  m=SHIELD.matcher(raw);
  if(m.matches())return "🛡 魔力護盾 ["+m.group(1)+"/"+m.group(2)+"]";
  m=HEAL.matcher(raw);
  if(m.matches())return "❤ 治療 +"+m.group(1)+" | 總治療量 "+m.group(2);
  return null;
 }
}
