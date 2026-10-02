package com.pinwheel.core.media.captions

import org.junit.Assert.*
import org.junit.Test

class CaptionWordsTest {
    @Test fun timingsFollowSourceSpeedAndCompositionOffset() {
        val cues=captionWordsToCues(listOf(CaptionWord("Please",640),CaptionWord("bring",1120),CaptionWord("umbrella",2080)),3035,2f,4000)
        assertEquals(4320L,cues.first().startMs);assertEquals(5518L,cues.last().endMs)
        assertEquals("Please bring",cues.first().text)
        assertTrue(cues.zipWithNext().all{(a,b)->a.endMs<=b.startMs})
    }
    @Test fun speechPausesCreateSeparateReadableCues() {
        val cues=captionWordsToCues(listOf(CaptionWord("One",0),CaptionWord("two",300),CaptionWord("Three",2200)),4000,.5f,0)
        assertEquals(2,cues.size);assertEquals("One two",cues[0].text);assertEquals(4400L,cues[1].startMs)
    }
    @Test(expected=IllegalArgumentException::class) fun backwardsServiceTimingIsRejected() {
        captionWordsToCues(listOf(CaptionWord("wrong",1000),CaptionWord("clock",400)),2000,1f,0)
    }
    @Test fun fillerWordsAreRemovedButRealWordsKeepTheirTiming() {
        val kept=removeFillerWords(listOf(CaptionWord("So,",0),CaptionWord("um",200),CaptionWord("Uh...",400),CaptionWord("humble",600),CaptionWord("hmm",900)))
        assertEquals(listOf("So,","humble"),kept.map{it.text});assertEquals(600L,kept[1].startMs)
    }
    @Test fun voiceoverSpeedsOutsideClipRangeStillMapToTheTimeline() {
        val cues=captionWordsToCues(listOf(CaptionWord("Fast",0),CaptionWord("talk",500)),1000,8f,1000)
        assertEquals(1000L,cues.first().startMs);assertEquals(listOf(0L,63L),cues.first().wordOffsetsMs)
    }
}
