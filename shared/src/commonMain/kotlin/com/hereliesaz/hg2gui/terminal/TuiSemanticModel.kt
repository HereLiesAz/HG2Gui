package com.hereliesaz.hg2gui.terminal

/** Toolkit-neutral description of a terminal UI screen. */
data class TuiSnapshot(
    val title: String?,
    val layers: List<TuiLayer>,
    val prompt: TuiPrompt?,
    val regions: List<TuiRegion> = emptyList(),
    val tabs: List<TuiTab> = emptyList(),
    val alternateScreen: Boolean,
    val mouseAware: Boolean = false
) {
    val isWrappable: Boolean
        get() = alternateScreen && (
            layers.any { it.items.size >= 2 } || prompt != null || regions.isNotEmpty() || tabs.isNotEmpty()
        )

    val activeLayerIndex: Int?
        get() = layers.indexOfLast { it.activeIndex != null }.takeIf { it >= 0 }
}

data class TuiLayer(
    val heading: String?,
    val items: List<TuiMenuItem>,
    val activeIndex: Int?,
    val depth: Int = 0,
    val modal: Boolean = false,
    val scrollable: Boolean = false
)

data class TuiMenuItem(
    val row: Int,
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true,
    val control: TuiControlKind = TuiControlKind.ACTION,
    val checked: Boolean? = null
)

enum class TuiControlKind { ACTION, CHECKBOX, RADIO }

data class TuiPrompt(
    val row: Int,
    val label: String,
    val kind: TuiPromptKind
)

enum class TuiPromptKind { TEXT, PASSWORD, CONFIRMATION, UNKNOWN }

data class TuiTab(val label: String, val active: Boolean, val row: Int)

data class TuiRegion(
    val kind: TuiRegionKind,
    val startRow: Int,
    val endRow: Int,
    val lines: List<String>,
    val progress: Float? = null
)

enum class TuiRegionKind { TABLE, RESULTS, PROGRESS, STATUS, PANE }

/** Neutral terminal row supplied by the emulator adapter. */
data class TuiRow(
    val index: Int,
    val text: String,
    val highlighted: Boolean = false,
    val emphasized: Boolean = false,
    val styled: Boolean = false,
    val cursorColumn: Int? = null
)

/** Conservative semantic parser for curses/Rich/Textual/Ink/ratatui-style screens. */
object TuiSemanticParser {
    private val menuPrefix = Regex("""^\s*(?:[>›»▶►•●○◉✓✔*+-]|\[[ xX✓✔]\]|\([ xX*]\))\s+(.+?)\s*$""")
    private val numberedMenu = Regex("""^\s*(?:\d+[.)]|[A-Za-z][.)])\s+(.+?)\s*$""")
    private val checkbox = Regex("""^\s*\[([ xX✓✔])\]\s+(.+?)\s*$""")
    private val radio = Regex("""^\s*\(([ xX*●○◉])\)\s+(.+?)\s*$""")
    private val plainIndentedItem = Regex("""^\s{2,}\S.+$""")
    private val promptSuffix = Regex("""(?i)(?:[:>]\s*|\?\s*|\[[yYnN/]+]\s*)$""")
    private val passwordWords = Regex("""(?i)\b(password|passphrase|token|secret|api\s*key)\b""")
    private val confirmWords = Regex("""(?i)\b(confirm|continue|proceed|overwrite|delete|remove|install|allow|accept)\b""")
    private val modalWords = Regex("""(?i)\b(dialog|confirm|confirmation|warning|error|choose|select|options?)\b""")
    private val tabLine = Regex("""(?:^|\s)(?:\[([^]]+)]|([^|│]+))(?:\s*[|│]\s*|$)""")
    private val progressPercent = Regex("""\b(\d{1,3})%\b""")
    private val tableSeparator = Regex("""^\s*[+┌├└│|].*(?:[-─]{2,}|[+┬┼┴]).*$""")
    private val resultPrefix = Regex("""^\s*(?:[-*•]|\d+[.)])\s+\S""")
    private val paneSeparator = Regex("""\s[│|]\s""")

