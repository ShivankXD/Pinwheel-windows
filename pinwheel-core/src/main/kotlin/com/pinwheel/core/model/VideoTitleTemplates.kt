package com.pinwheel.core.model

/** Small editable title sequences; source clips, audio and captions are never replaced. */
object VideoTitleTemplates {
    data class Template(val id: String, val name: String, val subtitle: String, val sceneId: String)
    val all = listOf(
        Template("travel", "Travel journal", "Opening, chapter and closing titles", "riviera-home"),
        Template("editorial", "Editorial", "Quiet titles with soft entrances", "cafe-still-life"),
        Template("weekend", "Weekend", "Bold, warm title cards", "lisbon-street"),
        Template("minimal", "Minimal", "A simple opening and sign-off", "atlantic-dusk"),
        Template("noir", "Noir", "High-contrast cinematic titles", "quiet-horizon"),
        Template("ghost", "Ghost story", "Pale, eerie type with a slow arrival", "alpine-lake"),
        Template("vhs", "VHS diary", "Narrow, nostalgic time-card titles", "portrait-outdoors"),
        Template("postcard", "Postcard", "Soft travel notes in serif type", "coast"),
        Template("dispatch", "Dispatch", "Urgent label-style headlines", "still-life"),
        Template("afterglow", "Afterglow", "Warm closing titles", "portrait-daylight"),
    )
    fun titles(id: String, durationMs: Long): List<VideoTextOverlay> {
        require(all.any { it.id == id })
        if (durationMs < 300) return emptyList()
        val duration = durationMs.coerceAtMost(24 * 60 * 60 * 1000L)
        val opening = (duration / 4).coerceIn(100, 3500)
        val closing = (duration / 5).coerceIn(100, 2500)
        val color = when (id) { "weekend", "afterglow" -> 0xffffdc73L; "ghost" -> 0xffe7efffL; "dispatch" -> 0xff151a22L; "postcard" -> 0xfff4e5ceL; "vhs" -> 0xffa4edecL; else -> 0xffffffffL }
        val font = when (id) { "editorial", "postcard", "ghost" -> "serif"; "vhs", "dispatch" -> "monospace"; "noir", "weekend" -> "sans-serif-black"; "afterglow" -> "cursive"; else -> "sans-serif" }
        val plate = when (id) { "dispatch" -> 0xffffdc73L; "vhs" -> 0xc9000000L; else -> 0L }
        val appearance = when (id) { "weekend", "dispatch", "vhs" -> "Label"; "noir", "ghost" -> "Outline"; "afterglow" -> "Neon"; else -> "Clean" }
        fun title(text: String, start: Long, end: Long, size: Float, y: Float) = VideoTextOverlay(
            text = text, startMs = start, endMs = end, size = size, y = y, color = color,
            background = plate, bold = id !in listOf("editorial", "postcard", "ghost", "minimal"), style = appearance,
            fontFamily = font, widthScale = if (id in listOf("noir", "dispatch")) 1.15f else 1f,
            animation = if (id in listOf("editorial", "minimal", "ghost", "postcard", "afterglow")) "Fade" else "Rise", animationDurationMs = if (id == "ghost") 900 else 500,
        ).sanitized()
        val first = when (id) { "travel" -> "A PLACE TO REMEMBER"; "editorial" -> "The everyday, beautifully"; "weekend" -> "WEEKEND STORIES";
            "noir" -> "AFTER DARK"; "ghost" -> "SOMETHING IS HERE"; "vhs" -> "PLAYBACK 01"; "postcard" -> "Wish you were here";
            "dispatch" -> "THIS JUST IN"; "afterglow" -> "Golden hour"; else -> "A moment in time" }
        val last = when (id) { "travel" -> "Until next time"; "editorial" -> "Made with intention"; "weekend" -> "LET'S DO IT AGAIN";
            "noir" -> "THE END"; "ghost" -> "DON'T LOOK BACK"; "vhs" -> "END OF TAPE"; "postcard" -> "See you soon";
            "dispatch" -> "STAY TUNED"; "afterglow" -> "Keep the light"; else -> "Thanks for watching" }
        val result = mutableListOf(title(first, 0, opening, if (id == "weekend") .075f else .06f, .46f))
        if (duration >= 9000 && id in listOf("travel", "weekend", "ghost", "vhs", "noir", "dispatch")) result += title(when (id) {
            "travel" -> "THE LITTLE DETAILS"; "weekend" -> "GOOD COMPANY"; "ghost" -> "A QUIET WARNING";
            "vhs" -> "REC • MEMORIES"; "noir" -> "CHAPTER TWO"; else -> "MORE TO COME"
        }, duration / 2, (duration / 2 + 2200).coerceAtMost(duration - closing), .045f, .72f)
        if (duration >= 1500) result += title(last, duration - closing, duration, .045f, .64f)
        return result
    }
}
