package com.hereliesaz.hg2gui.commands.main.raw;

import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;
import com.hereliesaz.hg2gui.commands.main.specific.PermanentSuggestionCommand;
import com.hereliesaz.hg2gui.commands.main.MainPack;
import com.hereliesaz.hg2gui.tuils.Tuils;

/**
 * Calculator command.
 * <p>
 * Performs simple arithmetic operations.
 * Syntax: `calc [expression]`
 * Example: `calc 10 + 5 * 2`
 * </p>
 */
public class calc extends PermanentSuggestionCommand {

    /**
     * Execution logic.
     * Evaluates the string expression using Tuils.eval().
     */
    @Override
    public String exec(ExecutePack pack) throws Exception {
        try {
            // pack.getString() joins all arguments into a single string
            return String.valueOf(Tuils.eval(pack.getString()));
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Argument definition.
     * Expects a plain text string (the mathematical expression).
     */
    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PLAIN_TEXT};
    }

    /**
     * Suggestion priority.
     * 3 is a moderate priority.
     */
    @Override
    public int priority() {
        return 3;
    }

    /**
     * Resource ID for the help text.
     */
    @Override
    public int helpRes() {
        return R.string.help_calc;
    }

    /**
     * Handler for missing arguments (not used here as type is PLAIN_TEXT).
     */
    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    /**
     * Handler for insufficient arguments.
     * Returns the help text if no expression is provided.
     */
    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.res.getString(helpRes());
    }

    /**
     * Provides permanent suggestions (buttons) above the keyboard.
     * Useful for operators.
     */
    @Override
    public String[] permanentSuggestions() {
        return new String[] {"(", ")", "+", "-", "*", "/", "%", "^", "sqrt"};
    }
}
