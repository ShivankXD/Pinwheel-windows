plugins { kotlin("jvm") }
dependencies {
 implementation("org.json:json:20240303")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}
