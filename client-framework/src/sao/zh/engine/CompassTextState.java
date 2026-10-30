package sao.zh.engine;
import java.lang.reflect.*;
/** Only invalidates the compass's derived display-source cache when the dictionary epoch changes. */
final class CompassTextState {
 private CompassTextState(){}
 private static final class State {
  Field label,detail,language;long epoch=Long.MIN_VALUE;boolean ready;
  State(Class<?>owner){
   if(!owner.getName().equals("fr.sao.questtracker.hud.QuestCompassRenderer"))return;
   try{
    label=owner.getDeclaredField("infoLabelSource");detail=owner.getDeclaredField("infoDetailSource");language=owner.getDeclaredField("infoLanguage");
    for(Field f:new Field[]{label,detail,language}){if(!Modifier.isStatic(f.getModifiers())||Modifier.isFinal(f.getModifiers()))return;f.setAccessible(true);}
    ready=label.getType().getName().equals("net.minecraft.class_2561")&&detail.getType()==label.getType()&&language.getType().getName().equals("net.minecraft.class_2477");
   }catch(ReflectiveOperationException ignored){ready=false;}
  }
 }
 private static final ClassValue<State> STATES=new ClassValue<>(){protected State computeValue(Class<?>owner){return new State(owner);}};
 static void refresh(Class<?>owner,long generation)throws ReflectiveOperationException {
  if(owner==null)return;State state=STATES.get(owner);
  synchronized(state){if(!state.ready||state.epoch==generation)return;state.label.set(null,null);state.detail.set(null,null);state.language.set(null,null);state.epoch=generation;}
 }
}
