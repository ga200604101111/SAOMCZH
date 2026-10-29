package sao.zh.compat16;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

/** JDK-only, read-only GitHub JSON sync. Never executes downloaded content or modifies game assets. */
public final class GitHubTranslationClient {
 private GitHubTranslationClient(){}
 private static final int INDEX_LIMIT=1024*1024, VERSION_LIMIT=65536, TABLE_LIMIT=8*1024*1024;
 private static final Pattern ID=Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");
 private static final Pattern LOCALE=Pattern.compile("[a-z]{2,3}_[a-z]{2,4}");
 private static final Pattern SHA=Pattern.compile("[0-9a-f]{64}");
 private static final Pattern SEMVER=Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?");
 private static final String[] SECTIONS={"names","messages","lines","items"};
 private static final Pattern TOKENS=Pattern.compile("%(?:[0-9]+\\$)?[-+#0 ,(]*[0-9]*(?:\\.[0-9]+)?[a-zA-Z]|%%|\\{[^{}\\r\\n]{1,80}\\}");
 private static ScheduledExecutorService executor;

 public interface Fetch { byte[] get(String path,int limit)throws Exception; }

 public static final class Config {
  public final String owner,repo,ref,locale;
  public final int intervalMinutes;
  Config(String a,String b,String c,String d,int m){owner=a;repo=b;ref=c;locale=d;intervalMinutes=m;}
  public String identity(){return owner+"/"+repo+"@"+ref;}
 }
 private static Map<?,?> map(Object value,String what)throws IOException {
  if(!(value instanceof Map))throw new IOException("Expected JSON object: "+what);
  return (Map<?,?>)value;
 }
 private static String field(Map<?,?> m,String name)throws IOException {
  Object x=m.get(name);
  if(!(x instanceof String)||((String)x).isEmpty())throw new IOException("Invalid required field "+name);
  return (String)x;
 }
 private static int number(Map<?,?>m,String name)throws IOException {
  Object v=m.get(name);
  if(!(v instanceof Number))throw new IOException("Invalid numeric field "+name);
  double n=((Number)v).doubleValue();
  if(!Double.isFinite(n)||n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new IOException("Invalid integer "+name);
  return (int)n;
 }
 private static boolean validId(String input){return ID.matcher(input).matches()&&!input.equals(".")&&!input.equals("..");}
 private static String safePath(String path,String locale)throws IOException {
  if(path==null||!path.startsWith("languages/"+locale+"/")||path.indexOf('\\')>=0||path.indexOf('?')>=0||path.indexOf('#')>=0)throw new IOException("Invalid remote path");
  String[] elements=path.split("/",-1);
  if(elements.length<3)throw new IOException("Short remote path");
  for(String e:elements)if(!validId(e))throw new IOException("Invalid remote path segment");
  return path;
 }
 private static Object json(byte[]data,int maximum)throws IOException {
  if(data==null||data.length==0||data.length>maximum)throw new IOException("Invalid JSON size");
  try{return MiniJson.parse(new String(data,StandardCharsets.UTF_8));}
  catch(RuntimeException e){throw new IOException("Invalid JSON",e);}
 }
 public static Config configuration(Path root)throws IOException {
  Path cf=root.resolve("SAO_TW_Dynamic/github-source.json");
  if(!Files.isRegularFile(cf))return null;
  Map<?,?> v=map(json(Files.readAllBytes(cf),65536),"config");
  if(!Boolean.TRUE.equals(v.get("enabled")))return null;
  String owner=field(v,"owner"),repo=field(v,"repo"),ref=field(v,"ref"),locale=field(v,"locale");
  if(!validId(owner)||!validId(repo)||!validId(ref)||!LOCALE.matcher(locale).matches())throw new IOException("Invalid repo identity/locale");
  int every=number(v,"check_interval_minutes");
  if(every<15||every>1440)throw new IOException("Check interval must be between 15 and 1440 minutes");
  return new Config(owner,repo,ref,locale,every);
 }
 public static byte[] httpsFetch(Config config,String path,int limit)throws IOException {
  String[] parts=path.split("/");
  for(String x:parts)if(!validId(x))throw new IOException("Invalid fetch segment");
  URI uri;
  try{uri=new URI("https","raw.githubusercontent.com","/"+config.owner+"/"+config.repo+"/"+config.ref+"/"+path,null);}
  catch(URISyntaxException e){throw new IOException("Bad GitHub URL",e);}
  HttpsURLConnection c=(HttpsURLConnection)uri.toURL().openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(6000);c.setReadTimeout(12000);
  c.setRequestProperty("User-Agent","SAOMCZH-GitHub-Localizer/1");
  c.setRequestProperty("Accept","application/json");
  try{
   if(c.getResponseCode()!=200)throw new IOException("GitHub HTTP "+c.getResponseCode());
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[]buffer=new byte[8192];int n;
    while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Exceeded GitHub JSON size limit");out.write(buffer,0,n);}
    return out.toByteArray();
   }
  }finally{c.disconnect();}
 }
 private static String sha256(byte[]data)throws IOException {
  try{
   byte[]hash=MessageDigest.getInstance("SHA-256").digest(data);
   char[]digits="0123456789abcdef".toCharArray(),out=new char[hash.length*2];
   for(int i=0;i<hash.length;i++){out[i*2]=digits[(hash[i]>>4)&15];out[i*2+1]=digits[hash[i]&15];}
   return new String(out);
  }catch(Exception e){throw new IOException("SHA-256 unavailable",e);}
 }
 private static List<String> tokens(String text) {
  ArrayList<String>found=new ArrayList<>();
  java.util.regex.Matcher m=TOKENS.matcher(text);while(m.find())found.add(m.group());
  Collections.sort(found);return found;
 }
 public static Map<String,Map<String,String>> validateDictionary(byte[] bytes,String locale)throws IOException {
  Map<?,?> top=map(json(bytes,TABLE_LIMIT),"translations");
  if(number(top,"schema_version")!=1||!locale.equals(field(top,"locale")))throw new IOException("Translation schema/locale mismatch");
  Map<String,Map<String,String>> result=new LinkedHashMap<>();int count=0;
  for(String section:SECTIONS){
   Map<?,?> records=map(top.get(section),section);
   if(records.size()>30000)throw new IOException("Too many entries");
   Map<String,String>rows=new LinkedHashMap<>();
   for(Map.Entry<?,?> record:records.entrySet()){
    if(!(record.getKey() instanceof String)||!(record.getValue() instanceof String))throw new IOException("Nonstring dictionary entry");
    String key=(String)record.getKey(),value=(String)record.getValue();
    if(key.isEmpty()||value.isEmpty()||key.length()>16384||value.length()>16384)throw new IOException("Invalid dictionary text length");
    if(!tokens(key).equals(tokens(value)))throw new IOException("Placeholder mismatch");
    rows.put(key,value);count++;
   }
   result.put(section,Collections.unmodifiableMap(rows));
  }
  if(count>120000)throw new IOException("Too many translations");
  return Collections.unmodifiableMap(result);
 }
 private static void atomic(Path p,byte[] bytes)throws IOException {
  Files.createDirectories(p.getParent());
  Path tmp=Files.createTempFile(p.getParent(),".sao-gh-", ".pending");
  try{
   Files.write(tmp,bytes);
   try{Files.move(tmp,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}
  }finally{Files.deleteIfExists(tmp);}
 }
 private static Path stateFile(Path root,String locale){return root.resolve("SAO_TW_Dynamic/github-cache/"+locale+".state.json");}
 private static Path blobFile(Path root,String locale,String hash){return root.resolve("SAO_TW_Dynamic/github-cache/"+locale+"."+hash+".json");}
 /** Only remove superseded content-addressed language files created by this agent, never the local review dictionary. */
 static int discardRetiredLanguageBlobs(Path root,String locale,String activeHash){
  if(locale==null||!LOCALE.matcher(locale).matches()||activeHash==null||!SHA.matcher(activeHash).matches())return 0;
  Path dir=root.resolve("SAO_TW_Dynamic/github-cache");
  if(!Files.isDirectory(dir))return 0;
  int removed=0;
  try(DirectoryStream<Path> entries=Files.newDirectoryStream(dir)){
   for(Path p:entries){
    String filename=p.getFileName().toString();
    if(!filename.matches(java.util.regex.Pattern.quote(locale)+"\\.[0-9a-f]{64}\\.json"))continue;
    if(filename.equals(locale+"."+activeHash+".json"))continue;
    try{
     if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&Files.deleteIfExists(p))removed++;
    }catch(IOException ignored){RuntimeBridge.log("GITHUB_STALE_CACHE_LOCKED "+locale);}
   }
  }catch(IOException ignored){}
  return removed;
 }
 public static String syncOnce(Path root,Config config,Fetch fetch)throws Exception {
  Map<?,?> index=map(json(fetch.get("manifest.json",INDEX_LIMIT),INDEX_LIMIT),"manifest");
  if(number(index,"schema_version")!=1||number(index,"client_protocol")!=1)throw new IOException("Unsupported repository protocol");
  Map<?,?> locales=map(index.get("locales"),"locales");
  Map<?,?> selected=map(locales.get(config.locale),"locale entry");
  String versionPath=safePath(field(selected,"version_manifest"),config.locale);
  Map<?,?> version=map(json(fetch.get(versionPath,VERSION_LIMIT),VERSION_LIMIT),"language version");
  if(number(version,"schema_version")!=1||number(version,"min_client_protocol")!=1
     ||!config.locale.equals(field(version,"locale")))throw new IOException("Unsupported version manifest");
  String semantic=field(version,"version");
  if(!SEMVER.matcher(semantic).matches())throw new IOException("Invalid version number");
  Map<?,?> file=map(version.get("translation"),"translation asset");
  String path=safePath(field(file,"path"),config.locale);
  String digest=field(file,"sha256");
  if(!SHA.matcher(digest).matches())throw new IOException("Invalid translation digest");
  int length=number(file,"size_bytes");
  if(length<1||length>TABLE_LIMIT)throw new IOException("Invalid translation table size");

  Path saved=blobFile(root,config.locale,digest);
  boolean cached=Files.isRegularFile(saved)&&Files.size(saved)==length;
  byte[]raw=cached?Files.readAllBytes(saved):fetch.get(path,TABLE_LIMIT);
  boolean verified=raw.length==length&&sha256(raw).equals(digest);
  if(!verified&&cached){
   raw=fetch.get(path,TABLE_LIMIT);
   verified=raw.length==length&&sha256(raw).equals(digest);
  }
  if(!verified)throw new IOException("Translation checksum mismatch");
  validateDictionary(raw,config.locale);
  if(!cached||!sha256(Files.readAllBytes(saved)).equals(digest))atomic(saved,raw);
  Map<String,Object> state=new LinkedHashMap<>();
  state.put("schema_version",1);state.put("locale",config.locale);
  state.put("version",semantic);state.put("sha256",digest);state.put("repo",config.identity());state.put("size_bytes",length);
  Path status=stateFile(root,config.locale);
  byte[]next=(MiniJson.stringify(state)+"\n").getBytes(StandardCharsets.UTF_8);
   boolean unchanged=Files.isRegularFile(status)&&Arrays.equals(Files.readAllBytes(status),next);
   if(!unchanged)atomic(status,next);
   // State has been committed, so stale download copies are safe to discard.
   discardRetiredLanguageBlobs(root,config.locale,digest);
   return (unchanged?"UNCHANGED ":"UPDATED ")+config.locale+" "+semantic;
 }
 public static Map<String,Map<String,String>> cached(Path root,String locale) {
  try{
   Config config=configuration(root);
   if(config==null||!locale.equals(config.locale))return Collections.emptyMap();
   Path state=stateFile(root,locale);if(!Files.isRegularFile(state))return Collections.emptyMap();
   Map<?,?> v=map(json(Files.readAllBytes(state),VERSION_LIMIT),"state");
   String hash=field(v,"sha256");
   if(!SHA.matcher(hash).matches()||!locale.equals(field(v,"locale"))
      ||!config.identity().equals(field(v,"repo")))return Collections.emptyMap();
   Path blob=blobFile(root,locale,hash);
   if(!Files.isRegularFile(blob)||Files.size(blob)>TABLE_LIMIT)return Collections.emptyMap();
   byte[]raw=Files.readAllBytes(blob);
   if(!sha256(raw).equals(hash))return Collections.emptyMap();
   return validateDictionary(raw,locale);
  }catch(Throwable e){return Collections.emptyMap();}
 }
 public static synchronized void start(Path root,Runnable onUpdated) {
  if(executor!=null)return;
  final Config config;
  try{config=configuration(root);}catch(Exception e){RuntimeBridge.log("GITHUB_CONFIG_INVALID "+e.getClass().getSimpleName());return;}
  if(config==null)return;
  executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"SAO-TW-GitHub-Sync");t.setDaemon(true);return t;});
  executor.scheduleWithFixedDelay(()->{
   try{
    String result=syncOnce(root,config,(path,limit)->httpsFetch(config,path,limit));
    RuntimeBridge.log("GITHUB_SYNC "+result);
    if(result.startsWith("UPDATED "))onUpdated.run();
    PackLifecycleManager.backgroundStage(root,config);
   }catch(Throwable e){RuntimeBridge.log("GITHUB_SYNC_KEEP_LAST_GOOD "+e.getClass().getSimpleName());
    PackLifecycleManager.backgroundStage(root,config);}
  },5,config.intervalMinutes*60L,TimeUnit.SECONDS);
  RuntimeBridge.log("GITHUB_SYNC_SCHEDULED source="+config.owner+"/"+config.repo+" locale="+config.locale+" every_minutes="+config.intervalMinutes);
 }
}
