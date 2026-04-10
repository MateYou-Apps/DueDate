# Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.examples.android.model.** { <fields>; }
-keep class com.google.gson.** { *; }

# Keep data models used for Backup/Restore and Database
-keep class com.mateyou.duedate.data.** { *; }
-keep class com.mateyou.duedate.ThemeMode { *; }
-keep class com.mateyou.duedate.DateFormatPreference { *; }
-keep class com.mateyou.duedate.SortOption { *; }
