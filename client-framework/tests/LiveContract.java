package fixture;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
/** One JVM, already-created UI object. No Minecraft instance or GL context. */
public final class LiveContract {
 public static final class Screen {
  public String heading="Quest title"; public String input="typed search"; public int scroll=37; public Object selected=new Object();
  public String title(){return "Quest title";}
  public String draw(){return heading;}
  public String echo(long offset,String text){return text;}
  public String nil(){return null;}
  public String invoke(){return source();}
  public String source(){return "Quest title";}
 }
 static int assertions;
 static void ok(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
 static Object engine;
 static Object invoke(String method,Class<?>[]types,Object...args)throws Exception{
  try{return engine.getClass().getMethod(method,types).invoke(engine,args);}
  catch(InvocationTargetException x){Throwable cause=x.getCause();if(cause instanceof Exception)throw (Exception)cause;throw x;}
 }
 static void apply(Path root,String name)throws Exception {
  invoke("applySigned",new Class<?>[]{byte[].class,byte[].class},Files.readAllBytes(root.resolve(name+".json")),Files.readAllBytes(root.resolve(name+".sig")));
 }
 public static void main(String[]args)throws Exception{
  Screen screen=new Screen();Object chosen=screen.selected;Path fixtures=Paths.get(args[0]);
  ok(screen.title().equals("Quest title"),"Fixture must be loaded before adding its hook");
  Class<?> boot;
  try{boot=Class.forName("sao.zh.boot.Agent");}catch(ClassNotFoundException absent){throw new AssertionError("No external hot runtime: an already-open screen cannot receive new interception rules",absent);}
  engine=boot.getMethod("runtime").invoke(null);ok(engine!=null,"External engine not loaded");
  apply(fixtures,"a");
  ok(screen.title().equals("任務標題"),"New RETURN hook did not affect already-loaded class");
  ok(screen.draw().equals("任務標題"),"Field projection did not refresh existing screen");
  ok(screen.heading.equals("Quest title"),"Original UI model mutated");
  ok(screen.input.equals("typed search")&&screen.scroll==37&&screen.selected==chosen,"UI interaction state changed");
  ok(screen.echo(42L,"Quest title").equals("任務標題"),"Long-slot argument hook failed");
  ok(screen.invoke().equals("任務標題"),"Invocation-result projection failed");
  ok(screen.source().equals("Quest title"),"Call-result hook altered unrelated source method");
  ok(screen.nil()==null,"Null changed");
  long builds=((Number)invoke("transformCount",new Class<?>[]{})).longValue();
  apply(fixtures,"b");
  ok(screen.title().equals("新的任務標題"),"Hot policy revision not visible");
  ok(((Number)invoke("transformCount",new Class<?>[]{})).longValue()==builds,"Only text changed but bytecode was retransformed");
  try{apply(fixtures,"badtarget");throw new AssertionError("Bad target accepted");}catch(IllegalArgumentException expected){assertions++;}
  ok(screen.title().equals("新的任務標題"),"Invalid method update destroyed last good state");
  try{apply(fixtures,"badop");throw new AssertionError("Unbounded operation accepted");}catch(IllegalArgumentException expected){assertions++;}
  ok(screen.title().equals("新的任務標題"),"Invalid operation destroyed previous snapshot");
  byte[]wrong=Files.readAllBytes(fixtures.resolve("b.sig"));wrong[0]^=1;
  try{invoke("applySigned",new Class<?>[]{byte[].class,byte[].class},Files.readAllBytes(fixtures.resolve("b.json")),wrong);throw new AssertionError("Tampered signature accepted");}catch(SecurityException expected){assertions++;}
  ok(screen.title().equals("新的任務標題"),"Signature failure modified display");
  apply(fixtures,"empty");
  ok(screen.title().equals("Quest title")&&screen.draw().equals("Quest title"),"Removing hooks failed to restore original behavior");
  invoke("rollback",new Class<?>[]{});
  ok(screen.title().equals("新的任務標題"),"Rollback did not restore previous complete generation");
  ok(screen.input.equals("typed search")&&screen.scroll==37&&screen.selected==chosen,"Rollback reset UI state");
  System.out.println("LIVE_ALREADY_LOADED_HOOK_HOT_UPDATE_ROLLBACK_PASS assertions="+assertions+" pid="+ProcessHandle.current().pid());
 }
}
