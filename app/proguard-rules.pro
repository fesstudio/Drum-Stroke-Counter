# --- General Rules ---
-keepattributes Signature, Exceptions, *Annotation*

# --- Gson Rules ---
# Gson uses reflection to serialize/deserialize classes.
# We must keep our data models intact.
-keep class com.drummer.speed.data.model.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements com.google.gson.TypeAdapterFactory
-keep public class * implements com.google.gson.JsonSerializer
-keep public class * implements com.google.gson.JsonDeserializer

# --- Room Database Rules ---
# Room also uses reflection and generated code.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- AndroidX & Compose Rules ---
-keep class androidx.lifecycle.ViewModel { *; }
-dontwarn androidx.compose.**
