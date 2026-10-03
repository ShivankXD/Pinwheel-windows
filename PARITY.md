# Parity checklist

P0 is owner-approved. P1 is closed with four passing owner-supplied mobile JSON/package inputs. P2 runtime and Effect Lab are implemented, but the strict pixel audit has 215 failures. Under the owner noise amendment, 18 deterministic frames and 84 noise structural frames still fail; 688 noise passes remain provisional for owner review. Mobile editing and visual parity are incomplete. Windows target: 10 22H2+ and 11, x64.

✅ means the stated check has evidence; ⚠️ means partial implementation or missing comparison evidence; ❌ means unimplemented. P1 contract checks below have evidence; full features remain partial until rendering/UI and mobile comparisons pass. Deterministic golden acceptance requires sRGB per-channel mean absolute error <= 2/255 and 99th percentile <= 8/255 at 0.3, 0.9, 1.5 and 2.1 s. The owner amended noise-driven specs to Gaussian/histogram/mean-luminance structural review; proposed limits in docs/p2-noise-policy.json require owner review.

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
| Models, codecs, atomic stores, synthetic v1..10 normalization | ✅ | `StorageContractTest`, `evidence/p1/clean-build-output.txt`; four real mobile inputs also pass |
| All 44 mobile JVM test classes ported | ✅ | 219 total desktop tests, no failures/skips; `evidence/p1/test-summary.txt`, source/test manifest |
| Command JSON, autosave failures, persisted undo, one-step drafts | ✅ | `EditCommandCodecTest`, `ProjectSessionTest` |
| Free/Plus core/export limits and account gate | ✅ | `ExportPolicyTest`, Free bypass tests; production encoder is P5 |
| Debug toggle excluded from release and shared with core policy | ✅ | `verifyBuildVariant`, `debugCheck`, `evidence/p1/debug-build-output.txt`, debug screenshot |
| Catalog IDs/defaults/assets and GLSL source preservation | ✅ | `CatalogContractTest`, `scripts/verify-p1-port.py`, 404 provenance hashes |
| Golden metrics, dimensions, per-channel p99 and strict missing gates | ✅ | `GoldenImagesTest`, expected-missing logs; no rendered parity claim |
| Real mobile project round trips | ✅ | 4 passed, 0 failed; `evidence/p1/real-mobile-projects.json`, `real-mobile-project-check.txt`; device-library projects not yet supplied |
| Mobile effect pixels and screenshots | ⚠️ | All 1688 PNGs and 21 screenshots supplied; first P2 comparison: 1411 passed, 277 failed, 0 missing |
| Hosted Windows/JDK 17 P1 build and smoke | ✅ | [Run 36981850747](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36981850747), runtime ca92d2b, `evidence/p1/ci-result.json` |
| Mobile repository unchanged | ✅ | `evidence/p1/mobile-status.txt`: exactly `?? output/` |

## P2 runtime checks

