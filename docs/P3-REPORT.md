# P3 video engine checkpoint

The owner authorized P3 on 2026-10-03 while P2 acceptance remains open. This is
an implementation checkpoint, not completed P3 or P8 acceptance. P2 production
pixels, deterministic limits and provisional noise limits remain unchanged.

## 1. Built

- **pinwheel-media:** persistent in-process libav/JNI decoder with D3D11VA and
  reported software fallback, independent bounded video/PCM queues, accurate
  generation-tagged seeks, still reuse and cancellation. VideoFrameSource adds
  an eight-cursor LRU with separate main/PIP video clocks. Up to two upcoming
  inputs prepare off the GPU thread, counted inside that same cursor limit. TimelineAudioMixer
  produces 48 kHz stereo float PCM with trim, speed, pitch, volume, fades and
  silence padding. VideoPlayer separates audio and GL workers, uses consumed
  JavaSound PCM frames as its master clock, drops slow video ticks, and coalesces
  project edits for 120 ms.
- **pinwheel-render:** GpuFrameEvaluator shares one recipe for preview/export
  input frames: source clock, rotation/mirror, crop, mobile grade/legacy matrix,
  canvas Fit/Fill/blur, timed PIP masks/geometry/animation/stacking, clip motion,
  existing P2 effects and caller-painted targeted layers. Preview sizing uses a
  720 px short side. Inactive GPU passes are borrowed without changing pixels.
- **native/media:** C++ bridge dynamically links the already-pinned LGPL FFmpeg
  8.1 shared libraries. Bootstrap verifies and extracts matching SDK headers and
  builds with Visual Studio 2022/CMake. No playback subprocess or GPL codec
  runtime is added. The Media3 1.11.1 Sonic DSP source and Apache-2.0 notice are
  retained with a hash audit allowing only dependency/package adaptations.
- **pinwheel-app:** debug-only Player Lab has real audio output, play/pause,
  scrub/restart and moving circular PIP. Jar checks exclude both Labs and the
  debug entitlement toggle from release. Production editor UI remains P4.
- **core/platform/photo:** existing P1 policies and tests remain intact; no
  beyond-mobile features are implemented.

## 2. Tests and failures

```text
Core:     205 passed
Platform:   2 passed
Media:     19 passed
Render:    57 passed
Total:    283 passed, 0 failures, 0 errors, 0 skipped
All 16 named still-transition playback scenarios passed
Local engine-only stress: 55.32 fps; maximum frame gap 142.07 ms
Unchanged local limits: >=24 fps, <=2500 ms frame gap; PASS
35 inactive/history/legacy/glow RGBA byte-exact regression comparisons passed
Real hardware audio-clock playback reached 2.6 s; paused 1.4 s PIP seek passed
Local Player Lab first frame: 1101.34 ms; warmed seek: 29.02 ms
Source audit: 404 P1 hashes, eight P2 literals/seven source hashes,
             nine P3 sections/assets/nine sources; Sonic DSP unchanged
```

[Full runtime output](../evidence/p3/runtime-tests-output.txt),
[per-module summary](../evidence/p3/test-summary.json),
[stress metrics and retained limits](../evidence/p3/performance.json),
[current stress test output](../evidence/p3/stress-current-output.txt),
[real audio/window output](../evidence/p3/player-lab-output.txt),
[source audit](../evidence/p3/source-audit.txt).

The initial engine run produced 224 frames in 25.43 s, about 8.8 fps, below
24 fps. The complete [initial performance output](../evidence/p3/stress-initial-performance.txt)
is retained. Removing unnecessary inactive/neutral full-frame passes raised the
measured local rate; 35 byte-exact comparisons verify output preservation.
No shader, active-stage effect or threshold was changed to obtain that result.

The initial cold first activation of the PIP input took **204.42 ms**, exceeding
the unchanged 150 ms seek budget. A separate bounded input worker now prepares
upcoming decoders while the GPU graph is built; startup also prepares the same
PIP mask pixels. Current first activation is **41.51 ms** and the repeat is
33.97 ms. The real Player Lab warmed seek is 29.02 ms. All four PNG files remain
byte-identical to the published prefetch baseline. This local fixture passes;
arbitrary cache misses and multi-layer/target hardware latency remain open.
See [before/after timings](../evidence/p3/latency.json),
[pixel hashes](../evidence/p3/prefetch-pixel-regression.json),
[preparation tests](../evidence/p3/prefetch-tests-output.txt) and
[individual stage timings](../evidence/p3/engine-frames.json).
The local first-frame probe is one sample, not broad hardware qualification.

