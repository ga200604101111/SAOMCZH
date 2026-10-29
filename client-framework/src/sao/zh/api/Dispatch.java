package sao.zh.api;
import java.util.concurrent.atomic.AtomicLong;
/** Tiny stable bootstrap ABI. No IO, no reflection, no game dependencies on render paths. */
public final class Dispatch {
 public interface Handler {Object apply(String key,Object value,Class<?>owner)throws Exception;}
 private static volatile Handler handler;private static final ThreadLocal<Boolean> busy=new ThreadLocal<>();
 private static final AtomicLong faults=new AtomicLong();private static volatile long epoch;
 public static void bind(Handler next,long generation){handler=next;epoch=generation;}
 public static long epoch(){return epoch;}public static long faults(){return faults.get();}
 public static Object value(String key,Object original,Class<?>owner){
  Handler h=handler;if(h==null||original==null||Boolean.TRUE.equals(busy.get()))return original;
  busy.set(true);
  try{Object changed=h.apply(key,original,owner);if(changed==null||!original.getClass().isInstance(changed))return original;return changed;}
  catch(VirtualMachineError fatal){throw fatal;}
  catch(Throwable bad){faults.incrementAndGet();return original;}
  finally{busy.remove();}
 }
 public static String text(String key,String original,Class<?>owner){Object result=value(key,original,owner);return result instanceof String?(String)result:original;}
}
