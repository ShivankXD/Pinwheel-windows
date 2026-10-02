# Parity checklist

P0 status: desktop foundation implemented and locally validated. Mobile editing parity is not complete.

✅ means the stated check has evidence; ⚠️ means partial implementation or missing comparison evidence; ❌ means unimplemented. No mobile editing or catalog feature is marked ✅ in P0. Golden acceptance requires sRGB per-channel mean absolute error <= 2/255 and 99th percentile <= 8/255 at 0.3, 0.9, 1.5 and 2.1 s.

## P0 foundation checks

| Check | Status | Evidence |
|---|---|---|
| Six Gradle modules build from clean | ✅ | `evidence/p0/clean-build-output.txt`, `build.gradle.kts`, `settings.gradle.kts` |
| ANGLE D3D11 through LWJGL, real shader pixels and top-down orientation | ✅ | `AngleTriangleTest` (2 tests), `evidence/p0/angle-triangle.png` |
| FFmpeg shared LGPL demo decode at 0 and 1 s without source writes | ✅ | `FfmpegDecoderTest` (3 tests), `evidence/p0/demo-frame.png`, native hash log |
| Compose window opens and intro completes | ✅ | `evidence/p0/smoke-summary.txt`, intro/workspace images |
| Broken native startup returns a failing exit code | ✅ | `scripts/check-smoke-failure.ps1`, `evidence/p0/expected-native-failure.txt` |
| Windows storage-path contract | ✅ | `AppPathsTest` (1 test) |
| Copied assets equal read-only mobile assets | ✅ | `evidence/p0/asset-hashes.txt` |
| Mobile status stays at initial `?? output/` | ✅ | `evidence/p0/mobile-status.txt` |
| Current mobile intro visually matches desktop | ⚠️ | Source timings/geometry copied; current mobile screenshot/recording unavailable |
| Hosted CI clean build, real-window smoke and startup failure check | ✅ | [Windows P0 run 36973747235](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36973747235), `evidence/p0/ci-result.json`; tested runtime commit `cd9fa45` |

## Feature inventory from brief section 1.1

