package sao.zh.compat16;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.net.ssl.HttpsURLConnection;

/** Versioned resource-pack cache: stage during play; promote only at the next game launch. */
public final class PackLifecycleManager {
 private PackLifecycleManager(){}
 private static final int MAX_MANIFEST=65536;
 private static final long MAX_PACK=600L*1024*1024;
 private static final String MAIN="main",SUPPORT="support";
 private static final String[] TYPES={MAIN,SUPPORT};
 private static final AtomicBoolean hookRegistered=new AtomicBoolean();
 private static volatile Path activeRoot;
 private static volatile Map<String,String> activeHashes=Collections.emptyMap();

 public static final class Selection {
  public final Path main,support;
  Selection(Path m,Path s){main=m;support=s;}
 }
 public interface Fetch { byte[] get(String path,int maxBytes)throws Exception; }
 public interface Download { InputStream open(String url)throws Exception; }
 static final class Asset {
  final String kind,sha,url;final long size;
  Asset(String kind,String sha,long size,String url){this.kind=kind;this.sha=sha;this.size=size;this.url=url;}
 }
 static final class Plan {
  final Map<String,Asset> files;
  Plan(Map<String,Asset> a){files=a;}
 }
 /** Separate translation and resource repositories: no arbitrary GitHub asset sources. */
 static final class PackSource {
  final GitHubTranslationClient.Config config;
  final String manifestPath;
  PackSource(GitHubTranslationClient.Config c,String p){config=c;manifestPath=p;}
 }
 static PackSource packSource(Path root,GitHubTranslationClient.Config translation)throws IOException {
  Path conf=root.resolve("SAO_TW_Dynamic/github-source.json");
  if(!Files.isRegularFile(conf))return new PackSource(translation,null);
  byte[]raw=Files.readAllBytes(conf);
  if(raw.length>65536)throw new IOException("Pack config too large");
  Map<?,?> opts=map(MiniJson.parse(new String(raw,StandardCharsets.UTF_8)),"github-source");
  Object packRepo=opts.get("pack_repo");
  if(packRepo==null)return new PackSource(translation,null);
  String owner=str(opts,"pack_owner"),repo=str(opts,"pack_repo"),ref=str(opts,"pack_ref");
  String manifest=opts.containsKey("pack_manifest")?str(opts,"pack_manifest"):"pack-version.json";
  if(!owner.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")
      ||!repo.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")
      ||!ref.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")
      ||!manifest.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}\\.json")
      ||owner.equals("..")||repo.equals(".."))
   throw new IOException("Untrusted pack repository configuration");
  GitHubTranslationClient.Config pack=new GitHubTranslationClient.Config(owner,repo,ref,translation.locale,translation.intervalMinutes);
  return new PackSource(pack,manifest);
 }
 private static Map<?,?> map(Object x,String name)throws IOException {
  if(!(x instanceof Map))throw new IOException("Expected JSON object for "+name);
  return (Map<?,?>)x;
 }
 private static String str(Map<?,?>m,String k)throws IOException {
  Object v=m.get(k);if(!(v instanceof String)||((String)v).isEmpty())throw new IOException("Missing "+k);
  return (String)v;
 }
 private static long whole(Map<?,?>m,String name)throws IOException {
  Object v=m.get(name);
  if(!(v instanceof Number))throw new IOException("Missing numeric "+name);
  double n=((Number)v).doubleValue();
  if(!Double.isFinite(n)||n!=Math.rint(n)||n<0||n>Long.MAX_VALUE)throw new IOException("Invalid "+name);
  return (long)n;
 }
 static String hash(byte[]data)throws Exception {
  byte[]dig=MessageDigest.getInstance("SHA-256").digest(data);
  char[]out=new char[dig.length*2],hex="0123456789abcdef".toCharArray();
  for(int i=0;i<dig.length;i++){out[i*2]=hex[(dig[i]>>4)&15];out[i*2+1]=hex[dig[i]&15];}
  return new String(out);
 }
 private static String checkedDigest(String s)throws IOException {
  if(!s.matches("[a-f0-9]{64}"))throw new IOException("Bad resource hash");
  return s;
 }
 private static String checkedUrl(String url,GitHubTranslationClient.Config cfg,String kind)throws IOException {
  try{
   URI uri=new URI(url);
   if(!"https".equals(uri.getScheme())||!"github.com".equalsIgnoreCase(uri.getHost())
     ||uri.getRawQuery()!=null||uri.getRawFragment()!=null||uri.getUserInfo()!=null||uri.getPort()!=-1)
    throw new IOException("Release URL must use GitHub HTTPS");
   String prefix="/"+cfg.owner+"/"+cfg.repo+"/releases/download/";
   String path=uri.getPath();
   if(path==null||!path.startsWith(prefix)||path.indexOf("..")>=0||
      !path.matches("[A-Za-z0-9_./-]+\\.zip"))throw new IOException("Invalid Release asset path");
   return url;
  }catch(URISyntaxException e){throw new IOException("Malformed GitHub Release URL",e);}
 }
 private static boolean supportedHost(URI uri){
  String h=uri.getHost();
  return "https".equalsIgnoreCase(uri.getScheme())&&h!=null&&
    (h.equalsIgnoreCase("github.com")||h.equalsIgnoreCase("release-assets.githubusercontent.com")||
     h.equalsIgnoreCase("objects.githubusercontent.com")||
     h.endsWith(".githubusercontent.com"))&&uri.getUserInfo()==null;
 }
 static InputStream openRelease(String url)throws Exception {
  URI target=new URI(url);
  for(int i=0;i<6;i++){
   if(!supportedHost(target))throw new IOException("Untrusted asset download host");
   HttpsURLConnection conn=(HttpsURLConnection)target.toURL().openConnection();
   conn.setInstanceFollowRedirects(false);
   conn.setConnectTimeout(10000);conn.setReadTimeout(45000);
   conn.setRequestProperty("User-Agent","SAOMCZH-LanguagePack/1");
   int status=conn.getResponseCode();
   if(status==200){
    final HttpsURLConnection connection=conn;
    final InputStream in=conn.getInputStream();
    return new FilterInputStream(in){public void close()throws IOException{try{super.close();}finally{connection.disconnect();}}};
   }
   if(status==301||status==302||status==303||status==307||status==308){
    String next=conn.getHeaderField("Location");conn.disconnect();
    if(next==null)throw new IOException("Release redirect missing Location");
    target=target.resolve(next);
    continue;
   }
   conn.disconnect();throw new IOException("Release download HTTP "+status);
  }
  throw new IOException("Too many Release redirects");
 }
 private static Plan parse(byte[]bytes,GitHubTranslationClient.Config cfg)throws Exception {
  if(bytes==null||bytes.length>MAX_MANIFEST)throw new IOException("Pack metadata too large");
  Map<?,?> v=map(MiniJson.parse(new String(bytes,StandardCharsets.UTF_8)),"language manifest");
  if(!(v.get("resource_packs") instanceof Map))return new Plan(Collections.<String,Asset>emptyMap());
  Map<?,?> packs=map(v.get("resource_packs"),"resource_packs");
  if(!Boolean.TRUE.equals(packs.get("available")))return new Plan(Collections.<String,Asset>emptyMap());
  Map<String,Asset>files=new LinkedHashMap<>();
  for(String kind:TYPES){
   Map<?,?> item=map(packs.get(kind),kind);
   String sha=checkedDigest(str(item,"sha256"));
   long size=whole(item,"size_bytes");
   if(size<128||size>MAX_PACK)throw new IOException("Invalid "+kind+" archive size");
   String url=checkedUrl(str(item,"url"),cfg,kind);
   files.put(kind,new Asset(kind,sha,size,url));
  }
  return new Plan(files);
 }
 private static Path rootDir(Path root){return root.resolve("SAO_ZH_Cache");}
 private static Path packFile(Path root,String kind,String sha){return rootDir(root).resolve("packs/"+kind+"-"+sha+".zip");}
 private static Path marker(Path root,String type){return rootDir(root).resolve(type+".json");}
 private static void writeAtomic(Path to,byte[]bytes)throws IOException {
  Files.createDirectories(to.getParent());
  Path temp=Files.createTempFile(to.getParent(),".asset-", ".tmp");
  try{
   Files.write(temp,bytes);
   try{Files.move(temp,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException e){Files.move(temp,to,StandardCopyOption.REPLACE_EXISTING);}
  }finally{Files.deleteIfExists(temp);}
 }
 private static Map<String,String> readMarker(Path root,String kind){
  try{
   Path p=marker(root,kind);if(!Files.isRegularFile(p)||Files.size(p)>65536)return Collections.emptyMap();
   Map<?,?> record=map(MiniJson.parse(new String(Files.readAllBytes(p),StandardCharsets.UTF_8)),"marker");
   if(!"SAO_ZH_Cache".equals(str(record,"owner"))||((Number)record.get("schema_version")).intValue()!=1)return Collections.emptyMap();
   Map<?,?> sources=map(record.get("packs"),"packs");
   Map<String,String> result=new HashMap<>();
   for(String type:TYPES)result.put(type,checkedDigest(str(sources,type)));
   return result;
  }catch(Throwable ignored){return Collections.emptyMap();}
 }
 private static void writeMarker(Path root,String kind,Map<String,String> hashes)throws IOException {
  Map<String,Object>entry=new LinkedHashMap<>();
  entry.put("schema_version",1);entry.put("owner","SAO_ZH_Cache");
  entry.put("packs",new TreeMap<String,String>(hashes));
  writeAtomic(marker(root,kind),(MiniJson.stringify(entry)+"\n").getBytes(StandardCharsets.UTF_8));
 }
 private static boolean zipIsValid(Path p,String kind){
  if(!Files.isRegularFile(p))return false;
  try(ZipFile z=new ZipFile(p.toFile())){
   ZipEntry meta=z.getEntry("pack.mcmeta");
   if(meta==null||meta.getSize()>65536||meta.getSize()<0)return false;
   try(InputStream in=z.getInputStream(meta)){
    byte[]first=new byte[64];if(in.read(first)==-1)return false;
   }
   if(MAIN.equals(kind)&&z.getEntry("assets/saoui/lang/zh_tw.json")==null)return false;
   return true;
  }catch(IOException e){return false;}
 }
 private static boolean cached(Path root,String kind,String sha,boolean checkHash){
  Path p=packFile(root,kind,sha);
  if(!zipIsValid(p,kind))return false;
  if(!checkHash)return true;
  try{return hashFile(p).equals(sha);}catch(Exception e){return false;}
 }
 private static String hashFile(Path p)throws Exception{
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  try(InputStream in=Files.newInputStream(p)){
   byte[]buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);
  }
  char[]hex="0123456789abcdef".toCharArray(),out=new char[64];byte[]bytes=digest.digest();
  for(int i=0;i<bytes.length;i++){out[2*i]=hex[(bytes[i]>>4)&15];out[2*i+1]=hex[bytes[i]&15];}
  return new String(out);
 }
 private static boolean validMarkerFiles(Path root,Map<String,String> mark){
  for(String k:TYPES)if(!mark.containsKey(k)||!cached(root,k,mark.get(k),true))return false;
  return true;
 }
 private static void download(Path root,Asset asset,Download downloader)throws Exception {
  Path target=packFile(root,asset.kind,asset.sha);
  if(cached(root,asset.kind,asset.sha,true))return;
  Files.createDirectories(target.getParent());
  Path pending=Files.createTempFile(target.getParent(),".pack-", ".download");
  try{
   MessageDigest digest=MessageDigest.getInstance("SHA-256");
   long written=0;
   try(InputStream from=downloader.open(asset.url);OutputStream to=Files.newOutputStream(pending,StandardOpenOption.WRITE)){
    byte[]chunk=new byte[131072];int n;
    while((n=from.read(chunk))!=-1){
     written+=n;
     if(written>asset.size||written>MAX_PACK)throw new IOException("Asset exceeds declared size");
     digest.update(chunk,0,n);to.write(chunk,0,n);
    }
   }
   if(written!=asset.size||!hashFile(pending).equals(asset.sha))throw new IOException("Release asset digest/size mismatch");
   if(!zipIsValid(pending,asset.kind))throw new IOException("Downloaded ZIP failed pack metadata validation");
   try{Files.move(pending,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(AtomicMoveNotSupportedException e){Files.move(pending,target,StandardCopyOption.REPLACE_EXISTING);}
  }finally{Files.deleteIfExists(pending);}
 }
 public static String stage(Path root,GitHubTranslationClient.Config cfg,Fetch fetch,Download downloader)throws Exception {
  PackSource packSource=packSource(root,cfg);
  Plan plan;
  if(packSource.manifestPath!=null){
   byte[]input=fetch.get(packSource.manifestPath,MAX_MANIFEST);
   Map<?,?>version=map(MiniJson.parse(new String(input,StandardCharsets.UTF_8)),"dedicated pack manifest");
   if(whole(version,"schema_version")!=1||whole(version,"min_agent_version")>12
      ||!cfg.locale.equals(str(version,"locale"))
      ||!str(version,"pack_version").matches("[0-9]+\\.[0-9]+\\.[0-9]+"))
    throw new IOException("Incompatible dedicated pack manifest");
   plan=parse(input,packSource.config);
  }else{
   // Backward-compatible lookup for clients whose assets share the translation repo.
   byte[]index=fetch.get("manifest.json",MAX_MANIFEST);
   Map<?,?> manifest=map(MiniJson.parse(new String(index,StandardCharsets.UTF_8)),"manifest");
   if(((Number)manifest.get("schema_version")).intValue()!=1)throw new IOException("Unsupported manifest schema");
   Map<?,?> langs=map(manifest.get("locales"),"languages");
   Map<?,?>lang=map(langs.get(cfg.locale),"selected language");
   String versionPath=str(lang,"version_manifest");
   if(!versionPath.startsWith("languages/"+cfg.locale+"/")||versionPath.contains(".."))throw new IOException("Invalid version path");
   plan=parse(fetch.get(versionPath,MAX_MANIFEST),cfg);
  }
  if(plan.files.isEmpty())return "NOT_PUBLISHED";
  Map<String,String>desired=new HashMap<>();
  for(String kind:TYPES){Asset a=plan.files.get(kind);desired.put(kind,a.sha);}
  Map<String,String>active=readMarker(root,"active");
  Map<String,String>existing=readMarker(root,"staged");
  if(desired.equals(active)&&validMarkerFiles(root,active))return "CURRENT";
  if(desired.equals(existing)&&validMarkerFiles(root,existing))return "STAGED_ALREADY";
  // Download both into content-addressed files; never commit a partial pair.
  List<Path> newlyCreated=new ArrayList<>();
  try{
   for(String kind:TYPES){
    Path file=packFile(root,kind,desired.get(kind));
    boolean existed=Files.isRegularFile(file);
    download(root,plan.files.get(kind),downloader);
    if(!existed)newlyCreated.add(file);
   }
   writeMarker(root,"staged",desired);
   return "STAGED_NEXT_LAUNCH";
  }catch(Exception failed){
   // Remove only assets downloaded by this attempt and never touch active or staged versions.
   for(Path file:newlyCreated){
    boolean referenced=false;
    for(String kind:TYPES){
     if(file.equals(packFile(root,kind,active.get(kind)==null?"":active.get(kind))))referenced=true;
     if(file.equals(packFile(root,kind,existing.get(kind)==null?"":existing.get(kind))))referenced=true;
    }
    if(!referenced)try{Files.deleteIfExists(file);}catch(IOException ignored){}
   }
   throw failed;
  }
 }
 /** Remove abandoned cache revisions while preserving the currently mounted pack and staged update.
  * All deletion is restricted to the agent-owned cache directory and strict hash-based filenames. */
 static int cleanInactiveCache(Path root){
  Path activeMarker=marker(root,"active"),stagedMarker=marker(root,"staged");
  Map<String,String>active=readMarker(root,"active"),staged=readMarker(root,"staged");
  // If marker exists but cannot be verified, never guess which ZIP is in use.
  if((Files.exists(activeMarker)&&active.isEmpty())||(Files.exists(stagedMarker)&&staged.isEmpty()))return 0;
  if(active.isEmpty()&&staged.isEmpty())return 0;
  Set<Path>preserve=new HashSet<>();
  for(String kind:TYPES){
   for(Map<String,String>record:Arrays.asList(active,staged)){
    String sha=record.get(kind);
    if(sha!=null&&sha.matches("[a-f0-9]{64}"))preserve.add(packFile(root,kind,sha).toAbsolutePath().normalize());
   }
  }
  Path cache=rootDir(root).resolve("packs");
  if(!Files.isDirectory(cache))return 0;
  int removed=0;
  try(DirectoryStream<Path> candidates=Files.newDirectoryStream(cache,"*.zip")){
   for(Path p:candidates){
    String name=p.getFileName().toString();
    if(!name.matches("(?:main|support)-[a-f0-9]{64}\\.zip"))continue;
    if(preserve.contains(p.toAbsolutePath().normalize()))continue;
    try{
     if(Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&Files.deleteIfExists(p))removed++;
    }catch(IOException e){RuntimeBridge.log("PACK_STALE_CACHE_LOCKED_RETRY_NEXT_START "+name.substring(0,4));}
   }
  }catch(IOException ignored){}
  if(removed>0)RuntimeBridge.log("PACK_STALE_CACHE_REMOVED "+removed);
  return removed;
 }
 private static void prune(Path root,Map<String,String>old,Map<String,String>now) {
  if(old.isEmpty())return;
  for(String kind:TYPES){
   String hash=old.get(kind);
   if(hash==null||hash.equals(now.get(kind)))continue;
   Path remove=packFile(root,kind,hash);
   try{Files.deleteIfExists(remove);}
   catch(IOException e){RuntimeBridge.log("PACK_OLD_LOCKED_RETRY_NEXT_LAUNCH "+kind);}
  }
 }
 /** Fresh installations get safe defaults, but user-owned settings are never overwritten. */
 static boolean ensureDefaultConfig(Path root)throws IOException {
  if(Boolean.getBoolean("sao.zh.offline-test"))return false;
  Path conf=root.resolve("SAO_TW_Dynamic/github-source.json");
  if(Files.exists(conf))return false;
  Files.createDirectories(conf.getParent());
  Map<String,Object>settings=new LinkedHashMap<>();
  settings.put("enabled",true);settings.put("owner","ga200604101111");
  settings.put("repo","SAOMCZH");settings.put("ref","main");settings.put("locale","zh_tw");
  settings.put("check_interval_minutes",60);settings.put("allow_asset_downloads",true);
  settings.put("pack_owner","ga200604101111");settings.put("pack_repo","SAOMCZHPACK");
  settings.put("pack_ref","main");settings.put("pack_manifest","pack-version.json");
  try{
   Files.write(conf,(MiniJson.stringify(settings)+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE_NEW);
   RuntimeBridge.log("GITHUB_DEFAULT_CONFIG_CREATED repo=SAOMCZH packs=SAOMCZHPACK");
   return true;
  }catch(FileAlreadyExistsException concurrent){return false;}
 }
 /** Called during premain before Minecraft opens ZIPs. Configuration is opt-in and outside the game directory. */
 public static Selection startAutomatic(Path root,Path legacyMain,Path legacySupport){
  try{ensureDefaultConfig(root);}catch(IOException e){RuntimeBridge.log("DEFAULT_GITHUB_CONFIG_SKIPPED "+e.getClass().getSimpleName());}
  GitHubTranslationClient.Config cfg=null;
  try{
   Path settings=root.resolve("SAO_TW_Dynamic/github-source.json");
   if(Files.isRegularFile(settings)){
    Map<?,?>opts=map(MiniJson.parse(new String(Files.readAllBytes(settings),StandardCharsets.UTF_8)),"github source");
    if(Boolean.TRUE.equals(opts.get("allow_asset_downloads")))cfg=GitHubTranslationClient.configuration(root);
   }
  }catch(Throwable e){RuntimeBridge.log("RESOURCE_PACK_CONFIG_SKIPPED "+e.getClass().getSimpleName());}
  GitHubTranslationClient.Config selected=cfg;
  GitHubTranslationClient.Config net=selected;
  try{if(selected!=null)net=packSource(root,selected).config;}
  catch(IOException e){RuntimeBridge.log("PACK_REPOSITORY_CONFIG_INVALID "+e.getClass().getSimpleName());selected=null;}
  GitHubTranslationClient.Config finalNet=net;
  return startup(root,legacyMain,legacySupport,selected,
      (path,max)->GitHubTranslationClient.httpsFetch(finalNet,path,max),PackLifecycleManager::openRelease);
 }
 public static Selection startup(Path root,Path legacyMain,Path legacySupport,
                                GitHubTranslationClient.Config cfg,Fetch fetch,Download downloader) {
  activeRoot=root;
  try{
   Map<String,String>old=readMarker(root,"active"),staged=readMarker(root,"staged");
   if(!staged.isEmpty()&&validMarkerFiles(root,staged)){
    writeMarker(root,"active",staged);
    Files.deleteIfExists(marker(root,"staged"));
    prune(root,old,staged);
    old=staged;
    cleanInactiveCache(root);
    RuntimeBridge.log("RESOURCE_PACK_PROMOTED_AT_STARTUP");
   }
   if(!old.isEmpty()&&validMarkerFiles(root,old)){
    activeHashes=old;
    registerShutdown(root);
    cleanInactiveCache(root);
    return new Selection(packFile(root,MAIN,old.get(MAIN)),packFile(root,SUPPORT,old.get(SUPPORT)));
   }
   if(cfg!=null){
    // First use on a new client without any local pack: synchronous bootstrap only once.
    if(legacyMain==null){
     String status=stage(root,cfg,fetch,downloader);
     if(status.startsWith("STAGED")){
      Map<String,String>fresh=readMarker(root,"staged");
      if(validMarkerFiles(root,fresh)){
       writeMarker(root,"active",fresh);Files.deleteIfExists(marker(root,"staged"));activeHashes=fresh;
       registerShutdown(root);
       cleanInactiveCache(root);
       RuntimeBridge.log("FIRST_LAUNCH_RESOURCE_PACK_DOWNLOADED");
       return new Selection(packFile(root,MAIN,fresh.get(MAIN)),packFile(root,SUPPORT,fresh.get(SUPPORT)));
      }
     }
    }
   }
  }catch(Throwable e){RuntimeBridge.log("PACK_STARTUP_FALLBACK "+e.getClass().getSimpleName());}
  activeHashes=Collections.emptyMap();
  registerShutdown(root);
  return new Selection(legacyMain,legacySupport);
 }
 private static void registerShutdown(Path root){
  if(!hookRegistered.compareAndSet(false,true))return;
  Runtime.getRuntime().addShutdownHook(new Thread(()->{
   // Staged pack must remain, and game might still hold a ZIP handle at this point.
   Map<String,String>staged=readMarker(root,"staged");
   Map<String,String>active=readMarker(root,"active");
   if(staged.isEmpty()||!validMarkerFiles(root,staged))return;
   prune(root,active,staged);
   cleanInactiveCache(root);
  },"SAO-ZH-Pack-Cleanup"));
 }
 public static void backgroundStage(Path root,GitHubTranslationClient.Config cfg) {
  try{
   Path configFile=root.resolve("SAO_TW_Dynamic/github-source.json");
   if(!Files.isRegularFile(configFile))return;
   Map<?,?>raw=map(MiniJson.parse(new String(Files.readAllBytes(configFile),StandardCharsets.UTF_8)),"github source");
   if(!Boolean.TRUE.equals(raw.get("allow_asset_downloads")))return;
   GitHubTranslationClient.Config remote=packSource(root,cfg).config;
   String result=stage(root,cfg,(path,max)->GitHubTranslationClient.httpsFetch(remote,path,max),PackLifecycleManager::openRelease);
   if(result.startsWith("STAGED"))RuntimeBridge.log("RESOURCE_PACK_UPDATE_READY_RESTART_REQUIRED "+result);
  }catch(Throwable e){RuntimeBridge.log("RESOURCE_PACK_UPDATE_SKIPPED_KEEP_CURRENT "+e.getClass().getSimpleName());}
 }
}
