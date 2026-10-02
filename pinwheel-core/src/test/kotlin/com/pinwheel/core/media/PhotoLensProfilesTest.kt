package com.pinwheel.core.media

import org.junit.Assert.*
import org.junit.Test

class PhotoLensProfilesTest {
    @Test fun exactExifNamesAndRationalFocalLengthsMatchSupportedCalibrations() {
        val canon=PhotoLensProfiles.match("Canon Canon EOS 6D","EF50mm f/1.8 STM","50/1",6000,4000,"50")
        assertEquals("canon-ef50-f18stm-ff",canon?.profile?.id);assertEquals(1.005f,canon!!.cameraCrop,0f)
        val nikon=PhotoLensProfiles.match("Nikon Corporation NIKON D750","AF-S NIKKOR 35mm f/1.8G ED","35.0",4000,6000,"35")
        assertEquals("nikon-afs35-f18ged-ff",nikon?.profile?.id)
    }
    @Test fun missingAmbiguousCroppedAndUnsupportedMetadataCannotEnableAProfile() {
        fun match(camera:String?="Canon EOS 5D Mark II",lens:String?="EF50mm f/1.8 II",focal:String?="50",w:Int=6000,h:Int=4000,equivalent:String?="50")=PhotoLensProfiles.match(camera,lens,focal,w,h,equivalent)
        assertNotNull(match())
        assertNull(match(camera=null));assertNull(match(lens=null));assertNull(match(focal=null));assertNull(match(equivalent=null))
        assertNull(match(lens="EF50mm f/1.8"));assertNull(match(focal="35"));assertNull(match(focal="NaN"));assertNull(match(focal="50/0"))
        assertNull(match(camera="Canon EOS 7D"));assertNull(match(equivalent="75"));assertNull(match(h=4500));assertNull(match(w=0))
        assertNull(match(camera="Nikon D750",lens="EF50mm f/1.8 II"))
    }
    @Test fun expandedProfilesKeepExactMatchingAcrossBrandsAndRejectSimilarVersions() {
        assertEquals(35,PhotoLensProfiles.profiles.size)
        assertEquals(35,PhotoLensProfiles.profiles.map{it.id}.toSet().size)
        for(profile in PhotoLensProfiles.profiles) {
            val camera=when(profile.maker){"Canon"->"Canon EOS 5DS R";"Nikon"->"Nikon D6";else->"SONY ILCE-7RM5"}
            for(alias in profile.aliases) {
                assertEquals(profile.id,PhotoLensProfiles.match(camera,alias,profile.focal.toString(),6000,4000,profile.focal.toString())?.profile?.id)
                assertNull(PhotoLensProfiles.match(camera,alias,profile.focal.toString(),6000,4000,(profile.focal*1.5f).toString()))
            }
        }
        assertEquals("sony-fe35-f18-ff",PhotoLensProfiles.match("Sony SONY ILCE-7M3","FE 35mm F1.8","35",6000,4000,"35")?.profile?.id)
        assertNull(PhotoLensProfiles.match("Sony ILCE-6700","FE 35mm F1.8","35",6000,4000,"35"))
        assertNull(PhotoLensProfiles.match("Canon EOS 5DS","EF 35mm f/1.4L II USM","35",6000,4000,"35"))
        assertNull(PhotoLensProfiles.match("Sony ILCE-7M3","FE 85mm f/1.4 GM II","85",6000,4000,"85"))
        assertNull(PhotoLensProfiles.match("Nikon D6","AF-S DX NIKKOR 35mm f/1.8G","35",6000,4000,"35"))
    }
    @Test fun calibratedPolynomialMatchesPublishedPtlensDefinition() {
        val p=PhotoLensProfiles.byId("canon-ef50-f14-ff")!!
        assertEquals(.00225f,p.a,0f);assertEquals(-.01104f,p.b,0f);assertEquals(.00161f,p.c,0f)
        assertEquals(1f,p.factor(1f),.000001f)
        assertEquals(1f-p.a-p.b-p.c,p.factor(0f),.000001f)
        val r=1.7f
        assertEquals(p.a*r*r*r*r+p.b*r*r*r+p.c*r*r+(1f-p.a-p.b-p.c)*r,r*p.factor(r),.000001f)
        for(profile in PhotoLensProfiles.profiles)for(i in 0..200)assertTrue(profile.derivative(i/100f)>.5f)
    }
    @Test fun profileAndManualGeometryRoundTripAndKeepEdgesFilled() {
        for(profile in PhotoLensProfiles.profiles)for((w,h) in listOf(6000 to 4000,4000 to 6000))for(manual in listOf(-1f,0f,1f))for(crop in listOf(1f,1.005f)) {
            val mapping=PhotoLens(w,h,manual,profile.id,true,crop)
            assertTrue(mapping.active)
            for(x in 0..20)for(y in 0..20) {
                val source=mapping.inverse(x/20f,y/20f)
                assertTrue("${profile.id} filled edges",source.x in 0f..1f && source.y in 0f..1f)
                val output=mapping.forward(source.x,source.y)
                assertEquals(x/20f,output.x,.00002f);assertEquals(y/20f,output.y,.00002f)
            }
        }
    }
    @Test fun disabledUnknownAndWrongAspectProfilesAreNeutral() {
        val id=PhotoLensProfiles.profiles.first().id
        for(mapping in listOf(PhotoLens(6000,4000,0f,id,false),PhotoLens(6000,4000,0f,"unknown",true),PhotoLens(4000,3000,0f,id,true))) {
            assertFalse(mapping.active);val p=mapping.inverse(.1f,.9f);assertEquals(.1f,p.x,.000001f);assertEquals(.9f,p.y,.000001f)
        }
    }
}
