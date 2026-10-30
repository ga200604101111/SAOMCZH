package net.minecraft;
import java.util.*;
public class class_5250 implements class_2561 {
 private final String text;private final List<class_2561> children=new ArrayList<>();
 private class_2583 style=class_2583.field_24360;
 public class_5250(String s){text=s;}
 public class_5250 method_10862(class_2583 s){style=s;return this;}
 public class_5250 method_10852(class_2561 s){children.add(s);return this;}
 public List<class_2561> method_10855(){return children;}
 public class_2583 method_10866(){return style;}
 public String getString(){StringBuilder b=new StringBuilder(text);for(var c:children)b.append(c.getString());return b.toString();}
 public <T> Optional<T> method_27658(class_5348.class_5246<T> visitor,class_2583 parent){
  Optional<T> result=visitor.accept(style,text);if(result.isPresent())return result;
  for(var c:children){result=c.method_27658(visitor,style);if(result.isPresent())return result;}return Optional.empty();
 }
}
