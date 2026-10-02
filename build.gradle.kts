import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
}

allprojects { group = "com.pinwheel"; version = "0.1.0-p1" }
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    tasks.withType<KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
    tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        workingDir = rootDir
        testLogging { events("passed", "skipped", "failed"); showStandardStreams = true }
    }
    dependencies {
        "testImplementation"(kotlin("test-junit5"))
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.11.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.11.4")
    }
}
tasks.register("p0Check") { dependsOn(subprojects.map { "${it.path}:check" }) }
tasks.register("p1Check") { dependsOn(subprojects.map { "${it.path}:check" }); dependsOn(":pinwheel-render:p1References") }
