# Keep Room, Media3, and ONNX Runtime JNI classes intact during R8 release shrinking
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**
