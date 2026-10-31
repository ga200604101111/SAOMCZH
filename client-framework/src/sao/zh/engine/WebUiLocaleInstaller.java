package sao.zh.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Opt-in UI-only localization performed by the SAOMCZH Java Agent for Sound of Aincrad 1.0.0.
 * The upstream extractor always refreshes index.html from the original mod.
 * Called immediately after extraction, before Chromium loads the page.
 * Never changes the original mod JAR, voice-client.js or a WebRTC path.
 */
public final class WebUiLocaleInstaller {
    private WebUiLocaleInstaller() {}
    // Pin the exact source we audited. Future mod versions fail open, with no rewrite.
    private static final String ORIGINAL_HTML_SHA256 =
        "c068d2aeb93478b0d49a1953aeee58f906debb78651d89ea79ad25564338cd02";
    private static final String PATCH_MARKER =
        "    <script src=\"saomczh-zh-tw.js\"></script>";
    private static final String SOURCE_ANCHOR =
        "    </script>\n    <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\" />";
    private static final String SCRIPT_ASSET =
        "/assets/saomczh_zapaudio/web/saomczh-zh-tw.js";

    /** Does not throw for unknown or edited upstream HTML: simply leaves it alone. */
    public static boolean install(Path htmlPath) throws IOException {
        if (htmlPath == null) return false;
        Path root = Path.of(System.getProperty("user.home"), ".zapaudio-web-cache")
            .toAbsolutePath().normalize();
        Path target = htmlPath.toAbsolutePath().normalize();
        if (!target.startsWith(root) || !target.getFileName().toString().equals("index.html")) return false;
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(target)) return false;
        if (Files.isSymbolicLink(target.getParent()) || Files.isSymbolicLink(root)) return false;
        if (Files.size(target) > 256 * 1024) return false;
        byte[] source = Files.readAllBytes(target);
        if (source.length > 256 * 1024) return false;

        byte[] script;
        try (InputStream stream = WebUiLocaleInstaller.class.getResourceAsStream(SCRIPT_ASSET)) {
            if (stream == null) throw new IOException("SAOMCZH voice UI locale asset unavailable");
            script = stream.readAllBytes();
        }
        if (script.length < 1024 || script.length > 96 * 1024) {
            throw new IOException("Invalid size for voice UI locale asset");
        }
        String html = new String(source, StandardCharsets.UTF_8);
        Path sidecar = target.resolveSibling("saomczh-zh-tw.js");
        if (Files.isSymbolicLink(sidecar)) return false;
        if (html.contains(PATCH_MARKER)) {
            // Repeated extract calls must not duplicate handlers or change unrelated files.
            if (Files.isRegularFile(sidecar, LinkOption.NOFOLLOW_LINKS)
                    && Arrays.equals(Files.readAllBytes(sidecar), script)) return true;
            return false; // Unknown/tampered localized copy; don't silently modify it.
        }
        if (!ORIGINAL_HTML_SHA256.equals(sha256(source))) return false;
        int anchor = html.indexOf(SOURCE_ANCHOR);
        if (anchor < 0 || html.indexOf(SOURCE_ANCHOR, anchor + 1) >= 0) return false;
        String next = html.substring(0, anchor)
            + "    </script>\n" + PATCH_MARKER
            + "\n    <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\" />"
            + html.substring(anchor + SOURCE_ANCHOR.length());
        if (next.length() > 272 * 1024) return false;
        // Asset FIRST: the patched HTML is never published with a missing script.
        writeAtomically(sidecar, script);
        writeAtomically(target, next.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path temp = Files.createTempFile(target.getParent(), ".saomczh-ui-", ".tmp");
        try {
            Files.write(temp, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
