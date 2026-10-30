package sao.zh.compat16;
import java.io.*;import java.lang.instrument.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
/** Independently switchable local raster cache. Does not start a game or modify packs. */
public final class GlyphCacheService {
 private static ScheduledExecutorService monitor;private static boolean installed;
 private GlyphCacheService(){}
 private static Properties read(Path p)throws IOException{
  Properties v=new Properties();if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))return v;
  if(Files.isSymbolicLink(p)||Files.size(p)>8192)throw new IOException("Invalid glyph cache configuration");
  try(InputStream in=Files.newInputStream(p)){v.load(in);}return v;
 }
 private static long budget(Properties p,String key,int fallback,int min,int max){int n=Integer.parseInt(p.getProperty(key,""+fallback));if(n<min||n>max)throw new IllegalArgumentException("Budget out of range "+key);return n*1024L*1024;}
 public static synchronized void start(Instrumentation inst,Path extensionRoot){
  if(installed)return;
  Path folder=extensionRoot.resolve("SAO_ZH_GlyphCache"),control=folder.resolve("cache.properties");
  try{
   Properties config=read(control);if(!Boolean.parseBoolean(config.getProperty("enabled","false")))return;
   for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("net.minecraft.class_395")||c.getName().equals("net.minecraft.class_395$class_397$1"))throw new IOException("Font renderer already loaded; restart required");
   Class<?>bridge=Class.forName("sao.zh.compat16.GlyphCacheBridge",true,null);Method stop=bridge.getMethod("shutdown");
   bridge.getMethod("configure",Path.class,long.class,long.class).invoke(null,folder.resolve("pages-v1"),budget(config,"memoryMiB",32,8,128),budget(config,"diskMiB",256,32,1024));
   try{inst.addTransformer(new GlyphCacheTransformer(),false);}catch(Throwable e){stop.invoke(null);throw e;}
   installed=true;Runtime.getRuntime().addShutdownHook(new Thread(()->{try{stop.invoke(null);}catch(Exception ignored){}},"SAO-GlyphCache-Shutdown"));
   monitor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"SAO-GlyphCache-Control");t.setDaemon(true);return t;});
   monitor.scheduleWithFixedDelay(()->{
    try{if(!Boolean.parseBoolean(read(control).getProperty("enabled","false"))){stop.invoke(null);monitor.shutdown();System.out.println("[SAO-GlyphCache] DISABLED_KEEP_NATIVE");}}
    catch(Exception e){try{stop.invoke(null);}catch(Exception ignored){}monitor.shutdown();System.out.println("[SAO-GlyphCache] CONTROL_ERROR_KEEP_NATIVE");}
   },2,2,TimeUnit.SECONDS);
   System.out.println("[SAO-GlyphCache] ENABLED native-ttf disk-pages=v1 memoryMiB="+config.getProperty("memoryMiB","32")+" diskMiB="+config.getProperty("diskMiB","256"));
  }catch(Throwable error){System.err.println("[SAO-GlyphCache] DISABLED_KEEP_NATIVE "+error.getClass().getSimpleName());}
 }
}
