package sao.zh.engine;
import java.text.BreakIterator;
import java.util.*;
import java.util.regex.*;
import java.util.function.ToDoubleFunction;
/** Pure bounded layout. Width comes from the active renderer, never guessed from character count. */
public final class CjkWrap {
 private CjkWrap(){}
 public static List<String> wrap(String text,double width,int lines,ToDoubleFunction<String> metric){
  if(text==null||text.length()>8192||!Double.isFinite(width)||width<=0||width>1000000||lines<1||lines>64||metric==null)throw new IllegalArgumentException("Layout budget/width invalid");
  if(text.isEmpty())return List.of();
  int[]budget={2048};ToDoubleFunction<String> measure=s->{if(--budget[0]<0)throw new IllegalArgumentException("Measurement budget exceeded");double n=metric.applyAsDouble(s);if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid renderer width");return n;};
  List<String>out=new ArrayList<>();String normalized=text.replace("\r\n","\n").replace('\r','\n');
  String[]paragraphs=normalized.split("\n",-1);boolean truncated=false;
  outer:for(String p:paragraphs){
   if(out.size()>=lines){truncated=true;break;}
   if(p.isEmpty()){out.add("");continue;}
   int[]bounds=clusters(p);BreakIterator breaks=BreakIterator.getLineInstance(Locale.TRADITIONAL_CHINESE);String readable=LiteralGlyphBridge.readable(p);breaks.setText(readable.length()==p.length()?readable:p);BitSet legal=new BitSet(p.length()+1);for(int j=breaks.first();j!=BreakIterator.DONE;j=breaks.next())legal.set(j);
   protectTokens(readable.length()==p.length()?readable:p,legal,width,measure);
   int at=0;
   while(at<bounds.length-1){
    if(out.size()>=lines){truncated=true;break outer;}
    int lo=at+1,hi=bounds.length-1,fit=at;
    while(lo<=hi){int mid=(lo+hi)>>>1;String part=p.substring(bounds[at],bounds[mid]).stripTrailing();if(measure.applyAsDouble(part)<=width){fit=mid;lo=mid+1;}else hi=mid-1;}
    if(fit==at)throw new IllegalArgumentException("Single glyph wider than target");
    int end=fit;
    if(fit<bounds.length-1){while(end>at&&!legal.get(bounds[end]))end--;if(end==at)end=fit;}
    // Improve only the final two rows of a CJK paragraph. Never join explicit lines,
    // add a row, change the font size or consume the caller's final truncation row.
    if(fit<bounds.length-1&&out.size()+1<lines&&hasHan(readable)&&p.indexOf('§')<0
       &&measure.applyAsDouble(p.substring(bounds[fit]).strip())<=width){
     int candidate=naturalEnd(p,readable,bounds,legal,at,end,width,measure);
     if(candidate>at)end=candidate;
    }
    String row=p.substring(bounds[at],bounds[end]).stripTrailing();
    if(measure.applyAsDouble(row)>width)throw new IllegalArgumentException("Non-monotonic font metric");
    out.add(row);at=end;while(at<bounds.length-1&&Character.isWhitespace(p.codePointAt(bounds[at])))at++;
   }
  }
  if(truncated&&!out.isEmpty()){
   int i=out.size()-1;String row=out.get(i);int[]b=clusters(row);int n=b.length-1;
   if(measure.applyAsDouble("…")>width)throw new IllegalArgumentException("Ellipsis wider than target");
   while(n>0&&measure.applyAsDouble(row.substring(0,b[n]).stripTrailing()+"…")>width)n--;
   out.set(i,row.substring(0,b[n]).stripTrailing()+"…");
  }
  return List.copyOf(out);
 }
 private static final Pattern UNITS=Pattern.compile("(?i)(?:Lv[.]?[ \t]*[0-9]{1,6}[+]?|[+-]?[0-9]+(?:[.,][0-9]+)?(?:/[0-9]+)?(?:%|°[CF]|/s|km|m|XP|秒|點)?)");
 private static boolean hasHan(String text){return text.codePoints().anyMatch(c->Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN);}
 private static void protectTokens(String text,BitSet legal,double width,ToDoubleFunction<String> metric){
  Matcher m=UNITS.matcher(text);while(m.find())if(m.end()-m.start()<=64&&metric.applyAsDouble(m.group())<=width)legal.clear(m.start()+1,m.end());
  for(int p=1;p<text.length();p++){
   int next=text.codePointAt(p),prev=text.codePointBefore(p);
   if("，。！？；：、）》】」』…％".indexOf(next)>=0||"（《【「『".indexOf(prev)>=0)legal.clear(p);
  }
 }
 private static int naturalEnd(String p,String readable,int[]b,BitSet legal,int start,int end,double width,ToDoubleFunction<String>metric){
  String visible=readable.length()==p.length()?readable:p;
  int lowest=Math.max(start+1,end-16),best=end;
  // A nearby clause end is preferred only when both resulting rows still fit.
  for(int n=end;n>=lowest;n--){
   if(!legal.get(b[n]))continue;String left=p.substring(b[start],b[n]).stripTrailing(),right=p.substring(b[n]).strip();
   double a=metric.applyAsDouble(left),z=metric.applyAsDouble(right);if(a<width*.7)break;
   int at=b[n];while(at>b[start]&&Character.isWhitespace(visible.charAt(at-1)))at--;
   if(at>b[start]&&"，。；！？、".indexOf(visible.codePointBefore(at))>=0&&z<=width&&z>=width*.25){best=n;break;}
  }
  double tail=metric.applyAsDouble(p.substring(b[best]).strip());
  if(tail>0&&tail<width*.25){
   for(int n=best-1;n>=lowest;n--){
    if(!legal.get(b[n]))continue;double a=metric.applyAsDouble(p.substring(b[start],b[n]).stripTrailing()),z=metric.applyAsDouble(p.substring(b[n]).strip());
    if(a<width*.45||z>width)break;
    if(z>=width*.30){best=n;break;}
   }
  }
  return best;
 }
 static int[]clusters(String s){
  ArrayList<Integer>b=new ArrayList<>();b.add(0);int pos=0;
  while(pos<s.length()){
   int cp=s.codePointAt(pos);pos+=Character.charCount(cp);
   if(cp==0x00a7&&pos<s.length())pos+=Character.charCount(s.codePointAt(pos));
   if(cp>=0x1f1e6&&cp<=0x1f1ff&&pos<s.length()){int q=s.codePointAt(pos);if(q>=0x1f1e6&&q<=0x1f1ff)pos+=Character.charCount(q);}
   boolean again=true;while(again&&pos<s.length()){
    again=false;int q=s.codePointAt(pos),type=Character.getType(q);
    if(type==Character.NON_SPACING_MARK||type==Character.COMBINING_SPACING_MARK||type==Character.ENCLOSING_MARK||q==0xfe0f||q==0xfe0e||q>=0x1f3fb&&q<=0x1f3ff){pos+=Character.charCount(q);again=true;}
    else if(q==0x200d&&pos+1<s.length()){pos++;pos+=Character.charCount(s.codePointAt(pos));again=true;}
   }
   b.add(pos);
  }
  return b.stream().mapToInt(Integer::intValue).toArray();
 }
}
