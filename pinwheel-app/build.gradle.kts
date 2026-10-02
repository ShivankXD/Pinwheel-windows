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
tasks.register<JavaExec>("p0Smoke") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.app.MainKt")
    workingDir = rootDir
    args("--smoke", rootDir.resolve("evidence/p0/runtime").absolutePath)
}
tasks.withType<JavaExec>().configureEach { workingDir = rootDir }
