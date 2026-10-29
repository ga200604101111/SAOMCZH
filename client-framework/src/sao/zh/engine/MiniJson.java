package sao.zh.engine;
import java.util.*;
/** Small strict JSON codec for local configuration and lossless table exports. */
public final class MiniJson {
 private MiniJson(){}
 public static Object parse(String s){Parser p=new Parser(s);Object v=p.value(0);p.ws();if(p.i!=s.length())throw new IllegalArgumentException("Trailing JSON");return v;}
 public static String stringify(Object o){StringBuilder b=new StringBuilder();write(b,o,0);return b.toString();}
 private static void write(StringBuilder b,Object o,int depth){
  if(depth>64)throw new IllegalArgumentException("JSON depth");
  if(o==null){b.append("null");return;}
  if(o instanceof String){quote(b,(String)o);return;}
  if(o instanceof Boolean||o instanceof Number){b.append(o);return;}
  if(o instanceof Map){b.append('{');boolean sep=false;for(Object obj:((Map<?,?>)o).entrySet()){Map.Entry<?,?> e=(Map.Entry<?,?>)obj;if(!(e.getKey() instanceof String))throw new IllegalArgumentException("JSON key");if(sep)b.append(',');sep=true;quote(b,(String)e.getKey());b.append(':');write(b,e.getValue(),depth+1);}b.append('}');return;}
  if(o instanceof Iterable){b.append('[');boolean sep=false;for(Object v:(Iterable<?>)o){if(sep)b.append(',');sep=true;write(b,v,depth+1);}b.append(']');return;}
  throw new IllegalArgumentException("Unsupported JSON value");
 }
 private static void quote(StringBuilder b,String s){b.append('"');for(int i=0;i<s.length();i++){char c=s.charAt(i);switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;case '\b':b.append("\\b");break;case '\f':b.append("\\f");break;default:if(c<32||Character.isSurrogate(c)){b.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));}else b.append(c);}}b.append('"');}
 private static final class Parser {
  final String s;int i;Parser(String text){if(text==null||text.length()>16*1024*1024)throw new IllegalArgumentException("JSON size");s=text;}
  void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
  Object value(int d){if(d>64)throw err();ws();if(i>=s.length())throw err();char c=s.charAt(i);
   if(c=='"')return string();if(c=='{')return object(d);if(c=='[')return array(d);
   if(s.startsWith("true",i)){i+=4;return true;}if(s.startsWith("false",i)){i+=5;return false;}if(s.startsWith("null",i)){i+=4;return null;}
   int a=i;if(c=='-')i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;if(i<s.length()&&s.charAt(i)=='.'){i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}
   if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}
   String n=s.substring(a,i);if(!n.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))throw err();try{return new java.math.BigDecimal(n);}catch(NumberFormatException e){throw err();}
  }
  Map<String,Object> object(int d){i++;LinkedHashMap<String,Object> m=new LinkedHashMap<>();ws();if(take('}'))return m;do{ws();if(i>=s.length()||s.charAt(i)!='"')throw err();String k=string();ws();if(!take(':'))throw err();Object v=value(d+1);if(m.containsKey(k))throw new IllegalArgumentException("Duplicate JSON key");m.put(k,v);ws();if(take('}'))return m;}while(take(','));throw err();}
  List<Object> array(int d){i++;ArrayList<Object>a=new ArrayList<>();ws();if(take(']'))return a;do{a.add(value(d+1));ws();if(take(']'))return a;}while(take(','));throw err();}
  boolean take(char c){ws();if(i<s.length()&&s.charAt(i)==c){i++;return true;}return false;}
  String string(){i++;StringBuilder b=new StringBuilder();while(i<s.length()){char c=s.charAt(i++);if(c=='"')return b.toString();if(c<32)throw err();if(c!='\\'){b.append(c);continue;}if(i>=s.length())throw err();char e=s.charAt(i++);switch(e){case '"':case '\\':case '/':b.append(e);break;case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'u':if(i+4>s.length())throw err();try{b.append((char)Integer.parseInt(s.substring(i,i+4),16));}catch(NumberFormatException x){throw err();}i+=4;break;default:throw err();}}throw err();}
  IllegalArgumentException err(){return new IllegalArgumentException("Malformed JSON at "+i);}
 }
}
