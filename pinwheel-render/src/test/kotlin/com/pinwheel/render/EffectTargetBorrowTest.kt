package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.*
import com.pinwheel.core.media.video.VideoFxCatalog
import java.nio.file.Path
import kotlin.test.*

class EffectTargetBorrowTest {
    @Test fun skippingInactiveAttachmentsPreservesEveryOutputByteIncludingHistoryAndLegacyStages() {
        val width=64;val height=48
        val frame=RgbaFrame(width,height,ByteArray(width*height*4) { i->if(i%4==3)255.toByte() else ((i*37+i/29)%256).toByte() })
        val history=VideoFxCatalog.effects.first { it.history }.id
        val configurations=listOf(emptyList(),listOf(VideoTimedEffect(kind="fx-vignette",startMs=300,endMs=700,intensity=0f)),
            listOf(VideoTimedEffect(kind=history,startMs=300,endMs=700)),listOf(VideoTimedEffect(kind="Grain",startMs=300,endMs=700,intensity=.7f)),
            listOf(VideoTimedEffect(kind="Soft Glow",startMs=300,endMs=700,intensity=.4f)))
        for(fx in configurations) {
            val project=StudioProject(name="Borrow",kind=ProjectKind.VIDEO,clips=listOf(Clip(uri="file:///still.jpg",name="Still",sourceDurationMs=1500)),video=VideoProjectEdits(effects=fx))
            AngleDevice(Path.of("native/windows-x64")).use { device ->
                GpuTarget(device,width,height).use { input ->
                    input.texture.upload(frame)
                    EffectRuntime(Path.of("native/windows-x64"),width,height,project,device).use { original ->
                        EffectRuntime(Path.of("native/windows-x64"),width,height,project,device).use { borrowed ->
                            for(time in listOf(0L,100_000,300_000,500_000,600_000,900_000,1_200_000))
                                assertContentEquals(original.render(frame,time).pixels,borrowed.renderTarget(input,time).read().pixels,"${fx.map { it.kind }} at $time")
                        }
                    }
                }
            }
        }
    }
}
