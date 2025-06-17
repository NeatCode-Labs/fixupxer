# Add project specific ProGuard rules here.

# Keep the application class
-keep class com.fixupxer.** { *; }

# Keep Compose UI classes
-keep class androidx.compose.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep Material3 components
-keep class com.google.android.material.** { *; }

# Keep AndroidX components
-keep class androidx.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep R8 rules
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

# Keep Android runtime classes
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep any classes referenced from XML layouts
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.view.View
-keep public class * extends android.widget.TextView
-keep public class * extends android.widget.Button

# Keep the R class for resources
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable

# Keep all model classes
-keep class com.fixupxer.models.** { *; }

# Keep all enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BuildConfig
-keep class com.fixupxer.BuildConfig { *; }

# Preserve the special static methods that are required in all enumeration classes
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
} 