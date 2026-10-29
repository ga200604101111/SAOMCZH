package sao.zh.compat16;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;import java.util.zip.*;
/** Color-only correction. Explicit server bitmap icons keep their original behavior; no metrics or GPU calls. */
public final class FontColorPolicy {
 private static volatile int[]bitmapHan;
 private FontColorPolicy(){}
 private static boolean han(int cp){return cp>=0x3400&&cp<=0x4dbf||cp>=0x4e00&&cp<=0x9fff||cp>=0xf900&&cp<=0xfaff||cp>=0x20000&&cp<=0x323af;}
 public static boolean correct(boolean original,int cp){if(LiteralGlyphs.isAlias(cp))return false;int[]icons=bitmapHan;return !original||icons==null||!han(cp)?original:Arrays.binarySearch(icons,cp)>=0;}
 public static int configure(Path main,Path support,Path server)throws IOException {
  // A complete allowlist is built before publication. Failure leaves the original renderer untouched.
  if(main==null||!Files.isRegularFile(main))throw new IOException("No verified main pack for icon inventory");
  SortedSet<Integer>icons=new TreeSet<>();int[]counts={0,0};readPack(main,icons,counts);if(support!=null&&Files.isRegularFile(support))readPack(support,icons,counts);
  if(server!=null){Path assets=server.resolve("assets");if(Files.isDirectory(assets))try(java.util.stream.Stream<Path>files=Files.walk(assets,4)){Iterator<Path>it=files.iterator();while(it.hasNext()){Path p=it.next();if(Files.isSymbolicLink(p)||!Files.isRegularFile(p))continue;String n=assets.relativize(p).toString().replace('\\','/');if(n.matches("[^/]+/font/[^/]+\\.json")){if(Files.size(p)>4*1024*1024)throw new IOException("Font metadata too large");read(Files.readAllBytes(p),icons,counts);}}}}
  if(counts[0]==0||icons.size()>4096)throw new IOException("Invalid font metadata inventory");int[]ready=icons.stream().mapToInt(Integer::intValue).toArray();bitmapHan=ready;return ready.length;
 }
 private static void readPack(Path path,Set<Integer>icons,int[]counts)throws IOException {
  try(ZipFile zip=new ZipFile(path.toFile())){Enumeration<? extends ZipEntry>all=zip.entries();while(all.hasMoreElements()){ZipEntry e=all.nextElement();String n=e.getName();if(!n.matches("assets/[^/]+/font/[^/]+\\.json"))continue;if(e.getSize()<0||e.getSize()>4*1024*1024)throw new IOException("Font metadata bound");try(InputStream in=zip.getInputStream(e)){byte[]b=in.readNBytes(4*1024*1024+1);if(b.length>4*1024*1024)throw new IOException("Font metadata bound");read(b,icons,counts);}}}
 }
 private static void read(byte[]bytes,Set<Integer>icons,int[]counts)throws IOException {
  if(++counts[0]>512||(counts[1]+=bytes.length)>32*1024*1024)throw new IOException("Font inventory budget");Object parsed;
  try{parsed=MiniJson.parse(new String(bytes,StandardCharsets.UTF_8));}catch(RuntimeException e){throw new IOException("Invalid font metadata",e);}
  if(!(parsed instanceof Map))return;Object providers=((Map<?,?>)parsed).get("providers");if(!(providers instanceof List))return;
  for(Object provider:(List<?>)providers){if(!(provider instanceof Map))continue;Map<?,?>p=(Map<?,?>)provider;Object type=p.get("type");if(!("bitmap".equals(type)||"minecraft:bitmap".equals(type)))continue;Object chars=p.get("chars");if(!(chars instanceof List))continue;
   for(Object row:(List<?>)chars)if(row instanceof String){String s=(String)row;if(s.length()>8192)throw new IOException("Glyph row bound");s.codePoints().filter(FontColorPolicy::han).forEach(icons::add);}
  }
 }
}
