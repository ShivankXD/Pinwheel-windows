# Source-based noise classification

Owner amendment: noise-driven specs use structural comparisons for owner review;
deterministic specs retain strict per-pixel MAE/p99. Shared shaders stay unchanged.
193 noise-driven specs; 229 deterministic specs.

The classifier selects each spec's active MODE preprocessor branch and traverses
calls from fx(). A reachable hash/noise/rand helper or inline fract(sin(...))
flags the spec. Ordinary sin/cos motion without hash noise stays deterministic.
Unused pure local assignments are removed from the analysis so noise in an
unused shared prelude cannot reclassify a deterministic output.
Classification is independent of golden results: passing noise specs are included.
The JSON records active bodies, helper paths and complete-source hashes for review.
Runtime parameter branches are conservatively included; this is a source-based
class of the spec, not a claim that noise contributes at every timestamp.

Structural comparisons use a separable Gaussian with sigma 8 px, radius 24,
edge clamping, unrounded float sRGB8 channels, channel histogram Wasserstein-1
distance and absolute channel mean error. Luminance uses Rec.709 RGB weights.
Metric limits and owner-review results are in the current structural report.
Strict results remain available separately with their original heatmaps.

| Spec | Noise helper call paths |
|---|---|
| fx-shake | fx -> hash1; fx -> hash2; fx -> hash2 -> hash |
| fx-cc-slash-reveal | fx -> hash1 |
| fx-cc-diamond-zoom | fx -> hash; fx -> hash2 |
| fx-ct-error-quake | fx -> hash; fx -> hash1 |
| fx-ct-explosion | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ct-scene-cut | fx -> hash1; fx -> hash2; fx -> hash2 -> hash |
| fx-cc-shiny-stack | fx -> hash1 |
| fx-ct-glass-breaking | fx -> hash1; fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ct-negative-panels | fx -> hash1 |
| fx-ct-fireplace | fx -> hash; fx -> hash1; fx -> hash2 |
| fx-ct-vignette-noir | fx -> hash |
| fx-ct-lightning-crack | fx -> hash1; fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-vignette | fx -> hash |
| fx-ct-offset-slice | fx -> hash; fx -> hash1 |
| fx-cc-feverish | fx -> hash1 |
| fx-ct-tension-zoom | fx -> vnoise; fx -> vnoise -> hash |
| fx-ct-cut-shift | fx -> hash1 |
| fx-cc-move-cloud | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ct-chaotic-heat | fx -> hash1; fx -> hash2; fx -> hash2 -> hash |
| fx-ct-magical-tome | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ct-grim-reaper | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ct-phone-zoom | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-retro-flicker | fx -> hash; fx -> hash1 |
| fx-cc-handheld | fx -> vnoise; fx -> vnoise -> hash |
| fx-cc-wiggle-flicker | fx -> hash1; fx -> hash2; fx -> hash2 -> hash |
| fx-cc-hex-split | fx -> hash |
| fx-cc-heart-ascent | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cc-butterfly | fx -> hash1 |
| fx-cc-butterfly-dream | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cc-leak-warm | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-leak-neon | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-falling-petals | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cc-smoky-focus | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-vintage-film | fx -> hash; fx -> hash1 |
| fx-cc-stellar | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cc-ink-spill | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-exploding-love | fx -> hash1 |
| fx-cc-damaged-vignette | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-firefly | fx -> hash1 |
| fx-cc-misty-tint | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cc-sepia-cool | fx -> hash |
| fx-cc-snow-night | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cc-rain | fx -> hash |
| fx-cc-rose-bloom | fx -> hash1 |
| fx-cc-sparkle-shine | fx -> hash |
| fx-cc-pixel-blocks | fx -> hash |
| fx-cc-pixel-mutant | fx -> hash; fx -> hash2 |
| fx-cc-pixel-universe | fx -> hash |
| fx-pb-pixel-scan | fx -> hash |
| fx-pb-flip-phone | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-pb-pixel-breakdown | fx -> hash |
| fx-pb-sweet-party | fx -> hash1 |
| fx-pb-misty-tint | fx -> hash |
| fx-pb-bead-art | fx -> hash |
| fx-pb-pixel-rain | fx -> hash |
| fx-st-polaroid | fx -> vnoise; fx -> vnoise -> hash |
| fx-st-retro-tv | fx -> hash1; fx -> vnoise; fx -> vnoise -> hash |
| fx-st-billboard | fx -> hash |
| fx-st-film-strip | fx -> hash1 |
| fx-ne-frame | fx -> flick -> hash1 |
| fx-ne-ring | fx -> flick -> hash1 |
| fx-ne-heart | fx -> flick -> hash1 |
| fx-ne-lightning | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ne-stars | fx -> hash1 |
| fx-cr-sparkle-burst | fx -> hash1 |
| fx-cr-glitter-rain | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cr-matrix | fx -> hash |
| fx-cr-signal-lost | fx -> hash; fx -> hash1 |
| fx-cr-slice-drift | fx -> hash |
| fx-cr-projector | fx -> hash; fx -> hash1 |
| fx-cr-flash-cut | fx -> hash1 |
| fx-cr-aurora | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-cr-twinkle-sky | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-cr-tri-split | fx -> hash1 |
| fx-mt-photo-snap | fx -> hash1 |
| fx-mt-zoom-shake | fx -> hash1 |
| fx-globe | fx -> hash |
| fx-hyperspace | fx -> streaks -> hash1 |
| fx-star-rush | fx -> streaks -> hash1 |
| fx-camera-shake | fx -> vnoise; fx -> vnoise -> hash |
| fx-film-grain | fx -> hash |
| fx-glitch-in | fx -> hash |
| fx-film-burn | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-torn-reveal | fx -> hash |
| fx-mirror-ball | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-confetti | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-balloons | fx -> hash |
| fx-earthquake | fx -> hash; fx -> hash2 |
| fx-light-leak | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-bokeh | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-grid-flash | fx -> hash |
| fx-pencil-sketch | fx -> hash2; fx -> vnoise; fx -> hash2 -> hash |
| fx-marker-lines | fx -> hash2; fx -> hash2 -> hash |
| fx-spray-neon | fx -> hash |
| fx-chalkboard | fx -> hash2; fx -> vnoise; fx -> hash2 -> hash |
| fx-scribble-frame | fx -> vnoise; fx -> vnoise -> hash |
| fx-confetti-burst | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-gold-glitter | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-heart-rain | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-bubbles | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-star-pop | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-petals | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-snowfall | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-eight-bit | fx |
| fx-pixel-breakdown | fx -> hash |
| fx-pixel-glitch | fx -> hash |
| fx-digital-blocks | fx -> hash; fx -> hash2 |
| fx-vhs | fx -> hash |
| fx-scanline-jitter | fx -> hash |
| fx-datamosh | fx -> hash2; fx -> hash2 -> hash |
| fx-signal-loss | fx -> hash |
| fx-bad-tv | fx -> hash |
| fx-glitch-flash | fx -> hash |
| fx-vhs-rewind | fx -> hash |
| fx-film-8mm | fx -> hash; fx -> vnoise |
| fx-old-tv | fx -> hash |
| fx-film-burn-leak | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-seventies | fx -> hash |
| fx-dust-scratches | fx -> hash |
| fx-tr-slam-merge | fx -> hash |
| fx-tr-heat-flicks | fx -> hash |
| fx-tr-shake | fx -> vnoise; fx -> vnoise -> hash |
| fx-tr-glitch | fx -> hash |
| fx-tr-flare | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-tr-film-burn | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-glitter-rain | fx -> hash; fx -> hash2 |
| fx-ov-gold-dust | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bokeh-gold | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bokeh-pink | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-sparks | fx -> hash; fx -> hash2 |
| fx-ov-embers | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-smoke | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-fog-white | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-galaxy | fx -> fbm -> vnoise; fx -> stars -> hash |
| fx-ov-fireflies | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-magic-dust | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-sparkle-white | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-glitter | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-hearts-pink | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-stars | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-confetti | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-snow | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-rain | fx -> hash |
| fx-ov-bubbles | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-leak-warm | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-leak-pink | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-leak-blue | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-light-streaks | fx -> hash1 |
| fx-ov-stage-lights | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-bg-purple-smoke | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-bg-pastel-ink | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-bg-pink-gradient | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bg-blue-hearts | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bg-pink-bokeh | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bg-golden-band | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bg-blue-smoke | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-bg-light-wall | fx -> hash; fx -> fbm -> vnoise |
| fx-ov-bg-3d-shapes | fx -> hash1 |
| fx-ov-bg-red-hearts | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-sc-aurora | fx -> fbm -> vnoise; fx -> stars -> hash |
| fx-ov-sc-milky-way | fx -> fbm -> vnoise; fx -> stars -> hash |
| fx-ov-sc-sunset-sea | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-sc-moon-dunes | fx -> fbm -> vnoise; fx -> stars -> hash |
| fx-ov-sc-clouds | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-sc-snowy-night | fx -> particles -> hash; fx -> particles -> hash2; fx -> ridge -> fbm -> vnoise |
| fx-ov-sc-ocean | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-sc-shooting-stars | fx -> hash1; fx -> stars -> hash |
| fx-ov-tr-shape-pop | fx -> hash |
| fx-ov-tr-glow-blob | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-tr-fire-wipe | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-tr-ink | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-tr-light-burst | fx -> hash1 |
| fx-ov-tr-scribble | fx -> hash1 |
| fx-ov-el-speed-lines | fx -> hash |
| fx-ov-el-lightning | fx -> hash1; fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-el-moon | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-el-smoke-puff | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-film-dust | fx -> hash; fx -> hash1 |
| fx-ov-grain | fx -> hash |
| fx-ov-vhs | fx -> hash |
| fx-ov-paper | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-burnt-edges | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-leak-green | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-leak-purple | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-leak-red | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-lightning | fx -> hash1 |
| fx-ov-bokeh-blue | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-bokeh-rainbow | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-sparkle-pink | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-sparkle-blue | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-hearts-red | fx -> particles -> hash; fx -> particles -> hash2 |
| fx-ov-fog-pink | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
| fx-ov-fog-blue | fx -> fbm -> vnoise; fx -> fbm -> vnoise -> hash |
