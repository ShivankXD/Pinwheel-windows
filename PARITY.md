# Parity checklist

P0 is owner-approved. P1 core implementation passes local checks; acceptance awaits real mobile projects. Mobile editing and visual parity are incomplete. Windows target: 10 22H2+ and 11, x64.

✅ means the stated check has evidence; ⚠️ means partial implementation or missing comparison evidence; ❌ means unimplemented. P1 contract checks below have evidence; full features remain partial until rendering/UI and mobile comparisons pass. Golden acceptance requires sRGB per-channel mean absolute error <= 2/255 and 99th percentile <= 8/255 at 0.3, 0.9, 1.5 and 2.1 s.

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

## P1 contract checks

| Check | Status | Evidence |
|---|---|---|
| Models, codecs, atomic stores, synthetic v1..10 normalization | ✅ | `StorageContractTest`, `evidence/p1/clean-build-output.txt`; real mobile packages remain pending |
| All 44 mobile JVM test classes ported | ✅ | 219 total desktop tests, no failures/skips; `evidence/p1/test-summary.txt`, source/test manifest |
| Command JSON, autosave failures, persisted undo, one-step drafts | ✅ | `EditCommandCodecTest`, `ProjectSessionTest` |
| Free/Plus core/export limits and account gate | ✅ | `ExportPolicyTest`, Free bypass tests; production encoder is P5 |
| Debug toggle excluded from release and shared with core policy | ✅ | `verifyBuildVariant`, `debugCheck`, `evidence/p1/debug-build-output.txt`, debug screenshot |
| Catalog IDs/defaults/assets and GLSL source preservation | ✅ | `CatalogContractTest`, `scripts/verify-p1-port.py`, 404 provenance hashes |
| Golden metrics, dimensions, per-channel p99 and strict missing gates | ✅ | `GoldenImagesTest`, expected-missing logs; no rendered parity claim |
| Real mobile project round trips | ⚠️ | Input absent; `evidence/p1/reference-status.json`, strict gate fails |
| Mobile effect pixels and screenshots | ⚠️ | 1688 cases missing; P2 runtime and owner references pending |
| Hosted Windows/JDK 17 P1 build and smoke | ✅ | [Run 36981850747](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36981850747), runtime ca92d2b, `evidence/p1/ci-result.json` |
| Mobile repository unchanged | ✅ | `evidence/p1/mobile-status.txt`: exactly `?? output/` |

## Feature inventory from brief section 1.1

