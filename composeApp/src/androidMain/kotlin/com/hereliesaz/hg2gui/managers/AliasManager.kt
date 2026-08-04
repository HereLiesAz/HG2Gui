package com.hereliesaz.hg2gui.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.tuils.Tuils
import java.io.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * Manages user-defined aliases for commands.
 *
 * Aliases allow users to create shortcuts for long commands or chain commands.
 * They are stored in a simple text file (`alias.txt`) in the format `name=value`.
 * Supports parameter substitution (e.g., `search=google %` where `%` is replaced by the argument).
 */
class AliasManager(private val context: Context) {

    companion object {
        // Actions for broadcasting alias operations
        const val ACTION_LS = "com.hereliesaz.hg2gui.alias_ls"
        const val ACTION_ADD = "com.hereliesaz.hg2gui.alias_add"
        const val ACTION_RM = "com.hereliesaz.hg2gui.alias_rm"

        const val NAME = "name"
        const val PATH = "alias.txt"

        // Temporary placeholder for parameter substitution to avoid conflicts
        private const val SECURITY_REPLACEMENT = "{#@"
    }

    private var aliases: MutableList<Alias> = mutableListOf()
    private val paramSeparator: String?
    private val aliasLabelFormat: String?
    private val replaceAllMarkers: Boolean

    private val paramMarker: String? // The character sequence used as a placeholder (e.g., "%")
    private val parameterPattern: Pattern
    private val receiver: BroadcastReceiver

    private val securityPattern = Pattern.compile(Pattern.quote(SECURITY_REPLACEMENT))

    private val pv = Pattern.compile("%v", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
    private val pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)

