package net.minecraft;
import java.util.*;
// A resolved translation-content Text: not a literal implementation, matching real content visitation.
public final class FixtureResolvedText implements class_2561 {
 private final String resolved;private final class_2583 style;
 public FixtureResolvedText(String s,class_2583 style){resolved=s;this.style=style;}
 public String getString(){return resolved;}
 public List<class_2561> method_10855(){return List.of();}
 public class_2583 method_10866(){return style;}
 public <T> Optional<T> method_27658(class_5348.class_5246<T> visitor,class_2583 parent){return visitor.accept(style,resolved);}
}
