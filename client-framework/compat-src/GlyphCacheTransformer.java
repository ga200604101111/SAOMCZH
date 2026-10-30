package sao.zh.compat16;
import java.lang.instrument.*;import java.security.ProtectionDomain;import java.util.*;import org.objectweb.asm.*;
/** Fixed MC 1.21.4 call sites. No global NativeImage, font metrics or GPU alteration. */
public final class GlyphCacheTransformer implements ClassFileTransformer {
 public static final String FONT="net/minecraft/class_395",BAKER="net/minecraft/class_395$class_397$1";
 private static final String BRIDGE="sao/zh/compat16/GlyphCacheBridge",FACE="Lorg/lwjgl/util/freetype/FT_Face;";
 public byte[] transform(ClassLoader l,String name,Class<?>old,ProtectionDomain d,byte[]bytes){
  if(!name.equals(FONT)&&!name.equals(BAKER))return null;
  try{return patch(name,bytes);}catch(Exception|LinkageError e){System.err.println("[SAO-GlyphCache] UNSUPPORTED_TARGET_KEEP_NATIVE "+name+" "+e.getClass().getSimpleName());return null;}
 }
 public static byte[] patch(String name,byte[]bytes){
  if(!name.equals(FONT)&&!name.equals(BAKER))return bytes;
  ClassReader r=new ClassReader(bytes);if(!r.getClassName().equals(name))throw new IllegalArgumentException("Wrong glyph target");
  ClassWriter w=new ClassWriter(r,ClassWriter.COMPUTE_MAXS);int[]counts={0,0,0};
  r.accept(new ClassVisitor(Opcodes.ASM9,w){
   public MethodVisitor visitMethod(int flags,String method,String desc,String sig,String[]exceptions){
    MethodVisitor mv=super.visitMethod(flags,method,desc,sig,exceptions);
    boolean ctor=name.equals(FONT)&&method.equals("<init>")&&desc.equals("(Ljava/nio/ByteBuffer;"+FACE+"FFFFLjava/lang/String;)V");
    boolean close=name.equals(FONT)&&method.equals("close")&&desc.equals("()V");
    boolean bake=name.equals(BAKER)&&method.equals("method_2030")&&desc.equals("(II)V");
    if(!ctor&&!close&&!bake)return mv;
    if((flags&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE|Opcodes.ACC_STATIC))!=0)throw new IllegalArgumentException("Unsupported glyph method flags");
    return new MethodVisitor(Opcodes.ASM9,mv){
     public void visitCode(){super.visitCode();if(close){counts[1]++;super.visitVarInsn(Opcodes.ALOAD,0);super.visitFieldInsn(Opcodes.GETFIELD,FONT,"field_48383",FACE);super.visitMethodInsn(Opcodes.INVOKESTATIC,BRIDGE,"unregister","(Ljava/lang/Object;)V",false);}}
     public void visitMethodInsn(int op,String owner,String called,String descriptor,boolean iface){
      if(bake&&op==Opcodes.INVOKEVIRTUAL&&owner.equals("net/minecraft/class_1011")&&called.equals("method_4316")&&descriptor.equals('('+FACE+"I)Z")){
       counts[2]++;super.visitMethodInsn(Opcodes.INVOKESTATIC,BRIDGE,"renderGlyph","(Ljava/lang/Object;Ljava/lang/Object;I)Z",false);
      }else {
       super.visitMethodInsn(op,owner,called,descriptor,iface);
       // At RETURN Mojang has deliberately killed constructor locals in stack maps.
       // Register immediately after native size/transform setup, while args are live.
       if(ctor&&op==Opcodes.INVOKESTATIC&&owner.equals("org/lwjgl/util/freetype/FreeType")&&called.equals("FT_Set_Transform")&&descriptor.equals("("+FACE+"Lorg/lwjgl/util/freetype/FT_Matrix;Lorg/lwjgl/util/freetype/FT_Vector;)V")){
        counts[0]++;super.visitVarInsn(Opcodes.ALOAD,1);super.visitVarInsn(Opcodes.ALOAD,2);for(int i=3;i<=6;i++)super.visitVarInsn(Opcodes.FLOAD,i);super.visitMethodInsn(Opcodes.INVOKESTATIC,BRIDGE,"register","(Ljava/nio/ByteBuffer;Ljava/lang/Object;FFFF)V",false);
       }
      }
     }
    };
   }
  },0);
  if(name.equals(FONT)&&(counts[0]!=1||counts[1]!=1)||name.equals(BAKER)&&counts[2]!=1)throw new IllegalArgumentException("Incomplete native glyph interception "+Arrays.toString(counts));
  return w.toByteArray();
 }
}
