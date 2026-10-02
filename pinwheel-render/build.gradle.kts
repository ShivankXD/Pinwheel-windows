plugins { kotlin("jvm") }
dependencies {
    implementation("org.json:json:20240303")
    implementation(project(":pinwheel-core"))
    implementation(platform("org.lwjgl:lwjgl-bom:3.3.6"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-egl")
    implementation("org.lwjgl:lwjgl-opengles")
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
