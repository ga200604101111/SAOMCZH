package sao.zh.compat16;
import java.nio.file.*;import java.io.*;import java.util.*;import java.util.zip.*;import org.objectweb.asm.*;
public final class PerformanceNativeContract {
 static int checks;static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static long count(int id){Map<?,?> snapshot=PerformanceBridge.snapshot();return ((Number)((Map<?,?>)((List<?>)snapshot.get("channels")).get(id)).get("count")).longValue();}
 static byte[] target(Path jar,String name)throws Exception{try(ZipFile z=new ZipFile(jar.toFile())){ZipEntry e=z.getEntry(name+".class");return e==null?null:z.getInputStream(e).readAllBytes();}}
 static byte[] fixture(){
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"net/minecraft/class_310",null,"java/lang/Object",null);
  MethodVisitor ctor=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(0,0);ctor.visitEnd();
  MethodVisitor tick=w.visitMethod(Opcodes.ACC_PUBLIC,"method_1574","()V",null,null);tick.visitCode();tick.visitInsn(Opcodes.RETURN);tick.visitMaxs(0,0);tick.visitEnd();
  MethodVisitor frame=w.visitMethod(Opcodes.ACC_PUBLIC,"method_1523","(Z)V",null,null);frame.visitCode();Label good=new Label();frame.visitVarInsn(Opcodes.ILOAD,1);frame.visitJumpInsn(Opcodes.IFEQ,good);frame.visitTypeInsn(Opcodes.NEW,"java/lang/IllegalStateException");frame.visitInsn(Opcodes.DUP);frame.visitLdcInsn("original exception");frame.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/IllegalStateException","<init>","(Ljava/lang/String;)V",false);frame.visitInsn(Opcodes.ATHROW);frame.visitLabel(good);frame.visitInsn(Opcodes.RETURN);frame.visitMaxs(0,0);frame.visitEnd();w.visitEnd();return w.toByteArray();
 }
 public static void main(String[]args)throws Exception{
  int classes=0,methods=0;
  for(String owner:PerformanceTransformer.owners()){
   byte[] raw=null;for(String jar:args){raw=target(Path.of(jar),owner);if(raw!=null)break;}ok(raw!=null,"Actual target absent "+owner);
   byte[] patched=PerformanceTransformer.patch(owner,raw);int[] invokes={0,0};
   new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){return new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int opcode,String o,String name,String desc,boolean iface){if(o.equals("sao/zh/compat16/PerformanceBridge")){if(name.equals("begin"))invokes[0]++;if(name.equals("end"))invokes[1]++;}}};}},ClassReader.SKIP_DEBUG);
   ok(invokes[0]>0&&invokes[1]>=invokes[0],"Timing injection incomplete "+owner);classes++;methods+=invokes[0];
  }
  ok(classes==5&&methods==7,"Unexpected timing target count");
  byte[] fixture=PerformanceTransformer.patch("net/minecraft/class_310",fixture());
  Class<?> c=new ClassLoader(PerformanceNativeContract.class.getClassLoader()){Class<?> define(){return defineClass("net.minecraft.class_310",fixture,0,fixture.length);}}.define();
  Object object=c.getConstructor().newInstance();PerformanceBridge.setEnabled(true);c.getMethod("method_1574").invoke(object);ok(count(1)==1,"Native fixture tick was not timed");
  try{c.getMethod("method_1523",boolean.class).invoke(object,true);throw new AssertionError("Original exception swallowed");}catch(java.lang.reflect.InvocationTargetException e){ok(e.getCause() instanceof IllegalStateException&&e.getCause().getMessage().equals("original exception"),"Exception changed");}
  ok(count(0)==0,"Exceptional frame counted as completed");c.getMethod("method_1523",boolean.class).invoke(object,false);ok(count(0)==1,"Next normal frame corrupted");
  PerformanceBridge.setEnabled(false);c.getMethod("method_1574").invoke(object);ok(count(1)==1,"Disabled transformed fixture still recording");
  Path root=Files.createTempDirectory("perf-disabled-");ok(!PerformanceService.install(null,root),"Missing opt-in settings unexpectedly enabled profiling");ok(Files.list(root).findAny().isEmpty(),"Disabled module wrote files");
  System.out.println("PERFORMANCE_NATIVE_BYTECODE_PASS checks="+checks+" classes="+classes+" methods="+methods+" fixture_vm_verified=true no_game=true");
 }
}
