# Default ProGuard rules
-keepattributes *Annotation*
-keepclasseswithmembernames class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
