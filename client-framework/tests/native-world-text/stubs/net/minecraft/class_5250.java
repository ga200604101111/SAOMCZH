package net.minecraft;
import java.util.*;
public class class_5250 implements class_2561 {
 private final Object content;
 private final List<class_2561> siblings=new ArrayList<>();
 private class_2583 style=class_2583.DEFAULT;
 public class_5250(Object c){content=c;}
 public Object method_10851(){return content;}
 public List<class_2561> method_10855(){return siblings;}
 public class_2583 method_10866(){return style;}
 public class_5250 method_27662(){return new class_5250(content);}
 public class_5250 method_10862(class_2583 s){style=s;return this;}
 public class_5250 method_10852(class_2561 s){siblings.add(s);return this;}
 public String getString(){
  StringBuilder b=new StringBuilder();if(content instanceof class_8828 text)b.append(text.comp_737());
  for(class_2561 child:siblings)b.append(((class_5250)child).getString());return b.toString();
 }
}
