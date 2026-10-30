package sao.zh.engine;
import java.util.*;
import net.minecraft.*;
/** Two-tone system messages: style leaf boundaries must survive localization. */
public final class StyledSystemTextContract {
 private static int checks;
 private static final Map<String,Map<String,String>> DICT=Map.of(
  "messages",Map.of("Hey! Sorry, but you can't change that here.","嘿！抱歉，你不能在這裡更改。"),
  "items",Map.of("Coal Ore","煤礦石","Lingot de Fer Chaud","灼熱鐵錠"),
  "names",Map.of(),"lines",Map.of());
 private static final Script.Hook HOOK=new Script.Hook(Map.of("id","dynamic-translation:game-system-display","class","net/minecraft/class_7594","method","display","descriptor","(Ljava/lang/String;)Ljava/lang/String;","kind","return","section","messages"));
 private static class_5250 child(String s,int rgb){return class_2561.method_43470(s).method_10862(new class_2583(rgb));}
 private static class_5250 root(class_5250...children){
  class_5250 root=class_2561.method_43470("");
  for(class_5250 c:children)root.method_10852(c);
  return root;
 }
 private static void equal(String label,Object a,Object b){
  checks++;if(!Objects.equals(a,b))throw new AssertionError(label+" expected="+b+" actual="+a);
 }
 private static void test(String label,class_5250 source,String expect){
  equal(label+" gate",SemanticText.permitTree(HOOK,source.getString(),DICT),true);
  try{
   class_5250 result=(class_5250)TextProjection.apply(source,StyledSystemTextContract.class,x->RenderTextPipeline.resolve(HOOK,x,DICT),true);
   equal(label+" pixels",result.getString(),expect);
   equal(label+" input immutable",source.getString(),source.getString());
   equal(label+" spans",result.method_10855().size(),source.method_10855().size());
   for(int i=0;i<source.method_10855().size();i++)equal(label+" style "+i,result.method_10855().get(i).method_10866(),source.method_10855().get(i).method_10866());
  }catch(Exception problem){throw new AssertionError(label,problem);}
 }
 public static void main(String[]args){
  class_5250 received=root(child("You received  ",0x00bb00),child("Coal Ore",0xbbbbbb));
  String before=received.getString();
  test("received",received,"已獲得  煤礦石");
  equal("received original unchanged",received.getString(),before);
  test("hey",root(child("Hey!",0xff0000),child(" Sorry, but you can't change that here.",0xaaaaaa)),"嘿！ 抱歉，你不能在這裡更改。");
  test("furnace",root(child("Furnace — temperature ",0xbbbbbb),child("100 pts",0x00cc00),child(" | time ",0xbbbbbb),child("100 pts",0x00cc00),child(" | precision ",0xbbbbbb),child("100%",0x00cc00)),
       "熔爐 — 溫度 100 分 | 時間 100 分 | 精準度 100%");
  test("heating",root(child("Heating stopped at ",0xffcc00),child("108°C.",0xffcc00),child(" Right-click with the item to insert it.",0xbbbbbb)),
       "已在 108°C 停止加熱。手持物品右鍵放入。");
  class_5250 player=root(child("Player_One: ",0xff00ff),child("You received  Coal Ore",0xbbbbbb));
  equal("player gate",SemanticText.permitTree(HOOK,player.getString(),DICT),false);
  System.out.println("STYLED_SYSTEM_TEXT_PASS checks="+checks+" colors_preserved=true mixed_styles=true");
 }
}
