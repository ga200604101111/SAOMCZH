package sao.zh.compat16;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** JDK-only bootstrap bridge. Local display overrides and passive, allowlisted table capture. */
public final class RuntimeBridge {
 private RuntimeBridge(){}
 private static volatile Path root;
 private static volatile String badgePlayer="YingHuang09";
 private static volatile boolean badgeEnabled=true,captureEnabled=true,translationEnabled=true,asciiEnabled=true;
 private static volatile boolean uiCaptureEnabled=true;
 private static final Set<String> uiSeen=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
 private static volatile Map<String,Map<String,String>> dictionary=Collections.emptyMap();
 private static final Set<String> missingSeen=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
 private static volatile long dictionaryModifiedTime=-1L, nextDictionaryPoll=0L;
 private static final Set<String> dailySeen=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
 private static volatile ThreadPoolExecutor writer;
 private static volatile long writtenBytes;
 private static final long MAX_EXPORT_BYTES=512L*1024*1024;
 private static final Set<String> logged=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
 private static final Set<String> rowSeen=new HashSet<>();
 private static final ThreadLocal<Boolean> inFallback=new ThreadLocal<>();
 private static final Pattern TOKENS=Pattern.compile("%(?:[0-9]+\\$)?[-+#0 ,(]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z]|%%|\\{[^{}\\r\\n]{1,80}\\}");
 private static final String[] SECTIONS={"names","messages","lines"};
 private static final String[] DICTIONARY_SECTIONS={"names","messages","lines","items"};
 private static final Set<String> itemSeen=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
 private static final ClassValue<Method> pseudoMethod=new ClassValue<Method>(){protected Method computeValue(Class<?> c){try{return c.getMethod("pseudo");}catch(Exception e){throw new IllegalStateException(e);}}};
 private static final ClassValue<Method> languageMethod=new ClassValue<Method>(){protected Method computeValue(Class<?> c){try{return c.getClassLoader().loadClass("fr.sao.util.LanguageWatch").getMethod("current");}catch(Exception e){throw new IllegalStateException(e);}}};