| Feature | Mobile behaviour | Desktop status | Evidence or remaining work |
|---|---|---|---|
| Startup intro | Spinning fan artwork, stick geometry, appearance and letter timing | ⚠️ | P0 source port and desktop screenshot; current mobile visual reference pending |
| Welcome and Google sign-in | Google authentication; export requires sign-in | ⚠️ | AuthProvider and signed-out fake tested; real OAuth/welcome UI P7 |
| Home | New video, Edit photo, project cards, Open/Rename/Delete, freed storage message, Trending and Templates | ❌ | P4; no home screen implemented |
| Media picker | Video/photo multi-select and canvas presets | ❌ | P4; no picker implemented |
| Settings | Cards, Plus, storage manager, licences and export folder | ⚠️ | P0 copied-notice dialog only; all other settings and P7 packaging pending |
| Plan badge and Plus page | Free pill, gold Plus crown and subscription page | ⚠️ | Debug-only entitlement toggle and release guard tested; production page/backend P7 |
| Multi-track timeline | Mixed clips, effects, overlays/stickers/text, audio, captions, ruler, filmstrips, trim/move/zoom; 64 lanes | ⚠️ | Models, drag/trim/lane/snapping math and JVM assertions ported; engine/UI P3/P4 |
| Clip tools | Split, Extract audio, Volume, Speed 0.25-4x, Delete, Duplicate, Replace, Crop/zoom, Rotate/mirror, grade, Looks, Motion and transitions | ⚠️ | Commands cover clip edits, extraction, split/trim/duplicate/move and apply-all; render/UI P3/P4 |
| GPU effects | 277 effects, parameters, animated previews, search, Adjust and layer targeting | ⚠️ | 277 catalog contracts/GLSL preserved; command placement/swap/Free tests pass; GPU/goldens/UI P2/P4 |
| Overlay library | 90 timed full-frame looks with opacity and timing | ⚠️ | 80 active and 19 legacy looks preserved; pending picker swaps tested; GPU/UI P2/P4 |
| File overlays and PIP | Photo/video layers, masks, border, shadow, opacity, transforms and in/out animation | ⚠️ | Models/codecs/timing/commands tested; PIP rendering/UI P3/P4 |
| Animated stickers | Time-based vector stickers; settled paused pose and animated playback/export | ⚠️ | IDs/categories/metadata ported; deterministic drawing and paused pose P2/P4 |
| Text and titles | Styles, colours, backgrounds, animations, templates and canvas-scaled typography | ⚠️ | Title/style/motion catalogs and models/JVM tests ported; painters/UI pending |
| Captions | Auto speech captions, SRT, word times, styles, built-in/imported motion templates and 17 fonts | ⚠️ | SRT, times, words, styles, template catalogs and 17 TTF/15 license assets ported; speech/painters/UI pending |
| Audio | 16 tracks, Sounds/Openverse, art/waveforms/search/moods/Commercial/saved/credits, bundled SFX, device/extracted audio, voiceover, volume/fades/speed/DSP/denoise | ⚠️ | Track/voiceover/DSP/envelope contracts and JVM tests ported; native recording/mixer/library UI P3/P4 |
| Canvas | Aspect ratios, Fit/Fill, solid or blurred background | ⚠️ | Sanitized model and sizing math tested; compositing/UI P3/P4 |
| Undo and redo | Persisted history, gesture drafts and one commit on release | ⚠️ | Persisted histories, autosave, gesture freeze/commit/cancel and reload tests pass; editor UI P4 |
| Video preview | Playback, scrubbing, fullscreen and live overlays | ❌ | P3/P4; one decoded frame is not playback |
| Video export | 480p through 4K, 24/25/30/50/60 fps, bitrate slider/size, 2.5 s ending, Movies/Pinwheel, progress/result | ⚠️ | Free/Plus and signed-in export policy tested; encoding/muxing/progress P5 |
| Photo filters | Approximately 75 looks with amount and saved user presets | ⚠️ | Looks/preset catalogs and pure transform assertions ported; bitmap preview/UI P6 |
| Photo adjust | Dial, light/colour, curves, HSL/picker, grading, detail, dehaze, vignette/grain, WB and Magic Enhance | ⚠️ | Colour/curve/HSL/grading/dehaze/WB/Enhance pure math and tests ported; complete pipeline/UI P6 |
| Photo masks | Brush, linear and radial local adjustments | ⚠️ | Pure mask/coordinate math tested; bitmap pipeline/UI P6 |
| Photo cutout | Erase/restore/tap select, edges/sensitivity/softness and replacement background | ⚠️ | Mask math and recipes tested; bitmap replacement/pipeline/UI P6 |
| Photo retouch | Clone and heal spots | ⚠️ | Clone blend math and recipe codecs tested; complete clone/heal pipeline/UI P6 |
| Photo crop and lens | Crop/straighten/rotate/flip, guided perspective, lensfun distortion/TCA/vignette | ⚠️ | Crop/perspective/lens/TCA math and 35-profile tests ported; pipeline/UI P6 |
| Photo frame | Even/aspect borders, colours, blur and width | ⚠️ | Registry/layout and codec tests pass; drawing/blur/UI P6 |
| RAW development | Same LibRaw decoder, linear precision renderer and Ultra HDR | ⚠️ | Linear colour and RGB16 TIFF writer tests pass; LibRaw/precision pipeline/Ultra HDR P6 |
| Photo compare, versions and export | Hold/split, fullscreen before/after, histogram, versions, adjustment copy/paste, presets/info; JPEG/PNG/WebP/TIFF/Ultra HDR sizes | ⚠️ | Atomic version/preset/recipe storage ported; compare/info/formats UI and complete export P6 |
| Free plan | All effects blocked with Plus dialog; enforced 1080p/30 fps/8 Mbps, ending always on | ⚠️ | Core rejects effect injection; policy caps 1080p/30fps/8Mbps and forces ending; UI/encoder P4/P5 |
| Plus plan | 4K/60 fps/100 Mbps; optional ending off by default | ⚠️ | Policy allows 4K/60fps/100Mbps and optional ending; debug provider tested; backend P7 |
| Subscription offers | Mobile monthly/yearly plans and first-month offer; desktop entitlement policy to be chosen | ⚠️ | Owner chose Google-account shared phone/PC Plus, later backend; no production billing yet |
| Project interoperability | Version 10, read 1-10, same keys/defaults/clamps/IDs; package media remapping | ⚠️ | v10 codecs, relative-media packages and synthetic round trips pass; real mobile inputs pending |
| Desktop affordances | Shortcuts, wheel zoom, file drop, context menus, DPI, saved window position, fullscreen/Esc | ❌ | P4/P7; no editor affordance claim |
| Platform distribution | Storage/settings, crash reports, MSIX/MSI and update channel | ❌ | P7; paths defined but services/installers absent |

