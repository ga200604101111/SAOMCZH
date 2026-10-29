package sao.zh.engine;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;
import java.io.*;
import org.objectweb.asm.*;
import net.minecraft.*;
/** Static target-bytecode and direct semantic/layout contract; no live Minecraft/GL. */
public final class WorldTextNativeProbe {
 static int checks;
 static void ok(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 static Script.Hook h(String id,String owner,String method,String desc,String kind,int argument,String section,boolean refresh) {
  Map<String,Object> m=new LinkedHashMap<>();
  m.put("id",id);m.put("class",owner);m.put("method",method);
  m.put("descriptor",desc);m.put("kind",kind);m.put("section",section);
  if(kind.equals("argument"))m.put("argument",argument);
  if(refresh)m.put("cache_fields",List.of(Map.of("name","field_42442","descriptor","Lnet/minecraft/class_8113$class_8123$class_8125;","action","reset_display_lines")));
  return new Script.Hook(m);
 }
 static byte[] source(Path jar,String className)throws Exception{
  try(ZipFile z=new ZipFile(jar.toFile())){
   ZipEntry e=z.getEntry(className+".class");ok(e!=null,"Missing actual native class "+className);
   try(InputStream in=z.getInputStream(e)){return in.readAllBytes();}
  }
 }
 static List<String> verify(String cls,byte[] bytes,String target){
  List<String>calls=new ArrayList<>();
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9){
   public MethodVisitor visitMethod(int a,String method,String desc,String s,String[]ex){
    if(!target.equals(method))return null;
    return new MethodVisitor(Opcodes.ASM9){
     public void visitMethodInsn(int op,String owner,String name,String desc,boolean iface){
      calls.add(owner+"."+name);
     }
    };
   }
  },ClassReader.SKIP_DEBUG);
  ok(!calls.isEmpty(),"No invocation in "+cls+"."+target);
  return calls;
 }
 static void before(List<String>inv,String first,String later,String label){
  int a=inv.indexOf(first),b=inv.indexOf(later);
  ok(a>=0&&b>a,label+" call order "+inv);
 }
 public static void main(String[]args)throws Exception{
  Path mc=Path.of(args[0]),sao=Path.of(args[1]);
  String cacheClass="net/minecraft/class_8113$class_8123";
  String displayClass="net/minecraft/class_8138$class_8141";
  String holoClass="com/eighteen/saojob/hologram/HologramRenderer";
  Script.Hook invalidate=h("worlds-locations:text-display-cache-refresh",cacheClass,"method_48905",
   "(Lnet/minecraft/class_8113$class_8123$class_8127;)Lnet/minecraft/class_8113$class_8123$class_8125;","refresh",-1,"lines",true);
  Script.Hook display=h("worlds-locations:text-display-before-native-wrap",displayClass,"method_49057",
   "(Lnet/minecraft/class_2561;I)Lnet/minecraft/class_8113$class_8123$class_8125;","argument",0,"lines",false);
  Script.Hook before=h("worlds-locations:saojob-hologram-preformat",holoClass,"parseLegacySectionFormatting",
   "(Ljava/lang/String;)Lnet/minecraft/class_2561;","argument",0,"lines",false);
  Script.Hook after=h("worlds-locations:saojob-hologram-styled-text",holoClass,"parseLegacySectionFormatting",
   "(Ljava/lang/String;)Lnet/minecraft/class_2561;","return",-1,"lines",false);
  // Exercise the actual per-entity refresh state machine with a mocked TextDisplay.
  Class<?> displayEntity=Class.forName("net.minecraft.class_8113$class_8123");
  Class<?> cachedLines=Class.forName("net.minecraft.class_8113$class_8123$class_8125");
  Object entity=displayEntity.getConstructor().newInstance();
  java.lang.reflect.Field cached=displayEntity.getDeclaredField("field_42442");
  cached.setAccessible(true);
  LayoutSupport layout=new LayoutSupport();
  Object first=cachedLines.getConstructor().newInstance();cached.set(entity,first);
  layout.refresh(invalidate,entity,displayEntity,31L);
  ok(cached.get(entity)==null,"Initial version kept stale lines");
  Object second=cachedLines.getConstructor().newInstance();cached.set(entity,second);
  layout.refresh(invalidate,entity,displayEntity,31L);
  ok(cached.get(entity)==second,"Stable version invalidated cache every frame");
  layout.refresh(invalidate,entity,displayEntity,32L);
  ok(cached.get(entity)==null,"Changed dictionary epoch did not rewrap cached TextDisplay");
  String armorClass="net/minecraft/class_877";
  Script.Hook armor=h("worlds-locations:armorstand-nameplate",armorClass,"method_62375",
   "(Lnet/minecraft/class_1531;Lnet/minecraft/class_9998;F)V","armorstand_nameplate",-1,"lines",false);
  try{
   h("bad:generic","net/minecraft/class_897","method_62375",
    "(Lnet/minecraft/class_1531;Lnet/minecraft/class_9998;F)V","armorstand_nameplate",-1,"lines",false);
   throw new AssertionError("Generic player/entity renderer accepted as ArmorStand-only projection");
  }catch(IllegalArgumentException expected){checks++;}
  RuleTransformer t=new RuleTransformer();
  byte[] armorPatched=t.patch(armorClass,source(mc,armorClass),List.of(armor));
  before(verify(armorClass,armorPatched,"method_62375"),
         "net/minecraft/class_922.method_62355","sao/zh/api/Dispatch.value",
         "ArmorStand nameplate must be projected only after superclass state extraction");
  int[] nametagFields={0,0};
  new ClassReader(armorPatched).accept(new ClassVisitor(Opcodes.ASM9){
   public MethodVisitor visitMethod(int a,String n,String desc,String sig,String[]ex){
    if(!n.equals("method_62375"))return null;
    return new MethodVisitor(Opcodes.ASM9){
     public void visitFieldInsn(int op,String owner,String name,String desc){
      if(owner.equals("net/minecraft/class_10017")&&name.equals("field_53337")){
       if(op==Opcodes.GETFIELD)nametagFields[0]++;
       if(op==Opcodes.PUTFIELD)nametagFields[1]++;
      }
     }
    };
   }
  },ClassReader.SKIP_DEBUG);
  ok(nametagFields[0]==1&&nametagFields[1]==1,"ArmorStand nameplate field mutation shape mismatch");
  byte[] cache=t.patch(cacheClass,source(mc,cacheClass),List.of(invalidate));
  before(verify(cacheClass,cache,"method_48905"),"sao/zh/api/Dispatch.refresh",
         "net/minecraft/class_8113$class_8123$class_8230.comp_1334","cache refresh before split");
  byte[] dr=t.patch(displayClass,source(mc,displayClass),List.of(display));
  before(verify(displayClass,dr,"method_49057"),"sao/zh/api/Dispatch.value",
         "net/minecraft/class_327.method_1728","translate before native line wrapping");
  byte[] hr=t.patch(holoClass,source(sao,holoClass),List.of(before,after));
  List<String>calls=verify(holoClass,hr,"parseLegacySectionFormatting");
  ok(calls.contains("sao/zh/api/Dispatch.text"),"SAOJob parser did not localize source String");
  ok(calls.contains("sao/zh/api/Dispatch.value"),"SAOJob parser did not localize styled Text");
  try{
   h("unsafe",cacheClass,"method_48905","()Ljava/lang/Object;","refresh",-1,"lines",true);
   throw new AssertionError("Unsafe cache hook accepted");
  }catch(IllegalArgumentException expected){checks++;}
  Map<String,Map<String,String>>dictionary=Map.of("lines",Map.of(
    "Jump and Run (Easy)","跳躍跑酷（簡單）",
    "(Respect if you finish it)","（成功通關者值得敬佩）",
    "Jump and Run (Easy)\n(Respect if you finish it)","跳躍跑酷（簡單）\n（成功通關者值得敬佩）"),
    "names",Map.of("Jump and Run (Easy)","BAD_NAMES_SCOPE"));
  ok(RenderTextPipeline.resolve(display,"Jump and Run (Easy)",dictionary).equals("跳躍跑酷（簡單）"),"TextDisplay scope");
  ok(RenderTextPipeline.resolve(before,"(Respect if you finish it)",dictionary).equals("（成功通關者值得敬佩）"),"SAOJob semantic source");
  ok(RenderTextPipeline.resolve(display,"Other_123",dictionary).equals("Other_123"),"Unknown player-like string touched");
  ok(RenderTextPipeline.resolve(armor,"Jump and Run (Easy)",dictionary).equals("跳躍跑酷（簡單）"),"ArmorStand semantic name");
  ok(RenderTextPipeline.wholeFirst(armor),"ArmorStand whole-source translation disabled");
  ok(TextProjection.apply(null,WorldTextNativeProbe.class,v->v,true)==null,"Null entity label was modified");
  ok(RenderTextPipeline.wholeFirst(display),"TextDisplay whole source disabled");
  ok(RenderTextPipeline.wholeFirst(after),"SAOJob safe assembled text disabled");
  Script.Hook generic=h("ui-text-layout:util-draw-string","fr/sao/font/TextUtil","drawString","(Ljava/lang/String;)Ljava/lang/String;","argument",0,"messages",false);
  ok(!RenderTextPipeline.wholeFirst(generic),"Generic renderer may not merge arbitrary text");
  Script.Hook combat=h("combat-feedback:damage-text","fr/sao/saoui/hud/actionbar/ActionBarTranslator",
   "translate","(Lnet/minecraft/class_2561;)Lnet/minecraft/class_2561;","return",-1,"messages",false);
  ok(RenderTextPipeline.resolve(combat,"You dodged 11.25 damage!",Map.of()).equals("你閃避了 11.25 點傷害！"),"Dodge number");
  ok(RenderTextPipeline.resolve(combat,"🛡 Bouclier de Mana [25/26]",Map.of()).equals("🛡 魔力護盾 [25/26]"),"Shield numbers");
  ok(RenderTextPipeline.resolve(combat,"❤ Soin +548 | total soigne 548",Map.of()).equals("❤ 治療 +548 | 總治療量 548"),"Heal numbers");
  ok(RenderTextPipeline.resolve(generic,"You dodged 11.25 damage!",Map.of()).equals("You dodged 11.25 damage!"),"Combat syntax leaked outside domain");
  class_5250 root=class_2561.method_43470("");
  class_5250 x=class_2561.method_43470("Jump and ");
  class_5250 y=class_2561.method_43470("Run (Easy)");
  class_2583 red=new class_2583(0xff0000);root.method_10862(red);x.method_10862(red);y.method_10862(red);
  root.method_10852(x).method_10852(y);
  Object translated=TextProjection.apply(root,WorldTextNativeProbe.class,s->dictionary.get("lines").getOrDefault(s,s),true);
  ok(translated!=root,"Uniform styled lines were not assembled");
  ok(((class_5250)translated).getString().equals("跳躍跑酷（簡單）"),"Whole text not translated");
  ok(((class_5250)translated).method_10866()==red,"Red style lost on assembled line");
  ok(root.getString().equals("Jump and Run (Easy)")&&root.method_10855().size()==2,"Source text tree was changed");
  class_5250 mixed=class_2561.method_43470("");
  class_5250 z1=class_2561.method_43470("Jump and ");
  class_5250 z2=class_2561.method_43470("Run (Easy)");
  z1.method_10862(new class_2583(0x111111));z2.method_10862(new class_2583(0xeeeeee));
  mixed.method_10852(z1).method_10852(z2);
  Object untouched=TextProjection.apply(mixed,WorldTextNativeProbe.class,s->dictionary.get("lines").getOrDefault(s,s),true);
  ok(untouched==mixed,"Mixed colors were improperly flattened");
  class_5250 glyph=class_2561.method_43470("");glyph.method_10852(class_2561.method_43470("Jump and ")).method_10852(class_2561.method_43470("\uea72Run (Easy)"));
  Object noGlyph=TextProjection.apply(glyph,WorldTextNativeProbe.class,s->dictionary.get("lines").getOrDefault(s,s),true);
  ok(noGlyph==glyph,"Private-use font icon unexpectedly flattened");
  System.out.println("WORLD_TEXT_NATIVE_BYTECODE_AND_RENDER_PIPELINE_PASS checks="+checks+
    " classes=4 original=2 scripts=5 display_reflow=true style_safe=true no_game=true");
 }
}
