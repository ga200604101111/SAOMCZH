package sao.zh.engine;
import java.util.*;import java.util.zip.*;import java.nio.file.*;import org.objectweb.asm.*;
public final class NativeHudSourceContract {
 static int checks;
 static void ok(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);}
 static byte[] read(Path jar,String owner)throws Exception{try(ZipFile z=new ZipFile(jar.toFile())){var e=z.getEntry(owner+".class");ok(e!=null,"Missing native target "+owner);return z.getInputStream(e).readAllBytes();}}
 static List<String> events(byte[] b,String name){List<String>out=new ArrayList<>();new ClassReader(b).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int f,String n,String d,String s,String[]ex){if(!name.equals(n))return null;return new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String owner,String n,String d,boolean i){out.add(owner+"."+n);}public void visitFieldInsn(int op,String owner,String n,String d){if(op==Opcodes.PUTSTATIC||op==Opcodes.PUTFIELD)out.add("STORE:"+n);}};}},ClassReader.SKIP_DEBUG);return out;}
 static void before(List<String>events,String first,String later,String label){ok(events.contains(first)&&events.indexOf(first)<events.indexOf(later),label+" "+events);}
 public static void main(String[]args)throws Exception{
  Script rules=Script.parse(Files.readAllBytes(Path.of(args[0])));RuleTransformer transformer=new RuleTransformer();String hud="fr/sao/questtracker/hud/QuestCompassRenderer";
  byte[] changed=transformer.patch(hud,read(Path.of(args[1]),hud),rules.byClass.get(hud));
  List<String> split=events(changed,"splitInfo"),city=events(changed,"drawTitle");
  ok(Collections.frequency(split,"sao/zh/api/Dispatch.text")==2,"Both flattened objective and detail must be translated");
  before(split,hud+".flat","sao/zh/api/Dispatch.text","Flatten first");
  before(split,"sao/zh/api/Dispatch.text","STORE:infoName","Translate BEFORE derived string cache");
  before(split,"sao/zh/api/Dispatch.text","STORE:infoNameText","Translate BEFORE native Text used for measure and draw");
  before(city,"fr/sao/questtracker/hud/QuestCompassRenderer$FlatText.of","sao/zh/api/Dispatch.text","Region projection after cache read");
  before(city,"sao/zh/api/Dispatch.text",hud+".cityFold","Translated region before native title state");
  before(city,"sao/zh/api/Dispatch.text","fr/sao/font/TextUtil.drawTextSized","Translated region before draw");
  String vanilla="net/minecraft/class_329";byte[] vh=transformer.patch(vanilla,read(Path.of(args[2]),vanilla),rules.byClass.get(vanilla));
  before(events(vh,"method_34004"),"sao/zh/api/Dispatch.value","STORE:field_2016","Title only projection");
  before(events(vh,"method_34002"),"sao/zh/api/Dispatch.value","STORE:field_2039","Subtitle only projection");
  for(String hook:List.of("worlds-locations:hud-location-title","worlds-locations:hud-location-subtitle")){var h=rules.byId.get(hook);ok(h.capture.isEmpty()&&h.section.equals("names"),"No global message lookup or capture");}
  System.out.println("NATIVE_HUD_SOURCES_PASS checks="+checks+" before_measure=true actual_sao_bytecode=true actual_vanilla_bytecode=true");
 }
}
