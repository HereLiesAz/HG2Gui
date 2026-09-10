# Keep public API surface of the embedded libsuperuser fork — accessed via reflection by
# Shell.Interactive and its callback interfaces. Private members excluded.
-keep class com.hereliesaz.hg2gui.util.libsuperuser.** {
    public *;
    protected *;
}
