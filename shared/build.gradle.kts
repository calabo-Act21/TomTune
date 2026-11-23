import com.android.build.gradle.LibraryExtension
plugins { kotlin("multiplatform") }
val mobile = providers.gradleProperty("mobile").orNull != "false"
if (mobile) apply(plugin = "com.android.library")
kotlin {
    jvm()
    if (mobile) {
        androidTarget()
        iosArm64()
        iosSimulatorArm64()
    }
    sourceSets {
        commonMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        if (mobile) androidMain.dependencies {
            implementation("com.google.android.filament:filament-android:1.64.1")
            implementation("com.google.android.filament:filament-utils-android:1.64.1")
            implementation("com.google.android.filament:gltfio-android:1.64.1")
        }
    }
}
if (mobile) extensions.configure<LibraryExtension> {
    namespace = "com.tomtune.shared"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
tasks.withType<Test>().configureEach {
    systemProperty("tomtune.root", rootProject.projectDir.absolutePath)
    testLogging { events("passed", "skipped", "failed"); showStandardStreams = true }
}
