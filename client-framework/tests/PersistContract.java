package fixture;
import java.nio.file.*;import java.lang.reflect.*;
public final class PersistContract {
 public static void main(String[]args)throws Exception{
  Object engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);if(engine==null)throw new AssertionError("Engine absent");
  LiveContract.Screen screen=new LiveContract.Screen();
  if(args[0].equals("write")){
   for(String name:new String[]{"a","b"})engine.getClass().getMethod("applySigned",byte[].class,byte[].class).invoke(engine,Files.readAllBytes(Paths.get(args[1],name+".json")),Files.readAllBytes(Paths.get(args[1],name+".sig")));
   if(!screen.title().equals("新的任務標題"))throw new AssertionError("Write phase failed");
   System.out.println("PERSIST_WRITE_PASS");
  }else{
   if(!screen.title().equals("新的任務標題"))throw new AssertionError("Active generation not restored");
   engine.getClass().getMethod("rollback").invoke(engine);
   if(!screen.title().equals("任務標題"))throw new AssertionError("Prior generation lost across restart");
   System.out.println("ROLLBACK_SURVIVES_RESTART_PASS");
  }
 }
}
