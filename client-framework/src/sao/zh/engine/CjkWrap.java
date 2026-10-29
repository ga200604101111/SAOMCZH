package sao.zh.engine;
import java.text.BreakIterator;
import java.util.*;
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
   int at=0;
   while(at<bounds.length-1){
    if(out.size()>=lines){truncated=true;break outer;}
    int lo=at+1,hi=bounds.length-1,fit=at;
    while(lo<=hi){int mid=(lo+hi)>>>1;String part=p.substring(bounds[at],bounds[mid]).stripTrailing();if(measure.applyAsDouble(part)<=width){fit=mid;lo=mid+1;}else hi=mid-1;}
    if(fit==at)throw new IllegalArgumentException("Single glyph wider than target");
    int end=fit;
    if(fit<bounds.length-1){while(end>at&&!legal.get(bounds[end]))end--;if(end==at)end=fit;}
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