| Feature | Mobile behaviour | Desktop status | Evidence or remaining work |
|---|---|---|---|
| Startup intro | Spinning fan artwork, stick geometry, appearance and letter timing | ⚠️ | P0 source port and desktop screenshot; current mobile visual reference pending |
| Welcome and Google sign-in | Google authentication; export requires sign-in | ❌ | P7; desktop OAuth client required |
| Home | New video, Edit photo, project cards, Open/Rename/Delete, freed storage message, Trending and Templates | ❌ | P4; no home screen implemented |
| Media picker | Video/photo multi-select and canvas presets | ❌ | P4; no picker implemented |
| Settings | Cards, Plus, storage manager, licences and export folder | ⚠️ | P0 copied-notice dialog only; all other settings and P7 packaging pending |
| Plan badge and Plus page | Free pill, gold Plus crown and subscription page | ❌ | P7; entitlement and billing absent |
| Multi-track timeline | Mixed clips, effects, overlays/stickers/text, audio, captions, ruler, filmstrips, trim/move/zoom; 64 lanes | ❌ | P1/P3/P4; timeline absent |
| Clip tools | Split, Extract audio, Volume, Speed 0.25-4x, Delete, Duplicate, Replace, Crop/zoom, Rotate/mirror, grade, Looks, Motion and transitions | ❌ | P1/P3/P4; tools absent |
| GPU effects | 277 effects, parameters, animated previews, search, Adjust and layer targeting | ❌ | P1/P2/P4; triangle is not a catalog effect |
| Overlay library | 90 timed full-frame looks with opacity and timing | ❌ | P1/P2/P4; catalog and compositing absent |
| File overlays and PIP | Photo/video layers, masks, border, shadow, opacity, transforms and in/out animation | ❌ | P3/P4; absent |
| Animated stickers | Time-based vector stickers; settled paused pose and animated playback/export | ❌ | P1/P2/P4; absent |
| Text and titles | Styles, colours, backgrounds, animations, templates and canvas-scaled typography | ❌ | P1/P2/P4; absent |
| Captions | Auto speech captions, SRT, word times, styles, built-in/imported motion templates and 32 fonts | ❌ | P1/P2/P4; absent |
| Audio | 16 tracks, Sounds/Openverse, art/waveforms/search/moods/Commercial/saved/credits, bundled SFX, device/extracted audio, voiceover, volume/fades/speed/DSP/denoise | ❌ | P1/P3/P4; absent |
| Canvas | Aspect ratios, Fit/Fill, solid or blurred background | ❌ | P3/P4; absent |
| Undo and redo | Persisted history, gesture drafts and one commit on release | ❌ | P1/P4; absent |
| Video preview | Playback, scrubbing, fullscreen and live overlays | ❌ | P3/P4; one decoded frame is not playback |
| Video export | 480p through 4K, 24/25/30/50/60 fps, bitrate slider/size, 2.5 s ending, Movies/Pinwheel, progress/result | ❌ | P5; absent |
| Photo filters | Approximately 75 looks with amount and saved user presets | ❌ | P6; absent |
| Photo adjust | Dial, light/colour, curves, HSL/picker, grading, detail, dehaze, vignette/grain, WB and Magic Enhance | ❌ | P6; absent |
| Photo masks | Brush, linear and radial local adjustments | ❌ | P6; absent |
| Photo cutout | Erase/restore/tap select, edges/sensitivity/softness and replacement background | ❌ | P6; absent |
| Photo retouch | Clone and heal spots | ❌ | P6; absent |
| Photo crop and lens | Crop/straighten/rotate/flip, guided perspective, lensfun distortion/TCA/vignette | ❌ | P6; absent |
| Photo frame | Even/aspect borders, colours, blur and width | ❌ | P6; absent |
| RAW development | Same LibRaw decoder, linear precision renderer and Ultra HDR | ❌ | P6; absent |
| Photo compare, versions and export | Hold/split, fullscreen before/after, histogram, versions, adjustment copy/paste, presets/info; JPEG/PNG/WebP/TIFF/Ultra HDR sizes | ❌ | P6; absent |
| Free plan | All effects blocked with Plus dialog; enforced 1080p/30 fps/8 Mbps, ending always on | ❌ | P1/P5/P7; enforcement absent |
| Plus plan | 4K/60 fps/100 Mbps; optional ending off by default | ❌ | P1/P5/P7; enforcement absent |
| Subscription offers | Mobile monthly/yearly plans and first-month offer; desktop entitlement policy to be chosen | ❌ | P7; billing direction required |
| Project interoperability | Version 10, read 1-10, same keys/defaults/clamps/IDs; package media remapping | ❌ | P1; real mobile project packages required |
| Desktop affordances | Shortcuts, wheel zoom, file drop, context menus, DPI, saved window position, fullscreen/Esc | ❌ | P4/P7; no editor affordance claim |
| Platform distribution | Storage/settings, crash reports, MSIX/MSI and update channel | ❌ | P7; paths defined but services/installers absent |

## Catalog item groups

All groups below are unported. There is no catalog shader compilation or golden-diff report yet. Trending categories contain curated subsets, not additional unique catalog IDs.

