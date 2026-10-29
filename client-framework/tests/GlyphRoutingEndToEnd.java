package sao.zh.engine;
import java.lang.reflect.*;import java.lang.instrument.Instrumentation;import java.nio.file.*;import java.util.*;
/** Executes the real Engine -> bootstrap compatibility -> manifest mapping route. */
public final class GlyphRoutingEndToEnd {
 static int checks;static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 public static void main(String[]args)throws Exception{
  Class<?> policy=Class.forName("sao.zh.compat16.LiteralGlyphs",true,null);
  ok(policy.getClassLoader()==null,"Glyph service must live on the shared bootstrap loader");
  policy.getMethod("configure",Path.class).invoke(null,Path.of(args[0]));
  LiteralGlyphBridge.bind();
  Constructor<Engine> c=Engine.class.getDeclaredConstructor(Instrumentation.class,Path.class,Path.class,boolean.class);c.setAccessible(true);
  Path temp=Files.createTempDirectory("glyph-engine-test-");Engine e=c.newInstance(null,temp,null,true);
  Field dictionary=Engine.class.getDeclaredField("dictionary");dictionary.setAccessible(true);
  dictionary.set(e,Map.of("lines",Map.of("19. The Chamber Beneath the Fields","19. 田野下的密室","Bandit home","前往盜匪的居所","Fresh world","嶄新的世界")));
  Script.Hook h=new Script.Hook(Map.of("id","quests-objectives:full-title","class","fr/sao/questtracker/screen/QuestMenuScreen","method","label","descriptor","()Ljava/lang/String;","kind","return","section","lines"));
  Method translate=Engine.class.getDeclaredMethod("translate",Script.Hook.class,String.class);translate.setAccessible(true);
  String display=(String)translate.invoke(e,h,"19. The Chamber Beneath the Fields");
  ok(!display.contains("田"),"Engine emitted the menu-banner codepoint");
  ok(LiteralGlyphBridge.readable(display).equals("19. 田野下的密室"),"Displayed title no longer represents the same Chinese");
  ok(translate.invoke(e,h,display).equals(display),"Second interception damaged an already projected title");
  ok(translate.invoke(e,h,"田").equals("田"),"Raw server banner icon was modified without a translation");
  ok(translate.invoke(e,h,"Player_123").equals("Player_123"),"Unknown player ID modified");
  ok(translate.invoke(e,h,"Bandit home").equals("前往盜匪的居所"),"Non-colliding Han changed unnecessarily");
  ok(translate.invoke(e,h,"Fresh world").equals("嶄新的世界"),"Newly covered Han requires no transliteration or word substitution");
  List<String> rows=CjkWrap.wrap(display,5,8,s->s.codePointCount(0,s.length()));
  ok(rows.size()>1,"CJK aliases did not participate in wrapping");
  ok(rows.stream().allMatch(s->s.codePointCount(0,s.length())<=5),"Wrap exceeds supplied native width");
  Class<?> oldLayout=Class.forName("sao.zh.compat16.CjkLayout",true,null);
  ok(Boolean.TRUE.equals(oldLayout.getMethod("containsCjk",String.class).invoke(null,display)),"Compatibility layout did not recognize aliases");
  Field collector=Engine.class.getDeclaredField("unknownCollector");collector.setAccessible(true);((UnknownCollector)collector.get(e)).close();
  System.out.println("GLYPH_ROUTING_END_TO_END_PASS checks="+checks+" bootstrap=true no_game=true");
 }
}
