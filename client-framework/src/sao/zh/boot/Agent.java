package sao.zh.boot;
import java.io.*;import java.lang.instrument.*;import java.net.*;import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.time.Instant;
/** Stable, small launch bridge. All SAO-specific features are external. */
public final class Agent {
 private static Object runtime;private static URLClassLoader loader;private static Path root;
 public static Object runtime(){return runtime;}
 public static Path root(){return root;}
 public static void log(String s){
  System.out.println("[SAO-ZH-Kernel] "+s);
  if(root!=null)try{Path p=root.resolve("SAO_ZH_Kernel.log");if(Files.exists(p)&&Files.size(p)>2*1024*1024)Files.move(p,p.resolveSibling("SAO_ZH_Kernel.previous.log"),StandardCopyOption.REPLACE_EXISTING);Files.writeString(p,Instant.now()+" "+s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}
 }
 public static void premain(String args,Instrumentation inst){
  try{
   root=Paths.get(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().getParent();
   long agents=java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().stream().filter(x->x.startsWith("-javaagent:")&&x.matches(".*SAO_(?:ZH|TW)_Agent_v[0-9]+\\.jar.*")).count();
   if(agents>1){log("MULTIPLE_SAO_AGENTS_DETECTED_SKIP_NEW_KERNEL");return;}
   if(Boolean.getBoolean("sao.zh.kernel.active")){log("DUPLICATE_KERNEL_SKIPPED");return;}System.setProperty("sao.zh.kernel.active","true");
   if(!inst.isRetransformClassesSupported()){log("RETRANSFORM_UNSUPPORTED_GAME_UNCHANGED");return;}
   Path dir=root.resolve("SAO_ZH_Extensions/runtime");Files.createDirectories(dir);
   Path manifest=dir.resolve("current.properties"),sig=dir.resolve("current.sig");
   if(!Files.isRegularFile(manifest)||!Files.isRegularFile(sig)){
    String base="https://raw.githubusercontent.com/ga200604101111/SAOMCZH/main/extensions/v1/runtime";
    byte[]body=Verified.fetch(base+".properties",65536),signature=Verified.fetch(base+".sig",4096);Verified.descriptor(body,signature);
    Verified.atomic(sig,signature);Verified.atomic(manifest,body);
   }
   Map<String,String>spec=Verified.selectModule(dir);
   Path jar=Verified.module(dir,spec);
   // Parent shares only generic public kernel APIs. Engine owns its isolated implementation and ASM.
   loader=new URLClassLoader(new URL[]{jar.toUri().toURL()},Agent.class.getClassLoader());
   Class<?>entry=Class.forName("sao.zh.engine.Engine",true,loader);
   runtime=entry.getMethod("start",Instrumentation.class,Path.class,Path.class,String.class).invoke(null,inst,root,jar,args==null?"":args);
   log("EXTERNAL_RUNTIME_READY version="+spec.get("version")+" retransform=true");
  }catch(Throwable error){runtime=null;Throwable t=error.getCause()==null?error:error.getCause();log("KERNEL_DISABLED_KEEP_GAME "+t.getClass().getSimpleName()+" "+String.valueOf(t.getMessage()));}
 }
}
