package sao.zh.engine;
import java.util.*;import java.nio.charset.StandardCharsets;import java.util.concurrent.atomic.AtomicInteger;import org.objectweb.asm.Type;
/** A bounded declarative language: no loops, filesystem, reflection or arbitrary Java calls. */
final class Script {
 static final int LIMIT=2*1024*1024;
 static final String TEXT="Lnet/minecraft/class_2561;",STR="Ljava/lang/String;";
 final String version;final long sequence;final Map<String,Hook>byId;final Map<String,List<Hook>>byClass;final String shape;
 Script(String v,long sequence,Map<String,Hook>h){
  version=v;this.sequence=sequence;byId=Collections.unmodifiableMap(h);Map<String,List<Hook>>index=new HashMap<>();
  StringBuilder shape=new StringBuilder();for(Hook x:h.values()){index.computeIfAbsent(x.owner,k->new ArrayList<>()).add(x);shape.append(x.shape()).append('\n');}
  Map<String,List<Hook>>frozen=new HashMap<>();index.forEach((k,vv)->frozen.put(k,List.copyOf(vv)));byClass=Map.copyOf(frozen);this.shape=shape.toString();
 }
 static Script empty(){return new Script("0.0.0",0,new TreeMap<>());}
 static Map<?,?> obj(Object o){if(!(o instanceof Map))throw new IllegalArgumentException("Expected object");return (Map<?,?>)o;}
 static String str(Map<?,?>m,String k){Object o=m.get(k);if(!(o instanceof String)||((String)o).isEmpty())throw new IllegalArgumentException("Missing string "+k);return (String)o;}
 static int number(Map<?,?>m,String k,int def){Object o=m.get(k);if(o==null)return def;if(!(o instanceof Number)||((Number)o).doubleValue()!=((Number)o).intValue())throw new IllegalArgumentException("Bad integer "+k);return ((Number)o).intValue();}
 static boolean valueType(String desc){return STR.equals(desc)||TEXT.equals(desc);}
 /** Single approved StringVisitable interface argument in Zap Dialogue's pure UI methods. */
 static boolean dialogueVisitable(String id,String owner,String method,String desc,int argument){
  if(argument!=0||!owner.equals("fr/zapdialogue/client/text/StyledText"))return false;
  return switch(id){
   case "npc-dialogue:text-wrap"->method.equals("wrap")&&desc.equals("(Lnet/minecraft/class_5348;ILnet/minecraft/class_327;)Ljava/util/List;");
   case "npc-dialogue:text-trim"->method.equals("trim")&&desc.equals("(Lnet/minecraft/class_5348;I)Lnet/minecraft/class_5250;");
   case "npc-dialogue:text-length"->method.equals("length")&&desc.equals("(Lnet/minecraft/class_5348;)I");
   default->false;
  };
 }

