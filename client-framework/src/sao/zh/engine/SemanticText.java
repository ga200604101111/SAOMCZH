package sao.zh.engine;
import java.util.*;import java.util.regex.*;
/** Bounded exact-span translation for approved semantic sources, never a global regex replacer. */
final class SemanticText {
 private SemanticText(){}
 private static final Pattern FORMAT=Pattern.compile("(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])");
 private static final String LEVEL="(?:Lv\\.?|Lvl\\.?|Level|Niv\\.?|Niveau)\\s*(?:[°º·•:]\\s*)?[0-9]{1,6}\\+?";
 private static final Pattern LEVEL_END=Pattern.compile("^(.*?)(\\s+(?:\\[\\s*)?"+LEVEL+"(?:\\s*\\])?\\s*)$",Pattern.CASE_INSENSITIVE);
 private static final Pattern LEVEL_START=Pattern.compile("^((?:\\[\\s*)?"+LEVEL+"(?:\\s*\\])?\\s+)(.+)$",Pattern.CASE_INSENSITIVE);
 private static final Pattern DISTANCE=Pattern.compile("^(.*?)(\\s*(?:\\|\\s*|\\[)[0-9]{1,9}(?:[.,][0-9]{1,3})?\\s*(?:km|m)\\]?\\s*)$",Pattern.CASE_INSENSITIVE);
 private static final Pattern COUNT_START=Pattern.compile("^([0-9]{1,6}[x×]\\s*)(.+)$");
 private static final Pattern COUNT_END=Pattern.compile("^(.*?)(\\s+[x×][0-9]{1,6})$");
 private static final Pattern CHAT=Pattern.compile("(?s)^\\s*(?:\\[[^]\\r\\n]{1,40}]\\s*)*[A-Za-z0-9_]{3,16}\\s*[:：].*");
 static boolean npc(Script.Hook h){return h.id.equals("worlds-locations:npc-tagged-label")&&h.owner.equals("net/minecraft/class_897");}
 static boolean system(Script.Hook h){return h.id.equals("dynamic-translation:game-system-display")&&h.owner.equals("net/minecraft/class_7594");}
 static boolean world(Script.Hook h){return h.owner.equals("com/eighteen/saojob/hologram/HologramRenderer")||h.owner.equals("fr/sao/translate/SaoJobHologramTexts")||h.owner.equals("net/minecraft/class_8138$class_8141")||h.owner.equals("net/minecraft/class_877");}
 static boolean mob(Script.Hook h){return h.id.equals("codex-creatures:mob-shown-name")&&h.owner.equals("fr/sao/saoui/mob/MobBarRenderer");}
 static boolean compass(Script.Hook h){return h.owner.equals("fr/sao/questtracker/hud/QuestCompassRenderer");}
 static boolean item(Script.Hook h){return h.section.equals("items");}
 static boolean enabled(Script.Hook h){return mob(h)||npc(h)||system(h)||world(h)||compass(h)||item(h);}
 static String captureKey(Script.Hook h,String raw){
  if(raw==null||!enabled(h)||!permitted(h,raw))return raw;String s=FORMAT.matcher(raw).replaceAll("").strip();
  if(mob(h)||world(h)){
   Matcher tail=LEVEL_END.matcher(s);if(tail.matches())return tail.group(1).strip();
   Matcher first=LEVEL_START.matcher(s);if(first.matches())return first.group(2).strip();
  }
  if(compass(h)){Matcher distance=DISTANCE.matcher(s);if(distance.matches())return distance.group(1).strip();}
  if(ItemPresentation.title(h))return ItemPresentation.canonical(raw);
  if(item(h)){
   Matcher first=COUNT_START.matcher(s);if(first.matches())return first.group(2).strip();
   Matcher tail=COUNT_END.matcher(s);if(tail.matches())return tail.group(1).strip();
  }
  return s;
 }
 static boolean privateMessage(String plain){return plain==null||plain.length()>8192||plain.indexOf('»')>=0||plain.indexOf('›')>=0||plain.indexOf('<')>=0||plain.indexOf('>')>=0||plain.contains("://")||CHAT.matcher(plain).matches();}
 static boolean permitted(Script.Hook h,String raw){
  if(!enabled(h))return true;if(raw==null)return false;
  String visible=FORMAT.matcher(raw).replaceAll("").strip();
  if((system(h)||mob(h)||npc(h))&&privateMessage(visible))return false;
  return !npc(h)||visible.startsWith("[NPC]");
 }
 /** Whole-message gate is applied before traversing Text leaves, so a player's
  * line cannot have an embedded system phrase translated by a child-node match. */
 static boolean permitTree(Script.Hook h,String whole,Map<String,Map<String,String>>data){
  if(!system(h)&&!npc(h))return true;if(!permitted(h,whole))return false;
  return !system(h)||!resolve(h,whole,data).equals(whole);
 }
 static String resolve(Script.Hook h,String raw,Map<String,Map<String,String>>data){
  if(raw==null||raw.length()>8192||!enabled(h)||!permitted(h,raw))return raw;
  if(raw.indexOf('\n')>=0){
   if(system(h)||npc(h))return raw;
   String[]parts=raw.split("\n",-1);if(parts.length>16)return raw;boolean changed=false;
   for(int i=0;i<parts.length;i++){String next=one(h,parts[i],data);changed|=!parts[i].equals(next);parts[i]=next;}
   return changed?String.join("\n",parts):raw;
  }
  return one(h,raw,data);
 }
 private static String one(Script.Hook h,String raw,Map<String,Map<String,String>>data){
  if(raw.length()>2048)return raw;
  Visible text=new Visible(raw);String plain=text.plain;
  int left=0,right=plain.length();while(left<right&&Character.isWhitespace(plain.charAt(left)))left++;
  while(right>left&&Character.isWhitespace(plain.charAt(right-1)))right--;
  if(left==right)return raw;
  // The label marker is metadata, not part of the NPC dictionary key.
  if((npc(h)||world(h))&&plain.startsWith("[NPC]",left)){left+=5;while(left<right&&Character.isWhitespace(plain.charAt(left)))left++;}
  if(system(h)){
   while(left<right&&(Character.isWhitespace(plain.charAt(left))||"✔✓☑".indexOf(plain.charAt(left))>=0))left++;
  }else if(world(h)||mob(h)){
   while(left<right){int cp=plain.codePointAt(left);if(Character.getType(cp)==Character.PRIVATE_USE||"●•◆◇".indexOf(cp)>=0||Character.isWhitespace(cp))left+=Character.charCount(cp);else break;}
  }
  String candidate=plain.substring(left,right);String exact=lookup(h,candidate,data);
  if(exact!=null)return text.replace(left,right,exact);
  Matcher match;
  if(compass(h)){
   match=DISTANCE.matcher(candidate);
   if(match.matches())return replaceGroup(h,text,left,match,1,data);
  }
  if(mob(h)||world(h)||npc(h)){
   match=LEVEL_END.matcher(candidate);if(match.matches())return replaceGroup(h,text,left,match,1,data);
   match=LEVEL_START.matcher(candidate);if(match.matches())return replaceGroup(h,text,left,match,2,data);
  }
  if(item(h)){
   match=COUNT_START.matcher(candidate);if(match.matches())return replaceGroup(h,text,left,match,2,data);
   match=COUNT_END.matcher(candidate);if(match.matches())return replaceGroup(h,text,left,match,1,data);
  }
  return raw;
 }
 private static String replaceGroup(Script.Hook h,Visible text,int offset,Matcher m,int group,Map<String,Map<String,String>>data){
  int begin=offset+m.start(group),end=offset+m.end(group);
  while(begin<end&&Character.isWhitespace(text.plain.charAt(begin)))begin++;
  while(end>begin&&Character.isWhitespace(text.plain.charAt(end-1)))end--;
  String next=lookup(h,text.plain.substring(begin,end),data);return next==null?text.raw:text.replace(begin,end,next);
 }
 private static String lookup(Script.Hook h,String name,Map<String,Map<String,String>>data){
  if(name.isEmpty())return null;
  return SharedDictionary.lookup(h,name,data);
 }
 private static final class Visible {
  final String raw,plain;final int[]start,end;
  Visible(String raw){this.raw=raw;start=new int[raw.length()];end=new int[raw.length()];StringBuilder out=new StringBuilder(raw.length());Matcher tokens=FORMAT.matcher(raw);int i=0;
   while(i<raw.length()){tokens.region(i,raw.length());if(tokens.lookingAt()){i=tokens.end();continue;}int n=out.length();start[n]=i;end[n]=i+1;out.append(raw.charAt(i++));}plain=out.toString();
  }
  String replace(int from,int to,String translated){
   if(from<0||to<=from||to>plain.length())return raw;
   int a=start[from],b=end[to-1];
   // Preserve mixed-style runs rather than flatten inline color boundaries blindly.
   if(FORMAT.matcher(raw.substring(a,b)).find())return raw;
   return raw.substring(0,a)+translated+raw.substring(b);
  }
 }
}
