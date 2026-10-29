package fixture;
import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.lang.reflect.*;import org.objectweb.asm.*;
/** Executes the actual native icon-predicate bytecode without loading a Minecraft window or GL context. */
public final class FontIconContract {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static byte[]extract(byte[]nativeBytes){ClassWriter out=new ClassWriter(ClassWriter.COMPUTE_MAXS);out.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"fixture/ExtractedNativeIcon",null,"java/lang/Object",null);new ClassReader(nativeBytes).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){return n.equals("icon")&&d.equals("(I)Z")?out.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,n,d,null,null):null;}},0);out.visitEnd();return out.toByteArray();}
 static final class Loader extends ClassLoader {Loader(){super(FontIconContract.class.getClassLoader());}Class<?>define(byte[]b){return defineClass(null,b,0,b.length);}}
 public static void main(String[]args)throws Exception{
  byte[]original;try(ZipFile z=new ZipFile(args[0])){original=z.getInputStream(z.getEntry("fr/sao/font/msdf/MsdfRenderer.class")).readAllBytes();}
  byte[]bytes=original;boolean fixed=args.length>1&&args[1].equals("fixed");
  if(fixed){Class<?>t=Class.forName("sao.zh.compat16.FontColorTransformer");Method p=t.getDeclaredMethod("patch",byte[].class);p.setAccessible(true);bytes=(byte[])p.invoke(null,original);ok(bytes!=null,"No patched bytes");}
  if(fixed){Class<?>policy=Class.forName("sao.zh.compat16.FontColorPolicy");int retained=(Integer)policy.getMethod("configure",Path.class,Path.class,Path.class).invoke(null,Path.of(args[2]),null,null);ok(retained>0,"Explicit bitmap icon list is empty");}
  Method icon=new Loader().define(extract(bytes)).getMethod("icon",int.class);
  for(int cp:"訓氾嶄匪米井彭文字測試".codePoints().toArray())ok(!(Boolean)icon.invoke(null,cp),"Chinese fallback wrongly classified as colored icon: U+"+Integer.toHexString(cp));
  for(int cp:new int[]{0xe07d,0xe9f0,0x1f4a5,0x1f600,0x4e26,0x4e2d,0x4e32,0x7530})ok((Boolean)icon.invoke(null,cp),"Real resource icon/PUA lost classification: U+"+Integer.toHexString(cp));
  for(int cp:new int[]{-1,0,32,65,0x00e9})ok(!(Boolean)icon.invoke(null,cp),"Latin/control behavior changed");
  int gray=0xff676767,gold=0xffffaa00;for(int color:new int[]{gray,gold})for(int cp:"訓米彭".codePoints().toArray()){int output=(Boolean)icon.invoke(null,cp)?color|0x00ffffff:color;ok(output==color,"CJK fallback tint forced white");}
  if(fixed){Class<?>t=Class.forName("sao.zh.compat16.FontColorTransformer");Object transformer=t.getConstructor().newInstance();Method transform=t.getMethod("transform",ClassLoader.class,String.class,Class.class,java.security.ProtectionDomain.class,byte[].class);ok(transform.invoke(transformer,null,"unrelated/Class",null,null,original)==null,"Unrelated class touched");ok(transform.invoke(transformer,null,"fr/sao/font/msdf/MsdfRenderer",null,null,new byte[]{1,2,3})==null,"Bad class did not fail open");}
  System.out.println("NATIVE_CJK_FALLBACK_ICON_TINT_POLICY_PASS checks="+checks+" no_GL=true fixed="+fixed);
 }
}
