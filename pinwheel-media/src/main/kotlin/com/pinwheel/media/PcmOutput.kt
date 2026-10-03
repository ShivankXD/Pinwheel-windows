package com.pinwheel.media

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** Consumed PCM frames, rather than submitted bytes, are the playback master clock. */
interface PcmOutput : AutoCloseable {
    val consumedFrames: Long
    val availableFrames: Int
    fun start()
    fun stop()
    fun flush()
    /** Caller limits frames to availableFrames; output never blocks the video renderer. */
    fun write(interleavedStereo: FloatArray, frames: Int)
}

class JavaSoundOutput : PcmOutput {
    private val line: SourceDataLine=AudioSystem.getSourceDataLine(AudioFormat(48000f,16,2,true,false)).apply {
        open(AudioFormat(48000f,16,2,true,false),4800*4)
    }
    override val consumedFrames get()=line.longFramePosition
    override val availableFrames get()=line.available()/4
    override fun start()=line.start()
    override fun stop()=line.stop()
    override fun flush()=line.flush()
    override fun write(interleavedStereo:FloatArray,frames:Int) {
        require(frames in 0..interleavedStereo.size/2 && frames<=availableFrames)
        val bytes=ByteArray(frames*4)
        repeat(frames*2) { i-> val sample=(interleavedStereo[i].coerceIn(-1f,1f)*32767).toInt();bytes[i*2]=sample.toByte();bytes[i*2+1]=(sample shr 8).toByte() }
        check(line.write(bytes,0,bytes.size)==bytes.size) { "Audio device did not accept a complete block" }
    }
    override fun close() { line.stop();line.flush();line.close() }
}
