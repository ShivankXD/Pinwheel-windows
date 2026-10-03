package com.pinwheel.media

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import kotlin.test.*

class SonicTest {
    @Test fun tempoPreservesPitchAcrossTheMobileSpeedRange() {
        for(speed in listOf(.25f,.5f,1f,1.5f,2f,4f)) checkTone(speed,1f,440f)
    }
    @Test fun voicePitchKeepsDurationAndUsesMobilePresetFactors() {
        for(pitch in listOf(.72f,1.55f,1.23f)) checkTone(1f,pitch,440f*pitch)
    }
    private fun checkTone(speed:Float,pitch:Float,frequency:Float) {
        val sonic=Sonic(48000,2,speed,pitch,48000,true);val out=ArrayList<Float>()
        fun drain() { val buffer=ByteBuffer.allocate(sonic.outputSize).order(ByteOrder.nativeOrder());sonic.getOutput(buffer);buffer.flip();while(buffer.hasRemaining())out+=buffer.float }
        var offset=0
        while(offset<48000) {
            val frames=minOf(960,48000-offset);val input=ByteBuffer.allocate(frames*8).order(ByteOrder.nativeOrder())
            repeat(frames) { i->val value=sin((i+offset)*2*PI*440/48000).toFloat()*.4f;input.putFloat(value).putFloat(value) }
            input.flip();sonic.queueInput(input);drain();offset+=frames
        }
        sonic.queueEndOfStream();drain()
        val frames=out.size/2;val expected=48000/speed
        assertTrue(abs(frames-expected)<480,"$speed/$pitch duration $frames vs $expected")
        val first=frames/4;val last=frames*3/4
        val crossings=(first+1 until last).count { out[(it-1)*2]<=0 && out[it*2]>0 }
        val actual=crossings*48000f/(last-first)
        assertTrue(abs(actual-frequency)<8,"$speed/$pitch frequency $actual vs $frequency")
        assertTrue(out.all { it.isFinite() && abs(it)<.5f })
    }
}
