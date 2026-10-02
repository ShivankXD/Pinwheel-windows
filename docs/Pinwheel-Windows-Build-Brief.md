# Pinwheel for Windows — Build Brief

> **For the agent reading this:** you are building **Pinwheel for Windows**, a desktop photo and video editor that must first reach **exact parity** with the existing Android app *Pinwheel: Edit Photos & Videos*. After parity is signed off by the owner, the desktop app will grow beyond mobile: an MCP server so AI agents can edit projects, keyframes, After Effects-style layer animation and more. Build the baseline so that growth is easy, but **do not build any beyond-mobile feature until parity is accepted.**
>
> Work at maximum care. Read before you write. Verify everything you claim. When unsure what the mobile app does, read its source; do not guess.

---

## 0. Non-negotiable rules

1. **The mobile project is read-only.** It lives at `D:\Nativeoffice-photo&videoeditor`. You may read any file in it. You may **not** create, edit, move, delete, format, rebuild, `git commit`, `git checkout`, `git stash`, `git clean`, or run Gradle tasks that write into it, and you may not change its git state in any way. Copy what you need into the new project. Before you start and before every hand-off, run `git -C "D:\Nativeoffice-photo&videoeditor" status --short` and confirm it is unchanged from your first run (an untracked `output/` folder already exists; leave it alone).
2. **Create the Windows app in a new folder:** `D:\Pinwheel-Windows` (its own git repository). Everything you build lives there.
3. **Parity first.** Every effect, transition, overlay, sticker, filter, caption template, title animation, photo tool, export rule and UI behaviour that exists on mobile must exist on Windows and look and behave the same. "Similar" is not enough for effects: the same effect at the same time on the same frame must produce visually the same image (see §8 for the tolerance and the test method).
4. **No phone or emulator actions without the owner's say-so.** Never run `connectedAndroidTest` or `gradlew connected…` against the owner's phone: it uninstalls the app and deletes their projects. If you need reference renders from Android, ask the owner.
5. **Keep stable IDs.** Effect IDs (`fx-…`), transition IDs (`fx-tr-…`), overlay IDs (`fx-ov-…`), sticker IDs, look IDs, caption-template IDs and project JSON keys are part of the file format. A project saved on Android must open on Windows, and vice versa (§4.3).
6. **Licences travel with assets.** Fonts, sounds, sample images, LibRaw, lensfun data and third-party notices come with licence files in the mobile repo (`app/src/main/assets/licenses`, `assets/third_party`, `LICENSES/`, `THIRD_PARTY_NOTICES.md`, `assets/audio_catalog/LICENSES.md`). Copy those notices with the assets and show them in the app's licences screen, as mobile does.
7. **Report honestly.** If a test fails, say so with the output. If something is not at parity yet, list it. Never mark a parity item done without the evidence described in §8.

---

## 1. What Pinwheel is (mobile, today)

Android app, package `com.nativeoffice.pinwheel` (Kotlin namespace `com.nativeoffice.studio`), version 0.31.0. About 31,000 lines of Kotlin in 150+ files, Jetpack Compose UI, Media3 1.11.1 for video, GLES2 / GLSL ES 1.00 shaders for every effect, a CPU photo engine in Kotlin, LibRaw (C++) for RAW photos.

### 1.1 Feature inventory (all must be ported)

**Home / shell**
- Animated startup intro: the brand pinwheel spins with its stick (`ui/HomeKit.kt` `PinwheelMark`, `res/drawable-nodpi/pinwheel_fan*.png`).
- Welcome / sign-in screen (Google sign-in; export requires sign-in — `ui/PinwheelWelcome.kt`, `ui/HomeKit.kt` `ExportGate`, `auth/`).
- Home: hero with **New video** / **Edit photo**, **Continue editing** project cards (long-press: Open / Rename / Delete with storage freed message), **Trending effects** row, photo **Templates** gallery (`ui/StudioHome.kt`, `media/PinwheelTemplates.kt`, `ui/PinwheelTemplateGallery.kt`).
- In-app media picker for videos/photos with multi-select and canvas presets (`ui/DeviceMediaPicker.kt`, `data/DeviceMediaLibrary.kt`).
- Settings (restyled cards, Plus card, storage manager, licences, export folder) — `ui/PinwheelSettingsScreen.kt`, `ui/LibraryDialogs.kt`.
- Plan badge (Free pill / gold crown "Plus") and the Plus page — `ui/PinwheelPlus.kt`.

