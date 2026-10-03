package com.pinwheel.media

import com.pinwheel.core.RgbaFrame
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class VideoFrameSourceTest {
    private class TestFactory(private val blocked:Boolean=false):MediaDecoderFactory {
        val entered=CountDownLatch(1);val release=CountDownLatch(if(blocked)1 else 0)
        val live=AtomicInteger();val peak=AtomicInteger();val opened=AtomicInteger();val closed=AtomicInteger()
        @Volatile var opener=""
        override fun open(source:Path,config:DecodeConfig):MediaDecoder {
            opener=Thread.currentThread().name;entered.countDown();release.await()
            opened.incrementAndGet();peak.accumulateAndGet(live.incrementAndGet(),::maxOf)
            return object:MediaDecoder {
                private var time=0L;private var generation=0L;private var disposed=false
                override val description=MediaDescription(3_000_000,2,2,null,null,false)
                override val config=config
                override fun seek(targetUs:Long):Long { time=targetUs;return ++generation }
                override fun pollVideo(timeout:Duration):DecodedVideo = DecodedVideo(time,generation,RgbaFrame(2,2,ByteArray(16))).also { time+=33_333 }
                override fun pollAudio(timeout:Duration):DecodedAudio?=null
                override val videoEnded=false
                override val audioEnded=true
                override fun close() { if(!disposed) { disposed=true;live.decrementAndGet();closed.incrementAndGet() } }
            }
        }
    }
    @Test fun preparationRunsOffCallerAndReadyInputIsReusedWithinGlobalLimit() {
        val factory=TestFactory(blocked=true)
        VideoFrameSource(factory,DecodeConfig(decodeAudio=false),maxOpen=2).use { source ->
            source.prefetch("one.mp4",500_000,"pip-one")
            assertTrue(factory.entered.await(2,TimeUnit.SECONDS));assertEquals("pinwheel-video-prefetch",factory.opener)
            // maxOpen=2 permits only one pending input, reserving a slot for foreground work.
            source.prefetch("ignored.mp4",0,"pip-two");factory.release.countDown()
            source.frame("one.mp4",500_000,identity="pip-one");source.frame("one.mp4",510_000,identity="pip-one")
            assertEquals(1,factory.opened.get())
            source.frame("two.mp4",0);source.frame("three.mp4",0)
            assertTrue(factory.peak.get()<=2);assertEquals(2,factory.live.get())
        }
        assertEquals(0,factory.live.get());assertEquals(factory.opened.get(),factory.closed.get())
    }
    @Test fun closeCancelsQueuedPreparationAndJoinsItsWorker() {
        val factory=TestFactory(blocked=true)
        val source=VideoFrameSource(factory,DecodeConfig(decodeAudio=false),maxOpen=3)
        source.prefetch("one.mp4");assertTrue(factory.entered.await(2,TimeUnit.SECONDS));source.prefetch("two.mp4")
        source.close()
        assertEquals(0,factory.opened.get());assertEquals(0,factory.live.get())
        assertFalse(Thread.getAllStackTraces().keys.any { it.isAlive && it.name=="pinwheel-video-prefetch" })
    }
    @Test fun failedSpeculativeInputDoesNotLeakOrHideItsFailureWhenActivated() {
        val factory=object:MediaDecoderFactory { override fun open(source:Path,config:DecodeConfig):MediaDecoder=error("bad media fixture") }
        VideoFrameSource(factory,DecodeConfig(decodeAudio=false)).use { source ->
            source.prefetch("bad.mp4")
            val failure=assertFails { source.frame("bad.mp4",0) }
            assertEquals("bad media fixture",failure.cause?.message)
        }
    }
}
