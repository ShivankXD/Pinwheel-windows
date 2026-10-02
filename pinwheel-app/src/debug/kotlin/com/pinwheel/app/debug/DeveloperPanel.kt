package com.pinwheel.app.debug

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinwheel.app.DebugPanel
import com.pinwheel.app.ApplicationServices
import com.pinwheel.core.plans.*

/** Never compiled into the default/release source set. No billing verification claim. */
class DebugEntitlement : Entitlement {
    override var tier by mutableStateOf(PlanTier.FREE)
        private set
    fun toggle() { tier = if (tier == PlanTier.FREE) PlanTier.PLUS else PlanTier.FREE }
}
class DeveloperPanel : DebugPanel {
    override val entitlement = DebugEntitlement()
    @Composable override fun Content(services: ApplicationServices) {
        var lab by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Developer plan: ${services.entitlement.tier}")
            Button(onClick = entitlement::toggle) { Text("Toggle Plus (debug)") }
            OutlinedButton(onClick = { lab = true }) { Text("Effect Lab") }
        }
        if (lab) EffectLabWindow(onClose = { lab = false })
    }
}
