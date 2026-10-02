package com.pinwheel.app

import androidx.compose.runtime.Composable
import com.pinwheel.core.plans.Entitlement

/** Implementations and service metadata are compiled only by -Ppinwheel.debug=true. */
interface DebugPanel {
    val entitlement: Entitlement
    @Composable fun Content(services: ApplicationServices)
}
