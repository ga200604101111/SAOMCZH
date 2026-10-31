package sao.zh.engine;

import java.util.Map;

/**
 * Translation of independently reviewed hologram lines separated by the SAO server's
 * private-use display marker (U+E001). The marker is not a word, color, or punctuation
 * and must remain byte-for-byte unchanged in the Text sent to Minecraft's renderer.
 *
 * Used ONLY by existing allowlisted world-text display hooks. This is not a general
 * chat/text replacer. Each separated fragment must be an exact reviewed key.
 */
final class WorldTextSegments {
 private WorldTextSegments() {}
 private static final char DISPLAY_SEPARATOR='\uE001';
 private static final int MAX_SEGMENTS=8, MAX_LENGTH=2048;

 static String resolve(Script.Hook h,String raw,Map<String,Map<String,String>>dictionary){
  if(raw==null||!SemanticText.world(h)||raw.length()>MAX_LENGTH
      ||raw.indexOf(DISPLAY_SEPARATOR)<0||raw.indexOf('\u00a7')>=0)return raw;
  int segments=1;
  for(int i=0;i<raw.length();i++)if(raw.charAt(i)==DISPLAY_SEPARATOR)segments++;
  if(segments>MAX_SEGMENTS)return raw;

  StringBuilder result=new StringBuilder(raw.length());
  int start=0;boolean changed=false;
  for(int i=0;i<=raw.length();i++){
   if(i<raw.length()&&raw.charAt(i)!=DISPLAY_SEPARATOR)continue;
   String segment=raw.substring(start,i);
   String replaced=segment(h,segment,dictionary);
   changed|=!segment.equals(replaced);
   result.append(replaced);
   if(i<raw.length())result.append(DISPLAY_SEPARATOR);
   start=i+1;
  }
  return changed?result.toString():raw;
 }

 private static String segment(Script.Hook h,String raw,Map<String,Map<String,String>>data){
  int a=0,b=raw.length();
  while(a<b&&Character.isWhitespace(raw.charAt(a)))a++;
  while(b>a&&Character.isWhitespace(raw.charAt(b-1)))b--;
  if(b<=a||b-a>256)return raw;
  String key=raw.substring(a,b);
  String value=SharedDictionary.lookup(h,key,data);
  if(value==null||value.equals(key)||value.length()>512||value.indexOf(DISPLAY_SEPARATOR)>=0
     ||value.indexOf('\n')>=0||value.indexOf('\u00a7')>=0)return raw;
  return raw.substring(0,a)+value+raw.substring(b);
 }
}
