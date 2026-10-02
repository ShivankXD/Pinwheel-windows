package com.pinwheel.app

import androidx.compose.runtime.Composable

/** Implementations and service metadata are compiled only by -Ppinwheel.debug=true. */
interface DebugPanel { @Composable fun Content() }
