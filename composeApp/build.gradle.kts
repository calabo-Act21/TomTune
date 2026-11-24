plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.application")
}
kotlin {
    androidTarget()
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            export(project(":shared"))
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        androidMain.dependencies { implementation("androidx.activity:activity-compose:1.10.1") }
    }
}
android {
    namespace = "com.tomtune.app"
    compileSdk = 36
    defaultConfig { applicationId = "com.tomtune.app"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "0.0.1" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
compose.resources { packageOfResClass = "com.tomtune.app.resources" }
