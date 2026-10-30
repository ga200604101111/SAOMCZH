package sao.zh.engine;
import java.util.*;import java.util.regex.*;
/** Chinese display-field punctuation only. Source keys, times, ids and player chat are not normalized. */
final class FieldTypography {
 private FieldTypography(){}
 private static final Pattern FORMAT=Pattern.compile("(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])");
 private static boolean space(char c){return c==' '||c=='\t'||c=='\u00a0'||c=='\u3000';}
 static String normalize(String raw){
  if(raw==null||raw.length()>8192||raw.indexOf(':')<0&&raw.indexOf('：')<0)return raw;
  StringBuilder plain=new StringBuilder(raw.length());int[]start=new int[raw.length()],end=new int[raw.length()];Matcher code=FORMAT.matcher(raw);
  for(int i=0;i<raw.length();){code.region(i,raw.length());if(code.lookingAt()){i=code.end();continue;}int n=plain.length();start[n]=i;plain.append(raw.charAt(i++));end[n]=i;}
  String s=plain.toString(),result=raw;
  for(int at=s.length()-1;at>=0;at--){
   char c=s.charAt(at);if(c!=':'&&c!='：')continue;
   int before=at-1,after=at+1;while(before>=0&&space(s.charAt(before)))before--;while(after<s.length()&&space(s.charAt(after)))after++;
   if(before<0||Character.UnicodeScript.of(s.codePointBefore(before+1))!=Character.UnicodeScript.HAN)continue;
   int from=end[before],to=after<s.length()?start[after]:raw.length();String between=raw.substring(from,to);StringBuilder rebuilt=new StringBuilder(between.length()+1);Matcher codes=FORMAT.matcher(between);
   for(int i=0;i<between.length();){codes.region(i,between.length());if(codes.lookingAt()){rebuilt.append(between,i,codes.end());i=codes.end();continue;}char ch=between.charAt(i++);if(space(ch))continue;rebuilt.append(ch=='：'?':':ch);}
   rebuilt.append(' ');String next=rebuilt.toString();if(!next.equals(between))result=result.substring(0,from)+next+result.substring(to);
  }
  return result;
 }
}
