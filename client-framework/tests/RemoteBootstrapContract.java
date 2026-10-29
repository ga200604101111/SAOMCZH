package fixture;
import java.lang.reflect.*;import java.nio.file.*;
public final class RemoteBootstrapContract {
 public static void main(String[]args)throws Exception{
  Class<?>kernel=Class.forName("sao.zh.boot.Agent");Object engine=kernel.getMethod("runtime").invoke(null);
  if(engine==null)throw new AssertionError("Clean installation did not download/load signed runtime");
  Path root=(Path)kernel.getMethod("root").invoke(null);
  if(!Files.isRegularFile(root.resolve("SAO_ZH_Extensions/runtime/current.properties")))throw new AssertionError("Remote runtime manifest not cached");
  engine.getClass().getMethod("checkRemoteNow").invoke(engine);
  String status=(String)engine.getClass().getMethod("status").invoke(engine);
  if(!status.contains("version=1.0.0 ")||!status.contains("sequence=100 ")||!status.contains("hooks=3 "))throw new AssertionError("Remote signed rule set not applied: "+status);
  Path rules=root.resolve("SAO_ZH_Extensions/rules/active.txt");if(!Files.isRegularFile(rules))throw new AssertionError("Verified remote rules not retained");
  System.out.println("CLEAN_INSTALL_REAL_GITHUB_SIGNED_RUNTIME_AND_HOT_RULES_PASS "+status+" no_game_started=true");
 }
}