| Check | Status | Evidence |
|---|---|---|
| ANGLE program, ordered active chain, previous/trail history, exact 250 ms resets | ✅ | `EffectRuntimeTest`, asymmetric identity pixels and history/order/timing assertions; `evidence/p2/final-clean-build.txt` |
| Cut transitions, library opacity, orphan targets, six legacy kinds | ✅ | `EffectRuntimeTest`, unchanged mobile shader audit; runtime components only |
| Layer colour/coverage effects and compositing | ✅ | Targeted layer over styled main, coverage/end-gate assertions; geometry/PIP/painters remain P3/P4 |
| All catalog GLSL compiles | ✅ | 422 compiled, 0 failures; `evidence/p2/shader-compile.json` |
| Sample/SWAY preview recipe, history inputs, params, loop/cache | ✅ | `PreviewTileRendererTest`, 1688 generated frames; recipe/runtime assertions do not imply every mobile pixel passes |
| RGB565 normalized preview expansion | ✅ | Exhaustive 65,536-colour regression; baseline fails and repair passes; all 1688 actuals regenerated; `rgb565-regression-before.txt`, `rgb565-regression-after.txt` |
| Sample crop-local matrix and float bounds | ✅ | Android matrix order retained before rounding allocation; 37 new strict passes, zero pass-to-fail regressions; two sample geometry/crop/flip regressions pass; `diagnostics/sample-matrix/SAMPLE-MATRIX-REPORT.md`, `sample-matrix-runtime-checks.txt` |
| Android crop filtering constraint | ✅ | Fast source-rectangle filtering preserves neighbouring pixels during upscaling. Exact quarter-pixel boundary regression fails the published code and passes the repair; all 1688 supplied frames and channel metric rows remain unchanged. `diagnostics/sample-filter/SAMPLE-FILTER-REPORT.md`, `sample-filter-runtime-and-frames.txt`; current total 237 tests |
| GPU fixed-point portability | ⚠️ | Dither toggle does not fix the half-value mismatch; an endpoint sampler proves it exists in GPU texture storage before readback. Separate float conversion experiments repair Diamond but are not adopted: fp32 regresses seven frames; fp16 changes precision. `docs/P2-PARITY-BUGS.md`, `diagnostics/numeric/numeric-probe.json`, `diagnostics/framebuffer/framebuffer-probe.json` |
| Deterministic stage isolation | ✅ | 28 hardware hashes match production; 56 stored-input replays are byte-exact; crossed-input/backend results, raw quantization signatures, precision and translated HLSL retained under `diagnostics/deterministic` and `diagnostics/sampling`. This validates the diagnostic method, not pixel acceptance |
| Alternative 8-bit attachment isolation | ✅ | RGBA/BGRA unsized and sized formats all verify eight RGBA bits, channel upload and endpoint storage controls on NVIDIA and WARP. All 224 selected renders match their backend's default pixels and strict metrics; format changes do not repair any of the seven specs. `diagnostics/storage8/STORAGE8-REPORT.md`; diagnostic evidence only |
| Current-baseline output conversion investigation | ✅ | Four routes cover 6752 strict frames and 3088 structural comparisons; raw float captures and 8 paired worst-20 galleries retained. The staged numeric method passes 511 controls; the initial single-pass negative control fails 126. Every route has regressions or unverified precision changes and remains diagnostic. `diagnostics/framebuffer-review/FRAMEBUFFER-REVIEW.md`; production acceptance unchanged |
| Debug Effect Lab and release exclusion | ✅ | `evidence/p2/effect-lab.png`, actual-window smoke and jar variant guard |
| Mobile effect pixels | ⚠️ | Strict audit: 1473 pass / 215 fail / 0 missing. Owner class policy: 898 deterministic passes / 18 failures, 688 noise provisional passes / 84 failures. `evidence/p2/STRUCTURAL-REPORT.md`, both metric CSV sets and worst 20 galleries; acceptance remains open |
| P2 read-only source/reference provenance | ✅ | 404 P1 hashes, 7 P2 mobile source hashes, 8 legacy/layer shader literals, 1715 reference input hashes unchanged |
| Hosted Windows/JDK 17 runtime build | ✅ | [Run 37099505455](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37099505455), runtime 6ac2753; all 15 steps pass, including crop regression, clean checks, frame generation, storage/dither probes, class verification and smoke. Alternative attachment task compiles in CI; owner comparisons and its 224 controls run locally. `evidence/p2/ci-result.json` |
| Publish latest deterministic controls and run hosted CI | ✅ | Exact commits 13d23cb and 1b7c8a3 pushed by fast-forward on 2026-10-03; hosted CI succeeds for 1b7c8a3. Historical transport errors retained in `evidence/p2/PUBLICATION-STATUS.md` |

