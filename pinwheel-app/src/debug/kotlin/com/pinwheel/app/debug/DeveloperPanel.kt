package com.pinwheel.app.debug

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pinwheel.app.DebugPanel
import com.pinwheel.core.plans.*

/** Never compiled into the default/release source set. No billing verification claim. */
class DebugEntitlement : Entitlement {
    override var tier by mutableStateOf(PlanTier.FREE)
        private set
    fun toggle() { tier = if (tier == PlanTier.FREE) PlanTier.PLUS else PlanTier.FREE }
}
class DeveloperPanel : DebugPanel {
    @Composable override fun Content() {
        val entitlement = remember { DebugEntitlement() }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Developer plan: ${entitlement.tier}")
            Button(onClick = entitlement::toggle) { Text("Toggle Plus (debug)") }
        }
    }
}
