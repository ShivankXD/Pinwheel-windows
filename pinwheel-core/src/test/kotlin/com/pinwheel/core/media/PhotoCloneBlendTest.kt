package com.pinwheel.core.media

import org.junit.Assert.*
import org.junit.Test

class PhotoCloneBlendTest {
    @Test fun transparentColoursDoNotBleedIntoFeatheredEdges() {
        assertEquals(0x80ff0000.toInt(),PhotoClone.blend(0xffff0000.toInt(),0x000000ff,.5f))
        assertEquals(0x800000ff.toInt(),PhotoClone.blend(0x00ff0000,0xff0000ff.toInt(),.5f))
        assertEquals(0,PhotoClone.blend(0x00ff0000,0x000000ff,.5f))
    }
    @Test fun endpointsCopyPixelsExactlyAndOpaqueMixStaysOpaque() {
        assertEquals(0xff123456.toInt(),PhotoClone.blend(0xff123456.toInt(),0xffabcdef.toInt(),0f))
        assertEquals(0xffabcdef.toInt(),PhotoClone.blend(0xff123456.toInt(),0xffabcdef.toInt(),1f))
        assertEquals(0xff800080.toInt(),PhotoClone.blend(0xffff0000.toInt(),0xff0000ff.toInt(),.5f))
    }
}