**Video editor** (`ui/VideoEditor.kt` is the hub)
- Multi-track timeline: main clip track (videos and photos mixed), effect lanes, overlay/sticker/text lanes, audio lanes, captions; pinch zoom, ruler, filmstrip thumbnails (lazy), drag to trim/move, lanes up to 64 (`ui/VideoTimeline.kt`, `model/VideoTimeline.kt`, `model/VideoTimelineTrim.kt`, `ui/VideoFilmstrip.kt`).
- Clip tools: Split, Extract audio, Volume, Speed (0.25–4×), Delete, Duplicate, Replace, Crop/zoom, Rotate/mirror, Adjust (exposure, contrast, saturation, warmth, grade), Looks/filters, Motion (Ken Burns: Zoom in/out, Pan ×4), Transition at each cut (46 transitions) — `ui/VideoPanels.kt`, `ui/VideoGradePanels.kt`, `ui/VideoMotionPanel.kt`, `ui/VideoTransitionTray.kt`, `media/video/VideoGrade.kt`, `media/video/VideoCropGeometry.kt`, `media/video/VideoClipMotionEffect.kt`.
- **Effects**: 277 GPU effects in 16 categories (Trending, Montage, 3D, Pixel, Neon, Whimsical, Classic, Intro & Outro, Party, Motion, Light, Split, Retro, Glitch, Celebrate, Graffiti), each with 0–8 adjustable params, looping animated preview tiles, search, Adjust sheet, effect targeting (apply an effect to an overlay/sticker layer instead of the main video) — `media/video/VideoFxCatalog.kt`, `VideoFxShaders*.kt`, `VideoFxEffect.kt`, `VideoFxPreviewRenderer.kt`, `VideoLayerFxOverlay.kt`, `ui/VideoEffectsPage.kt`, `ui/VideoEffectTargetPanel.kt`.
- **Overlay library**: 90 full-frame looks (Atmosphere, Light, Background, Scenery, Transitions, Intro & End, Elements, Texture, Frames) applied as timed layers with opacity — `VideoFxCatalog.overlays`, `VideoFxShadersOverlays.kt`.
- **Overlays from files**: photo layers and video picture-in-picture layers with masks (Circle, Rounded, Heart, Star, Diamond…), border, shadow, opacity, in/out animations (Fade, Zoom, Slide, Spin, Pop), transform gestures — `ui/VideoOverlayWorkspace.kt`, `media/video/VideoOverlayPainter.kt`, `VideoOverlayGeometry.kt`, `VideoPipCompositor.kt`, `VideoPipPreview.kt`.
- **Stickers**: ~130 animated vector stickers drawn per frame from time `t` (emoji motions, text badges, social cards, arrows, party, doodles, chat, elements, intro/end) — `media/video/VideoAnimatedStickers.kt`, `VideoStickerCards.kt`, `VideoStickerCatalog.kt`, `ui/VideoStickerPanel.kt`.
- **Text/titles**: styles, colours, backgrounds, animations, title templates, typography scaling with canvas — `model/VideoTitleTemplates.kt`, `media/video/VideoTitleMotion.kt`, `ui/VideoTitleTemplatePanel.kt`.
- **Captions**: auto captions from speech, SRT import/export, word timings, caption styles, animated caption templates (built-in + imported packages from `assets/caption_packages`), 32 bundled fonts — `media/captions/*`, `media/video/CaptionMotion*.kt`, `ImportedCaption*.kt`, `CaptionFonts.kt`, `VideoCaptionPainter.kt`, `data/SrtCodec.kt`, `ui/VideoCaption*.kt`, `ui/VideoAutoCaptionDialog.kt`, `ui/CaptionTemplateUi.kt`, `ui/ImportedCaptionBrowser.kt`.
- **Audio**: up to 16 tracks; Sounds library (free Creative Commons music via the Openverse API with real album art, waveform previews, search, mood shelves, Commercial filter with ? help, saved songs, credit copying), bundled sound effects, device audio, extract audio from a video file or from a clip (mutes the clip), voiceover recording, per-track volume (0–2), fades, speed, voice effects (Deep, Chipmunk, Robot, Radio, Telephone…), noise reduction, waveforms — `media/audio/*`, `media/video/VideoAudioDspProcessor.kt`, `VideoAudioEnvelope.kt`, `media/AudioWaveform.kt`, `ui/VideoMusicLibrary.kt`, `ui/VideoAudio*.kt`, `ui/VideoVoiceoverPanel.kt`.
- **Canvas**: aspect ratio (Original, 9:16, 16:9, 1:1, 4:5, …), Fit/Fill, background (solid colour, blur of the video) — `media/video/VideoCanvasSizing.kt`, `VideoCreativeEffects.kt` (`VideoBlurredCanvasEffect`, `VideoCanvasBackgroundEffect`).
- **Undo/redo** with persisted history — `model/VideoHistory.kt`, `data/VideoHistoryCodec.kt`.
- **Preview**: real-time playback, scrubbing, fullscreen, overlays drawn live over the player.
- **Export**: resolution 480p/720p/1080p/2K/4K, frame rate 24/25/30/50/60, **live bitrate slider**, estimated size, Pinwheel ending card (2.5 s), save to `Movies/Pinwheel`, progress and result screen — `ui/VideoExportScreens.kt`, `StudioViewModel.export`, `media/video/PinwheelEndingOverlay.kt`.

