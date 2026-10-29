package sao.zh.compat16;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Pure measured layout: no network, state mutation, inserted spaces or font shrinking. */
public final class CjkLayout {
 private CjkLayout(){}
 private static final String CLOSE="，。！？、；：）》」』】〕〉］｝〗〙〛…,.!?;:%";
 private static final String OPEN="（《「『【〔〈［｛〖〘〚([{“‘";
 public static boolean containsCjk(String s){
  if(s==null)return false;
  for(int i=0;i<s.length();){int c=s.codePointAt(i);i+=Character.charCount(c);if(cjk(c))return true;}
  return false;
 }
 private static boolean cjk(int c){return LiteralGlyphs.isAlias(c)||(c>=0x2e80&&c<=0xa4cf)||(c>=0xf900&&c<=0xfaff)||(c>=0xff01&&c<=0xff60)||(c>=0x20000&&c<=0x323af)||(c>=0xe900&&c<=0xe902);}
 private static boolean mark(int c){int t=Character.getType(c);return t==Character.NON_SPACING_MARK||t==Character.COMBINING_SPACING_MARK||t==Character.ENCLOSING_MARK||c==0xfe0e||c==0xfe0f||(c>=0x1f3fb&&c<=0x1f3ff);}
 private static int nextUnit(String s,int i){
  int c=s.codePointAt(i);i+=Character.charCount(c);
  if(c==167&&i<s.length())return i+Character.charCount(s.codePointAt(i));
  while(i<s.length()){
   c=s.codePointAt(i);
   if(mark(c)){i+=Character.charCount(c);continue;}
   if(c==0x200d&&i+1<s.length()){i++;i+=Character.charCount(s.codePointAt(i));continue;}
   break;
  }
  return i;
 }
 private static boolean boundary(String s,int end){
  if(end>=s.length())return true;
  int a=s.codePointBefore(end),b=s.codePointAt(end);
  if(CLOSE.indexOf(b)>=0||OPEN.indexOf(a)>=0)return false;
  return Character.isWhitespace(a)||Character.isWhitespace(b)||cjk(a)||cjk(b)||a=='-'||a=='/';
 }
 public static List<String> wrap(String source,double maxWidth,ToDoubleFunction<String> measure){
  Objects.requireNonNull(measure,"measure");
  if(source==null)source="";
  if(Double.isNaN(maxWidth))throw new IllegalArgumentException("NaN width");
  maxWidth=Math.max(0,maxWidth);
  List<String> result=new ArrayList<>();
  for(String paragraph:source.replace("\r\n","\n").replace('\r','\n').split("\n",-1)){
   if(paragraph.isEmpty()){result.add("");continue;}
   if(measure.applyAsDouble(paragraph)<=maxWidth){result.add(paragraph);continue;}
   List<Integer> ends=new ArrayList<>();ends.add(0);
   for(int i=0;i<paragraph.length();){i=nextUnit(paragraph,i);ends.add(i);}
   int start=0;
   while(start<ends.size()-1){
    int low=start+1,high=ends.size()-1,fit=start;
    while(low<=high){int mid=(low+high)>>>1;double width=measure.applyAsDouble(paragraph.substring(ends.get(start),ends.get(mid)));
     if(!Double.isFinite(width)||width<0)throw new IllegalArgumentException("Invalid measured width");
     if(width<=maxWidth+0.0001){fit=mid;low=mid+1;}else high=mid-1;
    }
    if(fit==start)fit=start+1; // At least one whole glyph/cluster: bounded progress in a tiny box.
    int cut=fit;
    if(fit<ends.size()-1){
     for(int j=fit;j>start;j--)if(boundary(paragraph,ends.get(j))){cut=j;break;}
    }
    result.add(paragraph.substring(ends.get(start),ends.get(cut)));start=cut;
   }
  }
  return Collections.unmodifiableList(result);
 }
}
