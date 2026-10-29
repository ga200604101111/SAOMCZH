package sao.zh.compat16;
import org.objectweb.asm.*;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/** Adds only the early splash hooks; the existing 18 translation targets are unchanged. */
public final class SplashTransformer implements ClassFileTransformer {
 public static final String TARGET="fr/sao/saoui/splash/SplashOverlayRenderer";
 private static final String BRIDGE="sao/zh/compat16/SplashBridge";
 public byte[] transform(ClassLoader loader,String name,Class<?> redefined,ProtectionDomain domain,byte[] bytes){
  if(!TARGET.equals(name))return null;
  try{byte[]out=patch(bytes);DirectAgent.log("STARTUP_SPLASH_TRANSFORMED "+name);return out;}
  catch(Throwable e){DirectAgent.log("STARTUP_SPLASH_PATCH_SKIPPED "+e.getClass().getSimpleName());return null;}
 }
 public static byte[] patch(byte[]input){
  ClassReader r=new ClassReader(input);ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);Set<String>found=new HashSet<>();
  r.accept(new ClassVisitor(Opcodes.ASM9,w){
   public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[]ex){
    MethodVisitor parent=super.visitMethod(access,name,desc,sig,ex);
    boolean map=name.equals("readSplashLang")&&desc.equals("(Ljava/lang/String;)Ljava/util/Map;");
    boolean draw=(name.equals("drawAtlasText")||name.equals("drawPhraseAtlasText"))&&desc.equals("(Lnet/minecraft/class_332;Lnet/minecraft/class_310;Ljava/lang/String;IIII)V");
    boolean measure=name.equals("measureLineWidthPx")&&desc.equals("(Ljava/lang/String;I)I");
    boolean fit=name.equals("computeFittedCharDestH")&&desc.equals("(Lnet/minecraft/class_310;Ljava/lang/String;II)I");
    boolean wrap=name.equals("wrapPhraseRaw")&&desc.equals("(Ljava/lang/String;II)Ljava/util/List;");
    if(!(map||draw||measure||fit||wrap))return parent;found.add(name);
    return new MethodVisitor(Opcodes.ASM9,parent){
     void call(String method,String d){super.visitMethodInsn(Opcodes.INVOKESTATIC,BRIDGE,method,d,false);}
     public void visitCode(){
      super.visitCode();Label fallback=new Label();
      if(draw){
       for(int n=1;n<=3;n++)super.visitVarInsn(Opcodes.ALOAD,n);
       for(int n=4;n<=7;n++)super.visitVarInsn(Opcodes.ILOAD,n);
       super.visitLdcInsn(Type.getObjectType(TARGET));
       call("draw","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;IIIILjava/lang/Class;)Z");
       super.visitJumpInsn(Opcodes.IFEQ,fallback);super.visitInsn(Opcodes.RETURN);super.visitLabel(fallback);super.visitFrame(Opcodes.F_SAME,0,null,0,null);
      }else if(measure||fit){
       super.visitVarInsn(Opcodes.ALOAD,fit?2:1);super.visitVarInsn(Opcodes.ILOAD,fit?3:2);
       if(fit)super.visitVarInsn(Opcodes.ILOAD,4);
       call(fit?"fit":"measureChinese",fit?"(Ljava/lang/String;II)I":"(Ljava/lang/String;I)I");
       super.visitInsn(Opcodes.DUP);super.visitJumpInsn(Opcodes.IFLT,fallback);super.visitInsn(Opcodes.IRETURN);
       super.visitLabel(fallback);super.visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{Opcodes.INTEGER});super.visitInsn(Opcodes.POP);
      }else if(wrap){
       super.visitVarInsn(Opcodes.ALOAD,1);super.visitVarInsn(Opcodes.ILOAD,2);super.visitVarInsn(Opcodes.ILOAD,3);
       call("wrap","(Ljava/lang/String;II)Ljava/util/List;");
       super.visitInsn(Opcodes.DUP);super.visitJumpInsn(Opcodes.IFNULL,fallback);super.visitInsn(Opcodes.ARETURN);
       super.visitLabel(fallback);super.visitFrame(Opcodes.F_SAME1,0,null,1,new Object[]{"java/util/List"});super.visitInsn(Opcodes.POP);
      }
     }
     public void visitInsn(int opcode){
      if(map&&opcode==Opcodes.ARETURN){super.visitVarInsn(Opcodes.ALOAD,0);call("translations","(Ljava/util/Map;Ljava/lang/String;)Ljava/util/Map;");}
      super.visitInsn(opcode);
     }
    };
   }
  },0);
  if(found.size()!=6)throw new IllegalStateException("Splash method contract changed: "+found);
  return w.toByteArray();
 }
 public static void main(String[]args)throws Exception{
  try(ZipFile z=new ZipFile(args[0]);InputStream in=z.getInputStream(z.getEntry(TARGET+".class"));ByteArrayOutputStream b=new ByteArrayOutputStream()){
   byte[]temp=new byte[8192];int n;while((n=in.read(temp))!=-1)b.write(temp,0,n);byte[]patched=patch(b.toByteArray());new ClassReader(patched);
   System.out.println("STARTUP_SPLASH_BYTECODE_PATCH_PASS methods=6 bytes="+patched.length);
  }
 }
}