**Photo editor** (`ui/PhotoEditor.kt` is the hub; six tabs: Filters, Adjust, Cutout, Retouch, Crop, Frame)
- Filters: ~75 Instagram/Lightroom-style looks with amount, saved user presets — `media/PhotoLookLibrary.kt`, `media/PhotoLooks.kt`.
- Adjust: the Pinwheel dial + sliders for light (exposure, contrast, highlights, shadows, whites, blacks), colour (temperature, tint, vibrance, saturation), curves, colour mix (HSL bands + targeted colour picker), three-way colour grading, detail (sharpen, clarity, texture, noise reduction), dehaze, vignette/grain/effects, white-balance picker, Magic Enhance (scene analysis + adjustable amount) — `media/Photo*.kt`, `ui/PhotoAdjustPanel.kt`, `ui/PhotoDial.kt`, `ui/Photo*Controls.kt`.
- Masks (brush, linear, radial with local adjustments) — `media/PhotoMasks.kt`, `media/BrushMaskIndex.kt`, `ui/PhotoMaskControls.kt`.
- Cutout: remove background (erase, restore, tap select, smart edges, sensitivity, softness) and replace it with colours, blur or a photo — `media/PhotoCutout.kt`, `ui/PhotoCutoutUi.kt`.
- Retouch: clone and heal spots — `media/PhotoClone.kt`, `media/PhotoHeal.kt`.
- Crop & straighten, rotate, flip, guided perspective, lens corrections (lensfun profiles, distortion, TCA, vignetting) — `ui/PhotoCrop*.kt`, `media/PhotoPerspective.kt`, `PhotoGuidedPerspective.kt`, `PhotoLens*.kt`, `PhotoTca.kt`, `LensfunCalibrations.kt`.
- Frame: borders (Even, 1:1, 4:5, 9:16, 3:4), colours, blur border, width — `media/PhotoFrame.kt`, `ui/PhotoFrameControls.kt`.
- RAW development: LibRaw decode to linear, precision renderer, Ultra HDR export — `media/Raw*.kt`, `app/src/main/cpp/*`.
- Compare (hold, split divider), fullscreen Preview with before/after, histogram, versions, copy/paste adjustments, presets, photo information, export formats (JPEG, PNG, WebP lossless, TIFF, Ultra HDR) with size options — `export/*`, `ui/PhotoControls.kt`.

**Plans** (`billing/PinwheelBilling.kt`, `ui/PinwheelPlus.kt`)
- Free: no effects (every effect tile shows a gold crown; tapping one opens "Effects are available for Plus users only" with **Buy Plus** and **Continue as free user**), export capped at 1080p / 30 fps / 8 Mbps, Pinwheel ending card always added.
- Plus: everything; up to 4K / 60 fps / 100 Mbps; ending card optional (off by default).
- Mobile prices: ₹99 first month, then ₹199/month; ₹1,499/year (Google Play subscription `pinwheel_plus`, base plans `monthly`, `yearly`, offer `first-month-99`).

### 1.2 Read these first (in this order)

1. `D:\Nativeoffice-photo&videoeditor\README.md`, `NATIVE_OFFICE_STUDIO_PLAN.md`, `CHECKPOINT.md`
2. `docs/VIDEO_EDITOR_BLUEPRINT.md`, `docs/VIDEO_EFFECTS_AUTHORING.md`, `docs/VIDEO_PIP.md`, `docs/VIDEO_HISTORY.md`, `docs/VIDEO_CHECKPOINT_0*.md`
3. `docs/PHOTO_EDITOR_REFERENCE.md`, `docs/PHOTO_CHECKPOINT*.md`, `docs/RAW_DEVELOPMENT.md`, `docs/LENS_PROFILES.md`, `docs/GUIDED_PERSPECTIVE.md`
4. `docs/PINWHEEL_BRAND.md`, `docs/PINWHEEL_BRANDING_SOURCE.md` (colours, gradient, typography, icon rules)
5. `docs/AUTO_CAPTIONS_ASSESSMENT.md`, `docs/CAPTION_RECHECK_*.md`, `docs/VOICEOVER_RELIABILITY.md`
6. Source, in this order: `model/` → `data/` → `media/video/VideoFxCatalog.kt` + `VideoFxShaders.kt` → `media/video/VideoRenderGraph.kt` → `StudioViewModel.kt` → `ui/VideoEditor.kt` → `ui/PhotoEditor.kt`.
7. Tests are executable specifications: `app/src/test/…` (JVM) and `app/src/androidTest/…` (59 device test classes, e.g. `VideoStressTest`, `VideoFxCatalogTest`, `VideoStillTransitionPlaybackTest`, `VideoPipTest`, `Photo*PipelineTest`).

All Kotlin source is under `app/src/main/java/com/nativeoffice/studio/`. Assets are under `app/src/main/assets/`, drawables under `app/src/main/res/`.

---

## 2. Recommended technology stack

Choose the stack that lets you **copy the most mobile code unchanged** and run **the exact same GLSL**. Recommended:

