# ============================================
# ML Kit Barcode Scanning
# ============================================
# 保持ML Kit相关类不被混淆（包括所有内部类）
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.android.gms.vision.**

# 保持Google Play Services相关类
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# 保持Vision API相关类
-keep interface com.google.mlkit.** { *; }

# ============================================
# CameraX
# ============================================
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ============================================
# Kotlin
# ============================================
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ============================================
# Compose
# ============================================
-keep class androidx.compose.** { *; }
-keepattributes *Annotation*

# ============================================
# JSON (org.json)
# ============================================
-keep class org.json.** { *; }

# ============================================
# OkHttp
# ============================================
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# ============================================
# Coil (Image Loading)
# ============================================
-keep class coil.** { *; }
-dontwarn coil.**

# ============================================
# Media3 (ExoPlayer)
# ============================================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ============================================
# Shizuku
# ============================================
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# ============================================
# Miuix
# ============================================
-keep class top.yukonga.miuix.** { *; }
-dontwarn top.yukonga.miuix.**

# ============================================
# General Rules
# ============================================
# 保持枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保持Parcelable实现
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保持Serializable实现
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保持Native方法
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
