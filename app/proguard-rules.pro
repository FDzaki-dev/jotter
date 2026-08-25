-keep class com.jotter.notes.data.** { *; }
-keepattributes *Annotation*

# Google Tink (dibawa oleh androidx.security-crypto) mereferensikan library
# annotation compile-time-only (error-prone, JSR-305) yang tidak ada di runtime.
# Aman diabaikan — sesuai rekomendasi resmi Tink & missing_rules.txt dari R8.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
