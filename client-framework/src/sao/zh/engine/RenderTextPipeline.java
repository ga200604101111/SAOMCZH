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
 static boolean wholeFirst(Script.Hook h){
  String owner=h.owner;
  // Never assemble player chat/nameplate/scoreboard nodes in a generic renderer.
  return owner.equals("net/minecraft/class_897")||SemanticText.system(h)
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
  if(raw==null||raw.length()>8192||!SemanticText.permitted(hook,raw))return raw;
  if(ItemPresentation.title(hook))return ItemPresentation.title(hook,raw,dictionaries);
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
