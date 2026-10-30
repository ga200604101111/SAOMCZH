package sao.zh.engine;
import java.util.*;import java.util.regex.*;
/** Counts belong to native quantity widgets, not item-name strings. Lore numbers are independent. */
final class ItemPresentation {
 private ItemPresentation(){}
 private static final Map<String,String> TITLES=Map.ofEntries(
  Map.entry("items-economy:item-name","fr/sao/itemlang/ItemLangClient"),
  Map.entry("items-economy:native-item-name","net/minecraft/class_1799"),
  Map.entry("items-economy:tooltip-name","fr/sao/tooltip/TipModel"),
  Map.entry("items-economy:detail-info-name","fr/sao/iteminfo/ItemInfoClient$Info"),
  Map.entry("items-economy:detail-visible-title","fr/sao/iteminfo/ItemInfoOverlay"),
  Map.entry("items-economy:reward-name","fr/sao/daily/DailyModel$Reward"),
  Map.entry("items-economy:reward-cached-name","fr/sao/daily/DailyScreen"),
  Map.entry("items-economy:auction-item-name","fr/sao/auctions/AuctionScreen"));
 private static final Pattern FORMAT=Pattern.compile("(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])");
 private static final Pattern SUFFIX=Pattern.compile("^(.+?)[\\h]+[xX×][\\h]*[1-9][0-9]{0,5}$");
 private static final Pattern PREFIX=Pattern.compile("^[1-9][0-9]{0,5}[\\h]*[xX×][\\h]+(.+)$");
 static boolean title(Script.Hook h){return h.section.equals("items")&&h.owner.equals(TITLES.get(h.id));}
 private static String bare(String s){Matcher m=SUFFIX.matcher(s);if(m.matches())return m.group(1).stripTrailing();m=PREFIX.matcher(s);return m.matches()?m.group(1).stripLeading():s;}
 static String canonical(String raw){if(raw==null)return null;return bare(FORMAT.matcher(raw).replaceAll("").strip());}
 static String title(Script.Hook h,String raw,Map<String,Map<String,String>> data){
  if(raw==null||raw.length()>2048||raw.indexOf('\n')>=0)return raw;
  int[] pos=new int[raw.length()];StringBuilder visible=new StringBuilder();Matcher codes=FORMAT.matcher(raw);
  for(int i=0;i<raw.length();){codes.region(i,raw.length());if(codes.lookingAt()){i=codes.end();continue;}pos[visible.length()]=i;visible.append(raw.charAt(i++));}
  String plain=visible.toString();int left=0,right=plain.length();
  while(left<right&&Character.isWhitespace(plain.charAt(left)))left++;while(right>left&&Character.isWhitespace(plain.charAt(right-1)))right--;
  if(left==right)return raw;String candidate=plain.substring(left,right),name=bare(candidate);
  String translated=SharedDictionary.lookup(h,name,data);
  if(translated==null){String rule=h.exact.get(name);if(rule!=null&&!rule.equals(name))translated=rule;}
  boolean counted=!name.equals(candidate);if(translated==null&&!counted)return raw;
  int a=pos[left],b=pos[right-1]+1;String core;
  int local=candidate.indexOf(name),nameBegin=pos[left+local],nameEnd=pos[left+local+name.length()-1]+1;
  if(translated==null)core=raw.substring(nameBegin,nameEnd);
  else {
   if(FORMAT.matcher(raw.substring(nameBegin,nameEnd)).find())return raw;
   core=bare(translated);if(core.length()>16384)return raw;
  }
  // Formatting after the visible name (including a terminal reset) remains outside the changed span.
  return raw.substring(0,a)+core+raw.substring(b);
 }
 private static final Pattern DURABILITY=Pattern.compile("^(\\s*⛏\\s+)(?:Durabilité|Durability)\\s*:\\s*([0-9]{1,9}/[0-9]{1,9})(\\s*)$");
 private static final Pattern HARVEST=Pattern.compile("^(\\s*⛏\\s+)(?:Puissance de récolte|Harvest Power)\\s*:\\s*([0-9]{1,6})(\\s*)$");
 private static final Pattern STOCK=Pattern.compile("^(\\s*)You have: ([0-9]{1,9}/[0-9]{1,9})(\\s*[✔✓]?\\s*)$");
 private static final Pattern XP=Pattern.compile("^(\\s*)Reward: (\\+?[0-9]{1,9}) XP(\\s*)$");
 static String stats(Script.Hook h,String raw){
  if(!h.section.equals("items")||title(h)||raw.length()>160)return raw;
  Matcher m=DURABILITY.matcher(raw);if(m.matches())return m.group(1)+"耐久度: "+m.group(2)+m.group(3);
  m=HARVEST.matcher(raw);if(m.matches())return m.group(1)+"採集能力: "+m.group(2)+m.group(3);
  m=STOCK.matcher(raw);if(m.matches())return m.group(1)+"持有: "+m.group(2)+m.group(3);
  m=XP.matcher(raw);if(m.matches())return m.group(1)+"獎勵: "+m.group(2)+" 經驗值"+m.group(3);
  return raw;
 }
}
