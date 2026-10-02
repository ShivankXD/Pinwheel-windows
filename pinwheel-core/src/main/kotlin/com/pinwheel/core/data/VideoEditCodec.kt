package com.pinwheel.core.data

import com.pinwheel.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Optional fields keep projects from before the video editor compatible. Sanitize at both boundaries. */
object VideoEditCodec {
    fun encodeClip(value: VideoClipEdits): JSONObject = value.sanitized().let { v -> JSONObject().apply {
        put("rotation", v.rotation); put("mirrored", v.mirrored)
        put("exposure", v.exposure); put("contrast", v.contrast); put("saturation", v.saturation)
        put("warmth", v.warmth); put("lookId", v.lookId); put("lookIntensity", v.lookIntensity); put("volume", v.volume)
        put("motion", v.motion); put("motionAmount", v.motionAmount)
        put("cropZoom", v.cropZoom); put("cropX", v.cropX); put("cropY", v.cropY)
        put("speed", v.speed); put("transition", v.transition); put("transitionDurationMs", v.transitionDurationMs); put("transitionSpeed", v.transitionSpeed.toDouble())
        put("brightness", v.brightness); put("highlights", v.highlights); put("shadows", v.shadows); put("sharpen", v.sharpen)
        put("fade", v.fade); put("vignette", v.vignette); put("grain", v.grain); put("tint", v.tint); put("vibrance", v.vibrance)
    } }

    fun decodeClip(o: JSONObject?): VideoClipEdits {
        if (o == null) return VideoClipEdits()
        return VideoClipEdits(
            rotation = o.optInt("rotation"), mirrored = o.optBoolean("mirrored"),
            exposure = o.float("exposure", 0f), contrast = o.float("contrast", 0f),
            saturation = o.float("saturation", 0f), warmth = o.float("warmth", 0f),
            lookId = o.optString("lookId", "original"), lookIntensity = o.float("lookIntensity", 1f),
            volume = o.float("volume", 1f),
            speed = o.float("speed", 1f), transition = o.optString("transition", "Cut"),
            transitionDurationMs = o.optLong("transitionDurationMs", 600), transitionSpeed = o.float("transitionSpeed", .5f),
            motion = o.optString("motion", "None"), motionAmount = o.float("motionAmount", .35f),
            cropZoom = o.float("cropZoom", 1f), cropX = o.float("cropX", .5f), cropY = o.float("cropY", .5f),
            brightness = o.float("brightness", 0f), highlights = o.float("highlights", 0f), shadows = o.float("shadows", 0f),
            sharpen = o.float("sharpen", 0f), fade = o.float("fade", 0f), vignette = o.float("vignette", 0f),
            grain = o.float("grain", 0f), tint = o.float("tint", 0f), vibrance = o.float("vibrance", 0f),
        ).sanitized()
    }

