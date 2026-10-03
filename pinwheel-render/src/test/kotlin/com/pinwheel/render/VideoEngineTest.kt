package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.*
import com.pinwheel.media.RenderMode
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import kotlin.test.*

class VideoPipTest {
    private val native=Path.of("native/windows-x64")
    private fun project(mask: String="Circle", opacity: Float=1f): StudioProject = StudioProject(name="PIP regression",kind=ProjectKind.VIDEO,
        clips=listOf(Clip(uri=Path.of("assets/p3/pip-base.mp4").toAbsolutePath().toUri().toString(),name="Blue",sourceDurationMs=4000,muted=true)),
        video=VideoProjectEdits(images=listOf(VideoImageOverlay(uri=Path.of("assets/p3/pip-layer.mp4").toAbsolutePath().toUri().toString(),name="Moving overlay",
            startMs=1000,endMs=3000,x=.7f,y=.4f,width=.5f,mask=mask,opacity=opacity,video=VideoOverlaySource(3000,startMs=500,width=160,height=120)))))
    private fun channel(frame: RgbaFrame,x: Int,y: Int,c: Int) {
        val rgb=(0..2).map { frame.pixels[(y*frame.width+x)*4+it].toInt() and 255 }
        assertTrue(rgb[c]>190 && rgb.filterIndexed { i,_->i!=c }.all { it<65 },"$x,$y: $rgb")
    }
    @Test fun delayedTrimmedMovingVideoHasTransparentMask() {
        GpuFrameEvaluator(native,project(),320,240).use { evaluator ->
            for((time,c) in listOf(400L to 2,1400L to 0,2600L to 1,3500L to 2,1400L to 0)) {
                val frame=evaluator.render(time*1000);channel(frame,224,96,c);channel(frame,150,42,2);channel(frame,30,190,2)
            }
        }
    }
    @Test fun overlappingVideoLayersKeepTheirStackingOrder() {
        val p=project("None");val bottom=p.video.images.single().copy(x=.5f,y=.5f,width=.6f,endMs=2500)
        val top=bottom.copy(id="top",width=.2f,startMs=1200,endMs=2500,video=bottom.video!!.copy(startMs=1500))
        GpuFrameEvaluator(native,p.copy(video=p.video.copy(images=listOf(bottom,top))),320,240).use { evaluator ->
            val frame=evaluator.render(1_500_000);channel(frame,160,120,1);channel(frame,95,120,0);channel(frame,20,200,2)
        }
    }
    @Test fun opacityAndEntranceExitAnimationMatchMobileSamples() {
        val p=project("None",.5f);val layer=p.video.images.single().copy(animIn="Fade",animOut="Fade")
        GpuFrameEvaluator(native,p.copy(video=p.video.copy(images=listOf(layer))),320,240).use { evaluator ->
            fun rgb(ms:Long)=evaluator.render(ms*1000).pixels.let { px -> (0..2).map { px[(96*320+224)*4+it].toInt() and 255 } }
            val middle=rgb(1500);assertTrue(middle[0] in 95..170 && middle[2] in 95..170,"$middle")
            assertTrue(rgb(1033)[0]<middle[0]/2);assertTrue(rgb(2966)[1]<65)
        }
    }
    @Test fun endedInputsDropOutWithoutChangingRemainingLayerIdentity() {
        val p=project("None");val first=p.video.images.single().copy(id="early",endMs=1800)
        val second=first.copy(id="late",startMs=1400,endMs=3000,video=first.video!!.copy(startMs=1500))
        GpuFrameEvaluator(native,p.copy(video=p.video.copy(images=listOf(first,second))),320,240).use { evaluator ->
            channel(evaluator.render(2_000_000),224,96,1);channel(evaluator.render(3_500_000),224,96,2)
        }
    }
}

class VideoFrameEvaluatorTest {
    private val native=Path.of("native/windows-x64")
    @Test fun geometryMirrorsBeforeClockwiseRotationAndRetainsTopDownOrientation() {
        val file=Files.createTempFile(Path.of("pinwheel-render/build"),"corners-",".png")
        try {
            val image=BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB)
            for(y in 0..3)for(x in 0..3)image.setRGB(x,y,when {y<2&&x<2->0xffff0000.toInt();y<2->0xff00ff00.toInt();x<2->0xff0000ff.toInt();else->0xffffff00.toInt()})
            ImageIO.write(image,"png",file.toFile())
            val p=StudioProject(name="Geometry",kind=ProjectKind.VIDEO,clips=listOf(Clip(uri=file.toUri().toString(),name="corners",sourceDurationMs=1000,muted=true,video=VideoClipEdits(rotation=90,mirrored=true))))
            GpuFrameEvaluator(native,p,4,4).use { e ->
                val frame=e.render(0)
                fun rgb(x:Int,y:Int)=(0..2).map { frame.pixels[(y*4+x)*4+it].toInt() and 255 }
                assertEquals(listOf(255,255,0),rgb(0,0));assertEquals(listOf(0,255,0),rgb(3,0))
                assertEquals(listOf(0,0,255),rgb(0,3));assertEquals(listOf(255,0,0),rgb(3,3))
            }
        } finally { Files.deleteIfExists(file) }
    }
    @Test fun previewAndExportUseIdenticalStagesOnMatchingInputs() {
        val p=StudioProject(name="Shared recipe",kind=ProjectKind.VIDEO,clips=listOf(Clip(uri=Path.of("assets/p3/pip-base.mp4").toAbsolutePath().toUri().toString(),name="Blue",sourceDurationMs=4000,
            video=VideoClipEdits(rotation=180,lookId="flt-candy",brightness=.1f,motion="Zoom in"))),video=VideoProjectEdits(effects=listOf(VideoTimedEffect(kind="fx-vignette",startMs=0,endMs=3000))))
        val preview=GpuFrameEvaluator(native,p,320,240,RenderMode.PREVIEW).use { it.render(1_000_000) }
        val export=GpuFrameEvaluator(native,p,320,240,RenderMode.EXPORT).use { it.render(1_000_000) }
        assertContentEquals(preview.pixels,export.pixels)
    }
    @Test fun audioTailFillerUsesExactCanvasSize() {
        val uri=Path.of("assets/p3/pip-base.mp4").toAbsolutePath().toUri().toString()
        val p=StudioProject(name="Tail",kind=ProjectKind.VIDEO,clips=listOf(Clip(uri=uri,name="Short",sourceDurationMs=4000,endMs=1000)),
            video=VideoProjectEdits(audio=listOf(VideoAudioTrack(uri=uri,name="Extracted",sourceDurationMs=4000,extendsVideo=true))))
        GpuFrameEvaluator(native,p,320,240).use { e -> val frame=e.render(2_000_000);assertEquals(320,frame.width);assertEquals(240,frame.height)
            assertTrue(frame.pixels.indices.all { i->(frame.pixels[i].toInt() and 255)==if(i%4==3)255 else 0 }) }
    }
    @Test fun previewShortSideIs720AndUnsetSideIsEven() { assertEquals(720 to 1280,GpuFrameEvaluator.previewSize(9f/16));assertEquals(1280 to 720,GpuFrameEvaluator.previewSize(16f/9)) }
}