    init {
        val filter = IntentFilter()
        filter.addAction(ACTION_ADD)
        filter.addAction(ACTION_LS)
        filter.addAction(ACTION_RM)

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_ADD -> {
                        add(context, intent.getStringExtra(NAME), intent.getStringExtra(XMLPrefsManager.VALUE_ATTRIBUTE))
                    }
                    ACTION_RM -> {
                        remove(context, intent.getStringExtra(NAME))
                    }
                    ACTION_LS -> {
                        Tuils.sendOutput(context, printAliases())
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(context.applicationContext).registerReceiver(receiver, filter)

        // Load configuration
        paramMarker = XMLPrefsManager.get(Behavior.alias_param_marker)
        parameterPattern = Pattern.compile(Pattern.quote(paramMarker ?: "%"))
        paramSeparator = XMLPrefsManager.get(Behavior.alias_param_separator)
        aliasLabelFormat = XMLPrefsManager.get(Behavior.alias_content_format)
        replaceAllMarkers = XMLPrefsManager.getBoolean(Behavior.alias_replace_all_markers)

        // Load aliases from file
        reload()
    }

    /**
     * Returns a string representation of all aliases for display.
     */
    fun printAliases(): String {
        return aliases.joinToString(Tuils.NEWLINE) { "${it.name} --> ${it.value}" }.trim()
    }

    /**
     * Resolves an alias from a command string.
     * @param aliasInput The input string that might contain an alias.
     * @param supportSpaces Whether to attempt matching aliases with arguments separated by spaces.
     * @return String array: [0]=Resolved Value, [1]=Alias Name, [2]=Arguments/Residual
     */
    fun getAlias(aliasInput: String, supportSpaces: Boolean): Array<String?> {
        var alias = aliasInput
        if (supportSpaces) {
            var args = Tuils.EMPTYSTRING

            var aliasValue: String?
            // Iterate backwards to find longest matching alias name
            while (true) {
                aliasValue = getALias(alias)
                if (aliasValue != null) break
                else {
                    val index = alias.lastIndexOf(Tuils.SPACE)
                    if (index == -1) return arrayOf(null, null, alias)

                    args = alias.substring(index + 1) + Tuils.SPACE + args
                    args = args.trim()
                    alias = alias.substring(0, index)
                }
            }

            return arrayOf(aliasValue, alias, args)
        } else {
            return arrayOf(getALias(alias), alias, Tuils.EMPTYSTRING)
        }
    }

    /**
     * Formats the alias value by injecting parameters.
     * @param aliasValueInput The raw alias value (e.g., "echo %")
     * @param paramsInput The parameters to inject (e.g., "hello")
     * @return The formatted string (e.g., "echo hello")
     */
    fun format(aliasValueInput: String, paramsInput: String): String {
        var aliasValue = aliasValueInput
        val params = paramsInput.trim()
        if (params.isEmpty()) return aliasValue

        // Count markers
        val before = aliasValue.length
        aliasValue = parameterPattern.matcher(aliasValue).replaceAll(SECURITY_REPLACEMENT)
        val denom = abs(SECURITY_REPLACEMENT.length - (paramMarker?.length ?: 1))
        val replaced = if (denom == 0) 0 else (aliasValue.length - before) / denom

        // Split parameters
        val split = params.split(Pattern.quote(paramSeparator ?: ",").toRegex(), replaced.coerceAtLeast(1)).toTypedArray()

        // Replace markers sequentially
        for (s in split) {
            aliasValue = securityPattern.matcher(aliasValue).replaceFirst(s)
        }

        // If configured, replace all remaining markers with the first param
        if (replaceAllMarkers && split.isNotEmpty()) {
            aliasValue = securityPattern.matcher(aliasValue).replaceAll(split[0])
        }

        return aliasValue
    }

    /**
     * Retrieves the value of a specific alias by name.
     */
    private fun getALias(name: String): String? {
        return aliases.find { it.name == name }?.value
    }

    /**
     * Removes an alias from the memory list.
     */
    private fun removeAlias(name: String): Boolean {
        return aliases.removeIf { it.name == name }
    }

    /**
     * Formats the display label for an alias (used in UI feedback).
     */
    fun formatLabel(aliasName: String, aliasValue: String): String {
        var a = aliasLabelFormat ?: ""
        a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE))
        a = pv.matcher(a).replaceAll(Matcher.quoteReplacement(aliasValue))
        a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(aliasName))
        return a
    }

    /**
     * Reloads aliases from the `alias.txt` file.
     */
    fun reload() {
        aliases.clear()

        val root = Tuils.getFolder() ?: return
        val file = File(root, PATH)

        try {
            if (!file.exists()) file.createNewFile()

            file.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val splatted = line.split("=")
                    if (splatted.size >= 2) {
                        val name = splatted[0].trim()
                        val value = splatted.drop(1).joinToString("=").trim()

                        // Prevent recursive loops
                        if (name.equals(value, ignoreCase = true)) {
                            Tuils.sendOutput(
                                Color.RED, context,
                                context.getString(R.string.output_notaddingalias1) + Tuils.SPACE + name + Tuils.SPACE + context.getString(
                                    R.string.output_notaddingalias2
                                )
                            )
                        } else if (value.startsWith(name + Tuils.SPACE)) {
                            Tuils.sendOutput(
                                Color.RED, context,
                                context.getString(R.string.output_notaddingalias1) + Tuils.SPACE + name + Tuils.SPACE + context.getString(
                                    R.string.output_notaddingalias3
                                )
                            )
                        } else {
                            aliases.add(Alias(name, value, parameterPattern))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Tuils.log(e)
        }
    }

    fun dispose() {
        LocalBroadcastManager.getInstance(context.applicationContext).unregisterReceiver(receiver)
    }

    /**
     * Adds a new alias to the file and memory.
     */
    fun add(context: Context, name: String?, value: String?) {
        if ((name == null) || (value == null)) return

        if (aliases.any { it.name == name }) {
            Tuils.sendOutput(context, R.string.unavailable_name)
            return
        }

        try {
            File(Tuils.getFolder(), PATH).appendText(Tuils.NEWLINE + name + "=" + value)
            aliases.add(Alias(name, value, parameterPattern))
        } catch (e: Exception) {
            Tuils.sendOutput(context, e.toString())
        }
    }

    /**
     * Removes an alias from the file and memory.
     */
    fun remove(context: Context, name: String?) {
        if (name == null) return
        reload() // Ensure we are up to date

        if (!removeAlias(name)) {
            Tuils.sendOutput(context, R.string.invalid_name)
            return
        }

        try {
            val inputFile = File(Tuils.getFolder(), PATH)
            val tempFile = File(Tuils.getFolder(), PATH + "2")

            val prefix = "$name="
            inputFile.bufferedReader().use { reader ->
                tempFile.bufferedWriter().use { writer ->
                    reader.forEachLine { line ->
                        if (!line.startsWith(prefix)) {
                            writer.write(line + Tuils.NEWLINE)
                        }
                    }
                }
            }

            tempFile.renameTo(inputFile)
        } catch (e: Exception) {
            Tuils.sendOutput(context, e.toString())
        }
    }

    fun getAliases(excludeEmpty: Boolean): List<Alias> {
        val l = ArrayList(aliases)
        if (excludeEmpty) {
            l.removeIf { it.name.isEmpty() }
        }
        return l
    }

    /**
     * Internal representation of an alias.
     */
    class Alias(@JvmField var name: String, @JvmField var value: String, parameterPattern: Pattern) {
        @JvmField
        val isParametrized: Boolean = parameterPattern.matcher(value).find()

        override fun equals(other: Any?): Boolean {
            return (other is Alias && other.name == name) || (other is String && other == name)
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }
}
