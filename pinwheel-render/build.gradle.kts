plugins { kotlin("jvm") }
dependencies {
    implementation("org.json:json:20240303")
    implementation(project(":pinwheel-core"))
    implementation(platform("org.lwjgl:lwjgl-bom:3.3.6"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-egl")
    implementation("org.lwjgl:lwjgl-opengles")
    implementation("org.jetbrains.skiko:skiko-awt:0.9.37.4")
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.37.4")
    runtimeOnly("org.lwjgl:lwjgl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengles::natives-windows")
}

fun referenceTask(name: String, mode: String) = tasks.register<JavaExec>(name) {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P1EvidenceKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"), mode)
}
referenceTask("p1References", "inventory")
referenceTask("goldenCheck", "golden")
referenceTask("mobilePackagesCheck", "projects")
referenceTask("p1ProjectInventory", "project-inventory")

tasks.register<JavaExec>("p2Frames") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2EvidenceKt")
    args(rootDir.absolutePath)
    workingDir = rootDir
}
tasks.register<JavaExec>("p2MathProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2MathProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
    workingDir = rootDir
}