CMYK Print preserves the mobile reversed-smoothstep quirk. Reversed bounds are
undefined by the [Khronos function reference](https://raw.githubusercontent.com/KhronosGroup/OpenGL-Refpages/main/es3.0/smoothstep.xml);
this remains an open portability lead, with no shader edit, noise reclassification
or threshold change. [Stage investigation](evidence/p2/diagnostics/deterministic/DETERMINISTIC-REPORT.md).

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
| GPU effects | 277 effects, parameters, animated previews, search, Adjust and layer targeting | ⚠️ | 277 catalog contracts/GLSL preserved; command placement/swap/Free tests pass; P2 shaders/tiles/targeted layer runtime implemented; 18 deterministic and 84 structural frame failures remain; effects UI P4 |
| Overlay library | 90 timed full-frame looks with opacity and timing | ⚠️ | 80 active and 19 legacy looks preserved; pending picker swaps tested; P2 timed-look runtime implemented; strict golden report has failures; UI P4 |
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
| Project interoperability | Version 10, read 1-10, same keys/defaults/clamps/IDs; package media remapping | ⚠️ | v10 codecs, relative-media packages and synthetic round trips pass; four real mobile JSON/package inputs pass; device-library inputs pending |
| Desktop affordances | Shortcuts, wheel zoom, file drop, context menus, DPI, saved window position, fullscreen/Esc | ❌ | P4/P7; no editor affordance claim |
| Platform distribution | Storage/settings, crash reports, MSIX/MSI and update channel | ❌ | P7; paths defined but services/installers absent |

## Catalog item groups

All groups below have ported IDs, defaults and assets verified by CatalogContractTest and the provenance audit. All 422 shaders compile and draw; 354 specs pass every strict golden time, while 68 specs fail. The owner class policy separately has seven failing deterministic specs and 33 failing noise specs. Full visual acceptance remains open in P2. Active overlays number 80, with 19 legacy IDs retained; the brief's 90 is stale. Trending categories contain curated subsets, not additional unique catalog IDs.

| Catalog group | Mobile behaviour | Desktop status | Evidence or remaining work |
|---|---|---|---|
| Effects: Trending | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Montage | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: 3D | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Pixel | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Neon | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Whimsical | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Classic | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Intro & Outro | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Party | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Motion | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Light | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Split | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Retro | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Glitch | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Celebrate | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Effects: Graffiti | 277 effect IDs; exact GLSL/default params and timed previews | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Transitions: Trending | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Transitions: Basic | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Transitions: Camera | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Transitions: Slide | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Transitions: Mask | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Transitions: Effects | 46 transition IDs; cut timing and exact shader pixels | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Trending | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Atmosphere | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Light | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Background | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Scenery | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Transitions | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Intro & End | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Elements | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Texture | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
| Overlay looks: Frames | 80 active + 19 legacy look IDs; category membership, opacity and timing; mixed sticker cards preserved | ⚠️ | CatalogContractTest + unchanged GLSL hashes; all shaders compile; per-spec results in `evidence/p2/per-spec-metrics.csv`; visual acceptance/UI incomplete |
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

## Noise-driven specs under the owner amendment

193 specs are flagged from active shader branches and reachable noise helpers. Unused pure noise variables are excluded. Classification is independent of image failures. The original strict audit remains available. Each row lists provisional metric checks over every RGBA channel and all four times; parity remains partial until owner review and remaining bugs are resolved. Ordinary sin/cos movement without noise stays deterministic.

| Spec | Class | Parity | Provisional metric checks | Evidence |
|---|---|---|---|---|
| fx-shake | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-slash-reveal | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-diamond-zoom | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-error-quake | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-explosion | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-scene-cut | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-shiny-stack | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-glass-breaking | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-negative-panels | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-fireplace | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-vignette-noir | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-lightning-crack | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-vignette | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-offset-slice | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-feverish | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-tension-zoom | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-cut-shift | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-move-cloud | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-chaotic-heat | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-magical-tome | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-grim-reaper | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ct-phone-zoom | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-retro-flicker | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-handheld | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-wiggle-flicker | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-hex-split | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-heart-ascent | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-butterfly | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-butterfly-dream | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-leak-warm | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-leak-neon | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-falling-petals | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-smoky-focus | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-vintage-film | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-stellar | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-ink-spill | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-exploding-love | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-damaged-vignette | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-firefly | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-misty-tint | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-sepia-cool | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-snow-night | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-rain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-rose-bloom | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-sparkle-shine | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-pixel-blocks | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-pixel-mutant | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cc-pixel-universe | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-pixel-scan | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-flip-phone | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-pixel-breakdown | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-sweet-party | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-misty-tint | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-bead-art | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pb-pixel-rain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-st-polaroid | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-st-retro-tv | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-st-billboard | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-st-film-strip | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ne-frame | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ne-ring | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ne-heart | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ne-lightning | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ne-stars | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-sparkle-burst | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-glitter-rain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-matrix | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-signal-lost | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-slice-drift | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-projector | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-flash-cut | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-aurora | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-twinkle-sky | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-cr-tri-split | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-mt-photo-snap | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-mt-zoom-shake | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-globe | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-hyperspace | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-star-rush | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-camera-shake | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-film-grain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-glitch-in | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-film-burn | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-torn-reveal | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-mirror-ball | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-confetti | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-balloons | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-earthquake | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-light-leak | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-bokeh | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-grid-flash | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pencil-sketch | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-marker-lines | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-spray-neon | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-chalkboard | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-scribble-frame | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-confetti-burst | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-gold-glitter | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-heart-rain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-bubbles | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-star-pop | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-petals | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-snowfall | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-eight-bit | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pixel-breakdown | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-pixel-glitch | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-digital-blocks | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-vhs | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-scanline-jitter | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-datamosh | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-signal-loss | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-bad-tv | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-glitch-flash | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-vhs-rewind | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-film-8mm | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-old-tv | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-film-burn-leak | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-seventies | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-dust-scratches | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-tr-slam-merge | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-tr-heat-flicks | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-tr-shake | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-tr-glitch | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-tr-flare | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-tr-film-burn | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-glitter-rain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-gold-dust | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bokeh-gold | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bokeh-pink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sparks | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-embers | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-smoke | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-fog-white | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-galaxy | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-fireflies | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-magic-dust | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sparkle-white | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-glitter | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-hearts-pink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-stars | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-confetti | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-snow | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-rain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bubbles | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-leak-warm | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-leak-pink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-leak-blue | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-light-streaks | noise-driven | ⚠️ | blur FAIL, histogram FAIL, channel mean FAIL, luminance FAIL | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-stage-lights | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-purple-smoke | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-pastel-ink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-pink-gradient | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-blue-hearts | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-pink-bokeh | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-golden-band | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-blue-smoke | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-light-wall | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-3d-shapes | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bg-red-hearts | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-aurora | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-milky-way | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-sunset-sea | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-moon-dunes | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-clouds | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-snowy-night | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-ocean | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sc-shooting-stars | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-tr-shape-pop | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-tr-glow-blob | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-tr-fire-wipe | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-tr-ink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-tr-light-burst | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-tr-scribble | noise-driven | ⚠️ | blur FAIL, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-el-speed-lines | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-el-lightning | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-el-moon | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-el-smoke-puff | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-film-dust | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-grain | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-vhs | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-paper | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-burnt-edges | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-leak-green | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-leak-purple | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-leak-red | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-lightning | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bokeh-blue | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-bokeh-rainbow | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sparkle-pink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-sparkle-blue | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-hearts-red | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-fog-pink | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |
| fx-ov-fog-blue | noise-driven | ⚠️ | blur PASS, histogram PASS, channel mean PASS, luminance PASS | `evidence/p2/STRUCTURAL-REPORT.md`, per-channel CSV and heatmaps |

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
- P2 has all required real mobile goldens. Resolve 18 deterministic strict failures and 84 noise structural failures; confirm provisional structural limits. See `docs/P2-PARITY-BUGS.md`.
- Device-library JSONs were not supplied. The four raw/package inputs pass and close P1.
- All section 8 behaviour tests, performance budgets, fresh-machine installation and P8 owner acceptance.

No beyond-mobile feature is implemented.

P1 mobile quirks: only the matching history is saved; inactive cutout backgrounds are omitted; deleting an image keeps orphaned effect targets; LibraryStorage ignores photo cutout image references during original cleanup. See docs/P1-PORT-NOTES.md. These behaviours were preserved.
