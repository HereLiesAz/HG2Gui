package com.hereliesaz.hg2gui.managers.suggestions

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.hereliesaz.hg2gui.R
import com.hereliesaz.hg2gui.UIManager
import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.commands.CommandTuils
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.commands.main.Param
import com.hereliesaz.hg2gui.commands.main.specific.ParamCommand
import com.hereliesaz.hg2gui.commands.main.specific.PermanentSuggestionCommand
import com.hereliesaz.hg2gui.managers.AliasManager
import com.hereliesaz.hg2gui.managers.AppsManager
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.FileManager
import com.hereliesaz.hg2gui.managers.RssManager
import com.hereliesaz.hg2gui.managers.TerminalManager
import com.hereliesaz.hg2gui.managers.music.Song
import com.hereliesaz.hg2gui.managers.notifications.NotificationManager
import com.hereliesaz.hg2gui.managers.notifications.reply.BoundApp
import com.hereliesaz.hg2gui.managers.notifications.reply.ReplyManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.classes.XMLPrefsSave
import com.hereliesaz.hg2gui.managers.xml.options.*
import com.hereliesaz.hg2gui.tuils.StoppableThread
import com.hereliesaz.hg2gui.tuils.Tuils
import it.andreuzzi.comparestring2.AlgMap
import it.andreuzzi.comparestring2.CompareObjects
import it.andreuzzi.comparestring2.CompareStrings
import it.andreuzzi.comparestring2.StringableObject
import it.andreuzzi.comparestring2.algs.interfaces.Algorithm
import java.io.File
import java.util.*
import java.util.regex.Pattern

class SuggestionsManager(private val suggestionsView: LinearLayout, private val pack: MainPack, private val mTerminalAdapter: TerminalManager) {

    private var hideViewValue: HideSuggestionViewValues? = null

    private val SPLITTERS = arrayOf(Tuils.SPACE)
    private val FILE_SPLITTERS = arrayOf(Tuils.SPACE, "-", "_")
    private val XML_PREFS_SPLITTERS = arrayOf("_")

    private var showAliasDefault: Boolean = false
    private var clickToLaunch: Boolean = false
    private var showAppsGpDefault: Boolean = false
    private var enabled: Boolean = true
    private var minCmdPriority: Int = 0

    private var multipleCmdSeparator: String = ""
    private var doubleSpaceFirstSuggestion: Boolean = false
    private var suggestionRunnable: SuggestionRunnable? = null
    private var suggestionViewParams: LinearLayout.LayoutParams? = null
    private var lastFirst: Suggestion? = null

    private val clickListener = View.OnClickListener { v ->
        val suggestion = v.getTag(R.id.suggestion_id) as Suggestion
        clickSuggestion(suggestion)
    }

    private var lastSuggestionThread: StoppableThread? = null
    private val handler = Handler(Looper.getMainLooper())
    private val removeAllSuggestions: RemoverRunnable = RemoverRunnable(suggestionsView)

    private val spaces: IntArray
    private val counts: IntArray
    private val noInputCounts: IntArray
    private val rmQuotes = Pattern.compile("['\"]")

    var suggestionsPerCategory: Int = 0
    var suggestionsDeadline: Float = 0f

    private val comparator: CustomComparator
    private var algInstance: Algorithm? = null
    private var alg: AlgMap.Alg? = null
    private var quickCompare: Int = 0

    private var currentVineView: View? = null

    init {
        setAlgorithm(XMLPrefsManager.getInt(Suggestions.suggestions_algorithm))
        quickCompare = XMLPrefsManager.getInt(Suggestions.suggestions_quickcompare_n)
        suggestionsPerCategory = XMLPrefsManager.getInt(Suggestions.suggestions_per_category)
        suggestionsDeadline = (XMLPrefsManager.get(Suggestions.suggestions_deadline) ?: "0").toFloat()

        doubleSpaceFirstSuggestion = XMLPrefsManager.getBoolean(Suggestions.double_space_click_first_suggestion)
        Suggestion.appendQuotesBeforeFile = XMLPrefsManager.getBoolean(Behavior.append_quote_before_file)
        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator) ?: ""

        showAliasDefault = XMLPrefsManager.getBoolean(Suggestions.suggest_alias_default)
        showAppsGpDefault = XMLPrefsManager.getBoolean(Suggestions.suggest_appgp_default)
        clickToLaunch = XMLPrefsManager.getBoolean(Suggestions.click_to_launch)
        minCmdPriority = XMLPrefsManager.getInt(Suggestions.noinput_min_command_priority)

        spaces = UIManager.getListOfIntValues(XMLPrefsManager.get(Suggestions.suggestions_spaces) ?: "", 4, 0)

        hideViewValue = try {
            HideSuggestionViewValues.valueOf((XMLPrefsManager.get(Suggestions.hide_suggestions_when_empty) ?: "").uppercase())
        } catch (e: Exception) {
            HideSuggestionViewValues.valueOf(Suggestions.hide_suggestions_when_empty.defaultValue().uppercase())
        }

        val orderPattern = Pattern.compile("(\\d+)\\((\\d+)\\)")
        val indexes = IntArray(4)
        counts = IntArray(4)
        var m = orderPattern.matcher(XMLPrefsManager.get(Suggestions.suggestions_order))
        var index = 0
        while (m.find() && index < indexes.size) {
            val type = m.group(1).toInt()
            if (type >= indexes.size) {
                Tuils.sendOutput(Color.RED, Tuils.getContext(pack), "Invalid suggestion type: $type")
                break
            }
            indexes[type] = index
            counts[type] = m.group(2).toInt()
            index++
        }