    fun encodeProject(value: VideoProjectEdits): JSONObject = value.sanitized().let { v -> JSONObject().apply {
        put("aspectRatio", v.aspectRatio); put("scaleMode", v.scaleMode)
        put("backgroundColor", v.backgroundColor)
        put("backgroundMode", v.backgroundMode); put("backgroundBlur", v.backgroundBlur)
        put("effects", JSONArray().apply { v.effects.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("kind", e.kind); put("startMs", e.startMs); put("endMs", e.endMs)
            put("intensity", e.intensity); put("enabled", e.enabled); if (e.lane > 0) put("lane", e.lane)
            if (e.target.isNotEmpty()) put("target", e.target)
            if (e.params.isNotEmpty()) put("params", JSONObject().apply { e.params.forEach { (k, v) -> put(k, v.toDouble()) } })
        }) } })
        put("texts", JSONArray().apply { v.texts.forEach { t -> put(JSONObject().apply {
            put("id", t.id); put("text", t.text); put("startMs", t.startMs); put("endMs", t.endMs)
            put("x", t.x); put("y", t.y); put("size", t.size); put("color", t.color)
            put("background", t.background); put("bold", t.bold); put("style", t.style)
            put("animation", t.animation); put("animationDurationMs", t.animationDurationMs)
            put("fontFamily", t.fontFamily); put("italic", t.italic); put("alignment", t.alignment); put("widthScale", t.widthScale)
            if (t.lane > 0) put("lane", t.lane)
        }) } })
        put("images", JSONArray().apply { v.images.forEach { i -> put(JSONObject().apply {
            put("id", i.id); put("uri", i.uri); put("name", i.name); put("startMs", i.startMs); put("endMs", i.endMs)
            put("x", i.x); put("y", i.y); put("width", i.width); put("opacity", i.opacity)
            put("rotation", i.rotation); put("mirrored", i.mirrored)
            i.video?.let { source -> put("video", JSONObject().apply {
                put("durationMs", source.durationMs); put("startMs", source.startMs)
                put("width", source.width); put("height", source.height)
            }) }
            put("mask", i.mask); put("animIn", i.animIn); put("animOut", i.animOut); put("shadow", i.shadow); put("border", i.border)
            if (i.lane > 0) put("lane", i.lane)
        }) } })
        put("audio", JSONArray().apply { v.audio.forEach { a -> put(JSONObject().apply {
            put("id", a.id); put("uri", a.uri); put("name", a.name); put("sourceDurationMs", a.sourceDurationMs)
            put("sourceStartMs", a.sourceStartMs); put("sourceEndMs", a.sourceEndMs); put("startMs", a.startMs)
            put("volume", a.volume); put("fadeInMs", a.fadeInMs); put("fadeOutMs", a.fadeOutMs)
            put("speed", a.speed); put("voiceEffect", a.voiceEffect); put("reduceNoise", a.reduceNoise)
            put("extendsVideo", a.extendsVideo); if (a.lane > 0) put("lane", a.lane)
        }) } })
        put("captions", JSONArray().apply { v.captions.forEach { c -> put(JSONObject().apply {
            put("id", c.id); put("text", c.text); put("startMs", c.startMs); put("endMs", c.endMs)
            put("wordOffsetsMs", JSONArray().apply { c.wordOffsetsMs.forEach { put(it) } })
        }) } })
        put("captionStyle", JSONObject().apply {
            val s = v.captionStyle
            put("preset", s.preset); put("size", s.size); put("y", s.y); put("color", s.color)
            put("background", s.background); put("bold", s.bold); put("fontFamily", s.fontFamily); put("alignment", s.alignment)
            put("templateId", s.templateId); put("motion", s.motion); put("keywordHighlight", s.keywordHighlight)
        })
    } }

    fun decodeProject(o: JSONObject?): VideoProjectEdits {
        if (o == null) return VideoProjectEdits()
        return VideoProjectEdits(
            aspectRatio = o.optString("aspectRatio", "Original"), scaleMode = o.optString("scaleMode", "Fit"),
            backgroundColor = o.optLong("backgroundColor", 0xff000000L),
            backgroundMode = o.optString("backgroundMode", "Solid"), backgroundBlur = o.float("backgroundBlur", .5f),
            effects = o.optJSONArray("effects").objects(MAX_VIDEO_EFFECTS).map { e -> VideoTimedEffect(
                id = e.optString("id", ""), kind = e.optString("kind", "Vignette"),
                startMs = e.optLong("startMs", 0), endMs = e.optLong("endMs", 3000),
                intensity = e.float("intensity", .5f), enabled = e.optBoolean("enabled", true),
                params = e.optJSONObject("params")?.let { p -> p.keys().asSequence().take(12).associateWith { p.optDouble(it, .5).toFloat() } } ?: emptyMap(),
                lane = e.optInt("lane", 0), target = e.optString("target", ""),
            ) },
            texts = o.optJSONArray("texts").objects().map { t -> VideoTextOverlay(
                id = t.optString("id", ""), text = t.optString("text", "Your title"),
                startMs = t.optLong("startMs", 0), endMs = t.optLong("endMs", 3000),
                x = t.float("x", .5f), y = t.float("y", .5f), size = t.float("size", .07f),
                color = t.optLong("color", 0xffffffffL), background = t.optLong("background", 0),
                bold = t.optBoolean("bold", true), style = t.optString("style", "Clean"),
                animation = t.optString("animation", "None"), animationDurationMs = t.optLong("animationDurationMs", 300),
                fontFamily = t.optString("fontFamily", "sans-serif"), italic = t.optBoolean("italic", false), alignment = t.optString("alignment", "Center"),
                widthScale = t.float("widthScale", 1f), lane = t.optInt("lane", 0),
            ) },
            images = o.optJSONArray("images").objects().map { i -> VideoImageOverlay(
                id = i.optString("id", ""), uri = i.optString("uri", ""), name = i.optString("name", "Overlay"),
                startMs = i.optLong("startMs", 0), endMs = i.optLong("endMs", 3000),
                x = i.float("x", .5f), y = i.float("y", .5f), width = i.float("width", .35f), opacity = i.float("opacity", 1f),
                rotation = i.optInt("rotation", 0), mirrored = i.optBoolean("mirrored", false),
                mask = i.optString("mask", "None"), animIn = i.optString("animIn", "None"), animOut = i.optString("animOut", "None"),
                shadow = i.optBoolean("shadow", false), border = i.optLong("border", 0L),
                video = i.optJSONObject("video")?.let { source -> VideoOverlaySource(
                    durationMs = source.optLong("durationMs", 1), startMs = source.optLong("startMs", 0),
                    width = source.optInt("width", 1920), height = source.optInt("height", 1080)) },
                lane = i.optInt("lane", 0),
            ) },
            audio = o.optJSONArray("audio").objects(MAX_VIDEO_AUDIO_TRACKS).map { a -> VideoAudioTrack(
                id = a.optString("id", ""), uri = a.optString("uri", ""), name = a.optString("name", "Audio"),
                sourceDurationMs = a.optLong("sourceDurationMs", 0), sourceStartMs = a.optLong("sourceStartMs", 0),
                sourceEndMs = a.optLong("sourceEndMs", a.optLong("sourceDurationMs", 0)), startMs = a.optLong("startMs", 0),
                volume = a.float("volume", .7f), fadeInMs = a.optLong("fadeInMs", 0), fadeOutMs = a.optLong("fadeOutMs", 0),
                speed = a.float("speed", 1f), voiceEffect = a.optString("voiceEffect", "None"), reduceNoise = a.optBoolean("reduceNoise", false),
                extendsVideo = a.optBoolean("extendsVideo", false), lane = a.optInt("lane", 0),
            ) },
            captions = o.optJSONArray("captions").objects(MAX_VIDEO_CAPTIONS).map { c -> VideoCaptionCue(
                id = c.optString("id", ""), text = c.optString("text", ""),
                startMs = c.optLong("startMs", 0), endMs = c.optLong("endMs", 3000),
                wordOffsetsMs = c.optJSONArray("wordOffsetsMs")?.let { a -> (0 until minOf(a.length(), 80)).map { a.optLong(it) } } ?: emptyList(),
            ) },
            captionStyle = o.optJSONObject("captionStyle")?.let { s -> VideoCaptionStyle(
                preset = s.optString("preset", "Outline"), size = s.float("size", .055f), y = s.float("y", .84f),
                color = s.optLong("color", 0xffffffffL), background = s.optLong("background", 0),
                bold = s.optBoolean("bold", true), fontFamily = s.optString("fontFamily", "sans-serif"), alignment = s.optString("alignment", "Center"),
                templateId = s.optString("templateId", ""), motion = s.optString("motion", ""),
                keywordHighlight = s.optBoolean("keywordHighlight", false),
            ) } ?: VideoCaptionStyle(),
        ).sanitized()
    }

    private fun JSONObject.float(key: String, default: Float) = optDouble(key, default.toDouble()).toFloat()
    private fun JSONArray?.objects(limit: Int = MAX_VIDEO_OVERLAYS): List<JSONObject> = if (this == null) emptyList() else
        (0 until minOf(length(), limit)).mapNotNull { optJSONObject(it) }
}
