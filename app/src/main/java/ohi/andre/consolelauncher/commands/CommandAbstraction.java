package ohi.andre.consolelauncher.commands;

/**
 * CommandAbstraction
 * <p>
 * The Interface that all HG2Gui commands must implement.
 * This defines the contract between the command logic and the {@link MainManager}.
 * </p>
 */
public interface CommandAbstraction {

    // -----------------------------------------------------------------------------------------
    // Argument Type Constants
    // -----------------------------------------------------------------------------------------
    // These constants define what kind of arguments the command expects.
    // The parsing engine uses this to validate input and provide autocomplete suggestions.

    /** Expects a raw string, potentially a Vogon poem. */
    int PLAIN_TEXT = 10;

    /** Expects a file path (enables file system autocomplete). */
    int FILE = 11;

    /** Expects an installed package name (enables app autocomplete). */
    int VISIBLE_PACKAGE = 12;

    /** Expects a contact name/number. */
    int CONTACTNUMBER = 13;

    /** Expects a song name. */
    int SONG = 15;

    /** Expects a boolean (True/False, or perhaps 42/Not 42). */
    int BOOLEAN = 19;

    /** Expects a HEX color code. */
    int COLOR = 21;

    /** Expects a numerical input, likely for coordinates or quantum probability calculations. */
    int INT = 24;

    // -----------------------------------------------------------------------------------------
    // Interface Methods
    // -----------------------------------------------------------------------------------------

    /**
     * Executes the command.
     *
     * @param pack The {@link ExecutePack} containing context (Context, Managers, Arguments).
     * @return The String output to display in the terminal. If null, nothing is displayed.
     * @throws Exception If execution fails.
     */
    String exec(ExecutePack pack) throws Exception;

    /**
     * Defines the expected argument types for this command.
     *
     * @return An array of integers representing the expected types (e.g., {INT, PLAIN_TEXT}).
     *         Return an empty array if no arguments are required.
     */
    int[] argType();

    /**
     * Defines the autocomplete priority.
     * Higher priority commands appear earlier in the suggestion list.
     *
     * @return Integer priority (usually 0-10).
     */
    int priority();

    /**
     * Returns the Resource ID for the help text string.
     *
     * @return R.string.help_command_name
     */
    int helpRes();

    /**
     * Called when a required argument is missing.
     *
     * @param pack The execution context.
     * @param indexNotFound The index of the missing argument.
     * @return Error message to display.
     */
    String onArgNotFound(ExecutePack pack, int indexNotFound);

    /**
     * Called when the total number of arguments provided is insufficient.
     *
     * @param pack The execution context.
     * @param nArgs The number of arguments provided.
     * @return Error message (usually usage instructions).
     */
    String onNotArgEnough(ExecutePack pack, int nArgs);
}
