package sao.zh.engine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;

/**
 * Opt-in local observation of untranslated text at explicitly approved game display entry points.
 * Rendering threads only perform bounded filtering and queue.offer; all disk and JSON work is on
 * one daemon writer. No network access. Not a general chat/packet/heap interceptor.
 */
final class UnknownCollector implements AutoCloseable {
 static final int MAX_TEXT=480, MAX_RECORDS=15000, MAX_QUEUE=1024;
 static final long MAX_FILE_BYTES=12L*1024*1024;
 private static final Pattern LATIN=Pattern.compile("[A-Za-z\\u00C0-\\u02AF]");
 private static final Pattern EMAIL=Pattern.compile("(?i)[^\\s@]+@[^\\s@]+\\.[A-Z]{2,}");
 private static final Pattern IP=Pattern.compile("(?<!\\d)(?:\\d{1,3}\\.){3}\\d{1,3}(?!\\d)");
 private static final Pattern UUID=Pattern.compile("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b");
 private static final Pattern CHAT=Pattern.compile("(?s)^\\s*(?:\\[[^\\]{}\\r\\n]{1,30}\\]\\s*)*[A-Za-z0-9_]{3,16}\\s*[:：»].*");
 private static final Pattern USERNAME=Pattern.compile("[A-Za-z0-9_]{3,16}");
 private static final Pattern RESOURCE=Pattern.compile("(?i)[a-z_][a-z0-9_.-]*:[a-z0-9_./-]+");
 private final Path file;
 private final ArrayBlockingQueue<Row>queue=new ArrayBlockingQueue<>(MAX_QUEUE);
 private final Set<String>seen=ConcurrentHashMap.newKeySet();
 private final Thread writer;
 private final AtomicInteger accepted=new AtomicInteger(),errors=new AtomicInteger();
 private volatile boolean running=true;
 private record Row(String origin,String section,String hook,String text){}
 UnknownCollector(Path root){
  file=root.resolve("SAO_TW_Dynamic/review/hook_missing_zh_tw.jsonl").toAbsolutePath().normalize();
  writer=new Thread(this::writeLoop,"SAO-ZH-Missing-Local");
  writer.setDaemon(true);writer.setPriority(Thread.MIN_PRIORITY);writer.start();
 }
 static boolean permitted(String owner,String kind,String origin){
  if(owner==null||kind==null||origin==null)return false;
  if(origin.equals("mob"))return owner.equals("fr/sao/saoui/mob/MobBarRenderer")&&kind.equals("mob_name");
  if(!Set.of("return","argument","call","field").contains(kind))return false;
  if(owner.startsWith("net/minecraft/")||owner.startsWith("fr/sao/pass/")||owner.startsWith("fr/sao/font/")
      ||owner.startsWith("fr/sao/playerdisplay/")||owner.startsWith("fr/sao/guild/")
      ||owner.startsWith("fr/sao/saoui/tab/")||owner.startsWith("fr/sao/saoui/chat/"))return false;
  return switch(origin){
   case "quest"->owner.startsWith("fr/sao/questtracker/screen/")||owner.startsWith("fr/sao/questtracker/hud/");
   case "codex"->owner.startsWith("fr/sao/codex/");
   case "skill"->owner.startsWith("com/eighteen/saojob/skilltree/")||owner.startsWith("com/eighteen/saojob/skills/");
   case "item"->owner.startsWith("fr/sao/iteminfo/")||owner.startsWith("fr/sao/tooltip/")||
         owner.startsWith("fr/sao/daily/")||owner.startsWith("fr/sao/itemlang/");
   case "world"->owner.startsWith("fr/sao/teleport/")||owner.startsWith("fr/sao/interlink/")||
         owner.equals("fr/sao/saoui/mob/BossHud");
   case "system"->owner.equals("fr/sao/saoui/hud/actionbar/ActionBarTranslator");
   default->false;
  };
 }
 static boolean safe(String text,String origin){
  if(text==null||origin==null||text.length()<4||text.length()>MAX_TEXT)return false;
  String s=text.trim();
  if(s.length()<4||(!origin.equals("mob")&&USERNAME.matcher(s).matches())||RESOURCE.matcher(s).matches()
      ||s.equalsIgnoreCase("null")||s.equalsIgnoreCase("old"))return false;
  if(!LATIN.matcher(s).find())return false;
  if(s.indexOf('@')>=0||s.indexOf('§')>=0||s.indexOf('<')>=0||s.indexOf('>')>=0||
      s.indexOf('\\')>=0||s.indexOf('/')>=0||s.indexOf('\u0000')>=0||
      s.contains("://")||s.startsWith("\\u")||s.length()>MAX_TEXT)return false;
  if(EMAIL.matcher(s).find()||IP.matcher(s).find()||UUID.matcher(s).find()||CHAT.matcher(s).matches())return false;
  if(s.matches("(?is).*\\b(?:password|passphrase|token|secret|bearer|authorization|api[_-]?key|private[_-]?key|access[_-]?key)\\s*[=:].*"))return false;
  int letters=0;
  for(int offset=0;offset<s.length();){
   int cp=s.codePointAt(offset);offset+=Character.charCount(cp);
   if(Character.isLetter(cp))letters++;
   if(Character.isISOControl(cp) && cp!='\n'&&cp!='\t')return false;
  }
  return letters>=3;
 }
 boolean offer(String origin,String section,String hook,String text){
  if(!running||!safe(text,origin)||hook==null||hook.length()>100||
     !hook.matches("[A-Za-z0-9_.:-]+")||!Set.of("names","lines","items","messages").contains(section))return false;
  if(accepted.get()>=MAX_RECORDS||seen.size()>=MAX_RECORDS)return false;
  String key=origin+'\u0000'+section+'\u0000'+text;
  if(!seen.add(key))return false;
  Row row=new Row(origin,section,hook,text);
  if(!queue.offer(row)){seen.remove(key);return false;}
  accepted.incrementAndGet();return true;
 }
 int accepted(){return accepted.get();}
 int errors(){return errors.get();}
 private static String hex(byte[]hash){
  char[]digits="0123456789abcdef".toCharArray(),res=new char[hash.length*2];
  for(int i=0;i<hash.length;i++){res[i*2]=digits[(hash[i]>>>4)&15];res[i*2+1]=digits[hash[i]&15];}
  return new String(res);
 }
 private static String stableId(Row row) throws Exception{
  MessageDigest sha=MessageDigest.getInstance("SHA-256");
  byte[]raw=(row.section+"\u0000"+row.text).getBytes(StandardCharsets.UTF_8);
  return hex(sha.digest(raw)).substring(0,24);
 }
 private void flush(List<Row> batch){
  if(batch.isEmpty())return;
  try {
   Files.createDirectories(file.getParent());
   if(Files.isSymbolicLink(file)||Files.exists(file,LinkOption.NOFOLLOW_LINKS) &&
       (!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)>=MAX_FILE_BYTES))return;
   StringBuilder out=new StringBuilder(batch.size()*240);
   for(Row row:batch){
    Map<String,Object>record=new LinkedHashMap<>();
    record.put("schema_version",1);record.put("locale","zh_tw");
    record.put("id",stableId(row));record.put("source_key",row.text);
    record.put("section",row.section);record.put("origin","hook."+row.origin);
    record.put("hook",row.hook);record.put("zh_tw","");record.put("status","unreviewed");
    out.append(MiniJson.stringify(record)).append('\n');
   }
   byte[]blob=out.toString().getBytes(StandardCharsets.UTF_8);
   if(blob.length<=512*1024&&(!Files.exists(file)||Files.size(file)+blob.length<=MAX_FILE_BYTES))
    Files.write(file,blob,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  }catch(Exception problem){errors.incrementAndGet();}
 }
 private void writeLoop(){
  List<Row>batch=new ArrayList<>(32);
  while(running||!queue.isEmpty()){
   try {
    Row value=queue.poll(1000,TimeUnit.MILLISECONDS);
    if(value!=null)batch.add(value);
    if(batch.size()>=32||value==null||!running){flush(batch);batch.clear();}
   }catch(InterruptedException ignored){running=false;}
   catch(Throwable problem){errors.incrementAndGet();batch.clear();}
  }
  if(!batch.isEmpty())flush(batch);
 }
 @Override public void close(){
  running=false;
  writer.interrupt();
  try{writer.join(5000);}catch(InterruptedException e){Thread.currentThread().interrupt();}
 }
}
