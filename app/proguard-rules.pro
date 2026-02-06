# ReShare ProGuard rules

# Keep all app classes - Pandoc process execution and JNI interactions
# must not be obfuscated
-keep class com.reshare.** { *; }

# SQLite / Android database classes used by ConversionHistoryDb
-keep class android.database.** { *; }
-keep class android.database.sqlite.** { *; }

# Keep native method names (JNI)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes used via reflection in Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep View constructors (used by XML layout inflation)
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
