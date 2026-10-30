package sao.zh.compat16;
import java.util.Map;
public final class SplashPunctuationContract {
 public static void main(String[]args){
  if(!SplashBridge.available())throw new AssertionError("Startup assets unavailable");
  Map<String,String> original=Map.of("foreign","Keep: original");
  if(SplashBridge.translations(original,"en_us")!=original)throw new AssertionError("Unrelated language modified");
  Map<String,String> localized=SplashBridge.translations(Map.of(),"zh_tw");
  if(localized.size()!=53)throw new AssertionError("Startup entries lost");
  for(String value:localized.values()){
   if(value.indexOf('\uff1a')>=0)throw new AssertionError("Fullwidth colon remains in startup translation");
   if(!SplashBridge.supported(value))throw new AssertionError("Normalized startup text has missing glyphs");
  }
  System.out.println("SPLASH_PUNCTUATION_PASS entries=53 atlas_supported=true other_locale_unchanged=true");
 }
}
