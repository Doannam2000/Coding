plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.natncompany.media"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(files("libs/ffmpeg-kit-full-gpl-5.1.LTS-16K-full.aar"))
    implementation(project(":gpuimage"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
