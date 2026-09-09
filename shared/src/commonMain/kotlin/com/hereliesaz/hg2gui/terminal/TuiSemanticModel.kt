package com.hereliesaz.hg2gui.terminal

/**
 * A toolkit-neutral description of a terminal UI screen. The terminal emulator remains the
 * source of truth; this model only captures enough structure for HG2Gui to render a native
 * wrapper around an already-running interactive program.
 */
data class TuiSnapshot(
    val title: String?,
    val layers: List<TuiLayer>,
    val prompt: TuiPrompt?,
    val alternateScreen: Boolean
) {
    val isWrappable: Boolean
        get() = alternateScreen && (layers.any { it.items.size >= 2 } || prompt != null)

    /** The topmost selectable layer owns navigation; lower layers remain visible context. */
    val activeLayerIndex: Int?
        get() = layers.indexOfLast { it.activeIndex != null }.takeIf { it >= 0 }
}

data class TuiLayer(
    val heading: String?,
    val items: List<TuiMenuItem>,
    val activeIndex: Int?,
    val depth: Int = 0,
    val modal: Boolean = false
)

data class TuiMenuItem(
    val row: Int,
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true
)

data class TuiPrompt(
    val row: Int,
    val label: String,
    val kind: TuiPromptKind
)

enum class TuiPromptKind {
    TEXT,
    PASSWORD,
    CONFIRMATION,
    UNKNOWN
}

/**
 * Input to [TuiSemanticParser]. Android's terminal adapter converts emulator rows into these
 * neutral rows so the parser can be tested without a PTY or Android runtime.
 */
data class TuiRow(
    val index: Int,
    val text: String,
    val highlighted: Boolean = false,
    val cursorColumn: Int? = null
)

/**
 * Semantic parser for the layered-menu TUIs commonly produced by curses, Rich, Textual, Ink and
 * similar toolkits. It deliberately favors conservative recognition: an uncertain screen stays on
 * the raw terminal fallback rather than inventing controls.
 */
object TuiSemanticParser {
    private val menuPrefix = Regex("""^\s*(?:[>›»▶►•●○◉✓✔*+-]|\[[ xX✓✔]\]|\([ xX*]\))\s+(.+?)\s*$""")
    private val numberedMenu = Regex("""^\s*(?:\d+[.)]|[A-Za-z][.)])\s+(.+?)\s*$""")
    private val plainIndentedItem = Regex("""^\s{2,}\S.+$""")
    private val promptSuffix = Regex("""(?i)(?:[:>]\s*|\?\s*|\[[yYnN/]+]\s*)$""")
    private val passwordWords = Regex("""(?i)\b(password|passphrase|token|secret|api\s*key)\b""")
    private val confirmWords = Regex("""(?i)\b(confirm|continue|proceed|overwrite|delete|remove|install|allow|accept)\b""")
    private val modalWords = Regex("""(?i)\b(dialog|confirm|confirmation|warning|error|choose|select|options?)\b""")

    fun parse(rows: List<TuiRow>, alternateScreen: Boolean): TuiSnapshot {
        val visible = rows
            .map { it.copy(text = it.text.trimEnd()) }
            .filter { it.text.isNotBlank() }

        val menuRuns = mutableListOf<List<TuiRow>>()
        var current = mutableListOf<TuiRow>()
        for (row in visible) {
            val explicit = looksLikeExplicitMenuItem(row)
            val continuation = current.isNotEmpty() &&
                row.index == current.last().index + 1 &&
                plainIndentedItem.matches(row.text)
            if (explicit || continuation) {
                if (current.isNotEmpty() && row.index != current.last().index + 1) {
                    if (current.size >= 2) menuRuns += current.toList()
                    current = mutableListOf()
                }
                current += row
            } else if (current.isNotEmpty()) {
                if (current.size >= 2) menuRuns += current.toList()
                current = mutableListOf()
            }
        }
        if (current.size >= 2) menuRuns += current.toList()

        val runIndents = menuRuns.map { run -> run.minOf(::leadingIndent) }
        val indentLevels = runIndents.distinct().sorted()

        val layers = menuRuns.mapIndexed { runIndex, run ->
            val firstRow = run.first().index
            val heading = visible
                .lastOrNull { it.index < firstRow && firstRow - it.index <= 2 && !looksLikeExplicitMenuItem(it) }
                ?.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val items = run.map { row ->
                TuiMenuItem(
                    row = row.index,
                    label = menuLabel(row.text),
                    selected = row.highlighted || row.cursorColumn != null
                )
            }
            val depth = indentLevels.indexOf(runIndents[runIndex]).coerceAtLeast(0)
            TuiLayer(
                heading = heading,
                items = items,
                activeIndex = items.indexOfFirst { it.selected }.takeIf { it >= 0 },
                depth = depth,
                modal = depth > 0 || (heading != null && modalWords.containsMatchIn(heading))
            )
        }

        val prompt = visible
            .lastOrNull { looksLikePrompt(it) && layers.none { layer -> layer.items.any { item -> item.row == it.index } } }
            ?.let { row ->
                TuiPrompt(row.index, row.text.trim(), promptKind(row.text))
            }

        val title = visible.firstOrNull()
            ?.takeIf { row -> layers.none { layer -> layer.items.any { it.row == row.index } } }
            ?.text
            ?.trim()
            ?.takeIf { it.length in 1..80 }

        return TuiSnapshot(
            title = title,
            layers = layers,
            prompt = prompt,
            alternateScreen = alternateScreen
        )
    }

    private fun leadingIndent(row: TuiRow): Int = row.text.takeWhile(Char::isWhitespace).length

    private fun looksLikeExplicitMenuItem(row: TuiRow): Boolean =
        row.highlighted || menuPrefix.matches(row.text) || numberedMenu.matches(row.text)

    private fun menuLabel(text: String): String =
        menuPrefix.matchEntire(text)?.groupValues?.getOrNull(1)
            ?: numberedMenu.matchEntire(text)?.groupValues?.getOrNull(1)
            ?: text.trim()

    private fun looksLikePrompt(row: TuiRow): Boolean =
        row.cursorColumn != null && (promptSuffix.containsMatchIn(row.text) || row.text.length <= 100)

    private fun promptKind(text: String): TuiPromptKind = when {
        passwordWords.containsMatchIn(text) -> TuiPromptKind.PASSWORD
        confirmWords.containsMatchIn(text) || Regex("""(?i)\[[yYnN/]+]""").containsMatchIn(text) ->
            TuiPromptKind.CONFIRMATION
        promptSuffix.containsMatchIn(text) -> TuiPromptKind.TEXT
        else -> TuiPromptKind.UNKNOWN
    }
}
