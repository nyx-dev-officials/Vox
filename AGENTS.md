# AGENTS.md — Vox Development & Playback Rules (READ FIRST)

> **CRITICAL FOR ALL AGENTS & SUBAGENTS**: You MUST read and follow this file before making any changes to Vox (`com.vox.music`).

---

## 1. Core Architecture Principles

1. **Frontend vs. Backend Separation**:
   - **Frontend (UI)**: Jetpack Compose screens, components, and animations (`app/src/main/kotlin/com/vox/music/ui/`). The UI must NEVER resolve stream URLs, parse network requests, or directly control playback audio threads.
   - **Bridge**: `PlayerConnection.kt` provides reactive StateFlows (`isPlaying`, `currentFormat`, `queue`, `playbackState`) and forwards player actions to the service.
   - **Backend (Engine)**: `MusicService.kt` owns the ExoPlayer instance, background foreground service lifecycle, audio DSP processors, and delegates stream resolution to `StreamResolver` and `YTPlayerUtils.kt`.
   - Complete file map: [`docs/CODEBASE_FILE_MAP.md`](file:///c:/Avoxapps/Convx-main/docs/CODEBASE_FILE_MAP.md).

2. **Playback Engine & Fallback Chain**:
   - Every audio playback request goes through `YTPlayerUtils.playerResponseForPlayback()`.
   - Resolution Priority Order:
     1. **Spine Modules** (if custom modules are installed and enabled).
     2. **Tidal Max Lossless** (`TidalEngine.kt`): Official Tidal API (PKCE OAuth) -> Public proxy -> fallback.
        - *Warning*: Only fail hard if engine mode is explicitly `TidalEngineMode.TIDAL_ONLY`. Otherwise, ALWAYS fall through to Innertube.
     3. **JioSaavn** (`SaavnService.kt`): If enabled in settings.
     4. **YouTube InnerTube** (`core/innertube/`): `IOS` and fallback client chain.

3. **Audio Quality & Codec Badges**:
   - The badge (`AudioQualityBadge.kt`) requires **both**:
     - `playerConnection.currentFormat` from the database `FormatEntity` (containing bitrate, sample rate, and codec string).
     - Live decoding format from ExoPlayer (`rememberLiveAudioFormat`).
   - If playback fails to resolve, or ExoPlayer does not transition to `STATE_READY`, the badge returns early and disappears. When debugging missing badges, always fix playback resolution in the backend first.

---

## 2. Release & APK Signing Protocol

1. **Strict Signature Enforcement (v1 + v2)**:
   - Older Android OEMs (Oppo/ColorOS, Vivo, Xiaomi) **require v1 JAR signing**.
   - Missing v1 signing causes Android to fail installation with: `"App not installed as package appears to be invalid"`.
   - Never upload unsigned or v2-only APKs to GitHub releases.
   - Always verify release APKs with:
     ```bash
     apksigner verify --verbose --print-certs --min-sdk-version 21 <apk-path>
     ```
     Ensure `Verified using v1 scheme (JAR signing): true` and `Verified using v2 scheme (APK Signature Scheme v2): true`.

2. **GitHub Release Automation**:
   - Official releases are built and published by GitHub Actions (`.github/workflows/release.yml`) on tag pushes (`2.*`).
   - A release notes file **MUST** exist at `.github/release-notes/<tag>.md` before pushing a tag, otherwise the CI release job fails with HTTP 422.

---

## 3. Strict Development Rules

- **Zero Breaking Refactors**: Do not rewrite working modules; apply surgical, targeted fixes.
- **Never Hardcode Secrets**: Keystore passwords and API secrets live in `local.properties` or environment variables only; never commit credentials to git.
- **Verification First**: Verify changes compile with `./gradlew assembleDebug` or `:app:assembleArm64FossRelease` before pushing commits.
