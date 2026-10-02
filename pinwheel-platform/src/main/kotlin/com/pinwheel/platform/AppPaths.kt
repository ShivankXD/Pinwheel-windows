package com.pinwheel.platform

import java.nio.file.Path

data class AppPaths(val data: Path, val cache: Path) {
    val projects: Path get() = data.resolve("projects")
    val originals: Path get() = data.resolve("originals")
    val crashReports: Path get() = cache.parent.resolve("crash-reports")
    companion object {
        fun windows(env: Map<String, String> = System.getenv()): AppPaths = AppPaths(
            Path.of(requireNotNull(env["APPDATA"]) { "APPDATA is unavailable" }, "Pinwheel"),
            Path.of(requireNotNull(env["LOCALAPPDATA"]) { "LOCALAPPDATA is unavailable" }, "Pinwheel", "cache"),
        )
    }
}
