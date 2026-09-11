package com.hereliesaz.hg2gui.api

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Foreground approval boundary for API requests that ask for ADB-shell or root authority. */
class Hg2ApiAuthorityActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authority = intent.getStringExtra(Hg2ApiReceiver.EXTRA_AUTHORITY).orEmpty().lowercase()
        val command = intent.getStringExtra(Hg2ApiReceiver.EXTRA_COMMAND).orEmpty()
        if (authority !in setOf("adb", "root") || command.isBlank()) {
            complete(false, error = "invalid authority request")
            return
        }

        AlertDialog.Builder(this)
            .setTitle(if (authority == "root") "RUN AS ROOT?" else "RUN THROUGH ADB SHELL?")
            .setMessage(command)
            .setPositiveButton("RUN") { _, _ -> runApproved(authority, command) }
            .setNegativeButton("CANCEL") { _, _ -> complete(false, error = "authority request denied") }
            .setOnCancelListener { complete(false, error = "authority request denied") }
            .show()
    }

    private fun runApproved(authority: String, command: String) {
        scope.launch(Dispatchers.IO) {
            val engine = TerminalEngine(applicationContext)
            val output = StringBuilder()
            var exitCode: Int? = null
            try {
                val line = if (authority == "root") "hg2auth root shell $command" else "hg2auth adb shell $command"
                engine.run(
                    line,
                    onNeedInput = { prompt ->
                        // Match only the three specific prompts that hg2auth itself emits (see
                        // TerminalEngine.kt). A generic [y/N]-suffix match would fire on any
                        // nested interactive program's own confirmation, auto-accepting destructive
                        // prompts the user never saw.
                        val t = prompt.trimStart()
                        if (t.startsWith("Run as root?", ignoreCase = true) ||
                            t.startsWith("Run through ADB shell?", ignoreCase = true) ||
                            t.startsWith("Request root access to test", ignoreCase = true)) "y" else ""
                    },
                    onExit = { exitCode = it }
                ).collect { chunk -> if (output.length < MAX_OUTPUT) output.append(chunk.take(MAX_OUTPUT - output.length)) }
                complete(
                    success = exitCode == 0,
                    data = JSONObject().put("output", output.toString()).put("exitCode", exitCode).toString(),
                    error = if (exitCode == 0) null else "elevated command exited with ${exitCode ?: -1}"
                )
            } catch (e: Exception) {
                complete(false, error = e.message ?: "authority execution failed")
            } finally {
                engine.destroy()
            }
        }
    }

    private fun complete(success: Boolean, data: String? = null, error: String? = null) {
        callback()?.let { callback ->
            runCatching {
                callback.send(this, if (success) 0 else 1, Intent().apply {
                    putExtra(Hg2ApiReceiver.EXTRA_SUCCESS, success)
                    data?.let { putExtra(Hg2ApiReceiver.EXTRA_DATA, it) }
                    error?.let { putExtra(Hg2ApiReceiver.EXTRA_ERROR, it) }
                })
            }
        }
        runOnUiThread { if (!isFinishing) finish() }
    }

    @Suppress("DEPRECATION")
    private fun callback(): PendingIntent? = if (Build.VERSION.SDK_INT >= 33) {
        intent.getParcelableExtra(Hg2ApiReceiver.EXTRA_REPLY, PendingIntent::class.java)
    } else {
        intent.getParcelableExtra(Hg2ApiReceiver.EXTRA_REPLY) as? PendingIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object { private const val MAX_OUTPUT = 128_000 }
}
