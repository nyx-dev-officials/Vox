# AGENT_PLAYBOOK.md — Workflow & Troubleshooting Runbook

> **READ FIRST**: This runbook provides step-by-step diagnostic workflows for common agent tasks in this repository.

---

## 1. Playback Engine Diagnostics & Fix Runbook

When a user reports: **"Songs don't play / stream engine is dead"** or **"Codec/Bitrate badge is missing"**:

### Step 1: Trace the Backend Resolution
1. Open [`app/src/main/kotlin/com/vox/music/utils/YTPlayerUtils.kt`](file:///c:/Avoxapps/Convx-main/app/src/main/kotlin/com/vox/music/utils/YTPlayerUtils.kt).
2. Inspect `YTPlayerUtils.playerResponseForPlayback`:
   - Check if Tidal intercept is throwing `TIDAL_ONLY` error.
   - Check which client is used for `MAIN_CLIENT`:
     - If set to `ANDROID_VR`, check if YouTube returned bot verification errors.
     - Switch to `IOS` or fallback chain if needed.
3. Verify `streamPlayerResponse`:
   - Ensure the client's `adaptiveFormats` contains audio streams with direct URLs.
   - For web clients (`WEB_REMIX`, `WEB`), ensure `EjsNTransformSolver` and `poToken` are attached properly.

### Step 2: Verify ExoPlayer Playback State
1. Open [`app/src/main/kotlin/com/vox/music/playback/MusicService.kt`](file:///c:/Avoxapps/Convx-main/app/src/main/kotlin/com/vox/music/playback/MusicService.kt).
2. Check `StreamResolver.fetchResolvedStream`:
   - Inspect `ResolvedStream`: direct file vs. HLS (`.m3u8`) vs. DASH (`.mpd`).
   - Confirm `FormatEntity` is upserted into `MusicDatabase`.
3. Check `onPlayerError`:
   - Look for `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED` or `ERROR_CODE_DECODER_INIT_FAILED`.

### Step 3: Check UI Badge Display
1. Open [`app/src/main/kotlin/com/vox/music/ui/player/AudioQualityBadge.kt`](file:///c:/Avoxapps/Convx-main/app/src/main/kotlin/com/vox/music/ui/player/AudioQualityBadge.kt).
2. `audioQualityLabel` requires `mime`, `codecs`, `bitrate`, and `sampleRate`.
3. If the badge doesn't show:
   - Check `ShowAudioQualityBadgeKey` preference in DataStore.
   - Check if `currentFormat.agreesWith(playerFormat)` is failing due to codec family mismatch.

---

## 2. Release & Version Bump Runbook

When the user asks to bump version, tag, or push a new release:

### Step 1: Bump Version
1. Update [`version.properties`](file:///c:/Avoxapps/Convx-main/version.properties):
   ```properties
   VERSION_MAJOR=2
   VERSION_MINOR=0
   VERSION_PATCH=<next_patch>
   ```

### Step 2: Create Release Notes
1. Create `.github/release-notes/<version>.md` (e.g. `.github/release-notes/2.0.10.md`).
2. Document features, fixes, and engine changes.
   *Note: Without this file, GitHub Actions will fail with HTTP 422.*

### Step 3: Commit, Tag and Push
```bash
git add version.properties .github/release-notes/<version>.md
git commit -m "chore: bump version to <version>"
git tag <version>
git push origin main --tags
```

### Step 4: Monitor CI Release
1. Check running workflows:
   ```bash
   gh run list --limit 3
   ```
2. Wait for `Signed Release` to finish and publish `vox-<version>.apk` and `vox-universal-foss.apk`.
3. Never upload unverified local APKs over the CI release.
