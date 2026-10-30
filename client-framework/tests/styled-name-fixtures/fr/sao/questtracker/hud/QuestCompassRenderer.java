package fr.sao.questtracker.hud;
import net.minecraft.*;
public final class QuestCompassRenderer {
 private static class_2561 infoLabelSource,infoDetailSource;
 private static class_2477 infoLanguage;
 public static String infoName="original";
 public static void seed(){infoLabelSource=class_2561.method_43470("original");infoDetailSource=class_2561.method_43470("123m");infoLanguage=new class_2477();}
 public static boolean empty(){return infoLabelSource==null&&infoDetailSource==null&&infoLanguage==null;}
}
