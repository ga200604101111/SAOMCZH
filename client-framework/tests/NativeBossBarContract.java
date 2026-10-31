package sao.zh.engine;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/**
 * Exact MC 1.21.4 BossBarHud site: getName()->[translate]->TextRenderer.getWidth
 * ->DrawContext.drawText. BossBar packet/model data must remain byte-for-byte untouched.
 */
public final class NativeBossBarContract {
 private static int checks;
 static void ok(boolean truth,String why){checks++;if(!truth)throw new AssertionError(why);}
 private static Map<String,Object> base(){
  Map<String,Object> m=new LinkedHashMap<>();
  m.put("id","worlds-locations:vanilla-bossbar-title");
  m.put("class","net/minecraft/class_337");
  m.put("method","method_1796");
  m.put("descriptor","(Lnet/minecraft/class_332;)V");
  m.put("kind","call");
  m.put("section","names");
  m.put("member_class","net/minecraft/class_345");
  m.put("member","method_5414");
  m.put("member_descriptor","()Lnet/minecraft/class_2561;");
  m.put("sites",1);
  return m;
 }
 static void mustReject(String k,Object v){
  Map<String,Object> bad=base();bad.put(k,v);
  try{new Script.Hook(bad);}
  catch(IllegalArgumentException expected){checks++;return;}
  throw new AssertionError("BossBar hook should not accept "+k+"="+v);
 }
 static Set<String> abi(byte[] bytes){
  Set<String> a=new TreeSet<>();
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
   @Override public FieldVisitor visitField(int access,String name,String desc,String sig,Object value){
    a.add("F "+name+" "+desc);return null;
   }
   @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex){
    a.add("M "+name+" "+desc);return null;
   }
  },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  return a;
 }
 public static void main(String[] args)throws Exception {
  Script.Hook h=new Script.Hook(base());
  for(var entry:List.<Map.Entry<String,Object>>of(
   Map.entry("id","worlds-locations:other"),
   Map.entry("class","net/minecraft/class_329"),
   Map.entry("method","method_1799"),
   Map.entry("descriptor","()V"),
   Map.entry("kind","return"),
   Map.entry("section","messages"),
   Map.entry("member_class","net/minecraft/class_1259"),
   Map.entry("member","method_5412"),
   Map.entry("member_descriptor","()Ljava/lang/String;"),
   Map.entry("sites",2),
   Map.entry("capture","bossbar"),
   Map.entry("locale","en_us")
  ))mustReject(entry.getKey(),entry.getValue());

  try(ZipFile original=new ZipFile(Path.of(args[0]).toFile())){
   ZipEntry entry=original.getEntry("net/minecraft/class_337.class");
   ok(entry!=null,"Real Minecraft BossBarHud missing");
   byte[] old=original.getInputStream(entry).readAllBytes();
   byte[] patched=new RuleTransformer().patch("net/minecraft/class_337",old,List.of(h));
   ok(patched.length>old.length,"Native hook not inserted");
   ok(abi(old).equals(abi(patched)),"BossBarHud native ABI modified");
   List<String> calls=new ArrayList<>();
   new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM9){
    @Override public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] e){
     if(!name.equals("method_1796")||!desc.equals("(Lnet/minecraft/class_332;)V"))return null;
     return new MethodVisitor(Opcodes.ASM9){
      @Override public void visitMethodInsn(int op,String owner,String method,String type,boolean iface){
       if(owner.equals("net/minecraft/class_345")&&method.equals("method_5414"))
        calls.add("name");
       if(owner.equals("sao/zh/api/Dispatch")&&method.equals("value"))
        calls.add("projection");
       if(owner.equals("net/minecraft/class_327")&&method.equals("method_27525"))
        calls.add("width");
       if(owner.equals("net/minecraft/class_332")&&method.equals("method_27535"))
        calls.add("draw");
      }
     };
    }
   },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   ok(calls.equals(List.of("name","projection","width","draw")),"Name translation must precede both width and drawing: "+calls);
  }
  Map<String,Map<String,String>> data=Map.of(
   "names",Map.of("Ambush!","伏擊!","Ambush — wave 1/3","伏擊 — 第 1/3 波",
     "Ambush — wave 2/3","伏擊 — 第 2/3 波",
     "Ambush — wave 3/3","伏擊 — 第 3/3 波",
     "Shepherd Ivo","牧羊人伊沃"),
   "items",Map.of("Iron Axe","鐵斧"),
   "lines",Map.of(),
   "messages",Map.of());
  ok(RenderTextPipeline.resolve(h,"Ambush — wave 1/3",data).equals("伏擊 — 第 1/3 波"),"BossBar 1/3 exact");
  ok(RenderTextPipeline.resolve(h,"Ambush — wave 2/3",data).equals("伏擊 — 第 2/3 波"),"BossBar 2/3 exact");
  ok(RenderTextPipeline.resolve(h,"Ambush — wave 3/3",data).equals("伏擊 — 第 3/3 波"),"BossBar 3/3 exact");
  ok(RenderTextPipeline.resolve(h,"Ambush! ",data).equals("伏擊! "),"BossBar trailing space preserved");
  ok(RenderTextPipeline.resolve(h,"Iron Axe",data).equals("鐵斧"),"BossBar may reuse approved shared item name");
  ok(RenderTextPipeline.resolve(h,"YingHuang09",data).equals("YingHuang09"),"Player identifiers unchanged");
  ok(RenderTextPipeline.resolve(h,"YingHuang09: hello",data).equals("YingHuang09: hello"),"Player chat-like content unchanged");
  ok(RenderTextPipeline.resolve(h,"Unknown Boss",data).equals("Unknown Boss"),"Unknown BossBar text unchanged");
  System.out.println("NATIVE_VANILLA_BOSSBAR_PASS assertions="+checks+" display_only=true measure_and_draw_same_text=true");
 }
}