 public static synchronized void configure(Path base)throws Exception {
  root=base.toAbsolutePath().normalize();
  Path folder=root.resolve("SAO_TW_Dynamic");Files.createDirectories(folder.resolve("raw"));Files.createDirectories(folder.resolve("review"));
  Map<String,Object> settings=new LinkedHashMap<>();settings.put("badge_enabled",true);settings.put("badge_player","YingHuang09");settings.put("capture_tables",true);settings.put("capture_skilltree",true);settings.put("translation_enabled",true);settings.put("ascii_fallback",true);
  Path cf=folder.resolve("settings.json");if(!Files.exists(cf))atomic(cf,MiniJson.stringify(settings));
  Object config=MiniJson.parse(new String(Files.readAllBytes(cf),StandardCharsets.UTF_8));
  if(config instanceof Map){Map<?,?>m=(Map<?,?>)config;badgeEnabled=bool(m,"badge_enabled",true);captureEnabled=bool(m,"capture_tables",true);uiCaptureEnabled=bool(m,"capture_skilltree",true);translationEnabled=bool(m,"translation_enabled",true);asciiEnabled=bool(m,"ascii_fallback",true);Object player=m.get("badge_player");if(player instanceof String && ((String)player).matches("[A-Za-z0-9_]{1,16}"))badgePlayer=(String)player;}
  Path dict=folder.resolve("zh_tw.json");if(!Files.exists(dict))atomic(dict,MiniJson.stringify(defaultDictionary()));
  loadDictionary();
  if(writer==null){writer=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(4),r->{Thread t=new Thread(r,"SAO-TW-v7-table-export");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());}
  try(java.util.stream.Stream<Path> paths=Files.walk(folder.resolve("raw"))){writtenBytes=paths.filter(Files::isRegularFile).mapToLong(p->{try{return Files.size(p);}catch(IOException e){return 0;}}).sum();}
  log("FEATURES_READY badge="+badgeEnabled+" capture="+captureEnabled+" translation="+translationEnabled+" ascii="+asciiEnabled);
  GitHubTranslationClient.start(root,RuntimeBridge::loadDictionary);
 }
 private static boolean bool(Map<?,?>m,String k,boolean fallback){Object v=m.get(k);return v instanceof Boolean?(Boolean)v:fallback;}
 private static Map<String,Object> defaultDictionary(){
  Map<String,Object>d=new LinkedHashMap<>();d.put("schema_version",1);d.put("locale","zh_tw");d.put("policy","exact reviewed matches; unknown text stays unchanged");
  Map<String,String> n=new LinkedHashMap<>();n.put("Corrupted Boar","腐化野豬");n.put("Forêts du 1er Palier","第一層森林");n.put("Monstres","怪物");n.put("Assassin Path","刺客之道");n.put("Assassin","刺客");d.put("names",n);
  Map<String,String>l=new LinkedHashMap<>();l.put("The center of the tree.","技能樹的核心。");l.put("A wild beast from the forests of the first Floor. It charges relentlessly, driven by primal rage.","來自第一層森林的野獸。受到原始狂怒驅使，牠會猛烈衝撞，毫不退縮。");d.put("lines",l);d.put("messages",new LinkedHashMap<String,String>());d.put("items",new LinkedHashMap<String,String>());return d;
 }
 @SuppressWarnings("unchecked")
 public static synchronized void loadDictionary(){
  if(root==null)return;
  try{
   Path p=root.resolve("SAO_TW_Dynamic/zh_tw.json");if(Files.size(p)>8*1024*1024)throw new IOException("Dictionary too large");
   Object v=MiniJson.parse(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));if(!(v instanceof Map))throw new IOException("Expected dictionary object");
   Map<String,Map<String,String>>next=new HashMap<>();int count=0;
   for(String section:DICTIONARY_SECTIONS){Map<String,String>m=new HashMap<>();Object items=((Map<?,?>)v).get(section);if(items instanceof Map)for(Map.Entry<?,?>e:((Map<?,?>)items).entrySet()){
    if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String))throw new IOException("Invalid dictionary entry");String a=(String)e.getKey(),b=(String)e.getValue();
    if(a.isEmpty()||b.isEmpty()||a.length()>16384||b.length()>16384)continue;
    if(!tokens(a).equals(tokens(b)))throw new IOException("Placeholder mismatch for "+stableId(section,a));
    m.put(a,b);count++;
   }next.put(section,Collections.unmodifiableMap(m));}
   Map<String,Map<String,String>>cloud=GitHubTranslationClient.cached(root,"zh_tw");
   for(String sec:DICTIONARY_SECTIONS){
    Map<String,String>merged=new HashMap<>(next.get(sec));
    Map<String,String>fromCloud=cloud.get(sec);
    if(fromCloud!=null)merged.putAll(fromCloud);
    next.put(sec,Collections.unmodifiableMap(merged));
   }
   count=0;for(Map<String,String> section:next.values())count+=section.size();
   dictionary=Collections.unmodifiableMap(next);dictionaryModifiedTime=Files.getLastModifiedTime(p).toMillis();
   log("DICTIONARY_READY entries="+count+" github_entries="+cloud.values().stream().mapToInt(Map::size).sum());
  }catch(Throwable e){once("dictionary","DICTIONARY_ERROR retaining_previous "+e.getClass().getSimpleName());}
 }
 /** Pull reviewed updates without restarting the game; at most once per three seconds. */
 private static void maybeReloadDictionary(){
  if(root==null)return;
  long now=System.nanoTime();
  if(now<nextDictionaryPoll)return;
  synchronized(RuntimeBridge.class){
   now=System.nanoTime();if(now<nextDictionaryPoll)return;
   nextDictionaryPoll=now+TimeUnit.SECONDS.toNanos(3);
   try{
    Path file=root.resolve("SAO_TW_Dynamic/zh_tw.json");
    if(Files.getLastModifiedTime(file).toMillis()!=dictionaryModifiedTime)loadDictionary();
   }catch(Throwable e){once("reload-dictionary","DICTIONARY_POLL_SKIPPED "+e.getClass().getSimpleName());}
  }
 }
 public static String lookup(String text,String section,String language){
  if(!translationEnabled||text==null||!"zh_tw".equalsIgnoreCase(language))return text;
  maybeReloadDictionary();
  String value;
  if("items".equals(section))value=itemReviewedLookup(text);
  else {Map<String,String>m=dictionary.get(section);value=m==null?null:m.get(text);}
  if(value==null)return text;
  return value.replace('並','\ue900').replace('中','\ue901').replace('串','\ue902');
 }

 private static final Pattern ITEM_FORMAT=Pattern.compile("(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])");
 private static String itemExact(String value){
  for(String section:new String[]{"items","names","lines"}){
   Map<String,String>map=dictionary.get(section);String next=map==null?null:map.get(value);
   if(next!=null)return next;
  }
  return null;
 }
 /** Item-only fallback. Never partial-replace prose or rewrite chat. Preserve outer formatting. */
 private static String itemReviewedLookup(String text){
  String exact=itemExact(text);if(exact!=null)return exact;
  int left=0,right=text.length();
  while(left<right&&Character.isWhitespace(text.charAt(left)))left++;
  Matcher m=ITEM_FORMAT.matcher(text);
  while(left<right){m.region(left,right);if(!m.lookingAt())break;left=m.end();}
  while(right>left&&Character.isWhitespace(text.charAt(right-1)))right--;
  boolean more=true;
  while(more&&right>left){more=false;m.region(left,right);while(m.find())if(m.end()==right){right=m.start();more=true;break;}}
  if(left==0&&right==text.length())return null;
  String translated=itemExact(text.substring(left,right));
  return translated==null?null:text.substring(0,left)+translated+text.substring(right);
 }
 public static String itemString(String value,Class<?>source){
  String result=translateString(value,source,"items");
  if(value!=null&&value.equals(result))captureMissing(value,"items","item_name");
  return result;
 }
 public static Object itemText(Object value,Class<?>source){
  Object result=translateText(value,source,"items");
  if(value!=null&&result==value)try{
   Class<?> t=source.getClassLoader().loadClass("net.minecraft.class_2561");
   captureMissing(String.valueOf(t.getMethod("getString").invoke(value)),"items","item_lore");
  }catch(Throwable ignored){}
  return result;
 }
 /** Called after native lore processing, so native caches remain untranslated and updateable. */
 @SuppressWarnings({"rawtypes","unchecked"})
 public static void itemTooltip(java.util.List lines,Class<?>source){
  if(lines==null||source==null)return;
  try{
   if(!translationEnabled||!"zh_tw".equalsIgnoreCase(String.valueOf(languageMethod.get(source).invoke(null))))return;
   for(int i=0;i<lines.size();i++){
    Object old=lines.get(i);if(old==null)continue;
    Object next=itemText(old,source);if(next!=old)lines.set(i,next);
   }
  }catch(Throwable e){once("item-tooltip","ITEM_TOOLTIP_TRANSLATION_SKIPPED "+e.getClass().getSimpleName());}
 }

 private static final String[] PUBLIC_MISSING_SURFACES={"equipment_stat","skill_name","skill_desc","skill_stat","skill_lore","mob_name","bossbar_title","scoreboard_title","actionbar_system","item_name","item_lore"};
 /** Never collect chats, names, identifiers, network addresses, or already-localized text. */
 public static boolean safeMissing(String text,String origin){
  if(text==null||origin==null||text.length()<3||text.length()>480)return false;
  if(Arrays.binarySearch(sortedMissingOrigins(),origin)<0)return false;
  if(CjkLayout.containsCjk(text))return false;
  if(text.indexOf('@')>=0||text.contains("://")||text.indexOf('§')>=0||text.indexOf('»')>=0
       ||text.indexOf('‹')>=0||text.indexOf('<')>=0||text.indexOf('>')>=0||text.indexOf('\\')>=0)return false;
  if(text.matches("(?s).*[0-9]{1,3}(\\.[0-9]{1,3}){3}.*")
      ||text.matches("(?is).*\\b[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\b.*")
      ||text.matches("(?s).*[-+]?[0-9]{3,}[, ][-+]?[0-9]{3,}.*"))return false;
  if(text.matches("(?s).*\\[[^]]{0,20}]\\s*\\w{3,16}\\s*[:：].*"))return false;
  int letters=0;
  for(int i=0;i<text.length();){int ch=text.codePointAt(i);i+=Character.charCount(ch);if(Character.isLetter(ch))letters++;}
  if(letters<3)return false;
  if("mob_name".equals(origin)&&!text.matches("(?s).*\\s+.*"))return false; // Single-word names might identify a player.
  return true;
 }
 private static final String[] PUBLIC_MISSING_SORTED=PUBLIC_MISSING_SURFACES.clone();
 static {Arrays.sort(PUBLIC_MISSING_SORTED);}
 private static String[] sortedMissingOrigins(){return PUBLIC_MISSING_SORTED;}
 private static void captureMissing(String raw,String section,String origin){
  if(!captureEnabled||writer==null||root==null||!safeMissing(raw,origin)||missingSeen.size()>=30000)return;
  String id=stableId(section,raw),key=origin+"\u0000"+id;
  if(!missingSeen.add(key))return;
  try{
   writer.execute(()->{
    try{
     Path output=root.resolve("SAO_TW_Dynamic/review/missing_zh_tw.jsonl");
     if(Files.exists(output)&&Files.size(output)>12L*1024*1024)return;
     Map<String,Object>m=new LinkedHashMap<>();
     m.put("schema_version",1);m.put("locale","zh_tw");m.put("id",id);
     m.put("source_key",raw);m.put("section",section);m.put("origin",origin);
     m.put("source_language","undetermined");m.put("zh_tw","");m.put("status","unreviewed");m.put("placeholders",tokens(raw));
     Files.write(output,(MiniJson.stringify(m)+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
     once("missing-surface","MISSING_TRANSLATION_CAPTURE_ACTIVE review/missing_zh_tw.jsonl");
    }catch(Throwable e){missingSeen.remove(key);once("missing-write","MISSING_CAPTURE_ERROR "+e.getClass().getSimpleName());}
   });
  }catch(RejectedExecutionException e){missingSeen.remove(key);}
 }
 public static String localizeUiString(String text,Class<?> source,String section,String origin){
  if(text==null)return null;
  String result=translateString(text,source,section);
  if(result.equals(text)&&text.indexOf('\n')>=0){
   String[] segments=text.split("\\n",-1);boolean any=false;
   for(int i=0;i<segments.length;i++){
    String translated=translateString(segments[i],source,section);
    if(!segments[i].equals(translated)){any=true;segments[i]=translated;}
    else if("lines".equals(section))captureMissing(segments[i],section,origin);
   }
   if(any)result=String.join("\n",segments);
  }else if(result.equals(text))captureMissing(text,section,origin);
  return result;
 }
 public static Object localizeUiText(Object value,Class<?> source,String section,String origin){
  if(value==null)return null;
  Object translated=translateText(value,source,section);
  if(translated==value&&source!=null){
   try{
    Class<?> type=source.getClassLoader().loadClass("net.minecraft.class_2561");
    String raw=String.valueOf(type.getMethod("getString").invoke(value));
    captureMissing(raw,section,origin);
   }catch(Throwable ignored){}
  }
  return translated;
 }
 public static java.util.List<String> localizeLore(java.util.List<String>original,Class<?> source){
  if(original==null)return null;
  List<String>out=new ArrayList<>(original.size());boolean changed=false;
  for(String item:original){
   if(item==null){out.add(null);continue;}
   int i=0;while(i+1<item.length()&&(item.charAt(i)=='&'||item.charAt(i)=='§')
       &&"0123456789abcdefklmnor".indexOf(Character.toLowerCase(item.charAt(i+1)))>=0)i+=2;
   String translated=localizeUiString(item.substring(i),source,"lines","skill_lore");
   String result=item.substring(0,i)+translated;out.add(result);changed|=!result.equals(item);
  }
  return changed?Collections.unmodifiableList(out):original;
 }
 public static String[] localizeMobParts(String[]parts,Class<?> source){
  if(parts==null)return null;
  String[]out=parts.clone();boolean changed=false;
  for(int i=0;i<out.length;i++)if(out[i]!=null){
   String result=localizeUiString(out[i],source,"names","mob_name");
   changed|=!result.equals(out[i]);out[i]=result;
  }
  return changed?out:parts;
 }
 private static boolean probablyPlayerChat(String body){
  if(body==null||body.contains("»")||body.contains("›")||body.matches("(?s)^\\s*<[^>]{1,32}>.*"))return true;
  if(body.matches("(?s)^\\s*(?:\\[[^]]{1,25}])?\\s*[A-Za-z0-9_]{3,16}\\s*[:：].*"))return true;
  return false;
 }
 /** Only unsigned, explicitly-reviewed system lines; never capture unknown chat or rewrite signed player messages. */
 public static Object systemChat(Object value,Object signature,Object indicator){
  if(value==null||signature!=null||indicator==null)return value;
  try{
   Class<?> t=value.getClass().getClassLoader().loadClass("net.minecraft.class_2561");
   String original=String.valueOf(t.getMethod("getString").invoke(value));
   if(probablyPlayerChat(original)||CjkLayout.containsCjk(original))return value;
   if(!"zh_tw".equalsIgnoreCase(String.valueOf(languageMethod.get(value.getClass()).invoke(null))))return value;
   String translated=lookup(original,"messages","zh_tw");
   if(translated.equals(original))return value;
   return translateTree(value,value.getClass().getClassLoader(),"messages","zh_tw",0);
  }catch(Throwable e){return value;}
 }

 public static String translateString(String text,Class<?> context,String section){
  if(text==null||context==null)return text;
  try{return lookup(text,section,String.valueOf(languageMethod.get(context).invoke(null)));}
  catch(Throwable e){once("translate-language","LANGUAGE_LOOKUP_FAILED "+e.getClass().getSimpleName());return text;}
 }
 /** Translate literal leaves only; style/click/hover/siblings preserved by cloning. */
 public static Object translateText(Object value,Class<?> context,String section){
  if(value==null||context==null)return value;
  try{
   String lang=String.valueOf(languageMethod.get(context).invoke(null));if(!translationEnabled||!"zh_tw".equalsIgnoreCase(lang))return value;
   return translateTree(value,context.getClassLoader(),section,lang,0);
  }catch(Throwable e){once("translate-text","TEXT_TRANSLATION_SKIPPED "+e.getClass().getSimpleName());return value;}
 }
 private static Object translateTree(Object text,ClassLoader cl,String section,String lang,int depth)throws Exception {
  if(depth>32)return text;
  Class<?> t=cl.loadClass("net.minecraft.class_2561"),m=cl.loadClass("net.minecraft.class_5250"),st=cl.loadClass("net.minecraft.class_2583");
  Object content=t.getMethod("method_10851").invoke(text);
  Class<?> plain=cl.loadClass("net.minecraft.class_8828");
  String literal=plain.isInstance(content)?(String)plain.getMethod("comp_737").invoke(content):null;
  List<?> siblings=(List<?>)t.getMethod("method_10855").invoke(text);
  String next=literal==null?null:lookup(literal,section,lang);boolean changed=literal!=null&&!literal.equals(next);
  List<Object> children=new ArrayList<>(siblings.size());for(Object child:siblings){Object n=translateTree(child,cl,section,lang,depth+1);children.add(n);changed|=n!=child;}
  if(!changed)return text;
  Object copy=literal!=null?t.getMethod("method_43470",String.class).invoke(null,next):t.getMethod("method_27662").invoke(text);
  m.getMethod("method_10862",st).invoke(copy,t.getMethod("method_10866").invoke(text));
  for(Object child:children)m.getMethod("method_10852",t).invoke(copy,child);
  return copy;
 }
 private static final UUID TARGET_UUID=UUID.fromString("eddeac86-5a32-4205-b22e-9a12b30db4df");
 private static final String TARGET_NAME="YingHuang09";
 private static final String OLD_PLAYER_ICON="\ue07d", NEW_RANK_ICON="\ue9f0";
 /** Scoped identity check for real TabEntry and player entity objects. */
 public static boolean matchesUuid(Object entry){
  if(entry==null)return false;
  try{
   Method method;
   try{method=entry.getClass().getMethod("uuid");}
   catch(NoSuchMethodException e){method=entry.getClass().getMethod("method_5667");}
   Object val=method.invoke(entry);
   return TARGET_UUID.equals(val);
  }catch(Throwable error){once("uuid-reflect","RANK_UUID_LOOKUP_FAILED "+error.getClass().getSimpleName());return false;}
 }
 public static String tabPrefix(String original,Object entry){
  if(!badgeEnabled||original==null||entry==null||!matchesUuid(entry)||original.indexOf('\ue07d')<0)return original;
  // Never override an already elevated HERO, CHAMPION or admin badge.
  String result=original.replace(OLD_PLAYER_ICON,NEW_RANK_ICON);
  if(!result.equals(original))once("rank-tab","LOCAL_BADGE_ACTIVE player="+TARGET_UUID+" surface=tab");
  return result;
 }
 /** Conservative bounded exact prefix matcher; never rewrites arbitrary chat mentions. */
 public static String previewChat(String line,boolean verifiedRoster){
  if(line==null||!badgeEnabled||!verifiedRoster||line.indexOf('\ue07d')<0)return line;
  int pos=line.indexOf('\ue07d');
  if(pos>64)return line;
  String preceding=line.substring(0,pos);
  // Only system style country glyph plus optional [server/group] prefix are accepted.
  String withoutCodes=preceding.replaceAll("(?i)§[0-9a-fk-or]","");
  if(!withoutCodes.matches("(?s)[\\ue000-\\uf8ff ]{0,3}(?:\\[[^\\]\\r\\n]{1,32}\\]\\s*)?"))return line;
  String tail=line.substring(pos+1);
  if(!tail.matches("(?s)^\\s*YingHuang09\\s*(?:[:：»]|\\u00bb).*"))return line;
  return line.substring(0,pos)+NEW_RANK_ICON+tail;
 }

 /** Make the existing main-menu flag button cycle through native zh_tw. */
 @SuppressWarnings({"rawtypes","unchecked"})
 public static List extendLanguages(List original,Class<?> selector){
  if(original==null||selector==null)return original;
  try{
   ClassLoader cl=selector.getClassLoader();
   for(Object entry:original){
    if("zh_tw".equals(String.valueOf(entry.getClass().getMethod("code").invoke(entry))))return original;
   }
   Method flag=selector.getDeclaredMethod("flag",String.class);
   flag.setAccessible(true);
   Object resource=flag.invoke(null,"tw");
   Class<?> cls=cl.loadClass("fr.sao.triggersmod.screen.LanguageSelector$Entry");
   Class<?> identifier=cl.loadClass("net.minecraft.class_2960");
   Object taiwan=cls.getConstructor(String.class,identifier,String.class).newInstance("zh_tw",resource,"繁體中文（台灣）");
   ArrayList copy=new ArrayList(original);copy.add(taiwan);
   once("language-button","LANGUAGE_BUTTON_ADDED zh_tw using original LanguageSelector.apply");
   return Collections.unmodifiableList(copy);
  }catch(Throwable e){once("language-button-error","LANGUAGE_BUTTON_SKIPPED "+e.getClass().getSimpleName());return original;}
 }
 /** Confirm the target UUID is actually assigned to the expected username by this server. */
 private static boolean verifiedRoster(ClassLoader cl){
  try{
   Class<?> mc=Class.forName("net.minecraft.class_310",false,cl);
   Object client=mc.getMethod("method_1551").invoke(null);
   if(client==null)return false;
   Object net=mc.getMethod("method_1562").invoke(client);
   if(net==null)return false;
   Object p=net.getClass().getMethod("method_2871",UUID.class).invoke(net,TARGET_UUID);
   if(p==null)return false;
   Object profile=p.getClass().getMethod("method_2966").invoke(p);
   String name=String.valueOf(profile.getClass().getMethod("getName").invoke(profile));
   return TARGET_NAME.equals(name);
  }catch(Throwable e){once("roster-verify","RANK_ROSTER_CHECK_FAILED "+e.getClass().getSimpleName());return false;}
 }
 /** Replace only plain text leaf containing the server's PLAYER glyph, preserving styles and all siblings. */
 private static Object replaceGlyphTree(Object text,ClassLoader cl,boolean[] used,int depth)throws Exception{
  if(text==null||depth>24||used[0])return text;
  Class<?> t=cl.loadClass("net.minecraft.class_2561");
  Class<?> plain=cl.loadClass("net.minecraft.class_8828");
  Object contents=t.getMethod("method_10851").invoke(text);
  String literal=plain.isInstance(contents)?(String)plain.getMethod("comp_737").invoke(contents):null;
  List<?> siblings=(List<?>)t.getMethod("method_10855").invoke(text);
  String next=literal;
  boolean changed=false;
  if(literal!=null && literal.indexOf('\ue07d')>=0 && !used[0]){
   next=literal.replaceFirst("\ue07d",NEW_RANK_ICON);
   changed=!literal.equals(next);
   if(changed)used[0]=true;
  }
  List<Object> children=new ArrayList<Object>();
  for(Object child:siblings){
   Object n=replaceGlyphTree(child,cl,used,depth+1);children.add(n);changed|=(child!=n);
  }
  if(!changed)return text;
  Object copy=literal!=null?t.getMethod("method_43470",String.class).invoke(null,next):t.getMethod("method_27662").invoke(text);
  Class<?> mutable=cl.loadClass("net.minecraft.class_5250"),style=cl.loadClass("net.minecraft.class_2583");
  mutable.getMethod("method_10862",style).invoke(copy,t.getMethod("method_10866").invoke(text));
  for(Object child:children)mutable.getMethod("method_10852",t).invoke(copy,child);
  return copy;
 }
 /** The UUID comes from the actual player entity, never from a display-name substring. */
 public static void nameplateState(Object player,Object state){
  if(!badgeEnabled||!matchesUuid(player)||state==null)return;
  try{
   Field f=state.getClass().getField("field_53337");
   Object text=f.get(state);
   if(text==null)return;
   ClassLoader cl=state.getClass().getClassLoader();
   Class<?> t=cl.loadClass("net.minecraft.class_2561");
   if(!t.isInstance(text))return;
   String original=String.valueOf(t.getMethod("getString").invoke(text));
   if(!original.contains(TARGET_NAME)||original.contains(NEW_RANK_ICON))return;
   Object changed;
   if(original.indexOf('\ue07d')>=0)changed=replaceGlyphTree(text,cl,new boolean[1],0);
   else {
    // Do not overwrite any other official rank.
    for(char ch=0xe076;ch<=0xe081;ch++)if(original.indexOf(ch)>=0)return;
    Object prefix=t.getMethod("method_43470",String.class).invoke(null,NEW_RANK_ICON+" ");
    cl.loadClass("net.minecraft.class_5250").getMethod("method_10852",t).invoke(prefix,text);
    changed=prefix;
   }
   f.set(state,changed);
   once("rank-nameplate","LOCAL_BADGE_ACTIVE player="+TARGET_UUID+" surface=nameplate");
  }catch(Throwable e){once("nameplate-error","NAMEPLATE_SKIPPED "+e.getClass().getSimpleName());}
 }
 /** Chat is display-only, not packet or signed message data. */
 public static Object chatText(Object value){
  if(!badgeEnabled||value==null||!verifiedRoster(value.getClass().getClassLoader()))return value;
  try{
   ClassLoader cl=value.getClass().getClassLoader();
   Class<?> t=cl.loadClass("net.minecraft.class_2561");
   if(!t.isInstance(value))return value;
   String before=String.valueOf(t.getMethod("getString").invoke(value));
   if(previewChat(before,true).equals(before))return value;
   Object next=replaceGlyphTree(value,cl,new boolean[1],0);
   if(next!=value)once("rank-chat","LOCAL_BADGE_ACTIVE player="+TARGET_UUID+" surface=chat");
   return next;
  }catch(Throwable e){once("rank-chat-error","CHAT_PREFIX_SKIPPED "+e.getClass().getSimpleName());return value;}
 }
 public static String grade(String original,Object state){
  if(!badgeEnabled||state==null)return original;
  try{Object name=pseudoMethod.get(state.getClass()).invoke(state);if(badgePlayer.equals(name)){once("badge","LOCAL_BADGE_ACTIVE player="+badgePlayer+" surface=equipment-grade");return "\ue9f0";}}
  catch(Throwable e){once("badge-error","BADGE_SKIPPED "+e.getClass().getSimpleName());}
  return original;
 }

 /** Capture reward description visible in the Daily UI, then translate reviewed exact matches. */
 public static String dailyRewardDescription(String value,Class<?> source){
  if(value==null)return null;
  if(captureEnabled&&root!=null&&writer!=null&&value.length()<=4096&&!value.trim().isEmpty()){
   String marker=stableId("daily-reward-desc",value);
   if(dailySeen.size()<20000&&dailySeen.add(marker)){
    try{
     writer.execute(()->{
      try{
       Path f=root.resolve("SAO_TW_Dynamic/review/daily_seen.jsonl");
       if(Files.exists(f)&&Files.size(f)>32L*1024*1024)return;
       Map<String,Object> row=new LinkedHashMap<>();
       row.put("schema_version",1);row.put("id",stableId("lines",value));
       row.put("section","lines");row.put("source_key",value);
       row.put("origin","SAOUI.DailyModel.Reward.desc");row.put("source_language","undetermined");
       row.put("zh_tw","");row.put("status","unreviewed");
       row.put("placeholders",tokens(value));row.put("notes","");
       Files.write(f,(MiniJson.stringify(row)+"\n").getBytes(StandardCharsets.UTF_8),
             StandardOpenOption.CREATE,StandardOpenOption.APPEND);
       once("daily-captured","DAILY_REWARD_DESCRIPTION_CAPTURED review/daily_seen.jsonl");
      }catch(Throwable e){dailySeen.remove(marker);once("daily-capture-error","DAILY_CAPTURE_FAILED "+e.getClass().getSimpleName());}
     });
    }catch(RejectedExecutionException e){dailySeen.remove(marker);}
   }
  }
  return translateString(value,source,"lines");
 }

 /** Only allowlisted native translation table fields are copied; no packets or player chat are read. */
 public static void snapshot(Object table){snapshot(table,"assembled");}
 public static void snapshot(Object table,String event){
  if(!captureEnabled||table==null||writer==null)return;
  try{
   Map<String,Object>copy=new LinkedHashMap<>();copy.put("schema_version",1);copy.put("origin","SAOUI.LangTable");copy.put("event",event);copy.put("captured_at",Instant.now().toString());
   copy.put("served_language",String.valueOf(field(table,"lang")));copy.put("table_version",String.valueOf(field(table,"version")));
   Map<String,Object>sections=new LinkedHashMap<>();int count=0;long chars=0;
   for(String s:SECTIONS){Object object=field(table,s);if(!(object instanceof Map))throw new IllegalArgumentException("Table schema changed");Map<String,String>entries=new TreeMap<>();
    for(Map.Entry<?,?>e:((Map<?,?>)object).entrySet()){
     if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String))continue;
     String k=(String)e.getKey(),v=(String)e.getValue();count++;chars+=k.length()+v.length();
     if(count>100000||chars>16*1024*1024||k.length()>65536||v.length()>65536)throw new IllegalArgumentException("Table export size limit");
     entries.put(k,v);
    }sections.put(s,entries);
   }
   copy.put("entry_count",count);copy.put("sections",sections);
   writer.execute(()->{try{export(copy);}catch(Throwable e){once("export-error","TABLE_EXPORT_ERROR "+e.getClass().getSimpleName());}});
  }catch(RejectedExecutionException e){log("TABLE_EXPORT_QUEUE_FULL snapshot_skipped");}
   catch(Throwable e){once("capture-error","TABLE_CAPTURE_ERROR "+e.getClass().getSimpleName());}
 }
 private static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 @SuppressWarnings("unchecked")
 private static void export(Map<String,Object>data)throws Exception {
  Map<String,Object>sections=(Map<String,Object>)data.get("sections");String lang=String.valueOf(data.get("served_language")),version=String.valueOf(data.get("table_version"));
  String fingerprint=sha(lang+"\u0000"+version+"\u0000"+MiniJson.stringify(sections));
  Path base=root.resolve("SAO_TW_Dynamic");Path raw=base.resolve("raw").resolve(lang.replaceAll("[^A-Za-z0-9_-]","_")+"_"+fingerprint.substring(0,24)+".json");
  if(Files.exists(raw))return;
  String serialized=MiniJson.stringify(data);long estimate=serialized.getBytes(StandardCharsets.UTF_8).length;
  if(writtenBytes+estimate>MAX_EXPORT_BYTES){once("quota","TABLE_EXPORT_QUOTA_REACHED 512MiB raw_limit");return;}
  atomic(raw,serialized);writtenBytes+=estimate;
  StringBuilder lines=new StringBuilder();int rows=0;
  for(String section:SECTIONS)for(Map.Entry<String,String>e:((Map<String,String>)sections.get(section)).entrySet()){
   String id=stableId(section,e.getKey());String variant=sha(id+"\u0000"+lang+"\u0000"+e.getValue());if(!rowSeen.add(variant))continue;
   Map<String,Object>r=new LinkedHashMap<>();r.put("id",id);r.put("section",section);r.put("source_key",e.getKey());r.put("source_language","undetermined");r.put("server_text",e.getValue());r.put("served_language",lang);r.put("table_version",version);r.put("snapshot",raw.getFileName().toString());r.put("zh_tw","");r.put("status","unreviewed");r.put("placeholders",tokens(e.getValue()));r.put("notes","");lines.append(MiniJson.stringify(r)).append('\n');rows++;
  }
  if(lines.length()>0)Files.write(base.resolve("review/entries.jsonl"),lines.toString().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  log("TABLE_CAPTURED lang="+lang+" entries="+data.get("entry_count")+" new_review_rows="+rows+" snapshot="+raw.getFileName());
 }
 /** Capture only three server-fed MapNode fields at the known skill-tree UI call site. */
 private static final class LayoutAccess {
  final Method literal,measure;
  LayoutAccess(Class<?> context)throws Exception {
   ClassLoader cl=context.getClassLoader();
   Class<?> text=cl.loadClass("net.minecraft.class_2561");
   Class<?> renderer=cl.loadClass("net.minecraft.class_327");
   literal=text.getMethod("method_43470",String.class);
   measure=cl.loadClass("com.eighteen.saojob.text.TextUtil").getMethod("measureWidth",renderer,text,float.class,String.class);
  }
  double width(Object renderer,String s,int height){
   try{return ((Number)measure.invoke(null,renderer,literal.invoke(null,s),(float)height,"worksans-medium")).doubleValue();}
   catch(Exception e){throw new IllegalStateException(e);}
  }
 }
 private static final ClassValue<LayoutAccess> layoutAccess=new ClassValue<LayoutAccess>(){
  protected LayoutAccess computeValue(Class<?> c){try{return new LayoutAccess(c);}catch(Exception e){throw new IllegalStateException(e);}}
 };
 /** Called before native popup height is computed. Never alters MapNode or server data. */
 public static List<String> wrapHoverDescription(List<String> original,String text,Object renderer,int width,int height,Class<?> context){
  if(original==null||renderer==null||context==null||!CjkLayout.containsCjk(text)||width<=0||height<=0)return original;
  try{
   final LayoutAccess access=layoutAccess.get(context);
   List<String> result=CjkLayout.wrap(text,Math.max(1,width-1),line->access.width(renderer,line,height));
   once("cjk-hover-layout","CJK_HOVER_LAYOUT_ACTIVE measured_font=worksans-medium");
   return result;
  }catch(Throwable e){once("cjk-hover-layout-failure","CJK_HOVER_LAYOUT_SKIPPED "+e.getClass().getSimpleName());return original;}
 }
 public static String observeSkilltree(String text,String fieldName){
  if(!uiCaptureEnabled||root==null||writer==null||text==null||text.trim().isEmpty()||text.length()>8192)return text;
  if(!("displayName".equals(fieldName)||"category".equals(fieldName)||"description".equals(fieldName)))return text;
  final String section="description".equals(fieldName)?"lines":"names";
  final String dedup=fieldName+"\u0000"+text;
  if(uiSeen.contains(dedup))return text;
  if(uiSeen.size()>=20000){once("ui-count-limit","UI_CAPTURE_LIMIT_REACHED entries=20000");return text;}
  if(!uiSeen.add(dedup))return text;
  try{
   writer.execute(()->{
    try{
     Path file=root.resolve("SAO_TW_Dynamic/review/ui_seen.jsonl");
     if(Files.exists(file)&&Files.size(file)>=32L*1024*1024){once("ui-disk-limit","UI_CAPTURE_LIMIT_REACHED disk=32MiB");return;}
     Map<String,Object> row=new LinkedHashMap<>();
     row.put("schema_version",1);row.put("id",stableId(section,text));row.put("section",section);
     row.put("source_key",text);row.put("original_text",text);row.put("source_language","undetermined");
     row.put("origin","saojob.MapNode");row.put("field",fieldName);row.put("captured_at",Instant.now().toString());
     row.put("zh_tw","");row.put("status","unreviewed");row.put("placeholders",tokens(text));row.put("notes","");
     Files.write(file,(MiniJson.stringify(row)+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
     once("ui-first-capture","UI_TEXT_CAPTURED origin=saojob.MapNode file=review/ui_seen.jsonl");
    }catch(Throwable e){uiSeen.remove(dedup);once("ui-write","UI_CAPTURE_ERROR "+e.getClass().getSimpleName());}
   });
  }catch(RejectedExecutionException e){uiSeen.remove(dedup);once("ui-queue","UI_CAPTURE_QUEUE_FULL retry_on_next_render");}
   catch(Throwable e){uiSeen.remove(dedup);once("ui-error","UI_CAPTURE_ERROR "+e.getClass().getSimpleName());}
  return text;
 }

 /** Passive read-only snapshot of only SaoUI's already-received item translation cache. */
 public static void snapshotItemlang(Class<?> source){
  if(!captureEnabled||writer==null||root==null||source==null||itemSeen.size()>100000)return;
  List<String>added=new ArrayList<>();
  try{
   Field store=source.getDeclaredField("BY_LANG");store.setAccessible(true);Object by=store.get(null);
   if(!(by instanceof Map))return;
   Field sentinel=source.getDeclaredField("NONE");sentinel.setAccessible(true);Object none=sentinel.get(null);
   List<Map<String,Object>>batch=new ArrayList<>();
   for(Map.Entry<?,?>langEntry:((Map<?,?>)by).entrySet()){
    Object items=langEntry.getValue();if(items==null)continue;
    Object mapped=field(items,"known");if(!(mapped instanceof Map))continue;
    String lang=String.valueOf(langEntry.getKey());
    try{Object served=field(items,"served");if(served instanceof String&&!((String)served).isEmpty())lang=(String)served;}catch(ReflectiveOperationException ignored){}
    for(Map.Entry<?,?>entry:((Map<?,?>)mapped).entrySet()){
     if(!(entry.getKey() instanceof String)||entry.getValue()==null||entry.getValue()==none)continue;
     Object raw;
     try{raw=field(entry.getValue(),"raw");}catch(NoSuchFieldException unknownEntry){continue;}
     if(!(raw instanceof String))continue;
     String original=(String)entry.getKey(),translated=(String)raw;
     if(original.isEmpty()||original.length()>4096||translated.length()>4096)continue;
     String marker=lang+"\u0000"+original+"\u0000"+translated;
     if(!itemSeen.add(marker))continue;added.add(marker);
     Map<String,Object>row=new LinkedHashMap<>();row.put("schema_version",1);row.put("id",stableId("items",original));
     row.put("section","items");row.put("source_key",original);row.put("source_language","undetermined");
     row.put("served_language",lang);row.put("server_text",translated);row.put("origin","SAOUI.ItemLangClient");
     row.put("zh_tw","");row.put("status","unreviewed");row.put("placeholders",tokens(translated));row.put("notes","");batch.add(row);
     if(batch.size()>=500||itemSeen.size()>=100000)break;
    }
    if(batch.size()>=500)break;
   }
   if(batch.isEmpty())return;
   writer.execute(()->{
    try{
     Path f=root.resolve("SAO_TW_Dynamic/review/item_seen.jsonl");
     if(Files.exists(f)&&Files.size(f)>=64L*1024*1024){itemSeen.removeAll(added);return;}
     StringBuilder output=new StringBuilder();for(Map<String,Object>r:batch)output.append(MiniJson.stringify(r)).append('\n');
     Files.write(f,output.toString().getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);
     once("item-capture","ITEM_TRANSLATION_CAPTURED file=review/item_seen.jsonl");
    }catch(Throwable e){itemSeen.removeAll(added);once("item-capture-error","ITEM_TRANSLATION_CAPTURE_FAILED "+e.getClass().getSimpleName());}
   });
  }catch(RejectedExecutionException e){itemSeen.removeAll(added);once("item-queue","ITEM_TRANSLATION_QUEUE_FULL");}
   catch(Throwable e){itemSeen.removeAll(added);once("item-cache-error","ITEM_TRANSLATION_CACHE_ERROR "+e.getClass().getSimpleName());}
 }
 public static void awaitWriter()throws Exception{ThreadPoolExecutor w=writer;if(w!=null)w.submit(()->{}).get(15,TimeUnit.SECONDS);}
 public static String stableId(String section,String key){return sha(section+"\u0000"+key).substring(0,24);}
 private static String sha(String s){try{byte[]b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));char[] digits="0123456789abcdef".toCharArray();char[] out=new char[b.length*2];for(int i=0;i<b.length;i++){out[2*i]=digits[(b[i]>>>4)&15];out[2*i+1]=digits[b[i]&15];}return new String(out);}catch(Exception e){throw new IllegalStateException(e);}}
 private static List<String> tokens(String s){ArrayList<String>r=new ArrayList<>();Matcher m=TOKENS.matcher(s);while(m.find())r.add(m.group());Collections.sort(r);return r;}
 private static void atomic(Path path,String text)throws IOException{Files.createDirectories(path.getParent());Path temp=Files.createTempFile(path.getParent(),".sao-tw-",".tmp");try{Files.write(temp,text.getBytes(StandardCharsets.UTF_8));try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}

 /** Called only from Minecraft's actual missing-glyph return branch. */
 public static Object missingGlyph(Object original,Object storage,int codepoint){
  String id=fontId(storage);once("glyph-"+id+"-"+codepoint,"MISSING_GLYPH font="+id+" codepoint="+String.format(Locale.ROOT,"U+%04X",codepoint));
  if(!asciiEnabled||codepoint<32||codepoint>126||Boolean.TRUE.equals(inFallback.get())||id.contains("negative")||id.contains("space"))return original;
  try{inFallback.set(true);Object safe=safeStorage(storage);if(safe==null||safe==storage)return original;
   Method get=safe.getClass().getMethod("method_2014",int.class);Object result=get.invoke(safe,codepoint);
   Object missing=field(safe,"field_2256");if(result!=null&&result!=missing){once("fallback-"+id+"-"+codepoint,"ASCII_FALLBACK_USED font="+id+" codepoint="+codepoint);return result;}
  }catch(Throwable e){once("glyph-fallback-error","GLYPH_FALLBACK_FAILED "+e.getClass().getSimpleName());}finally{inFallback.remove();}
  return original;
 }
 public static Object missingMetrics(Object original,Object storage,int codepoint,boolean validated){
  if(!asciiEnabled||original==null||codepoint<32||codepoint>126||Boolean.TRUE.equals(inFallback.get()))return original;
  if(!original.getClass().getName().startsWith("net.minecraft.class_7191"))return original;
  try{ClassLoader cl=storage.getClass().getClassLoader();Class<?> missing=cl.loadClass("net.minecraft.class_7191");if(original!=missing.getField("field_37899").get(null))return original;
   String id=fontId(storage);if(id.contains("negative")||id.contains("space"))return original;
   inFallback.set(true);Object safe=safeStorage(storage);if(safe!=null&&safe!=storage){Object result=safe.getClass().getMethod("method_2011",int.class,boolean.class).invoke(safe,codepoint,validated);if(result!=null)return result;}
  }catch(Throwable e){once("glyph-metrics-error","GLYPH_METRICS_FALLBACK_FAILED "+e.getClass().getSimpleName());}finally{inFallback.remove();}return original;
 }
 private static String fontId(Object storage){try{return String.valueOf(field(storage,"field_2246"));}catch(Throwable e){return "unknown";}}
 private static Object safeStorage(Object storage)throws Exception{
  ClassLoader cl=storage.getClass().getClassLoader();Class<?> mc=cl.loadClass("net.minecraft.class_310");Object client=mc.getMethod("method_1551").invoke(null);if(client==null)return null;
  Object renderer=mc.getField("field_1772").get(client);if(renderer==null)return null;Class<?> identifier=cl.loadClass("net.minecraft.class_2960");Object id=identifier.getMethod("method_60655",String.class,String.class).invoke(null,"saotw","safe_ascii");
  Method method=renderer.getClass().getDeclaredMethod("method_27526",identifier);method.setAccessible(true);return method.invoke(renderer,id);
 }
 private static void once(String key,String message){if(logged.size()<4096&&logged.add(key))log(message);}
 public static void log(String message){try{System.out.println("[SAO-ZH-Compatibility] "+message);Path p=root;if(p!=null)Files.write(p.resolve("SAO_ZH_compat16.log"),(Instant.now()+" [SAO-ZH-Compatibility] "+message+"\r\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Throwable ignored){}}
}
