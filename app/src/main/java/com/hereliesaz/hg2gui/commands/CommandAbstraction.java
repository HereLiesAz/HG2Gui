package com.hereliesaz.hg2gui.commands;

/**
 * Interface defining the structure of a terminal command.
 * <p>
 * Every command (e.g. `clear`, `apps`, `config`) must implement this interface.
 * It defines the argument types required, priority level, and the execution logic.
 * </p>
 */
public interface CommandAbstraction {

    // --- Argument Types ---
    // These constants define what kind of data the command expects for each argument.
    // Used by the parser to validate input and suggest autocompletions.

    /** Standard string argument. */
    int PLAIN_TEXT = 10;
    /** File path. */
    int FILE = 11;
    /** Package name of an installed app. */
    int VISIBLE_PACKAGE = 12;
    /** Phone number from contacts. */
    int CONTACTNUMBER = 13;
    /** List of strings. */
    int TEXTLIST = 14;
    /** Song from music library. */
    int SONG = 15;
    /** Another command name. */
    int COMMAND = 17;
    /** Parameter key (e.g. `-f` or `color`). */
    int PARAM = 18;
    /** Boolean value (true/false). */
    int BOOLEAN = 19;
    /** Package name (including hidden ones). */
    int HIDDEN_PACKAGE = 20;
    /** Hex color code or color name. */
    int COLOR = 21;
    /** XML configuration file. */
    int CONFIG_FILE = 22;
    /** Entry within a config file. */
    int CONFIG_ENTRY = 23;
    /** Integer value. */
    int INT = 24;
    /** Default application slot. */
    int DEFAULT_APP = 25;
    /** Any package installed on system. */
    int ALL_PACKAGES = 26;
    /** String without spaces. */
    int NO_SPACE_STRING = 27;
    /** Name of an App Group. */
    int APP_GROUP = 28;
    /** App within a specific group. */
    int APP_INSIDE_GROUP = 29;
    /** Long integer value. */
    int LONG = 30;
    /** Notification reply app. */
    int BOUND_REPLY_APP = 31;
    /** Datastore path. */
    int DATASTORE_PATH_TYPE = 32;

    /**
     * Executes the command.
     * @param pack Contextual information (Context, arguments, etc.).
     * @return The string output to be displayed in the terminal.
     * @throws Exception If execution fails.
     */
    String exec(ExecutePack pack) throws Exception;

    /**
     * Defines the expected argument types.
     * @return Array of argument type constants (e.g. `{FILE, INT}`).
     */
    int[] argType();

    /**
     * Defines the priority of the command in suggestions.
     * Higher values appear earlier.
     * @return Priority integer.
     */
    int priority();

    /**
     * Resource ID for the help string associated with this command.
     * @return R.string resource ID.
     */
    int helpRes();

    /**
     * Called when a required argument is missing or invalid (not found in suggestions).
     * @param pack Execution context.
     * @param indexNotFound The index of the argument that failed validation.
     * @return Error message.
     */
    String onArgNotFound(ExecutePack pack, int indexNotFound);

    /**
     * Called when the user provides fewer arguments than required.
     * @param pack Execution context.
     * @param nArgs Number of arguments provided.
     * @return Error message.
     */
    String onNotArgEnough(ExecutePack pack, int nArgs);
}
