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
 static String owner(String n){
  if(n==null||n.length()>240||!n.matches("[A-Za-z_$][A-Za-z0-9_$/]*")||!n.contains("/")||n.contains("//"))throw new IllegalArgumentException("Invalid class name");
  for(String prefix:new String[]{"java/","javax/","jdk/","sun/","sao/zh/","org/objectweb/","net/fabricmc/","org/spongepowered/"})if(n.startsWith(prefix))throw new IllegalArgumentException("Protected class namespace");return n;
 }
 static Script parse(byte[]data){
  if(data.length>LIMIT)throw new IllegalArgumentException("Script too large");Map<?,?>root=obj(MiniJson.parse(new String(data,StandardCharsets.UTF_8)));
  if(number(root,"schema_version",0)!=1)throw new IllegalArgumentException("Unsupported script schema");
  String version=str(root,"version");if(!version.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?"))throw new IllegalArgumentException("Bad version");
  int sequence=number(root,"sequence",0);if(sequence<1)throw new IllegalArgumentException("Invalid sequence");
  Object expiry=root.get("expires");if(!(expiry instanceof Number)||((Number)expiry).longValue()<System.currentTimeMillis()/1000)throw new IllegalArgumentException("Script expired");
  if(!(root.get("hooks") instanceof List))throw new IllegalArgumentException("Hooks must be array");List<?>rows=(List<?>)root.get("hooks");if(rows.size()>128)throw new IllegalArgumentException("Too many hooks");
  Map<String,Hook>all=new TreeMap<>();Set<String>sites=new HashSet<>();
  for(Object row:rows){Hook hook=new Hook(obj(row));if(all.put(hook.id,hook)!=null)throw new IllegalArgumentException("Duplicate hook id");if(!sites.add(hook.shape().substring(hook.id.length()+1)))throw new IllegalArgumentException("Ambiguous double hook site");}
  Script next=new Script(version,sequence,all);
  Object samples=root.get("tests");if(samples instanceof List){if(((List<?>)samples).size()>512)throw new IllegalArgumentException("Too many tests");for(Object sample:(List<?>)samples){Map<?,?>s=obj(sample);Hook hook=all.get(str(s,"hook"));if(hook==null||!str(s,"expect").equals(hook.literal(str(s,"input"))))throw new IllegalArgumentException("Script self-test failed");}}
  return next;
 }
 static final class Hook {
  final String id,owner,method,descriptor,kind,memberOwner,memberName,memberDescriptor,locale,section;final int argument,sites;
  final Map<String,String>exact;final List<Operation>ops;final AtomicInteger failures=new AtomicInteger();
  Hook(Map<?,?>m){
   id=str(m,"id");if(!id.matches("[a-zA-Z0-9_.:-]{1,100}"))throw new IllegalArgumentException("Invalid hook id");
   owner=owner(str(m,"class"));method=str(m,"method");if(!method.matches("[A-Za-z_$][A-Za-z0-9_$]*"))throw new IllegalArgumentException("Constructor/initializer hooks are not hot-safe");
   descriptor=str(m,"descriptor");Type[]args;Type ret;
   try{args=Type.getArgumentTypes(descriptor);ret=Type.getReturnType(descriptor);if(!Type.getMethodDescriptor(ret,args).equals(descriptor))throw new IllegalArgumentException();}catch(Exception x){throw new IllegalArgumentException("Invalid method descriptor");}
   kind=str(m,"kind");argument=number(m,"argument",-1);sites=number(m,"sites",1);if(sites<1||sites>32)throw new IllegalArgumentException("Invalid site count");
   memberOwner=m.get("member_class") instanceof String?owner((String)m.get("member_class")):"";
   memberName=m.get("member") instanceof String?(String)m.get("member"):"";memberDescriptor=m.get("member_descriptor") instanceof String?(String)m.get("member_descriptor"):"";
   switch(kind){
    case "return":if(!valueType(ret.getDescriptor()))throw new IllegalArgumentException("Return is not supported text");break;
    case "argument":if(argument<0||argument>=args.length||!valueType(args[argument].getDescriptor()))throw new IllegalArgumentException("Argument is not supported text");break;
    case "field":if(memberOwner.isEmpty()||!memberName.matches("[A-Za-z_$][A-Za-z0-9_$]*")||!valueType(memberDescriptor))throw new IllegalArgumentException("Invalid field projection");break;
    case "call":try{if(memberOwner.isEmpty()||!memberName.matches("[A-Za-z_$][A-Za-z0-9_$]*")||!valueType(Type.getReturnType(memberDescriptor).getDescriptor()))throw new IllegalArgumentException();}catch(Exception x){throw new IllegalArgumentException("Invalid call-result projection");}break;
    default:throw new IllegalArgumentException("Unknown operation kind: "+kind);
   }
   locale=m.get("locale") instanceof String?(String)m.get("locale"):"zh_tw";if(!locale.equals("*")&&!locale.matches("[a-z]{2,3}_[a-z]{2,4}"))throw new IllegalArgumentException("Invalid locale");
   section=m.get("section") instanceof String?(String)m.get("section"):"names";if(!Set.of("names","items","lines","messages").contains(section))throw new IllegalArgumentException("Unknown dictionary section");
   Map<String,String>map=new HashMap<>();Object entries=m.get("exact");if(entries instanceof Map){if(((Map<?,?>)entries).size()>10000)throw new IllegalArgumentException("Too many replacements");for(Map.Entry<?,?>e:((Map<?,?>)entries).entrySet()){
    if(!(e.getKey() instanceof String)||!(e.getValue() instanceof String)||((String)e.getKey()).isEmpty()||((String)e.getKey()).length()>8192||((String)e.getValue()).length()>16384)throw new IllegalArgumentException("Invalid literal replacement");map.put((String)e.getKey(),(String)e.getValue());}}
   exact=Map.copyOf(map);List<Operation>operations=new ArrayList<>();Object a=m.get("operations");if(a instanceof List){if(((List<?>)a).size()>8)throw new IllegalArgumentException("Instruction budget exceeded");for(Object v:(List<?>)a)operations.add(new Operation(obj(v)));}ops=List.copyOf(operations);
  }
  String shape(){return id+'|'+owner+'|'+method+'|'+descriptor+'|'+kind+'|'+argument+'|'+memberOwner+'|'+memberName+'|'+memberDescriptor+'|'+sites;}
  String literal(String s){if(s==null||s.length()>8192)return s;String v=exact.getOrDefault(s,s);for(Operation op:ops){v=op.apply(v);if(v.length()>16384)return s;}return v;}
 }
 static final class Operation {
  final String op,from,to;
  Operation(Map<?,?>m){op=str(m,"op");if(!Set.of("prefix","suffix","replace_literal").contains(op))throw new IllegalArgumentException("Unsupported/unbounded instruction "+op);from=str(m,"from");to=m.get("to") instanceof String?(String)m.get("to"):"";if(from.length()>256||to.length()>256)throw new IllegalArgumentException("Instruction operand too large");}
  String apply(String s){if(s.length()>8192)return s;switch(op){case "prefix":return s.startsWith(from)?to+s.substring(from.length()):s;case "suffix":return s.endsWith(from)?s.substring(0,s.length()-from.length())+to:s;default:int n=0,at=0;while((at=s.indexOf(from,at))>=0){n++;at+=from.length();if((long)s.length()+(long)n*(to.length()-from.length())>16384)return s;}return s.replace(from,to);}}
 }
}
