plugins { kotlin("jvm") }
dependencies {
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}
