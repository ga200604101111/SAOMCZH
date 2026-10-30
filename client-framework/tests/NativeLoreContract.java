package sao.zh.engine;
import java.nio.file.*;import java.util.*;import java.util.function.*;import java.util.zip.*;import org.objectweb.asm.*;import sao.zh.api.Dispatch;
/** Transform actual installed SAOUI bytecode, execute a same-signature JVM fixture; no game launch. */
public final class NativeLoreContract {
 static int checks;static void ok(boolean b,String what){checks++;if(!b)throw new AssertionError(what);}
 static byte[] load(Path jar,String name)throws Exception{try(ZipFile z=new ZipFile(jar.toFile())){var entry=z.getEntry(name+".class");ok(entry!=null,"native class missing "+name);try(var in=z.getInputStream(entry)){return in.readAllBytes();}}}
 static byte[] fixture(){
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);String o="fr/sao/tooltip/TipModel";
  w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,o,null,"java/lang/Object",null);
  w.visitField(Opcodes.ACC_PRIVATE|Opcodes.ACC_FINAL,"description","Ljava/util/List;",null,null).visitEnd();
  MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","(Ljava/util/List;)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,o,"description","Ljava/util/List;");m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
  m=w.visitMethod(Opcodes.ACC_PUBLIC,"description","()Ljava/util/List;",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,o,"description","Ljava/util/List;");m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  Script.Hook hook=new Script.Hook(Map.of("id","items-economy:tooltip-description-paragraphs","class","fr/sao/tooltip/TipModel","method","description","descriptor","()Ljava/util/List;","kind","description_list","section","items","capture","item"));
  RuleTransformer t=new RuleTransformer();Path jar=Path.of(args[0]);byte[] nativeBytes=load(jar,hook.owner);
  byte[] changed=t.patch(hook.owner,nativeBytes,List.of(hook));ok(!Arrays.equals(nativeBytes,changed),"actual TipModel unpatched");
  int[] calls={0};new ClassReader(changed).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){if(!n.equals("description"))return null;return new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String o,String n,String d,boolean i){if(o.equals("sao/zh/api/Dispatch"))calls[0]++;}};}},ClassReader.SKIP_DEBUG);ok(calls[0]==1,"description dispatch count");
  List<String> order=new ArrayList<>();new ClassReader(load(jar,"fr/sao/tooltip/ItemTip")).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){return new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String o,String name,String d,boolean i){if(o.equals(hook.owner)&&name.equals("description"))order.add("description");if(o.equals("fr/sao/pass/screen/PassCanvas")&&name.equals("wrap"))order.add("wrap");}};}},ClassReader.SKIP_DEBUG);ok(order.indexOf("description")>=0&&order.indexOf("wrap")>order.indexOf("description"),"native wrap must follow description");
  LoreParagraphs plan=new LoreParagraphs(LoreParagraphContract.DATA);
  Dispatch.bind((key,v,owner)->plan.project(v,UnaryOperator.identity(),UnaryOperator.identity(),(a,b)->b,1L),1L);
  byte[] patched=t.patch(hook.owner,fixture(),List.of(hook));Class<?> fixture=new ClassLoader(NativeLoreContract.class.getClassLoader()){Class<?> define(){return defineClass("fr.sao.tooltip.TipModel",patched,0,patched.length);}}.define();
  List<String> raw=new ArrayList<>(List.of(LoreParagraphContract.EN.split("\n")));Object model=fixture.getConstructor(List.class).newInstance(raw);
  Object out=fixture.getMethod("description").invoke(model);ok(out.equals(List.of(LoreParagraphContract.ZH)),"real dispatch must accept a new List implementation instead of rejecting ListN/List12/ArrayList changes");
  ok(raw.size()==4&&raw.get(0).equals("A material of nature,"),"model mutated");
  var field=fixture.getDeclaredField("description");field.setAccessible(true);ok(field.get(model)==raw,"source list replaced");
  List<String> unknown=List.of("Deliberate first line","Deliberate second line");Object other=fixture.getConstructor(List.class).newInstance(unknown);ok(fixture.getMethod("description").invoke(other)==unknown,"unknown layout identity");
  Dispatch.bind(null,2L);ok(fixture.getMethod("description").invoke(model)==raw,"disabled handler fail open");ok(Dispatch.faults()==0,"dispatch faults");
  System.out.println("NATIVE_LORE_CONTRACT_PASS checks="+checks+" transformed_native=true fixture_execution=true before_wrap=true");
 }
}
