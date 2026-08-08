package com.hereliesaz.hg2gui.util;

/**
 * Action/extra names for the output broadcast [Utils.sendOutput] and the flashlight
 * implementation still fire. Nothing has registered a receiver for these since the legacy
 * engine's output view went away - broadcasting is a harmless no-op destination, not a crash,
 * so callers keep working without needing their own error-reporting path.
 */
public final class PrivateIOReceiver {

    public static final String ACTION_OUTPUT = "com.hereliesaz.hg2gui.action_output";
    public static final String TEXT = "com.hereliesaz.hg2gui.text";
    public static final String COLOR = "com.hereliesaz.hg2gui.color";

    private PrivateIOReceiver() {}
}
