package sao.zh.compat16;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

/** Reflection-only bridge; Minecraft classes are loaded by Fabric Knot, not by this agent. */
public final class PackBridge {
    private static volatile Path zipPath;
    private static volatile String packId;
    private static volatile Path supportPath;
    private static final Map<ClassLoader,Object> SUPPORT_CACHE = new WeakHashMap<ClassLoader,Object>();
    private static final Map<ClassLoader,Object> PROFILE_CACHE = new WeakHashMap<ClassLoader,Object>();
    private static volatile boolean reportedDiscover;
    private static volatile boolean reportedActive;
    private static volatile boolean reportedFailure;

    private PackBridge() { }

    public static void configure(Path path){configure(path,null);}
    public static void configure(Path path,Path support){
        zipPath = path.toAbsolutePath().normalize();
        packId = "file/" + zipPath.getFileName();
        supportPath = support==null?zipPath.getParent().resolve("SAO_ZH_v16_support.zip"):support.toAbsolutePath().normalize();
        reportedDiscover = false;
        reportedActive = false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Map addPack(Map current, Class<?> managerClass) {
        if (zipPath == null || current == null || managerClass == null) return current;
        try {
            Object profile = buildProfile(managerClass.getClassLoader());
            if (profile == null) return current;
            String id = packId;
            Map next = new TreeMap(current);
            next.put(id, profile);
            if (Files.isRegularFile(supportPath)) {
                Object support = profileFor(managerClass.getClassLoader(), supportPath, SUPPORT_CACHE);
                if (support != null) next.put("file/" + supportPath.getFileName(), support);
            }
            if (!reportedDiscover) {
                reportedDiscover = true;
                bridgeLog("EXTERNAL_PACK_REGISTERED " + id + " source=" + zipPath);
                if (Files.isRegularFile(supportPath)) bridgeLog("SUPPORT_PACK_REGISTERED file/" + supportPath.getFileName());
            }
            return Collections.unmodifiableMap(next);
        } catch (Throwable e) {
            failure("register", e);
            return current;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List forceActive(List selected, Class<?> managerClass) {
        if (zipPath == null || selected == null || managerClass == null) return selected;
        try {
            Object profile;
            synchronized (PackBridge.class) {
                profile = PROFILE_CACHE.get(managerClass.getClassLoader());
            }
            if (profile == null) {
                failure("active:profile-not-found", null);
                return selected;
            }
            List next = new ArrayList(selected);
            // Remove existing reference (if any); we always enforce last/highest priority.
            for (int i = next.size() - 1; i >= 0; i--) {
                if (profile == next.get(i)) next.remove(i);
            }
            next.add(profile);
            Object support;
            synchronized (PackBridge.class) { support = SUPPORT_CACHE.get(managerClass.getClassLoader()); }
            if (support != null) { next.remove(support); next.add(support); }
            if (!reportedActive) {
                reportedActive = true;
                bridgeLog("EXTERNAL_PACK_ACTIVE_HIGHEST_PRIORITY " + packId);
            }
            return Collections.unmodifiableList(next);
        } catch (Throwable e) {
            failure("activate", e);
            return selected;
        }
    }

    private static synchronized Object buildProfile(ClassLoader loader) throws Exception {
        return profileFor(loader, zipPath, PROFILE_CACHE);
    }

    private static synchronized Object profileFor(ClassLoader loader, Path path, Map<ClassLoader,Object> cache) throws Exception {
        Object old = cache.get(loader);
        if (old != null) return old;
        Class<?> text = loader.loadClass("net.minecraft.class_2561");
        Class<?> packInfo = loader.loadClass("net.minecraft.class_9224");
        Class<?> factoryType = loader.loadClass("net.minecraft.class_3288$class_7680");
        Class<?> factory = loader.loadClass("net.minecraft.class_3258$class_8615");
        Class<?> resourceType = loader.loadClass("net.minecraft.class_3264");
        Class<?> selection = loader.loadClass("net.minecraft.class_9225");
        Class<?> profile = loader.loadClass("net.minecraft.class_3288");
        Class<?> folderSource = loader.loadClass("net.minecraft.class_1065");

        String label = path.getFileName().toString().replace(".zip", "");
        Object display = text.getMethod("method_43470", String.class).invoke(null, label);
        Method infoFactory = folderSource.getDeclaredMethod("method_57056", String.class, text);
        infoFactory.setAccessible(true);
        Object info = infoFactory.invoke(null, "file/" + path.getFileName(), display);
        Constructor<?> ctor = factory.getConstructor(Path.class);
        Object source = ctor.newInstance(path);
        Object typeClient = resourceType.getField("field_14188").get(null);
        Field posField = folderSource.getDeclaredField("field_49133");
        posField.setAccessible(true);
        Object normalPackPosition = posField.get(null);
        Method make = profile.getMethod("method_45275", packInfo, factoryType, resourceType, selection);
        Object made = make.invoke(null, info, source, typeClient, normalPackPosition);
        if (made == null) {
            bridgeLog("PACK_PROFILE_REJECTED_INVALID_METADATA");
            return null;
        }
        cache.put(loader, made);
        return made;
    }

    private static void bridgeLog(String message) {
        try { System.out.println("[SAO-ZH-Compatibility] " + message); }
        catch (Throwable ignored) { }
        Path p=zipPath;
        if(p==null) return;
        try {
            String s=Instant.now().toString()+" [SAO-ZH-Compatibility] "+message+"\r\n";
            Files.write(p.getParent().resolve("SAO_ZH_compat16.log"),
                s.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch (Throwable ignored) { }
    }

    private static void failure(String where, Throwable e) {
        if (e != null && Boolean.getBoolean("sao.tw.debug")) e.printStackTrace(System.err);
        if (!reportedFailure) {
            reportedFailure = true;
            String type = e == null ? "missing" : e.getClass().getSimpleName();
            String cause = e != null && e.getCause() != null ? e.getCause().getClass().getSimpleName() : "-";
            bridgeLog("PACK_BRIDGE_ERROR " + where + " " + type + " cause=" + cause);
        }
    }
}
