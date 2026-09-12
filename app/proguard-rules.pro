# libVLC : classes appelees depuis le code natif (JNI)
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.medialibrary.** { *; }
-dontwarn org.videolan.**

# Gson : les DTO sont instancies par reflexion
-keep class com.footix.tv.data.remote.dto.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
