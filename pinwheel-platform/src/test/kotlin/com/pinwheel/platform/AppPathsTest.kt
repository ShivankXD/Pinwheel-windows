package com.pinwheel.platform

import kotlin.test.*

class AppPathsTest {
    @Test fun storageMatchesBriefAndSeparatesDisposableCache() {
        val paths = AppPaths.windows(mapOf("APPDATA" to "C:\\Users\\Owner\\AppData\\Roaming", "LOCALAPPDATA" to "C:\\Users\\Owner\\AppData\\Local"))
        assertTrue(paths.projects.toString().endsWith("Roaming\\Pinwheel\\projects"))
        assertTrue(paths.originals.toString().endsWith("Roaming\\Pinwheel\\originals"))
        assertTrue(paths.cache.toString().endsWith("Local\\Pinwheel\\cache"))
        assertTrue(paths.crashReports.toString().endsWith("Local\\Pinwheel\\crash-reports"))
    }
}
