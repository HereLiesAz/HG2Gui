-keep class com.hereliesaz.hg2gui.util.libsuperuser.** { *; }
-keep public class it.andreuzzi.comparestring2.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# anthropic-java pulls in Jackson, whose optional java.beans.* introspection path (Java7SupportImpl)
# references JDK classes that don't exist on Android - never reached at runtime on this platform.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
