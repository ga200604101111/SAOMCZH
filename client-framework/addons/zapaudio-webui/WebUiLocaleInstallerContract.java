package sao.zh.zapaudio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.ZipFile;

public final class WebUiLocaleInstallerContract {
    private static int assertions = 0;
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Path home = Files.createTempDirectory("saomczh-webui-fixture-home");
        System.setProperty("user.home", home.toString());
        Path folder = home.resolve(".zapaudio-web-cache").resolve("v-fixture");
        Files.createDirectories(folder);
        Path index = folder.resolve("index.html");
        Path originalMod = Path.of(args[0]);
        byte[] original;
        byte[] voice, music, ambience;
        try (ZipFile upstream = new ZipFile(originalMod.toFile())) {
            original = upstream.getInputStream(upstream.getEntry("web/index-template.html")).readAllBytes();
            voice = upstream.getInputStream(upstream.getEntry("web/voice-client.js")).readAllBytes();
            music = upstream.getInputStream(upstream.getEntry("web/music-player.js")).readAllBytes();
            ambience = upstream.getInputStream(upstream.getEntry("web/ambiance-player.js")).readAllBytes();
        }
        Files.write(index, original);
        Files.write(folder.resolve("voice-client.js"), voice);
        Files.write(folder.resolve("music-player.js"), music);
        Files.write(folder.resolve("ambiance-player.js"), ambience);
        check(WebUiLocaleInstaller.install(index), "Original source should be localized before browser");
        String before = new String(original, StandardCharsets.UTF_8);
        String after = Files.readString(index, StandardCharsets.UTF_8);
        String marker = "    <script src=\"saomczh-zh-tw.js\"></script>";
        check(after.contains(marker), "Locale script not attached");
        check(after.indexOf(marker) < after.indexOf("https://cdn.tailwindcss.com"), "Locale loaded too late");
        check(after.replace(marker + "\n", "").equals(before), "Original HTML functions/layout must be byte-identical");
        check(Files.readString(folder.resolve("saomczh-zh-tw.js")).contains("window.ZAP_T"), "Locale asset missing");
        check(Arrays.equals(voice, Files.readAllBytes(folder.resolve("voice-client.js"))), "Voice client changed");
        check(Arrays.equals(music, Files.readAllBytes(folder.resolve("music-player.js"))), "Music client changed");
        check(Arrays.equals(ambience, Files.readAllBytes(folder.resolve("ambiance-player.js"))), "Ambient client changed");
        byte[] patched = Files.readAllBytes(index);
        check(WebUiLocaleInstaller.install(index), "Idempotent install should succeed");
        check(Arrays.equals(patched, Files.readAllBytes(index)), "Second install changed HTML");
        check(Files.readString(index).split("saomczh-zh-tw.js",-1).length == 2, "Duplicate patch");
        Files.writeString(index, "<html>Unrecognized plugin upgrade</html>", StandardCharsets.UTF_8);
        check(!WebUiLocaleInstaller.install(index), "Unknown upstream version must fail open");
        check(Files.readString(index).contains("Unrecognized plugin upgrade"), "Unknown version modified");
        Path outsider = Files.createTempFile("outside-saomczh-cache-", ".html");
        Files.write(outsider, original);
        check(!WebUiLocaleInstaller.install(outsider), "Outside cache must never be modified");
        check(Arrays.equals(original, Files.readAllBytes(outsider)), "Outside cache file changed");
        System.out.println("ZAPAUDIO_WEBUI_INSTALLER_PASS assertions=" + assertions
            + " original_mod_untouched=true media_js_untouched=true fail_open=true");
    }
}
