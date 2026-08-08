-keep class com.hereliesaz.hg2gui.util.libsuperuser.** { *; }
-keep public class it.andreuzzi.comparestring2.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
