package sao.zh.engine;
import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;import java.io.*;
import org.objectweb.asm.*;import net.minecraft.*;import sao.zh.api.Dispatch;
/** Actual target-bytecode contract + executable rendering fixtures, never a game launch. */
public final class NativeSemanticContract {
 static int checks;
 static void ok(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static class Loader extends ClassLoader{Loader(){super(NativeSemanticContract.class.getClassLoader());}Class<?> define(String name,byte[]b){return defineClass(name.replace('/','.'),b,0,b.length);}}
 static byte[] read(Path jar,String name)throws Exception{try(ZipFile z=new ZipFile(jar.toFile());InputStream in=z.getInputStream(z.getEntry(name+".class"))){return in.readAllBytes();}}
 static ClassWriter base(String owner){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,owner,null,"java/lang/Object",null);MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();return w;}
 static byte[] entry(){ClassWriter w=base("fr/sao/saoui/mob/MobBarEntry");w.visitField(Opcodes.ACC_PUBLIC,"name","Ljava/lang/String;",null,null).visitEnd();w.visitField(Opcodes.ACC_PUBLIC,"player","Z",null,null).visitEnd();w.visitEnd();return w.toByteArray();}
 static byte[] renderer(){String e="fr/sao/saoui/mob/MobBarEntry";ClassWriter w=base("fr/sao/saoui/mob/MobBarRenderer");MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"shownName","(L"+e+";)Ljava/lang/String;",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitFieldInsn(Opcodes.GETFIELD,e,"name","Ljava/lang/String;");m.visitInsn(Opcodes.ARETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();}
 static byte[] chat(){String c="net/minecraft/class_338";ClassWriter w=base(c);w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"visible",Script.TEXT,null,null).visitEnd();MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"method_1812","("+Script.TEXT+")V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTSTATIC,c,"visible",Script.TEXT);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();}
 static byte[] message(){String o="net/minecraft/class_7594";ClassWriter w=base(o);w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"original",Script.TEXT,null,null).visitEnd();MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"method_44736","("+Script.TEXT+"Z)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTSTATIC,o,"original",Script.TEXT);m.visitTypeInsn(Opcodes.NEW,"net/minecraft/class_338");m.visitInsn(Opcodes.DUP);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"net/minecraft/class_338","<init>","()V",false);m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"net/minecraft/class_338","method_1812","("+Script.TEXT+")V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();}
 static void set(Object target,String field,Object value)throws Exception{Field f=target.getClass().getDeclaredField(field);f.setAccessible(true);f.set(target,value);}
 static String text(Object o){return ((class_5250)o).getString();}
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
  Script rules=Script.parse(Files.readAllBytes(Path.of(args[0])));Path mc=Path.of(args[1]),sao=Path.of(args[2]);RuleTransformer transformer=new RuleTransformer();
  Set<String>targets=Set.of("fr/sao/saoui/mob/MobBarRenderer","fr/sao/tooltip/TipModel","net/minecraft/class_7594","net/minecraft/class_897","fr/sao/questtracker/hud/QuestCompassRenderer");
  for(String owner:targets){byte[]raw=read(owner.startsWith("net/")?mc:sao,owner);byte[]next=transformer.patch(owner,raw,rules.byClass.get(owner));ok(next.length>raw.length,"Native target unpatched "+owner);new ClassReader(next);}
  final boolean[]playerField={false};new ClassReader(read(sao,"fr/sao/saoui/mob/MobBarEntry")).accept(new ClassVisitor(Opcodes.ASM9){public FieldVisitor visitField(int a,String n,String d,String sig,Object v){if(n.equals("player")&&d.equals("Z"))playerField[0]=true;return null;}},ClassReader.SKIP_CODE);
  ok(playerField[0],"Native player discriminator absent");
  Script.Hook mob=rules.byId.get("codex-creatures:mob-shown-name"),sys=rules.byId.get("dynamic-translation:game-system-display"),npc=rules.byId.get("worlds-locations:npc-tagged-label");
  List<String>calls=new ArrayList<>();byte[]messageBytes=transformer.patch("net/minecraft/class_7594",read(mc,"net/minecraft/class_7594"),List.of(sys));
  new ClassReader(messageBytes).accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int a,String n,String d,String s,String[]ex){if(!n.equals("method_44736"))return null;return new MethodVisitor(Opcodes.ASM9){public void visitMethodInsn(int op,String o,String n,String d,boolean itf){calls.add(o+"."+n);}};}},0);
  int show=calls.indexOf("net/minecraft/class_338.method_1812");ok(show>0&&calls.get(show-1).equals("sao/zh/api/Dispatch.value"),"System projection not directly before display call");
  ok(Collections.frequency(calls,"sao/zh/api/Dispatch.value")==1,"Game/system projection touched multiple consumers");
  Constructor<Engine> ctor=Engine.class.getDeclaredConstructor(java.lang.instrument.Instrumentation.class,Path.class,Path.class,boolean.class);ctor.setAccessible(true);Engine engine=ctor.newInstance(null,Files.createTempDirectory("sao-semantic-"),null,true);
  Map<String,Map<String,String>>data=(Map)MiniJson.parse(Files.readString(Path.of(args[3])));set(engine,"current",rules);set(engine,"dictionary",data);
  Method bind=Engine.class.getDeclaredMethod("bind");bind.setAccessible(true);bind.invoke(engine);
  Loader loader=new Loader();Class<?> entry=loader.define("fr/sao/saoui/mob/MobBarEntry",entry());Class<?> renderer=loader.define("fr/sao/saoui/mob/MobBarRenderer",transformer.patch("fr/sao/saoui/mob/MobBarRenderer",renderer(),List.of(mob)));
  Object entity=entry.getConstructor().newInstance();entry.getField("name").set(entity,"§fMini Treant §eLv ° 3");Method name=renderer.getMethod("shownName",entry);
  ok(name.invoke(null,entity).equals("§f迷你樹人 §eLv ° 3"),"Mob name/level path not translated");
  ok(Dispatch.mobText(mob.id,"已被上游改名","PlayerOriginal",renderer,true).equals("PlayerOriginal"),"Player source not restored after legacy name cache");
  entry.getField("player").setBoolean(entity,true);ok(name.invoke(null,entity).equals("§fMini Treant §eLv ° 3"),"Player was translated by mob path");
  entry.getField("player").setBoolean(entity,false);fr.sao.util.LanguageWatch.value="en_us";ok(name.invoke(null,entity).equals("§fMini Treant §eLv ° 3"),"Non-Chinese language altered");fr.sao.util.LanguageWatch.value="zh_tw";
  Class<?> chat=loader.define("net/minecraft/class_338",chat());Class<?> message=loader.define("net/minecraft/class_7594",transformer.patch("net/minecraft/class_7594",message(),List.of(sys)));Object handler=message.getConstructor().newInstance();Method receive=message.getMethod("method_44736",class_2561.class,boolean.class);
  class_2583 red=new class_2583(0xff0000),gold=new class_2583(0xffaa00),white=new class_2583(0xffffff);
  class_5250 original=class_2561.method_43470("Calme Mortel déclenché !").method_10862(red);receive.invoke(handler,original,false);
  Object visual=chat.getField("visible").get(null);ok(text(visual).equals("致命沉靜已觸發！"),"System notification untranslated");ok(((class_2561)visual).method_10866()==red,"System color lost");ok(message.getField("original").get(null)==original&&text(original).equals("Calme Mortel déclenché !"),"Original packet/log representation changed");
  class_5250 player=class_2561.method_43470("[Floor 1] User » ");player.method_10852(class_2561.method_43470("Calme Mortel déclenché !").method_10862(red));receive.invoke(handler,player,false);ok(chat.getField("visible").get(null)==player,"Embedded player phrase translated");
  class_5250 npcLine=class_2561.method_43470("");npcLine.method_10852(class_2561.method_43470("[NPC] ").method_10862(gold)).method_10852(class_2561.method_43470("Local Lumberjack").method_10862(white));
  Object npcResult=Dispatch.value(npc.id,npcLine,renderer);ok(text(npcResult).equals("[NPC] 當地伐木工"),"Split styled NPC name untranslated");ok(((class_2561)npcResult).method_10855().get(0).method_10866()==gold,"NPC marker style lost");ok(((class_2561)npcResult).method_10855().get(1).method_10866()==white,"NPC name style lost");
  class_5250 noTag=class_2561.method_43470("Local Lumberjack");ok(Dispatch.value(npc.id,noTag,renderer)==noTag,"Untagged native label changed");
  class_5250 split=class_2561.method_43470("");split.method_10852(class_2561.method_43470("Se rendre là où ").method_10862(white)).method_10852(class_2561.method_43470("résident les brigands | 2773m").method_10862(white));
  Object quest=Dispatch.value("quests-objectives:compass-top-line",split,renderer);
  if(rules.byId.containsKey("quests-objectives:compass-flat-info")){
   ok(quest==split,"Before native flat(), original Text must remain unchanged apart from readable aliases");
   ok(Dispatch.text("quests-objectives:compass-flat-info",text(quest),renderer).equals("前往盜匪的居所 | 2773m"),"Flattened HUD source must translate before caches/measurement");
   ok(((class_2561)quest).method_10855().get(0).method_10866()==white,"Original styles changed before native flatten");
  }else{
   ok(text(quest).equals("前往盜匪的居所 | 2773m"),"Uniform children under empty root failed assembly");ok(((class_2561)quest).method_10866()==white,"Assembled effective style lost");
  }
  ok(Dispatch.text("items-economy:tooltip-name","§aTraining Pavise§r",renderer).equals("§a訓練用大盾§r"),"Custom tooltip title not fixed");
  Field collector=Engine.class.getDeclaredField("unknownCollector");collector.setAccessible(true);((UnknownCollector)collector.get(engine)).close();
  ok(Dispatch.faults()==0,"Dispatch silently failed");
  System.out.println("NATIVE_SEMANTIC_RENDER_CONTRACT_PASS checks="+checks+" actual_classes="+targets.size()+" players_unchanged=true original_messages_preserved=true no_game=true");
 }
}
