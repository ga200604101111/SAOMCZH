package sao.zh.boot;
import java.nio.file.*;
/** Run through a real -javaagent in an isolated fixture, never starts Minecraft. */
public final class StartupRulesLaunchContract {
 public static void main(String[]args)throws Exception{
  Object runtime=Agent.runtime();if(runtime==null)throw new AssertionError("Runtime did not initialize");
  String status=String.valueOf(runtime.getClass().getMethod("status").invoke(runtime));
  if(!status.contains("version=1.7.0")||!status.contains("sequence=109")||!status.contains("hooks=120"))throw new AssertionError(status);
  Path staged=Agent.root().resolve("SAO_ZH_Extensions/rules/next-start.json");if(Files.exists(staged))throw new AssertionError("Startup rule offer was not consumed");
  System.out.println("REAL_AGENT_STARTUP_RULES_PASS "+status+" no_game=true");
 }
}