Other full failing outputs are retained:
[initial decoder test](../evidence/p3/decoder-tests-initial-failure.txt) assumed an
incorrectly non-silent demo intro; an independent known PCM tone replaces that
assumption. [Initial playback compilation](../evidence/p3/playback-tests-initial-output.txt)
had a Kotlin test declaration typo. [Initial frame evidence compilation](../evidence/p3/frame-evidence-initial-failure.txt)
used the wrong metadata field name. These mistakes were corrected; limits were
not relaxed. FFmpeg warns about deprecated JPEG pixel format; conversion sets
colour range explicitly and current pixel assertions pass. SDK headers emit
conversion warnings but build successfully.

The added mono resampling test exposed a real amplitude mismatch: default
libswresample mono upmix attenuated the signal, while mobile uses constant gain.
An explicit unity stereo matrix fixes the bridge; the independent test keeps
the .25 amplitude requirement and verifies exactly 4800 output frames from
100 ms of 44.1 kHz input, including resampler drain. Full
[initial console output](../evidence/p3/mono-gain-initial-failure.txt) and
[failure stack](../evidence/p3/mono-gain-initial-failure.xml) are retained.
The [native configure attempt](../evidence/p3/native-configure-initial-failure.txt)
failed because JAVA_HOME was unset; rerunning with the installed JDK succeeded.
Bootstrap now validates JNI headers immediately and the README lists this
requirement.

P3 tests use a bounded injected consumed-PCM clock. The Player Lab separately
uses real Windows JavaSound output. Local stress is NVIDIA RTX 4060 Laptop/D3D11;
no Intel Iris Xe result or complete painted heavy-project result is claimed.
Other multichannel layouts remain unqualified.
The [test map](P3-TEST-MAP.md) distinguishes retained mobile assertions from
frame-only adaptations, reduced edit fixtures and future encoder tests.

## 3. Visual evidence

[Actual Player Lab](../evidence/p3/player-lab.png),
[paired mobile editor context](../evidence/p3/mobile-player-lab.png),
[engine PIP frames](../evidence/p3/frames/pip),
[frame timings/backend descriptions](../evidence/p3/engine-frames.json).
The pair is explicitly a P3 harness beside a P4 editor reference, not editor UI
parity. Only screenshot 07 is copied into the pair; private gallery screenshot
06 is not published. Known-colour PIP fixtures are byte-identical mobile test
assets, not fabricated mobile goldens.

## 4. Parity and remaining work

[PARITY.md](../PARITY.md) and [P3 test map](P3-TEST-MAP.md) record evidence and gaps.
P3 remains open for full sticker/title/caption/photo painters, the full heavy
seek/edit/audio-cut matrices, cold cache-miss latency qualification, HDR-to-SDR tone
mapping, combined voice/speed/DSP output qualification and target hardware
budgets. HDR PQ/HLG is explicitly rejected currently; silently wrong SDR is not
accepted. HEIC/EXIF handling and full-size 4K decode budgets remain unqualified.
Main clips apply stream rotation metadata. PIP currently applies only the layer
rotation; its source display-matrix rotation still needs a per-input stage before
target FX. The supplied PIP tests have no rotation metadata and do not prove that
case. This is an open P3 behaviour bug, not an accepted mobile difference.
Encoder, ending card and actual 720p/1080p/every-effect export assertions are P5.

P1's four real inputs remain passing. P2 still has 215 strict-audit failures;
under the owner amendment, 18 deterministic frames and 84 provisional structural
frames fail. The 688 structural passes remain provisional. Current gate output
is retained in [P1/P2 checks](../evidence/p3/p1-p2-gates-output.txt); no production
frame or comparison threshold is replaced by P3 fixtures.

## 5. Owner inputs

P2 phone GL_RENDERER/GL_VERSION, precision/raw/neutral probes, device-library
JSONs and owner review of provisional noise limits are still pending. Google
Desktop OAuth client ID is needed before P7. Shared entitlement/backend direction
is recorded. No new owner permission is needed for this engine checkpoint.

## 6. Read-only inputs and publication

Mobile HEAD remains 2a417fd29ef43a8792eb4d58cbe4d102327b11c9 and status exactly
`?? output/`. It was not built or modified. All 1715 reference files retain their
pinned SHA-256 values. [Read-only audit](../evidence/p3/read-only-inputs.json).
Every checkpoint uses the owner's configured author/committer identity, no
em dash or AI attribution trailer, and is pushed immediately. Hosted clean-build
evidence for checkpoint 3ec3d61 passed all 15 hosted steps with JDK 17;
[run 37120115705](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37120115705)
and [step/artifact record](../evidence/p3/ci-checkpoint-3ec3d61.json).
Prefetch/mono-gain code checkpoint fa29207 also passed all 15 hosted steps;
[run 37121272000](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37121272000)
and [exact-commit record](../evidence/p3/ci-checkpoint-fa29207.json).
The final report/evidence commit changes documentation only; both code
checkpoints were pushed immediately and are independently validated.
