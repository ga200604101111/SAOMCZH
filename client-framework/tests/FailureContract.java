package fixture;
import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
public final class FailureContract {
 static int checks;static void ok(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 public static void main(String[]args)throws Exception{
  Class<?>boot=Class.forName("sao.zh.boot.Agent");Object engine=boot.getMethod("runtime").invoke(null);
  if(args[0].equals("badboot")){ok(engine==null,"Untrusted runtime executed");System.out.println("UNTRUSTED_RUNTIME_KEEPS_JVM_ALIVE_PASS");return;}
  ok(engine!=null,"Runtime absent");Class<?>bridge=Class.forName("sao.zh.api.Dispatch",true,null),handler=Class.forName("sao.zh.api.Dispatch$Handler",true,null);
  Method bind=bridge.getMethod("bind",handler,long.class),text=bridge.getMethod("text",String.class,String.class,Class.class);
  Object fail=Proxy.newProxyInstance(handler.getClassLoader(),new Class<?>[]{handler},(proxy,m,a)->{throw new IllegalStateException("expected extension failure");});
  bind.invoke(null,fail,42L);ok("original".equals(text.invoke(null,"x","original",FailureContract.class)),"Handler exception escaped");
  ok(((Number)bridge.getMethod("faults").invoke(null)).longValue()==1,"Fault counter wrong");
  Object recurse=Proxy.newProxyInstance(handler.getClassLoader(),new Class<?>[]{handler},(proxy,m,a)->text.invoke(null,"recursive",a[1],FailureContract.class));
  bind.invoke(null,recurse,43L);ok("original".equals(text.invoke(null,"x","original",FailureContract.class)),"Recursion guard failed");
  Object wrong=Proxy.newProxyInstance(handler.getClassLoader(),new Class<?>[]{handler},(proxy,m,a)->Integer.valueOf(4));
  bind.invoke(null,wrong,44L);ok("original".equals(text.invoke(null,"x","original",FailureContract.class)),"Unexpected result type corrupted String");
  bind.invoke(null,null,45L);ok(text.invoke(null,"x",null,FailureContract.class)==null,"Null handling failed");
  System.out.println("DISPATCH_FAILURE_RECURSION_TYPE_NULL_PASS checks="+checks);
 }
}
