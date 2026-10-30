package sao.zh.zapaudio.mixin;

import java.io.IOException;
import java.nio.file.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sao.zh.zapaudio.WebUiLocaleInstaller;

/**
 * Never intercepts the browser or its voice/audio handlers.
 * Only patches the local extracted index AFTER the original extractor succeeds.
 */
@Mixin(targets = "fr.saofr.zapaudio.client.web.WebClientExtractor", remap = false)
public abstract class WebClientExtractorMixin {
    @Inject(method = "extract()Ljava/nio/file/Path;", at = @At("RETURN"), remap = false)
    private static void saomczh$afterExtract(CallbackInfoReturnable<Path> cir) {
        try {
            WebUiLocaleInstaller.install(cir.getReturnValue());
        } catch (IOException | RuntimeException failure) {
            // Fail open: a locale error must never prevent microphone or web client startup.
            System.err.println("[SAOMCZH-WebUI] Localization unavailable; keeping original Sound of Aincrad UI ("
                + failure.getClass().getSimpleName() + ")");
        }
    }
}
