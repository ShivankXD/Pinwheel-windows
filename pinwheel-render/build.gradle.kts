plugins { kotlin("jvm") }
dependencies {
    implementation(project(":pinwheel-core"))
    implementation(platform("org.lwjgl:lwjgl-bom:3.3.6"))
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-egl")
    implementation("org.lwjgl:lwjgl-opengles")
    runtimeOnly("org.lwjgl:lwjgl::natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengles::natives-windows")
}
