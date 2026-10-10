-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class com.devkit.app.** { *; }
-keep,includedescriptorclasses class com.devkit.app.**$$serializer { *; }
-keepclasseswithmembers class com.devkit.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# SnakeYAML
-dontwarn org.yaml.snakeyaml.**
-keep class org.yaml.snakeyaml.** { *; }
