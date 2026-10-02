package com.pinwheel.core.media.video

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Animated stickers drawn fresh each frame as a function of time, so the picker, playback and
 * export all show the same motion. Stored on an overlay layer as the uri "sticker:<id>".
 */
object VideoAnimatedStickers {
    const val PREFIX = "sticker:"
    data class Entry(val id: String, val name: String, val category: String, val emoji: String = "", val motion: String = "")

    val categories = listOf("Trending", "Social", "Text", "Emoji", "Chat", "Love", "Arrows", "Doodle", "Party", "Elements", "Intro & End")

    private val emojis = listOf(
        "laugh" to ("😂" to "bounce"), "heart-eyes" to ("😍" to "pulse"), "fire" to ("🔥" to "flicker"), "hundred" to ("💯" to "pop"),
        "thumbs" to ("👍" to "wiggle"), "clap" to ("👏" to "clap"), "tada" to ("🎉" to "pop"), "red-heart" to ("❤️" to "beat"),
        "cool" to ("😎" to "tilt"), "mind-blown" to ("🤯" to "shake"), "crying" to ("😭" to "bounce"), "party-face" to ("🥳" to "wiggle"),
        "sparkles" to ("✨" to "twinkle"), "skull" to ("💀" to "wiggle"), "hands-up" to ("🙌" to "bounce"), "eyes" to ("👀" to "shift"),
        "scream" to ("😱" to "shake"), "thinking" to ("🤔" to "tilt"), "boom" to ("💥" to "pop"), "star" to ("⭐" to "spin"),
        "rainbow" to ("🌈" to "float"), "rocket" to ("🚀" to "launch"), "cake" to ("🎂" to "float"), "sparkling-heart" to ("💖" to "beat"),
    )

    val entries: List<Entry> = emojis.map { (id, v) -> Entry("emoji-$id", v.first, "Emoji", v.first, v.second) } + listOf(
        Entry("text-wow", "WOW", "Text"), Entry("text-omg", "OMG", "Text"), Entry("text-lol", "LOL", "Text"), Entry("text-new", "NEW", "Text"),
        Entry("text-sale", "SALE", "Text"), Entry("text-lets-go", "LET'S GO", "Text"), Entry("text-yes", "YES!", "Text"),
        Entry("text-no-way", "NO WAY", "Text"), Entry("text-brb", "BRB", "Text"), Entry("text-hot", "HOT", "Text"),
        Entry("love-beat", "Heartbeat", "Love"), Entry("love-burst", "Heart Burst", "Love"), Entry("love-text", "Love", "Love"),
        Entry("love-float", "Floating Hearts", "Love"),
        Entry("arrow-right", "Arrow", "Arrows"), Entry("arrow-down", "Look Down", "Arrows"), Entry("mark-circle", "Circle It", "Arrows"),
        Entry("mark-underline", "Underline", "Arrows"), Entry("mark-check", "Tick", "Arrows"), Entry("mark-cross", "Cross", "Arrows"),
        Entry("point-right", "Point", "Arrows", "👉", "nudge"), Entry("mark-exclaim", "Alert", "Arrows"), Entry("mark-question", "Question", "Arrows"),
        Entry("mark-emphasis", "Emphasis", "Arrows"),
        Entry("party-confetti", "Confetti Pop", "Party"), Entry("party-sparkles", "Twinkles", "Party"), Entry("party-fireworks", "Fireworks", "Party"),
        Entry("party-balloon", "Balloon", "Party", "🎈", "float"), Entry("party-crown", "Crown", "Party", "👑", "shine"),
        Entry("party-champagne", "Cheers", "Party", "🍾", "wiggle"), Entry("party-gift", "Gift", "Party", "🎁", "wiggle"),
        Entry("party-disco", "Disco", "Party", "🪩", "spin"),
        Entry("social-like", "Like", "Social"), Entry("social-heart", "Heart Like", "Social"), Entry("social-typing", "Typing", "Social"),
        Entry("social-bell", "Bell", "Social", "🔔", "ring"), Entry("social-pin", "Location", "Social", "📍", "drop"),
        Entry("social-follow", "Follow", "Social"), Entry("social-subscribe", "Subscribe", "Social"), Entry("social-play", "Play", "Social"),
    ) + VideoStickerCards.entries
    private val trending = listOf("social-subscribe", "social-like", "el-subscribe-click", "el-like-sub-bar", "social-heart", "social-follow-more",
        "text-wow", "emoji-fire", "social-link-bio", "el-cursor-click", "social-live", "love-burst", "emoji-laugh", "mark-circle", "chat-hi",
        "party-confetti", "text-omg", "doodle-sparkle", "social-swipe-up", "el-rating", "text-breaking", "social-verified", "emoji-hundred", "doodle-bolt")

    private val byId = entries.associateBy { it.id }
    fun find(id: String) = byId[id]
    fun idOf(uri: String): String? = uri.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.takeIf { it in byId }
    fun inCategory(c: String) = if (c == "Trending") trending.mapNotNull { byId[it] } else entries.filter { it.category == c }

}
