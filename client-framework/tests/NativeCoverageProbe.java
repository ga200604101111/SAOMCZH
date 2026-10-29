package fixture;
import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
public final class NativeCoverageProbe {
 static int checks;static void ok(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 static Object getter(Class<?>type,String name,Object value)throws Exception{Method method=type.getDeclaredMethod(name);method.setAccessible(true);return method.invoke(value);}
 static Object record(Class<?>c,Map<String,String>values)throws Exception{
  RecordComponent[]components=c.getRecordComponents();Class<?>[]types=Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);Object[]args=new Object[types.length];
  for(int i=0;i<args.length;i++){Class<?>t=types[i];String n=components[i].getName();args[i]=t==String.class?values.getOrDefault(n,""):t==int.class?0:t==long.class?0L:t==boolean.class?false:t==float.class?0f:t==double.class?0d:List.class.isAssignableFrom(t)?List.of():null;}
  Constructor<?>ctor=c.getDeclaredConstructor(types);ctor.setAccessible(true);return ctor.newInstance(args);
 }
 public static void verify()throws Exception{
  Path p=Path.of(System.getProperty("sao.zh.coverage.fixtures"));ClassLoader cl=NativeCoverageProbe.class.getClassLoader();
  List<String>targets=Files.readAllLines(p.resolve("targets.txt"));for(String n:targets)Class.forName(n,false,cl).getDeclaredMethods();
  Object engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);Method apply=engine.getClass().getMethod("applySigned",byte[].class,byte[].class);
  apply.invoke(engine,Files.readAllBytes(p.resolve("native-rules.json")),Files.readAllBytes(p.resolve("native-rules.sig")));
  Class<?>action=Class.forName("fr.sao.saoui.hud.actionbar.ActionBarTranslator");Method segment=action.getDeclaredMethod("translateSegment",String.class,List.class);segment.setAccessible(true);
  String raw="⚔ 打擊 21 | total 1009 | ~26 DPS";String result=(String)segment.invoke(null,raw,List.of());ok(result.equals("⚔ 單次傷害 21 | 總傷害 1009 | 約 26 DPS"),"Actual ActionBar label transformation failed: "+result);
  ok(segment.invoke(null,"total soigné 548",List.of()).equals("總治療量 548"),"Healing mislabeled");ok(segment.invoke(null,"null",List.of()).equals("null"),"Unrelated null was removed");
  Class<?>model=Class.forName("fr.sao.tooltip.TipModel");Object item=record(model,Map.of("name","Training Pavise"));ok(getter(model,"name",item).equals("訓練用大盾"),"Cached native tooltip name still untranslated");Field field=model.getDeclaredField("name");field.setAccessible(true);ok(field.get(item).equals("Training Pavise"),"Original tooltip model mutated");
  Class<?>stat=Class.forName("fr.sao.tooltip.TipModel$Stat");Object durability=record(stat,Map.of("label","Guard Durability","value","8"));ok(getter(stat,"label",durability).equals("格擋耐久"),"Native stat label missing");ok(getter(stat,"value",durability).equals("8"),"Native stat number changed");
  Class<?>screen=Class.forName("fr.sao.interlink.InterlinkScreen");Method duration=screen.getDeclaredMethod("duration",long.class);duration.setAccessible(true);String time=(String)duration.invoke(null,5L);ok(time.contains("秒")&&!time.contains("s"),"Transfer duration is not seconds: "+time);
  Class<?>dispatch=Class.forName("sao.zh.api.Dispatch",true,null),text=Class.forName("net.minecraft.class_2561"),mutable=Class.forName("net.minecraft.class_5250"),style=Class.forName("net.minecraft.class_2583");
  Object line=text.getMethod("method_43470",String.class).invoke(null,"打擊 ");Object count=text.getMethod("method_43470",String.class).invoke(null,"21");Object tail=text.getMethod("method_43470",String.class).invoke(null," | total 1009 | ~26 DPS");Object color=style.getMethod("method_36139",int.class).invoke(style.getField("field_24360").get(null),0xff2244);mutable.getMethod("method_10862",style).invoke(line,color);mutable.getMethod("method_10852",text).invoke(line,count);mutable.getMethod("method_10852",text).invoke(line,tail);
  Object changed=dispatch.getMethod("value",String.class,Object.class,Class.class).invoke(null,"combat-feedback:damage-text",line,action);
  ok(text.getMethod("getString").invoke(changed).equals("單次傷害 21 | 總傷害 1009 | 約 26 DPS"),"Split native Text lost translation");ok(color.equals(text.getMethod("method_10866").invoke(changed)),"Native text color lost");ok(text.getMethod("getString").invoke(line).equals("打擊 21 | total 1009 | ~26 DPS"),"Source Text mutated");
  Object title=dispatch.getMethod("text",String.class,String.class,Class.class).invoke(null,"worlds-locations:server-title","傳送→FRACTURED UNDERWORLD",screen);ok(title.equals("傳送→破碎冥界"),"World title not localized");
  apply.invoke(engine,Files.readAllBytes(p.resolve("native-remove.json")),Files.readAllBytes(p.resolve("native-remove.sig")));
  ok(segment.invoke(null,raw,List.of()).equals(raw),"Removed actionbar hook not restored");ok(getter(model,"name",item).equals("Training Pavise"),"Removed tooltip hook not restored");
  System.out.println("CURRENT_SAOUI_NATIVE_COVERAGE_STYLE_NUMBERS_ROLLBACK_PASS checks="+checks+" targets="+targets.size()+" no_GL=true");
 }
}
