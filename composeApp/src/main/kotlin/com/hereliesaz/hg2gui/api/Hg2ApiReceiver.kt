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
import com.hereliesaz.hg2gui.terminal.CooperativeMetadata
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Typed, same-signature external capability API. */
class Hg2ApiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { dispatch(context.applicationContext, intent) } finally { pending.finish() }
        }
    }

    private suspend fun dispatch(context: Context, request: Intent) {
        when (request.action) {
            ACTION_CAPABILITIES -> reply(context, request, true, data = capabilitySchema())
            ACTION_AUDIT_HISTORY -> reply(context, request, true, data = Hg2ApiAuditStore.history(context))
            ACTION_METADATA_PUBLISH -> metadataPublish(context, request)
            ACTION_METADATA_READ -> metadataRead(context, request)
            ACTION_METADATA_LIST -> metadataList(context, request)
            ACTION_METADATA_CLEAR -> metadataClear(context, request)
            ACTION_EXECUTE -> execute(context, request)
            ACTION_PACKAGE -> packageAction(context, request)
            ACTION_CLIPBOARD_GET -> reply(context, request, true, data = clipboard(context)?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty())
            ACTION_CLIPBOARD_SET -> {
                val service = clipboard(context) ?: return reply(context, request, false, error = "clipboard unavailable")
                service.setPrimaryClip(ClipData.newPlainText("HG2Gui API", request.getStringExtra(EXTRA_TEXT).orEmpty()))
                reply(context, request, true)
            }
            ACTION_DEVICE_INFO -> reply(context, request, true, data = deviceInfo())
            ACTION_NOTIFY -> notify(context, request)
            ACTION_DIALOG -> launchDialog(context, request)
            ACTION_SHARE -> share(context, request)
            ACTION_OPEN -> open(context, request)
            ACTION_PICK_FILE -> launchPicker(context, request, directory = false)
            ACTION_PICK_DIRECTORY -> launchPicker(context, request, directory = true)
            ACTION_LIFECYCLE -> lifecycle(context, request)
            ACTION_AUTHORITY -> authority(context, request)
            else -> reply(context, request, false, error = "unknown capability")
        }
    }

    private fun metadataPublish(context: Context, request: Intent) {
        val channel = request.getStringExtra(EXTRA_CHANNEL)?.trim().orEmpty()
        val payload = request.getStringExtra(EXTRA_PAYLOAD)
            ?: return reply(context, request, false, error = "missing payload")
        if (channel.isEmpty()) return reply(context, request, false, error = "missing channel")
        val ttlMillis = if (request.hasExtra(EXTRA_TTL_MILLIS)) request.getLongExtra(EXTRA_TTL_MILLIS, DEFAULT_METADATA_TTL) else DEFAULT_METADATA_TTL
        runCatching { CooperativeMetadata.publish(channel, payload, ttlMillis) }
            .onSuccess { reply(context, request, true, data = CooperativeMetadata.asJson(it)) }
            .onFailure { reply(context, request, false, error = it.message ?: "metadata publish failed") }
    }

    private fun metadataRead(context: Context, request: Intent) {
        val channel = request.getStringExtra(EXTRA_CHANNEL)?.trim().orEmpty()
        if (channel.isEmpty()) return reply(context, request, false, error = "missing channel")
        val entry = CooperativeMetadata.read(channel)
            ?: return reply(context, request, false, error = "metadata channel not found or expired")
        reply(context, request, true, data = CooperativeMetadata.asJson(entry))
    }

    private fun metadataList(context: Context, request: Intent) {
        val prefix = request.getStringExtra(EXTRA_PREFIX)?.trim()?.takeIf(String::isNotEmpty)
        reply(context, request, true, data = CooperativeMetadata.listJson(prefix))
    }

    private fun metadataClear(context: Context, request: Intent) {
        val channel = request.getStringExtra(EXTRA_CHANNEL)?.trim().orEmpty()
        if (channel.isEmpty()) return reply(context, request, false, error = "missing channel")
        reply(context, request, true, data = JSONObject().put("cleared", CooperativeMetadata.clear(channel)).toString())
    }

    private suspend fun execute(context: Context, request: Intent) {
        val command = request.getStringExtra(EXTRA_COMMAND)?.trim().orEmpty()
        if (command.isEmpty()) return reply(context, request, false, error = "missing command")
        // Block all hg2auth and bootstrap invocations — both require foreground approval
        val lower = command.lowercase()
        if (lower.startsWith("hg2auth") || lower == "bootstrap" || lower.startsWith("bootstrap ")) {
            return reply(context, request, false, error = "authority commands require foreground approval")
        }
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
        if (!SAFE_WORD.matches(operation) || (name.isNotEmpty() && !SAFE_PACKAGE.matches(name))) {
            return reply(context, request, false, error = "invalid package operation/name")
        }
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
        if (operation !in setOf("info", "disable", "enable", "isolate", "snapshot")) {
            return reply(context, request, false, error = "destructive lifecycle actions require foreground user interaction")
        }
        if (!SAFE_WORD.matches(manager) || !SAFE_PACKAGE.matches(name)) return reply(context, request, false, error = "invalid manager/package")
        execute(context, Intent(request).putExtra(EXTRA_COMMAND, "hg2package $operation $manager $name").setAction(ACTION_EXECUTE))
    }

    private fun authority(context: Context, request: Intent) {
        val authority = request.getStringExtra(EXTRA_AUTHORITY).orEmpty().lowercase()
        val command = request.getStringExtra(EXTRA_COMMAND).orEmpty()
        if (authority !in setOf("adb", "root")) return reply(context, request, false, error = "unknown authority")
        if (command.isBlank()) return reply(context, request, false, error = "missing command")
        context.startActivity(Intent(context, Hg2ApiAuthorityActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_AUTHORITY, authority)
            putExtra(EXTRA_COMMAND, command)
            callback(request)?.let { putExtra(EXTRA_REPLY, it) }
        })
    }

    private fun notify(context: Context, request: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return reply(context, request, false, error = "notifications unavailable")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "HG2Gui API", NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(request.getIntExtra(EXTRA_ID, 1), NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(request.getStringExtra(EXTRA_TITLE) ?: "HG2Gui")
            .setContentText(request.getStringExtra(EXTRA_TEXT).orEmpty())
            .build())
        reply(context, request, true)
    }

    private fun launchDialog(context: Context, request: Intent) {
        context.startActivity(Intent(context, Hg2ApiDialogActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_TITLE, request.getStringExtra(EXTRA_TITLE))
            putExtra(EXTRA_TEXT, request.getStringExtra(EXTRA_TEXT))
            callback(request)?.let { putExtra(EXTRA_REPLY, it) }
        })
    }

    private fun share(context: Context, request: Intent) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = request.getStringExtra(EXTRA_MIME) ?: "text/plain"
            putExtra(Intent.EXTRA_TEXT, request.getStringExtra(EXTRA_TEXT).orEmpty())
        }
        context.startActivity(Intent.createChooser(intent, request.getStringExtra(EXTRA_TITLE) ?: "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        reply(context, request, true)
    }

    private fun open(context: Context, request: Intent) {
        val uri = request.getStringExtra(EXTRA_URI)?.let(Uri::parse) ?: return reply(context, request, false, error = "missing uri")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onSuccess { reply(context, request, true) }
            .onFailure { reply(context, request, false, error = it.message ?: "no app can open uri") }
    }

    private fun launchPicker(context: Context, request: Intent, directory: Boolean) {
        context.startActivity(Intent(context, Hg2ApiPickerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_PICK_DIRECTORY, directory)
            putExtra(EXTRA_MIME, request.getStringExtra(EXTRA_MIME))
            callback(request)?.let { putExtra(EXTRA_REPLY, it) }
        })
    }

    private fun deviceInfo(): String = JSONObject()
        .put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL).put("device", Build.DEVICE)
        .put("sdk", Build.VERSION.SDK_INT).put("release", Build.VERSION.RELEASE)
        .put("abis", Build.SUPPORTED_ABIS.joinToString(",")).toString()

    private fun capabilitySchema(): String {
        fun capability(
            action: String,
            required: List<String> = emptyList(),
            optional: List<String> = emptyList(),
            foregroundApproval: Boolean = false
        ) = JSONObject()
            .put("action", action)
            .put("requiredExtras", JSONArray(required))
            .put("optionalExtras", JSONArray(optional))
            .put("foregroundApproval", foregroundApproval)

        return JSONObject()
            .put("apiVersion", API_VERSION)
            .put("permission", PERMISSION)
            .put("metadataChannels", JSONObject()
                .put("tui/<command>", "TuiSnapshot JSON")
                .put("shell/<family>", "ShellPresentation JSON")
                .put("guide/<command>", "live command metadata JSON"))
            .put("resultEnvelope", JSONObject()
                .put("success", EXTRA_SUCCESS)
                .put("data", EXTRA_DATA)
                .put("error", EXTRA_ERROR)
                .put("apiVersion", EXTRA_API_VERSION)
                .put("resultType", EXTRA_RESULT_TYPE))
            .put("capabilities", JSONArray(listOf(
                capability(ACTION_CAPABILITIES),
                capability(ACTION_AUDIT_HISTORY),
                capability(ACTION_METADATA_PUBLISH, required = listOf(EXTRA_CHANNEL, EXTRA_PAYLOAD), optional = listOf(EXTRA_TTL_MILLIS)),
                capability(ACTION_METADATA_READ, required = listOf(EXTRA_CHANNEL)),
                capability(ACTION_METADATA_LIST, optional = listOf(EXTRA_PREFIX)),
                capability(ACTION_METADATA_CLEAR, required = listOf(EXTRA_CHANNEL)),
                capability(ACTION_EXECUTE, required = listOf(EXTRA_COMMAND)),
                capability(ACTION_PACKAGE, required = listOf(EXTRA_MANAGER, EXTRA_OPERATION), optional = listOf(EXTRA_PACKAGE)),
                capability(ACTION_PICK_FILE, optional = listOf(EXTRA_MIME), foregroundApproval = true),
                capability(ACTION_PICK_DIRECTORY, foregroundApproval = true),
                capability(ACTION_NOTIFY, optional = listOf(EXTRA_ID, EXTRA_TITLE, EXTRA_TEXT)),
                capability(ACTION_DIALOG, optional = listOf(EXTRA_TITLE, EXTRA_TEXT), foregroundApproval = true),
                capability(ACTION_CLIPBOARD_GET),
                capability(ACTION_CLIPBOARD_SET, required = listOf(EXTRA_TEXT)),
                capability(ACTION_DEVICE_INFO),
                capability(ACTION_SHARE, optional = listOf(EXTRA_MIME, EXTRA_TITLE, EXTRA_TEXT), foregroundApproval = true),
                capability(ACTION_OPEN, required = listOf(EXTRA_URI), foregroundApproval = true),
                capability(ACTION_LIFECYCLE, required = listOf(EXTRA_OPERATION, EXTRA_MANAGER, EXTRA_PACKAGE)),
                capability(ACTION_AUTHORITY, required = listOf(EXTRA_AUTHORITY, EXTRA_COMMAND), foregroundApproval = true)
            )))
            .toString()
    }

    private fun resultType(action: String?): String = when (action) {
        ACTION_CAPABILITIES -> "capability-schema"
        ACTION_AUDIT_HISTORY -> "api-audit-history"
        ACTION_METADATA_PUBLISH, ACTION_METADATA_READ -> "metadata-entry"
        ACTION_METADATA_LIST -> "metadata-list"
        ACTION_METADATA_CLEAR -> "metadata-clear"
        ACTION_EXECUTE, ACTION_PACKAGE, ACTION_LIFECYCLE -> "command-result"
        ACTION_DEVICE_INFO -> "device-info"
        ACTION_CLIPBOARD_GET -> "text"
        ACTION_PICK_FILE, ACTION_PICK_DIRECTORY -> "uri"
        else -> "ack"
    }

    private fun clipboard(context: Context): ClipboardManager? = context.getSystemService(ClipboardManager::class.java)

    @Suppress("DEPRECATION")
    private fun callback(request: Intent): PendingIntent? =
        if (Build.VERSION.SDK_INT >= 33) request.getParcelableExtra(EXTRA_REPLY, PendingIntent::class.java)
        else request.getParcelableExtra(EXTRA_REPLY) as? PendingIntent

    private fun reply(context: Context, request: Intent, success: Boolean, data: String? = null, error: String? = null) {
        val callback = callback(request)
        Hg2ApiAuditStore.record(
            context = context,
            action = request.action.orEmpty(),
            callerPackage = runCatching { callback?.creatorPackage }.getOrNull(),
            success = success,
            error = error
        )
        callback ?: return
        runCatching {
            callback.send(context, if (success) 0 else 1, Intent().apply {
                putExtra(EXTRA_SUCCESS, success)
                putExtra(EXTRA_API_VERSION, API_VERSION)
                putExtra(EXTRA_RESULT_TYPE, resultType(request.action))
                data?.let { putExtra(EXTRA_DATA, it) }
                error?.let { putExtra(EXTRA_ERROR, it) }
            })
        }
    }

    companion object {
        const val API_VERSION = 1
        const val PERMISSION = "com.hereliesaz.hg2gui.permission.API"
        const val ACTION_CAPABILITIES = "com.hereliesaz.hg2gui.api.CAPABILITIES"
        const val ACTION_AUDIT_HISTORY = "com.hereliesaz.hg2gui.api.AUDIT_HISTORY"
        const val ACTION_METADATA_PUBLISH = "com.hereliesaz.hg2gui.api.METADATA_PUBLISH"
        const val ACTION_METADATA_READ = "com.hereliesaz.hg2gui.api.METADATA_READ"
        const val ACTION_METADATA_LIST = "com.hereliesaz.hg2gui.api.METADATA_LIST"
        const val ACTION_METADATA_CLEAR = "com.hereliesaz.hg2gui.api.METADATA_CLEAR"
        const val ACTION_EXECUTE = "com.hereliesaz.hg2gui.api.EXECUTE"
        const val ACTION_PACKAGE = "com.hereliesaz.hg2gui.api.PACKAGE"
        const val ACTION_PICK_FILE = "com.hereliesaz.hg2gui.api.PICK_FILE"
        const val ACTION_PICK_DIRECTORY = "com.hereliesaz.hg2gui.api.PICK_DIRECTORY"
        const val ACTION_NOTIFY = "com.hereliesaz.hg2gui.api.NOTIFY"
        const val ACTION_DIALOG = "com.hereliesaz.hg2gui.api.DIALOG"
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
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_TTL_MILLIS = "ttl_millis"
        const val EXTRA_PREFIX = "prefix"
        const val EXTRA_REPLY = "reply"
        const val EXTRA_SUCCESS = "success"
        const val EXTRA_DATA = "data"
        const val EXTRA_ERROR = "error"
        const val EXTRA_API_VERSION = "api_version"
        const val EXTRA_RESULT_TYPE = "result_type"
        const val EXTRA_PICK_DIRECTORY = "pick_directory"
        private val SAFE_WORD = Regex("[A-Za-z0-9_.+-]+")
        private val SAFE_PACKAGE = Regex("[A-Za-z0-9@._+=-]+")
        private const val CHANNEL = "hg2gui-api"
        private const val MAX_OUTPUT = 128_000
        private const val DEFAULT_METADATA_TTL = 30_000L
    }
}
