# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- R8 / ProGuard keep rules for ZWallet release builds ---

# Keep AndroidX Room entities, DAOs, and database (reflection-based).
-keep class com.example.data.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.Entity { *; }
-keepclassmembers class * extends androidx.room.Dao { *; }

# Keep Compose runtime and generated code.
-keep class androidx.compose.** { *; }
-keep class com.example.ui.** { *; }

# Keep Moshi (JSON) adapters and model classes used by GeminiService / network.
-keep class com.squareup.moshi.** { *; }
-keep class com.example.data.GeminiService { *; }

# Keep OkHttp and logging interceptor.
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep coroutines.
-keep class kotlinx.coroutines.** { *; }

# Keep AndroidX lifecycle and ViewModel.
-keep class androidx.lifecycle.** { *; }
-keep class androidx.savedstate.** { *; }

# Keep Activities / entry points.
-keep class com.example.MainActivity { *; }
-keep class androidx.activity.ComponentActivity { *; }

# Preserve names of classes referenced only via reflection (BuildConfig, R).
-keep class com.example.BuildConfig { *; }
-keep class com.example.R { *; }

# Do not obfuscate MainActivity entry point.
-keepnames class com.example.MainActivity { *; }
