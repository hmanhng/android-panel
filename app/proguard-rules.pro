# ===========================
# General Attributes
# ===========================
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

# ===========================
# Kotlin & Coroutines
# ===========================
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }

# ===========================
# Libraries: Retrofit, OkHttp, Gson
# ===========================
# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===========================
# Android & Material Design
# ===========================
-dontwarn com.google.android.material.**
-dontwarn androidx.**

# ===========================
# App Specific Rules
# ===========================
# Models (Serialization)
-keep class com.panel.keymanager.models.** { *; }

# API Interfaces (Retrofit Reflection)
-keep interface com.panel.keymanager.api.** { *; }
