package sao.zh.boot;
import java.io.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.security.spec.X509EncodedKeySpec;import java.net.*;import java.util.*;import javax.net.ssl.HttpsURLConnection;
/** Generic trust and bounded IO; no Minecraft classes, text rules or drawing logic. */
public final class Verified {
 private Verified(){}
 public static final int MAX_MODULE=16*1024*1024;
 public static String hash(byte[]b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 public static void signature(byte[]b,byte[]s)throws Exception{
  if(s.length!=64)throw new SecurityException("Invalid signature length");
  PublicKey key=KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Trust.PUBLIC_KEY)));
  Signature v=Signature.getInstance("Ed25519");v.initVerify(key);v.update(b);try{if(!v.verify(s))throw new SecurityException("Signature rejected");}catch(SignatureException malformed){throw new SecurityException("Malformed signature rejected",malformed);}
 }
 public static Map<String,String> descriptor(byte[]b,byte[]s)throws Exception{
  if(b.length>65536)throw new SecurityException("Manifest too large");signature(b,s);
  Map<String,String>m=new LinkedHashMap<>();
  for(String line:new String(b,StandardCharsets.UTF_8).split("\\r?\\n")){
   if(line.isBlank()||line.startsWith("#"))continue;
   int i=line.indexOf('=');if(i<1)throw new SecurityException("Malformed descriptor");
   String k=line.substring(0,i),v=line.substring(i+1);if(!k.matches("[a-z0-9_.-]{1,80}")||m.put(k,v)!=null)throw new SecurityException("Duplicate descriptor key");
  }
  if(!"1".equals(m.get("api")))throw new SecurityException("Unsupported extension API");
  long expires=Long.parseLong(m.get("expires"));if(expires<System.currentTimeMillis()/1000)throw new SecurityException("Descriptor expired");
  return Collections.unmodifiableMap(m);
 }
 public static byte[] read(Path p,int max)throws IOException{
  if(!Files.isRegularFile(p)||Files.size(p)>max)throw new IOException("Missing or oversized file");return Files.readAllBytes(p);
 }
 public static void atomic(Path p,byte[]b)throws IOException{
  Files.createDirectories(p.getParent());Path t=Files.createTempFile(p.getParent(),".sao-",".pending");
  try{Files.write(t,b);try{Files.move(t,p,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(t,p,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(t);}
 }
 private static boolean allowed(URI u,boolean initial){
  if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;
  String h=u.getHost(),p=u.getPath();if(h==null||p==null)return false;
  if(initial)return (h.equals("raw.githubusercontent.com")&&p.startsWith("/ga200604101111/SAOMCZH/"))||(h.equals("github.com")&&p.startsWith("/ga200604101111/SAOMCZH/releases/download/"));
  return h.equals("github.com")||h.equals("raw.githubusercontent.com")||h.equals("release-assets.githubusercontent.com")||h.equals("objects.githubusercontent.com");
 }
 public static byte[] fetch(String url,int max)throws Exception{
  if(max<1||max>MAX_MODULE)throw new IOException("Invalid download limit");URI u=URI.create(url);if(!allowed(u,true))throw new SecurityException("Untrusted download source");
  for(int redirects=0;redirects<5;redirects++){
   if(!allowed(u,false))throw new SecurityException("Untrusted redirect host");
   HttpsURLConnection c=(HttpsURLConnection)u.toURL().openConnection();c.setConnectTimeout(6000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","SAO-ZH-Modular/1");
   try{int status=c.getResponseCode();if(status>=300&&status<=308){String l=c.getHeaderField("Location");if(l==null)throw new IOException("Missing redirect");u=u.resolve(l);continue;}
    if(status!=200)throw new IOException("HTTP "+status);
    try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[]buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>max)throw new IOException("Download limit exceeded");out.write(buf,0,n);}return out.toByteArray();}
   }finally{c.disconnect();}
  }throw new IOException("Too many redirects");
 }
 public static Map<String,String> selectModule(Path dir)throws Exception{
  Path pending=dir.resolve("pending.properties"),pendingSig=dir.resolve("pending.sig"),current=dir.resolve("current.properties"),currentSig=dir.resolve("current.sig");
  if(Files.isRegularFile(pending)&&Files.isRegularFile(pendingSig))try{
   byte[]b=read(pending,65536),sig=read(pendingSig,4096);Map<String,String>spec=descriptor(b,sig);module(dir,spec);
   if(Files.isRegularFile(current)&&Files.isRegularFile(currentSig)){
    byte[]old=read(current,65536),oldSig=read(currentSig,4096);descriptor(old,oldSig);atomic(dir.resolve("previous.sig"),oldSig);atomic(dir.resolve("previous.properties"),old);
   }
   atomic(currentSig,sig);atomic(current,b);Files.deleteIfExists(pending);Files.deleteIfExists(pendingSig);Agent.log("EXTERNAL_BINARY_PROMOTED "+dir.getFileName());
  }catch(Exception rejected){Agent.log("PENDING_BINARY_REJECTED_KEEP_PREVIOUS "+rejected.getClass().getSimpleName());}
  Exception failure=null;
  for(String slot:new String[]{"current","previous"})try{
   Map<String,String>m=descriptor(read(dir.resolve(slot+".properties"),65536),read(dir.resolve(slot+".sig"),4096));module(dir,m);if(slot.equals("previous"))Agent.log("EXTERNAL_BINARY_PREVIOUS_RECOVERED "+dir.getFileName());return m;
  }catch(Exception bad){failure=bad;}
  throw failure==null?new IOException("No usable module"):failure;
 }
 public static Path module(Path dir,Map<String,String>spec)throws Exception{
  String digest=spec.get("sha256");if(digest==null||!digest.matches("[0-9a-f]{64}"))throw new SecurityException("Bad module hash");
  int size=Integer.parseInt(spec.get("bytes"));if(size<128||size>MAX_MODULE)throw new SecurityException("Bad module size");
  Path p=dir.resolve(digest+".jar");byte[]b=null;
  if(Files.isRegularFile(p)&&Files.size(p)==size){byte[]x=read(p,size);if(hash(x).equals(digest))b=x;}
  if(b==null){b=fetch(spec.get("url"),size);if(b.length!=size||!hash(b).equals(digest))throw new SecurityException("Module checksum mismatch");atomic(p,b);}
  return p;
 }
}
