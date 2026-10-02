package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoTitleTemplatesTest {
    @Test fun everyTemplateFitsShortAndLongProjectsWithoutOverlappingItsTitles() {
        for (template in VideoTitleTemplates.all) for (duration in listOf(100L, 300L, 1499L, 1500L, 9000L, 30000L, 86400000L)) {
            val titles = VideoTitleTemplates.titles(template.id, duration)
            assertTrue(titles.all { it.startMs >= 0 && it.endMs <= duration && it.endMs > it.startMs })
            assertTrue(titles.zipWithNext().all { (a, b) -> a.endMs <= b.startMs })
            assertEquals(titles.size, titles.map { it.id }.distinct().size)
        }
    }
}
