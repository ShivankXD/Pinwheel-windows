# P3 mobile scenario coverage

These are engine checkpoints. A frame assertion does not replace an encoded-file
assertion, and a stress run without the mobile painters does not close P3.
`docs/p3-port-manifest.json` pins the three original Android test sources.

| Mobile scenario | Desktop evidence | Remaining acceptance gap |
|---|---|---|
| VideoStillTransitionPlaybackTest, all 16 named cases | Same method names, red/blue/green 360x640 JPEG inputs, split at 1.629 s, same FX windows and Pull In transition; 270x480/60 fps, progress >=2800 ms within 6 s; prepare/seek/play order | Optional personal phone original is replaced by bundled demo.jpg, also present in owner packages. Injected PCM device clock; real hardware output separately tested in Player Lab |
| VideoPipTest delayed trimmed moving layer, transparent Circle mask | Byte-identical blue/red/green MP4 inputs; times 400/1400/2600/3500 ms; original dominant-channel >190 and other-channel <65 limits; mask corners | Original Transformer encoded-file validation belongs to P5 |
| VideoPipTest overlap/opacity/animation | Original stack sample locations, opacity 95..170 and fade boundary assertions in VideoPipTest | Encoded-file validation belongs to P5 |
| VideoPipTest playback/scrub and ended layers | Player Lab consumes actual Windows PCM frames through 2.6 s and seeks to 1.4 s; ended-input identity regression | Full mobile cached-prerender workflow and editor interaction remain |
| VideoStressTest heavy project | Ported deterministic factory: eight clips, 40 effects, three sounds, video PIP, sticker/title/caption/photo metadata. Actual GPU engine at 720x1280 plays through 25 s; unchanged >=24 fps local gate and <=2500 ms published-frame-gap gate | Sticker/title/caption/photo painting is absent, so this is explicitly engine-only. Full painted heavy-project acceptance remains open |
| VideoStressTest many seeks and rapid edits | Native decoder 26 seek requests, player rapid seek final-generation assertion, 30 rapid edits with 120 ms debounce | Existing rapid edit test uses a smaller project. Port full heavy-project 25-seek/edit matrices and repeated audio-cut scenarios |
| VideoStressTest sounds end before picture | PCM sample assertions, seeks/starts at 11.9/12.411/15 s advance past early sound end, silence until movie end | Full original repeated audio-cut matrix remains |
| VideoStressTest ending, 720p/1080p durations, every effect stacked 40 exports | P1 export limit policy is enforced; matching preview/export frame inputs have identical RGBA bytes | Encoder, full ending painter, actual duration/non-black output checks are P5; no exported movie claimed |
| Grade/canvas/motion source port | Four unchanged shader literals, complete recipe section, needsGrade and overlayPose audited; rotation/mirror/orientation and shared preview/export assertions | Dedicated rendered mobile engine probes, HDR tone mapping, HEIC/EXIF and full-size export decode budgets remain |
| Audio time/pitch/volume/fade/clip mute | Unchanged Media3 Sonic DSP; independent 440 Hz frequency/duration checks; exact constant-sample trim/delay/fade/gain/mixing and silent-tail checks | Combined voice/speed/DSP pipeline output order needs mobile audio qualification |

The debug Player Lab is a P3 harness. Its paired mobile editor screenshot gives
phase context; it is not a claim of P4 UI appearance parity. The public evidence
contains no private gallery thumbnails or phone filesystem originals.
