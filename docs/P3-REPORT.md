# P3 video engine checkpoint

The owner authorized P3 on 2026-10-03 while P2 acceptance remains open. P2
production pixels, strict limits and provisional noise limits are unchanged.
This report records implementation checkpoints, not completed phase acceptance.

## 1. Built

`pinwheel-media/LibavDecoder.kt` implements the P1 in-process decoder contract.
`native/media/media_bridge.cpp` binds the already-pinned shared LGPL FFmpeg
8.1 SDK through JNI. `scripts/build-media-native.ps1` verifies and extracts
that SDK and builds the bridge with Visual Studio 2022. Bootstrap builds it
locally and on hosted Windows; no JavaCPP GPL codec runtime is introduced.

Video and audio have independent persistent demux/codec contexts. D3D11VA
frames transfer to RGBA for ANGLE upload; software fallback is explicit in
configuration and description. Decode output can be downscaled. Stereo float
PCM is 48 kHz. Separate frame/block/byte budgets stop video backpressure from
blocking audio. Seek requests flush queues and tag delivery with a generation;
workers seek to a previous keyframe and decode through the requested timestamp.
Stills decode once and reuse the same bytes across split/trim seeks. Cancellation
interrupts libav, wakes queue waits, joins workers and then frees contexts.

Frame evaluation, clocked playback, PIP and the ported device-test scenarios
are being implemented next. P5 encoder/export assertions remain future work.

## 2. Tests

```text
Media tests: 10 passed, 0 failures, 0 errors, 0 skipped
Real demo: D3D11VA, 960x720 RGBA, 48 kHz stereo PCM
Rapid seeks: 25 superseded requests plus final accurate generation passed
Video queue saturation: 20 audio blocks consumed without draining video passed
Known PCM tone: stereo sample values match independently generated 440 Hz input
Still seek: decoded pixel array reused at 1.629 s passed
Cancellation: no remaining libav worker threads passed
```

[Native build output](../evidence/p3/native-build-output.txt),
[decoder test output](../evidence/p3/decoder-tests-output.txt).
The initial test incorrectly assumed the demo's first audio block was audible.
That assertion failed; the full [initial output](../evidence/p3/decoder-tests-initial-failure.txt)
is retained. An independent generated PCM tone now verifies actual sample
conversion rather than assuming a non-silent movie intro. Queue and seek
assertions are retained. Third-party SDK headers emit conversion warnings;
the bridge builds successfully.

## 3. Visual evidence

No new editor screen is claimed in this decoder checkpoint. P3 playback/frame
evidence will accompany the frame evaluator. Existing P2 paired mobile screens
and golden heatmaps remain in `evidence/p2`.

## 4. Parity

`PARITY.md` records the in-process decoder evidence and P3's remaining scope.
P2 remains at 18 deterministic and 84 provisional structural frame failures.

## 5. Owner inputs and limits

Phone GPU/precision and raw/neutral probes, device-library JSONs, and review of
provisional structural limits are still pending for P2. These do not block P3
engine work. Google Desktop OAuth is still needed before P7. No phone or
emulator action is authorized or performed.

## 6. Read-only inputs and publication

Mobile HEAD is `2a417fd29ef43a8792eb4d58cbe4d102327b11c9`, status exactly
`?? output/`. It has not been built or modified. Reference inputs are read-only.
Every checkpoint is committed with the owner identity and pushed immediately.