## Catalog item groups

All groups below have ported IDs, defaults and assets verified by CatalogContractTest and the provenance audit. Shader compilation/drawing and golden pixel acceptance remain P2 work. Active overlays number 80, with 19 legacy IDs retained; the brief's 90 is stale. Trending categories contain curated subsets, not additional unique catalog IDs.

| Catalog group | Mobile behaviour | Desktop status | Evidence or remaining work |
|---|---|---|---|
| Effects: Trending | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Montage | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: 3D | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Pixel | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Neon | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Whimsical | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Classic | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Intro & Outro | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Party | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Motion | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Light | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Split | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Retro | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Glitch | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Celebrate | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Effects: Graffiti | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Transitions: Trending | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Transitions: Basic | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Transitions: Camera | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Transitions: Slide | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Transitions: Mask | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Transitions: Effects | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Trending | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Atmosphere | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Light | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Background | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Scenery | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Transitions | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Intro & End | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Elements | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Texture | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Overlay looks: Frames | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Trending | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Social | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Text | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Emoji | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Chat | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Love | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Arrows | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Doodle | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Party | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Elements | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Animated stickers: Intro & End | Same IDs and deterministic drawing/motion; settled paused pose | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Geometric stickers: Marks | Same static geometry and saved overlay IDs | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Geometric stickers: Shapes | Same static geometry and saved overlay IDs | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Geometric stickers: Frames | Same static geometry and saved overlay IDs | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Video looks | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Title styles and motion | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Title templates | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Built-in caption templates | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Imported caption packages | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Caption fonts (17 TTF + 15 licenses) | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Bundled sound effects | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: Home photo templates | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Other catalogs: User presets | All mobile entries, parameters, assets and applicable notices | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Aesthetic | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Cinematic | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Film | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Food | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Landscape | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Monochrome | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Moody | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Night | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Portrait | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Seasons | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Social | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Travel | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |
| Photo looks: Vintage | Same look parameters and rendering | ⚠️ | CatalogContractTest + source/asset hashes; render/UI/mobile goldens pending |

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

P1 mobile quirks: only the matching history is saved; inactive cutout backgrounds are omitted; deleting an image keeps orphaned effect targets; LibraryStorage ignores photo cutout image references during original cleanup. See docs/P1-PORT-NOTES.md. These behaviours were preserved.