 static String owner(String n){
  if(n==null||n.length()>240||!n.matches("[A-Za-z_$][A-Za-z0-9_$/]*")||!n.contains("/")||n.contains("//"))throw new IllegalArgumentException("Invalid class name");
  for(String prefix:new String[]{"java/","javax/","jdk/","sun/","sao/zh/","org/objectweb/","net/fabricmc/","org/spongepowered/"})if(n.startsWith(prefix))throw new IllegalArgumentException("Protected class namespace");return n;
 }
 static Script parse(byte[]data){return parse(data,true);}
 /** Only after signature/hash verification of already installed local rules. */
 static Script parseInstalled(byte[]data){return parse(data,false);}
 private static Script parse(byte[]data,boolean requireFresh){
  if(data.length>LIMIT)throw new IllegalArgumentException("Script too large");Map<?,?>root=obj(MiniJson.parse(new String(data,StandardCharsets.UTF_8)));
  int schema=number(root,"schema_version",0);if(schema!=1&&schema!=2)throw new IllegalArgumentException("Unsupported script schema");
  String version=str(root,"version");if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?"))throw new IllegalArgumentException("Bad version");
  int sequence=number(root,"sequence",0);if(sequence<1)throw new IllegalArgumentException("Invalid sequence");
  Object expiry=root.get("expires");if(!(expiry instanceof Number)||(requireFresh&&((Number)expiry).longValue()<System.currentTimeMillis()/1000))throw new IllegalArgumentException("Script expired");
  if(!(root.get("hooks") instanceof List))throw new IllegalArgumentException("Hooks must be array");List<?>rows=(List<?>)root.get("hooks");if(rows.size()>160)throw new IllegalArgumentException("Too many hooks");
  Map<String,Hook>all=new TreeMap<>();Set<String>sites=new HashSet<>();
  for(Object row:rows){Hook hook=new Hook(obj(row));if(schema==1&&!hook.capture.isEmpty())throw new IllegalArgumentException("Capture requires schema 2");if(schema==1&&(hook.kind.equals("wrap")||hook.kind.equals("refresh")))throw new IllegalArgumentException("Layout capabilities require schema 2");if(all.put(hook.id,hook)!=null)throw new IllegalArgumentException("Duplicate hook id");if(!sites.add(hook.shape().substring(hook.id.length()+1)))throw new IllegalArgumentException("Ambiguous double hook site");}
  Script next=new Script(version,sequence,all);
  Object samples=root.get("tests");if(samples instanceof List){if(((List<?>)samples).size()>512)throw new IllegalArgumentException("Too many tests");for(Object sample:(List<?>)samples){Map<?,?>s=obj(sample);Hook hook=all.get(str(s,"hook"));if(hook==null||!str(s,"expect").equals(hook.literal(str(s,"input"))))throw new IllegalArgumentException("Script self-test failed");}}
  return next;
 }
 static final class Hook {
  final String id,owner,method,descriptor,kind,memberOwner,memberName,memberDescriptor,locale,section,capture;final int argument,sites;
  final Map<String,String>exact;final List<Operation>ops;final AtomicInteger failures=new AtomicInteger();
  final String measure;final Map<String,String>contextFields;final List<CacheField>cacheFields;
  Hook(Map<?,?>m){
   id=str(m,"id");if(!id.matches("[a-zA-Z0-9_.:-]{1,100}"))throw new IllegalArgumentException("Invalid hook id");
   owner=owner(str(m,"class"));method=str(m,"method");if(!method.matches("[A-Za-z_$][A-Za-z0-9_$]*"))throw new IllegalArgumentException("Constructor/initializer hooks are not hot-safe");
   descriptor=str(m,"descriptor");Type[]args;Type ret;
   try{args=Type.getArgumentTypes(descriptor);ret=Type.getReturnType(descriptor);if(!Type.getMethodDescriptor(ret,args).equals(descriptor))throw new IllegalArgumentException();}catch(Exception x){throw new IllegalArgumentException("Invalid method descriptor");}
   kind=str(m,"kind");argument=number(m,"argument",-1);sites=number(m,"sites",1);if(sites<1||sites>32)throw new IllegalArgumentException("Invalid site count");
   memberOwner=m.get("member_class") instanceof String?owner((String)m.get("member_class")):"";
   memberName=m.get("member") instanceof String?(String)m.get("member"):"";memberDescriptor=m.get("member_descriptor") instanceof String?(String)m.get("member_descriptor"):"";
   measure=m.get("measure") instanceof String?(String)m.get("measure"):"";
   Map<String,String>context=new TreeMap<>();Object rawContext=m.get("context_fields");
   if(rawContext!=null){Map<?,?>cm=obj(rawContext);if(cm.size()>4)throw new IllegalArgumentException("Context budget");for(Map.Entry<?,?>e:cm.entrySet()){if(!(e.getKey() instanceof String)||!((String)e.getKey()).matches("[A-Za-z_$][A-Za-z0-9_$]*")||!Set.of(STR,"F","I","Z").contains(e.getValue()))throw new IllegalArgumentException("Invalid layout context field");context.put((String)e.getKey(),(String)e.getValue());}}
   contextFields=Collections.unmodifiableMap(context);List<CacheField>fields=new ArrayList<>();Object rawFields=m.get("cache_fields");
   if(rawFields!=null){if(!(rawFields instanceof List)||((List<?>)rawFields).size()>8)throw new IllegalArgumentException("Cache field budget");Set<String>seen=new HashSet<>();for(Object x:(List<?>)rawFields){CacheField f=new CacheField(obj(x));if(!seen.add(f.name))throw new IllegalArgumentException("Duplicate cache field");fields.add(f);}}
   cacheFields=List.copyOf(fields);
   // An explicitly allowlisted vanilla TextDisplay layout cache, not general entity state.
   boolean displayLines=owner.equals("net/minecraft/class_8113$class_8123")
      &&method.equals("method_48905")
      &&descriptor.equals("(Lnet/minecraft/class_8113$class_8123$class_8127;)Lnet/minecraft/class_8113$class_8123$class_8125;");
   for(CacheField f:cacheFields)if(f.action.equals("reset_display_lines")
      &&(!displayLines||!f.name.equals("field_42442")))throw new IllegalArgumentException("World display cache target is not allowlisted");
   if(displayLines&&kind.equals("refresh")&&(cacheFields.size()!=1
      ||!cacheFields.get(0).action.equals("reset_display_lines")))throw new IllegalArgumentException("TextDisplay refresh must only reset its own line cache");
   switch(kind){
    case "wrap":if(!descriptor.equals("(Ljava/lang/String;FZFI)Ljava/util/List;")||!measure.matches("[A-Za-z_$][A-Za-z0-9_$]*")||!cacheFields.isEmpty())throw new IllegalArgumentException("Invalid bounded wrap contract");break;
    case "refresh":if((ret.getSort()!=Type.VOID&&!displayLines)||cacheFields.isEmpty()||!contextFields.isEmpty())throw new IllegalArgumentException("Invalid display refresh contract");break;
    case "armorstand_nameplate":
     if(!owner.equals("net/minecraft/class_877")||!method.equals("method_62375")
       ||!descriptor.equals("(Lnet/minecraft/class_1531;Lnet/minecraft/class_9998;F)V"))
      throw new IllegalArgumentException("Not a dedicated ArmorStand nameplate render state");
     break;
    case "mob_name":
     if(!id.equals("codex-creatures:mob-shown-name")||!owner.equals("fr/sao/saoui/mob/MobBarRenderer")||!method.equals("shownName")||!descriptor.equals("(Lfr/sao/saoui/mob/MobBarEntry;)Ljava/lang/String;"))throw new IllegalArgumentException("Not a guarded mob display source");break;
    case "system_text":
     if(!id.equals("dynamic-translation:game-system-display")||!owner.equals("net/minecraft/class_7594")||!method.equals("method_44736")||!descriptor.equals("(Lnet/minecraft/class_2561;Z)V"))throw new IllegalArgumentException("Not a game-system display callsite");break;
    case "description_list":
     if(!id.equals("items-economy:tooltip-description-paragraphs")||!owner.equals("fr/sao/tooltip/TipModel")||!method.equals("description")||!descriptor.equals("()Ljava/util/List;")||!"items".equals(m.get("section")))throw new IllegalArgumentException("Not a dedicated description-only list source");break;
    case "web_extract":
     if(!id.equals("zapaudio-webui:extracted-html")
       ||!owner.equals("fr/saofr/zapaudio/client/web/WebClientExtractor")
       ||!method.equals("extract")||!descriptor.equals("()Ljava/nio/file/Path;")
       ||argument!=-1||sites!=1||!memberOwner.isEmpty()||!memberName.isEmpty()
       ||!memberDescriptor.isEmpty()||!measure.isEmpty()||!contextFields.isEmpty()
       ||!cacheFields.isEmpty()||!"lines".equals(m.get("section")))
      throw new IllegalArgumentException("Only Sound of Aincrad HTML extraction can return a Path");
     break;
    case "return":if(!valueType(ret.getDescriptor()))throw new IllegalArgumentException("Return is not supported text");break;
    case "argument":if(argument<0||argument>=args.length||!(valueType(args[argument].getDescriptor())||
       args[argument].getDescriptor().equals("Lnet/minecraft/class_5348;")&&dialogueVisitable(id,owner,method,descriptor,argument)))
       throw new IllegalArgumentException("Argument is not supported text");break;
    case "field":if(memberOwner.isEmpty()||!memberName.matches("[A-Za-z_$][A-Za-z0-9_$]*")||!valueType(memberDescriptor))throw new IllegalArgumentException("Invalid field projection");break;
    case "call":try{if(memberOwner.isEmpty()||!memberName.matches("[A-Za-z_$][A-Za-z0-9_$]*")||!valueType(Type.getReturnType(memberDescriptor).getDescriptor()))throw new IllegalArgumentException();}catch(Exception x){throw new IllegalArgumentException("Invalid call-result projection");}break;
    default:throw new IllegalArgumentException("Unknown operation kind: "+kind);
   }
   locale=m.get("locale") instanceof String?(String)m.get("locale"):"zh_tw";if(!locale.equals("*")&&!locale.matches("[a-z]{2,3}_[a-z]{2,4}"))throw new IllegalArgumentException("Invalid locale");
   section=m.get("section") instanceof String?(String)m.get("section"):"names";if(!Set.of("names","items","lines","messages").contains(section))throw new IllegalArgumentException("Unknown dictionary section");if(kind.equals("armorstand_nameplate")&&!section.equals("lines"))throw new IllegalArgumentException("ArmorStand labels require lines dictionary");
   if(kind.equals("mob_name")&&!section.equals("names")||kind.equals("system_text")&&!section.equals("messages"))throw new IllegalArgumentException("Semantic source dictionary mismatch");
   Object captureInput=m.get("capture");capture=captureInput==null?"":captureInput instanceof String?(String)captureInput:"!invalid!";
   if(kind.equals("web_extract")&&!capture.isEmpty())throw new IllegalArgumentException("Web extraction cannot capture");
   // Minecraft BossBarHud: after the native client-side name GETTER but before
   // TextRenderer measurement and drawing. Never mutate stored BossBar, packets
   // or arbitrary entities. The target name getter occurs exactly once.
   if(id.equals("worlds-locations:vanilla-bossbar-title")
     ||owner.equals("net/minecraft/class_337")&&method.equals("method_1796")){
    if(!id.equals("worlds-locations:vanilla-bossbar-title")
      ||!owner.equals("net/minecraft/class_337")||!method.equals("method_1796")
      ||!descriptor.equals("(Lnet/minecraft/class_332;)V")||!kind.equals("call")
      ||!memberOwner.equals("net/minecraft/class_345")||!memberName.equals("method_5414")
      ||!memberDescriptor.equals("()Lnet/minecraft/class_2561;")
      ||!section.equals("names")||sites!=1||argument!=-1||!locale.equals("zh_tw")
      ||!capture.isEmpty()||!measure.isEmpty()||!contextFields.isEmpty()
      ||!cacheFields.isEmpty())
     throw new IllegalArgumentException("BossBar translation may only project the single rendered name");
   }
   if(id.equals("named-container:visible-title") || owner.equals("net/minecraft/class_465")&&method.equals("method_2388")){
    if(!id.equals("named-container:visible-title")||!owner.equals("net/minecraft/class_465")
     ||!method.equals("method_2388")||!descriptor.equals("(Lnet/minecraft/class_332;II)V")
     ||!kind.equals("field")||!memberOwner.equals("net/minecraft/class_465")
     ||!memberName.equals("field_22785")||!memberDescriptor.equals(TEXT)
     ||sites!=1||argument!=-1||!section.equals("names")||!locale.equals("zh_tw")
     ||!capture.isEmpty()||!measure.isEmpty()||!contextFields.isEmpty()||!cacheFields.isEmpty())
     throw new IllegalArgumentException("Only whitelisted HandledScreen visual title is allowed");
   }
   if(id.equals("dynamic-translation:game-actionbar-display")
     ||owner.equals("net/minecraft/class_329")&&method.equals("method_1758")){
    if(!id.equals("dynamic-translation:game-actionbar-display")||!owner.equals("net/minecraft/class_329")
     ||!method.equals("method_1758")||!descriptor.equals("(Lnet/minecraft/class_2561;Z)V")
     ||!kind.equals("argument")||argument!=0||sites!=1||!section.equals("messages")
     ||!locale.equals("zh_tw")||!capture.isEmpty()||!memberOwner.isEmpty()
     ||!memberName.isEmpty()||!memberDescriptor.isEmpty()||!measure.isEmpty()
     ||!contextFields.isEmpty()||!cacheFields.isEmpty())
     throw new IllegalArgumentException("Only client Actionbar Text display is allowed");
   }

   // The only legal Zap Dialogue hook targets are its four audited UI-only text entries.
   if(id.startsWith("npc-dialogue:")||owner.startsWith("fr/zapdialogue/")){
    boolean speaker=id.equals("npc-dialogue:speaker")
     &&owner.equals("fr/zapdialogue/client/gui/BubbleRenderer")
     &&method.equals("drawSpeakerName")
     &&descriptor.equals("(Lnet/minecraft/class_332;Lfr/zapdialogue/client/gui/BubbleRenderer$Bubble;Lnet/minecraft/class_2561;Lnet/minecraft/class_327;)V")
     &&argument==2&&section.equals("names")&&(capture.isEmpty()||capture.equals("npc_name"));
    boolean dialogue=dialogueVisitable(id,owner,method,descriptor,argument)
     &&section.equals("lines")&&(capture.isEmpty()||id.equals("npc-dialogue:text-wrap")&&capture.equals("dialogue"));
    if(!kind.equals("argument")||!(speaker||dialogue)||sites!=1
      ||!locale.equals("zh_tw")||!memberOwner.isEmpty()||!memberName.isEmpty()
      ||!memberDescriptor.isEmpty()||!measure.isEmpty()||!contextFields.isEmpty()||!cacheFields.isEmpty())
     throw new IllegalArgumentException("Only audited Zap Dialogue display arguments are allowed");
   }

   if(!capture.isEmpty()&&!UnknownCollector.permitted(owner,kind,capture))throw new IllegalArgumentException("Capture is not approved for this owner/kind/origin");
   Map<String,String>map=new HashMap<>();Object entries=m.get("exact");if(entries instanceof Map){if(((Map<?,?>)entries).size()>10000)throw new IllegalArgumentException("Too many replacements");for(Map.Entry<?,?>e:((Map<?,?>)entries).entrySet()){
    if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String)||((String)e.getKey()).isEmpty()||((String)e.getKey()).length()>8192||((String)e.getValue()).length()>16384)throw new IllegalArgumentException("Invalid literal replacement");map.put((String)e.getKey(),(String)e.getValue());}}
   exact=Map.copyOf(map);List<Operation>operations=new ArrayList<>();Object a=m.get("operations");if(a instanceof List){if(((List<?>)a).size()>8)throw new IllegalArgumentException("Instruction budget exceeded");for(Object v:(List<?>)a)operations.add(new Operation(obj(v)));}ops=List.copyOf(operations);
  }
  String shape(){return id+'|'+owner+'|'+method+'|'+descriptor+'|'+kind+'|'+argument+'|'+memberOwner+'|'+memberName+'|'+memberDescriptor+'|'+sites+'|'+measure+'|'+contextFields+'|'+cacheFields+'|'+capture;}
  String literal(String s){if(s==null||s.length()>8192)return s;String v=exact.getOrDefault(s,s);for(Operation op:ops){v=op.apply(v);if(v.length()>16384)return s;}return v;}
 }
 static final class CacheField {
  final String name,descriptor,action;
  CacheField(Map<?,?>m){name=str(m,"name");descriptor=str(m,"descriptor");action=str(m,"action");if(!name.matches("[A-Za-z_$][A-Za-z0-9_$]*")||!(action.equals("clear_map")&&descriptor.equals("Ljava/util/Map;")||action.equals("reset_key")&&descriptor.equals(STR)||action.equals("reset_display_lines")&&descriptor.equals("Lnet/minecraft/class_8113$class_8123$class_8125;")))throw new IllegalArgumentException("Only explicit display Map caches or String cache keys can be invalidated");}
  public String toString(){return name+":"+descriptor+":"+action;}
 }
 static final class Operation {
  final String op,from,to;
  Operation(Map<?,?>m){op=str(m,"op");if(!Set.of("prefix","suffix","replace_literal").contains(op))throw new IllegalArgumentException("Unsupported/unbounded instruction "+op);from=str(m,"from");to=m.get("to") instanceof String?(String)m.get("to"):"";if(from.length()>256||to.length()>256)throw new IllegalArgumentException("Instruction operand too large");}
  String apply(String s){if(s.length()>8192)return s;switch(op){case "prefix":return s.startsWith(from)?to+s.substring(from.length()):s;case "suffix":return s.endsWith(from)?s.substring(0,s.length()-from.length())+to:s;default:int n=0,at=0;while((at=s.indexOf(from,at))>=0){n++;at+=from.length();if((long)s.length()+(long)n*(to.length()-from.length())>16384)return s;}return s.replace(from,to);}}
 }
}