| Layer | Choice | Why |
|---|---|---|
| Language | **Kotlin (JVM 17+)** | ~70% of mobile logic (models, codecs, catalogs, effect specs, photo engine maths, sticker/caption drawing logic, timeline rules) is plain Kotlin and ports by copy-and-adapt. |
| UI | **Compose Multiplatform for Desktop** (JetBrains) | Mobile UI is Jetpack Compose; most composables port with small changes. Same animation and gesture model, so behaviour matches. |
| 2D drawing | **Skia via Skiko** (comes with Compose Desktop) | Replaces `android.graphics.Canvas/Paint/Path/Bitmap` for stickers, titles, captions, overlay painting. API is very close. |
| GPU effects | **OpenGL ES 2.0/3.0 through ANGLE (D3D11 backend)**, bound with **LWJGL 3** (EGL + GLES) | The 277 effects, 46 transitions and 90 overlays are GLSL ES 1.00. ANGLE runs them **unchanged** with ES semantics (precision, `texture2D`, `gl_FragColor`), which is what makes pixel parity realistic. Ship `libEGL.dll` / `libGLESv2.dll` from an official ANGLE build. |
| Decode | **FFmpeg (LGPL build)** via JavaCPP presets or a small C++ JNI bridge, with **D3D11VA** hardware decode | Covers every format phones produce (H.264/HEVC/VP9/AV1, AAC/Opus, HEIC/JPEG stills via `image2` or Windows Imaging Component). |
| Encode | **Windows Media Foundation H.264/HEVC encoder** (hardware MFT) via FFmpeg `h264_mf`/`hevc_mf`, or NVENC/QSV/AMF when present; AAC via Media Foundation | Avoids GPL `libx264`. If you ever add x264, the whole app becomes GPL — don't. |
| Audio out | **WASAPI** via FFmpeg/`javax.sound` or LWJGL OpenAL Soft; audio clock drives A/V sync | Matches mobile mixing (per-track gain, fades, speed with pitch preservation, DSP). |
| RAW | **LibRaw** (same version as `app/src/main/cpp/vendor`) built for Windows x64 with CMake, exposed via JNI (mirror `raw_bridge.cpp`) | Same decoder → same photos. |
| Speech | **whisper.cpp** (MIT) with a small/base multilingual model, run locally | Replaces Android `SpeechRecognizer`; produce the same `VideoCaptionCue` + word offsets. |
| Storage | Per-user `%APPDATA%\Pinwheel\` (projects, originals, caches) | Mirrors mobile `filesDir/projects`, `filesDir/originals`, `cacheDir`. |
| Packaging | **MSIX** (Microsoft Store) + signed **MSI/EXE** via `jpackage`/Conveyor; Windows 10 21H2+ and 11, x64 first, ARM64 later | |
| Build | Gradle (Kotlin DSL), multi-module | Same tooling as mobile. |

Alternatives considered (only switch with a written reason): C++/Qt + Vulkan (best raw performance, but everything is rewritten and shaders need translation); Electron/Tauri + WebGL1 (WebGL1 is GLSL ES 1.00 too, so shaders port, but all Kotlin logic must be rewritten in TS/Rust). If ANGLE + LWJGL proves blocked, the fallback is desktop OpenGL 3.3 core with a GLSL ES→330 compatibility header, **plus** the golden-image tests in §8 to prove it still matches.

---

## 3. Architecture

### 3.1 Big picture

Build a **headless editing core** that knows nothing about UI. The Compose UI is one client of it; the future MCP server will be another. Every edit is a typed, serializable **command**. That is the key to future AI editing, scripting, macros and collaborative features.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ pinwheel-app  (Compose Desktop UI: Home, Video editor, Photo editor,       │
│                Settings, Plus, Export, pickers, dialogs)                    │
└───────────────▲──────────────────────────────▲─────────────────────────────┘
                │ observes state / sends Commands │ (later) pinwheel-mcp
┌───────────────┴──────────────────────────────┴─────────────────────────────┐
│ pinwheel-core                                                               │
│  model/      StudioProject, Clip, VideoProjectEdits, VideoTimedEffect,     │
│              VideoImageOverlay, VideoTextOverlay, VideoAudioTrack,          │
│              VideoCaptionCue, Adjustments, PhotoHistory … (ported 1:1)      │
│  codec/      ProjectStore JSON v10, VideoEditCodec, AdjustmentCodec,        │
│              VideoHistoryCodec, PhotoHistoryCodec, SrtCodec (1:1)           │
│  catalog/    VideoFxCatalog (277 fx, 46 transitions, 90 overlays),          │
│              stickers, looks, caption templates, title templates, sounds    │
│  editing/    Command bus, undo/redo, timeline rules (split, trim, lanes,    │
│              snapping, extract audio, effect placement), validation         │
│  plans/      Entitlement interface, Free/Plus limits                        │
└───────────────▲───────────────────────▲──────────────────────▲─────────────┘
                │                       │                      │
┌───────────────┴────────┐ ┌────────────┴──────────┐ ┌─────────┴──────────────┐
│ pinwheel-render        │ │ pinwheel-media        │ │ pinwheel-photo         │
│ GL context (ANGLE),    │ │ FFmpeg demux/decode,  │ │ CPU photo engine       │
│ FxProgram, effect      │ │ frame cache, seeking, │ │ (ported Kotlin),       │
│ chain ping-pong,       │ │ audio mixer + DSP,    │ │ LibRaw JNI, lensfun,   │
│ history/trail buffers, │ │ A/V clock, encoder    │ │ cutout, heal/clone,    │
│ transitions, overlay   │ │ (Media Foundation),   │ │ export writers         │
│ looks, layer fx, PIP   │ │ muxer, waveform,      │ │ (JPEG/PNG/WebP/TIFF/   │
│ compositor, Skia       │ │ thumbnails/filmstrip  │ │ Ultra HDR)             │
│ overlay upload, preview│ │                       │ │                        │
│ tile renderer          │ │                       │ │                        │
└────────────────────────┘ └───────────────────────┘ └────────────────────────┘
┌────────────────────────────────────────────────────────────────────────────┐
│ pinwheel-platform: storage paths, settings, Google sign-in (OAuth loopback │
│ + PKCE), entitlement provider, crash log, Openverse client, whisper.cpp    │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 The video frame pipeline (one function, used by preview **and** export)

Mobile builds a Media3 `Composition` in `VideoRenderGraph.composition(...)`. On desktop you own the pipeline, so write a deterministic **frame evaluator**:

```
renderFrame(project, tUs, outputSize, mode = PREVIEW | EXPORT) -> GPU texture
  1. Resolve which main-track clip covers tUs (clip.durationMs = span / speed; stills have no source time).
  2. Decode that clip's frame at sourceTimeUs = clip.startMs*1000 + (tUs - clipStartUs) * speed.
     Stills: decode once, reuse (a split or trimmed photo is just the photo; there is no source time).
  3. Per-clip effects in mobile order: rotation/mirror → crop/zoom → grade (VideoGrade) or colour matrix →
     canvas fit (aspect ratio, Fit/Fill, blur or colour background).
  4. Composition-level chain in mobile order (see VideoRenderGraph):
     a. Clip motion (Ken Burns) b. legacy timed effects c. catalog effects aimed at the main video
     d. Soft Glow e. transitions (one window per cut) f. overlay-library looks g. output Presentation
     h. layer-fx overlays (stickers/photos with their own effects) i. titles/captions/stickers/photos (export only;
     preview draws them live in the UI layer, exactly like mobile) j. Pinwheel ending card.
  5. PIP video layers are composited by a compositor (mobile: VideoPipCompositor) with alpha, anchor, scale,
     rotation and masks.
