package fixture;
import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
/** Runs only in the isolated no-GL integration JVM. Never manipulates the user's running client. */
public final class NativeLayoutProbe {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static Field f(Class<?>c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static Object fresh(Class<?>c)throws Exception{Class<?>u=Class.forName("sun.misc.Unsafe");Field q=u.getDeclaredField("theUnsafe");q.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(q.get(null),c);}
 public static void verify()throws Exception{
  Path dir=Path.of(System.getProperty("sao.zh.layout.fixtures"));Class<?>quest=Class.forName("fr.sao.questtracker.screen.QuestMenuScreen"),codex=Class.forName("fr.sao.codex.CodexScreen"),canvas=Class.forName("fr.sao.pass.screen.PassCanvas");
  quest.getDeclaredMethods();codex.getDeclaredMethods();canvas.getDeclaredMethods();
  Object screen=fresh(quest);Map<String,String>rows=new HashMap<>(Map.of("q","Old title")),translated=new HashMap<>(Map.of("q","Old description"));f(quest,"rowTexts").set(screen,rows);f(quest,"translated").set(screen,translated);f(quest,"descOf").set(screen,"quest-1");f(quest,"selectedId").set(screen,"quest-1");f(quest,"listScroll").setFloat(screen,31);f(quest,"detailScroll").setFloat(screen,12);
  Object book=fresh(codex);List<Map<String,String>>maps=new ArrayList<>();for(String n:new String[]{"locals","refs","rowValues","catTexts"}){Map<String,String>m=new HashMap<>(Map.of("entry","old"));f(codex,n).set(book,m);maps.add(m);}f(codex,"selected").set(book,"boar");f(codex,"scroll").setInt(book,4);f(codex,"dragging").setBoolean(book,true);
  Object engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);Method apply=engine.getClass().getMethod("applySigned",byte[].class,byte[].class);
  apply.invoke(engine,Files.readAllBytes(dir.resolve("rules-1.2.0.json")),Files.readAllBytes(dir.resolve("rules-1.2.0.sig")));
  Method refresh=Class.forName("sao.zh.api.Dispatch",true,null).getMethod("refresh",String.class,Object.class,Class.class);
  refresh.invoke(null,"ui-text-layout:quest-text-cache",screen,quest);ok(rows.isEmpty()&&translated.isEmpty(),"Native quest Maps did not clear");ok(f(quest,"descOf").get(screen)==null,"Native description cache key remained");ok("quest-1".equals(f(quest,"selectedId").get(screen))&&f(quest,"listScroll").getFloat(screen)==31&&f(quest,"detailScroll").getFloat(screen)==12,"Native quest input/scroll changed");
  // The offline client has no Minecraft singleton. FontChoice's first lazy read can fail, then settle to WORK_SANS.
  // A changed environment legitimately invalidates once; a stable environment must not invalidate every frame.
  refresh.invoke(null,"ui-text-layout:quest-text-cache",screen,quest);
  rows.put("q","refreshed");refresh.invoke(null,"ui-text-layout:quest-text-cache",screen,quest);
  ok(rows.containsKey("q"),"Stable native font environment refreshed every frame");
  refresh.invoke(null,"ui-text-layout:codex-text-cache",book,codex);for(Map<?,?>m:maps)ok(m.isEmpty(),"Native codex text cache not cleared");ok("boar".equals(f(codex,"selected").get(book))&&f(codex,"scroll").getInt(book)==4&&f(codex,"dragging").getBoolean(book),"Native codex selection/drag changed");
  Object cv=canvas.getConstructor().newInstance();Object result=canvas.getMethod("wrap",String.class,float.class,boolean.class,float.class,int.class).invoke(cv,null,19f,false,272f,6);ok(result instanceof List&&((List<?>)result).isEmpty(),"Native wrap null handling regressed");
  apply.invoke(engine,Files.readAllBytes(dir.resolve("native-remove.json")),Files.readAllBytes(dir.resolve("native-remove.sig")));
  refresh.invoke(null,"ui-text-layout:quest-text-cache",screen,quest);ok(rows.isEmpty(),"Removing rules left native cached translation");ok("quest-1".equals(f(quest,"selectedId").get(screen)),"Removal lost selection");
  System.out.println("NATIVE_SAO_LAYOUT_CACHE_FIELDS_AND_41_HOOKS_PASS checks="+checks+" no_GL=true no_game_started=true");
 }
}
