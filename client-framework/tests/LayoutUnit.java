package fixture;
import java.util.*;import java.util.function.*;import java.lang.reflect.*;
public final class LayoutUnit {
 static int checks;static Method method;
 static void ok(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static double width(String s){return s.codePoints().mapToDouble(c->Character.getType(c)==Character.NON_SPACING_MARK||c==0x200d?0:c<128?7:19).sum();}
 @SuppressWarnings("unchecked") static List<String> wrap(String s,double w,int n)throws Exception{
  try{return (List<String>)method.invoke(null,s,w,n,(ToDoubleFunction<String>)LayoutUnit::width);}catch(InvocationTargetException e){throw (Exception)e.getCause();}
 }
 public static void main(String[]args)throws Exception{
  try{method=Class.forName("sao.zh.engine.CjkWrap").getMethod("wrap",String.class,double.class,int.class,ToDoubleFunction.class);}
  catch(ClassNotFoundException absent){throw new AssertionError("Missing shared CJK wrapping: auction treats Chinese as a single whitespace word",absent);}
  String text="直購商品可立即購買，請先確認金額與數量。購買後可以到領取區取回物品。";
  List<String>x=wrap(text,272,6);ok(x.size()>1,"Auction Chinese still one overflowing line");
  ok(String.join("",x).equals(text),"Characters lost at ordinary boundaries");for(String line:x)ok(width(line)<=272,"Measured auction width overflow");
  x=wrap("abc def ghi",49,6);ok(x.equals(List.of("abc def","ghi")),"English word boundaries changed: "+x);
  x=wrap("第一行\r\n\r\n第二行",100,6);ok(x.equals(List.of("第一行","","第二行")),"CRLF/blank paragraph lost: "+x);
  x=wrap("第一行\n第二行\n第三行",100,2);ok(x.size()==2&&x.get(1).endsWith("…"),"Truncation must be visible");
  x=wrap(text,76,2);ok(x.size()==2&&x.get(1).endsWith("…"),"Line count overflow not marked");for(String line:x)ok(width(line)<=76,"Ellipsis overflow");
  x=wrap("中文𠮷漢字",38,8);ok(String.join("",x).equals("中文𠮷漢字"),"Supplementary codepoint corrupted");for(String l:x)ok(!Character.isLowSurrogate(l.charAt(0)),"Split surrogate");
  x=wrap("Cafe\u0301文字",28,8);ok(String.join("",x).equals("Cafe\u0301文字"),"Combining mark lost");for(String l:x)ok(!l.startsWith("\u0301"),"Combining mark detached");
  ok(wrap("",100,6).isEmpty(),"Empty input");
  for(double bad:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY}){try{wrap("字",bad,6);throw new AssertionError("Invalid width accepted");}catch(IllegalArgumentException expected){checks++;}}
  try{wrap("字".repeat(8193),200,6);throw new AssertionError("Unbounded text");}catch(IllegalArgumentException expected){checks++;}
  try{wrap(text,200,65);throw new AssertionError("Unbounded line count");}catch(IllegalArgumentException expected){checks++;}
  try{method.invoke(null,"字",200d,6,(ToDoubleFunction<String>)(s->Double.NaN));throw new AssertionError("Invalid native measure accepted");}catch(InvocationTargetException expected){ok(expected.getCause() instanceof IllegalArgumentException,"Unexpected failure type");}
  System.out.println("CJK_LAYOUT_MEASURED_WIDTH_UNICODE_AND_LIMITS_PASS checks="+checks);
 }
}
