plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.nantcompany.clipy.export"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":edit"))
    implementation(libs.androidx.core.ktx)
    implementation(files("libs/ffmpeg-kit-full-gpl-5.1.LTS-16K-full.aar"))
    implementation("com.arthenica:smart-exception-java:0.2.1")
}

