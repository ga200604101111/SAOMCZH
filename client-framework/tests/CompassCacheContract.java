package sao.zh.engine;
import fr.sao.questtracker.hud.QuestCompassRenderer;
public final class CompassCacheContract {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  QuestCompassRenderer.seed();ok(!QuestCompassRenderer.empty(),"Fixture not seeded");
  CompassTextState.refresh(QuestCompassRenderer.class,4);ok(QuestCompassRenderer.empty(),"Initial epoch must invalidate source identity cache");
  ok(QuestCompassRenderer.infoName.equals("original"),"Stored title/string should be rebuilt by original code, not overwritten");
  QuestCompassRenderer.seed();CompassTextState.refresh(QuestCompassRenderer.class,4);ok(!QuestCompassRenderer.empty(),"Same epoch should not repeatedly clear caches");
  CompassTextState.refresh(QuestCompassRenderer.class,5);ok(QuestCompassRenderer.empty(),"New dictionary epoch did not clear source cache");
  QuestCompassRenderer.seed();CompassTextState.refresh(CompassCacheContract.class,6);ok(!QuestCompassRenderer.empty(),"Wrong owner touched display data");
  CompassTextState.refresh(null,7);ok(!QuestCompassRenderer.empty(),"Null owner touched display data");
  System.out.println("COMPASS_CACHE_CONTRACT_PASS checks="+checks+" bounded_source_fields=true once_per_epoch=true wrong_owner_untouched=true");
 }
}
