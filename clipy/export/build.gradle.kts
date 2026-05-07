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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
}