    fun parse(rows: List<TuiRow>, alternateScreen: Boolean, mouseAware: Boolean = false): TuiSnapshot {
        val visible = rows.map { it.copy(text = it.text.trimEnd()) }.filter { it.text.isNotBlank() }
        val menuRuns = menuRuns(visible)
        val runIndents = menuRuns.map { run -> run.minOf(::leadingIndent) }
        val indentLevels = runIndents.distinct().sorted()

        val layers = menuRuns.mapIndexed { runIndex, run ->
            val firstRow = run.first().index
            val heading = visible.lastOrNull {
                it.index < firstRow && firstRow - it.index <= 2 && !looksLikeExplicitMenuItem(it)
            }?.text?.trim()?.takeIf(String::isNotEmpty)
            val items = run.map { row -> menuItem(row) }
            val depth = indentLevels.indexOf(runIndents[runIndex]).coerceAtLeast(0)
            TuiLayer(
                heading = heading,
                items = items,
                activeIndex = items.indexOfFirst { it.selected }.takeIf { it >= 0 },
                depth = depth,
                modal = depth > 0 || (heading != null && modalWords.containsMatchIn(heading)),
                scrollable = looksScrollable(run, visible)
            )
        }

        val menuRows = layers.flatMap { layer -> layer.items.map { it.row } }.toSet()
        val prompt = visible.lastOrNull { looksLikePrompt(it) && it.index !in menuRows }
            ?.let { row -> TuiPrompt(row.index, row.text.trim(), promptKind(row.text)) }

        val tabs = detectTabs(visible, menuRows)
        val regions = detectRegions(visible, menuRows, tabs.map { it.row }.toSet(), prompt?.row)
        val title = visible.firstOrNull { it.index !in menuRows }
            ?.text?.trim()?.takeIf { it.length in 1..80 }

        return TuiSnapshot(
            title = title,
            layers = layers,
            prompt = prompt,
            regions = regions,
            tabs = tabs,
            alternateScreen = alternateScreen,
            mouseAware = mouseAware
        )
    }

    private fun menuRuns(visible: List<TuiRow>): List<List<TuiRow>> {
        val runs = mutableListOf<List<TuiRow>>()
        var current = mutableListOf<TuiRow>()
        for (row in visible) {
            val explicit = looksLikeExplicitMenuItem(row)
            val continuation = current.isNotEmpty() && row.index == current.last().index + 1 && plainIndentedItem.matches(row.text)
            if (explicit || continuation) {
                if (current.isNotEmpty() && row.index != current.last().index + 1) {
                    if (current.size >= 2) runs += current.toList()
                    current = mutableListOf()
                }
                current += row
            } else if (current.isNotEmpty()) {
                if (current.size >= 2) runs += current.toList()
                current = mutableListOf()
            }
        }
        if (current.size >= 2) runs += current.toList()
        return runs
    }

    private fun menuItem(row: TuiRow): TuiMenuItem {
        val cb = checkbox.matchEntire(row.text)
        val rb = radio.matchEntire(row.text)
        return TuiMenuItem(
            row = row.index,
            label = when {
                cb != null -> cb.groupValues[2]
                rb != null -> rb.groupValues[2]
                else -> menuLabel(row.text)
            },
            selected = row.highlighted || row.emphasized || row.cursorColumn != null,
            control = when {
                cb != null -> TuiControlKind.CHECKBOX
                rb != null -> TuiControlKind.RADIO
                else -> TuiControlKind.ACTION
            },
            checked = when {
                cb != null -> cb.groupValues[1].isNotBlank() && cb.groupValues[1] != " "
                rb != null -> rb.groupValues[1] !in listOf("", " ", "○")
                else -> null
            }
        )
    }

    private fun detectTabs(visible: List<TuiRow>, menuRows: Set<Int>): List<TuiTab> {
        val candidate = visible.firstOrNull { row ->
            row.index !in menuRows && (row.text.count { it == '|' || it == '│' } >= 1 || row.text.count { it == '[' } >= 2)
        } ?: return emptyList()
        val matches = tabLine.findAll(candidate.text).mapNotNull { match ->
            val raw = (match.groupValues[1].ifBlank { match.groupValues[2] }).trim()
            raw.takeIf { it.length in 1..30 }
        }.toList().distinct()
        if (matches.size < 2) return emptyList()
        return matches.map { label ->
            val bracketed = "[$label]" in candidate.text
            TuiTab(label, bracketed || (candidate.highlighted && candidate.text.contains(label)), candidate.index)
        }
    }

    private fun detectRegions(
        visible: List<TuiRow>,
        menuRows: Set<Int>,
        tabRows: Set<Int>,
        promptRow: Int?
    ): List<TuiRegion> {
        val excluded = menuRows + tabRows + listOfNotNull(promptRow)
        val rows = visible.filter { it.index !in excluded }
        val result = mutableListOf<TuiRegion>()

        rows.forEach { row ->
            progressPercent.find(row.text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)?.let { percent ->
                result += TuiRegion(TuiRegionKind.PROGRESS, row.index, row.index, listOf(row.text.trim()), percent / 100f)
            }
        }

        var i = 0
        while (i < rows.size) {
            val row = rows[i]
            if (tableSeparator.matches(row.text) || row.text.count { it == '│' || it == '|' } >= 2) {
                val group = mutableListOf<TuiRow>()
                var j = i
                while (j < rows.size && rows[j].index <= row.index + 12 &&
                    (tableSeparator.matches(rows[j].text) || rows[j].text.count { it == '│' || it == '|' } >= 2)) {
                    group += rows[j]; j++
                }
                if (group.size >= 2) result += TuiRegion(TuiRegionKind.TABLE, group.first().index, group.last().index, group.map { it.text })
                i = j
            } else {
                i++
            }
        }

        val paneRows = rows.filter { paneSeparator.containsMatchIn(it.text) }
        if (paneRows.size >= 2) {
            result += TuiRegion(TuiRegionKind.PANE, paneRows.first().index, paneRows.last().index, paneRows.map { it.text })
        }

        val resultRows = rows.filter { resultPrefix.containsMatchIn(it.text) }
        if (resultRows.size >= 2) {
            result += TuiRegion(TuiRegionKind.RESULTS, resultRows.first().index, resultRows.last().index, resultRows.map { it.text.trim() })
        }
        return result.distinctBy { Triple(it.kind, it.startRow, it.endRow) }
    }

    private fun looksScrollable(run: List<TuiRow>, visible: List<TuiRow>): Boolean {
        if (run.size >= 6) return true
        val first = run.first().index
        val last = run.last().index
        return visible.any { it.index in (first - 1)..(last + 1) && Regex("""(?i)(more|scroll|↑|↓|page\s+\d+)""").containsMatchIn(it.text) }
    }

    private fun leadingIndent(row: TuiRow): Int = row.text.takeWhile(Char::isWhitespace).length

    private fun looksLikeExplicitMenuItem(row: TuiRow): Boolean =
        row.highlighted || row.emphasized || menuPrefix.matches(row.text) || numberedMenu.matches(row.text)

    private fun menuLabel(text: String): String =
        menuPrefix.matchEntire(text)?.groupValues?.getOrNull(1)
            ?: numberedMenu.matchEntire(text)?.groupValues?.getOrNull(1)
            ?: text.trim()

    private fun looksLikePrompt(row: TuiRow): Boolean =
        row.cursorColumn != null && (promptSuffix.containsMatchIn(row.text) || row.text.length <= 100)

    private fun promptKind(text: String): TuiPromptKind = when {
        passwordWords.containsMatchIn(text) -> TuiPromptKind.PASSWORD
        confirmWords.containsMatchIn(text) || Regex("""(?i)\[[yYnN/]+]""").containsMatchIn(text) -> TuiPromptKind.CONFIRMATION
        promptSuffix.containsMatchIn(text) -> TuiPromptKind.TEXT
        else -> TuiPromptKind.UNKNOWN
    }
}
