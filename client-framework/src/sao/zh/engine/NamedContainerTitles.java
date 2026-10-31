package sao.zh.engine;
import java.util.*;
/**
 * Approved server-authored HandledScreen titles only.
 * Never translates arbitrary player-named chests, inventory slot content, or game state.
 */
final class NamedContainerTitles {
 private NamedContainerTitles(){}
 private static final Set<String> TRUSTED=Set.of(
  "Forge - Categories","Forge – Categories","Forge — Categories","Forge Categories",
  "Anvil - Place item","Anvil - Done"
 );
 static boolean hook(Script.Hook h){
  return h.id.equals("named-container:visible-title")
   &&h.owner.equals("net/minecraft/class_465")
   &&h.method.equals("method_2388");
 }
 static String resolve(String raw,Map<String,Map<String,String>> dictionary){
  if(raw==null||raw.length()>180||raw.indexOf('\n')>=0||raw.indexOf('§')>=0)return raw;
  String stripped=raw.strip();
  if(!TRUSTED.contains(stripped))return raw;
  // Reuse the one reviewed entry already owned by items whenever a known
  // server-owned title also appears in item lore/hologram UI.
  String translated=dictionary.getOrDefault("names",Map.of()).get(stripped);
  if(translated==null)translated=dictionary.getOrDefault("items",Map.of()).get(stripped);
  if(translated==null||translated.length()>120||translated.equals(stripped))return raw;
  int left=raw.indexOf(stripped),right=left+stripped.length();
  return raw.substring(0,left)+translated+raw.substring(right);
 }
}
