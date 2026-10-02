package com.pinwheel.app

import com.pinwheel.core.plans.*
import com.pinwheel.core.model.StudioProject
import com.pinwheel.platform.FakeAuthProvider

/** The same provider is injected into editing and export clients in subsequent phases. */
class ApplicationServices(val auth: AuthProvider = FakeAuthProvider(), val entitlement: Entitlement = FreeEntitlement) {
    fun exportPlan(project: StudioProject, settings: ExportSettings) = ExportPolicy.authorize(project, settings, auth, entitlement)
}
