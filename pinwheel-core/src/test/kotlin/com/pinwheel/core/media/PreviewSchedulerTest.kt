package com.pinwheel.core.media

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Test

class PreviewSchedulerTest {
    @Test fun dragCoalescesFramesAndAlwaysEndsAtFullQualityLatestRecipe() = runBlocking {
        val frames = mutableListOf<Pair<Int, Boolean>>()
        PreviewScheduler<Int, Int>(interaction = { delay(25); it }, refinement = { delay(5); it }, discard = {}, settleMillis = 30).run(
            flow { for (i in 1..15) { emit(i); delay(2) } }
        ) { value, settled -> frames += value to settled }
        assertEquals(15 to true, frames.last())
        assertTrue("Conflation must skip obsolete intermediate edits", frames.count { !it.second } < 15)
    }

    @Test fun aNewEditCancelsAnExpensiveRefinementImmediately() = runBlocking {
        val refiningFirst = CompletableDeferred<Unit>()
        var firstCancelled = false
        val frames = mutableListOf<Pair<Int, Boolean>>()
        PreviewScheduler<Int, Int>(interaction = { it }, refinement = {
            if (it == 1) {
                refiningFirst.complete(Unit)
                try { delay(10_000) } finally { firstCancelled = true }
            }
            it
        }, discard = {}, settleMillis = 10).run(flow { emit(1); refiningFirst.await(); emit(2) }) { value, settled -> frames += value to settled }
        assertTrue(firstCancelled)
        assertFalse(frames.contains(1 to true))
        assertEquals(2 to true, frames.last())
    }

    @Test fun renderingNeverOverlapsAndInteractionCannotReplaceSettledFrame() = runBlocking {
        var active = 0; var peak = 0
        suspend fun work(value: Int): Int {
            active++; peak = maxOf(peak, active)
            try { delay(15); return value } finally { active-- }
        }
        val frames = mutableListOf<Pair<Int, Boolean>>()
        PreviewScheduler<Int, Int>(interaction = { work(it) }, refinement = { work(it) }, discard = {}, settleMillis = 1)
            .run(flow { emit(7); delay(4); emit(8) }) { value, settled -> frames += value to settled }
        assertEquals(1, peak)
        assertEquals(8 to true, frames.last())
    }

    @Test fun aFailedInteractionDoesNotPreventFullQualityRecovery() = runBlocking {
        var failures = 0
        val frames = mutableListOf<Pair<Int, Boolean>>()
        PreviewScheduler<Int, Int>(interaction = { error("Transient preview failure") }, refinement = { it }, discard = {}, settleMillis = 10)
            .run(flow { emit(3) }, failed = { failures++ }) { value, settled -> frames += value to settled }
        assertEquals(1, failures)
        assertEquals(listOf(3 to true), frames)
    }

    @Test fun finalInteractionFrameIsPublishedBeforeItsExpensiveRefinement() = runBlocking {
        val frames = mutableListOf<Pair<Int, Boolean>>()
        PreviewScheduler<Int, Int>(interaction = { delay(30); it }, refinement = { delay(40); it }, discard = {}, settleMillis = 1)
            .run(flow { emit(1); delay(5); emit(2) }) { value, settled -> frames += value to settled }
        assertTrue("Latest small preview must appear before the full render starts", frames.indexOf(2 to false) >= 0)
        assertTrue(frames.indexOf(2 to false) < frames.indexOf(2 to true))
    }
}
