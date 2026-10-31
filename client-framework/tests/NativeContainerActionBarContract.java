package sao.zh.engine;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Prove both vanilla client-class hook sites exist without changing original bytes. */
public final class NativeContainerActionBarContract {
 private static int checks;
 private static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 private static Map<String,Object> base(String id,String owner,String method,String desc,String kind,String section){
  Map<String,Object> m=new LinkedHashMap<>();
  m.put("id",id);m.put("class",owner);m.put("method",method);
  m.put("descriptor",desc);m.put("kind",kind);m.put("section",section);
  return m;
 }
 private static Script.Hook screen(){
  var m=base("named-container:visible-title","net/minecraft/class_465","method_2388",
     "(Lnet/minecraft/class_332;II)V","field","names");
  m.put("member_class","net/minecraft/class_465");
  m.put("member","field_22785");
  m.put("member_descriptor","Lnet/minecraft/class_2561;");
  return new Script.Hook(m);
 }
 private static Script.Hook hud(){
  var m=base("dynamic-translation:game-actionbar-display","net/minecraft/class_329",
    "method_1758","(Lnet/minecraft/class_2561;Z)V","argument","messages");
  m.put("argument",0);
  return new Script.Hook(m);
 }
 private static void forbid(Map<String,Object> m){
  try{new Script.Hook(m);}catch(IllegalArgumentException expected){checks++;return;}
  throw new AssertionError("Unsafe source authorized: "+m);
 }
 private static byte[] nativeClass(ZipFile z,String owner)throws Exception{
  var e=z.getEntry(owner+".class");
  ok(e!=null,"Missing native class "+owner);return z.getInputStream(e).readAllBytes();
 }
 public static void main(String[]args)throws Exception{
  Script.Hook title=screen(),bar=hud();
  var u=base(title.id,title.owner,title.method,title.descriptor,"field","names");
  u.put("member_class","net/minecraft/class_465");
  u.put("member","field_29347");u.put("member_descriptor","Lnet/minecraft/class_2561;");
  forbid(u); // Cannot rewrite the player's inventory header.
  u=base(title.id,title.owner,title.method,title.descriptor,"field","names");
  u.put("member_class","net/minecraft/class_465");u.put("member","field_22785");
  u.put("member_descriptor","Lnet/minecraft/class_2561;");u.put("capture","world");
  forbid(u); // Never collect player-created container titles.
  u=base(bar.id,bar.owner,bar.method,bar.descriptor,"argument","messages");
  u.put("argument",1);forbid(u); // Boolean title-format mode must be unchanged.
  u=base(bar.id,bar.owner,bar.method,bar.descriptor,"argument","messages");
  u.put("argument",0);u.put("capture","system");forbid(u); // No bulk Actionbar capture.
  try(ZipFile jar=new ZipFile(Path.of(args[0]).toFile())){
   for(var test:List.of(title,bar)){
    byte[] original=nativeClass(jar,test.owner);
    byte[] patched=new RuleTransformer().patch(test.owner,original,List.of(test));
    ok(patched.length>original.length,"Expected new native display call "+test.id);
    int[] injects={0},titleReads={0};
    new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM9){
     @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]except){
      if(!n.equals(test.method)||!d.equals(test.descriptor))return null;
      return new MethodVisitor(Opcodes.ASM9){
       @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
        if(owner.equals("sao/zh/api/Dispatch")&&name.equals("value"))injects[0]++;
       }
       @Override public void visitFieldInsn(int op,String owner,String name,String d){
        if(op==Opcodes.GETFIELD&&owner.equals("net/minecraft/class_465")
         &&name.equals("field_29347"))titleReads[0]++;
       }
      };
     }
    },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
    ok(injects[0]==1,"Must project exactly one display site "+test.id);
    if(test==title)ok(titleReads[0]==1,"Vanilla player's inventory caption must stay native");
    final Set<String> oldApi=new TreeSet<>(),newApi=new TreeSet<>();
    for(var pair:List.of(Map.entry(original,oldApi),Map.entry(patched,newApi)))
     new ClassReader(pair.getKey()).accept(new ClassVisitor(Opcodes.ASM9){
      @Override public FieldVisitor visitField(int a,String n,String d,String s,Object x){pair.getValue().add("F "+n+" "+d);return null;}
      @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){pair.getValue().add("M "+n+" "+d);return null;}
     },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
    ok(oldApi.equals(newApi),"Vanilla class ABI changed "+test.id);
   }
  }
  System.out.println("NATIVE_VANILLA_CONTAINER_ACTIONBAR_PASS checks="+checks+
     " original_bytecode_untouched=true player_container_titles_preserved=true");
 }
}
