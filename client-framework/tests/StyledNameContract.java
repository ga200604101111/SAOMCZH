package sao.zh.engine;
import net.minecraft.*;
import java.util.*;import java.util.function.Function;import java.lang.reflect.*;
public final class StyledNameContract {
 static int checks;static Method project;
 static void ok(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static Object apply(Object source,Function<String,String> translator)throws Exception{return project.invoke(null,source,StyledNameContract.class,translator);}
 static class_5250 gradient(String text,String meta){class_5250 result=class_2561.method_43470("");int n=0;for(int cp:text.codePoints().toArray())result.method_10852(class_2561.method_43470(new String(Character.toChars(cp))).method_10862(new class_2583(n++*100,meta)));return result;}
 public static void main(String[]args)throws Exception{
  try{Class<?>c=Class.forName("sao.zh.engine.StyledNameProjection");project=c.getDeclaredMethod("apply",Object.class,Class.class,Function.class);project.setAccessible(true);}catch(ReflectiveOperationException missing){throw new AssertionError("Scoped resolved/gradient Text projection is not implemented",missing);}
  Map<String,String> dictionary=Map.of("Shard Keeper","碎片管理員","Hanaka","哈納卡","\uea00 Shard Keeper Lv. 3+","\uea00 碎片管理員 Lv. 3+");Function<String,String> translate=s->dictionary.getOrDefault(s,s);
  class_5250 source=gradient("Shard Keeper","font-same");Object target=apply(source,translate);
  ok(target!=source,"Known gradient label stayed untranslated");ok(((class_2561)target).getString().equals("碎片管理員"),"Wrong translation");
  ok(source.getString().equals("Shard Keeper"),"Source modified");
  List<class_2561> parts=((class_2561)target).method_10855();
  ok(parts.get(0).method_10866().color==0,"First gradient color lost");ok(parts.get(parts.size()-1).method_10866().color==1100,"Last gradient color lost");
  ok(parts.stream().allMatch(p->p.method_10866().metadata.equals("font-same")),"Font/style metadata changed");
  FixtureResolvedText nonliteral=new FixtureResolvedText("Hanaka",new class_2583(0xffee00,"original-font"));
  class_2561 resolved=(class_2561)apply(nonliteral,translate);ok(resolved.getString().equals("哈納卡"),"Resolved translatable content ignored");ok(nonliteral.getString().equals("Hanaka"),"Nonliteral source mutated");
  class_5250 unknown=gradient("Other Title","font-same");ok(apply(unknown,translate)==unknown,"Unknown text rebuilt or translated");
  class_5250 mixed=class_2561.method_43470("");mixed.method_10852(class_2561.method_43470("Shard ").method_10862(new class_2583(0xff0000,"click-one"))).method_10852(class_2561.method_43470("Keeper").method_10862(new class_2583(0x0000ff,"click-two")));
  ok(apply(mixed,translate)==mixed,"Different click/font metadata must fail open");
  class_5250 icons=class_2561.method_43470("");icons.method_10852(class_2561.method_43470("\uea00 ").method_10862(new class_2583(0xaaaaaa,"icon-font"))).method_10852(gradient("Shard Keeper","name-font")).method_10852(class_2561.method_43470(" Lv. 3+").method_10862(new class_2583(0xffaa00,"level-font")));
  class_2561 iconResult=(class_2561)apply(icons,translate);ok(iconResult.getString().equals("\uea00 碎片管理員 Lv. 3+"),"Icon or level modified");
  ok(iconResult.method_10855().get(0).method_10866().metadata.equals("icon-font"),"Icon font lost");
  var last=iconResult.method_10855().get(iconResult.method_10855().size()-1);ok(last.method_10866().metadata.equals("level-font"),"Level font lost");
  ok(apply(null,translate)==null,"Null changed");ok(apply("Shard Keeper",translate).equals("Shard Keeper"),"Text helper must not rewrite arbitrary Strings");
  System.out.println("STYLED_NAME_CONTRACT_PASS checks="+checks+" gradient=true metadata_safe=true resolved_text=true");
 }
}
