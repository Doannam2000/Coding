# Keep this file intentionally narrow so release minification stays based on observed
# needs instead of broad keep-all rules.

-keep class androidx.core.content.FileProvider { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class com.example.clipystudio.** {
    @kotlinx.serialization.Serializable *;
}
