package com.pinwheel.core.media.video

/** One adjustable value. Stored 0..1 in the project; the shader reads it as P1..P8 in list order. */
data class FxParam(val key: String, val label: String, val default: Float)

data class VideoFxSpec(
    val id: String,
    val name: String,
    val category: String,
    val shader: String,
    val defines: String,
    val params: List<FxParam>,
    /** Needs the previous frame and the fading trail (Body effects, datamosh). */
    val history: Boolean = false,
    /** Plays best as a short intro/outro; placed for 1.5 s instead of 3 s. */
    val transitionLike: Boolean = false,
    /** Preview sample: 0 portrait, 1 action (parkour), 2 ride POV, 3 product (sneaker), 4 street (Hyperspace only). */
    val sample: Int = 0,
) {
    fun defaults(): Map<String, Float> = params.associate { it.key to it.default }
    /** Packs saved values (falling back to defaults) into the two vec4 uniforms. */
    fun uniforms(values: Map<String, Float>, out: FloatArray = FloatArray(8)): FloatArray {
        out.fill(.5f)
        params.take(8).forEachIndexed { i, p -> out[i] = (values[p.key] ?: p.default).coerceIn(0f, 1f) }
        return out
    }
}


object VideoFxCatalog {
    private val I = FxParam("intensity", "Intensity", .75f)
    private val S = FxParam("speed", "Speed", .45f)
    private fun size(d: Float = .4f) = FxParam("size", "Size", d)
    private fun color(d: Float = 0f) = FxParam("color", "Colour", d)
    private val BG = FxParam("backdrop", "Backdrop blur", 0f)
    private val std = listOf(I, S)
    private val std3d = listOf(FxParam("intensity", "Depth", .7f), S, size(.25f), BG)
    private val body = listOf(I, S, FxParam("size", "Thickness", .4f), color(.55f))

    private fun fx(id: String, name: String, category: String, shader: String, mode: Int, params: List<FxParam> = std,
        history: Boolean = false, intro: Boolean = false, sample: Int = 0) =
        VideoFxSpec("fx-$id", name, category, shader, "#define MODE $mode", params, history, intro, sample)

    val categories = listOf("Trending", "Montage", "3D", "Pixel", "Neon", "Whimsical", "Classic", "Intro & Outro", "Party", "Motion", "Light",
        "Split", "Retro", "Glitch", "Celebrate", "Graffiti")

