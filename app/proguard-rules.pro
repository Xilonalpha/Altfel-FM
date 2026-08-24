# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Media3/ExoPlayer classes used for streaming
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Gson model classes if reflection-based serialization is used
-keepattributes Signature
-keepattributes *Annotation*
