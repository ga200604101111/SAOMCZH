package sao.zh.compat16;
import java.lang.instrument.ClassFileTransformer;import java.security.ProtectionDomain;import org.objectweb.asm.*;
/** External compatibility adapter for the native fallback color predicate. */
public final class FontColorTransformer implements ClassFileTransformer {
 private static final String OWNER="fr/sao/font/msdf/MsdfRenderer",POLICY="sao/zh/compat16/FontColorPolicy";
 public byte[]transform(ClassLoader loader,String name,Class<?>redef,ProtectionDomain domain,byte[]bytes){
  if(!OWNER.equals(name)||bytes==null)return null;
  try{byte[]result=patch(bytes);DirectAgent.log("FONT_COLOR_CJK_TEXT_TINT_PATCHED");return result;}
  catch(Exception|LinkageError error){DirectAgent.log("FONT_COLOR_KEEP_NATIVE "+error.getClass().getSimpleName());return null;}
 }
 static byte[]patch(byte[]bytes){
  ClassReader reader=new ClassReader(bytes);if(!OWNER.equals(reader.getClassName()))throw new IllegalArgumentException("Unexpected renderer");
  final int[]found={0},returns={0},wideStart={0},wideEnd={0},calls={0};
  reader.accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){if(!n.equals("icon")||!d.equals("(I)Z"))return null;if((a&Opcodes.ACC_STATIC)==0)throw new IllegalArgumentException("Icon predicate changed");found[0]++;return new MethodVisitor(Opcodes.ASM9){public void visitIntInsn(int op,int operand){if(operand==0x3000)wideStart[0]++;}public void visitLdcInsn(Object v){if(Integer.valueOf(0xd7ff).equals(v))wideEnd[0]++;}public void visitInsn(int op){if(op==Opcodes.IRETURN)returns[0]++;}public void visitMethodInsn(int op,String o,String n,String d,boolean i){calls[0]++;}};}},ClassReader.SKIP_DEBUG);
  if(found[0]!=1||returns[0]!=1||wideStart[0]!=1||wideEnd[0]!=1||calls[0]!=0)throw new IllegalArgumentException("Unrecognized native CJK icon predicate");
  ClassWriter out=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
  reader.accept(new ClassVisitor(Opcodes.ASM9,out){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){MethodVisitor base=super.visitMethod(a,n,d,s,e);if(!n.equals("icon")||!d.equals("(I)Z"))return base;return new MethodVisitor(Opcodes.ASM9,base){public void visitInsn(int op){if(op==Opcodes.IRETURN){super.visitVarInsn(Opcodes.ILOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,POLICY,"correct","(ZI)Z",false);}super.visitInsn(op);}};}},0);
  return out.toByteArray();
 }
}
