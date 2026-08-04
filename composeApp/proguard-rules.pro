-keep public class com.hereliesaz.hg2gui.commands.main.raw.* { public *; }
-keep public abstract class com.hereliesaz.hg2gui.commands.main.generals.* { public *; }
-keep public class com.hereliesaz.hg2gui.commands.tuixt.raw.* { public *; }
-keep public class com.hereliesaz.hg2gui.managers.notifications.NotificationService
-keep public class com.hereliesaz.hg2gui.managers.notifications.KeeperService
-keep public class com.hereliesaz.hg2gui.managers.xml.options.**
-keep class com.hereliesaz.hg2gui.tuils.libsuperuser.**
-keep class com.hereliesaz.hg2gui.managers.suggestions.HideSuggestionViewValues
-keep public class it.andreuzzi.comparestring2.**

-dontwarn com.hereliesaz.hg2gui.commands.main.raw.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.htmlcleaner.**
-dontwarn com.jayway.jsonpath.**
-dontwarn org.slf4j.**

-dontwarn org.jdom2.**
