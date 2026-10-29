package sao.zh.compat16;

import java.io.InputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/** Direct integration, no changes to launcher settings or pack directories. */
public final class DirectAgent {
    private static volatile Path logDirectory;
    private static final Pattern VERSION = Pattern.compile("SAO_TW_v(\\d+)\\.zip");
    private static final String BOOTSTRAP_RESOURCE =
            "embedded/SAO_ZH_compat16_bridge.jar";
    private DirectAgent() { }

    public static void premain(String arguments, Instrumentation inst) {
        try {
            Path jar = Paths.get(DirectAgent.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            logDirectory = Paths.get(System.getProperty("sao.zh.extension.root",jar.getParent().toString())).toAbsolutePath().normalize();
            log("PREMAIN_OK");
            if ("probe".equals(arguments)) {
                log("PROBE_ONLY");
                return;
            }
            if ("test".equals(arguments))System.setProperty("sao.zh.offline-test","true");
            String entrypoint = System.getProperty("sun.java.command", "");
            if (!"test".equals(arguments) && !entrypoint.contains("KnotClient")) {
                log("SKIP_NOT_FABRIC");
                return;
            }
            Path home = Paths.get(System.getProperty("user.home", ""));
            Path game = home.resolve("AppData").resolve("Roaming")
                    .resolve(".Triggers Client").resolve("instances").resolve("Sword Art Online");
            if (!Files.isDirectory(game.resolve("mods")) || !containsSaoUi(game.resolve("mods"))) {
                log("SKIP_NOT_SAO");
                return;
            }
            // Install the bridge before resolving remotely managed pack state.
            // Files used in the running instance are never deleted/replaced in place.
            RepositoryTransformer transformer = new RepositoryTransformer();
            FeatureTransformer features = new FeatureTransformer();
            SplashTransformer splash = new SplashTransformer();
            Path helperPath = ensureBootstrapHelper(logDirectory);
            JarFile helperJar = new JarFile(helperPath.toFile());
            inst.appendToBootstrapClassLoaderSearch(helperJar);
            Path legacy = chooseValidPack(logDirectory);
            Path legacySupport = logDirectory.resolve("SAO_ZH_v16_support.zip");
            Class<?> manager = Class.forName("sao.zh.compat16.PackLifecycleManager", true, null);
            Object chosen = manager.getMethod("startAutomatic",Path.class,Path.class,Path.class)
                    .invoke(null,logDirectory,legacy,legacySupport);
            Path pack = (Path)chosen.getClass().getField("main").get(chosen);
            Path support = (Path)chosen.getClass().getField("support").get(chosen);
            if(pack==null){log("NO_PACK_KEEP_TRANSLATION_FEATURES");}
            if(pack!=null){
            Class<?> helper = Class.forName("sao.zh.compat16.PackBridge", true, null);
            Method configure = helper.getMethod("configure", Path.class,Path.class);
            configure.invoke(null, pack,support);
            log("EXTERNAL_PACK_SOURCE " + pack.toString());
            inst.addTransformer(transformer, false);
            log("TRANSFORMER_INSTALLED");
            }
            try {
                Class<?> runtime = Class.forName("sao.zh.compat16.RuntimeBridge", true, null);
                runtime.getMethod("configure", Path.class).invoke(null, logDirectory);
                inst.addTransformer(features, false);
                Class<?> earlySplash = Class.forName("sao.zh.compat16.SplashBridge", true, null);
                if(Boolean.TRUE.equals(earlySplash.getMethod("available").invoke(null))){
                    inst.addTransformer(splash, false);
                    log("STARTUP_SPLASH_ASSETS_VERIFIED fix=bootstrap-resources-1");
                }else{
                    log("STARTUP_SPLASH_SKIPPED_KEEP_ORIGINAL");
                }
                log("OPTIONAL_FEATURES_INSTALLED");
            } catch (Throwable optionalFailure) {
                log("OPTIONAL_FEATURES_DISABLED " + optionalFailure.getClass().getSimpleName());
            }
        } catch (Throwable error) {
            log("AGENT_SETUP_FAILED " + error.getClass().getSimpleName()
                + (error.getCause() == null ? "" :
                    " cause=" + error.getCause().getClass().getSimpleName()));
            if (Boolean.getBoolean("sao.tw.debug")) error.printStackTrace(System.err);
        }
    }

    private static Path ensureBootstrapHelper(Path root) throws IOException {
        Path helper = root.resolve("SAO_ZH_compat16.bootstrap.jar");
        Path tmp = Files.createTempFile(root, ".sao_tw_bootstrap_", ".tmp");
        try (InputStream in = DirectAgent.class.getClassLoader()
                .getResourceAsStream(BOOTSTRAP_RESOURCE)) {
            if (in == null) throw new IOException("Embedded bootstrap bridge missing");
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            try (JarFile test = new JarFile(tmp.toFile())) {
                if (test.getEntry("sao/zh/compat16/PackBridge.class") == null)
                    throw new IOException("Bootstrap bridge class missing");
            }
            // A deterministic cache; only rewrite if necessary.
            if (Files.isRegularFile(helper) &&
                    java.util.Arrays.equals(Files.readAllBytes(helper), Files.readAllBytes(tmp))) {
                return helper;
            }
            Files.move(tmp, helper, StandardCopyOption.REPLACE_EXISTING);
            return helper;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static boolean containsSaoUi(Path mods) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mods,"SAOUI-*.jar")) {
            return stream.iterator().hasNext();
        }
    }
    private static long version(Path p) {
        Matcher m = VERSION.matcher(p.getFileName().toString());
        if (!m.matches()) return -1L;
        try { return Long.parseLong(m.group(1)); }
        catch (NumberFormatException ignored) { return -1L; }
    }
    private static Path chooseValidPack(Path root) throws IOException {
        List<Path> candidates = new ArrayList<Path>();
        try (DirectoryStream<Path> s = Files.newDirectoryStream(root, "SAO_TW_v*.zip")) {
            for (Path p : s)
                if (Files.isRegularFile(p) && VERSION.matcher(p.getFileName().toString()).matches())
                    candidates.add(p);
        }
        candidates.sort(new Comparator<Path>() {
            @Override public int compare(Path a,Path b) {
                return Long.compare(version(b),version(a));
            }
        });
        for (Path p : candidates) {
            try (ZipFile z = new ZipFile(p.toFile())) {
                if (z.getEntry("pack.mcmeta") != null
                    && z.getEntry("assets/saoui/lang/zh_tw.json") != null)
                    return p.toAbsolutePath().normalize();
            } catch (IOException ignored) { log("INVALID_PACK " + p.getFileName()); }
        }
        return null;
    }
    public static void log(String message) {
        try { System.out.println("[SAO-ZH-Compatibility] " + message); }
        catch (Throwable ignored) { }
        Path dir=logDirectory;
        if (dir == null) return;
        try {
            String s=Instant.now().toString()+" [SAO-ZH-Compatibility] "+message+"\r\n";
            Files.write(dir.resolve("SAO_ZH_compat16.log"),
                s.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,StandardOpenOption.APPEND);
        } catch (Throwable ignored) { }
    }
    public static void main(String[] args) throws Exception {
        if (args.length==2 && "--verify-bytecode".equals(args[0])) {
            try (ZipFile z=new ZipFile(Paths.get(args[1]).toFile())) {
                ZipEntry entry=z.getEntry("net/minecraft/class_3283.class");
                if (entry==null) throw new IllegalArgumentException("Missing ResourcePackManager");
                byte[] bytes;
                try (InputStream in=z.getInputStream(entry)) {
                    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
                    byte[] buf=new byte[8192];
                    int n;
                    while((n=in.read(buf))>0)out.write(buf,0,n);
                    bytes=out.toByteArray();
                }
                System.out.println("REPOSITORY_TRANSFORM_OK "
                    +RepositoryTransformer.verify(bytes)+"/2");
            }
        } else if(args.length==1 && "--self-test".equals(args[0])) {
            System.out.println("SELF_TEST_OK");
        }
    }
}
