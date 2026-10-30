package net.minecraft;
import java.util.Objects;
public final class class_2583 {
 public static final class_2583 field_24360=new class_2583(0xffffff,"default");
 public final int color;public final String metadata;
 public class_2583(int rgb,String meta){color=rgb;metadata=meta;}
 public class_2583 method_36139(int rgb){return new class_2583(rgb,metadata);}
 public boolean equals(Object other){return other instanceof class_2583 s&&s.color==color&&Objects.equals(s.metadata,metadata);}
 public int hashCode(){return Objects.hash(color,metadata);}
}
