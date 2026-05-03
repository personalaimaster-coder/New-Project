# Keep Hilt + Room generated code
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.example.petmeds.**$$serializer { *; }
-keepclassmembers class com.example.petmeds.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.petmeds.** {
    kotlinx.serialization.KSerializer serializer(...);
}
