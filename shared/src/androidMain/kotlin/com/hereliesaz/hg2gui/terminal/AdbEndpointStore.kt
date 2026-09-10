package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/** Last known successful ADB connection endpoints. Pairing secrets are deliberately never stored. */
object AdbEndpointStore {
    private const val DIRECTORY = "authority"
    private const val FILE_NAME = "adb-endpoints.txt"
    private val endpointPattern = Regex("(?:[A-Za-z0-9._:-]+|\\[[0-9A-Fa-f:]+]):[0-9]{1,5}")

    fun list(context: Context): List<String> = runCatching {
        val file = file(context)
        if (!file.isFile) emptyList() else file.readLines()
            .map(String::trim)
            .filter(endpointPattern::matches)
            .distinct()
            .sorted()
    }.getOrDefault(emptyList())

    fun rememberCommand(context: Context, endpoint: String): String {
        require(endpointPattern.matches(endpoint)) { "Invalid ADB endpoint." }
        val file = file(context)
        return "mkdir -p ${q(file.parentFile!!.absolutePath)} && grep -Fqx -- ${q(endpoint)} ${q(file.absolutePath)} 2>/dev/null || printf '%s\\n' ${q(endpoint)} >> ${q(file.absolutePath)}"
    }

    fun forgetCommand(context: Context, endpoint: String): String {
        require(endpointPattern.matches(endpoint)) { "Invalid ADB endpoint." }
        val file = file(context)
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        return "if [ -f ${q(file.absolutePath)} ]; then grep -Fvx -- ${q(endpoint)} ${q(file.absolutePath)} > ${q(temp.absolutePath)} || true; mv ${q(temp.absolutePath)} ${q(file.absolutePath)}; fi"
    }

    fun clearCommand(context: Context): String = "rm -f ${q(file(context).absolutePath)}"

    private fun file(context: Context): File = File(File(context.filesDir, DIRECTORY), FILE_NAME)
    private fun q(value: String): String = "'${value.replace("'", "'\\''")}'"
}