    val effects: List<VideoFxSpec> = listOf(
        // ---- CapCut Trending (one entry per CapCut effect, in CapCut order) ----------------
        fx("shake", "Shake", "Motion", FX_TREND, 1, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.5f)), sample = 1),
        fx("blur", "Blur", "Classic", FX_TREND, 2, listOf(FxParam("blur", "Blur", 0.5f)), sample = 3),
        fx("ct-edge-glow", "Edge Glow", "Light", FX_TREND, 3, listOf(FxParam("glow", "Glow", 0.5f)), sample = 3),
        fx("cc-slash-reveal", "Slash Reveal", "Intro & Outro", FX_TREND, 4, listOf(FxParam("speed", "Speed", 0.5f), FxParam("glow", "Glow", 0.5f)), sample = 10),
        fx("cc-black-flash", "Black Flash 2", "Party", FX_TREND, 5, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.75f), FxParam("size", "Size", 0.4f), FxParam("twist", "Twist", 0.5f)), sample = 0),
        fx("cc-diamond-zoom", "Diamond Zoom", "Classic", FX_TREND, 6, listOf(FxParam("size", "Size", 0.5f), FxParam("intensity", "Intensity", 0.75f), FxParam("speed", "Speed", 0.35f), FxParam("horizontal", "Horizontal", 0.5f), FxParam("rotate", "Rotate", 0.5f)), sample = 13),
        fx("ct-error-quake", "Error Quake", "Glitch", FX_TREND, 7, listOf(FxParam("intensity", "Intensity", 0.6f), FxParam("speed", "Speed", 0.5f), FxParam("horizontal", "Horizontal", 0.5f), FxParam("rotate", "Rotate", 0.5f)), sample = 7),
        fx("ct-explosion", "Explosion", "Party", FX_TREND, 8, listOf(FxParam("speed", "Speed", 0.5f)), sample = 0),
        fx("ct-scene-cut", "Scene Cut", "Split", FX_TREND, 9, listOf(FxParam("intensity", "Intensity", 0.8f)), sample = 10),
        fx("ct-sharpen-edges", "Sharpen Edges", "Classic", FX_TREND, 10, listOf(FxParam("intensity", "Intensity", 0.6f)), sample = 1),
        fx("ct-game-over", "Game Over", "Retro", FX_TREND, 11, listOf(FxParam("intensity", "Intensity", 1.0f)), sample = 12),
        fx("cc-shiny-stack", "Shiny Stack", "3D", FX_TREND, 12, listOf(FxParam("intensity", "Intensity", 0.8f), FxParam("speed", "Speed", 0.5f)), sample = 3),
        fx("cc-dance-flash", "Dance Flash", "Party", FX_TREND, 13, listOf(FxParam("speed", "Speed", 0.35f), FxParam("intensity", "Intensity", 0.9f), FxParam("sharpen", "Sharpen", 0.5f)), sample = 0),
        fx("ct-glass-breaking", "Glass Breaking", "Classic", FX_TREND, 14, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.7f)), sample = 16),
        fx("ct-negative-panels", "Negative Panels", "Split", FX_TREND, 15, listOf(FxParam("speed", "Speed", 0.5f)), sample = 13),
        fx("ct-darken-flash", "Darken Flash", "Party", FX_TREND, 16, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.7f)), sample = 0),
        fx("horizontal-open", "Horizontal Open", "Intro & Outro", FX_TREND, 17, emptyList(), sample = 2),
        fx("ct-fireplace", "By the Fireplace", "Light", FX_TREND, 18, listOf(FxParam("intensity", "Intensity", 0.8f)), sample = 0),
        fx("fade-in", "Fade In", "Intro & Outro", FX_TREND, 19, emptyList(), sample = 3),
        fx("ct-neon", "Neon", "Neon", FX_TREND, 20, listOf(FxParam("atmosphere", "Atmosphere", 1.0f)), sample = 1),
        fx("ct-vignette-noir", "Vignette Noir", "Classic", FX_TREND, 21, listOf(FxParam("atmosphere", "Atmosphere", 1.0f)), sample = 1),
        fx("ct-dreamy-halo", "Dreamy Halo", "Light", FX_TREND, 22, listOf(FxParam("glow", "Glow", 0.5f), FxParam("range", "Range", 0.6f), FxParam("size", "Size", 0.5f), FxParam("softlight", "Soft light", 0.6f), FxParam("filter", "Filter", 0.65f), FxParam("sharpen", "Sharpen", 0.15f), FxParam("blur", "Blur", 0.13f)), sample = 0),
        fx("ct-lightning-crack", "Lightning Crack", "Whimsical", FX_TREND, 23, listOf(FxParam("twist", "Twist", 0.5f), FxParam("speed", "Speed", 0.7f), FxParam("stickers", "Stickers", 1.0f), FxParam("filter", "Filter", 0.5f)), sample = 13),
        fx("ct-narrow-focus", "Narrow Focus", "Classic", FX_TREND, 24, emptyList(), sample = 1),
        fx("vignette", "Vignette", "Classic", FX_TREND, 25, listOf(FxParam("texture", "Texture", 1.0f)), sample = 1),
        fx("ct-offset-slice", "Offset Slice", "Glitch", FX_TREND, 26, listOf(FxParam("speed", "Speed", 0.5f)), sample = 5),
        fx("zoom-lens", "Zoom Lens", "Classic", FX_TREND, 27, listOf(FxParam("speed", "Speed", 0.5f), FxParam("range", "Range", 0.5f)), sample = 1),
        fx("ct-picture-bumps", "Picture Bumps", "Montage", FX_TREND, 28, listOf(FxParam("speed", "Speed", 0.5f)), sample = 13),
        fx("ct-background-fit", "Background Fit", "Classic", FX_TREND, 29, listOf(FxParam("speed", "Speed", 0.5f)), sample = 14),
        fx("ct-astral", "Astral 2", "Whimsical", FX_TREND, 30, listOf(FxParam("intensity", "Intensity", 0.6f), FxParam("glow", "Glow", 0.6f), FxParam("twist", "Twist", 0.6f), FxParam("atmosphere", "Atmosphere", 0.8f)), sample = 0),
        fx("cc-wobbly-flash", "Wobbly Flash", "Party", FX_TREND, 31, listOf(FxParam("speed", "Speed", 0.5f)), sample = 1),
        fx("cc-feverish", "Feverish Imprint", "Party", FX_TREND, 32, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.7f)), sample = 0),
        fx("ct-tension-zoom", "Tension Zoom", "Motion", FX_TREND, 33, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.6f)), sample = 1),
        fx("ct-negative", "Negative", "Classic", FX_TREND, 34, emptyList(), sample = 0),
        fx("ct-cut-shift", "Cut Shift", "Glitch", FX_TREND, 35, listOf(FxParam("speed", "Speed", 0.5f)), sample = 11),
        fx("ct-effect-mashup", "Effect Mashup", "Montage", FX_TREND, 36, listOf(FxParam("speed", "Speed", 0.5f)), sample = 5),
        fx("ct-hexagonal-spot", "Hexagonal Spot", "Light", FX_TREND, 37, listOf(FxParam("intensity", "Intensity", 0.7f)), sample = 5),
        fx("ct-broken-shine", "Broken Shine", "Light", FX_TREND, 38, listOf(FxParam("speed", "Speed", 0.5f)), sample = 1),
        fx("cc-move-cloud", "Move Cloud", "Whimsical", FX_TREND, 39, listOf(FxParam("speed", "Speed", 0.5f)), sample = 9),
        fx("ct-grim-neo", "Grim Neo", "Neon", FX_TREND, 40, listOf(FxParam("intensity", "Intensity", 0.8f)), sample = 12),
        fx("ct-chaotic-heat", "Chaotic Heat", "Party", FX_TREND, 41, listOf(FxParam("speed", "Speed", 0.5f), FxParam("intensity", "Intensity", 0.8f)), sample = 1),
        fx("ct-delicate-rays", "Delicate Rays", "Light", FX_TREND, 42, listOf(FxParam("glow", "Glow", 0.5f), FxParam("range", "Range", 0.6f), FxParam("size", "Size", 0.5f), FxParam("softlight", "Soft light", 0.5f), FxParam("sharpen", "Sharpen", 0.5f), FxParam("atmosphere", "Atmosphere", 0.6f)), sample = 0),
        fx("ct-shine-zoom", "Shine Zoom", "Intro & Outro", FX_TREND, 43, listOf(FxParam("speed", "Speed", 0.5f)), sample = 13),
        fx("ct-magical-tome", "Magical Tome", "Whimsical", FX_TREND, 44, listOf(FxParam("intensity", "Intensity", 0.8f)), sample = 0),
        fx("ct-phone-cube", "3D Phone Cube", "3D", FX_TREND, 45, listOf(FxParam("speed", "Speed", 0.5f)), sample = 1),
        fx("ct-grim-reaper", "Grim Reaper", "Whimsical", FX_TREND, 46, listOf(FxParam("intensity", "Intensity", 0.8f)), sample = 1),
        fx("ct-phone-zoom", "Phone Zoom", "3D", FX_TREND, 47, listOf(FxParam("speed", "Speed", 0.5f)), sample = 2),
        // ---- CapCut-style set -------------------------------------------------------------
        fx("cc-chromatic", "Chromatic", "Glitch", FX_CC_CAMERA, 2, std, sample = 5),
        fx("cc-ripple", "Ripple", "Motion", FX_CC_CAMERA, 4, std, sample = 2),
        fx("cc-retro-flicker", "Retro Flicker", "Retro", FX_CC_CAMERA, 5, std, sample = 0),
        fx("cc-handheld", "Handheld Shake", "Motion", FX_CC_CAMERA, 7, std, sample = 2),
        fx("cc-film-wave", "Film Wave", "Retro", FX_CC_CAMERA, 9, std, sample = 2),
        fx("cc-wiggle-flicker", "Wiggle Flicker", "Motion", FX_CC_CAMERA, 10, std, sample = 1),
        fx("cc-bw-slider", "BW Slider", "Classic", FX_CC_CAMERA, 12, std, sample = 2),
        fx("cc-hex-split", "Hexagonal Split", "Split", FX_CC_CAMERA, 13, listOf(I, S, size(.2f)), sample = 3),
        fx("cc-shimmer-360", "360 Shimmer", "Classic", FX_CC_CAMERA, 14, std, sample = 3),
        fx("cc-dynamic-blur", "Dynamic Blur", "Classic", FX_CC_CAMERA, 15, std, sample = 1),
        fx("cc-rebound-swing", "Rebound Swing", "Motion", FX_CC_CAMERA, 17, std, sample = 3),
        fx("cc-cut-twist", "Cut-up Twist", "Glitch", FX_CC_CAMERA, 19, std, sample = 7),
        fx("cc-bg-copies", "BG Copies", "3D", FX_CC_CAMERA, 21, std, sample = 3),
        fx("cc-camera-punch", "Camera Punch", "Motion", FX_CC_CAMERA, 22, std, sample = 1),
        fx("cc-heart-ascent", "Heart Ascent", "Whimsical", FX_CC_WHIMSY, 1, std, sample = 0),
        fx("cc-butterfly", "Butterfly", "Whimsical", FX_CC_WHIMSY, 2, listOf(I, S, color(0f)), sample = 3),
        fx("cc-butterfly-dream", "Butterfly Dream", "Whimsical", FX_CC_WHIMSY, 3, std, sample = 0),
        fx("cc-leak-warm", "Leak 1", "Light", FX_CC_WHIMSY, 4, std, sample = 2),
        fx("cc-leak-neon", "Leak 2", "Light", FX_CC_WHIMSY, 5, std, sample = 0),
        fx("cc-vignette-rose", "Vignette Rose", "Whimsical", FX_CC_WHIMSY, 6, std, sample = 0),
        fx("cc-falling-petals", "Falling Petals", "Whimsical", FX_CC_WHIMSY, 7, std, sample = 3),
        fx("cc-smoky-focus", "Smoky Focus", "Light", FX_CC_WHIMSY, 8, std, sample = 0),
        fx("cc-vintage-film", "Vintage Film", "Retro", FX_CC_WHIMSY, 9, std, sample = 2),
        fx("cc-stellar", "Stellar", "Whimsical", FX_CC_WHIMSY, 10, std, sample = 2),
        fx("cc-border-current", "Border Current", "Light", FX_CC_WHIMSY, 11, listOf(I, S, size(.4f)), sample = 1),
        fx("cc-ink-spill", "Ink Spill", "Intro & Outro", FX_TREND, 48, listOf(FxParam("speed", "Speed", 0.35f), FxParam("atmosphere", "Atmosphere", 0.5f)), sample = 13),
        fx("cc-exploding-love", "Exploding Love", "Whimsical", FX_CC_WHIMSY, 14, std, sample = 0),
        fx("cc-damaged-vignette", "Damaged Vignette", "Retro", FX_CC_WHIMSY, 15, std, sample = 1),
        fx("cc-firefly", "Firefly Fairies", "Whimsical", FX_CC_WHIMSY, 16, std, sample = 2),
        fx("cc-misty-tint", "Misty Tint", "Light", FX_CC_WHIMSY, 17, std, sample = 2),
        fx("cc-sepia-cool", "Sepia Cool", "Retro", FX_CC_WHIMSY, 18, std, sample = 1),
        fx("cc-snow-night", "Snow Night", "Whimsical", FX_CC_WHIMSY, 19, std, sample = 2),
        fx("cc-rain", "Rain", "Whimsical", FX_CC_WHIMSY, 20, std, sample = 1),
        fx("cc-rose-bloom", "Rose Bloom", "Whimsical", FX_CC_WHIMSY, 21, std, sample = 0),
        fx("cc-sparkle-shine", "Sparkle Shine", "Light", FX_CC_WHIMSY, 22, std, sample = 3),
        fx("cc-color-pixel", "Color Pixel", "Pixel", FX_CC_PIXEL, 1, listOf(I, S, size(.4f)), sample = 1),
        fx("cc-pixel-blocks", "Pixel Blocks", "Pixel", FX_CC_PIXEL, 2, std, sample = 0),
        fx("cc-pixel-mutant", "Pixel Mutant", "Pixel", FX_CC_PIXEL, 3, std, sample = 1),
        fx("cc-pixel-art", "Pixel Art", "Pixel", FX_CC_PIXEL, 4, listOf(I, S, size(.4f)), sample = 3),
        fx("cc-pixel-universe", "Pixel Universe", "Pixel", FX_CC_PIXEL, 5, std, sample = 0),
        fx("pb-pixel-scan", "Pixel Scan", "Pixel", FX_PIXEL_BEAD, 1, std, sample = 0),
        fx("pb-flip-phone", "Flip Phone", "3D", FX_PHONES, 2, std, sample = 0),
        fx("pb-pixel-creation", "Pixel Creation", "Pixel", FX_PIXEL_BEAD, 3, std, intro = true, sample = 3),
        fx("pb-pixel-breakdown", "Pixel Breakdown", "Pixel", FX_PIXEL_BEAD, 4, std, sample = 1),
        fx("pb-losing-hp", "Losing HP", "Pixel", FX_PIXEL_BEAD, 5, std, sample = 1),
        fx("pb-sweet-party", "Sweet Party", "Pixel", FX_PIXEL_BEAD, 6, std, sample = 0),
        fx("pb-popping-bow", "Popping Bow", "Pixel", FX_PIXEL_BEAD, 7, std, sample = 0),
        fx("pb-misty-tint", "Misty Tint", "Pixel", FX_PIXEL_BEAD, 8, std, sample = 0),
        fx("pb-game-boy", "Game Boy", "Pixel", FX_PIXEL_BEAD, 9, std, sample = 1),
        fx("pb-pixel-dissolve", "Pixel Dissolve", "Pixel", FX_PIXEL_BEAD, 10, std, sample = 2),
        fx("pb-bead-art", "Bead Art", "Pixel", FX_PIXEL_BEAD, 11, listOf(I, S, size(.4f)), sample = 3),
        fx("pb-pixel-rain", "Pixel Rain", "Pixel", FX_PIXEL_BEAD, 12, std, sample = 2),
        fx("st-phone-3d", "3D Phone", "3D", FX_PHONES, 1, std, sample = 1),
        fx("st-card-deck", "Card Deck", "3D", FX_STYLE_3D, 2, std, sample = 0),
        fx("st-polaroid", "Polaroid Drop", "3D", FX_STYLE_3D, 3, std, sample = 3),
        fx("st-retro-tv", "Retro TV", "3D", FX_STYLE_3D, 4, std, sample = 2),
        fx("st-billboard", "Billboard", "3D", FX_STYLE_3D, 5, std, sample = 1),
        fx("st-film-strip", "Film Strip", "3D", FX_STYLE_3D, 6, std, sample = 2),
        fx("st-fanned-cards", "Fanned Cards", "3D", FX_STYLE_3D, 7, std, sample = 0),
        fx("st-glass-card", "Glass Card", "3D", FX_STYLE_3D, 8, std, sample = 3),
        fx("ne-frame", "Neon Frame", "Neon", FX_NEON, 1, listOf(I, S, color(.55f)), sample = 1),
        fx("ne-ring", "Neon Ring", "Neon", FX_NEON, 2, listOf(I, S, color(.85f)), sample = 0),
        fx("ne-heart", "Neon Heart", "Neon", FX_NEON, 3, std, sample = 0),
        fx("ne-edges", "Neon Glow Edges", "Neon", FX_NEON, 4, std, sample = 1),
        fx("ne-cyberpunk", "Cyberpunk", "Neon", FX_NEON, 5, std, sample = 2),
        fx("ne-tunnel", "Neon Tunnel", "Neon", FX_NEON, 6, listOf(I, S, color(.8f)), sample = 3),
        fx("ne-lightning", "Neon Lightning", "Neon", FX_NEON, 7, std, sample = 1),
        fx("ne-scan", "Neon Scan", "Neon", FX_NEON, 8, listOf(I, S, color(.5f)), sample = 2),
        fx("ne-scribble", "Neon Scribble", "Neon", FX_NEON, 9, listOf(I, S, color(.9f)), sample = 0),
        fx("ne-synthwave", "Synthwave", "Neon", FX_NEON, 10, std, sample = 2),
        fx("ne-stars", "Neon Stars", "Neon", FX_NEON, 11, listOf(I, S, color(.6f)), sample = 3),
        fx("cr-sparkle-burst", "Sparkle Burst", "Celebrate", FX_CREATIVE, 1, std, sample = 0),
        fx("cr-glitter-rain", "Glitter Rain", "Celebrate", FX_CREATIVE, 2, std, sample = 3),
        fx("cr-rainbow-prism", "Rainbow Prism", "Light", FX_CREATIVE, 3, std, sample = 0),
        fx("cr-anamorphic", "Anamorphic Flare", "Light", FX_CREATIVE, 4, std, sample = 1),
        fx("cr-matrix", "Matrix Rain", "Glitch", FX_CREATIVE, 5, std, sample = 10),
        fx("cr-signal-lost", "Signal Lost", "Glitch", FX_CREATIVE, 6, std, sample = 2),
        fx("cr-slice-drift", "Slice Drift", "Glitch", FX_CREATIVE, 7, std, sample = 12),
        fx("cr-camcorder", "Camcorder", "Retro", FX_CREATIVE, 8, std, sample = 2),
        fx("cr-projector", "Projector", "Retro", FX_CREATIVE, 9, std, sample = 0),
        fx("cr-comic-pop", "Comic Pop", "Graffiti", FX_CREATIVE, 10, std, sample = 1),
        fx("cr-dreamy", "Dreamy", "Light", FX_CREATIVE, 11, std, sample = 0),
        fx("cr-kaleido-bloom", "Kaleido Bloom", "Split", FX_CREATIVE, 12, std, sample = 2),
        fx("cr-zoom-burst", "Zoom Burst In", "Intro & Outro", FX_CREATIVE, 13, std, intro = true, sample = 1),
        fx("cr-ring-reveal", "Ring Reveal", "Intro & Outro", FX_CREATIVE, 14, listOf(I, S, color(0f)), intro = true, sample = 3),
        fx("cr-flash-cut", "Flash Cut", "Party", FX_CREATIVE, 15, std, sample = 1),
        fx("cr-rgb-strobe", "RGB Strobe", "Party", FX_CREATIVE, 16, std, sample = 0),
        fx("cr-aurora", "Aurora", "Light", FX_CREATIVE, 17, std, sample = 2),
        fx("cr-heart-pulse", "Heart Pulse", "Motion", FX_CREATIVE, 18, std, sample = 0),
        fx("cr-twinkle-sky", "Twinkle Sky", "Whimsical", FX_CREATIVE, 19, std, sample = 2),
        fx("cr-tri-split", "Tri Split Glitch", "Split", FX_CREATIVE, 20, std, sample = 13),
        fx("mt-velocity", "Velocity", "Montage", FX_MONTAGE, 1, std, sample = 1),
        fx("mt-beat-rotate", "Beat Rotate", "Montage", FX_MONTAGE, 2, std, sample = 1),
        fx("mt-photo-snap", "Photo Snap", "Montage", FX_MONTAGE, 3, std, sample = 6),
        fx("mt-zoom-shake", "Zoom Shake", "Montage", FX_MONTAGE, 4, std, sample = 2),
        fx("mt-split-swipe", "Split Swipe", "Montage", FX_MONTAGE, 5, std, sample = 9),
        fx("mt-blur-punch", "Blur Punch", "Montage", FX_MONTAGE, 6, std, sample = 0),
        fx("mt-mirror-beat", "Mirror Beat", "Montage", FX_MONTAGE, 7, std, sample = 11),
        fx("mt-step-zoom", "Step Zoom", "Montage", FX_MONTAGE, 8, std, sample = 3),
        fx("mt-rgb-punch", "RGB Punch", "Montage", FX_MONTAGE, 9, std, sample = 1),
        fx("mt-dutch-tilt", "Dutch Tilt", "Montage", FX_MONTAGE, 10, std, sample = 2),
        fx("mt-echo-zoom", "Echo Zoom", "Montage", FX_MONTAGE, 11, std, sample = 0),
        fx("mt-cinematic", "Cinematic Push", "Montage", FX_MONTAGE, 12, std, sample = 2),
        // ---- 3D ---------------------------------------------------------------------------
        fx("swing-card", "Swing Card", "3D", FX_3D_CARD, 1, std3d, sample = 2),
        fx("orbit-card", "Orbit", "3D", FX_3D_CARD, 3, std3d, sample = 0),
        fx("flip-card", "Flip Card", "3D", FX_3D_CARD, 4, std3d, sample = 1),
        fx("spin-card", "Spin Tilt", "3D", FX_3D_CARD, 5, std3d, sample = 2),
        fx("phone-spin", "Phone Spin", "3D", FX_3D_CARD, 8, std3d, sample = 0),
        fx("cube-spin", "Cube Spin", "3D", FX_3D_CUBE, 1, std3d, sample = 2),
        fx("cube-tumble", "Cube Tumble", "3D", FX_3D_CUBE, 2, std3d, sample = 3),
        fx("dice-roll", "Dice Roll", "3D", FX_3D_CUBE, 3, std3d, sample = 0),
        fx("globe", "Globe", "3D", FX_3D_SPHERE, 1, std3d, sample = 3),
        fx("crystal-ball", "Crystal Ball", "3D", FX_3D_SPHERE, 2, std3d, sample = 2),
        fx("tunnel", "Tunnel", "3D", FX_3D_TUNNEL, 1, std3d, sample = 2),
        fx("mirror-room", "Mirror Room", "3D", FX_3D_TUNNEL, 2, std3d, sample = 1),
        fx("spiral-tunnel", "Spiral Tunnel", "3D", FX_3D_TUNNEL, 3, std3d, sample = 3),
        fx("hyperspace", "Hyperspace", "3D", FX_3D_WARP, 1, std3d, sample = 4),
        fx("star-rush", "Star Rush", "3D", FX_3D_WARP, 2, std3d, sample = 1),
        fx("carousel", "Carousel", "3D", FX_3D_MISC, 5, std3d, sample = 1),
        fx("kaleido-drill", "Kaleido Drill", "3D", FX_3D_MISC, 8, std3d, sample = 2),
        fx("tiny-planet", "Tiny Planet", "3D", FX_3D_MISC, 9, std3d, sample = 3),
        fx("infinite-zoom", "Infinite Zoom", "3D", FX_3D_MISC, 13, std3d, sample = 3),
        fx("cylinder", "Cylinder", "3D", FX_3D_MISC, 14, std3d, sample = 2),
        fx("neon-stage", "Neon Stage", "3D", FX_3D_MISC, 15, std3d, sample = 1),

        // ---- Body (reacts to whoever moves) -------------------------------------------------

        // ---- Only in Pinwheel -------------------------------------------------------------

        // ---- Classic ----------------------------------------------------------------------
        fx("fade-out", "Fade Out", "Classic", FX_CLASSIC, 4, std, intro = true, sample = 3),
        fx("white-flash", "White Flash", "Classic", FX_CLASSIC, 5, std, sample = 0),
        fx("blink", "Blink", "Classic", FX_CLASSIC, 6, std, sample = 1),
        fx("motion-blur", "Motion Blur", "Classic", FX_CLASSIC, 7, std + FxParam("angle", "Angle", .5f), sample = 2),
        fx("mini-zoom", "Mini Zoom", "Classic", FX_CLASSIC, 8, std, sample = 0),
        fx("camera-shake", "Camera Shake", "Classic", FX_CLASSIC, 9, std, sample = 2),
        fx("film-grain", "Film Grain", "Classic", FX_CLASSIC, 10, std, sample = 3),
        fx("cinema", "Cinema", "Classic", FX_CLASSIC, 11, std, sample = 2),
        fx("vertical-open", "Vertical Open", "Classic", FX_CLASSIC, 12, std, intro = true, sample = 3),
        fx("camera-focus", "Camera Focus", "Classic", FX_CLASSIC, 14, std, sample = 1),
        fx("spin-blur", "Spin Blur", "Classic", FX_CLASSIC, 15, std, sample = 2),
        fx("swing", "Swing", "Classic", FX_CLASSIC, 16, std, sample = 0),
        fx("low-exposure", "Low Exposure", "Classic", FX_CLASSIC, 17, std, sample = 3),
        fx("hazy", "Hazy", "Classic", FX_CLASSIC, 18, std, sample = 3),
        fx("to-colour", "B&W to Colour", "Classic", FX_CLASSIC, 19, std, intro = true, sample = 2),
        fx("colorize", "Colourise", "Classic", FX_CLASSIC, 20, std + FxParam("color", "Hue", .08f), sample = 0),
        fx("tilt-shift", "Tilt Shift", "Classic", FX_CLASSIC, 22, std, sample = 2),

        // ---- Intro & Outro ----------------------------------------------------------------
        fx("iris-open", "Iris Open", "Intro & Outro", FX_INTRO, 1, std, intro = true, sample = 2),
        fx("iris-close", "Iris Close", "Intro & Outro", FX_INTRO, 2, std, intro = true, sample = 3),
        fx("slide-reveal", "Slide Reveal", "Intro & Outro", FX_INTRO, 3, std, intro = true, sample = 14),
        fx("zoom-intro", "Zoom In", "Intro & Outro", FX_INTRO, 4, std, intro = true, sample = 2),
        fx("blur-in", "Blur In", "Intro & Outro", FX_INTRO, 5, std, intro = true, sample = 3),
        fx("blur-out", "Blur Out", "Intro & Outro", FX_INTRO, 6, std, intro = true, sample = 3),
        fx("curtain", "Curtain", "Intro & Outro", FX_INTRO, 7, std, intro = true, sample = 8),
        fx("the-end", "The End", "Intro & Outro", FX_INTRO, 8, std, intro = true, sample = 3),
        fx("flash-in", "Flash In", "Intro & Outro", FX_INTRO, 9, std, intro = true, sample = 0),
        fx("glitch-in", "Glitch In", "Intro & Outro", FX_INTRO, 10, std, intro = true, sample = 5),
        fx("film-burn", "Film Burn", "Intro & Outro", FX_INTRO, 11, std, intro = true, sample = 2),
        fx("shutter", "Shutter", "Intro & Outro", FX_INTRO, 12, std, intro = true, sample = 3),
        fx("heart-reveal", "Heart Reveal", "Intro & Outro", FX_INTRO, 13, std, intro = true, sample = 0),
        fx("torn-reveal", "Torn Reveal", "Intro & Outro", FX_INTRO, 14, std, intro = true, sample = 2),

        // ---- Party ------------------------------------------------------------------------
        fx("disco", "Disco", "Party", FX_PARTY, 1, std + color(), sample = 1),
        fx("laser-show", "Laser Show", "Party", FX_PARTY, 3, std + color(), sample = 1),
        fx("club-lights", "Club Lights", "Party", FX_PARTY, 4, std + color(), sample = 0),
        fx("beat-zoom", "Beat Zoom", "Party", FX_PARTY, 5, std, sample = 1),
        fx("mirror-ball", "Mirror Ball", "Party", FX_PARTY, 6, std + color(), sample = 0),
        fx("confetti", "Confetti", "Party", FX_PARTY, 7, std + color(), sample = 1),
        fx("colour-cycle", "Colour Cycle", "Party", FX_PARTY, 9, std + color(), sample = 2),
        fx("balloons", "Balloons", "Party", FX_PARTY, 11, std + color(), sample = 0),

        // ---- Motion -----------------------------------------------------------------------
        fx("earthquake", "Earthquake", "Motion", FX_MOTION, 2, std, sample = 2),
        fx("swirl", "Swirl", "Motion", FX_MOTION, 4, std, sample = 2),
        fx("spin", "Spin", "Motion", FX_MOTION, 5, std, sample = 0),
        fx("bounce", "Bounce", "Motion", FX_MOTION, 6, std, sample = 1),
        fx("zoom-pulse", "Zoom Pulse", "Motion", FX_MOTION, 9, std, sample = 2),
        fx("heartbeat", "Heartbeat", "Motion", FX_MOTION, 12, std, sample = 1),

        // ---- Light ------------------------------------------------------------------------
        fx("light-leak", "Light Leak", "Light", FX_LIGHT, 1, std, sample = 0),
        fx("lens-flare", "Lens Flare", "Light", FX_LIGHT, 2, std, sample = 3),
        fx("bokeh", "Bokeh", "Light", FX_LIGHT, 4, std + color(.1f), sample = 2),
        fx("sunbeam", "Sunbeam", "Light", FX_LIGHT, 5, std, sample = 1),
        fx("rainbow-leak", "Rainbow Leak", "Light", FX_LIGHT, 6, std, sample = 0),
        fx("bloom", "Bloom", "Light", FX_LIGHT, 7, std, sample = 2),
        fx("neon-edges", "Neon Edges", "Light", FX_LIGHT, 8, std + color(.8f), sample = 2),
        fx("starburst", "Starburst", "Light", FX_LIGHT, 12, std, sample = 0),

        // ---- Split ------------------------------------------------------------------------
        fx("split-2", "Split 2", "Split", FX_SPLIT, 1, std, sample = 7),
        fx("stack-2", "Stack 2", "Split", FX_SPLIT, 2, std, sample = 2),
        fx("split-3", "Split 3", "Split", FX_SPLIT, 3, std, sample = 10),
        fx("grid-4", "Grid 4", "Split", FX_SPLIT, 4, std, sample = 12),
        fx("grid-9", "Grid 9", "Split", FX_SPLIT, 5, std, sample = 13),
        fx("mirror-lr", "Mirror", "Split", FX_SPLIT, 6, std, sample = 6),
        fx("mirror-tb", "Reflection", "Split", FX_SPLIT, 7, std, sample = 3),
        fx("kaleido-4", "Kaleido 4", "Split", FX_SPLIT, 8, std, sample = 2),
        fx("rgb-panes", "RGB Panes", "Split", FX_SPLIT, 9, std, sample = 9),
        fx("grid-flash", "Grid Flash", "Split", FX_SPLIT, 12, std, sample = 11),

        // ---- Graffiti ---------------------------------------------------------------------
        fx("pencil-sketch", "Pencil Sketch", "Graffiti", FX_GRAFFITI, 1, std, sample = 1),
        fx("marker-lines", "Marker Lines", "Graffiti", FX_GRAFFITI, 2, std + color(), sample = 0),
        fx("spray-neon", "Spray Neon", "Graffiti", FX_GRAFFITI, 4, std + color(.85f), sample = 1),
        fx("comic-print", "Comic Print", "Graffiti", FX_GRAFFITI, 5, std + size(), sample = 0),
        fx("chalkboard", "Chalkboard", "Graffiti", FX_GRAFFITI, 7, std, sample = 1),
        fx("scribble-frame", "Scribble Frame", "Graffiti", FX_GRAFFITI, 8, std + color(), sample = 2),

        // ---- Celebrate --------------------------------------------------------------------
        fx("confetti-burst", "Confetti Burst", "Celebrate", FX_CELEBRATE, 2, std + color(), sample = 0),
        fx("gold-glitter", "Gold Glitter", "Celebrate", FX_CELEBRATE, 3, std, sample = 1),
        fx("heart-rain", "Heart Rain", "Celebrate", FX_CELEBRATE, 4, std + color(), sample = 0),
        fx("bubbles", "Bubbles", "Celebrate", FX_CELEBRATE, 5, std + color(), sample = 3),
        fx("star-pop", "Star Pop", "Celebrate", FX_CELEBRATE, 6, std + color(), sample = 1),
        fx("petals", "Petals", "Celebrate", FX_CELEBRATE, 7, std + color(), sample = 0),
        fx("snowfall", "Snowfall", "Celebrate", FX_CELEBRATE, 8, std, sample = 3),
        fx("sparkler", "Sparkler", "Celebrate", FX_CELEBRATE, 10, std, sample = 2),

        // ---- Pixel ------------------------------------------------------------------------
        fx("pixelate", "Pixelate", "Pixel", FX_PIXEL, 1, std + size(), sample = 1),
        fx("pixel-scan", "Pixel Scan", "Pixel", FX_PIXEL, 2, std + size(), sample = 2),
        fx("mosaic-pulse", "Mosaic Pulse", "Pixel", FX_PIXEL, 3, std, sample = 0),
        fx("eight-bit", "8-Bit", "Pixel", FX_PIXEL, 4, std + size(.5f), sample = 3),
        fx("pixel-sort", "Pixel Sort", "Pixel", FX_PIXEL, 5, std, sample = 2),
        fx("pixel-breakdown", "Pixel Breakdown", "Pixel", FX_PIXEL, 6, std + size(.6f), sample = 1),
        fx("beads", "Pixel Beads", "Pixel", FX_PIXEL, 7, std + size(.6f), sample = 0),
        fx("led-wall", "LED Wall", "Pixel", FX_PIXEL, 8, std + size(.5f), sample = 2),
        fx("cross-stitch", "Cross Stitch", "Pixel", FX_PIXEL, 9, std + size(.5f), sample = 0),
        fx("toy-bricks", "Toy Bricks", "Pixel", FX_PIXEL, 10, std + size(.7f), sample = 3),
        fx("pixel-glitch", "Pixel Glitch", "Pixel", FX_PIXEL, 11, std + size(.4f), sample = 1),

        // ---- Glitch -----------------------------------------------------------------------
        fx("rgb-split", "RGB Split", "Glitch", FX_GLITCH, 1, std, sample = 14),
        fx("digital-blocks", "Digital Blocks", "Glitch", FX_GLITCH, 2, std, sample = 8),
        fx("vhs", "VHS", "Glitch", FX_GLITCH, 3, std, sample = 2),
        fx("scanline-jitter", "Scan Jitter", "Glitch", FX_GLITCH, 4, std, sample = 5),
        fx("datamosh", "Datamosh", "Glitch", FX_GLITCH, 5, std, history = true, sample = 7),
        fx("signal-loss", "Signal Loss", "Glitch", FX_GLITCH, 6, std, sample = 2),
        fx("chroma-wave", "Chroma Wave", "Glitch", FX_GLITCH, 7, std, sample = 10),
        fx("bad-tv", "Bad TV", "Glitch", FX_GLITCH, 8, std, sample = 3),
        fx("glitch-flash", "Glitch Flash", "Glitch", FX_GLITCH, 9, std, sample = 12),

        // ---- Retro ------------------------------------------------------------------------
        fx("vhs-rewind", "VHS Rewind", "Retro", FX_RETRO, 1, std, sample = 2),
        fx("film-8mm", "8mm Film", "Retro", FX_RETRO, 2, std, sample = 3),
        fx("old-tv", "Old TV", "Retro", FX_RETRO, 3, std, sample = 1),
        fx("film-burn-leak", "Burnt Edge", "Retro", FX_RETRO, 4, std, sample = 0),
        fx("faded-print", "Faded Print", "Retro", FX_RETRO, 5, std, sample = 3),
        fx("seventies", "70s", "Retro", FX_RETRO, 6, std, sample = 0),
        fx("dust-scratches", "Dust & Scratches", "Retro", FX_RETRO, 7, std, sample = 2),
        fx("cmyk-print", "CMYK Print", "Retro", FX_RETRO, 8, std, sample = 1),
        fx("sepia-flicker", "Sepia Flicker", "Retro", FX_RETRO, 9, std, sample = 3),
    )

    /** Trending mixes the most striking looks from every category. */
    private val trendingIds = listOf("fx-shake", "fx-blur", "fx-ct-edge-glow", "fx-cc-slash-reveal", "fx-cc-black-flash", "fx-cc-diamond-zoom", "fx-ct-error-quake", "fx-ct-explosion", "fx-ct-scene-cut", "fx-ct-sharpen-edges", "fx-ct-game-over", "fx-cc-shiny-stack", "fx-cc-dance-flash", "fx-ct-glass-breaking", "fx-ct-negative-panels", "fx-ct-darken-flash", "fx-horizontal-open", "fx-ct-fireplace", "fx-fade-in", "fx-fade-out", "fx-ct-neon", "fx-ct-vignette-noir", "fx-ct-dreamy-halo", "fx-ct-lightning-crack", "fx-ct-narrow-focus", "fx-vignette", "fx-ct-offset-slice", "fx-zoom-lens", "fx-ct-picture-bumps", "fx-ct-background-fit", "fx-ct-astral", "fx-cc-wobbly-flash", "fx-cc-feverish", "fx-ct-tension-zoom", "fx-ct-negative", "fx-ct-cut-shift", "fx-ct-effect-mashup", "fx-ct-hexagonal-spot", "fx-ct-broken-shine", "fx-cc-move-cloud", "fx-ct-grim-neo", "fx-ct-chaotic-heat", "fx-ct-delicate-rays", "fx-ct-shine-zoom", "fx-ct-magical-tome", "fx-cc-retro-flicker", "fx-cc-chromatic", "fx-ct-phone-cube", "fx-ct-grim-reaper", "fx-ct-phone-zoom", "fx-cc-ink-spill")

    /** Clip transitions. The shader reads uProgress across a window centred on the cut. */
    private fun tr(id: String, name: String, category: String, mode: Int) =
        VideoFxSpec("fx-tr-$id", name, category, FX_TRANSITIONS, "#define MODE $mode",
            listOf(I, FxParam("speed", "Speed", .5f)), transitionLike = true, sample = 5 + mode % 10)
    val transitionCategories = listOf("Trending", "Basic", "Camera", "Slide", "Mask", "Effects")
    val transitions: List<VideoFxSpec> = listOf(
        tr("vertical-blur", "Vertical Blur II", "Basic", 38),
        tr("slam-merge", "Slam Merge", "Camera", 39),
        tr("heat-flicks", "Heat Flicks", "Effects", 40),
        tr("glare", "Glare II", "Effects", 41),
        tr("cutout-scan", "Cutout Scan", "Effects", 42),
        tr("fake-zoom", "Fake Zoom", "Camera", 43),
        tr("zoom-swipe", "Zoom Swipe", "Camera", 44),
        tr("panel-clasp", "Panel Clasp", "Mask", 45),
        tr("heatwave-flash", "Heatwave Flash", "Effects", 46),
        tr("mix", "Mix", "Basic", 1),
        tr("black-fade", "Black Fade", "Basic", 2),
        tr("white-flash", "White Flash", "Basic", 3),
        tr("blur", "Blur", "Basic", 4),
        tr("blink", "Blink", "Basic", 5),
        tr("pull-in", "Pull In", "Camera", 6),
        tr("pull-out", "Pull Out", "Camera", 7),
        tr("zoom-spin", "Zoom Spin", "Camera", 8),
        tr("rotate-cw", "Rotate CW", "Camera", 9),
        tr("rotate-ccw", "Rotate CCW", "Camera", 10),
        tr("shake", "Shake", "Camera", 11),
        tr("whip-left", "Whip Left", "Camera", 12),
        tr("whip-right", "Whip Right", "Camera", 13),
        tr("whip-up", "Whip Up", "Camera", 14),
        tr("whip-down", "Whip Down", "Camera", 15),
        tr("slide-left", "Slide Left", "Slide", 16),
        tr("slide-right", "Slide Right", "Slide", 17),
        tr("slide-up", "Slide Up", "Slide", 18),
        tr("slide-down", "Slide Down", "Slide", 19),
        tr("squeeze", "Squeeze", "Slide", 20),
        tr("circle", "Circle", "Mask", 21),
        tr("heart", "Heart", "Mask", 22),
        tr("star", "Star", "Mask", 23),
        tr("diamond", "Diamond", "Mask", 24),
        tr("clock", "Clock Wipe", "Mask", 25),
        tr("blinds", "Blinds", "Mask", 26),
        tr("doors", "Doors", "Mask", 27),
        tr("glitch", "Glitch", "Effects", 28),
        tr("rgb-split", "RGB Split", "Effects", 29),
        tr("pixelate", "Pixelate", "Effects", 30),
        tr("swirl", "Swirl", "Effects", 31),
        tr("ripple", "Ripple", "Effects", 32),
        tr("flare", "Light Flare", "Effects", 33),
        tr("film-burn", "Film Burn", "Effects", 34),
        tr("kaleido", "Kaleido", "Effects", 35),
        tr("flash-zoom", "Flash Zoom", "Effects", 36),
        tr("stripes", "Stripes", "Mask", 37),
    )
    private val trendingTransitions = listOf("fx-tr-black-fade", "fx-tr-vertical-blur", "fx-tr-shake", "fx-tr-slam-merge", "fx-tr-mix", "fx-tr-flash-zoom",
        "fx-tr-pull-in", "fx-tr-heat-flicks", "fx-tr-glare", "fx-tr-cutout-scan", "fx-tr-fake-zoom", "fx-tr-zoom-swipe", "fx-tr-panel-clasp",
        "fx-tr-heatwave-flash", "fx-tr-whip-left", "fx-tr-zoom-spin", "fx-tr-glitch",
        "fx-tr-white-flash", "fx-tr-slide-left", "fx-tr-circle", "fx-tr-film-burn", "fx-tr-rgb-split")
    // Grids key tiles by id, so every listing must be free of repeats.
    fun transitionsIn(category: String) = (if (category == "Trending") trendingTransitions.mapNotNull { byId[it] } else transitions.filter { it.category == category }).distinctBy { it.id }
    /** Older projects stored transition names; map them onto the new specs. */
    fun transitionSpec(value: String): VideoFxSpec? = byId[value] ?: byId[when (value) {
        "Dip black" -> "fx-tr-black-fade"; "Dip white" -> "fx-tr-white-flash"; "Flash" -> "fx-tr-white-flash"
        "Zoom burst" -> "fx-tr-pull-in"; "Glitch" -> "fx-tr-glitch"; "Whip left" -> "fx-tr-whip-left"; else -> ""
    }]

    /** Full-frame overlay layers from the Overlay library, drawn over the picture with screen/blend looks. */
    private fun ov(id: String, name: String, category: String, mode: Int, hue: Float, sample: Int = 15 + mode % 6) =
        VideoFxSpec("fx-ov-$id", name, category, FX_OVERLAYS, "#define MODE $mode",
            listOf(FxParam("intensity", "Opacity", .85f), FxParam("speed", "Speed", .45f), FxParam("color", "Colour", hue)), sample = sample)
    /** Second-generation library looks; backgrounds and scenery default to full opacity like CapCut's stock clips. */
    private fun ov2(id: String, name: String, category: String, mode: Int, sample: Int, hue: Float? = null, opacity: Float = .85f) =
        VideoFxSpec("fx-ov-$id", name, category, FX_OVERLAYS2, "#define MODE $mode",
            listOf(FxParam("intensity", "Opacity", opacity), FxParam("speed", "Speed", .45f)) + listOfNotNull(hue?.let { FxParam("color", "Colour", it) }), sample = sample)
    val overlayCategories = listOf("Trending", "Atmosphere", "Light", "Background", "Scenery", "Transitions", "Intro & End", "Elements", "Texture", "Frames")
    val overlays: List<VideoFxSpec> = listOf(
        // Atmosphere
        ov2("glitter-rain", "Glitter Rain", "Atmosphere", 34, 17, hue = .92f),
        ov2("gold-dust", "Gold Dust", "Atmosphere", 35, 16),
        ov("bokeh-gold", "Bokeh Lights", "Atmosphere", 3, 0.12f, 16),
        ov("bokeh-pink", "Pink Bokeh", "Atmosphere", 3, 0.93f, 18),
        ov2("sparks", "Sparks", "Atmosphere", 36, 16),
        ov("embers", "Embers", "Atmosphere", 15, 0.06f, 19),
        ov2("smoke", "Smoke", "Atmosphere", 37, 16),
        ov("fog-white", "Fog", "Atmosphere", 14, 0.6f, 12),
        ov2("neon-ring", "Neon Ring", "Atmosphere", 38, 16, hue = .83f),
        ov2("neon-hearts", "Neon Hearts", "Atmosphere", 39, 16, hue = .9f),
        ov2("galaxy", "Galaxy", "Atmosphere", 40, 16),
        ov2("fireflies", "Fireflies", "Atmosphere", 41, 11),
        ov2("magic-dust", "Magic Dust", "Atmosphere", 42, 18, hue = .75f),
        ov2("swirl", "Light Swirl", "Atmosphere", 43, 16),
        ov("sparkle-white", "Sparkles", "Atmosphere", 7, 0.15f, 20),
        ov("glitter", "Gold Glitter", "Atmosphere", 8, 0.12f, 15),
        ov("hearts-pink", "Floating Hearts", "Atmosphere", 11, 0.95f, 20),
        ov("stars", "Twinkle Stars", "Atmosphere", 12, 0.14f, 16),
        ov("confetti", "Confetti", "Atmosphere", 13, 0.0f, 19),
        ov("snow", "Snow", "Atmosphere", 9, 0.6f, 5),
        ov("rain", "Rain", "Atmosphere", 10, 0.6f, 17),
        ov("bubbles", "Bubbles", "Atmosphere", 26, 0.55f, 18),
        // Light
        ov("leak-warm", "Light Leak Warm", "Light", 1, 0.07f, 15),
        ov("leak-pink", "Light Leak Pink", "Light", 1, 0.92f, 17),
        ov("leak-blue", "Light Leak Blue", "Light", 1, 0.58f, 12),
        ov2("light-streaks", "Light Streaks", "Light", 30, 16, hue = .58f),
        ov2("stage-lights", "Stage Lights", "Light", 31, 16, hue = .72f),
        ov2("prism-flare", "Prism Flare", "Light", 32, 19),
        ov("flare-gold", "Lens Flare", "Light", 2, 0.12f, 19),
        ov("sun-rays", "Sun Rays", "Light", 17, 0.1f, 11),
        ov2("spotlight", "Spotlight", "Light", 33, 17),
        // Background
        ov2("bg-purple-smoke", "Purple Smoke", "Background", 44, 15, opacity = 1f),
        ov2("bg-pastel-ink", "Pastel Ink", "Background", 45, 15, opacity = 1f),
        ov2("bg-pink-gradient", "Pink Gradient", "Background", 46, 15, opacity = 1f),
        ov2("bg-blue-hearts", "Blue Hearts", "Background", 47, 15, opacity = 1f),
        ov2("bg-pink-bokeh", "Pink Bokeh", "Background", 48, 15, opacity = 1f),
        ov2("bg-golden-band", "Golden Particles", "Background", 49, 15, opacity = 1f),
        ov2("bg-blue-smoke", "Blue Smoke", "Background", 50, 15, opacity = 1f),
        ov2("bg-soft-gradient", "Soft Gradient", "Background", 51, 15, opacity = 1f),
        ov2("bg-light-wall", "Light Wall", "Background", 52, 15, opacity = 1f),
        ov2("bg-3d-shapes", "3D Shapes", "Background", 53, 15, opacity = 1f),
        ov2("bg-red-hearts", "Red Hearts", "Background", 54, 15, opacity = 1f),
        // Scenery
        ov2("sc-aurora", "Aurora", "Scenery", 55, 15, opacity = 1f),
        ov2("sc-milky-way", "Milky Way", "Scenery", 56, 15, opacity = 1f),
        ov2("sc-sunset-sea", "Sunset Sea", "Scenery", 57, 15, opacity = 1f),
        ov2("sc-moon-dunes", "Moonlit Dunes", "Scenery", 58, 15, opacity = 1f),
        ov2("sc-clouds", "Cloud Time-lapse", "Scenery", 59, 15, opacity = 1f),
        ov2("sc-snowy-night", "Snowy Night", "Scenery", 60, 15, opacity = 1f),
        ov2("sc-ocean", "Ocean", "Scenery", 61, 15, opacity = 1f),
        ov2("sc-shooting-stars", "Shooting Stars", "Scenery", 62, 15, opacity = 1f),
        // Transitions
        ov2("tr-colour-wipe", "Colour Wipe", "Transitions", 63, 17, hue = .6f, opacity = 1f),
        ov2("tr-diamond", "Diamond Burst", "Transitions", 64, 20, opacity = 1f),
        ov2("tr-shape-pop", "Shape Pop", "Transitions", 65, 15, opacity = 1f),
        ov2("tr-glow-blob", "Glow Blob", "Transitions", 66, 18, opacity = 1f),
        ov2("tr-fire-wipe", "Fire Wipe", "Transitions", 67, 16, opacity = 1f),
        ov2("tr-ink", "Ink Wipe", "Transitions", 68, 19, opacity = 1f),
        ov2("tr-stripes", "Stripes", "Transitions", 69, 17, hue = .95f, opacity = 1f),
        ov2("tr-iris", "Circle Iris", "Transitions", 70, 20, hue = .12f, opacity = 1f),
        ov2("tr-light-burst", "Light Burst", "Transitions", 71, 16, opacity = 1f),
        ov2("tr-scribble", "Scribble", "Transitions", 72, 18, opacity = 1f),
        // Elements
        ov2("el-speed-lines", "Speed Lines", "Elements", 73, 12),
        ov2("el-lightning", "Lightning Strike", "Elements", 74, 16),
        ov2("el-moon", "Full Moon", "Elements", 75, 16, opacity = 1f),
        ov2("el-smoke-puff", "Smoke Puff", "Elements", 76, 19),
        ov2("el-scan-line", "Scan Line", "Elements", 77, 16, hue = .38f),
        ov2("el-stars-rating", "Star Rating", "Elements", 78, 19, opacity = 1f),
        // Texture
        ov("film-dust", "Film Dust", "Texture", 4, 0.0f, 20),
        ov("grain", "Heavy Grain", "Texture", 5, 0.0f, 12),
        ov("vhs", "VHS", "Texture", 6, 0.0f, 17),
        ov("halftone", "Halftone", "Texture", 24, 0.0f, 18),
        ov("paper", "Paper", "Texture", 25, 0.0f, 15),
        ov("burnt-edges", "Burnt Edges", "Texture", 28, 0.0f, 19),
        // Frames
        ov("frame-film", "Film Frame", "Frames", 19, 0.0f, 15),
        ov("frame-polaroid", "Polaroid Border", "Frames", 20, 0.0f, 20),
        ov("frame-neon-pink", "Neon Border Pink", "Frames", 21, 0.92f, 16),
        ov("frame-neon-blue", "Neon Border Blue", "Frames", 21, 0.55f, 17),
        ov("frame-neon-gold", "Neon Border Gold", "Frames", 21, 0.12f, 12),
        ov("frame-rec", "REC Camera", "Frames", 22, 0.0f, 18),
        ov("frame-vignette", "Dark Vignette", "Frames", 23, 0.0f, 11),
        ov("frame-grid", "Grid Lines", "Frames", 27, 0.0f, 19),
    )
    /** Looks from earlier versions: still resolvable for saved projects, but not listed in the library. */
    private val legacyOverlays = listOf(
        ov("leak-green", "Light Leak Green", "Light", 1, 0.36f), ov("leak-purple", "Light Leak Purple", "Light", 1, 0.78f),
        ov("leak-red", "Light Leak Red", "Light", 1, 0.0f), ov("flare-blue", "Lens Flare Blue", "Light", 2, 0.6f),
        ov("lightning", "Lightning", "Light", 29, 0.6f), ov("bokeh-blue", "Bokeh Blue", "Particles", 3, 0.6f),
        ov("bokeh-rainbow", "Bokeh Rainbow", "Particles", 3, 0.4f), ov("sparkle-pink", "Pink Sparkles", "Particles", 7, 0.92f),
        ov("sparkle-blue", "Blue Sparkles", "Particles", 7, 0.58f), ov("hearts-red", "Red Hearts", "Particles", 11, 0.0f),
        ov("fog-pink", "Pink Haze", "Atmosphere", 14, 0.92f), ov("fog-blue", "Blue Mist", "Atmosphere", 14, 0.58f),
        ov("wash-sunset", "Sunset Wash", "Colour", 18, 0.03f), ov("wash-ocean", "Ocean Wash", "Colour", 18, 0.55f),
        ov("wash-rose", "Rose Wash", "Colour", 18, 0.9f), ov("wash-neon", "Neon Wash", "Colour", 18, 0.78f),
        ov("wash-gold", "Golden Hour", "Colour", 18, 0.1f), ov("rainbow", "Rainbow", "Colour", 16, 0.0f),
        ov("frame-neon-green", "Neon Border Green", "Frames", 21, 0.33f),
    )
    private val cardExtras = setOf("el-subscribe-click", "el-like-counter", "el-heart-counter", "el-like-sub-bar")
    /** Library tiles for a category: overlay-look ids ("fx-ov-...") and animated card ids (drawn stickers). */
    fun overlayItems(category: String): List<String> = overlayItemsRaw(category).distinct()
    private fun overlayItemsRaw(category: String): List<String> = when (category) {
        "Trending" -> listOf("card-welcome-neon", "fx-ov-glitter-rain", "el-subscribe-click", "fx-ov-light-streaks", "fx-ov-bg-purple-smoke",
            "card-thanks-script", "fx-ov-sparks", "fx-ov-tr-diamond", "fx-ov-neon-hearts", "el-like-sub-bar", "fx-ov-sc-aurora", "fx-ov-leak-warm",
            "card-the-end-bold", "fx-ov-gold-dust", "fx-ov-stage-lights", "el-heart-counter", "fx-ov-tr-shape-pop", "fx-ov-galaxy", "fx-ov-bg-golden-band",
            "card-subscribe-neon", "fx-ov-sc-sunset-sea", "fx-ov-smoke", "el-rating", "fx-ov-frame-rec")
        "Intro & End" -> VideoAnimatedStickers.inCategory("Intro & End").map { it.id } + cardExtras
        "Elements" -> VideoAnimatedStickers.inCategory("Elements").map { it.id }.filterNot { it in cardExtras } +
            overlays.filter { it.category == "Elements" }.map { it.id }
        else -> overlays.filter { it.category == category }.map { it.id }
    }

    private val byId = (effects + transitions + overlays + legacyOverlays).associateBy { it.id }
    /** Home row: the three showcase looks first, then the rest of Trending. */
    fun homeTrending(): List<VideoFxSpec> = (listOf("fx-cc-chromatic", "fx-cc-retro-flicker", "fx-torn-reveal").mapNotNull { byId[it] } +
        inCategory("Trending")).distinctBy { it.id }.take(16)
    fun find(id: String): VideoFxSpec? = byId[id]
    /** Retired effects: saved projects that used them simply drop the layer. */
    val retired = setOf("fx-pinwheel-spin", "fx-pinwheel-bloom", "fx-pinwheel-portal", "fx-pinwheel-prism", "fx-breeze", "fx-pinwheel-pop", "fx-pinwheel-wipe")
    fun displayName(kind: String): String = byId[kind]?.name ?: kind
    fun inCategory(category: String): List<VideoFxSpec> =
        (if (category == "Trending") trendingIds.mapNotNull { byId[it] } else effects.filter { it.category == category }).distinctBy { it.id }
}
