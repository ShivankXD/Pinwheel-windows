package com.pinwheel.app.debug

import com.pinwheel.app.ApplicationServices
import com.pinwheel.app.DebugPanel
import com.pinwheel.core.plans.*
import com.pinwheel.core.model.*

fun main() {
    val panel = java.util.ServiceLoader.load(DebugPanel::class.java).single() as DeveloperPanel
    val services = ApplicationServices(entitlement = panel.entitlement)
    services.auth.signIn()
    val p = StudioProject(name = "Debug check", kind = ProjectKind.VIDEO, clips = listOf(Clip(uri = "file:///test.mp4", name = "test", sourceDurationMs = 1000)))
    val settings = ExportSettings(2160, 60, 100)
    check(services.exportPlan(p, settings).settings == ExportSettings(1080, 30, 8, true))
    panel.entitlement.toggle()
    check(services.entitlement.tier == PlanTier.PLUS)
    check(services.exportPlan(p, settings).settings == settings)
    panel.entitlement.toggle()
    check(services.entitlement.tier == PlanTier.FREE)
    services.auth.signOut()
    check(runCatching { services.exportPlan(p, settings) }.isFailure)
    println("PASS debug Plus toggle drives the shared core/export entitlement and sign-in gate")
}
