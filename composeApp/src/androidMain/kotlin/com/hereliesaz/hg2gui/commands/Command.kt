package com.hereliesaz.hg2gui.commands

import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.commands.main.Param
import com.hereliesaz.hg2gui.commands.main.specific.ParamCommand
import com.hereliesaz.hg2gui.tuils.Tuils

/**
 * Wrapper class for Executing Commands.
 * <p>
 * This class holds the concrete `CommandAbstraction` implementation along with the
 * arguments provided by the user. It handles the validation of arguments (checking count and type)
 * before delegating execution to the underlying command.
 * </p>
 */
class Command {
    /** The command implementation (e.g. Clear, Apps). */
    var cmd: CommandAbstraction? = null

    /** The parsed arguments provided by user input. */
    var mArgs: Array<Any?>? = null

    /** The number of arguments found. */
    var nArgs: Int = 0

    /**
     * If an argument was expected but not found/valid, this index indicates which one.
     * -1 if all good.
     */
    var indexNotFound: Int = -1

    /**
     * Validates arguments and executes the command.
     * @param info The execution context/pack.
     * @return Output string.
     * @throws Exception If execution fails.
     */
    @Throws(Exception::class)
    fun exec(info: ExecutePack): String? {
        val command = cmd ?: return null
        
        // Pass the arguments to the execution pack
        info.set(mArgs)

        // --- Parameterized Command Handling (e.g. `flash -on`) ---
        if (command is ParamCommand) {
            // If the first argument (the parameter itself) is invalid
            if (indexNotFound == 0) {
                return Tuils.getContext(info).getString(R.string.output_invalid_param) + Tuils.SPACE + mArgs!![0]
            }

            val pCmd = command
            val param = mArgs!![0] as Param
            val args = param.args()

            // Check if any argument *after* the parameter is invalid
            if (indexNotFound != -1) {
                return param.onArgNotFound(info, indexNotFound)
            }

            // Check argument count for the specific parameter
            if (pCmd.defaultParamReference() != null) {
                if (args.size > nArgs) {
                    return param.onNotArgEnough(info, nArgs)
                }
            } else {
                // +1 because mArgs includes the parameter itself
                if (args.size + 1 > nArgs) {
                    return param.onNotArgEnough(info, nArgs)
                }
            }
        } else if (indexNotFound != -1) {
            // Standard argument validation failed
            return command.onArgNotFound(info, indexNotFound)
        } else {
            // Check standard argument count
            val args = command.argType()
            if (nArgs < args.size || (mArgs == null && args.size > 0)) {
                return command.onNotArgEnough(info, nArgs)
            }
        }

        // Execution is safe
        return command.exec(info)
    }

    /**
     * Determines the type of the next expected argument based on current state.
     * Used for suggestion generation.
     * @return The integer constant representing the argument type.
     */
    fun nextArg(): Int {
        val command = cmd ?: return 0
        
        val useParamArgs = command is ParamCommand && mArgs != null && mArgs!!.size >= 1

        val args: IntArray?
        if (useParamArgs) {
            // If executing a param command and we have the param, look up *its* args
            if (mArgs!![0] !is Param) {
                args = null
            } else {
                args = (mArgs!![0] as Param).args()
            }
        } else {
            // Otherwise use command's standard args
            args = command.argType()
        }

        if (args == null || args.isEmpty()) {
            return 0 // No arguments expected
        }

        return try {
            // Return the type of the Nth argument (where N is current count)
            args[if (useParamArgs) nArgs - 1 else nArgs]
        } catch (e: ArrayIndexOutOfBoundsException) {
            0 // Overflow
        }
    }
}
