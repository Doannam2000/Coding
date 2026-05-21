# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep FFmpegKit classes
-keep class com.arthenica.ffmpegkit.** { *; }

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }

# Keep Data classes for GSON or internal serialization
-keepclassmembers class com.nantcompany.clipy.export.job.ProcessingRequest {
    *;
}
-keepclassmembers class * extends com.nantcompany.clipy.export.job.ProcessingRequest {
    *;
}

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile
