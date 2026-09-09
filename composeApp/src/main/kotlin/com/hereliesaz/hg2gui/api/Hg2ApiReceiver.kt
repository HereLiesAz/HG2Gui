package com.hereliesaz.hg2gui.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hereliesaz.hg2gui.TerminalActivity
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Typed, same-signature external API. Callers request named capabilities rather than gaining a
 * generic in-process shell. Capabilities that require user authority are handed to TerminalActivity
 * and never silently elevated by this receiver.
 */
class Hg2ApiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                dispatch(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun dispatch(context: Context, request: Intent) {
        when (request.action) {
            ACTION_EXECUTE -> execute(context, request)
            ACTION_PACKAGE -> packageAction(context, request)
            ACTION_CLIPBOARD_GET -> reply(context, request, true, data = clipboard(context).primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty())
            ACTION_CLIPBOARD_SET -> {
                clipboard(context).setPrimaryClip(ClipData.newPlainText("HG2Gui API", request.getStringExtra(EXTRA_TEXT).orEmpty()))
                reply(context, request, true)
            }
            ACTION_DEVICE_INFO -> reply(context, request, true, data = deviceInfo())
            ACTION_NOTIFY -> notify(context, request)
            ACTION_SHARE -> share(context, request)
            ACTION_OPEN -> open(context, request)
            ACTION_PICK_FILE -> launchPicker(context, request, directory = false)
            ACTION_PICK_DIRECTORY -> launchPicker(context, request, directory = true)
            ACTION_LIFECYCLE -> lifecycle(context, request)
            ACTION_AUTHORITY -> authority(context, request)
            else -> reply(context, request, false, error = "unknown capability")
        }
    }

    private suspend fun execute(context: Context, request: Intent) {
        val command = request.getStringExtra(EXTRA_COMMAND)?.trim().orEmpty()
        if (command.isEmpty()) return reply(context, request, false, error = "missing command")
        val engine = TerminalEngine(context)
        val output = StringBuilder()
        var exitCode: Int? = null
        try {
            engine.run(command, onNeedInput = { "" }, onExit = { exitCode = it }).collect { chunk ->
                if (output.length < MAX_OUTPUT) output.append(chunk.take(MAX_OUTPUT - output.length))
            }
            reply(context, request, true, data = JSONObject().put("output", output.toString()).put("exitCode", exitCode).toString())
        } catch (e: Exception) {
            reply(context, request, false, error = e.message ?: "execution failed")
        } finally {
            engine.destroy()
        }
    }

    private suspend fun packageAction(context: Context, request: Intent) {
        val manager = request.getStringExtra(EXTRA_MANAGER)?.trim().orEmpty()
        val operation = request.getStringExtra(EXTRA_OPERATION)?.trim().orEmpty()
        val name = request.getStringExtra(EXTRA_PACKAGE)?.trim().orEmpty()
        if (manager.isEmpty() || operation.isEmpty()) return reply(context, request, false, error = "missing manager/operation")
        val command = when (manager.lowercase()) {
            "pkg", "apt" -> listOf("pkg", operation, name)
            "pip" -> listOf("python", "-m", "pip", operation, name)
            "npm" -> listOf("npm", operation, name)
            "gem" -> listOf("gem", operation, name)
            else -> return reply(context, request, false, error = "unsupported package manager")
        }.filter(String::isNotBlank).joinToString(" ")
        execute(context, Intent(request).putExtra(EXTRA_COMMAND, command).setAction(ACTION_EXECUTE))
    }

    private suspend fun lifecycle(context: Context, request: Intent) {
        val operation = request.getStringExtra(EXTRA_OPERATION)?.trim().orEmpty()
        val manager = request.getStringExtra(EXTRA_MANAGER)?.trim().orEmpty()
        val name = request.getStringExtra(EXTRA_PACKAGE)?.trim().orEmpty()
        if (operation !in setOf("info", "disable", "enable", "isolate")) {
            return reply(context, request, false, error = "destructive lifecycle actions require foreground user interaction")
        }
        execute(context, Intent(request).putExtra(EXTRA_COMMAND, "hg2package $operation $manager $name").setAction(ACTION_EXECUTE))
    }

    private fun authority(context: Context, request: Intent) {
        val authority = request.getStringExtra(EXTRA_AUTHORITY).orEmpty()
        val command = request.getStringExtra(EXTRA_COMMAND).orEmpty()
        val launch = Intent(context, TerminalActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_API_AUTHORITY, authority)
            putExtra(EXTRA_API_COMMAND, command)
        }
        context.startActivity(launch)
        reply(context, request, true, data = "foreground approval requested")
    }

    private fun notify(context: Context, request: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "HG2Gui API", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(
            request.getIntExtra(EXTRA_ID, 1),
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(request.getStringExtra(EXTRA_TITLE) ?: "HG2Gui")
                .setContentText(request.getStringExtra(EXTRA_TEXT).orEmpty())
                .build()
        )
        reply(context, request, true)
    }

    private fun share(context: Context, request: Intent) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = request.getStringExtra(EXTRA_MIME) ?: "text/plain"
            putExtra(Intent.EXTRA_TEXT, request.getStringExtra(EXTRA_TEXT).orEmpty())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, request.getStringExtra(EXTRA_TITLE) ?: "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        reply(context, request, true)
    }

    private fun open(context: Context, request: Intent) {
        val uri = request.getStringExtra(EXTRA_URI)?.let(Uri::parse) ?: return reply(context, request, false, error = "missing uri")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        reply(context, request, true)
    }

    private fun launchPicker(context: Context, request: Intent, directory: Boolean) {
        val picker = if (directory) Intent(Intent.ACTION_OPEN_DOCUMENT_TREE) else Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = request.getStringExtra(EXTRA_MIME) ?: "*/*"
        }
        context.startActivity(picker.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        reply(context, request, true, data = "picker launched")
    }

    private fun deviceInfo(): String = JSONObject()
        .put("manufacturer", Build.MANUFACTURER)
        .put("model", Build.MODEL)
        .put("device", Build.DEVICE)
        .put("sdk", Build.VERSION.SDK_INT)
        .put("release", Build.VERSION.RELEASE)
        .put("abis", Build.SUPPORTED_ABIS.joinToString(","))
        .toString()

    private fun clipboard(context: Context) = context.getSystemService(ClipboardManager::class.java)

    private fun reply(context: Context, request: Intent, success: Boolean, data: String? = null, error: String? = null) {
        val callback = if (Build.VERSION.SDK_INT >= 33) request.getParcelableExtra(EXTRA_REPLY, PendingIntent::class.java) else @Suppress("DEPRECATION") (request.getParcelableExtra(EXTRA_REPLY) as? PendingIntent)
        callback?.send(context, if (success) 0 else 1, Intent().apply {
            putExtra(EXTRA_SUCCESS, success)
            data?.let { putExtra(EXTRA_DATA, it) }
            error?.let { putExtra(EXTRA_ERROR, it) }
        })
    }

    companion object {
        const val PERMISSION = "com.hereliesaz.hg2gui.permission.API"
        const val ACTION_EXECUTE = "com.hereliesaz.hg2gui.api.EXECUTE"
        const val ACTION_PACKAGE = "com.hereliesaz.hg2gui.api.PACKAGE"
        const val ACTION_PICK_FILE = "com.hereliesaz.hg2gui.api.PICK_FILE"
        const val ACTION_PICK_DIRECTORY = "com.hereliesaz.hg2gui.api.PICK_DIRECTORY"
        const val ACTION_NOTIFY = "com.hereliesaz.hg2gui.api.NOTIFY"
        const val ACTION_CLIPBOARD_GET = "com.hereliesaz.hg2gui.api.CLIPBOARD_GET"
        const val ACTION_CLIPBOARD_SET = "com.hereliesaz.hg2gui.api.CLIPBOARD_SET"
        const val ACTION_DEVICE_INFO = "com.hereliesaz.hg2gui.api.DEVICE_INFO"
        const val ACTION_SHARE = "com.hereliesaz.hg2gui.api.SHARE"
        const val ACTION_OPEN = "com.hereliesaz.hg2gui.api.OPEN"
        const val ACTION_LIFECYCLE = "com.hereliesaz.hg2gui.api.LIFECYCLE"
        const val ACTION_AUTHORITY = "com.hereliesaz.hg2gui.api.AUTHORITY"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_MANAGER = "manager"
        const val EXTRA_OPERATION = "operation"
        const val EXTRA_PACKAGE = "package"
        const val EXTRA_TEXT = "text"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MIME = "mime"
        const val EXTRA_URI = "uri"
        const val EXTRA_ID = "id"
        const val EXTRA_AUTHORITY = "authority"
        const val EXTRA_REPLY = "reply"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_DATA = "data"
        const val EXTRA_ERROR = "error"
        const val EXTRA_API_AUTHORITY = "hg2api.authority"
        const val EXTRA_API_COMMAND = "hg2api.command"
        private const val CHANNEL = "hg2gui-api"
        private const val MAX_OUTPUT = 128_000
    }
}
