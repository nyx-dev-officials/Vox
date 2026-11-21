<div align="center">

<img src="assets/banner dark.png" width="100%" alt="Vox poster with the words Just Listen and a phone displaying the music player">

<br>

# VOX

**Music, held in glass.**

An open-source Android player built on Kotlin, Jetpack Compose and Media3,
with Liquid Glass surfaces, Cupertino-style controls and one champagne accent.

<br>

<a href="https://github.com/nyx-dev-officials/Vox/releases/tag/2.0.4-nightly">
  <img src="https://img.shields.io/badge/DOWNLOAD-NIGHTLY%202.0.4-E5C697?style=for-the-badge&labelColor=0E0D0C&logo=android&logoColor=E5C697" alt="Download Vox Nightly 2.0.4">
</a>
<a href="https://github.com/nyx-dev-officials/Vox/releases">
  <img src="https://img.shields.io/badge/RELEASES-ALL%20BUILDS-1B1917?style=for-the-badge&labelColor=0E0D0C&logo=github&logoColor=F6F3EE" alt="All Vox releases">
</a>
<a href="LICENSE">
  <img src="https://img.shields.io/badge/LICENSE-GPL--3.0-262320?style=for-the-badge&labelColor=0E0D0C&logo=gnu&logoColor=F6F3EE" alt="GPL-3.0 license">
</a>

<br><br>

<a href="#features">Features</a> ·
<a href="#download">Download</a> ·
<a href="#build-locally">Build</a> ·
<a href=".github/CONTRIBUTING.md">Contributing</a> ·
<a href="https://discord.gg/Ejeb4cmzfd">Discord</a>

<br>

<img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&labelColor=0E0D0C" alt="Android 8.0 and later">
<img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=flat-square&labelColor=0E0D0C" alt="Kotlin">
<img src="https://img.shields.io/badge/Compose-UI-4285F4?style=flat-square&labelColor=0E0D0C" alt="Jetpack Compose">
<img src="https://img.shields.io/badge/Media3-playback-E5C697?style=flat-square&labelColor=0E0D0C" alt="Media3">

</div>

