package sao.zh.api;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;
/** Bootstrap ABI stored in the external runtime, not the installed V16 kernel. */
public final class Dispatch {
 public interface Handler {Object apply(String key,Object value,Class<?>owner)throws Exception;}
 public interface WrapHandler {List<String> apply(String key,Object receiver,Class<?>owner,String text,float size,boolean bold,float width,int limit,List<?>original)throws Exception;}
 public interface RefreshHandler {void apply(String key,Object receiver,Class<?>owner)throws Exception;}
 private static volatile Handler handler;private static volatile WrapHandler wrapper;private static volatile RefreshHandler refresher;
 private static final ThreadLocal<Boolean>busy=new ThreadLocal<>();private static final AtomicLong faults=new AtomicLong();private static volatile long epoch;
 public static void bind(Handler next,long generation){handler=next;epoch=generation;}
 public static void bindSupport(WrapHandler w,RefreshHandler r){wrapper=w;refresher=r;}
 public static long epoch(){return epoch;}public static long faults(){return faults.get();}
 public static Object value(String key,Object original,Class<?>owner){
  Handler h=handler;if(h==null||original==null||Boolean.TRUE.equals(busy.get()))return original;
  busy.set(true);try{Object changed=h.apply(key,original,owner);if(changed==null||!original.getClass().isInstance(changed))return original;return changed;}
  catch(VirtualMachineError fatal){throw fatal;}catch(Throwable bad){faults.incrementAndGet();return original;}finally{busy.remove();}
 }
 /** Description-only List ABI: accept another List implementation without weakening Text guards. */
 public static List<?> description(String key,List<?> original,Class<?> owner){
  Handler h=handler;if(h==null||original==null||Boolean.TRUE.equals(busy.get()))return original;
  busy.set(true);try{
   Object output=h.apply(key,original,owner);if(!(output instanceof List<?> list)||list.size()>128)return original;
   int chars=0;for(Object row:list){if(!(row instanceof String))return original;chars+=((String)row).length();if(chars>8192)return original;}
   return list;
  }catch(VirtualMachineError fatal){throw fatal;}catch(Throwable bad){faults.incrementAndGet();return original;}finally{busy.remove();}
 }
 public static String mobText(String key,String shown,String source,Class<?>owner,boolean player){
  if(player)return source==null?shown:source;
  String translated=text(key,source,owner);
  return source!=null&&!Objects.equals(translated,source)?translated:text(key,shown,owner);
 }
 public static String text(String key,String original,Class<?>owner){Object result=value(key,original,owner);return result instanceof String?(String)result:original;}
 public static List<?> wrap(List<?>original,String key,Object receiver,Class<?>owner,String text,float size,boolean bold,float width,int limit){
  WrapHandler h=wrapper;if(h==null||Boolean.TRUE.equals(busy.get()))return original;
  busy.set(true);try{List<String>r=h.apply(key,receiver,owner,text,size,bold,width,limit,original);return r==null?original:r;}
  catch(VirtualMachineError fatal){throw fatal;}catch(Throwable bad){faults.incrementAndGet();return original;}finally{busy.remove();}
 }
 public static void refresh(String key,Object receiver,Class<?>owner){
  RefreshHandler h=refresher;if(h==null||receiver==null||Boolean.TRUE.equals(busy.get()))return;
  busy.set(true);try{h.apply(key,receiver,owner);}catch(VirtualMachineError fatal){throw fatal;}catch(Throwable bad){faults.incrementAndGet();}finally{busy.remove();}
 }
}
