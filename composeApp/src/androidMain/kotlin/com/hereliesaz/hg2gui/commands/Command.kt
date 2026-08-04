package com.hereliesaz.hg2gui.commands;

import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.commands.main.Param;
import com.hereliesaz.hg2gui.commands.main.specific.ParamCommand;
import com.hereliesaz.hg2gui.tuils.Tuils;

/**
 * Wrapper class for Executing Commands.
 * <p>
 * This class holds the concrete `CommandAbstraction` implementation along with the
 * arguments provided by the user. It handles the validation of arguments (checking count and type)
 * before delegating execution to the underlying command.
 * </p>
 */
public class Command {

    /** The command implementation (e.g. Clear, Apps). */
    public CommandAbstraction cmd;
    /** The parsed arguments provided by user input. */
    public Object[] mArgs;
    /** The number of arguments found. */
    public int nArgs;

    /**
     * If an argument was expected but not found/valid, this index indicates which one.
     * -1 if all good.
     */
    public int indexNotFound = -1;

    /**
     * Validates arguments and executes the command.
     * @param info The execution context/pack.
     * @return Output string.
     * @throws Exception If execution fails.
     */
    public String exec(ExecutePack info) throws Exception {
        // Pass the arguments to the execution pack
        info.set(mArgs);

        // --- Parameterized Command Handling (e.g. `flash -on`) ---
        if(cmd instanceof ParamCommand) {
            // If the first argument (the parameter itself) is invalid
            if(indexNotFound == 0) {
                return info.context.getString(R.string.output_invalid_param) + Tuils.SPACE + mArgs[0];
            }

            ParamCommand pCmd = (ParamCommand) cmd;
            Param param = (Param) mArgs[0];

            int[] args = param.args();

            // Check if any argument *after* the parameter is invalid
            if(indexNotFound != -1) {
                return param.onArgNotFound(info, indexNotFound);
            }

            // Check argument count for the specific parameter
            if(pCmd.defaultParamReference() != null) {
                if(args.length > nArgs) {
                    return param.onNotArgEnough(info, nArgs);
                }
            } else {
                // +1 because mArgs includes the parameter itself
                if(args.length + 1 > nArgs) {
                    return param.onNotArgEnough(info, nArgs);
                }
            }
        }
        // --- Standard Command Handling ---
        else if(indexNotFound != -1) {
            // Standard argument validation failed
            return cmd.onArgNotFound(info, indexNotFound);
        }
        else {
            // Check standard argument count
            int[] args = cmd.argType();
            if (nArgs < args.length || (mArgs == null && args.length > 0)) {
                return cmd.onNotArgEnough(info, nArgs);
            }
        }

        // Execution is safe
        return cmd.exec(info);
    }

    /**
     * Determines the type of the next expected argument based on current state.
     * Used for suggestion generation.
     * @return The integer constant representing the argument type.
     */
    public int nextArg() {
        boolean useParamArgs = cmd instanceof ParamCommand && mArgs != null && mArgs.length >= 1;

        int[] args;
        if (useParamArgs) {
            // If executing a param command and we have the param, look up *its* args
            if(!(mArgs[0] instanceof Param)) args = null;
            else args = ((Param) mArgs[0]).args();
        } else {
            // Otherwise use command's standard args
            args = cmd.argType();
        }

        if (args == null || args.length == 0) {
            return 0; // No arguments expected
        }

        try {
            // Return the type of the Nth argument (where N is current count)
            return args[useParamArgs ? nArgs - 1 : nArgs];
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0; // Overflow
        }
    }
}
