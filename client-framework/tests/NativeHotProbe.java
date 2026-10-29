package fixture;
import java.lang.reflect.*;import java.nio.file.*;
public final class NativeHotProbe {
 public static void verify()throws Exception{
  String dir=System.getProperty("sao.zh.native.fixtures");if(dir==null)throw new AssertionError("Fixture directory not supplied");
  Class<?>t=Class.forName("net.minecraft.class_2561"),m=Class.forName("net.minecraft.class_5250"),st=Class.forName("net.minecraft.class_2583");
  Object raw=t.getMethod("method_43470",String.class).invoke(null,"New field text");
  Object style=st.getMethod("method_36139",int.class).invoke(st.getField("field_24360").get(null),0x55ccff);m.getMethod("method_10862",st).invoke(raw,style);
  Object child=t.getMethod("method_43470",String.class).invoke(null," + unchanged");m.getMethod("method_10852",t).invoke(raw,child);
  NativeTextScreen screen=new NativeTextScreen();screen.text=(net.minecraft.class_2561)raw;if(screen.draw()!=raw)throw new AssertionError("Initial model unexpected");
  Object engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);
  Method update=engine.getClass().getMethod("applySigned",byte[].class,byte[].class);
  update.invoke(engine,Files.readAllBytes(Paths.get(dir,"native.json")),Files.readAllBytes(Paths.get(dir,"native.sig")));
  Object translated=screen.draw();
  if(!"新文字 + unchanged".equals(t.getMethod("getString").invoke(translated)))throw new AssertionError("Native Text update not visible");
  if(!style.equals(t.getMethod("method_10866").invoke(translated)))throw new AssertionError("Native Text style lost");
  if(screen.text!=raw||!"keep input".equals(screen.input)||screen.scroll!=23)throw new AssertionError("Screen state/model changed");
  if(!"New field text + unchanged".equals(t.getMethod("getString").invoke(raw)))throw new AssertionError("Original Text mutated");
  update.invoke(engine,Files.readAllBytes(Paths.get(dir,"nativeempty.json")),Files.readAllBytes(Paths.get(dir,"nativeempty.sig")));
  if(screen.draw()!=raw)throw new AssertionError("Removing native hook failed to restore object identity");
  System.out.println("REAL_MINECRAFT_TEXT_HOT_FIELD_STYLE_SIBLINGS_STATE_AND_REMOVAL_PASS");
 }
}
