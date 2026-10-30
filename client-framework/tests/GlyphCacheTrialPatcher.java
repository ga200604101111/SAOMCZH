package sao.zh.compat16;
import java.nio.file.*;import org.objectweb.asm.*;
/** Add only GlyphCacheService startup to an otherwise byte-for-byte preserved stable compatibility JAR. */
public final class GlyphCacheTrialPatcher {
 public static byte[] patch(byte[]bytes){
  ClassReader r=new ClassReader(bytes);String self="sao/zh/compat16/DirectAgent";if(!self.equals(r.getClassName()))throw new IllegalArgumentException("Wrong stable compatibility entry");
  ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);int[]matches={0};
  r.accept(new ClassVisitor(Opcodes.ASM9,w){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){
   MethodVisitor mv=super.visitMethod(a,n,d,s,e);if(!n.equals("premain")||!d.equals("(Ljava/lang/String;Ljava/lang/instrument/Instrumentation;)V"))return mv;
   return new MethodVisitor(Opcodes.ASM9,mv){public void visitMethodInsn(int op,String o,String name,String desc,boolean iface){
    super.visitMethodInsn(op,o,name,desc,iface);
    if(op==Opcodes.INVOKEINTERFACE&&o.equals("java/lang/instrument/Instrumentation")&&name.equals("appendToBootstrapClassLoaderSearch")&&desc.equals("(Ljava/util/jar/JarFile;)V")){
     matches[0]++;super.visitVarInsn(Opcodes.ALOAD,1);super.visitFieldInsn(Opcodes.GETSTATIC,self,"logDirectory","Ljava/nio/file/Path;");super.visitMethodInsn(Opcodes.INVOKESTATIC,"sao/zh/compat16/GlyphCacheService","start","(Ljava/lang/instrument/Instrumentation;Ljava/nio/file/Path;)V",false);
    }
   }};
  }},0);
  if(matches[0]!=1)throw new IllegalArgumentException("Unexpected stable bootstrap install count: "+matches[0]);return w.toByteArray();
 }
 public static void main(String[]a)throws Exception{byte[] b=patch(Files.readAllBytes(Path.of(a[0])));Files.write(Path.of(a[1]),b);System.out.println("STABLE_COMPAT_GLYPH_ONLY_PATCH_PASS");}
}
