package com.hereliesaz.hg2gui.ui.guide

import com.hereliesaz.hg2gui.ui.menu.MenuNode

data class GuideRuntimeInfo(
    val available: Boolean,
    val cap: String? = null,
    val hints: List<String> = emptyList()
)

/** Read-only index over the same materialized command tree shown by the terminal composer. */
class GuideRuntimeIndex private constructor(private val nodes: List<MenuNode>) {
    fun info(command: String): GuideRuntimeInfo {
        val normalized = command.trim().replace(Regex("\\s+"), " ")
        if (normalized.isEmpty()) return GuideRuntimeInfo(false)
        val first = normalized.substringBefore(' ')

        val exact = nodes.firstOrNull { node ->
            node.value?.trim()?.replace(Regex("\\s+"), " ") == normalized || node.label == normalized
        }
        val root = exact ?: nodes
            .filter { node -> node.label == first || node.value?.trim()?.substringBefore(' ') == first }
            .maxByOrNull { it.children.size }
            ?: return GuideRuntimeInfo(false)

        val hintSource = when {
            exact?.children?.isNotEmpty() == true -> exact.children
            else -> nodes.filter { node ->
                val value = node.value?.trim().orEmpty()
                value.startsWith("$normalized ") ||
                    (normalized == first && value.startsWith("$first "))
            }
        }
        val hints = hintSource.asSequence()
            .filter { it.emitsToken }
            .map { it.value?.trim().orEmpty().ifBlank { it.label } }
            .map { value ->
                if (value.startsWith("$first ")) value.removePrefix("$first ") else value
            }
            .filter(String::isNotBlank)
            .distinct()
            .take(8)
            .toList()
        return GuideRuntimeInfo(true, exact?.cap ?: root.cap, hints)
    }

    companion object {
        val Empty = GuideRuntimeIndex(emptyList())

        fun fromTree(tree: List<MenuNode>): GuideRuntimeIndex = GuideRuntimeIndex(flatten(tree))

        private fun flatten(tree: List<MenuNode>): List<MenuNode> = buildList {
            fun addNode(node: MenuNode) {
                add(node)
                node.children.forEach(::addNode)
            }
            tree.forEach(::addNode)
        }
    }
}