| Catalog group | Mobile behaviour | Desktop status | Evidence or remaining work |
|---|---|---|---|
| Effects: Trending | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Montage | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: 3D | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Pixel | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Neon | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Whimsical | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Classic | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Intro & Outro | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Party | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Motion | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Light | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Split | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Retro | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Glitch | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Celebrate | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Effects: Graffiti | 277 effect IDs; exact GLSL/default params and timed previews | ❌ | P1/P2; no desktop catalog/render evidence |
| Transitions: Trending | 46 transition IDs; cut timing and exact shader pixels | ❌ | P1/P2; no desktop catalog/render evidence |
| Transitions: Basic | 46 transition IDs; cut timing and exact shader pixels | ❌ | P1/P2; no desktop catalog/render evidence |
| Transitions: Camera | 46 transition IDs; cut timing and exact shader pixels | ❌ | P1/P2; no desktop catalog/render evidence |
| Transitions: Slide | 46 transition IDs; cut timing and exact shader pixels | ❌ | P1/P2; no desktop catalog/render evidence |
| Transitions: Mask | 46 transition IDs; cut timing and exact shader pixels | ❌ | P1/P2; no desktop catalog/render evidence |
| Transitions: Effects | 46 transition IDs; cut timing and exact shader pixels | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Trending | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Atmosphere | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Light | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Background | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Scenery | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Transitions | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Intro & End | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Elements | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Texture | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Overlay looks: Frames | 90 look IDs; category membership, opacity and timing; mixed sticker cards preserved | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Trending | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Social | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Text | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Emoji | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Chat | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Love | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Arrows | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Doodle | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Party | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Elements | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Animated stickers: Intro & End | Same IDs and deterministic drawing/motion; settled paused pose | ❌ | P1/P2; no desktop catalog/render evidence |
| Geometric stickers: Marks | Same static geometry and saved overlay IDs | ❌ | P1/P2; no desktop catalog/render evidence |
| Geometric stickers: Shapes | Same static geometry and saved overlay IDs | ❌ | P1/P2; no desktop catalog/render evidence |
| Geometric stickers: Frames | Same static geometry and saved overlay IDs | ❌ | P1/P2; no desktop catalog/render evidence |
| Other catalogs: Video looks | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Title styles and motion | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Title templates | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Built-in caption templates | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Imported caption packages | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Caption fonts (32) | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Bundled sound effects | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: Home photo templates | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Other catalogs: User presets | All mobile entries, parameters, assets and applicable notices | ❌ | P1/P4/P6; no desktop catalog/render evidence |
| Photo looks: Aesthetic | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Cinematic | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Film | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Food | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Landscape | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Monochrome | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Moody | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Night | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Portrait | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Seasons | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Social | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Travel | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |
| Photo looks: Vintage | Same look parameters and rendering | ❌ | P1/P6; no desktop render evidence |

## Mobile quirks and regression invariants

| Mobile behaviour to preserve | Desktop status | Evidence or remaining work |
|---|---|---|
| Selecting an effect swaps the selected layer; tick commits; new windows are 3 s or 1.5 s; Free opens Plus. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| A split still starts at zero and uses its trimmed duration. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Pad shorter sounds with silence through movie end. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Playback near an audio end must not stall; preserve the mobile 220 ms workaround scenario. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Preview effects use a 720 px short side; test 40 stacked effects. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Ended PIP inputs leave the compositor; never assume input positions. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Full-frame library overlays show only Opacity/Timing with no drag hint. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Audio-tail and ending filler use the movie's exact dimensions. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Timeline time readout has a solid background. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Photo Preview equals the edited canvas including replaced/removed backgrounds. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Extracted clip audio preserves timeline start, trim and speed, then mutes the source clip. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Project deletion removes originals only when no other project uses them. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Autosave after every committed gesture; crash loses at most the current draft. | ❌ | Port the corresponding behaviour tests in P1-P6 |
| Drop retired effects safely on load. | ❌ | Port the corresponding behaviour tests in P1-P6 |

## Pending parity evidence

- Current mobile startup capture for side-by-side comparison; source inspection alone cannot prove appearance.
- Real mobile project packages with referenced media for P1 round trips.
- Mobile default effect/transition/overlay renders at the four required times. Current `writeContactSheets` uses different times; see `docs/P0-SOURCE-NOTES.md`.
- All section 8 behaviour tests, performance budgets, fresh-machine installation and P8 owner acceptance.

No beyond-mobile feature is implemented.
