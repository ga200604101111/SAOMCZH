package sao.zh.compat16;
import java.lang.reflect.*;import java.util.*;
public final class PerformanceContract {
 static int checks;static void ok(boolean x,String s){checks++;if(!x)throw new AssertionError(s);}
 static long count(Map<?,?> s,int i){return ((Number)((Map<?,?>)((List<?>)s.get("channels")).get(i)).get("count")).longValue();}
 public static void main(String[]args)throws Exception{
  Class<?> c;try{c=Class.forName("sao.zh.compat16.PerformanceBridge");}catch(ClassNotFoundException e){throw new AssertionError("Missing opt-in performance diagnostics",e);}
  Method enabled=c.getMethod("setEnabled",boolean.class),record=c.getMethod("record",int.class,long.class),begin=c.getMethod("begin",int.class),end=c.getMethod("end",int.class),snapshot=c.getMethod("snapshot");
  enabled.invoke(null,false);record.invoke(null,0,1000000L);ok(count((Map<?,?>)snapshot.invoke(null),0)==0,"Disabled diagnostic recorded data");
  enabled.invoke(null,true);
  for(int i=0;i<5000;i++)record.invoke(null,0,(i<4990?10000000L:100000000L));
  Map<?,?> s=(Map<?,?>)snapshot.invoke(null);Map<?,?> frame=(Map<?,?>)((List<?>)s.get("channels")).get(0);
  ok(count(s,0)==5000,"Total sample count lost");ok(((Number)frame.get("window_samples")).intValue()==4096,"Ring size is unbounded or incorrect");
  ok(((Number)frame.get("p50_ms")).doubleValue()==10.0,"Wrong median");ok(((Number)frame.get("max_ms")).doubleValue()==100.0,"Wrong maximum");
  ok(((Number)frame.get("over_50ms_count")).longValue()==10,"Stutter count wrong");
  begin.invoke(null,1);end.invoke(null,1);ok(count((Map<?,?>)snapshot.invoke(null),1)==1,"Completed tick not timed");
  begin.invoke(null,2);begin.invoke(null,2);end.invoke(null,2);s=(Map<?,?>)snapshot.invoke(null);
  ok(count(s,2)==1,"Abandoned timing contaminated next sample");ok(((Number)s.get("abandoned_scopes")).longValue()>=1,"Abandoned scope not disclosed");
  record.invoke(null,-1,1L);record.invoke(null,1000,1L);record.invoke(null,0,-10L);ok(count((Map<?,?>)snapshot.invoke(null),0)==5000,"Invalid samples accepted");
  enabled.invoke(null,false);long old=count((Map<?,?>)snapshot.invoke(null),1);begin.invoke(null,1);end.invoke(null,1);ok(count((Map<?,?>)snapshot.invoke(null),1)==old,"Disabled begin/end is active");
  enabled.invoke(null,true);ok(count((Map<?,?>)snapshot.invoke(null),0)==0,"New recording did not reset session");
  for(int i=0;i<10;i++){begin.invoke(null,0);end.invoke(null,0);}s=(Map<?,?>)snapshot.invoke(null);ok(count(s,7)==9,"Frame interval must be independently measured");
  enabled.invoke(null,false);System.out.println("PERFORMANCE_BOUNDED_TIMING_PASS checks="+checks+" no_game=true");
 }
}
