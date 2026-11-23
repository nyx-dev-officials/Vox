# ISSUE_REPORT_PLAYBACK_AND_BADGE_FAILURE.md — Active Incident Report

> **STATUS**: FIXED IN 2.0.10 — device verification pending  
> **AFFECTED VERSIONS**: 2.0.9  
> **COMPONENT**: Audio Playback Engine & Stream Resolution (`YTPlayerUtils.kt`, `MusicService.kt`, `AudioQualityBadge.kt`)

---

## 1. Problem Description

When a user taps any song in the app:
1. **Audio does not play**: ExoPlayer stalls at buffering or enters `STATE_IDLE` / `STATE_ENDED`. No audio is output.
2. **Codec & Bitrate Badge is Missing**: The `AudioQualityBadge` (which usually displays `MAX`, `CD`, `Opus`, `AAC`, sample rate, and bit depth) does not render at all.

---

## 2. Root Cause Analysis

### Cause A: Stream Resolution Failure in `YTPlayerUtils.kt`
- **Location**: [`app/src/main/kotlin/com/vox/music/utils/YTPlayerUtils.kt`](file:///c:/Avoxapps/Convx-main/app/src/main/kotlin/com/vox/music/utils/YTPlayerUtils.kt#L253)
- In `YTPlayerUtils.kt`:
  ```kotlin
  private val MAIN_CLIENT: YouTubeClient = ANDROID_VR_1_43_32
  ```
- **The Issue**: YouTube recently deployed bot-detection gating and IP restrictions targeting the legacy `ANDROID_VR` client versions without active PoTokens or user session tokens.
- When `MAIN_CLIENT` fails, the fallback loop attempts other clients. If fallback clients also fail or require Web PoTokens that are missing/invalid, `streamPlayerResponse` returns null or bad status, throwing a `PlaybackException` (e.g. `ERROR_CODE_REMOTE_ERROR` or `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED`).

### Cause B: Tidal Engine Configuration & Fallback Intercept
- **Location**: [`app/src/main/kotlin/com/vox/music/utils/YTPlayerUtils.kt`](file:///c:/Avoxapps/Convx-main/app/src/main/kotlin/com/vox/music/utils/YTPlayerUtils.kt#L416-L502)
- In commit `14631be`, Tidal Max was introduced:
  ```kotlin
  val engineMode = runCatching {
      TidalEngineMode.valueOf(
          context.dataStore.get(TidalEngineModeKey, TidalEngineMode.HYBRID.name),
      )
  }.getOrDefault(TidalEngineMode.HYBRID)
  val tidalEnabled = context.dataStore.get(EnableTidalStreamingKey, false) &&
      engineMode != TidalEngineMode.INNERTUBE_ONLY

  if (tidalResult != null) return Result.success(tidalResult)
  if (engineMode == TidalEngineMode.TIDAL_ONLY) {
      return Result.failure(IOException("Tidal could not serve this track and Tidal-only mode is on"))
  }
  ```
- If Tidal is enabled without valid credentials or public proxy access, queries fail. If `engineMode == TIDAL_ONLY`, the player immediately aborts without falling back to InnerTube.

### Cause C: Why the Codec / Bit Badge Disappears
- **Location**: [`app/src/main/kotlin/com/vox/music/ui/player/AudioQualityBadge.kt`](file:///c:/Avoxapps/Convx-main/app/src/main/kotlin/com/vox/music/ui/player/AudioQualityBadge.kt#L57-L75)
- The badge relies on two signals:
  1. `currentFormat` collected from Room database (`FormatEntity`).
  2. `playerFormat` from `rememberLiveAudioFormat(playerConnection)` (ExoPlayer `onTracksChanged`).
- Code logic:
  ```kotlin
  val quality = audioQualityLabel(
      mime = mime,
      codecs = codecs,
      bitrate = bitrate,
      sampleRate = sampleRate,
  ) ?: return
  ```
- Because stream resolution fails upstream, `FormatEntity` is never stored in Room, and ExoPlayer never loads media or reports audio tracks. `quality` evaluates to `null`, triggering `?: return` and causing the badge to vanish completely.

---

## 3. Targeted Fix Plan

1. **Update `MAIN_CLIENT` in `YTPlayerUtils.kt`**:
   - Change `MAIN_CLIENT` from `ANDROID_VR_1_43_32` to `IOS`:
     ```kotlin
     private val MAIN_CLIENT: YouTubeClient = IOS
     ```
   - `IOS` client currently bypasses YouTube bot challenges cleanly without requiring browser PoTokens.

2. **Harden Tidal Fallback Guard**:
   - Ensure that if `TidalEngine.resolve()` returns `null` or throws, it safely logs and allows the fallback chain to continue to YouTube unless explicitly in `TIDAL_ONLY` mode with credentials configured.

3. **Verify Audio Quality Badge Recovery**:
   - Once stream resolution succeeds, verify that `FormatEntity` is upserted and `AudioQualityBadge` reappears displaying the live codec and bitrate.
