package com.pinwheel.media

import com.pinwheel.core.model.*
import com.pinwheel.core.media.video.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration

/** Composition-clock stereo PCM. Inactive and exhausted sounds contribute silence to movie end. */
class TimelineAudioMixer(private val project: StudioProject, private val factory: MediaDecoderFactory) : AutoCloseable {
    private data class Source(val id:String,val uri:String,val start:Long,val end:Long,val sourceStart:Long,val sourceEnd:Long,
        val speed:Float,val volume:Float,val voice:String="None",val noise:Boolean=false,val fadeIn:Long=0,val fadeOut:Long=0,val clipGain:Boolean=false)
    private val sources=buildList {
        var time=0L
        for(clip in project.clips) {
            val end=time+clip.durationMs*1000
            if(!clip.muted) add(Source(clip.id,clip.uri,time,end,clip.startMs*1000,clip.endMs*1000,clip.playbackSpeed,clip.video.volume,clipGain=true))
            time=end
        }
        for(raw in project.video.audio.take(MAX_VIDEO_AUDIO_TRACKS)) {
            val a=raw.sanitized();val duration=minOf(a.durationMs,project.durationMs-a.startMs)
            if(duration>0) add(Source(a.id,a.uri,a.startMs*1000,(a.startMs+duration)*1000,a.sourceStartMs*1000,a.sourceEndMs*1000,
                a.speed,a.volume,a.voiceEffect,a.reduceNoise,a.fadeInMs,a.fadeOutMs))
        }
    }
    private class Cursor(val source:Source,val decoder:MediaDecoder) {
        var sonic=Sonic(48000,2,source.speed,pitch(source.voice),48000,true)
        val dsp=VideoAudioDspProcessor(source.voice,source.noise,source.volume).apply { configure(PcmFormat(48000,2,PcmEncoding.FLOAT32));flush() }
        val envelope=VideoAudioEnvelope((source.end-source.start)/1000,source.fadeIn,source.fadeOut)
        var position=-1L
        var ended=false
        fun reset(local:Long) {
            decoder.seek(source.sourceStart+(local*1_000_000.0/48000*source.speed).toLong())
            sonic=Sonic(48000,2,source.speed,pitch(source.voice),48000,true);dsp.flush();position=local;ended=false
        }
        fun samples(local:Long,frames:Int):FloatArray {
            val requested=local
            if(position!=requested) reset(local)
            val needed=frames*8;val deadline=System.nanoTime()+Duration.ofSeconds(5).toNanos()
            while(sonic.outputSize<needed && !ended) {
                val block=decoder.pollAudio(Duration.ofNanos((deadline-System.nanoTime()).coerceAtLeast(0)))
                if(block==null) {
                    check(decoder.audioEnded || System.nanoTime()<deadline) { "Audio decode timed out: ${source.uri}" }
                    if(decoder.audioEnded) { sonic.queueEndOfStream();ended=true };continue
                }
                val allowed=((source.sourceEnd-block.presentationTimeUs).coerceAtLeast(0)*48000/1_000_000).coerceAtMost(block.interleavedPcm.size.toLong()/2).toInt()
                val bytes=ByteBuffer.allocate(allowed*8).order(ByteOrder.nativeOrder())
                repeat(allowed*2) { bytes.putFloat(block.interleavedPcm[it]) };bytes.flip();sonic.queueInput(bytes)
                if(allowed<block.interleavedPcm.size/2) { sonic.queueEndOfStream();ended=true }
            }
            val output=ByteBuffer.allocate(needed).order(ByteOrder.nativeOrder());sonic.getOutput(output);val count=output.position()/4;output.flip()
            val pcm=FloatArray(frames*2);repeat(count) { pcm[it]=output.float }
            val bytes=ByteBuffer.allocate(pcm.size*4).order(ByteOrder.LITTLE_ENDIAN);pcm.forEach { bytes.putFloat(it) };bytes.flip()
            val processed=if(!source.clipGain && (source.voice!="None" || source.noise || source.volume!=1f)) { dsp.queueInput(bytes);dsp.getOutput().order(ByteOrder.LITTLE_ENDIAN) } else bytes
            repeat(frames) { i -> val gain=envelope.getGainFactorAtSamplePosition(position+i,48000)*(if(source.clipGain)source.volume else 1f);pcm[i*2]=processed.float*gain;pcm[i*2+1]=processed.float*gain }
            position+=frames;return pcm
        }
        companion object { fun pitch(voice:String)=when(voice) { "Deep"->.72f;"Chipmunk"->1.55f;"Alien"->1.23f;else->1f } }
    }
    private val cursors=LinkedHashMap<String,Cursor>(24,.75f,true)
    private val silent=HashSet<String>()
    fun render(timeUs:Long,frames:Int):FloatArray {
        require(timeUs>=0);return renderSamples(timeUs*48000/1_000_000,frames)
    }
    fun renderSamples(startSample:Long,frames:Int):FloatArray {
        require(startSample>=0 && frames in 1..4800)
        val out=FloatArray(frames*2)
        for(source in sources) {
            val first=maxOf(startSample,source.start*48000/1_000_000)
            val end=minOf(startSample+frames,source.end*48000/1_000_000)
            if(first>=end || source.id in silent) continue
            val cursor=cursors[source.id] ?: run {
                while(cursors.size>=18) cursors.remove(cursors.keys.first())!!.decoder.close()
                try { Cursor(source,factory.open(localMediaPath(source.uri),DecodeConfig(decodeVideo=false,audioQueueBlocks=4,maxQueuedBytes=1024*1024))).also { cursors[source.id]=it } }
                catch(missing:MissingMediaStream) { silent+=source.id;null }
            } ?: continue
            val local=first-source.start*48000/1_000_000
            val pcm=cursor.samples(local,(end-first).toInt());val offset=(first-startSample).toInt()*2
            for(i in pcm.indices)out[offset+i]+=pcm[i]
        }
        for(i in out.indices)out[i]=out[i].coerceIn(-1f,1f)
        return out
    }
    fun seek() { cursors.values.forEach { it.position=-1 } }
    override fun close() { cursors.values.forEach { it.decoder.close() };cursors.clear() }
}
