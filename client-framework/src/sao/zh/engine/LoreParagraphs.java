package sao.zh.engine;
import java.util.*;import java.util.function.*;import java.util.regex.*;
/** Exact reviewed multi-row lore -> a paragraph BEFORE the native pixel-width wrap.
 * No inference from punctuation, no joining across blanks/styles/stats, no model writes. */
final class LoreParagraphs {
 private static final int MAX_ROWS=128,MAX_SPAN=8,MAX_TEXT=8192,MAX_INDEX=1024;
 private static final Pattern FORMAT=Pattern.compile("(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])");
 private final Map<String,String> paragraphs;
 private record Part(String plain,String prefix,String suffix){}
 private static final class Memo {long epoch=Long.MIN_VALUE;final LinkedHashMap<List<String>,List<String>> values=new LinkedHashMap<>(16,.75f,true);}
 private final ThreadLocal<Memo> memo=ThreadLocal.withInitial(Memo::new);
 LoreParagraphs(Map<String,Map<String,String>> data){
  Map<String,String> explicit=new LinkedHashMap<>(),aliases=new LinkedHashMap<>();Set<String>ambiguous=new HashSet<>();
  // Copy only intentionally reviewed newline-separated source keys, not all prose.
  for(String section:List.of("items","lines"))for(Map.Entry<String,String> e:new TreeMap<>(data.getOrDefault(section,Map.of())).entrySet()){
   if(explicit.size()>=MAX_INDEX)break;
   String raw=e.getKey(),target=e.getValue();if(raw.indexOf('\n')<0||raw.length()>MAX_TEXT||target==null||target.isBlank()||target.length()>2048||target.indexOf('\n')>=0)continue;
   String[] rows=raw.split("\n",-1);if(rows.length<2||rows.length>MAX_SPAN)continue;
   ArrayList<String> plain=new ArrayList<>();boolean valid=true;
   for(String row:rows){Part p=part(row);if(p==null){valid=false;break;}plain.add(p.plain);}
   if(!valid)continue;String key=String.join("\n",plain);explicit.putIfAbsent(key,target);
   ArrayList<String> translated=new ArrayList<>();
   for(String row:plain){String value=lookup(row,data);Part p=value==null?null:part(value);if(p==null){valid=false;break;}translated.add(p.plain);}
   if(valid){String alias=String.join("\n",translated);String old=aliases.putIfAbsent(alias,target);if(old!=null&&!old.equals(target))ambiguous.add(alias);}
  }
  for(Map.Entry<String,String> e:aliases.entrySet())if(!ambiguous.contains(e.getKey())&&explicit.size()<MAX_INDEX)explicit.putIfAbsent(e.getKey(),e.getValue());
  paragraphs=Map.copyOf(explicit);
 }
 private static String lookup(String row,Map<String,Map<String,String>> data){
  for(String section:List.of("items","names","lines"))for(String key:List.of(row," "+row,row+" "," "+row+" ")){
   String hit=data.getOrDefault(section,Map.of()).get(key);if(hit!=null)return hit;
  }
  return null;
 }
 private static Part part(String raw){
  if(raw==null||raw.isBlank()||raw.length()>1024||raw.indexOf('\n')>=0||raw.indexOf('\r')>=0)return null;
  int left=0,right=raw.length();Matcher format=FORMAT.matcher(raw);
  while(left<right){if(Character.isWhitespace(raw.charAt(left))){left++;continue;}format.region(left,right);if(format.lookingAt()){left=format.end();continue;}break;}
  boolean again=true;while(again&&right>left){again=false;while(right>left&&Character.isWhitespace(raw.charAt(right-1)))right--;format.region(left,right);while(format.find())if(format.end()==right){right=format.start();again=true;break;}}
  if(left>=right)return null;String body=raw.substring(left,right);
  if(FORMAT.matcher(body).find()||body.indexOf(':')>=0||body.indexOf('：')>=0||body.startsWith("- ")||body.startsWith("[")||body.startsWith("{")||body.contains("{{"))return null;
  boolean letters=false;
  for(int at=0;at<body.length();){int c=body.codePointAt(at);at+=Character.charCount(c);int type=Character.getType(c);
   if(Character.isISOControl(c)||Character.isDigit(c)||type==Character.PRIVATE_USE||type==Character.OTHER_SYMBOL||type==Character.MATH_SYMBOL||"•●▪→✔✓⛏".indexOf(c)>=0)return null;
   letters|=Character.isLetter(c);
  }
  return letters?new Part(body,raw.substring(0,left),raw.substring(right)):null;
 }
 Object project(Object value,UnaryOperator<String> single,UnaryOperator<String> readable,BiFunction<String,String,String> encode,long epoch){
  if(!(value instanceof List<?> rows)||rows.size()>MAX_ROWS)return value;
  int chars=0;for(Object item:rows){if(!(item instanceof String))return value;chars+=((String)item).length();if(chars>MAX_TEXT)return value;}
  Memo cache=memo.get();if(cache.epoch!=epoch){cache.values.clear();cache.epoch=epoch;}
  List<String> cached=cache.values.get(rows);if(cached!=null)return cached;
  ArrayList<String> result=new ArrayList<>(rows.size());boolean modified=false,wholeMerged=false;
  for(int i=0;i<rows.size();){
   String original=(String)rows.get(i);Part first=part(readable.apply(original));String best=null;int span=0;
   if(first!=null&&!paragraphs.isEmpty()){
    StringBuilder key=new StringBuilder(first.plain);
    for(int j=i+1;j<rows.size()&&j-i<MAX_SPAN;j++){
     Part next=part(readable.apply((String)rows.get(j)));
     if(next==null||!next.prefix.equals(first.prefix)||!next.suffix.equals(first.suffix))break;
     key.append('\n').append(next.plain);if(key.length()>MAX_TEXT)break;
     String hit=paragraphs.get(key.toString());if(hit!=null){best=hit;span=j-i+1;}
    }
   }
   if(best!=null){
    StringBuilder source=new StringBuilder();for(int k=0;k<span;k++){if(k>0)source.append('\n');source.append((String)rows.get(i+k));}
    String encoded=encode.apply(source.toString(),best);if(encoded==null||encoded.length()>MAX_TEXT)return value;
    result.add(first.prefix+encoded+first.suffix);modified=true;wholeMerged=i==0&&span==rows.size();i+=span;
   }else{
    String next=single.apply(original);if(next==null||next.length()>MAX_TEXT)return value;
    result.add(next);modified|=!next.equals(original);i++;
   }
  }
  if(!modified)return value;List<String> out=List.copyOf(result);
  // Only fully reviewed, wholly merged paragraphs are memoized; unknown rows can retry capture.
  if(wholeMerged&&rows.size()<=MAX_SPAN){List<String> key=new ArrayList<>(rows.size());for(Object s:rows)key.add((String)s);cache.values.put(List.copyOf(key),out);while(cache.values.size()>32)cache.values.remove(cache.values.keySet().iterator().next());}
  return out;
 }
}
