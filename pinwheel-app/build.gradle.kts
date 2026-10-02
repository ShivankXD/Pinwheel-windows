import java.util.zip.ZipFile

plugins { kotlin("jvm"); kotlin("plugin.compose"); id("org.jetbrains.compose") }
dependencies {
    implementation(project(":pinwheel-core"))
    implementation(project(":pinwheel-platform"))
    implementation(project(":pinwheel-render"))
    implementation(project(":pinwheel-media"))
    implementation(project(":pinwheel-photo"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0-beta03")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
compose.desktop { application { mainClass = "com.pinwheel.app.MainKt" } }
val debugBuild = providers.gradleProperty("pinwheel.debug").orNull == "true"
layout.buildDirectory = layout.projectDirectory.dir(if (debugBuild) "build/debug" else "build/release")
if (debugBuild) {
    sourceSets.main { kotlin.srcDir("src/debug/kotlin"); resources.srcDir("src/debug/resources") }
    tasks.register<JavaExec>("debugCheck") {
        dependsOn("classes")
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.pinwheel.app.debug.DebugCheckKt")
        workingDir = rootDir
    }
    tasks.named("check") { dependsOn("debugCheck") }
    tasks.register<JavaExec>("effectLab") {
        dependsOn("classes")
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.pinwheel.app.debug.EffectLabKt")
        systemProperty("pinwheel.refs", providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
    }
    tasks.register<JavaExec>("p2LabSmoke") {
        dependsOn("classes")
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("com.pinwheel.app.debug.EffectLabKt")
        args(rootDir.resolve("evidence/p2").absolutePath)
        systemProperty("pinwheel.refs", providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
        workingDir = rootDir
    }
}
tasks.register("verifyBuildVariant") {
    dependsOn("jar")
    doLast {
        val archive = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        ZipFile(archive).use { zip ->
            val hasToggle = zip.getEntry("com/pinwheel/app/debug/DebugEntitlement.class") != null
            check(hasToggle == debugBuild) { "Debug entitlement leaked into the release jar or is missing from debug" }
            val hasProvider = zip.getEntry("META-INF/services/com.pinwheel.app.DebugPanel") != null
            check(hasProvider == debugBuild)
            check((zip.getEntry("com/pinwheel/app/debug/EffectLabKt.class") != null) == debugBuild) { "Effect Lab leaked into release or is missing from debug" }
        }
        println("PASS ${if (debugBuild) "debug includes" else "release excludes"} debug entitlement controls")
    }
}
tasks.named("check") { dependsOn("verifyBuildVariant") }
tasks.register<JavaExec>("p0Smoke") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.app.MainKt")
    workingDir = rootDir
    args("--smoke", rootDir.resolve("evidence/p0/runtime").absolutePath)
}
tasks.withType<JavaExec>().configureEach { workingDir = rootDir }
