package com.pinwheel.core.data

import com.pinwheel.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SrtCodecTest {
    @Test fun bomCrLfMultilineUnicodeAndDecimalFractionsImport() {
        val result = SrtCodec.parse("\uFEFF1\r\n00:00:00,25 --> 00:00:01.500\r\nHello\r\nनमस्ते 😀\r\n\r\n2\r\n00:00:01,500 --> 00:00:03,000\r\nNext")
        assertEquals(2, result.cues.size); assertEquals(0, result.skippedCues); assertTrue(result.warnings.isEmpty())
        assertEquals(250L, result.cues[0].startMs); assertEquals(1500L, result.cues[0].endMs)
        assertEquals("Hello\nनमस्ते 😀", result.cues[0].text)
    }

    @Test fun malformedOverflowReversedAndEmptyCuesReportSkips() {
        val result = SrtCodec.parse("""
            1
            999999999999999999999:00:00,000 --> 00:00:01,000
            Overflow

            2
            00:60:00,000 --> 00:61:00,000
            Invalid minute

            3
            00:00:02,000 --> 00:00:01,000
            Reversed

            4
            00:00:01,000 --> 00:00:02,000

            5
            00:00:03,000 --> 00:00:04,000
            Keep
        """.trimIndent())
        assertEquals(listOf("Keep"), result.cues.map { it.text }); assertEquals(4, result.skippedCues)
        assertTrue(result.warnings.size >= 4)
    }

    @Test fun sortAndOverlapPolicyKeepEarliestAndAllowTouchingEnds() {
        val result = SrtCodec.parse("""
            1
            00:00:03,000 --> 00:00:04,000
            Third

            2
            00:00:01,000 --> 00:00:02,000
            First

            3
            00:00:01,500 --> 00:00:03,000
            Overlap

            4
            00:00:02,000 --> 00:00:03,000
            Second
        """.trimIndent())
        assertEquals(listOf("First", "Second", "Third"), result.cues.map { it.text })
        assertEquals(1, result.skippedCues); assertTrue(result.warnings.any { "overlaps" in it })
    }

    @Test fun durationClipsTailAndReportsOutsideCues() {
        val result = SrtCodec.parse("00:00:01,000 --> 00:00:04,000\nTrim\n\n00:00:05,000 --> 00:00:06,000\nOutside", 3000)
        assertEquals(3000L, result.cues.single().endMs); assertEquals(1, result.skippedCues)
        assertEquals(2, result.warnings.size)
    }

    @Test fun missingSeparatorsRecoverAndInlineStylingIsReported() {
        val result = SrtCodec.parse("1\n00:00:00,000 --> 00:00:01,000\n<i>First</i>\n2\n00:00:01,000 --> 00:00:02,000\nSecond")
        assertEquals(listOf("First", "Second"), result.cues.map { it.text })
        assertEquals(0, result.skippedCues); assertTrue(result.warnings.any { "separators" in it })
        assertTrue(result.warnings.any { "styling" in it })
    }

    @Test fun limitsAreBoundedAndImportReportsEveryDiscard() {
        val cues = (0..MAX_VIDEO_CAPTIONS).joinToString("\n\n") { index ->
            val sec = index % 60; val min = index / 60
            val start = String.format(Locale.ROOT, "00:%02d:%02d,000", min, sec)
            val end = String.format(Locale.ROOT, "00:%02d:%02d,500", min, sec)
            "$index\n$start --> $end\nCaption"
        }
        val result = SrtCodec.parse(cues)
        assertEquals(MAX_VIDEO_CAPTIONS, result.cues.size); assertEquals(1, result.skippedCues)
        assertTrue(result.warnings.any { "limit" in it })
        val large = SrtCodec.parse("x".repeat(SrtCodec.MAX_INPUT_CHARACTERS + 1))
        assertTrue(large.cues.isEmpty()); assertTrue(large.warnings.isNotEmpty())
    }

    @Test fun writerIsCanonicalLocaleIndependentAndRoundTripsMultilineText() {
        val cues = listOf(VideoCaptionCue(text = "Second", startMs = 2000, endMs = 3000),
            VideoCaptionCue(text = "First\nहिन्दी", startMs = 123, endMs = 1000))
        val oldLocale = Locale.getDefault()
        val text = try { Locale.setDefault(Locale.forLanguageTag("ar")); SrtCodec.write(cues) } finally { Locale.setDefault(oldLocale) }
        assertTrue(text.startsWith("1\n00:00:00,123 --> 00:00:01,000\nFirst\nहिन्दी\n\n2\n"))
        assertEquals(text, SrtCodec.write(cues)); assertEquals(text, SrtCodec.write(SrtCodec.parse(text).cues))
        assertEquals("", SrtCodec.write(emptyList()))
    }

    @Test fun writerRejectsInvalidOrOverlappingCuesRatherThanDroppingThem() {
        assertTrue(runCatching { SrtCodec.write(listOf(VideoCaptionCue(text = ""))) }.isFailure)
        assertTrue(runCatching { SrtCodec.write(listOf(VideoCaptionCue(text = "Bad", startMs = 1000, endMs = 500))) }.isFailure)
        assertTrue(runCatching { SrtCodec.write(listOf(VideoCaptionCue(text = "First", endMs = 3000), VideoCaptionCue(text = "Second", startMs = 2000, endMs = 4000))) }.isFailure)
    }

    @Test fun ordinaryArrowsInCaptionTextAreNotTreatedAsTimingLines() {
        val cues = listOf(VideoCaptionCue(text = "Follow A --> B\n1 --> 2", endMs = 2000))
        val decoded = SrtCodec.parse("1\n00:00:00,000 --> 00:00:02,000\n${cues.single().text}")
        assertEquals(cues.single().text, decoded.cues.single().text)
        assertTrue(decoded.warnings.isEmpty())
    }

    @Test fun commonAndNumericEntitiesDecodeOnceAfterActualTagsAreRemoved() {
        val source = "1\n00:00:00,000 --> 00:00:02,000\n<b>Title</b> &amp; &lt;i&gt; &quot; &apos; &nbsp; &#128512; &#x1F600; &#X41; &amp;lt; &#xD800; &#1114112; &unknown;"
        val result = SrtCodec.parse(source)
        assertEquals("Title & <i> \" ' \u00a0 😀 😀 A &lt; &#xD800; &#1114112; &unknown;", result.cues.single().text)
        assertEquals(0, result.skippedCues)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings.single().contains("styling"))
    }

    @Test fun writerEscapesLiteralTagsAndEntitiesForLosslessPlainTextRoundTrip() {
        val original = VideoCaptionCue(text = "Use <i> literally &amp; &unknown; A --> B", endMs = 2000)
        val written = SrtCodec.write(listOf(original))
        assertTrue(written.contains("&lt;i&gt; literally &amp;amp;"))
        assertEquals(original.text, SrtCodec.parse(written).cues.single().text)
    }
}
