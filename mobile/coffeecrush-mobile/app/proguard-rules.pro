# RR-13: R8 (isMinifyEnabled) + shrinkResources are on for release. Most androidx/Hilt/Compose
# libraries ship their own consumer rules; the two areas that need explicit rules here are the
# ones the official docs call out because they rely on generated code R8 can't always trace on
# its own: kotlinx.serialization (models loaded via reflection-free but generator-named
# `$$serializer` companions) and protobuf-lite (DataStore's generated message classes).

# kotlinx.serialization: https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md#android
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.semstress.mobile.**$$serializer { *; }
-keepclassmembers class com.semstress.mobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.semstress.mobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# protobuf-lite (DataStore progress store, RR-06): generated messages use a runtime scheme R8
# needs to be told about explicitly, per protobuf-lite's own R8 guidance.
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
