package fixture;
import java.util.*;import java.nio.file.*;import java.lang.reflect.*;
public final class RefreshContract {
 public static final class Canvas {
  public String font="work_sans";public float scale=1;public int measures;
  public float width(String text,float size,boolean bold){measures++;return (float)text.codePoints().mapToDouble(c->c<128?size*.5:size).sum();}
  public List<String> wrap(String text,float size,boolean bold,float width,int max){return new ArrayList<>(List.of(text));}
 }
 public static final class Screen {
  public final Map<String,String> rowTexts=new HashMap<>(),translated=new HashMap<>();public String descOf="raw";
  public String input="still typing";public int scroll=37;public Object selected=new Object(),dragged=new Object();
  public String source(){return "Quest description";}
  public String dictionary(){return "Dictionary title";}
  public void render(){rowTexts.computeIfAbsent("q",k->source());rowTexts.computeIfAbsent("d",k->dictionary());}
 }
 static int checks;static Object engine;static Path dir;
 static void ok(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 static Object call(String method,Class<?>[]types,Object...a)throws Exception{try{return engine.getClass().getMethod(method,types).invoke(engine,a);}catch(InvocationTargetException ex){throw (Exception)ex.getCause();}}
 static void apply(String n)throws Exception{call("applySigned",new Class<?>[]{byte[].class,byte[].class},Files.readAllBytes(dir.resolve(n+".json")),Files.readAllBytes(dir.resolve(n+".sig")));}
 public static void main(String[]args)throws Exception{
  dir=Path.of(args[0]);Screen s=new Screen();Canvas c=new Canvas();s.render();Object chosen=s.selected,drag=s.dragged;
  ok(s.rowTexts.get("q").equals("Quest description"),"Must start with an already-open unhooked screen");
  engine=Class.forName("sao.zh.boot.Agent").getMethod("runtime").invoke(null);ok(engine!=null,"Kernel failed");
  apply("refresh-a");s.render();ok(s.rowTexts.get("q").equals("任務說明"),"Already-open cached text not invalidated");ok(s.descOf==null,"Description cache key not invalidated");
  s.descOf="q";s.translated.put("sticky","keep");s.render();ok(s.translated.containsKey("sticky")&&s.descOf.equals("q"),"Cache cleared every frame");
  List<String>x=c.wrap("測試中文換行以及拍賣行說明",19,false,76,6);ok(x.size()>1,"Auction still does not wrap");for(String l:x)ok(c.width(l,19,false)<=76,"Width overflow");
  for(int i=0;i<4;i++)ok(c.wrap("中文",19,false,0,6).equals(List.of("中文")),"Transient zero-width must keep original");
  ok(c.wrap("中文中文中文",19,false,38,6).size()==3,"Transient zero-width disabled layout permanently");
  c.wrap("測試中文換行以及拍賣行說明",19,false,76,6);int calls=c.measures;c.wrap("測試中文換行以及拍賣行說明",19,false,76,6);ok(c.measures==calls,"Warm layout cache remeasures every frame");
  c.scale=2;c.wrap("測試中文換行以及拍賣行說明",19,false,76,6);ok(c.measures>calls,"Scale change did not invalidate wrap");
  apply("refresh-b");s.render();ok(s.rowTexts.get("q").equals("新的任務說明"),"Second hot generation not visible");
  Path root=(Path)Class.forName("sao.zh.boot.Agent").getMethod("root").invoke(null);Path dict=root.resolve("SAO_TW_Dynamic/zh_tw.json");Files.createDirectories(dict.getParent());
  Files.writeString(dict,"{\"names\":{\"Dictionary title\":\"詞庫更新\"}}");Method reload=engine.getClass().getDeclaredMethod("reloadDictionary");reload.setAccessible(true);reload.invoke(engine);s.render();ok(s.rowTexts.get("d").equals("詞庫更新"),"Dictionary-only update did not refresh open screen");
  Files.writeString(dict,"{\"names\":{\"Dictionary title\":\"第二次更新\"}}");Files.setLastModifiedTime(dict,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()+5000));reload.invoke(engine);s.render();ok(s.rowTexts.get("d").equals("第二次更新"),"Second dictionary-only revision stale");
  try{apply("refresh-bad");throw new AssertionError("Unknown cache field accepted");}catch(IllegalArgumentException expected){checks++;}
  s.render();ok(s.rowTexts.get("q").equals("新的任務說明"),"Bad generation damaged old output");
  apply("refresh-empty");s.render();ok(s.rowTexts.get("q").equals("Quest description"),"Disabling rules left stale translated cache");
  ok(c.wrap("不應換行",19,false,20,6).size()==1,"Disabled wrap still replaces original");
  call("rollback",new Class<?>[]{});s.render();ok(s.rowTexts.get("q").equals("新的任務說明"),"Rollback failed to refresh existing screen");
  ok(s.input.equals("still typing")&&s.scroll==37&&s.selected==chosen&&s.dragged==drag,"Interaction state disturbed");
  String status=String.valueOf(call("status",new Class<?>[]{}));ok(status.contains("version=1.2.2"),"Wrong live generation "+status);
  System.out.println("LIVE_CACHE_WRAP_REFRESH_REMOVE_ROLLBACK_PASS checks="+checks+" pid="+ProcessHandle.current().pid()+" same_screen=true same_kernel=true");
 }
}
