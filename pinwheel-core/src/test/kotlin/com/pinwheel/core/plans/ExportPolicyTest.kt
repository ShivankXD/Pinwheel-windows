package com.pinwheel.core.plans

import com.pinwheel.core.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class ExportPolicyTest {
    private val movie = StudioProject(name = "test", kind = ProjectKind.VIDEO, clips = listOf(Clip(uri = "file:///a.mp4", name = "a", sourceDurationMs = 4000)))
    private val auth = object : AuthProvider { override val account = AuthAccount("test", "test"); override fun signIn() = account; override fun signOut() {} }
    @Test fun freeLimitsAreEnforcedWithoutAUi() {
        for (height in listOf(-1, 0, 2160)) {
            val plan = ExportPolicy.authorize(movie, ExportSettings(height, 60, 100, false), auth, FreeEntitlement)
            assertEquals(ExportSettings(1080, 30, 8, true), plan.settings); assertEquals(6500, plan.durationMs)
        }
    }
    @Test fun plusAllowsFourKAndOptionalEnding() {
        val plus = object : Entitlement { override val tier = PlanTier.PLUS }
        val plan = ExportPolicy.authorize(movie, ExportSettings(2160, 60, 100), auth, plus)
        assertEquals(ExportSettings(2160, 60, 100), plan.settings); assertEquals(4000, plan.durationMs)
    }
    @Test fun signedOutAndLoadedPlusEffectsCannotBypassExportGate() {
        val signedOut = object : AuthProvider { override val account: AuthAccount? = null; override fun signIn(): AuthAccount = error("fake"); override fun signOut() {} }
        assertFailsWith<IllegalStateException> { ExportPolicy.authorize(movie, ExportSettings(), signedOut, FreeEntitlement) }
        assertFailsWith<IllegalStateException> { ExportPolicy.authorize(movie.copy(video = VideoProjectEdits(effects = listOf(VideoTimedEffect()))), ExportSettings(), auth, FreeEntitlement) }
        assertFailsWith<IllegalArgumentException> { ExportPolicy.authorize(movie, ExportSettings(mbps = -2), auth, FreeEntitlement) }
    }
}
