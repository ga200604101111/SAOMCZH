package sao.zh.engine;
import java.util.*;
import java.util.regex.*;
/** Approved server system-notification templates, not generic chat or substring replacement. */
final class DynamicSystemMessages {
 private DynamicSystemMessages(){}
 private static final Pattern FORMATTING=Pattern.compile("(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])");
 private static final Pattern RECEIVED=Pattern.compile("^(You received)( {1,4})(.{1,120})$");
 private static final Pattern ADDED=Pattern.compile("^(You added)( )([1-9][0-9]{0,5}x)( )(.{1,120})$");
 private static final Pattern INSERTED=Pattern.compile("^(Item inserted:)( )([1-9][0-9]{0,5}x)( )(.{1,120})$");
 private static final Pattern STOPPED=Pattern.compile("^(Heating stopped at )([0-9]{1,5}(?:\\.[0-9]{1,3})?)(°C\\.)( Right-click with the item to insert it\\.)$");
 private static final Pattern REPORT=Pattern.compile("^(Furnace)([ \\t]*[—–-][ \\t]*)(temperature)( )([0-9]{1,4})( )(pts)( \\| )(time)( )([0-9]{1,4})( )(pts)( \\| )(precision)( )([0-9]{1,3})(%)$");
 private static final Set<String> SHARED_FIXED_SYSTEM_TEXT=Set.of(
  "Sneak + right-click the station with an empty hand to cancel the craft.");
 private static final Pattern CUTTING=Pattern.compile("^(Woodcutting)([\\s|]{2,120}[0-9]{1,3}%)$");
 private static final Pattern START_FORGING=Pattern.compile("^(You start forging: )(.{1,110})$");
 private static final Pattern QUALITY=Pattern.compile("^(Overall quality: )([0-9]{1,3}%)([\\s\\S]{0,60})$");
 private static final Pattern SCORE=Pattern.compile("^(Forging done! Score: )([0-9]{1,3}/[0-9]{1,3})$");
 private static final Pattern HIT=Pattern.compile("^(Hit )([0-9]{1,3}/[0-9]{1,3})(\\s*:\\s*)([0-9]{1,3})( pts)( ?\\([+\\-]?[0-9]{1,5} ms\\))?$");
 private static final Pattern POSITION=Pattern.compile("^(Queue for the server: place )([0-9]{1,3}/[0-9]{1,3})(\\s*[·•\\-]\\s*< ?[0-9]{1,5} min)( \\(estimated\\))$");
 private static final Pattern INTERLINK_QUEUED=Pattern.compile("^(\\[Interlink\\]\\s*(?:[✔✓⚠ℹ]\\s*)?)(.{1,100})( is on its way to Aincrad\\. Available there in )([0-9]{1,5})( s of play\\.)$");
 private static final Pattern INTERLINK_ARRIVED=Pattern.compile("^(\\[Interlink\\]\\s*(?:[✔✓⚠ℹ]\\s*)?)(.{1,100})( has arrived in Aincrad: collect it from the Interlink there\\.)$");
 private static final Pattern INTERLINK_DELIVERED=Pattern.compile("^(\\[Interlink\\]\\s*(?:[✔✓⚠ℹ]\\s*)?Transfer complete: )(.{1,100})( is in your inventory\\.)$");
 static boolean safeQueueEstimate(String raw){
  if(raw==null||raw.length()>110)return false;
  // Only this exact server Actionbar syntax may contain a literal comparison '<'.
  // Do not weaken generic chat, HTML or private-text filtering.
  return POSITION.matcher(raw.strip()).matches();
 }
 private record Edit(int from,int to,String replacement){}
 private static final class Plain {
  final String raw,text;final int[]start,end;
  Plain(String raw){
   this.raw=raw;start=new int[raw.length()];end=new int[raw.length()];
   Matcher tokens=FORMATTING.matcher(raw);StringBuilder out=new StringBuilder(raw.length());
   for(int i=0;i<raw.length();){
    tokens.region(i,raw.length());
    if(tokens.lookingAt()){i=tokens.end();continue;}
    int n=out.length();start[n]=i;end[n]=i+1;out.append(raw.charAt(i++));
   }
   text=out.toString();
  }
  String apply(List<Edit> input){
   if(input.isEmpty())return raw;
   ArrayList<Edit> edits=new ArrayList<>(input);edits.sort((a,b)->Integer.compare(b.from,a.from));
   String result=raw;int next=text.length();
   for(Edit e:edits){
    if(e.from<0||e.from>=e.to||e.to>text.length()||e.to>next||e.replacement.length()>256)return raw;
    next=e.from;int a=start[e.from],b=end[e.to-1];
    if(FORMATTING.matcher(raw.substring(a,b)).find())return raw; // Never erase embedded style boundaries.
    result=result.substring(0,a)+e.replacement+result.substring(b);
   }
   return result;
  }
 }
 private static int lead(String text){
  int pos=0,budget=0;
  while(pos<text.length()&&budget<6){
   int cp=text.codePointAt(pos);
   int type=Character.getType(cp);
   if(Character.isWhitespace(cp)||type==Character.PRIVATE_USE||type==Character.OTHER_SYMBOL||type==Character.MATH_SYMBOL){
    pos+=Character.charCount(cp);budget++;continue;
   }
   break;
  }
  return pos;
 }
 private static String item(String key,Map<String,Map<String,String>>dictionary){
  Map<String,String> items=dictionary.getOrDefault("items",Map.of());
  Map<String,String> names=dictionary.getOrDefault("names",Map.of());
  Map<String,String> lines=dictionary.getOrDefault("lines",Map.of());
  String one=items.get(key);if(one!=null)return one;
  one=names.get(key);
  if(one!=null&&lines.containsKey(key)&&!one.equals(lines.get(key)))return null;
  return one!=null?one:lines.get(key);
 }
 private static void replace(List<Edit>out,Matcher m,int index,String value,int offset){
  out.add(new Edit(offset+m.start(index),offset+m.end(index),value));
 }
 static String whole(String raw,Map<String,Map<String,String>> dictionary){
  if(raw==null||raw.length()>512||raw.indexOf('\n')>=0)return raw;
  Plain p=new Plain(raw);int left=lead(p.text),right=p.text.length();
  while(right>left&&Character.isWhitespace(p.text.charAt(right-1)))right--;
  if(left>=right)return raw;
  String text=p.text.substring(left,right);List<Edit> edits=new ArrayList<>(9);Matcher m;
  m=RECEIVED.matcher(text);
  if(m.matches()){
   String value=m.group(3),translated=item(value,dictionary);
   replace(edits,m,1,"已獲得",left);
   if(translated!=null)replace(edits,m,3,translated,left);
   return p.apply(edits);
  }
  m=ADDED.matcher(text);
  if(m.matches()){
   String translated=item(m.group(5),dictionary);
   replace(edits,m,1,"已加入",left);
   if(translated!=null)replace(edits,m,5,translated,left);
   return p.apply(edits);
  }
  m=INSERTED.matcher(text);
  if(m.matches()){
   String translated=item(m.group(5),dictionary);
   replace(edits,m,1,"已放入:",left);
   if(translated!=null)replace(edits,m,5,translated,left);
   return p.apply(edits);
  }
  m=STOPPED.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"已在 ",left);
   replace(edits,m,3,"°C",left);
   replace(edits,m,4," 停止加熱。手持物品右鍵放入。",left);
   return p.apply(edits);
  }
  m=REPORT.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"熔爐",left);replace(edits,m,3,"溫度",left);
   replace(edits,m,7,"分",left);replace(edits,m,9,"時間",left);
   replace(edits,m,13,"分",left);replace(edits,m,15,"精準度",left);
   return p.apply(edits);
  }
  // A tiny source-verified exception: this exact public instruction already
  // exists in the item glossary. Never duplicate it in messages or reuse all
  // item text inside player chat.
  if(SHARED_FIXED_SYSTEM_TEXT.contains(text)){
   String translated=item(text,dictionary);
   if(translated!=null&&translated.length()<=256)
    return p.apply(List.of(new Edit(left,right,translated)));
  }
  m=CUTTING.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"伐木",left);
   return p.apply(edits);
  }
  m=START_FORGING.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"開始鍛造: ",left);
   String itemName=item(m.group(2),dictionary);
   if(itemName!=null)replace(edits,m,2,itemName,left);
   return p.apply(edits);
  }
  m=QUALITY.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"整體品質: ",left);
   return p.apply(edits);
  }
  m=SCORE.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"鍛造完成！得分: ",left);
   return p.apply(edits);
  }
  m=HIT.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"敲擊 ",left);
   replace(edits,m,3,": ",left);
   replace(edits,m,5," 分",left);
   return p.apply(edits);
  }
  m=POSITION.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"伺服器排隊: 第 ",left);
   replace(edits,m,4,"（預估）",left);
   return p.apply(edits);
  }
  m=INTERLINK_QUEUED.matcher(text);
  if(m.matches()){
   String translated=item(m.group(2),dictionary);
   if(translated!=null)replace(edits,m,2,translated,left);
   replace(edits,m,3," 正在運往艾恩葛朗特，遊玩 ",left);
   replace(edits,m,5," s 後可領取。",left);
   return p.apply(edits);
  }
  m=INTERLINK_ARRIVED.matcher(text);
  if(m.matches()){
   String translated=item(m.group(2),dictionary);
   if(translated!=null)replace(edits,m,2,translated,left);
   replace(edits,m,3," 已抵達艾恩葛朗特，請在 Interlink 領取。",left);
   return p.apply(edits);
  }
  m=INTERLINK_DELIVERED.matcher(text);
  if(m.matches()){
   replace(edits,m,1,"[Interlink] 傳送完成: ",left);
   String translated=item(m.group(2),dictionary);
   if(translated!=null)replace(edits,m,2,translated,left);
   replace(edits,m,3," 已放入背包。",left);
   return p.apply(edits);
  }
  return raw;
 }
 /**
  * Fragment fallback for styled Text leaf trees. The containing entire system
  * message must first pass SemanticText.permitTree before these are applied.
  */
 static String fragment(String raw,Map<String,Map<String,String>>dictionary){
  if(raw==null||raw.length()>160)return raw;
  Plain p=new Plain(raw);String s=p.text;List<Edit> changes=new ArrayList<>(6);
  if(s.equals("You received")||s.equals("You added")||s.equals("Item inserted:")){
   return p.apply(List.of(new Edit(0,s.length(),s.equals("You received")?"已獲得":s.equals("You added")?"已加入":"已放入:")));
  }
  for(String prefix:List.of("You received","You added","Item inserted:")){
   if(s.startsWith(prefix)&&s.substring(prefix.length()).matches(" {1,4}")){
    changes.add(new Edit(0,prefix.length(),prefix.equals("You received")?"已獲得":prefix.equals("You added")?"已加入":"已放入:"));
    return p.apply(changes);
   }
  }
  if(s.equals("Heating stopped at "))return p.apply(List.of(new Edit(0,s.length(),"已在 ")));
  if(s.equals(" Right-click with the item to insert it."))
   return p.apply(List.of(new Edit(0,s.length()," 停止加熱。手持物品右鍵放入。")));
  if(s.endsWith("°C.")&&s.length()>3){
   String amount=s.substring(0,s.length()-3);
   if(amount.length()<=9&&amount.matches("[0-9]+(?:[.][0-9]{1,3})?"))
    return p.apply(List.of(new Edit(s.length()-1,s.length(),"")));
  }
  Matcher withCount=Pattern.compile("^(You added|Item inserted:)( )([1-9][0-9]{0,5}x)( )$").matcher(s);
  if(withCount.matches())return p.apply(List.of(new Edit(withCount.start(1),withCount.end(1),withCount.group(1).equals("You added")?"已加入":"已放入:")));
  if(s.equals("Hey!"))return p.apply(List.of(new Edit(0,s.length(),"嘿！")));
  if(s.equals(" Sorry, but you can't change that here."))return p.apply(List.of(new Edit(0,s.length()," 抱歉，你不能在這裡更改。")));
  if(s.equals(" Sorry, but you can't use that here."))return p.apply(List.of(new Edit(0,s.length()," 抱歉，你不能在這裡使用。")));
  String item=item(s,dictionary);
  if(item!=null&&s.length()>0)return p.apply(List.of(new Edit(0,s.length(),item)));
  // Colored numeric spans in the furnace score separate labels from their values.
  // These tokens are only reached after the WHOLE message passed the system gate.
  if(s.length()<=100&&s.matches("[ A-Za-z0-9\t|—–.%-]+")){
   Matcher labels=Pattern.compile("[A-Za-z]+").matcher(s);
   ArrayList<Edit> words=new ArrayList<>(6);
   while(labels.find()){
    String word=labels.group();
    String translated=switch(word){case "Furnace"->"熔爐";case "temperature"->"溫度";case "time"->"時間";case "precision"->"精準度";case "pts"->"分";default->null;};
    if(translated==null)return raw;
    words.add(new Edit(labels.start(),labels.end(),translated));
   }
   if(!words.isEmpty())return p.apply(words);
  }
  return raw;
 }
}
