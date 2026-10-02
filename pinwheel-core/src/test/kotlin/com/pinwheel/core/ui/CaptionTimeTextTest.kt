package com.pinwheel.core.ui

import org.junit.Assert.*
import org.junit.Test

class CaptionTimeTextTest {
    @Test fun millisecondTimesRoundTripIncludingLongVideos() {
        listOf(0L, 1L, 99L, 1234L, 60_001L, 3_600_017L, 86_400_000L).forEach { assertEquals(it, parseCaptionTime(captionTime(it))) }
        assertEquals(3_723_040L, parseCaptionTime("01:02:03.04"))
        assertEquals(1500L, parseCaptionTime("00:01,5"))
    }

    @Test fun invalidAndAmbiguousTimeFieldsDoNotBecomeZero() {
        listOf("", "word", "-00:01.000", "00:60.000", "25:00:00.000", "00:00.0001", "00:99:12.000", "999999999999:00", "NaN").forEach { assertNull(it, parseCaptionTime(it)) }
    }

    @Test fun timeFieldsStayAsciiAcrossDeviceLocales() {
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ar-EG"))
            assertEquals("01:01.250", captionTime(61_250))
            assertEquals(61_250L, parseCaptionTime(captionTime(61_250)))
        } finally { java.util.Locale.setDefault(previous) }
    }
}
