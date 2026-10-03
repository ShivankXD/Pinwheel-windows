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
referenceTask("strictGoldenCheck", "golden")
referenceTask("goldenAudit", "golden-audit")
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
tasks.register<JavaExec>("p2NoiseSources") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2NoiseSourcesKt")
    args(rootDir.absolutePath)
}
tasks.register<JavaExec>("p2NumericProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2NumericProbeKt")
    args(rootDir.absolutePath)
    workingDir = rootDir
}

tasks.register<JavaExec>("p2FramebufferProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2FramebufferProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
    workingDir = rootDir
}

tasks.register<JavaExec>("p2FramebufferCurrentProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2FramebufferProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"), "framebuffer-current", "structural")
    workingDir = rootDir
}

tasks.register<JavaExec>("p2RoundingProbe") {
    description = "Negative control: single-pass rounding does not enforce staged float32 scaling; expected numeric gate failure."
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2FramebufferProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"), "framebuffer-rounding", "structural", "even")
    workingDir = rootDir
}

tasks.register<JavaExec>("p2StagedRoundingProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2FramebufferProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"), "framebuffer-staged-rounding", "structural", "even", "staged")
    workingDir = rootDir
}

tasks.register<JavaExec>("p2DeterministicProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2DeterministicProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
    workingDir = rootDir
}

tasks.register<JavaExec>("p2SamplingProbe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2SamplingProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
    workingDir = rootDir
}

tasks.register<JavaExec>("p2Storage8Probe") {
    dependsOn("classes")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2Storage8ProbeKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
    workingDir = rootDir
}
tasks.register<JavaExec>("p2StructuralReview") {
    dependsOn("classes", "goldenAudit")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2StructuralEvidenceKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"))
}
tasks.register<JavaExec>("goldenCheck") {
    dependsOn("classes", "goldenAudit")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.pinwheel.render.qa.P2StructuralEvidenceKt")
    args(rootDir.absolutePath, providers.gradleProperty("pinwheel.refs").getOrElse("D:/Pinwheel-Windows-refs"), "check")
}