> [!WARNING]
> **The current Stable release is outdated and not working.** Use [Nightly 2.0.4](https://github.com/nyx-dev-officials/Vox/releases/tag/2.0.4-nightly) from GitHub while the remaining issues are fixed. A refreshed Stable release will follow once they are.
>
> The project website is being reworked, so download from GitHub Releases for now.

<br>

## The palette

Every surface in Vox is warm — no pure black, no cold grey, one accent. The glass
tints from whatever the album art gives it, but never past the ceiling.

<div align="center">

<img src="https://img.shields.io/badge/-0E0D0C-0E0D0C?style=for-the-badge" alt="#0E0D0C">
<img src="https://img.shields.io/badge/-1B1917-1B1917?style=for-the-badge" alt="#1B1917">
<img src="https://img.shields.io/badge/-262320-262320?style=for-the-badge" alt="#262320">
<img src="https://img.shields.io/badge/-E5C697-E5C697?style=for-the-badge" alt="#E5C697">
<img src="https://img.shields.io/badge/-F6F3EE-F6F3EE?style=for-the-badge" alt="#F6F3EE">
<img src="https://img.shields.io/badge/-9E968D-9E968D?style=for-the-badge" alt="#9E968D">

`Warm Obsidian` · `Smoked Espresso` · `Card` · `Champagne` · `Warm Alabaster` · `Warm Stone`

</div>

<br>

## Features

|  |  |
| :-- | :-- |
| **Playback**<br>Stream through Vox's InnerTube client, keep playing in the background with notification, lock-screen and Android Auto controls. Queues, playlists, sleep timer, speed control and an equalizer. | **Transitions**<br>Crossfade, Automix and DJ-style transitions that read the track instead of guessing at it. |
| **Lyrics**<br>Synchronized lyrics with word-level highlighting, pulled from multiple providers. | **Artwork**<br>Animated canvas artwork, album-inspired colour, and a working offline cache. |
| **Library**<br>Local music plus network shares over SMB and WebDAV, and playlist import from Spotify. | **Integrations**<br>Discord Rich Presence, Last.fm, music recognition, and Listen Together sessions. |
| **Glass**<br>Frosted, refractive surfaces for menus, sheets and floating controls — at 68% smoked glass, never a white lift. | **Layout**<br>Cupertino-style controls from Vox's own HIG, adaptive for phones and wider screens. |

<br>

## Download

| Channel | Status |
| :-- | :-- |
| **Nightly 2.0.4** | [Download from GitHub Releases](https://github.com/nyx-dev-officials/Vox/releases/tag/2.0.4-nightly). Pre-release builds; may be unstable. |
| **Stable** | Outdated and not working. A refreshed build follows the remaining fixes. |

On a 32-bit-only phone (for example Galaxy A13) that says *app not compatible*,
download the `arm32` APK from the same release.

Vox supports **Android 8.0 (API 26)** and later. Install APKs only from sources
you trust — Android may ask you to allow installation from the app you used to
download it.

<br>

## Project structure

A Gradle multi-module Android project. Code lives in the module that owns the
capability, and other modules use its existing public APIs.

| Path | Responsibility |
| :-- | :-- |
| `app/` | Application, screens, ViewModels, navigation, playback service, persistence, wiring. |
| `hig/` | Shared Cupertino-style controls, theme primitives, accessibility helpers, glass utilities. |
| `core/` | Design system, HIG, and shared foundations used by every client. |
| `feature/` | Playback, library, lyrics, catalog, account and Listen Together features. |
| `innertube/` | InnerTube client and related models. |
| `canvas/` | Animated canvas and catalog features. |
| `artwork/` | Artwork loading, caching and transitions. |
| `spotify/` | Spotify authentication and playlist import. |
| `integrations/` | Discord, Last.fm, music recognition and related integrations. |
| `network-storage/` | SMB and WebDAV music sources. |
| `spine/` | Add-on module hosting and lifecycle. |
| `listen-together-server/` | Worker + Durable Objects server for Listen Together. |

Compose for UI, Media3 for playback, Room for the local library, DataStore for
preferences, Hilt for wiring.

<br>

## Build locally

**Requirements** — JDK 21, Android Studio with SDK 37, and the Android NDK for
the native analyzer.

```bash
git clone https://github.com/nyx-dev-officials/Vox.git
cd Vox
./gradlew :app:assembleUniversalFossDebug      # debug APK
./gradlew :app:testUniversalFossDebugUnitTest  # unit tests
```

On Windows use `gradlew.bat` with the same task names. The **Foss** build omits
Google Play services; **Gms** includes Google Cast support. ABI flavours are
`universal`, `arm64`, `arm32` and `x86_64`.

<br>

## Contributing

Bug reports, fixes and features are welcome. Read the
[Contributing Guide](.github/CONTRIBUTING.md), follow the
[Code of Conduct](CODE_OF_CONDUCT.md), and read the
[Security Policy](SECURITY.md) before reporting a vulnerability.

Agents should also follow the [Agent Contribution Guide](AGENT_CONTRIBUTION_GUIDE.md);
its Android, animation, review and requirement-discovery skills are bundled
offline under [`contributor/skills/`](contributor/skills/).

<br>

## Support

- [Discord community](https://discord.gg/Ejeb4cmzfd)
- [Ko-fi](https://ko-fi.com/cosmictaser)
- UPI: `cosmictaser@okicici`

<br>

## Legal

Vox is an independent project, not affiliated with or endorsed by YouTube,
Google, or their affiliates. Some features use unofficial third-party
interfaces that may change or stop working. Users are responsible for following
applicable laws and service terms.

Licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) and
[NOTICE](NOTICE) for license and third-party attribution.

<br>

<div align="center">

<img src="https://img.shields.io/badge/CRAFTED%20BY-Ny-E5C697?style=for-the-badge&labelColor=0E0D0C" alt="Crafted by NyX">

<sub>NyX · 2026 · GPL-3.0</sub>

</div>
