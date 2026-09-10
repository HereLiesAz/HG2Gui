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
        val parts = command.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (parts.isEmpty()) return GuideRuntimeInfo(false)
        val first = parts.first()
        val roots = nodes.filter { node -> node.label == first || node.value?.trim()?.substringBefore(' ') == first }
        val root = roots.maxByOrNull { it.children.size } ?: return GuideRuntimeInfo(false)
        val remainder = parts.drop(1).joinToString(" ")
        val target = if (remainder.isBlank()) root else {
            flatten(root.children).firstOrNull { child ->
                child.label == remainder || child.value?.trim() == command || child.value?.trim() == remainder
            } ?: root
        }
        val hints = (if (target.children.isNotEmpty()) target.children else root.children)
            .asSequence()
            .filter { it.emitsToken }
            .map { it.value?.trim().orEmpty().ifBlank { it.label } }
            .filter(String::isNotBlank)
            .distinct()
            .take(8)
            .toList()
        return GuideRuntimeInfo(true, target.cap ?: root.cap, hints)
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
