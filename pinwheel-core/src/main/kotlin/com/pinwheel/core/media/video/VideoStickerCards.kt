package com.pinwheel.core.media.video

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The second sticker set: social call-to-actions, chat bubbles, doodles, animated elements and the
 * CapCut-style Intro & End cards. Everything is drawn from vectors and fonts as a function of time,
 * centred on (0, 0) in a square of side s, so previews, playback and export match.
 */
object VideoStickerCards {
    val entries: List<VideoAnimatedStickers.Entry> = listOf(
        // Social
        e("social-share", "Share", "Social"), e("social-comment", "Comment", "Social"), e("social-save", "Save", "Social"),
        e("social-link-bio", "Link in Bio", "Social"), e("social-swipe-up", "Swipe Up", "Social"), e("social-live", "Live", "Social"),
        e("social-new-video", "New Video", "Social"), e("social-follow-more", "Follow for More", "Social"), e("social-verified", "Verified", "Social"),
        e("social-notify", "Notification", "Social"), e("social-dm", "Send", "Social"),
        // Text
        e("text-thank-you", "Thank You", "Text"), e("text-hello", "Hello", "Text"), e("text-oops", "Oops", "Text"),
        e("text-wait", "Wait For It", "Text"), e("text-coming-soon", "Coming Soon", "Text"), e("text-sold-out", "Sold Out", "Text"),
        e("text-tutorial", "Tutorial", "Text"), e("text-tip", "Pro Tip", "Text"), e("text-day-one", "Day 1", "Text"),
        e("text-before", "Before", "Text"), e("text-after", "After", "Text"), e("text-top-five", "Top 5", "Text"),
        e("text-breaking", "Breaking News", "Text"), e("text-level-up", "Level Up", "Text"), e("text-best-day", "Best Day Ever", "Text"),
        // Chat
        e("chat-hi", "Hi!", "Chat"), e("chat-thought", "Thinking", "Chat"), e("chat-zzz", "Zzz", "Chat"),
        e("chat-what", "?!", "Chat"), e("chat-love", "Love Note", "Chat"), e("chat-haha", "Haha", "Chat"),
        // Doodle
        e("doodle-sun", "Sun", "Doodle"), e("doodle-moon", "Moon", "Doodle"), e("doodle-bolt", "Zap", "Doodle"),
        e("doodle-music", "Music", "Doodle"), e("doodle-crown", "Crown", "Doodle"), e("doodle-squiggle", "Squiggle", "Doodle"),
        e("doodle-caution", "Caution Tape", "Doodle"), e("doodle-sparkle", "Sparkle Pop", "Doodle"), e("doodle-flower", "Flower", "Doodle"),
        // Elements
        e("el-cursor-click", "Cursor Click", "Elements"), e("el-like-sub-bar", "Like & Subscribe", "Elements"), e("el-loading-bar", "Loading", "Elements"),
        e("el-tap-here", "Tap Here", "Elements"), e("el-rating", "5 Stars", "Elements"), e("el-countdown", "Countdown", "Elements"),
        e("el-subscribe-click", "Subscribe Click", "Elements"), e("el-like-counter", "Like Counter", "Elements"), e("el-heart-counter", "Heart Counter", "Elements"),
        // Intro & End
        e("card-welcome-neon", "Welcome Neon", "Intro & End"), e("card-welcome-bubbles", "Welcome", "Intro & End"), e("card-hello-neon", "Hello Neon", "Intro & End"),
        e("card-subscribe-neon", "Subscribe Neon", "Intro & End"), e("card-thanks-script", "Thanks Gold", "Intro & End"), e("card-thanks-box", "Thanks Box", "Intro & End"),
        e("card-the-end-bold", "The End", "Intro & End"), e("card-the-end-drip", "The End Drip", "Intro & End"), e("card-the-end-paper", "The End Card", "Intro & End"),
        e("card-enjoy-life", "Enjoy My Life", "Intro & End"), e("card-style-stack", "Style", "Intro & End"), e("card-loading-arrows", "Loading", "Intro & End"),
        e("card-see-you", "See You Soon", "Intro & End"), e("card-episode", "Episode 1", "Intro & End"),
    )
    private fun e(id: String, name: String, category: String) = VideoAnimatedStickers.Entry(id, name, category)
    private val ids = entries.map { it.id }.toSet()
    fun owns(id: String) = id in ids

}
