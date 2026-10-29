package sao.zh.engine;
import java.nio.file.*;import java.util.*;import java.nio.charset.StandardCharsets;
/** Strict opt-in source capture: never touch chat, credentials, player names or the render thread disk. */
public final class UnknownCollectorContract {
 static int checks;
 static void ok(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);}
 static void forbids(String s){ok(!UnknownCollector.safe(s,"quest"),"Private or unsafe text accepted "+s);}
 public static void main(String[]args)throws Exception{
  ok(UnknownCollector.permitted("fr/sao/questtracker/screen/QuestMenuScreen","argument","quest"),"Quest display source disallowed");
  ok(UnknownCollector.permitted("fr/sao/codex/CodexScreen","return","codex"),"Codex display source disallowed");
  ok(UnknownCollector.permitted("com/eighteen/saojob/skills/SkillDetailPopup","field","skill"),"Skill description source disallowed");
  ok(UnknownCollector.permitted("fr/sao/iteminfo/ItemInfoClient$Info","return","item"),"Item info source disallowed");
  ok(!UnknownCollector.permitted("net/minecraft/class_338","argument","quest"),"ChatHud wrongly allowed");
  ok(!UnknownCollector.permitted("fr/sao/pass/screen/PassCanvas","argument","quest"),"Generic draw falsely allowed");
  ok(!UnknownCollector.permitted("fr/sao/playerdisplay/card/PlayerCardRenderer","return","quest"),"Player name surface allowed");
  ok(!UnknownCollector.permitted("fr/sao/questtracker/screen/QuestMenuScreen","refresh","quest"),"Refresh listener allowed to capture");
  ok(!UnknownCollector.permitted("fr/sao/questtracker/screen/QuestMenuScreen","argument","item"),"Origin/owner mismatch allowed");
  ok(UnknownCollector.safe("A wild beast from the forests of the first floor.","codex"),"Codex text rejected");
  ok(UnknownCollector.safe("Recueillir 10 plantes dans la vallée.","quest"),"French quest rejected");
  ok(UnknownCollector.safe("恢復 40 Mana.","skill"),"Mixed Chinese/English should be collected");
  ok(UnknownCollector.safe("Restores 150 HP, 150 mana","item"),"Item text rejected");
  for(String value:List.of("","null","old","HI","YingHuang09","Example_123","User_123: hi",
      "[Floor 1] PLAYER: Chat","<Player_123> hi","test@example.com","https://example.com",
      "192.168.0.1","/home/user/private","D:\\Windows\\User\\config","0d46a000-f2c6-42f1-97f2-dbd604fb0dde",
      "§fHello","minecraft:overworld", "Password: apples", "PASSWORD: pears", "apiKey=secret123", "Bearer : token", "AUTHORIZATION: allowed"))forbids(value);
  ok(!UnknownCollector.safe("a".repeat(600),"codex"),"Long text not bounded");
  Path root=Files.createTempDirectory("sao-collector-test-");
  UnknownCollector collector=new UnknownCollector(root);
  ok(collector.offer("quest","names","quests-objectives:quest-row-title","The Chamber Beneath the Fields"),"First quest row dropped");
  ok(!collector.offer("quest","names","quests-objectives:quest-row-title","The Chamber Beneath the Fields"),"Duplicate captured");
  ok(collector.offer("codex","lines","codex-creatures:codex-display-lore","A wild beast from the forests of the first floor."),"Codex lore dropped");
  ok(collector.offer("skill","lines","skills-progression:skilltree-lore-read","恢復 40 Mana."),"Partial translation dropped");
  ok(!collector.offer("quest","lines","quests-objectives:quest-row-title","Player_123: hello"),"Player chat captured");
  collector.close();
  Path file=root.resolve("SAO_TW_Dynamic/review/hook_missing_zh_tw.jsonl");
  ok(Files.isRegularFile(file),"Capture file missing");
  List<String>lines=Files.readAllLines(file,StandardCharsets.UTF_8);
  ok(lines.size()==3,"Queue duplicates or skipped writes "+lines.size());
  ok(lines.stream().anyMatch(x->x.contains("The Chamber Beneath the Fields")),"Quest original missing");
  ok(lines.stream().anyMatch(x->x.contains("恢復 40 Mana.")),"Mixed text missing");
  ok(lines.stream().allMatch(x->x.contains("\"locale\":\"zh_tw\"")&&x.contains("\"status\":\"unreviewed\"")),"Unexpected line schema");
  ok(lines.stream().noneMatch(x->x.contains("Player_123:")||x.contains("YingHuang09")),"Private content in output");
  ok(Files.size(file)<65536,"Output unexpectedly large");
  System.out.println("LOCAL_MISSING_CAPTURE_PRIVACY_NONBLOCKING_PASS checks="+checks+" rows="+lines.size());
 }
}
