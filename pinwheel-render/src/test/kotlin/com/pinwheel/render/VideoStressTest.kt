package com.pinwheel.render

import com.pinwheel.render.qa.P3HeavyFixture
import com.pinwheel.media.*
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.*

/** Engine portion of mobile stress. Painter/encoder assertions remain explicit acceptance gaps. */
class VideoStressTest {
    private val root=Path.of("").toAbsolutePath()
    private val native=root.resolve("native/windows-x64")
    @Test fun heavyProjectPlaysThroughWithVideoLayerAndThreeSounds() {
        val project=P3HeavyFixture(root,root.resolve("pinwheel-render/build/stress-fixtures")).heavy(pip=true)
        assertEquals(8,project.clips.size);assertEquals(40,project.video.effects.size);assertEquals(3,project.video.audio.size)
        val started=System.nanoTime()
        val measuring=AtomicBoolean(false);val lastFrame=AtomicLong();val longestGap=AtomicLong()
        VideoPlayer(project,{GpuFrameEvaluator(native,it,720,1280)},LibavDecoderFactory(native),{ClockedTestOutput()},frameRate=60,onFrame={
            if(measuring.get()) { val now=System.nanoTime();val previous=lastFrame.getAndSet(now);longestGap.accumulateAndGet(now-previous,::maxOf) }
        }).use { player ->
            awaitPlayer(player,12000) { it.ready };lastFrame.set(System.nanoTime());measuring.set(true);player.play()
            awaitPlayer(player,project.durationMs*2+8000) { it.timeUs>=(project.durationMs-300)*1000 }
            measuring.set(false);longestGap.accumulateAndGet(System.nanoTime()-lastFrame.get(),::maxOf)
            val seconds=(System.nanoTime()-started)/1e9
            val fps=player.status.renderedFrames/seconds;val gapMs=longestGap.get()/1e6
            println("P3 engine-only stress: ${project.durationMs} ms, ${player.status.renderedFrames} frames in $seconds s, $fps fps, maximum frame gap $gapMs ms, ${player.status.droppedFrames} dropped; excludes missing sticker/title/caption painters and P5 encoder")
            assertTrue(player.status.renderedFrames>100)
            assertTrue(gapMs<=2500,"Preview froze for $gapMs ms; mobile limit is 2500 ms")
            if(java.lang.Boolean.getBoolean("pinwheel.p3.performance")) assertTrue(fps>=24,"Engine preview $fps fps is below the unchanged mobile 24 fps gate")
        }
    }
    @Test fun seeksPastEarlySoundEndAndStartsNearAudioCutsKeepAdvancing() {
        val project=P3HeavyFixture(root,root.resolve("pinwheel-render/build/stress-seeks")).heavy(seed=11,pip=true)
        VideoPlayer(project,{GpuFrameEvaluator(native,it,270,480)},LibavDecoderFactory(native),{ClockedTestOutput()}).use { player->
            awaitPlayer(player,12000) { it.ready }
            for(at in listOf(11_900_000L,12_411_000L,15_000_000L)) {
                player.pause();player.seek(at);awaitPlayer(player) { it.ready };player.play();awaitPlayer(player,5000) { it.timeUs>=at+600_000 }
            }
        }
    }
}