        val noInputIndexes = IntArray(4)
        noInputCounts = IntArray(4)
        m = orderPattern.matcher(XMLPrefsManager.get(Suggestions.noinput_suggestions_order))
        index = 0
        while (m.find() && index < noInputIndexes.size) {
            val type = m.group(1).toInt()
            if (type >= noInputIndexes.size) {
                Tuils.sendOutput(Color.RED, Tuils.getContext(pack), "Invalid suggestion type: $type")
                break
            }
            noInputIndexes[type] = index
            noInputCounts[type] = m.group(2).toInt()
            index++
        }

        comparator = CustomComparator(noInputIndexes, indexes)

        val uselessView = getSuggestionView(Tuils.getContext(pack))
        uselessView.visibility = View.INVISIBLE
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(spaces[0], spaces[1], spaces[2], spaces[3])
        (suggestionsView.parent as LinearLayout).addView(uselessView, params)
    }

    private fun setAlgorithm(id: Int) {
        alg = when (id) {
            0 -> AlgMap.DistAlg.LCS
            1 -> AlgMap.DistAlg.OSA
            2 -> AlgMap.DistAlg.QGRAM
            4 -> AlgMap.NormDistAlg.COSINE
            5 -> AlgMap.NormDistAlg.JACCARD
            6 -> AlgMap.NormDistAlg.JAROWRINKLER
            7 -> AlgMap.NormDistAlg.METRICLCS
            8 -> AlgMap.NormDistAlg.NGRAM
            9 -> AlgMap.NormDistAlg.NLEVENSHTEIN
            10 -> AlgMap.NormDistAlg.SORENSENDICE
            11 -> AlgMap.NormSimAlg.COSINE
            12 -> AlgMap.NormSimAlg.JACCARD
            13 -> AlgMap.NormSimAlg.JAROWRINKLER
            14 -> AlgMap.NormSimAlg.NLEVENSHTEIN
            15 -> AlgMap.NormSimAlg.SORENSENDICE
            16 -> AlgMap.MetricDistAlg.DAMERAU
            17 -> AlgMap.MetricDistAlg.JACCARD
            18 -> AlgMap.MetricDistAlg.LEVENSHTEIN
            19 -> AlgMap.MetricDistAlg.METRICLCS
            else -> AlgMap.DistAlg.LCS
        }
        algInstance = alg!!.buildAlg(id)
    }

    fun getSuggestionView(context: Context): TextView {
        val textView = TextView(context)
        textView.setOnClickListener(clickListener)
        textView.isFocusable = false
        textView.isLongClickable = false
        textView.isClickable = true
        textView.typeface = Tuils.getTypeface(context)
        textView.setTextSize(XMLPrefsManager.getInt(Suggestions.suggestions_size).toFloat())
        textView.setPadding(spaces[2], spaces[3], spaces[2], spaces[3])
        textView.setLines(1)
        textView.maxLines = 1
        return textView
    }

    private fun stop() {
        handler.removeCallbacksAndMessages(null)
        lastSuggestionThread?.interrupt()
    }

    fun dispose() {
        stop()
    }

    fun clear() {
        stop()
        suggestionsView.removeAllViews()
        currentVineView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            currentVineView = null
        }
    }

    private val hideRunnable = Runnable {
        suggestionsView.visibility = View.GONE
        stop()
    }

    fun hide() {
        if (Looper.getMainLooper() == Looper.myLooper()) hideRunnable.run()
        else (Tuils.getContext(pack) as Activity).runOnUiThread(hideRunnable)
    }

    private val showRunnable = Runnable {
        suggestionsView.visibility = View.VISIBLE
    }

    fun show() {
        if (Looper.getMainLooper() == Looper.myLooper()) showRunnable.run()
        else (Tuils.getContext(pack) as Activity).runOnUiThread(showRunnable)
    }

    fun enable() {
        enabled = true
        show()
    }

    fun disable() {
        enabled = false
        hide()
    }

    fun clickSuggestion(suggestion: Suggestion) {
        val execOnClick = suggestion.exec
        val text = suggestion.getText()
        val input = mTerminalAdapter.getInput()

        if (suggestion.type == Suggestion.TYPE_PERMANENT) {
            mTerminalAdapter.setInput(input + text)
        } else {
            val addSpace = suggestion.type != Suggestion.TYPE_FILE && suggestion.type != Suggestion.TYPE_COLOR
            if (multipleCmdSeparator.isNotEmpty()) {
                val split = input.split(multipleCmdSeparator).toMutableList()
                if (split.size == 1) {
                    mTerminalAdapter.setInput(text + (if (addSpace) Tuils.SPACE else Tuils.EMPTYSTRING))
                } else {
                    split[split.size - 1] = Tuils.EMPTYSTRING
                    var beforeInputs = ""
                    for (i in 0 until split.size - 1) beforeInputs += split[i] + multipleCmdSeparator
                    mTerminalAdapter.setInput(beforeInputs + text + (if (addSpace) Tuils.SPACE else Tuils.EMPTYSTRING))
                }
            } else {
                mTerminalAdapter.setInput(text + (if (addSpace) Tuils.SPACE else Tuils.EMPTYSTRING))
            }
        }

        if (execOnClick) mTerminalAdapter.simulateEnter()
        else mTerminalAdapter.focusInputEnd()
    }

    fun requestSuggestion(input: String) {
        if (!enabled) return

        if (suggestionViewParams == null) {
            suggestionViewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(15, 0, 15, 0)
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        if (suggestionRunnable == null) {
            suggestionRunnable = SuggestionRunnable(pack, suggestionsView, suggestionViewParams, suggestionsView.parent.parent as HorizontalScrollView, spaces, this)
        }

        lastSuggestionThread?.interrupt()
        suggestionRunnable?.interrupt()
        handler.removeCallbacks(suggestionRunnable!!)

        try {
            val l = input.length
            if (doubleSpaceFirstSuggestion && l > 0 && input[l - 1] == ' ') {
                if (l >= 2 && input[l - 2] == ' ') {
                    if (lastFirst == null && suggestionsView.childCount > 0) {
                        val s = suggestionsView.getChildAt(0).getTag(R.id.suggestion_id) as Suggestion
                        if (!input.trim().endsWith(s.getText())) lastFirst = s
                    }
                    if (lastFirst != null) {
                        val s = lastFirst!!
                        mTerminalAdapter.setInput(if (0 == l - 2) Tuils.EMPTYSTRING else input.substring(0, l - 2))
                        clickSuggestion(s)
                        return
                    }
                } else if (suggestionsView.childCount > 0) {
                    lastFirst = suggestionsView.getChildAt(0).getTag(R.id.suggestion_id) as Suggestion
                    if (lastFirst!!.getText() == input.trim()) lastFirst = null
                }
            } else {
                lastFirst = null
            }
        } catch (e: Exception) {
            Tuils.log(e)
        }

        lastSuggestionThread = object : StoppableThread() {
            override fun run() {
                super.run()
                val lastInput = if (multipleCmdSeparator.isNotEmpty()) {
                    val split = input.split(multipleCmdSeparator)
                    if (split.isEmpty()) input else split.last()
                } else input

                val lastSpace = lastInput.lastIndexOf(Tuils.SPACE)
                val (before, lastWord) = if (lastSpace == -1) {
                    Tuils.EMPTYSTRING to lastInput
                } else {
                    lastInput.substring(0, lastSpace) to lastInput.substring(lastSpace + 1)
                }

                val suggestions = try {
                    getSuggestions(before, lastWord)
                } catch (e: Exception) {
                    Tuils.log(e)
                    return
                }

                if (suggestions.isEmpty()) {
                    (Tuils.getContext(pack) as Activity).runOnUiThread(removeAllSuggestions)
                    removeAllSuggestions.isGoingToRun = true
                    if (hideViewValue == HideSuggestionViewValues.ALWAYS || (hideViewValue == HideSuggestionViewValues.TRUE && input.isEmpty())) {
                        hide()
                    }
                    return
                } else {
                    if (removeAllSuggestions.isGoingToRun) removeAllSuggestions.stop = true
                    show()
                }

                if (interrupted()) {
                    suggestionRunnable?.interrupt()
                    return
                }

                val existingViews = Array(suggestionsView.childCount) { i -> suggestionsView.getChildAt(i) as TextView }
                if (interrupted()) {
                    suggestionRunnable?.interrupt()
                    return
                }

                val n = suggestions.size - existingViews.size
                var toAdd: Array<TextView>? = null
                var toRecycle: Array<TextView>? = null
                if (n == 0) {
                    toRecycle = existingViews
                } else if (n > 0) {
                    toRecycle = existingViews
                    toAdd = Array(n) { getSuggestionView(Tuils.getContext(pack)) }
                } else {
                    toRecycle = Array(suggestions.size) { i -> existingViews[i] }
                }

                if (interrupted()) {
                    suggestionRunnable?.interrupt()
                    return
                }

                suggestionRunnable?.apply {
                    setN(n)
                    setSuggestions(suggestions)
                    setToAdd(toAdd)
                    setToRecycle(toRecycle)
                    reset()
                    (Tuils.getContext(pack) as Activity).runOnUiThread(this)
                }
            }
        }
        try {
            lastSuggestionThread!!.start()
        } catch (e: InternalError) {
            Tuils.log(e)
        }
    }

    fun getSuggestions(beforeLastSpace: String, lastWord: String): List<Suggestion> {
        val suggestionList = mutableListOf<Suggestion>()
        val fullInput = if (lastWord.isEmpty()) beforeLastSpace else if (beforeLastSpace.isEmpty()) lastWord else "$beforeLastSpace ${Tuils.SPACE} $lastWord"

        val menuOptions = CommandMenu.getSuggestions(fullInput)
        if (!menuOptions.isNullOrEmpty()) {
            for (opt in menuOptions) {
                var textBefore = ""
                val parts = fullInput.trim().split(Tuils.SPACE)
                if (fullInput.endsWith(Tuils.SPACE)) textBefore = fullInput.trim()
                else if (parts.size > 1) textBefore = parts.dropLast(1).joinToString(Tuils.SPACE)
                suggestionList.add(Suggestion(textBefore, opt.value, false, Suggestion.TYPE_MENU_OPTION, opt))
            }
            if (suggestionList.isNotEmpty()) return suggestionList
        }

        val bls = beforeLastSpace.trim()
        val lw = lastWord.trim()

        if (lw.isEmpty()) {
            if (bls.isEmpty()) {
                comparator.noInput = true
                pack.appsManager.getSuggestedApps().let { apps ->
                    for (i in 0 until apps.size.coerceAtMost(noInputCounts[Suggestion.TYPE_APP])) {
                        apps[i]?.let { suggestionList.add(Suggestion(bls, it.publicLabel ?: "", clickToLaunch, Suggestion.TYPE_APP, it)) }
                    }
                }
                suggestCommand(pack, suggestionList, "")
                if (showAliasDefault) suggestAlias(pack.aliasManager, suggestionList, lw)
                if (showAppsGpDefault) suggestAppGroup(pack, suggestionList, lw, bls)
            } else {
                comparator.noInput = false
                val cmd = try { CommandTuils.parse(bls, pack) } catch (e: Exception) { null }
                if (cmd != null) {
                    if (cmd.cmd is PermanentSuggestionCommand) suggestPermanentSuggestions(suggestionList, cmd.cmd as PermanentSuggestionCommand)
                    val mArgs = cmd.mArgs
                    if (mArgs != null && mArgs.isNotEmpty() && cmd.cmd is ParamCommand && cmd.nArgs >= 1 && mArgs[0] is Param && (mArgs[0] as Param).args().size + 1 == cmd.nArgs) {
                        // nothing
                    } else {
                        if (cmd.cmd is ParamCommand && (mArgs == null || mArgs.isEmpty() || mArgs[0] is String)) {
                            suggestParams(pack, suggestionList, cmd.cmd as ParamCommand, bls, null)
                        } else suggestArgs(pack, cmd.nextArg(), suggestionList, bls)
                    }
                } else {
                    val split = rmQuotes.matcher(bls).replaceAll(Tuils.EMPTYSTRING).split(Tuils.SPACE)
                    if (split.any { needsFileSuggestion(it) }) suggestFile(pack, suggestionList, Tuils.EMPTYSTRING, bls)
                    else if (!suggestAppInsideGroup(pack, suggestionList, Tuils.EMPTYSTRING, bls, false)) suggestApp(pack, suggestionList, "$bls ${Tuils.SPACE}", Tuils.EMPTYSTRING)
                }
            }
        } else {
            comparator.noInput = false
            if (bls.isNotEmpty()) {
                val cmd = try { CommandTuils.parse(bls, pack) } catch (e: Exception) { null }
                if (cmd != null) {
                    if (cmd.cmd is PermanentSuggestionCommand) suggestPermanentSuggestions(suggestionList, cmd.cmd as PermanentSuggestionCommand)
                    val mArgs = cmd.mArgs
                    if (cmd.cmd is ParamCommand && (mArgs == null || mArgs.isEmpty() || mArgs[0] is String)) {
                        suggestParams(pack, suggestionList, cmd.cmd as ParamCommand, bls, lw)
                    } else suggestArgs(pack, cmd.nextArg(), suggestionList, lw, bls)
                } else {
                    val split = bls.replace("['\"]".toRegex(), Tuils.EMPTYSTRING).split(Tuils.SPACE)
                    if (split.any { needsFileSuggestion(it) }) suggestFile(pack, suggestionList, lw, bls)
                    else if (!suggestAppInsideGroup(pack, suggestionList, lw, bls, false)) suggestApp(pack, suggestionList, "$bls ${Tuils.SPACE} $lw", Tuils.EMPTYSTRING)
                }
            } else {
                suggestCommand(pack, suggestionList, lw, bls)
                suggestAlias(pack.aliasManager, suggestionList, lw)
                suggestApp(pack, suggestionList, lw, Tuils.EMPTYSTRING)
                suggestAppGroup(pack, suggestionList, lw, bls)
            }
        }

        Collections.sort(suggestionList, comparator)
        return suggestionList
    }

    private fun needsFileSuggestion(cmd: String): Boolean = cmd.lowercase() in listOf("ls", "cd", "mv", "cp", "rm", "cat")

    private fun suggestPermanentSuggestions(suggestions: MutableList<Suggestion>, cmd: PermanentSuggestionCommand) {
        for (s in cmd.permanentSuggestions()) suggestions.add(Suggestion(null, s, false, Suggestion.TYPE_PERMANENT))
    }

    private fun suggestAlias(aliasManager: AliasManager, suggestions: MutableList<Suggestion>, lastWord: String) {
        var canInsert = if (lastWord.isEmpty()) noInputCounts[Suggestion.TYPE_ALIAS] else counts[Suggestion.TYPE_ALIAS]
        for (a in aliasManager.getAliases(true)) {
            if (lastWord.isEmpty() || a.name.startsWith(lastWord)) {
                if (canInsert == 0) return
                canInsert--
                suggestions.add(Suggestion(Tuils.EMPTYSTRING, a.name, clickToLaunch && !a.isParametrized, Suggestion.TYPE_ALIAS))
            }
        }
    }

    private fun suggestParams(pack: MainPack, suggestions: MutableList<Suggestion>, cmd: ParamCommand, beforeLastSpace: String, lastWord: String?) {
        val params = cmd.params() ?: return
        for (s in params) {
            val p = cmd.getParam(pack, s).value ?: continue
            if (lastWord == null || s.startsWith(lastWord) || s.replace("-", "").startsWith(lastWord)) {
                suggestions.add(Suggestion(beforeLastSpace, s, p.args().isEmpty() && clickToLaunch, 0))
            }
        }
    }

    private fun suggestArgs(info: MainPack, type: Int, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        when (type) {
            CommandAbstraction.FILE -> suggestFile(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.VISIBLE_PACKAGE -> suggestApp(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.COMMAND -> suggestCommand(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.CONTACTNUMBER -> suggestContact(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.SONG -> suggestSong(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.BOOLEAN -> suggestBoolean(suggestions, beforeLastSpace)
            CommandAbstraction.HIDDEN_PACKAGE -> suggestHiddenApp(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.COLOR -> suggestColor(suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.CONFIG_ENTRY -> suggestConfigEntry(suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.CONFIG_FILE -> suggestConfigFile(suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.DEFAULT_APP -> suggestDefaultApp(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.ALL_PACKAGES -> suggestAllPackages(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.APP_GROUP -> suggestAppGroup(info, suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.APP_INSIDE_GROUP -> suggestAppInsideGroup(info, suggestions, afterLastSpace, beforeLastSpace, true)
            CommandAbstraction.BOUND_REPLY_APP -> suggestBoundReplyApp(suggestions, afterLastSpace, beforeLastSpace)
            CommandAbstraction.DATASTORE_PATH_TYPE -> suggestDataStoreType(suggestions, beforeLastSpace)
        }
    }

    private fun suggestArgs(info: MainPack, type: Int, suggestions: MutableList<Suggestion>, beforeLastSpace: String) {
        suggestArgs(info, type, suggestions, null, beforeLastSpace)
    }

    private fun suggestBoolean(suggestions: MutableList<Suggestion>, beforeLastSpace: String) {
        suggestions.add(Suggestion(beforeLastSpace, "true", clickToLaunch, Suggestion.TYPE_BOOLEAN))
        suggestions.add(Suggestion(beforeLastSpace, "false", clickToLaunch, Suggestion.TYPE_BOOLEAN))
    }

    private fun suggestFile(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        val noAfter = afterLastSpace.isNullOrEmpty()
        if (noAfter || !afterLastSpace!!.endsWith(File.separator)) {
            suggestions.add(Suggestion(beforeLastSpace, File.separator, false, Suggestion.TYPE_FILE, afterLastSpace))
        }
        if (Suggestion.appendQuotesBeforeFile && !noAfter && !afterLastSpace!!.endsWith("'") && !afterLastSpace!!.endsWith("\"")) {
            suggestions.add(Suggestion(beforeLastSpace, "'", false, Suggestion.TYPE_FILE, afterLastSpace))
        }

        if (noAfter) {
            suggestFilesInDir(null, suggestions, info.currentDirectory, beforeLastSpace)
            return
        }

        if (!afterLastSpace!!.contains(File.separator)) {
            suggestFilesInDir(suggestions, info.currentDirectory, afterLastSpace, beforeLastSpace, null)
        } else {
            if (afterLastSpace!!.endsWith(File.separator)) {
                val total = "$beforeLastSpace ${Tuils.SPACE} $afterLastSpace"
                val qCount = total.count { it == '\'' || it == '\"' }
                if (qCount > 0) {
                    val lastQ = maxOf(total.lastIndexOf('\''), total.lastIndexOf('\"'))
                    val file = total.substring(lastQ + (if (qCount % 2 == 0) 1 else 2))
                    val dirInfo = FileManager.cd(info.currentDirectory, file)
                    suggestFilesInDir(afterLastSpace, suggestions, dirInfo.file, beforeLastSpace)
                } else {
                    val dirInfo = FileManager.cd(info.currentDirectory, afterLastSpace!!.dropLast(1))
                    suggestFilesInDir("${afterLastSpace}${File.separator}", suggestions, dirInfo.file, beforeLastSpace)
                }
            } else {
                val originalALS = afterLastSpace!!
                val cleanALS = rmQuotes.matcher(originalALS).replaceAll(Tuils.EMPTYSTRING)
                val index = cleanALS.lastIndexOf(File.separator)
                val dirInfo = FileManager.cd(info.currentDirectory, cleanALS.substring(0, index))
                val originalIndex = originalALS.lastIndexOf(File.separator)
                val alsals = originalALS.substring(0, originalIndex + 1)
                val als = originalALS.substring(originalIndex + 1)
                suggestFilesInDir(suggestions, dirInfo.file, als, beforeLastSpace, alsals)
            }
        }
    }

    private fun suggestFilesInDir(suggestions: MutableList<Suggestion>, dir: File?, afterLastSeparator: String, beforeLastSpace: String, afterLastSpaceWithoutALS: String?) {
        if (dir == null || !dir.isDirectory) return
        val files = dir.list() ?: return
        val temp = rmQuotes.matcher(afterLastSeparator).replaceAll("")
        val counter = quickCompare(temp, files, suggestions, beforeLastSpace, suggestionsPerCategory, false, Suggestion.TYPE_FILE, false)
        if (suggestionsPerCategory - counter <= 0) return
        val fs = CompareStrings.topMatchesWithDeadline(temp, files, suggestionsPerCategory - counter, suggestionsDeadline, FILE_SPLITTERS, algInstance, alg)
        for (f in fs) suggestions.add(Suggestion(beforeLastSpace, f, false, Suggestion.TYPE_FILE, afterLastSpaceWithoutALS))
    }

    private fun quickCompare(s1: String, ss: Array<String>, suggestions: MutableList<Suggestion>, before: String, max: Int, exec: Boolean, type: Int, tag: Any?): Int {
        if (s1.length > quickCompare) return 0
        var counter = 0
        for (i in ss.indices) {
            if (counter >= max) break
            if (ss[i].lowercase().startsWith(s1.lowercase())) {
                suggestions.add(Suggestion(before, ss[i], exec, type, if (tag is Boolean) (if (tag) ss[i] else null) else tag))
                ss[i] = ""
                counter++
            }
        }
        return counter
    }

    private fun quickCompare(s1: String, ss: MutableList<out StringableObject>, suggestions: MutableList<Suggestion>, before: String, max: Int, exec: Boolean, type: Int, tag: Any?): Int {
        if (s1.length > quickCompare) return 0
        var counter = 0
        val it = ss.iterator()
        while (it.hasNext()) {
            if (counter >= max) break
            val o = it.next()
            if (o.getLowercaseString().startsWith(s1.lowercase())) {
                suggestions.add(Suggestion(before, o.getString(), exec, type, if (tag is Boolean) (if (tag) o else null) else tag))
                it.remove()
                counter++
            }
        }
        return counter
    }

    private fun suggestFilesInDir(afterLastSpaceHolder: String?, suggestions: MutableList<Suggestion>, dir: File?, beforeLastSpace: String) {
        if (dir == null || !dir.isDirectory) return
        dir.list()?.sorted()?.forEach { suggestions.add(Suggestion(beforeLastSpace, it, false, Suggestion.TYPE_FILE, afterLastSpaceHolder)) }
    }

    private fun suggestContact(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        val contacts = info.contacts?.getContacts() ?: return
        if (contacts.isEmpty()) return
        if (afterLastSpace.isNullOrEmpty()) {
            for (c in contacts) suggestions.add(Suggestion(beforeLastSpace, c.name, true, Suggestion.TYPE_CONTACT, c))
        } else {
            val counter = quickCompare(afterLastSpace, ArrayList(contacts), suggestions, beforeLastSpace, suggestionsPerCategory, true, Suggestion.TYPE_CONTACT, true)
            if (suggestionsPerCategory - counter <= 0) return
            val cts = CompareObjects.topMatchesWithDeadline(ContactManager.Contact::class.java, afterLastSpace, contacts.size, contacts, suggestionsPerCategory - counter, suggestionsDeadline, SPLITTERS, algInstance, alg)
            for (c in cts) {
                if (c == null) break
                suggestions.add(Suggestion(beforeLastSpace, c.name, clickToLaunch, Suggestion.TYPE_CONTACT, c))
            }
        }
    }

    private fun suggestDataStoreType(suggestions: MutableList<Suggestion>, beforeLastSpace: String) {
        suggestions.add(Suggestion(beforeLastSpace, "json", false, Suggestion.TYPE_BOOLEAN))
        suggestions.add(Suggestion(beforeLastSpace, "xpath", false, Suggestion.TYPE_BOOLEAN))
        suggestions.add(Suggestion(beforeLastSpace, "format", false, Suggestion.TYPE_BOOLEAN))
    }

    private fun suggestSong(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        val songs = info.player?.songs ?: return
        if (songs.isEmpty()) return
        if (afterLastSpace.isNullOrEmpty()) {
            for (s in songs) suggestions.add(Suggestion(beforeLastSpace, s.title, clickToLaunch, Suggestion.TYPE_SONG))
        } else {
            val counter = quickCompare(afterLastSpace, ArrayList(songs), suggestions, beforeLastSpace, suggestionsPerCategory, clickToLaunch, Suggestion.TYPE_SONG, false)
            if (suggestionsPerCategory - counter <= 0) return
            val ss = CompareObjects.topMatchesWithDeadline(Song::class.java, afterLastSpace, songs.size, songs, suggestionsPerCategory - counter, suggestionsDeadline, SPLITTERS, algInstance, alg)
            for (s in ss) {
                if (s == null) break
                suggestions.add(Suggestion(beforeLastSpace, s.title, clickToLaunch, Suggestion.TYPE_SONG))
            }
        }
    }

    private fun suggestCommand(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        if (afterLastSpace.isNullOrEmpty()) {
            suggestCommand(info, suggestions, beforeLastSpace)
            return
        }
        if (afterLastSpace.length <= 6) {
            val term = afterLastSpace.lowercase().trim()
            val cmds = info.commandGroup?.commandNames ?: return
            var canInsert = counts[Suggestion.TYPE_COMMAND]
            for (s in cmds) {
                if (canInsert == 0 || Thread.currentThread().isInterrupted) return
                if (s.startsWith(term)) {
                    val cmd = info.commandGroup?.getCommandByName(s) ?: continue
                    val exec = cmd.argType() == null || cmd.argType().isEmpty()
                    suggestions.add(Suggestion(beforeLastSpace, s, exec && clickToLaunch, Suggestion.TYPE_COMMAND))
                    canInsert--
                }
            }
        }
    }

    private fun suggestCommand(info: MainPack, suggestions: MutableList<Suggestion>, beforeLastSpace: String) {
        val cmds = info.commandGroup?.commands ?: return
        var canInsert = if (beforeLastSpace.isNotEmpty()) Int.MAX_VALUE else noInputCounts[Suggestion.TYPE_COMMAND]
        for (cmd in cmds) {
            if (canInsert == 0 || Thread.currentThread().isInterrupted) return
            if (info.cmdPrefs.getPriority(cmd) >= minCmdPriority) {
                val exec = cmd.argType() == null || cmd.argType().isEmpty()
                suggestions.add(Suggestion(beforeLastSpace, cmd::class.java.simpleName, exec && clickToLaunch, Suggestion.TYPE_COMMAND))
                canInsert--
            }
        }
    }

    private fun suggestColor(suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        if (afterLastSpace.isNullOrEmpty() || (afterLastSpace.length == 1 && afterLastSpace[0] != '#')) {
            suggestions.add(Suggestion(beforeLastSpace, "#", false, Suggestion.TYPE_COLOR))
        }
    }

    private fun suggestApp(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        suggestApp(info.appsManager.shownApps(), suggestions, afterLastSpace, beforeLastSpace, true)
    }

    private fun suggestHiddenApp(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        suggestApp(info.appsManager.hiddenApps(), suggestions, afterLastSpace, beforeLastSpace, false)
    }

    private fun suggestAllPackages(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        val apps = ArrayList(info.appsManager.shownApps()).apply { addAll(info.appsManager.hiddenApps()) }
        suggestApp(apps, suggestions, afterLastSpace, beforeLastSpace, true)
    }

    private fun suggestDefaultApp(info: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        suggestions.add(Suggestion(beforeLastSpace, "most_used", false, Suggestion.TYPE_PERMANENT))
        suggestions.add(Suggestion(beforeLastSpace, "null", false, Suggestion.TYPE_PERMANENT))
        suggestApp(info.appsManager.shownApps(), suggestions, afterLastSpace, beforeLastSpace, true)
    }

    private fun suggestApp(apps: List<AppsManager.LaunchInfo>, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String, canClick: Boolean) {
        if (apps.isEmpty()) return
        val list = ArrayList(apps)
        var canInsert = counts[Suggestion.TYPE_APP]
        if (afterLastSpace.isNullOrEmpty()) {
            for (l in list) {
                if (canInsert == 0) return
                canInsert--
                suggestions.add(Suggestion(beforeLastSpace, l.publicLabel ?: "", canClick && clickToLaunch, Suggestion.TYPE_APP, l))
            }
        } else {
            val counter = quickCompare(afterLastSpace, list, suggestions, beforeLastSpace, canInsert, canClick && clickToLaunch, Suggestion.TYPE_APP, canClick && clickToLaunch)
            if (canInsert - counter <= 0) return
            val infos = CompareObjects.topMatchesWithDeadline(AppsManager.LaunchInfo::class.java, afterLastSpace, list.size, list, canInsert - counter, suggestionsDeadline, SPLITTERS, algInstance, alg)
            for (i in infos) {
                if (i == null) break
                if (canInsert == 0) return
                canInsert--
                suggestions.add(Suggestion(beforeLastSpace, i.publicLabel ?: "", canClick && clickToLaunch, Suggestion.TYPE_APP, if (canClick && clickToLaunch) i else null))
            }
        }
    }

    private fun suggestConfigEntry(suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        if (CommandTuils.xmlPrefsEntrys == null) {
            CommandTuils.xmlPrefsEntrys = ArrayList()
            for (root in XMLPrefsManager.XMLPrefsRoot.values()) CommandTuils.xmlPrefsEntrys!!.addAll(root.enums)
            Collections.addAll(CommandTuils.xmlPrefsEntrys!!, *Apps.values())
            Collections.addAll(CommandTuils.xmlPrefsEntrys!!, *Notifications.values())
            Collections.addAll(CommandTuils.xmlPrefsEntrys!!, *Rss.values())
            Collections.addAll(CommandTuils.xmlPrefsEntrys!!, *Reply.values())
        }
        val list = ArrayList(CommandTuils.xmlPrefsEntrys!!)
        if (afterLastSpace.isNullOrEmpty()) {
            for (s in list) suggestions.add(Suggestion(beforeLastSpace, s.label(), false, Suggestion.TYPE_COMMAND))
        } else {
            val counter = quickCompare(afterLastSpace, list, suggestions, beforeLastSpace, suggestionsPerCategory, false, Suggestion.TYPE_COMMAND, false)
            if (suggestionsPerCategory - counter <= 0) return
            val saves = CompareObjects.topMatchesWithDeadline(XMLPrefsSave::class.java, afterLastSpace, list.size, list, suggestionsPerCategory - counter, suggestionsDeadline, XML_PREFS_SPLITTERS, algInstance, alg)
            for (s in saves) suggestions.add(Suggestion(beforeLastSpace, s.label(), false, Suggestion.TYPE_COMMAND))
        }
    }

    private fun suggestConfigFile(suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        if (CommandTuils.xmlPrefsFiles == null) {
            CommandTuils.xmlPrefsFiles = ArrayList<String>().apply {
                for (root in XMLPrefsManager.XMLPrefsRoot.values()) add(root.path)
                add(AppsManager.PATH)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) add(ReplyManager.PATH)
                add(NotificationManager.PATH)
                add(RssManager.PATH)
            }
        }
        val files = CommandTuils.xmlPrefsFiles!!
        if (afterLastSpace.isNullOrEmpty()) {
            for (s in files) suggestions.add(Suggestion(beforeLastSpace, s, false, Suggestion.TYPE_CONFIGFILE, afterLastSpace))
        } else if (afterLastSpace.length <= 6) {
            val term = afterLastSpace.trim().lowercase()
            for (s in files) {
                if (Thread.currentThread().isInterrupted) return
                if (s.lowercase().startsWith(term)) suggestions.add(Suggestion(beforeLastSpace, s, false, Suggestion.TYPE_CONFIGFILE, afterLastSpace))
            }
        }
    }

    private fun suggestAppGroup(pack: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String) {
        val groups = ArrayList(pack.appsManager.groups)
        if (groups.isEmpty()) return
        var canInsert = if (afterLastSpace.isNullOrEmpty()) noInputCounts[Suggestion.TYPE_APPGP] else counts[Suggestion.TYPE_APPGP]
        if (afterLastSpace.isNullOrEmpty()) {
            for (g in groups) {
                if (canInsert == 0) return
                canInsert--
                suggestions.add(Suggestion(beforeLastSpace, g.name(), false, Suggestion.TYPE_APPGP, g))
            }
        } else {
            val counter = quickCompare(afterLastSpace, groups, suggestions, beforeLastSpace, canInsert, false, Suggestion.TYPE_APPGP, true)
            if (canInsert - counter <= 0) return
            val gps = CompareObjects.topMatchesWithDeadline(AppsManager.Group::class.java, afterLastSpace, groups.size, groups, canInsert, suggestionsDeadline, SPLITTERS, algInstance, alg)
            for (g in gps) {
                if (g == null) break
                suggestions.add(Suggestion(beforeLastSpace, g.name(), false, Suggestion.TYPE_APPGP, g))
            }
        }
    }

    private fun suggestAppInsideGroup(pack: MainPack, suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String, keepGroupName: Boolean): Boolean {
        var bls = beforeLastSpace
        var index = -1
        var app = ""

        if (!bls.contains(Tuils.SPACE)) {
            index = Tuils.find(bls, pack.appsManager.groups)
            app = afterLastSpace ?: ""
            if (!keepGroupName) bls = ""
        } else {
            val split = bls.split(Tuils.SPACE)
            for (count in split.indices) {
                index = Tuils.find(split[count], pack.appsManager.groups)
                if (index != -1) {
                    bls = ""
                    for (i in 0 until if (keepGroupName) count + 1 else count) bls += split[i] + Tuils.SPACE
                    bls = bls.trim()
                    var j = count + 1
                    while (j < split.size) {
                        app += split[j] + Tuils.SPACE
                        j++
                    }
                    if (afterLastSpace != null) app += Tuils.SPACE + afterLastSpace
                    app = app.trim()
                    break
                }
            }
        }

        if (index == -1) return false
        val g = pack.appsManager.groups[index]
        val apps = ArrayList(g.members() as List<AppsManager.Group.GroupLaunchInfo>)
        if (apps.isNotEmpty()) {
            if (app.isEmpty()) {
                for (o in apps) suggestions.add(Suggestion(bls, o.publicLabel ?: "", clickToLaunch, Suggestion.TYPE_APP, o))
            } else {
                val counter = quickCompare(app, apps, suggestions, bls, Int.MAX_VALUE, clickToLaunch, Suggestion.TYPE_APP, true)
                if (counter == apps.size) return true
                val infos = CompareObjects.topMatchesWithDeadline(AppsManager.Group.GroupLaunchInfo::class.java, app, apps.size, apps, apps.size, suggestionsDeadline, SPLITTERS, algInstance, alg)
                for (gli in infos) {
                    if (gli == null) break
                    suggestions.add(Suggestion(bls, gli.publicLabel ?: "", clickToLaunch, Suggestion.TYPE_APP, gli))
                }
            }
        }
        return true
    }

    fun showVineMenu(suggestions: List<Suggestion>) {
        suggestionsView.removeAllViews()
        currentVineView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        val hsv = suggestionsView.parent.parent as View
        val parent = hsv.parent as ViewGroup
        currentVineView = VineMenuHelper.createVineMenu(Tuils.getContext(pack), suggestions, this)
        if (parent is RelativeLayout) {
            val lp = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val toolsId = Tuils.getContext(pack).resources.getIdentifier("tools_view", "id", Tuils.getContext(pack).packageName)
            val inputGroupId = Tuils.getContext(pack).resources.getIdentifier("input_group", "id", Tuils.getContext(pack).packageName)
            if (toolsId != 0) lp.addRule(RelativeLayout.ABOVE, toolsId)
            else if (inputGroupId != 0) lp.addRule(RelativeLayout.ABOVE, inputGroupId)
            currentVineView!!.layoutParams = lp
            parent.addView(currentVineView)
        } else if (parent is LinearLayout) {
            parent.addView(currentVineView, 1)
        } else {
            parent.addView(currentVineView)
        }
    }

    private fun suggestBoundReplyApp(suggestions: MutableList<Suggestion>, afterLastSpace: String?, beforeLastSpace: String): Boolean {
        val apps = ArrayList(ReplyManager.boundApps)
        if (apps.isEmpty()) return false
        if (afterLastSpace.isNullOrEmpty()) {
            for (b in apps) suggestions.add(Suggestion(beforeLastSpace, b.label ?: "", false, Suggestion.TYPE_APP))
        } else {
            val counter = quickCompare(afterLastSpace, ArrayList(apps.map { object : StringableObject {
                override fun getLowercaseString() = it.label.lowercase()
                override fun getString() = it.label
            } }), suggestions, beforeLastSpace, suggestionsPerCategory, false, Suggestion.TYPE_APP, false)
            if (suggestionsPerCategory - counter <= 0) return true
            val b = CompareObjects.topMatchesWithDeadline(BoundApp::class.java, afterLastSpace, apps.size, apps, suggestionsPerCategory - counter, suggestionsDeadline, SPLITTERS, algInstance, alg)
            for (ba in b) {
                if (ba == null) break
                suggestions.add(Suggestion(beforeLastSpace, ba.label ?: "", false, Suggestion.TYPE_APP))
            }
        }
        return true
    }

    class Suggestion(@JvmField val textBefore: String?, @JvmField val text: String, @JvmField val exec: Boolean, @JvmField val type: Int, @JvmField val `object`: Any? = null) {
        companion object {
            const val TYPE_APP = 0
            const val TYPE_ALIAS = 1
            const val TYPE_COMMAND = 2
            const val TYPE_APPGP = 3
            const val TYPE_FILE = 10
            const val TYPE_BOOLEAN = 11
            const val TYPE_SONG = 12
            const val TYPE_CONTACT = 13
            const val TYPE_COLOR = 14
            const val TYPE_PERMANENT = 15
            const val TYPE_CONFIGFILE = 16
            const val TYPE_MENU_OPTION = 20
            var appendQuotesBeforeFile: Boolean = false
        }

        fun getText(): String {
            return when (type) {
                TYPE_CONTACT -> {
                    val c = `object` as ContactManager.Contact
                    if (c.numbers.size <= c.selectedNumber) c.selectedNumber = 0
                    "$textBefore ${Tuils.SPACE} ${c.numbers[c.selectedNumber]}"
                }
                TYPE_PERMANENT -> text
                TYPE_FILE -> {
                    val lastWord = `object` as? String ?: ""
                    val textIsSpecial = text == File.separator || text == "\"" || text == "'"
                    val appendLastWord = lastWord.endsWith(File.separator) || textIsSpecial
                    "$textBefore ${Tuils.SPACE} ${if (appendLastWord) lastWord else ""}${if (appendQuotesBeforeFile && !appendLastWord) "'" else ""}$text"
                }
                else -> if (textBefore.isNullOrEmpty()) text else "$textBefore ${Tuils.SPACE} $text"
            }
        }

        override fun toString(): String = text
    }

    private inner class CustomComparator(val noInputIndexes: IntArray, val inputIndexes: IntArray) : Comparator<Suggestion> {
        var noInput: Boolean = false
        override fun compare(o1: Suggestion, o2: Suggestion): Int {
            if (o1.type == o2.type) return 0
            return if (noInput) noInputIndexes[o1.type] - noInputIndexes[o2.type]
            else {
                if (o1.type < inputIndexes.size) {
                    if (o2.type < inputIndexes.size) inputIndexes[o1.type] - inputIndexes[o2.type]
                    else -1
                } else {
                    if (o2.type < inputIndexes.size) 1
                    else 0
                }
            }
        }
    }
}
