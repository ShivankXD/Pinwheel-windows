pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement { repositories { mavenCentral(); google() } }
rootProject.name = "Pinwheel-Windows"
include("pinwheel-core", "pinwheel-render", "pinwheel-media", "pinwheel-photo", "pinwheel-platform", "pinwheel-app")
