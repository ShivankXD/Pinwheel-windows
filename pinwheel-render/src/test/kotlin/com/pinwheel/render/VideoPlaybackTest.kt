package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.*
import com.pinwheel.media.*
import java.nio.file.Path
import java.nio.file.Files
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

/** Models device consumption and a bounded 60 ms device buffer; no wall-clock fallback in player. */
internal class ClockedTestOutput:PcmOutput {
    private var started=false;private var base=0L;private var written=0L;private var nano=0L
    @get:Synchronized override val consumedFrames:Long get()=if(started)minOf(written,base+(System.nanoTime()-nano)*48000/1_000_000_000) else base
    @get:Synchronized override val availableFrames:Int get()=(2880-(written-consumedFrames)).toInt().coerceAtLeast(0)
    @Synchronized override fun start() { if(!started) { nano=System.nanoTime();started=true } }
    @Synchronized override fun stop() { base=consumedFrames;started=false }
    @Synchronized override fun flush() { base=consumedFrames;written=base;nano=System.nanoTime() }
    @Synchronized override fun write(interleavedStereo:FloatArray,frames:Int) { require(frames<=availableFrames);written+=frames }
    override fun close() { stop();flush() }
}

internal fun awaitPlayer(player:VideoPlayer,timeoutMs:Long=8000,condition:(PlaybackStatus)->Boolean) {
    val deadline=System.nanoTime()+timeoutMs*1_000_000
    while(System.nanoTime()<deadline) { val status=player.status;assertNull(status.error,status.error?.stackTraceToString());if(condition(status))return;Thread.sleep(10) }
    fail("Player timed out: ${player.status}")
}

class VideoStillTransitionPlaybackTest {
    private val native=Path.of("native/windows-x64")
    private val factory=LibavDecoderFactory(native)
    private val fixtures=mutableListOf<Path>()
    private fun still(color:Int):String {
        val file=Files.createTempFile(Path.of("pinwheel-render/build"),"playback-still-",".jpg");fixtures.add(file)
        val image=BufferedImage(360,640,BufferedImage.TYPE_INT_RGB)
        image.createGraphics().let { graphics->graphics.color=java.awt.Color(color);graphics.fillRect(0,0,360,640);graphics.dispose() }
        check(ImageIO.write(image,"jpg",file.toFile()));return file.toAbsolutePath().toUri().toString()
    }
    @AfterTest fun removeGeneratedStills() { fixtures.forEach(Files::deleteIfExists) }
    private fun project(transition:Boolean,effects:Boolean,same:Boolean=false,file:String?=null):StudioProject {
        val a=file ?: still(0xff0000);val b=if(same)a else still(0x0000ff)
        return StudioProject(name="Stills",kind=ProjectKind.VIDEO,clips=listOf(
            Clip(uri=a,name="A.jpg",sourceDurationMs=1500,muted=true,video=VideoClipEdits(transition=if(transition)"fx-tr-pull-in" else "Cut",transitionDurationMs=600)),
            Clip(uri=b,name="B.jpg",sourceDurationMs=1500,muted=true)),video=VideoProjectEdits(effects=if(effects)listOf(
                VideoTimedEffect(kind="fx-ct-effect-mashup",startMs=73,endMs=1572),VideoTimedEffect(kind="fx-cc-bg-copies",startMs=1585,endMs=3000)) else emptyList()))
    }
    private fun splitPhoto(transition:Boolean=true,effects:Boolean=true,mashup:Boolean=true,copies:Boolean=true):StudioProject {
        val uri=still(0x00ff00)
        val fx=buildList { if(effects && mashup)add(VideoTimedEffect(kind="fx-ct-effect-mashup",startMs=73,endMs=1572))
            if(effects && copies)add(VideoTimedEffect(kind="fx-cc-bg-copies",startMs=1585,endMs=3000)) }
        return StudioProject(name="Split",kind=ProjectKind.VIDEO,clips=listOf(
            Clip(uri=uri,name="P.jpg",sourceDurationMs=3000,startMs=0,endMs=1629,muted=true,video=VideoClipEdits(transition=if(transition)"fx-tr-pull-in" else "Cut",transitionDurationMs=600)),
            Clip(uri=uri,name="P.jpg",sourceDurationMs=3000,startMs=1629,endMs=3000,muted=true)),video=VideoProjectEdits(effects=fx))
    }
    private fun playFrom(project:StudioProject,seekTo:Long?=null) {
        VideoPlayer(project,{GpuFrameEvaluator(native,it,270,480)},factory,{ClockedTestOutput()},frameRate=60).use { player->
            awaitPlayer(player,4000) { it.ready }
            val generation=player.seek((seekTo ?: 1000)*1000);awaitPlayer(player) { it.ready && it.generation==generation }
            assertEquals(generation,player.latestFrame!!.generation);player.play()
            awaitPlayer(player,6000) { it.timeUs>=2_800_000 };assertTrue(player.status.timeUs>=2_800_000)
        }
    }
    @Test fun samePhotoTwicePlaysThrough()=playFrom(project(false,false,same=true))
    @Test fun samePhotoTwiceWithTransitionAndEffectsPlaysThrough()=playFrom(project(true,true,same=true))
    // Owner project packages contain this same bundled photo; personal phone originals are not read.
    @Test fun userPhotoProjectPlaysThrough()=playFrom(project(true,true,same=true,file=Path.of("assets/demo.jpg").toAbsolutePath().toUri().toString()))
    @Test fun seekIntoTransitionThenPlay()=playFrom(project(true,true,same=true),seekTo=1430)
    @Test fun seekIntoTransitionThenPlayNoEffects()=playFrom(project(true,false,same=true),seekTo=1430)
    @Test fun seekBeforeCutThenPlayPlain()=playFrom(project(false,false,same=true),seekTo=1430)
    @Test fun splitPhotoPlaysThrough()=playFrom(splitPhoto(),seekTo=0)
    @Test fun splitPlain()=playFrom(splitPhoto(false,false),seekTo=0)
    @Test fun splitTransitionOnly()=playFrom(splitPhoto(true,false),seekTo=0)
    @Test fun splitEffectsOnly()=playFrom(splitPhoto(false,true),seekTo=0)
    @Test fun splitCopiesOnly()=playFrom(splitPhoto(false,true,mashup=false),seekTo=0)
    @Test fun splitMashupOnly()=playFrom(splitPhoto(false,true,copies=false),seekTo=0)
    @Test fun plainStillsPlayThrough()=playFrom(project(false,false))
    @Test fun stillsWithTransitionPlayThrough()=playFrom(project(true,false))
    @Test fun stillsWithEffectsPlayThrough()=playFrom(project(false,true))
    @Test fun stillsWithTransitionAndEffectsPlayThrough()=playFrom(project(true,true))
}

