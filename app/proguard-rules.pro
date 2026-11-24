# Add project specific ProGuard rules here.
# Room, Retrofit and kotlinx.serialization ship consumer rules, so this file is
# intentionally small — extend it if release builds start stripping something needed.

-keepattributes *Annotation*
-keep class com.eurojackpotai.app.data.remote.dto.** { *; }
-keep class com.eurojackpotai.app.data.local.entity.** { *; }
