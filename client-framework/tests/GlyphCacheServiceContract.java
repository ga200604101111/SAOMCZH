package sao.zh.compat16;
import java.lang.instrument.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.concurrent.atomic.AtomicInteger;
public final class GlyphCacheServiceContract {
 static int checks;static void ok(boolean c,String m){checks++;if(!c)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  Class.forName("sao.zh.compat16.DirectAgent").getDeclaredMethods(); // verifies the stable patched entrypoint without launching it.
  Path root=Files.createTempDirectory("glyph-service-");AtomicInteger installed=new AtomicInteger();
  Instrumentation inst=(Instrumentation)Proxy.newProxyInstance(Instrumentation.class.getClassLoader(),new Class<?>[]{Instrumentation.class},(p,m,a)->{
   if(m.getName().equals("getAllLoadedClasses"))return new Class<?>[0];if(m.getName().equals("addTransformer")){installed.incrementAndGet();return null;}
   if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==long.class)return 0L;if(m.getReturnType()==int.class)return 0;return null;
  });
  GlyphCacheService.start(inst,root);ok(installed.get()==0&&!GlyphCacheBridge.enabled(),"Default-off cache installed a transformer");
  Path control=root.resolve("SAO_ZH_GlyphCache/cache.properties");Files.createDirectories(control.getParent());Files.writeString(control,"enabled=true\nmemoryMiB=8\ndiskMiB=32\n");
  GlyphCacheService.start(inst,root);ok(installed.get()==1&&GlyphCacheBridge.enabled(),"Opt-in cache not started");
  ok(GlyphCacheBridge.class.getClassLoader()==null,"Cache helper is not bootstrap-visible");
  GlyphCacheService.start(inst,root);ok(installed.get()==1,"Duplicate cache installation");
  Files.writeString(control,"enabled=false\n");long limit=System.nanoTime()+8_000_000_000L;while(GlyphCacheBridge.enabled()&&System.nanoTime()<limit)Thread.sleep(30);
  ok(!GlyphCacheBridge.enabled(),"Live disable control was ignored");
  System.out.println("GLYPH_CACHE_SERVICE_BOOTSTRAP_PASS checks="+checks+" stable_entrypoint_verified=true no_game=true");
 }
}
