package com.hereliesaz.hg2gui.ui

data class Workflow(val name: String, val template: String)

/**
 * Drives the Workflows pill: saving and running named command templates. `{placeholder}`
 * substrings in the template are the only thing that makes a workflow parameterized - no
 * separate arg-declaration step. Reuses SessionUiState's awaitPromptAnswer machinery exactly
 * like SshFlow does; a run always ends by writing the assembled command into the session's
 * input field for the user to review and press Run themselves, never by executing it.
 */
object WorkflowFlow {
    private val placeholderRegex = Regex("\\{([a-zA-Z0-9_]+)\\}")

    fun placeholders(template: String): List<String> =
        placeholderRegex.findAll(template).map { it.groupValues[1] }.distinct().toList()

    fun render(template: String, values: Map<String, String>): String =
        placeholderRegex.replace(template) { match -> values[match.groupValues[1]] ?: match.value }

    suspend fun runNewWorkflowWizard(session: SessionUiState, save: suspend (Workflow) -> Unit) {
        val name = session.awaitPromptAnswer("Workflow name?").trim()
        if (name.isEmpty()) return

        val template = session.awaitPromptAnswer(
            "Command template? Use {name} for a value you'll fill in each time, " +
                "e.g. git commit -m \"{message}\""
        ).trim()
        if (template.isEmpty()) return

        save(Workflow(name, template))
    }

    suspend fun runWorkflowWizard(session: SessionUiState, workflow: Workflow) {
        val values = mutableMapOf<String, String>()
        for (placeholder in placeholders(workflow.template)) {
            values[placeholder] = session.awaitPromptAnswer("$placeholder?").trim()
        }
        session.tokens = emptyList()
        session.inputText = render(workflow.template, values)
    }
}