```

Preview and export must share this code path. Preview may render the effect stack at **720 px short side** (mobile does this to avoid stalls) and draw text/stickers in the UI layer; export renders everything at the chosen resolution.

### 3.3 Effect runtime (port exactly)

- Shader source = `FX_HEADER + FX_LIBRARY + spec.defines ("#define MODE n") + spec.shader + FX_MAIN` (`VideoFxShaders.kt`). Copy the strings byte for byte.
- Uniforms: `uTexture`, `uPrev`, `uTrail`, `uSize` (px), `uAspect`, `uTime` (seconds since effect start), `uDuration`, `uProgress` (0..1), `uP0`/`uP1` (8 params packed by `VideoFxSpec.uniforms`, 0..1, missing → default, unused → 0.5), `uFlipY`.
- Chain: active effects at `t` (start inclusive, end exclusive) run in list order, ping-ponging between two textures; the last one writes to the output.
- History effects (`spec.history`): keep previous frame + two trail buffers; trail = mix(current, trail, 0.86); reset history on seeks/jumps > 250 ms.
- Transitions: for each cut with a transition spec, a window centred on the cut, half-width `min(transitionDurationMs/2, prevClip/2, nextClip/2)`, skipped if half < 30 ms; params `intensity=1`, `speed=transitionSpeed`.
- Overlay-library layers (`uri = "overlay:fx-ov-…"`) are not images: they become timed effects with `intensity = default × layer.opacity` on the composition clock.
- Effects whose `target` is an overlay layer id run on that layer (colour + coverage-mask passes, `VideoLayerFxOverlay.kt`) before it is composited.
- Preview tiles: `VideoFxPreviewRenderer` renders each spec on the sample photos in `assets/fx_samples` (sample index in the spec) as a 2.4 s loop. Port it so the effects page looks identical.
- Legacy effect kinds (`Vignette`, `Grain`, `RGB split`, `Pixelate`, `Soft Glow`, `3D Tilt`) still exist in saved projects — support them.
- Retired IDs (`VideoFxCatalog.retired`) are dropped when loading.

### 3.4 Data model and file format

- Port `model/*.kt` **1:1** (same field names, defaults and `sanitized()` rules). Constants: `MAX_VIDEO_EFFECTS = 40`, `MAX_VIDEO_OVERLAYS = 32`, `MAX_VIDEO_AUDIO_TRACKS = 16`, `MAX_TIMELINE_LANE = 63`.
- Port `data/ProjectStore.kt` (JSON, `"version": 10`, accepts 1..10) and all codecs exactly, so **a mobile project JSON plus its media opens on Windows**. Add an import/export "project package" (`.pinwheel` zip = project JSON + referenced media with relative paths) because mobile stores absolute `file://…/files/originals/…` URIs; remap on import.
- Bump the version only for new desktop-only fields, keep older keys readable, and never repurpose an existing key.

### 3.5 Commands (design now, use everywhere)

```kotlin
sealed interface EditCommand { val label: String }
data class AddClips(val uris: List<String>, val at: Int?) : EditCommand
data class SplitClip(val clipId: String, val atMs: Long) : EditCommand
data class AddEffect(val kind: String, val atMs: Long, val lengthMs: Long?, val params: Map<String, Float>?) : EditCommand
data class SetEffectParams(val effectId: String, val params: Map<String, Float>) : EditCommand
data class SetTransition(val clipId: String, val transitionId: String, val durationMs: Long) : EditCommand
// … one per mobile action
```

- `ProjectSession.apply(cmd)` validates, applies, records undo, autosaves (mobile autosaves on every commit — "Saved on device").
- UI gestures that stream (dragging, sliders) update a draft and commit **one** command on release, like mobile (`draftCanvas` / `commitCanvas()` in `VideoEditor.kt`).
- Later, the MCP server exposes these commands as tools 1:1, plus queries (`list_effects`, `describe_project`, `render_frame(t)` returning a PNG for the AI to look at) and long-running `export`.

---

## 4. Porting guide by subsystem

### 4.1 Android → desktop API map

| Android | Desktop replacement |
|---|---|
| `Context`, `filesDir`, `cacheDir`, `SharedPreferences` | `AppPaths` (`%APPDATA%\Pinwheel`, `%LOCALAPPDATA%\Pinwheel\cache`) + a small JSON/Properties settings store with the same keys |
| `android.graphics.Bitmap/Canvas/Paint/Path/Typeface` | Skia (`org.jetbrains.skia.*`) via Skiko; keep a thin adapter so painter code stays close to mobile |
| `BitmapFactory`, EXIF | Skia `Image.makeFromEncoded`, Windows Imaging Component or `metadata-extractor` for EXIF orientation |
| `MediaMetadataRetriever`, `MediaExtractor` | FFmpeg probe / demux |
| Media3 `CompositionPlayer`, `Transformer` | Your frame evaluator (§3.2) + player loop + Media Foundation encoder |
| `GLES20` / EGL | LWJGL GLES + EGL on ANGLE |
| `AudioProcessor` chain | Port the DSP math (`VideoAudioDspProcessor`, `VideoAudioEnvelope`, voice pitch) as PCM float processors in your mixer |
| `SpeechRecognizer` | whisper.cpp |
| `ActivityResultContracts` pickers | Native Windows file dialogs plus the in-app media picker scanning Pictures/Videos/Downloads (same UI as mobile) |
| Credential Manager Google sign-in | OAuth 2.0 desktop flow with loopback redirect + PKCE (needs a new Google OAuth client of type *Desktop app* — ask the owner) |
| Google Play Billing | `Entitlement` interface. Phase 1: same Free/Plus rules with a local developer toggle **only in debug builds**. Phase 2 (owner decides): Microsoft Store subscription add-on and/or account-linked entitlement through a small backend that verifies Play purchases, so one Plus works on phone and PC |
| `R.drawable`, assets | Copy `assets/` and `res/drawable-nodpi/` into Compose resources |
| Haptics | Omit or map to subtle UI feedback |

### 4.2 Video playback

- Player loop: audio is the master clock; video frames are rendered for the clock time; drop frames rather than block audio.
- Decode ahead on worker threads, keep a small frame cache around the playhead, and keep thumbnails/waveforms in a disk cache.
- Seeking must land on the requested frame: decode from the previous keyframe.
- Rebuild only what changed on edit (mobile debounces edits by 120 ms and keeps overlays live without rebuilding the graph).

### 4.3 Export

- Same frame evaluator at the export size and fps; encoder bitrate from the slider.
- Audio: mix all tracks (clip audio unless muted, extracted audio, music, SFX, voiceovers) with per-track volume, fades, speed and DSP into AAC 48 kHz stereo.
- Output duration = project duration (+ 2.5 s ending card when it applies). Mobile tests assert |duration − expected| < 500 ms; keep that test.
- Free limits are enforced in the export code, not only in the UI: height ≤ 1080, fps ≤ 30, bitrate ≤ 8 Mbps, ending card forced on.

### 4.4 Photo editor

- `media/Photo*.kt` is mostly pure Kotlin on `IntArray` pixels: port it as is, then swap `Bitmap` I/O for Skia. Keep the preview scheduler idea (fast low-res interaction render + precise full render, `PreviewScheduler`, `PhotoPreviewCache`).
- RAW: build LibRaw for Windows from the same vendor sources; mirror `raw_bridge.cpp` functions.
- Thumbnails, versions, copy/paste adjustments, presets: same codecs.

### 4.5 Stickers, titles, captions

- Sticker drawing is deterministic from time `t` (seconds since layer start). Port the drawing code line by line onto Skia. Behaviour to keep: a paused editor shows stickers in their settled pose (`settled = !playing` → `t = max(t, 0.4)`), while playback and export show the pop-in.
- Caption templates and fonts: copy `assets/caption_fonts` (32 fonts) and `assets/caption_packages`, and port `CaptionMotionRenderer`, `ImportedCaptionMotion` and `VideoCaptionPainter`.

### 4.6 Sounds library (Openverse)

Port `media/audio/OnlineMusic.kt` as is: `https://api.openverse.org/v1/audio/` with no key, `category=music`, `license_type=modification` (All) or `commercial,modification` (Commercial), exclude ND licences, durations 5 s–15 min, pages of 20, waveform endpoint, artwork cache, downloads into the originals folder, saved songs, credit strings. Use User-Agent `Pinwheel/1.0 (Windows video editor)`.

---

## 5. Behaviours and invariants that must not regress

These were found the hard way on mobile and are covered by tests there. Port the tests too.

1. **Effects tray:** tapping a tile **swaps** the selected effect layer (never stacks a second one), and the tick confirms. A new effect is placed at the playhead for 3 s (1.5 s if `transitionLike`), then plays its window as a preview. Free users get the Plus window instead.
2. **Split photo clips:** a still is just its trimmed duration starting at zero. Never clip an image by start time, or playback stalls at the second half.
3. **Sounds shorter than the movie:** pad with silence to the movie end, or seeks past the sound's end freeze.
4. **Playing just before audio ends:** mobile starts 220 ms earlier if play is pressed in the last ~0.2 s before a point where audio stops (a Media3 bug). On desktop, make sure your own pipeline has no such stall, and keep the regression test.
5. **Preview resolution:** effects run at 720 px short side in preview; at full resolution, 40 stacked effects stalled playback.
6. **PIP compositing:** inputs that have ended drop out of the compositor's list; never index by position without checking.
7. **Library overlays** are full-frame looks: their panel shows only Opacity and Timing, and there is no drag hint.
8. **Filler clips** (audio tail, ending card) take the movie's exact pixel size; otherwise you get letterbox bars.
9. **Timeline ruler:** the time readout sits on a solid background so ruler labels never show through.
10. **Photo Preview** shows exactly what the canvas shows, including a removed or replaced background.
11. **Extract audio from a clip:** the new track starts at the clip's timeline position with the clip's trim and speed, and the clip is muted.
12. **Deleting a project** removes its imported originals only if no other project uses them.
13. **Autosave:** "Saved on device" after every commit; a crash never loses more than the current gesture.
14. **Retired effects** are dropped on load without breaking the project.

---

## 6. UX on desktop (same app, desktop-native)

- Same visual design: dark UI, Pinwheel gradient (cyan `#37D6E6` → blue `#5B8CFF` → violet `#A86BFF`), gold gradient for Plus, same icons and copy. Check `docs/PINWHEEL_BRAND.md`.
- Layout: keep the mobile structure (preview on top, timeline, tool tray, bottom toolbar) at narrow window widths, and use a wider layout when there is room: preview left, inspector panel right, full-width timeline below. Every mobile action must be reachable.
- Add desktop affordances that don't change behaviour: keyboard shortcuts (Space play/pause, J/K/L, ←/→ frame step, S split, Del delete, Ctrl+Z/Ctrl+Shift+Z, Ctrl+S is a no-op because autosave), mouse wheel zoom on the timeline, drag-and-drop media from Explorer, right-click context menus mirroring long-press menus, a high-DPI-correct UI.
- Window: remember size/position; full-screen preview with Esc to exit.

---

## 7. Phased plan

Each phase ends with a short written report, the tests you ran with their output, and screenshots.

| Phase | Scope | Done when |
|---|---|---|
| **P0 Setup** | Repo `D:\Pinwheel-Windows`, Gradle multi-module, CI script, ANGLE + LWJGL "hello triangle", FFmpeg decode of `assets/demo.mp4`, Compose window with Pinwheel theme and intro animation | Builds from clean; app opens; mobile repo unchanged |
| **P1 Core** | Port `model/`, `data/` codecs, catalogs (effects, transitions, overlays, stickers, looks, caption/title templates); command bus + undo; JVM tests ported from `app/src/test` | Round-trip tests pass on real mobile project JSON (ask the owner for a couple of exported project packages) |
| **P2 Effect runtime** | FxProgram, chain, history, transitions, overlay looks, preview-tile renderer, golden-image harness | All 277 + 46 + 90 shaders compile; golden comparisons pass (§8) |
| **P3 Video engine** | Frame evaluator, player loop with audio clock, scrubbing, stills, speed, PIP, canvas, grade, motion | Ported versions of `VideoStressTest`, `VideoStillTransitionPlaybackTest`, `VideoPipTest` pass |
| **P4 Video editor UI** | Home, picker, editor shell, timeline, every tool tray, effects page, overlays, stickers, text, captions, audio + Sounds library, voiceover | A scripted walkthrough reproduces every mobile action |
| **P5 Export** | Encoder, mixer, ending card, Free/Plus limits, progress/result screens | Export tests pass: duration, frames not black, ending fills the frame, limits enforced |
| **P6 Photo editor** | All six tabs, RAW, lens, masks, cutout, retouch, frame, compare/preview, versions, export formats | Ported photo pipeline tests pass; side-by-side screenshots match mobile |
| **P7 Platform** | Settings, storage manager, licences, sign-in, entitlement interface, crash log, installer (MSIX + MSI), auto-update channel | Clean install on a fresh Windows 11 VM works offline except sign-in, music and captions-model download |
| **P8 Parity sign-off** | The parity checklist (§8.3) completed with evidence | **Owner approves.** Only then start beyond-mobile work (MCP, keyframes, AE-style features) |

---

## 8. Proving parity

### 8.1 Golden images for every effect

- For each effect, transition and overlay, render the sample image(s) from `assets/fx_samples` at t = 0.3, 0.9, 1.5 and 2.1 s with default params (this matches mobile `VideoFxCatalogTest.everyEffectDrawsAndAnimates`).
- Reference renders come from Android: mobile `VideoFxCatalogTest.writeContactSheets` writes contact sheets to the app's external `fx-sheets` folder on a device or emulator. **Ask the owner** to produce them, or to approve running it on an emulator (never on their phone via `connectedAndroidTest`).
- Compare per pixel in sRGB: mean absolute error ≤ 2/255 and 99th-percentile error ≤ 8/255 per channel. Effects using random noise or time hashing must still match, since they are deterministic functions of `uv` and `uTime`. Any failure is a bug until explained.
- Build a desktop "Effect lab" window (debug only) that shows mobile reference vs desktop render vs difference heatmap, effect by effect.

### 8.2 Behaviour tests

Port these mobile tests as desktop tests (same scenarios, same assertions): `VideoStressTest` (heavy project: 8 clips, 40 effects, overlays, stickers, PIP, 3 sounds — plays at ≥ 24 fps in preview with no freeze over 2.5 s, seeks, 30 rapid edits, 720p/1080p export durations, every effect stacked 40 at a time exports), `VideoStillTransitionPlaybackTest`, `VideoPipTest`, `VideoAudioExtractTailTest`, `VideoRenderGraphTest`, `VideoGradeStickerTest`, `VideoOverlayPainterTest`, `VideoTitle*Test`, `VideoCaption*Test`, `VideoHistory*Test`, `Photo*PipelineTest`, `Raw*Test`, plus all JVM tests in `app/src/test`.

### 8.3 Parity checklist (deliver as `PARITY.md` in the Windows repo)

One row per feature in §1.1 and per catalog item group: *mobile behaviour → desktop status (✅ / ⚠️ / ❌) → evidence (test name, screenshot path, golden-diff report)*. Nothing is ✅ without evidence.

---

## 9. Getting ready for "more advanced than mobile" (design now, build after P8)

- **MCP server** (`pinwheel-mcp` module): stdio and local HTTP transports. Tools map to `EditCommand`s, plus `open_project`, `list_projects`, `describe_project` (structured JSON), `list_effects/transitions/overlays/stickers/fonts`, `render_frame(t, size) → PNG`, `export(settings) → job id`, `job_status`. Every AI edit is undoable and shows live in the UI. Ask for user confirmation on destructive tools (delete project, overwrite export).
- **Keyframes**: make every numeric property (transform, opacity, effect params, volume) a `Track<T>` that can be constant or keyframed with easing. Store constants exactly as mobile does today, so mobile-compatible projects stay compatible.
- **Layers and precompositions** (After Effects style): the frame evaluator should already treat the main track, overlays and effect layers as an ordered layer stack. Avoid hard-coding "main track + extras".
- **Deterministic renderer**: same project + same time = same pixels (no wall-clock time, seeded randomness only). This makes AI tooling, caching and tests reliable.
- **Plugin-ready effect registry**: effects defined as data (`VideoFxSpec` + GLSL), so new effects ship without code changes on mobile and desktop alike.

---

## 10. Practical tips

- **The `&` in `D:\Nativeoffice-photo&videoeditor`** breaks `cmd.exe` and some scripts. Always quote the path, prefer PowerShell `-LiteralPath`, or open files through your file tools.
- Mobile builds use `build.ps1` in the mobile repo. **Don't run it**: it writes to the mobile repo. You don't need to build mobile at all.
- GLSL ES 1.00 rules the shaders follow: float literals need decimal points, loops have constant bounds, no `round`/`tanh`/`flat`. Don't "fix" the shaders for desktop GLSL; run them on ANGLE.
- Mobile frames are bottom-up (row 0 = bottom) in GL; Skia/bitmaps are top-down. Track orientation explicitly (`uFlipY`) and add a test that a known asymmetric image comes out the right way up.
- Colour: mobile tone-maps HDR to SDR and works in sRGB 8-bit. Do the same for parity; add 10-bit/HDR later as a desktop extra.
- FFmpeg licensing: LGPL build only, dynamically linked, with notices in the licences screen.
- Performance budgets: 1080p preview with 10 active effects ≥ 30 fps on integrated Intel Iris Xe; first frame after opening a project < 1.5 s; seek < 150 ms on SSD.
- Keep `CrashLog`-style local crash reports in `%LOCALAPPDATA%\Pinwheel\crash-reports`.
- When a mobile behaviour looks like a bug, **don't silently "fix" it on desktop.** Match it, list it in `PARITY.md` under "mobile quirks", and let the owner decide.
- Commit often with clear messages. Never put AI co-author trailers in commits (owner preference). Author commits as the owner's configured git identity.

---

## 11. What to send back after each phase

1. What was built (module by module) and where it lives.
2. Tests run and results (paste the summary lines; attach failing output in full).
3. Screenshots / short screen recordings of the new screens next to the matching mobile screens.
4. Updated `PARITY.md`.
5. Open questions for the owner (sign-in client ID, billing direction, reference renders, sample projects).
6. Confirmation that `git -C "D:\Nativeoffice-photo&videoeditor" status --short` is unchanged.

**Start with P0 now.** Read §1.2 before writing code.
