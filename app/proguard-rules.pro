# ============================================================
# AnimY ProGuard / R8 规则
# ============================================================

# ---------- 基础属性 ----------
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ---------- Application / Activity ----------
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends androidx.activity.ComponentActivity
-keep class com.grillpoet.animy.MainActivity { *; }
-keep class com.grillpoet.animy.AnimYApp { *; }

# ---------- 数据模型与序列化（kotlinx.serialization）----------
# 保留所有 @Serializable 类及其成员
-keep @kotlinx.serialization.Serializable class * { *; }
-keep class com.grillpoet.animy.data.persistence.** { *; }
-keep class com.grillpoet.animy.domain.model.** { *; }

# 保留序列化器
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ---------- Kotlin 元数据 ----------
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---------- 枚举 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Jetpack Compose ----------
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# ---------- Timber ----------
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# ---------- Glide GIF Encoder ----------
-keep class com.bumptech.glide.gifencoder.** { *; }
-dontwarn com.bumptech.glide.**

# ---------- Reorderable ----------
-keep class sh.calvin.reorderable.** { *; }
-dontwarn sh.calvin.reorderable.**

# ---------- 其他通用保留 ----------
# 保留自定义 View（若后续添加）
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 保留 Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 R 类资源字段
-keepclassmembers class **.R$* {
    public static <fields>;
}