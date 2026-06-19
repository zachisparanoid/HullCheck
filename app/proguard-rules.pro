# HullCheck ProGuard / R8 rules.

# --- kotlinx.serialization ---
# Keep generated serializers and the companion @Serializable metadata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.hullcheck.**$$serializer { *; }
-keepclassmembers class com.hullcheck.** {
    *** Companion;
}
-keep class com.hullcheck.export.dto.** { *; }
-keep class com.hullcheck.data.seed.dto.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger generated code is handled by their own consumer rules ---

# --- java.time desugaring ---
-dontwarn java.time.**
