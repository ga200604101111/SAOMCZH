package sao.zh.compat16;
import java.lang.instrument.ClassFileTransformer;import java.security.ProtectionDomain;import java.util.*;import org.objectweb.asm.*;
/** Exact Minecraft 1.21.4/SAOUI signatures only; no stack-frame rewriting or return-value changes. */
public final class PerformanceTransformer implements ClassFileTransformer {
 private static final String BRIDGE="sao/zh/compat16/PerformanceBridge";
 private record Target(String method,String descriptor,int channel){}
 private static final Map<String,List<Target>> TARGETS=Map.of(
  "net/minecraft/class_310",List.of(new Target("method_1523","(Z)V",0),new Target("method_1574","()V",1)),
  "net/minecraft/class_757",List.of(new Target("method_3192","(Lnet/minecraft/class_9779;Z)V",2)),
  "net/minecraft/class_1041",List.of(new Target("method_15998","(Lnet/minecraft/class_10219;)V",3)),
  "net/minecraft/class_1011",List.of(new Target("method_4316","(Lorg/lwjgl/util/freetype/FT_Face;I)Z",4)),
  "fr/sao/font/msdf/MsdfRenderer",List.of(new Target("measure","(Lnet/minecraft/class_2960;Ljava/lang/String;Lnet/minecraft/class_5481;Lnet/minecraft/class_2960;)I",5),
   new Target("drawLine","(Lnet/minecraft/class_332;Lnet/minecraft/class_2960;Ljava/lang/String;Lnet/minecraft/class_5481;FFIZ)Z",6)));
 public static Set<String> owners(){return TARGETS.keySet();}
 public byte[] transform(ClassLoader loader,String name,Class<?> previous,ProtectionDomain domain,byte[] bytes){
  if(!TARGETS.containsKey(name))return null;
  try{return patch(name,bytes);}catch(Exception|LinkageError e){System.err.println("[SAO-Perf] TARGET_SKIPPED "+name+" "+e.getClass().getSimpleName());return null;}
 }
 public static byte[] patch(String name,byte[] bytes){
  List<Target> targets=TARGETS.get(name);if(targets==null)return bytes;
  ClassReader r=new ClassReader(bytes);if(!r.getClassName().equals(name))throw new IllegalArgumentException("Class identity mismatch");
  ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);Set<Target>seen=new HashSet<>();
  r.accept(new ClassVisitor(Opcodes.ASM9,w){
   public MethodVisitor visitMethod(int access,String method,String desc,String sig,String[]exceptions){
    MethodVisitor mv=super.visitMethod(access,method,desc,sig,exceptions);
    Target target=targets.stream().filter(x->x.method.equals(method)&&x.descriptor.equals(desc)).findFirst().orElse(null);
    if(target==null)return mv;if((access&(Opcodes.ACC_NATIVE|Opcodes.ACC_ABSTRACT))!=0)throw new IllegalArgumentException("Cannot time native/abstract body");seen.add(target);
    return new MethodVisitor(Opcodes.ASM9,mv){
     private void call(String n){super.visitLdcInsn(target.channel);super.visitMethodInsn(Opcodes.INVOKESTATIC,BRIDGE,n,"(I)V",false);}
     public void visitCode(){super.visitCode();call("begin");}
     public void visitInsn(int opcode){
      // Do not instrument ATHROW: it can be caught inside this method. Exceptional
      // exits are omitted, and the next begin replaces the abandoned timestamp.
      if(opcode>=Opcodes.IRETURN&&opcode<=Opcodes.RETURN)call("end");super.visitInsn(opcode);
     }
    };
   }
  },0);
  if(seen.size()!=targets.size())throw new IllegalArgumentException("Unsupported target signatures: "+seen.size()+"/"+targets.size());
  return w.toByteArray();
 }
}
