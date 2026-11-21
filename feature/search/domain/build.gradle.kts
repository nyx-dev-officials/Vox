plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

group = "com.vox.shared"
version = "0.1.0"

kotlin {
    android {
        namespace = "com.vox.shared.search"
        compileSdk = 37
        minSdk = 26
    }

    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "VoxSearch"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
