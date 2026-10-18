# Locus release shrinker rules

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.whereami.locus.** { *; }

# SQLCipher / Room
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class androidx.room.** { *; }
-dontwarn net.sqlcipher.**

# MapLibre
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**
-keep class com.google.gson.** { *; }

# EncryptedSharedPreferences / Tink
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Kotlin
-dontwarn kotlinx.coroutines.**
-keepclassmembers class **$WhenMappings { <fields>; }
