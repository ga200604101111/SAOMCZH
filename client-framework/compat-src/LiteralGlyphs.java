package sao.zh.compat16;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.security.MessageDigest;import java.util.*;import java.util.zip.*;
/** Display-only safe aliases generated from the active, already verified resource pack.
 * Original server icons are never remapped globally. No disk, network or reflection in
 * the render/lookup hot path; publish one immutable state after all asset checks pass. */
public final class LiteralGlyphs {
 private static final String CONTRACT="assets/saotw/glyph-routing.json";
 private record State(Map<Integer,Integer> forward,Map<Integer,Integer> reverse,int[] codes){}
 private static final State EMPTY=new State(Map.of(),Map.of(),new int[0]);
 private static volatile State state=EMPTY;
 private LiteralGlyphs(){}
 public static boolean active(){return state.codes.length!=0;}
 public static boolean isAlias(int cp){return Arrays.binarySearch(state.codes,cp)>=0;}
 private static boolean han(int cp){return cp>=0x3400&&cp<=0x4dbf||cp>=0x4e00&&cp<=0x9fff||cp>=0xf900&&cp<=0xfaff||cp>=0x20000&&cp<=0x323af;}
 private static byte[] read(ZipFile zip,String name,int max)throws IOException{
  ZipEntry entry=zip.getEntry(name);if(entry==null||entry.isDirectory()||entry.getSize()<0||entry.getSize()>max)throw new IOException("Missing/oversized glyph asset "+name);
  try(InputStream in=zip.getInputStream(entry)){byte[] b=in.readNBytes(max+1);if(b.length>max)throw new IOException("Glyph asset budget");return b;}
 }
 private static String hash(byte[] data)throws IOException{
  try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(Exception e){throw new IOException(e);}
 }
 private static Map<?,?> object(Object v)throws IOException{if(!(v instanceof Map))throw new IOException("Invalid glyph manifest");return (Map<?,?>)v;}
 private static int integer(Object v)throws IOException{if(!(v instanceof Number)||((Number)v).longValue()!=((Number)v).doubleValue())throw new IOException("Invalid glyph integer");return ((Number)v).intValue();}
 public static synchronized int configure(Path main)throws IOException{
  state=EMPTY;
  if(main==null||!Files.isRegularFile(main))return 0;
  try(ZipFile zip=new ZipFile(main.toFile())){
   if(zip.getEntry(CONTRACT)==null)return 0; // Previous packs keep previous behavior.
   Map<?,?> doc=object(MiniJson.parse(new String(read(zip,CONTRACT,512*1024),StandardCharsets.UTF_8)));
   if(integer(doc.get("schema_version"))!=1||!"zh_tw".equals(doc.get("locale")))throw new IOException("Unsupported glyph contract");
   Map<?,?> aliases=object(doc.get("aliases"));if(aliases.isEmpty()||aliases.size()>4096)throw new IOException("Alias count budget");
   Map<Integer,Integer> forward=new HashMap<>(),reverse=new HashMap<>();
   for(Map.Entry<?,?> e:aliases.entrySet()){
    int source;try{source=Integer.parseInt((String)e.getKey());}catch(Exception bad){throw new IOException("Invalid source glyph",bad);}
    int target=integer(e.getValue());
    if(!han(source)||target<0xe000||target>0xf8ff||forward.put(source,target)!=null||reverse.put(target,source)!=null)throw new IOException("Invalid/conflicting literal alias");
   }
   Map<?,?> assets=object(doc.get("assets_sha256"));if(assets.size()>512)throw new IOException("Asset inventory budget");
   int atlasCount=0;long bytes=0;
   for(Map.Entry<?,?> e:assets.entrySet()){
    if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String))throw new IOException("Invalid asset digest");
    String name=(String)e.getKey(),digest=(String)e.getValue();
    if(!name.startsWith("assets/")||name.contains("..")||name.indexOf('\\')>=0||!digest.matches("[0-9a-f]{64}"))throw new IOException("Unsafe asset path/digest");
    // The complete ZIP is SHA-verified by PackLifecycleManager. Check routing
    // metadata and small alias font payloads again, never decode textures here.
    if(!name.endsWith(".json")&&!name.endsWith(".ttf"))continue;
    byte[] raw=read(zip,name,8*1024*1024);bytes+=raw.length;if(bytes>96L*1024*1024)throw new IOException("Glyph metadata budget");
    if(!hash(raw).equals(digest))throw new IOException("Glyph asset digest mismatch "+name);
    if(name.endsWith(".json")&&name.contains("/msdf/")){
     Map<?,?> atlas=object(MiniJson.parse(new String(raw,StandardCharsets.UTF_8)));Object glyphs=atlas.get("glyphs");
     if(!(glyphs instanceof List))throw new IOException("Missing atlas glyphs");Set<Integer> present=new HashSet<>();
     for(Object g:(List<?>)glyphs)present.add(integer(object(g).get("unicode")));
     if(!present.containsAll(reverse.keySet()))throw new IOException("Partial alias atlas "+name);
     atlasCount++;
    }
   }
   if(atlasCount!=26||atlasCount!=integer(doc.get("atlas_count")))throw new IOException("Partial font family matrix");
   for(String weight:List.of("regular","bold"))if(!assets.containsKey("assets/saotw/font/literal-alias-"+weight+".ttf"))throw new IOException("Alias fallback font not verified");
   state=new State(Map.copyOf(forward),Map.copyOf(reverse),reverse.keySet().stream().mapToInt(Integer::intValue).sorted().toArray());
   return forward.size();
  }catch(RuntimeException e){throw new IOException("Invalid literal font assets",e);}
 }
 /** Map only newly translated natural-language characters; retain icon occurrences
  * inherited from the original server string. Unchanged text stays byte-for-byte. */
 public static String encodeTranslated(String source,String translated){
  State s=state;if(translated==null||s.codes.length==0||translated.equals(source)||translated.length()>16384)return translated;
  Set<Integer> inherited=null;
  if(source!=null&&source.length()<=16384){
   for(int i=0;i<source.length();){int cp=source.codePointAt(i);i+=Character.charCount(cp);
    if(s.forward.containsKey(cp)){if(inherited==null)inherited=new HashSet<>();inherited.add(cp);}
   }
  }
  StringBuilder result=null;
  for(int i=0;i<translated.length();){int start=i,cp=translated.codePointAt(i);i+=Character.charCount(cp);Integer alias=s.forward.get(cp);
   // A source glyph reused as both icon and prose is ambiguous. Preserve it rather
   // than guessing which occurrence is the original icon.
   if(alias!=null&&inherited!=null&&inherited.contains(cp))alias=null;
   if(alias!=null){if(result==null){result=new StringBuilder(translated.length());result.append(translated,0,start);}result.appendCodePoint(alias);}
   else if(result!=null)result.appendCodePoint(cp);
  }
  return result==null?translated:result.toString();
 }
 /** For dictionary matching only. Never changes the stored original Text tree. */
 public static String readable(String raw){
  State s=state;if(raw==null||s.codes.length==0||raw.length()>16384)return raw;StringBuilder out=null;
  for(int i=0;i<raw.length();){int start=i,cp=raw.codePointAt(i);i+=Character.charCount(cp);Integer original=s.reverse.get(cp);
   if(original!=null){if(out==null){out=new StringBuilder(raw.length());out.append(raw,0,start);}out.appendCodePoint(original);}
   else if(out!=null)out.appendCodePoint(cp);
  }
  return out==null?raw:out.toString();
 }
}
