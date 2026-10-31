package sao.zh.engine;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;

/**
 * Actual Minecraft 1.21.4 Text + Style, with SAO server U+E001 world label marker.
 * Uses the real intermediary Text implementation without starting a game or GL.
 */
public final class RealShardWorldTextContract {
 private static int checks;
 private static void ok(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 private static void eq(Object got,Object want,String why){
  ok(Objects.equals(got,want),why+" expected="+String.valueOf(want)+" actual="+String.valueOf(got));
 }
 private static Script.Hook displayHook(){
  Map<String,Object> m=new HashMap<>();
  m.put("id","worlds-locations:text-display-before-native-wrap");
  m.put("class","net/minecraft/class_8138$class_8141");
  m.put("method","method_49057");
  m.put("descriptor","(Lnet/minecraft/class_2561;I)Lnet/minecraft/class_8113$class_8123$class_8125;");
  m.put("kind","argument");m.put("argument",0);m.put("section","lines");
  return new Script.Hook(m);
 }
 private static Script.Hook otherHook(){
  Map<String,Object> m=new HashMap<>();
  m.put("id","dynamic-translation:game-actionbar-display");
  m.put("class","net/minecraft/class_329");
  m.put("method","method_1758");
  m.put("descriptor","(Lnet/minecraft/class_2561;Z)V");
  m.put("kind","argument");m.put("argument",0);m.put("section","messages");
  return new Script.Hook(m);
 }
 private static final Map<String,Map<String,String>> DICTIONARY=Map.of(
  "names", Map.of("Shard Keeper","碎片管理員","Gardien des Éclats","碎片管理員"),
  "lines", Map.of("Right-click: your shards of the day","右鍵領取每日碎片。",
          "Right-click: your daily shards","右鍵領取每日碎片。",
          "Clic droit : vos éclats du jour","右鍵領取每日碎片。"),
  "items",Map.of(),"messages",Map.of());
 private static String translate(String source){
  return RenderTextPipeline.resolve(displayHook(),source,DICTIONARY);
 }
 private static Object original(Class<?> text,Class<?> mutable,Class<?> style,Method literal,
                                Method append,Method set,Method withColor,Method bold,Object empty,
                                String name,String description)throws Exception{
  Object output=literal.invoke(null,"");
  int i=0;
  for(int cp:name.codePoints().toArray()){
   Object piece=literal.invoke(null,new String(Character.toChars(cp)));
   Object a=withColor.invoke(empty,0x4433aa+i++*97);
   set.invoke(piece,a);append.invoke(output,piece);
  }
  Object trailing=literal.invoke(null," ");
  set.invoke(trailing,withColor.invoke(empty,0x0099cc));
  append.invoke(output,trailing);
  Object sep=literal.invoke(null,"\uE001");
  set.invoke(sep,bold.invoke(withColor.invoke(empty,0xabcdef),Boolean.TRUE));
  append.invoke(output,sep);
  Object instruction=literal.invoke(null," "+description);
  set.invoke(instruction,bold.invoke(withColor.invoke(empty,0xffffff),Boolean.TRUE));
  append.invoke(output,instruction);
  return output;
 }
 private static long count(String value,int cp){
  return value.codePoints().filter(x->x==cp).count();
 }
 public static void main(String[]args)throws Exception{
  Script.Hook h=displayHook(),unrelated=otherHook();
  String pua="\uE001";
  String french="Gardien des Éclats "+pua+" Clic droit : vos éclats du jour";
  String english="Shard Keeper "+pua+" Right-click: your shards of the day";
  String expected="碎片管理員 "+pua+" 右鍵領取每日碎片。";
  eq(translate(french),expected,"The exact French source must translate as segments");
  eq(translate(english),expected,"English server-visible text should translate as segments");
  eq(translate("Gardien des Éclats"),"碎片管理員","French title exact shared fallback");
  eq(translate("Shard Keeper"),"碎片管理員","English title exact shared fallback");
  eq(translate("Clic droit : vos éclats du jour"),"右鍵領取每日碎片。","French instruction");
  eq(translate("Right-click: your shards of the day"),"右鍵領取每日碎片。","English prompt");
  eq(count(translate(french),0xe001),1L,"Private font separator remains exactly once");
  eq(translate("Unknown NPC "+pua+" Unknown help"),"Unknown NPC "+pua+" Unknown help","Unknown unchanged");
  eq(translate("YingHuang09 "+pua+" Player note"),"YingHuang09 "+pua+" Player note","Player label untouched");
  eq(translate("Shard Keeper "+pua+" Unknown help"),"碎片管理員 "+pua+" Unknown help","Independent checked segments");
  eq(RenderTextPipeline.resolve(unrelated,french,DICTIONARY),french,"Not a global Actionbar rewrite");
  eq(translate("Shard Keeper "+pua+" Too many "+pua.repeat(8)+" chunks"),
     "Shard Keeper "+pua+" Too many "+pua.repeat(8)+" chunks","Too many markers fail open");
  eq(translate("Shard Keeper "+pua+"YingHuang09: hi"),"碎片管理員 "+pua+"YingHuang09: hi",
     "Known public display label only; private-looking body is not translated");

  Class<?> text=Class.forName("net.minecraft.class_2561");
  Class<?> mutable=Class.forName("net.minecraft.class_5250");
  Class<?> style=Class.forName("net.minecraft.class_2583");
  Class<?> readable=Class.forName("net.minecraft.class_5348");
  Method literal=text.getMethod("method_43470",String.class);
  Method append=mutable.getMethod("method_10852",text);
  Method set=mutable.getMethod("method_10862",style);
  Method withColor=style.getMethod("method_36139",int.class);
  Method bold=style.getMethod("method_10982",Boolean.class);
  Method getString=readable.getMethod("getString");
  Object empty=style.getField("field_24360").get(null);
  Function<String,String> translation=RealShardWorldTextContract::translate;
  for(String[] row:new String[][]{
    {"Shard Keeper","Right-click: your shards of the day"},
    {"Gardien des Éclats","Clic droit : vos éclats du jour"}
  }){
   Object source=original(text,mutable,style,literal,append,set,withColor,bold,empty,row[0],row[1]);
   eq(getString.invoke(source),row[0]+" "+pua+" "+row[1],"Original combined Text assembled");
   Object projected=StyledNameProjection.apply(source,RealShardWorldTextContract.class,translation);
   ok(projected!=source,"Gradient PUA world label should be projected");
   eq(getString.invoke(projected),expected,"Gradient source names + description with immutable marker");
   eq(getString.invoke(source),row[0]+" "+pua+" "+row[1],"Original world entity Text not mutated");
   eq(count((String)getString.invoke(projected),0xe001),1L,"Exact PUA count after styled projection");
  }
  System.out.println("REAL_WORLD_SHARD_PUA_STYLED_TEXT_PASS checks="+checks+
   " french=true english=true two_segments=true icon_preserved=true no_game=true");
 }
}
