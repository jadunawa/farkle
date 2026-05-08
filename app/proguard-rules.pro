# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.orangezest.farkle.**$$serializer { *; }
-keepclassmembers class com.orangezest.farkle.** { *** Companion; }
-keepclasseswithmembers class com.orangezest.farkle.** { kotlinx.serialization.KSerializer serializer(...); }

# Protobuf
-keep class com.orangezest.farkle.data.proto.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# DataStore
-keep class androidx.datastore.** { *; }