class VideoPlaybackClockTest {
    private val native=Path.of("native/windows-x64")
    private val factory=LibavDecoderFactory(native)
    private fun project()=StudioProject(name="Clock",kind=ProjectKind.VIDEO,clips=listOf(Clip(uri=Path.of("assets/demo.jpg").toAbsolutePath().toUri().toString(),name="Still",sourceDurationMs=2000,muted=true)))
    @Test fun slowVideoDropsFramesWhileConsumedAudioAdvances() {
        val p=project()
        VideoPlayer(p,{snapshot->object:VideoFrameEvaluator {
            override val project=snapshot
            override fun render(timeUs:Long):RgbaFrame { Thread.sleep(120);return RgbaFrame(2,2,ByteArray(16)) }
            override fun resetHistory() {}
            override fun close() {}
        }},factory,{ClockedTestOutput()}).use { player->
            awaitPlayer(player) { it.ready };player.play();awaitPlayer(player,4000) { it.timeUs>=1_500_000 }
            assertTrue(player.status.droppedFrames>10);assertTrue(player.status.renderedFrames<25)
        }
    }
    @Test fun pausedSeekAndRapidSupersedingSeeksOnlyPublishTheFinalGeneration() {
        VideoPlayer(project(),{GpuFrameEvaluator(native,it,160,120)},factory,{ClockedTestOutput()}).use { player->
            awaitPlayer(player) { it.ready };repeat(25) { player.seek((it%7)*110_000L) };val generation=player.seek(1_230_000)
            awaitPlayer(player) { it.ready && it.generation==generation };assertEquals(generation,player.latestFrame!!.generation)
            assertEquals(1_230_000,player.latestFrame!!.timeUs);val paused=player.status.timeUs;Thread.sleep(100);assertEquals(paused,player.status.timeUs)
        }
    }
    @Test fun thirtyRapidEditsCoalesceAndThenPlay() {
        val base=project()
        VideoPlayer(base,{GpuFrameEvaluator(native,it,160,120)},factory,{ClockedTestOutput()}).use { player->
            awaitPlayer(player) { it.ready }
            repeat(30) { player.updateProject(base.copy(clips=base.clips.map { clip->clip.copy(video=clip.video.copy(exposure=it/100f)) }));Thread.sleep(5) }
            awaitPlayer(player) { it.ready && it.generation>0 };player.play();awaitPlayer(player,4000) { it.timeUs>=1_500_000 }
        }
    }
}
