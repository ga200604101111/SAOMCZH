package fixture;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.function.BooleanSupplier;
public final class WatchContract {
 static void await(BooleanSupplier test,String reason)throws Exception{long until=System.nanoTime()+12_000_000_000L;while(System.nanoTime()<until){if(test.getAsBoolean())return;Thread.sleep(25);}throw new AssertionError(reason);}
 static void put(Path to,byte[]data)throws Exception{Files.createDirectories(to.getParent());Path t=to.resolveSibling(to.getFileName()+".tmp");Files.write(t,data);Files.move(t,to,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
 public static void main(String[]args)throws Exception{
  Path root=Paths.get(args[0]),fixtures=Paths.get(args[1]),rules=root.resolve("SAO_ZH_Extensions/rules");LiveContract.Screen screen=new LiveContract.Screen();Object chosen=screen.selected;
  if(!screen.title().equals("Quest title"))throw new AssertionError("Initial text differs");
  for(String name:new String[]{"a","b"}){put(rules.resolve("candidate.sig"),Files.readAllBytes(fixtures.resolve(name+".sig")));put(rules.resolve("candidate.json"),Files.readAllBytes(fixtures.resolve(name+".json")));String wanted=name.equals("a")?"任務標題":"新的任務標題";await(()->screen.title().equals(wanted),"File watcher did not activate "+name);}
  put(root.resolve("SAO_ZH_Extensions/control.json"),"{\"action\":\"rollback\"}".getBytes(StandardCharsets.UTF_8));await(()->screen.title().equals("任務標題"),"Rollback control file ignored");
  if(!screen.input.equals("typed search")||screen.scroll!=37||screen.selected!=chosen)throw new AssertionError("Watcher update disturbed current UI state");
  // Wait for the daemon to persist the command acknowledgement before this test JVM exits.
  await(()->{try{return Files.readString(root.resolve("SAO_ZH_Extensions/status.txt")).contains("sequence=1 ")&&Files.readString(root.resolve("SAO_ZH_Extensions/watch-state.json")).contains("\"paused\":true");}catch(Exception e){return false;}},"Rollback acknowledgement not durable");
  System.out.println("AUTOMATIC_LOCAL_WATCH_REFRESH_AND_ROLLBACK_PASS same_jvm=true same_screen=true");
 }
}
