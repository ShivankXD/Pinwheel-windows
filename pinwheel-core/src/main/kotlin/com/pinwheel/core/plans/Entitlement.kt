package com.pinwheel.core.plans

enum class PlanTier { FREE, PLUS }

/** Later supplied by the backend using the same Google subject on phone and PC. */
interface Entitlement {
    val tier: PlanTier
    val googleSubject: String? get() = null
}
object FreeEntitlement : Entitlement { override val tier = PlanTier.FREE }

data class AuthAccount(val googleSubject: String, val displayName: String)
interface AuthProvider {
    val account: AuthAccount?
    fun signIn(): AuthAccount
    fun signOut()
}

data class ExportSettings(val height: Int = 1080, val fps: Int = 30,
    val mbps: Int = 8, val ending: Boolean = false)
data class ExportPlan(val settings: ExportSettings, val durationMs: Long)

/** Every encoder must request this plan, even when no settings UI is involved. */
object ExportPolicy {
    fun authorize(project: com.pinwheel.core.model.StudioProject, request: ExportSettings,
        auth: AuthProvider, entitlement: Entitlement): ExportPlan {
        check(auth.account != null) { "Sign in before exporting" }
        val plus = entitlement.tier == PlanTier.PLUS
        if (plus && entitlement.googleSubject != null) check(entitlement.googleSubject == auth.account?.googleSubject) { "Plus belongs to a different account" }
        // Existing Plus projects remain readable, but cannot bypass the Free effects gate.
        check(plus || project.video.effects.none { it.enabled }) { "Effects require Plus" }
        val height = if (plus) request.height else if (request.height <= 0 || request.height > 1080) 1080 else request.height
        val fps = if (plus) request.fps else minOf(request.fps, 30)
        val mbps = if (plus) request.mbps else minOf(request.mbps, 8)
        require(height in listOf(480, 720, 1080, 1440, 2160)) { "Unsupported export height" }
        require(fps in listOf(24, 25, 30, 50, 60)) { "Unsupported export frame rate" }
        require(mbps in 1..100) { "Bitrate must be between 1 and 100 Mbps" }
        val settings = ExportSettings(height, fps, mbps, !plus || request.ending)
        val tail = if (settings.ending) 2500L else 0L
        return ExportPlan(settings, Math.addExact(project.durationMs, tail))
    }
}
