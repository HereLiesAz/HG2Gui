package com.hereliesaz.hg2gui.commands

/**
 * Interface defining the structure of a terminal command.
 *
 * Every command (e.g. `clear`, `apps`, `config`) must implement this interface.
 * It defines the argument types required, priority level, and the execution logic.
 */
interface CommandAbstraction {

    companion object {
        // --- Argument Types ---
        // These constants define what kind of data the command expects for each argument.
        // Used by the parser to validate input and suggest autocompletions.

        /** Standard string argument. */
        const val PLAIN_TEXT: Int = 10
        /** File path. */
        const val FILE: Int = 11
        /** Package name of an installed app. */
        const val VISIBLE_PACKAGE: Int = 12
        /** Phone number from contacts. */
        const val CONTACTNUMBER: Int = 13
        /** List of strings. */
        const val TEXTLIST: Int = 14
        /** Song from music library. */
        const val SONG: Int = 15
        /** Another command name. */
        const val COMMAND: Int = 17
        /** Parameter key (e.g. `-f` or `color`). */
        const val PARAM: Int = 18
        /** Boolean value (true/false). */
        const val BOOLEAN: Int = 19
        /** Package name (including hidden ones). */
        const val HIDDEN_PACKAGE: Int = 20
        /** Hex color code or color name. */
        const val COLOR: Int = 21
        /** XML configuration file. */
        const val CONFIG_FILE: Int = 22
        /** Entry within a config file. */
        const val CONFIG_ENTRY: Int = 23
        /** Integer value. */
        const val INT: Int = 24
        /** Default application slot. */
        const val DEFAULT_APP: Int = 25
        /** Any package installed on system. */
        const val ALL_PACKAGES: Int = 26
        /** String without spaces. */
        const val NO_SPACE_STRING: Int = 27
        /** Name of an App Group. */
        const val APP_GROUP: Int = 28
        /** App within a specific group. */
        const val APP_INSIDE_GROUP: Int = 29
        /** Long integer value. */
        const val LONG: Int = 30
        /** Notification reply app. */
        const val BOUND_REPLY_APP: Int = 31
        /** Datastore path. */
        const val DATASTORE_PATH_TYPE: Int = 32
    }

    /**
     * Executes the command.
     * @param pack Contextual information (Context, arguments, etc.).
     * @return The string output to be displayed in the terminal.
     * @throws Exception If execution fails.
     */
    @Throws(Exception::class)
    fun exec(pack: ExecutePack): String?

    /**
     * Defines the expected argument types.
     * @return Array of argument type constants (e.g. `{FILE, INT}`).
     */
    fun argType(): IntArray

    /**
     * Defines the priority of the command in suggestions.
     * Higher values appear earlier.
     * @return Priority integer.
     */
    fun priority(): Int

    /**
     * Resource ID for the help string associated with this command.
     * @return R.string resource ID.
     */
    fun helpRes(): Int

    /**
     * Called when a required argument is missing or invalid (not found in suggestions).
     * @param pack Execution context.
     * @param indexNotFound The index of the argument that failed validation.
     * @return Error message.
     */
    fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String?

    /**
     * Called when the user provides fewer arguments than required.
     * @param pack Execution context.
     * @param nArgs Number of arguments provided.
     * @return Error message.
     */
    fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String?
}
