# SAOMCTW v1.0.0 — Release draft

This is a draft release, not yet published to GitHub Releases.

## Optional binary assets
- SAO_TW_v9.zip — main Traditional Chinese resource pack (~304 MB). This pack may contain third-party assets; redistribute only with authorization.
- SAO_TW_v9_support.zip — TLZH and Taiwan flag support textures.
- SAO_TW_Agent_v9.jar — locally built Java Agent for Minecraft 1.21.4 + SAOUI 2.35.0 integration.

## Verification
Compare SHA-256 values with SHA256SUMS.txt before installation. V9's offline tests passed, but the installed game has not been verified with V9 visually.

## Install
Place all files in the same local folder; set the JVM argument to:
```
-javaagent:D:\翻譯\SAO_TW_Agent_v9.jar
```
Only one SAO localization agent should be loaded. Do not claim to be an official SAO server or mod distribution.
