package com.pinwheel.render.qa

import com.pinwheel.core.media.*
import com.pinwheel.core.media.audio.AudioAssetCatalog
import com.pinwheel.core.media.video.*
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Files
import kotlin.test.*

class CatalogContractTest {
    @Test fun pinnedCatalogCountsIdsDefaultsAndLegacyLookupsStayStable() {
        assertEquals(277, VideoFxCatalog.effects.size); assertEquals(46, VideoFxCatalog.transitions.size)
        assertEquals(80, VideoFxCatalog.overlays.size); assertEquals(19, VideoFxCatalog.legacyOverlays.size)
        val specs = VideoFxCatalog.effects + VideoFxCatalog.transitions + VideoFxCatalog.overlays + VideoFxCatalog.legacyOverlays
        assertEquals(specs.size, specs.map { it.id }.distinct().size)
        for (spec in specs) {
            assertEquals(spec, VideoFxCatalog.find(spec.id)); assertTrue(spec.params.size <= 8)
            assertEquals(spec.params.size, spec.params.map { it.key }.distinct().size)
            assertTrue(spec.params.all { it.default.isFinite() && it.default in 0f..1f })
            assertTrue(FxShaderSources.source(spec).contains(spec.shader))
            for (time in VideoPreviewRecipe.goldenTimes) {
                val input = VideoPreviewRecipe.inputs(spec, time)
                assertTrue(Files.isRegularFile(Path.of("assets", input.sample)), input.sample)
                assertEquals(8, input.uniforms.size); assertTrue(input.progress in 0f..1f)
            }
        }
        assertEquals("flt-pop", VideoFilterCatalog.find("vivid")?.id)
        assertEquals("flt-noir-film", VideoFilterCatalog.find("noir")?.id)
    }
    @Test fun everyLibraryCategoryResolvesItsLookAndStickerCards() {
        for (category in VideoFxCatalog.overlayCategories) {
            val ids = VideoFxCatalog.overlayItems(category)
            assertTrue(ids.isNotEmpty(), category); assertEquals(ids.size, ids.distinct().size)
            for (id in ids) assertTrue(VideoFxCatalog.find(id) != null || VideoAnimatedStickers.find(id) != null, id)
        }
        assertEquals(VideoAnimatedStickers.entries.size, VideoAnimatedStickers.entries.map { it.id }.distinct().size)
        for (category in VideoAnimatedStickers.categories) assertTrue(VideoAnimatedStickers.inCategory(category).isNotEmpty(), category)
    }
    @Test fun captionFontsImportedPackagesAndHomeTemplatesHaveAssetsAndNotices() {
        assertEquals(17, CaptionFonts.files.size)
        for (file in CaptionFonts.files.values) assertTrue(Files.isRegularFile(Path.of("assets/caption_fonts", file)), file)
        val templates = ImportedCaptionCatalog.load(Path.of("assets"))
        assertTrue(templates.isNotEmpty()); assertEquals(templates.size, templates.map { it.id }.distinct().size)
        for (template in PinwheelTemplates.all) assertTrue(Files.isRegularFile(Path.of("assets", template.asset)), template.asset)
        assertTrue(Files.isRegularFile(Path.of("licenses/mobile/AUDIO-CATALOG-LICENSES.md")))
        assertEquals(15, Files.list(Path.of("licenses/mobile/caption_fonts")).use { it.count().toInt() })
    }
    @Test fun previewRecipeKeepsThirdHeightCropAndTransitionSwitch() {
        assertEquals(VideoPreviewRecipe.Crop(600, 0, 800, 1000), VideoPreviewRecipe.crop(2000, 1000))
        assertEquals(VideoPreviewRecipe.Crop(0, 166, 800, 1000), VideoPreviewRecipe.crop(800, 1500))
        val transition = VideoFxCatalog.transitions.first()
        assertNotEquals(VideoPreviewRecipe.inputs(transition, .3).sample, VideoPreviewRecipe.inputs(transition, 2.1).sample)
        val history = VideoFxCatalog.effects.first { it.history }
        assertEquals(.9f, VideoPreviewRecipe.inputs(history, .3).current.zoom)
        assertEquals(1f, VideoPreviewRecipe.inputs(history, .3).current.body)
    }
}
