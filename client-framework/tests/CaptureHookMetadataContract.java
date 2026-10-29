package sao.zh.engine;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class CaptureHookMetadataContract {
 static int checks;static void ok(boolean yes,String reason){checks++;if(!yes)throw new AssertionError(reason);}
 static byte[] document(String owner,String kind,String capture){
  String h="{\"id\":\"quest:test\",\"class\":\""+owner+"\",\"method\":\"title\",\"descriptor\":\"(Ljava/lang/String;)Ljava/lang/String;\",\"kind\":\""+kind+"\",\"section\":\"names\",\"locale\":\"zh_tw\",\"capture\":\""+capture+"\"}";
  return ("{\"schema_version\":2,\"version\":\"1.6.0\",\"sequence\":108,\"expires\":2100000000,\"hooks\":["+h+"]}").getBytes(StandardCharsets.UTF_8);
 }
 static void reject(String owner,String kind,String capture){
  try{Script.parse(document(owner,kind,capture));throw new AssertionError("Illegal Hook accepted "+owner+" "+kind+" "+capture);}
  catch(IllegalArgumentException expected){checks++;}
 }
 public static void main(String[]args){
  Script allowed=Script.parse(document("fr/sao/questtracker/screen/QuestMenuScreen","return","quest"));
  Script.Hook h=allowed.byId.get("quest:test");
  ok("quest".equals(h.capture),"Capture metadata absent");
  reject("net/minecraft/class_338","return","quest");
  reject("fr/sao/pass/screen/PassCanvas","return","quest");
  reject("fr/sao/questtracker/screen/QuestMenuScreen","return","codex");
  reject("fr/sao/questtracker/screen/QuestMenuScreen","return","unknown");
  reject("fr/sao/questtracker/screen/QuestMenuScreen","refresh","quest");
  System.out.println("CAPTURE_METADATA_ALLOWLIST_AND_RULE_PARSER_PASS checks="+checks);
 }
}
