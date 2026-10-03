package com.pinwheel.media

import com.pinwheel.core.model.*
import java.nio.*
import java.nio.file.*
import kotlin.test.*

class TimelineAudioMixerTest {
    private val factory=LibavDecoderFactory(Path.of("native/windows-x64"))
    private fun tone():Path {
        val frames=48000;val file=Files.createTempFile(Path.of("pinwheel-media/build"),"mix-",".wav")
        val bytes=ByteBuffer.allocate(44+frames*4).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36+frames*4).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(2)
            .putInt(48000).putInt(192000).putShort(4).putShort(16).put("data".toByteArray()).putInt(frames*4)
        repeat(frames) { bytes.putShort(8192).putShort(8192) };Files.write(file,bytes.array());return file
    }
    private fun project(uri:String,tracks:List<VideoAudioTrack>,muted:Boolean=true)=StudioProject(name="Mixer",kind=ProjectKind.VIDEO,
        clips=listOf(Clip(uri=uri,name="Source",sourceDurationMs=1500,muted=muted)),video=VideoProjectEdits(audio=tracks))
    @Test fun delayedTrimmedTrackFadesAndPadsSilenceWithoutStalling() {
        val file=tone();try {
            val uri=file.toUri().toString();val track=VideoAudioTrack(uri=uri,name="Tone",sourceDurationMs=1000,sourceEndMs=500,startMs=200,volume=2f,fadeInMs=100,fadeOutMs=100)
            TimelineAudioMixer(project(uri,listOf(track)),factory).use { mix ->
                assertTrue(mix.render(0,960).all { it==0f })
                assertEquals(.5f,mix.render(400_000,960)[0],.0001f)
                assertEquals(.125f,mix.render(225_000,960)[0],.0001f)
                assertTrue(mix.render(900_000,960).all { it==0f })
                assertEquals(.5f,mix.render(400_000,960)[0],.0001f)
            }
        } finally { Files.deleteIfExists(file) }
    }
    @Test fun clipGainIsLinearAndMuteRemovesOnlyTheClipContribution() {
        val file=tone();try {
            val uri=file.toUri().toString();val p=project(uri,emptyList(),false).let { it.copy(clips=it.clips.map { clip->clip.copy(video=VideoClipEdits(volume=.4f)) }) }
            TimelineAudioMixer(p,factory).use { assertEquals(.1f,it.render(100_000,960)[0],.0001f) }
            TimelineAudioMixer(p.copy(clips=p.clips.map { it.copy(muted=true) }),factory).use { assertTrue(it.render(100_000,960).all { sample->sample==0f }) }
        } finally { Files.deleteIfExists(file) }
    }
    @Test fun mixedTracksClipPeaksAndSilenceContinuesAfterAllSoundsEnd() {
        val file=tone();try {
            val uri=file.toUri().toString();val tracks=(0 until 4).map { VideoAudioTrack(id="track-$it",uri=uri,name="Tone",sourceDurationMs=1000,volume=2f) }
            TimelineAudioMixer(project(uri,tracks),factory).use { mix ->
                assertTrue(mix.render(100_000,960).all { it==1f });assertTrue(mix.render(1_300_000,960).all { it==0f })
            }
        } finally { Files.deleteIfExists(file) }
    }
}
