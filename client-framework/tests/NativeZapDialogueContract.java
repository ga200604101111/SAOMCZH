package sao.zh.engine;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Strict Java Agent-only display-hook contract against the real Zap Dialogue mod JAR. */
public final class NativeZapDialogueContract {
 private static int checks;
 static void ok(boolean condition,String why){checks++;if(!condition)throw new AssertionError(why);}
 static Script.Hook hook(String id,String cls,String method,String desc,int arg,String section,String capture){
  Map<String,Object> h=new LinkedHashMap<>();
  h.put("id","npc-dialogue:"+id);h.put("class",cls);h.put("method",method);h.put("descriptor",desc);
  h.put("kind","argument");h.put("argument",arg);h.put("section",section);
  if(capture!=null)h.put("capture",capture);
  return new Script.Hook(h);
 }
 static void forbid(String id,String cls,String method,String desc,int arg,String section,String capture){
  try{hook(id,cls,method,desc,arg,section,capture);}
  catch(IllegalArgumentException expected){checks++;return;}
  throw new AssertionError("Unapproved dialogue target accepted: "+cls+"/"+method);
 }
 static byte[] read(ZipFile zip,String cls)throws Exception{
  ZipEntry e=zip.getEntry(cls+".class");ok(e!=null,"Native class missing: "+cls);
  return zip.getInputStream(e).readAllBytes();
 }
 static int injectCount(byte[] changed,String method){
  int[] uses={0};
  new ClassReader(changed).accept(new ClassVisitor(Opcodes.ASM9){
   @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]exceptions){
    if(!n.equals(method))return null;
    return new MethodVisitor(Opcodes.ASM9){
     @Override public void visitMethodInsn(int op,String owner,String n,String d,boolean itf){
      if(owner.equals("sao/zh/api/Dispatch")&&n.equals("value"))uses[0]++;
     }
    };
   }
  },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
  return uses[0];
 }
 public static void main(String[]args)throws Exception{
  final String styled="fr/zapdialogue/client/text/StyledText",bubble="fr/zapdialogue/client/gui/BubbleRenderer";
  Script.Hook speaker=hook("speaker",bubble,"drawSpeakerName",
   "(Lnet/minecraft/class_332;Lfr/zapdialogue/client/gui/BubbleRenderer$Bubble;Lnet/minecraft/class_2561;Lnet/minecraft/class_327;)V",2,"names",null);
  Script.Hook wrap=hook("text-wrap",styled,"wrap",
   "(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;",0,"lines","dialogue");
  Script.Hook trim=hook("text-trim",styled,"trim",
   "(Lnet/minecraft/class_5348;I)Lnet/minecraft/class_5250;",0,"lines",null);
  Script.Hook length=hook("text-length",styled,"length",
   "(Lnet/minecraft/class_5348;)I",0,"lines",null);
  forbid("fake","fr/zapdialogue/client/network/ZapClientPackets","sendOptionSelect","(I)V",0,"lines","dialogue");
  forbid("fake",styled,"parse","(Ljava/lang/String;)Lnet/minecraft/class_2561;",0,"lines","dialogue");
  forbid("fake",styled,"wrap","(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;",1,"lines","dialogue");
  forbid("fake",styled,"wrap","(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;",0,"messages","dialogue");
  forbid("fake",bubble,"drawSpeakerName",speaker.descriptor,2,"names","dialogue");
  ok(UnknownCollector.permitted(styled,"argument","dialogue"),"Dialogue capture should be allowlisted at render source");
  ok(!UnknownCollector.permitted("fr/zapdialogue/client/network/ZapClientPackets","argument","dialogue"),"Never capture packet/network");
  ok(!UnknownCollector.safe("Player123: hello","dialogue"),"Private player chat leaked");
  ok(!UnknownCollector.safe("secret=12345678","dialogue"),"Private key-like data leaked");
  ok(UnknownCollector.safe("The pens are holding and my animals sleep. That is thanks to you.","dialogue"),"NPC sentence not collectable");
  try(ZipFile mod=new ZipFile(Path.of(args[0]).toFile())){
   byte[] styledOriginal=read(mod,styled),bubbleOriginal=read(mod,bubble);
   byte[] styledPatched=new RuleTransformer().patch(styled,styledOriginal,List.of(wrap,trim,length));
   byte[] bubblePatched=new RuleTransformer().patch(bubble,bubbleOriginal,List.of(speaker));
   for(String method:List.of("wrap","trim","length"))ok(injectCount(styledPatched,method)==1,"One dispatch required before "+method);
   ok(injectCount(bubblePatched,"drawSpeakerName")==1,"Speaker localized only in render method");
   byte[] options=read(mod,"fr/zapdialogue/client/gui/OptionScreen");
   int[] selection={0},optionWrap={0},dialogueTrim={0};
   new ClassReader(options).accept(new ClassVisitor(Opcodes.ASM9){
    @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){
     if(!Set.of("selectOption","layoutOptions","method_25394").contains(n))return null;
     return new MethodVisitor(Opcodes.ASM9){
      @Override public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
       if(n.equals("selectOption")&&owner.endsWith("/ZapClientPackets")&&name.equals("sendOptionSelect")&&desc.equals("(I)V"))selection[0]++;
       if(n.equals("layoutOptions")&&owner.equals(styled)&&name.equals("wrap"))optionWrap[0]++;
       if(n.equals("method_25394")&&owner.equals(styled)&&name.equals("trim"))dialogueTrim[0]++;
      }
     };
    }
   },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   ok(selection[0]==1,"Original choice index must be sent unchanged");
   ok(optionWrap[0]==1,"Options must use localized wrap before measurement");
   ok(dialogueTrim[0]==1,"Options screen text must use localized trim");
   byte[] content=read(mod,"fr/zapdialogue/client/gui/BubbleContent");
   int[] contentWrap={0};
   new ClassReader(content).accept(new ClassVisitor(Opcodes.ASM9){
    @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[]e){
     if(!n.equals("layout"))return null;
     return new MethodVisitor(Opcodes.ASM9){
      @Override public void visitMethodInsn(int op,String o,String name,String desc,boolean itf){
       if(o.equals(bubble)&&name.equals("wrapText"))contentWrap[0]++;
      }
     };
    }
   },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
   ok(contentWrap[0]==1,"Dialogue pages must use the same wrap path");
  }
  Map<String,Map<String,String>> glossary=Map.of(
   "names",Map.of("Shepherd Ivo","牧羊人伊沃"),
   "lines",Map.of("The pens are holding and my animals sleep. That is thanks to you.","圍欄守住了，我的牲畜也睡得安穩。這都多虧了你。"),
   "items",Map.of(),"messages",Map.of());
  ok(RenderTextPipeline.resolve(speaker,"Shepherd Ivo",glossary).equals("牧羊人伊沃"),"NPC speaker translation");
  ok(RenderTextPipeline.resolve(wrap,"The pens are holding and my animals sleep. That is thanks to you.",glossary)
     .equals("圍欄守住了，我的牲畜也睡得安穩。這都多虧了你。"),"Full dialogue translation");
  ok(RenderTextPipeline.resolve(wrap,"Unknown response",glossary).equals("Unknown response"),"Unknown option unchanged");
  ok(RenderTextPipeline.resolve(speaker,"YingHuang09",glossary).equals("YingHuang09"),"Player name unchanged");
  System.out.println("NATIVE_ZAP_DIALOGUE_AGENT_CONTRACT_PASS checks="+checks+" display_only=true option_index_preserved=true");
 }
}
