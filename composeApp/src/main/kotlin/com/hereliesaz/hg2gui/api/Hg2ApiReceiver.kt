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
import android.app.ActivityManager
import android.os.Process
import com.hereliesaz.hg2gui.LongTaskService
import com.hereliesaz.hg2gui.mcp.CommandOutputAccumulator
import com.hereliesaz.hg2gui.terminal.CooperativeMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val FOREGROUND_ONLY_COMMANDS = setOf("hg2auth", "bootstrap")

/**
 * True iff any shell token in [command] - after splitting on whitespace and shell control/
 * substitution characters and dropping quotes/backslashes - has a basename of `hg2auth` or
 * `bootstrap` (case-insensitive). Deliberately over-broad: a false positive just means the caller
 * has to run that command in the foreground. Pure, see Hg2ApiCommandFilterTest.
 */
internal fun containsForegroundOnlyCommand(command: String): Boolean =
    command.split(Regex("""[\s;|&()`<>{}$=]+"""))
        .asSequence()
        .map { it.replace(Regex("""["'\\]"""), "") }
        .map { it.substringAfterLast('/').lowercase() }
        .any { it in FOREGROUND_ONLY_COMMANDS }

/** Lifecycle operations that read state or only snapshot it, so they may run with HG2Gui in the background. */
private val BACKGROUND_LIFECYCLE_OPERATIONS = setOf("info", "snapshot")

/**
 * Whether a `hg2package` lifecycle [operation] may run for an external caller. Read-only/snapshot
 * operations always may; `enable` changes what a package may do, so it needs HG2Gui in the
 * foreground ([appInForeground]) with the user present. Everything else is never allowed here.
 * Pure, see Hg2ApiCommandFilterTest.
 */
internal fun lifecycleOperationAllowed(operation: String, appInForeground: Boolean): Boolean =
    operation in BACKGROUND_LIFECYCLE_OPERATIONS || (operation == "enable" && appInForeground)

/** Typed, same-signature external capability API. */
class Hg2ApiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        // A private copy, tagged with a fresh id that ties this request's audit rows together. The
        // broadcast Intent may be recycled once onReceive returns and both paths below keep working
        // on it afterwards; overwriting EXTRA_REQUEST_ID also stops a caller choosing its own.
        val request = Intent(intent).putExtra(EXTRA_REQUEST_ID, UUID.randomUUID().toString())
        // Shell commands can run far past a receiver's goAsync() window, so they move into a
        // foreground service and reply via the callback when done. Android 12+ often refuses a
        // foreground-service start from a broadcast; then the old goAsync path is all there is -
        // and it is only good for the short time the broadcast stays alive.
        if (request.action in LONG_RUNNING_ACTIONS &&
            LongTaskService.launch(appContext, "HG2Gui API command") { handle(appContext, request, viaForegroundService = true) }
        ) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { handle(appContext, request, viaForegroundService = false) } finally { pending.finish() }
        }
    }

    /**
     * Runs [dispatch] under a time budget and turns every failure into a reply. Before this an
     * exception escaping `dispatch` (a refused startActivity, a bad extra) crashed the process, and
     * the goAsync fallback had no bound at all, so a long command simply outlived the broadcast and
     * the process could be killed mid-`pkg install`.
     */
    private suspend fun handle(context: Context, request: Intent, viaForegroundService: Boolean) {
        val budget = if (viaForegroundService) FOREGROUND_SERVICE_CAP_MS else BROADCAST_BUDGET_MS
        try {
            val finished = withTimeoutOrNull(budget) {
                dispatch(context, request, viaForegroundService)
                true
            }
            if (finished == null) {
                reply(
                    context, request, false,
                    error = if (viaForegroundService) {
                        "timed out after ${budget / 1000}s"
                    } else {
                        "timed out after ${budget / 1000}s: Android refused HG2Gui a foreground service for this broadcast, " +
                            "so only short commands can finish - open HG2Gui and retry"
                    }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            reply(context, request, false, error = e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun dispatch(context: Context, request: Intent, viaForegroundService: Boolean) {
        // Logged before anything is validated or executed: a request that is refused, crashes or is
        // never answered must still be in the audit trail. The result row comes from reply().
        Hg2ApiAuditStore.record(
            context = context,
            action = request.action.orEmpty(),
            callerPackage = Hg2ApiReplies.callerPackageOf(callback(request)),
            success = null,
            error = null,
            phase = Hg2ApiAuditStore.PHASE_REQUEST,
            requestId = request.getStringExtra(EXTRA_REQUEST_ID),
            command = request.getStringExtra(EXTRA_COMMAND),
            detail = requestDetail(request)
        )
        when (request.action) {
            ACTION_CAPABILITIES -> reply(context, request, true, data = capabilitySchema())
            ACTION_AUDIT_HISTORY -> reply(context, request, true, data = Hg2ApiAuditStore.history(context))
            ACTION_METADATA_PUBLISH -> metadataPublish(context, request)
            ACTION_METADATA_READ -> metadataRead(context, request)
            ACTION_METADATA_LIST -> metadataList(context, request)
            ACTION_METADATA_CLEAR -> metadataClear(context, request)
            ACTION_EXECUTE -> execute(context, request)
            ACTION_PACKAGE -> packageAction(context, request, viaForegroundService)
            ACTION_CLIPBOARD_GET -> clipboardGet(context, request)
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

    /** Non-secret request parameters worth keeping in the audit row (never the command text - that is digested). */
    private fun requestDetail(request: Intent): String? = when (request.action) {
        ACTION_PACKAGE, ACTION_LIFECYCLE -> listOf(EXTRA_MANAGER, EXTRA_OPERATION, EXTRA_PACKAGE)
            .joinToString(" ") { "$it=${request.getStringExtra(it).orEmpty().take(MAX_DETAIL_FIELD)}" }
        ACTION_AUTHORITY -> "authority=${request.getStringExtra(EXTRA_AUTHORITY).orEmpty().take(MAX_DETAIL_FIELD)} " +
            "command=${auditPreview(request.getStringExtra(EXTRA_COMMAND).orEmpty().trim())}"
        else -> null
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
        runCommand(context, request, command)
    }

    /**
     * Runs [command] on a fresh headless engine and replies with its output and exit code. The
     * engine's own policy ([headlessEngine]) is the authority on what a background caller may do;
     * the two string checks below are defence in depth in front of it.
     */
    private suspend fun runCommand(context: Context, request: Intent, command: String) {
        // Block all hg2auth and bootstrap invocations - both require foreground approval. A plain
        // prefix check missed `env hg2auth`, `sh -c hg2auth`, `true; hg2auth`, `/abs/hg2auth`,
        // `X=1 hg2auth`, so every token anywhere in the line is checked instead.
        if (containsForegroundOnlyCommand(command)) {
            return reply(context, request, false, error = "authority commands require foreground approval")
        }
        val foreground = isAppInForeground(context)
        if (containsBackgroundDeniedCommand(command, foreground)) {
            return reply(context, request, false, error = "destructive lifecycle and mount commands require foreground user interaction")
        }
        val engine = headlessEngine(context, foreground)
        val output = CommandOutputAccumulator(MAX_OUTPUT)
        var exitCode: Int? = null
        try {
            engine.run(
                command,
                onNeedInput = { "" },
                onExit = { exitCode = it },
                onStyledOutput = { output.markFinalSnapshot() }
            ).collect { chunk -> output.add(chunk) }
            reply(context, request, true, data = JSONObject().put("output", output.text()).put("exitCode", exitCode).toString())
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            reply(context, request, false, error = e.message ?: "execution failed")
        } finally {
            engine.destroy()
        }
    }

    private suspend fun packageAction(context: Context, request: Intent, viaForegroundService: Boolean) {
        val manager = request.getStringExtra(EXTRA_MANAGER)?.trim().orEmpty()
        val operation = request.getStringExtra(EXTRA_OPERATION)?.trim().orEmpty()
        val name = request.getStringExtra(EXTRA_PACKAGE)?.trim().orEmpty()
        if (manager.isEmpty() || operation.isEmpty()) return reply(context, request, false, error = "missing manager/operation")
        if (!SAFE_WORD.matches(operation) || (name.isNotEmpty() && !SAFE_PACKAGE.matches(name))) {
            return reply(context, request, false, error = "invalid package operation/name")
        }
        // A package operation killed halfway leaves dpkg/npm/pip state half-mutated, and without the
        // foreground service the process only lives as long as the broadcast does.
        if (!viaForegroundService) {
            return reply(context, request, false, error = "package operations need HG2Gui's foreground service, which Android refused; open HG2Gui and retry")
        }
        val command = when (manager.lowercase()) {
            "pkg", "apt" -> listOf("pkg", operation, name)
            "pip" -> listOf("python", "-m", "pip", operation, name)
            "npm" -> listOf("npm", operation, name)
            "gem" -> listOf("gem", operation, name)
            else -> return reply(context, request, false, error = "unsupported package manager")
        }.filter(String::isNotBlank).joinToString(" ")
        // The request keeps its PACKAGE action (it used to be rewritten to EXECUTE, which is what the
        // audit log and result type then reported).
        runCommand(context, request, command)
    }

    private suspend fun lifecycle(context: Context, request: Intent) {
        val operation = request.getStringExtra(EXTRA_OPERATION)?.trim().orEmpty()
        val manager = request.getStringExtra(EXTRA_MANAGER)?.trim().orEmpty()
        val name = request.getStringExtra(EXTRA_PACKAGE)?.trim().orEmpty()
        // enable/disable/isolate change what a package may do and need the user present, like every
        // other destructive lifecycle action - only info/snapshot run from the background; enable
        // additionally runs while HG2Gui is in the foreground.
        if (!lifecycleOperationAllowed(operation, isAppInForeground(context))) {
            return reply(context, request, false, error = "destructive lifecycle actions require foreground user interaction")
        }
        if (!SAFE_WORD.matches(manager) || !SAFE_PACKAGE.matches(name)) return reply(context, request, false, error = "invalid manager/package")
        runCommand(context, request, "hg2package $operation $manager $name")
    }

    private fun authority(context: Context, request: Intent) {
        val authority = request.getStringExtra(EXTRA_AUTHORITY).orEmpty().lowercase()
        val command = request.getStringExtra(EXTRA_COMMAND).orEmpty().trim()
        if (authority !in setOf("adb", "root")) return reply(context, request, false, error = "unknown authority")
        if (command.isBlank()) return reply(context, request, false, error = "missing command")
        // The dialog shows the whole command, so it has to be bounded.
        if (authorityCommandOrNull(authority, command) == null) {
            return reply(context, request, false, error = "command is longer than $MAX_AUTHORITY_COMMAND_CHARS characters")
        }
        val callback = callback(request)
        val caller = Hg2ApiReplies.callerPackageOf(callback)
        launchOrNotify(context, Intent(context, Hg2ApiAuthorityActivity::class.java).apply {
            putExtra(EXTRA_AUTHORITY, authority)
            putExtra(EXTRA_COMMAND, command)
            putExtra(EXTRA_REQUEST_ID, request.getStringExtra(EXTRA_REQUEST_ID))
            caller?.let { putExtra(EXTRA_CALLER_PACKAGE, it) }
            callback?.let { putExtra(EXTRA_REPLY, it) }
        }, "Approve ${authority} command", caller)
    }

    /**
     * Android 10+ silently drops activity starts from a background app, so a request that needs
     * the user (dialog, picker, authority, share, open) only starts directly while HG2Gui is in the
     * foreground. Otherwise it is posted as a notification whose tap opens the same screen.
     * Returns true when the activity was started now, false when it waits on the notification.
     */
    private fun launchOrNotify(context: Context, intent: Intent, title: String, caller: String? = null): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (isAppInForeground(context)) {
            context.startActivity(intent)
            return true
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "HG2Gui API", NotificationManager.IMPORTANCE_DEFAULT))
        val id = PROMPT_NOTIFICATION_BASE + (nextPromptId++ and 0xFFFF)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("${caller ?: "An app"} is asking HG2Gui for this - tap to continue.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build())
        return false
    }

    private fun isAppInForeground(context: Context): Boolean {
        val processes = context.getSystemService(ActivityManager::class.java)?.runningAppProcesses ?: return false
        return processes.any {
            it.pid == Process.myPid() && it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    /** `launched` says whether the activity is up now or waits on a notification tap. */
    private fun launchResult(launched: Boolean): String = JSONObject().put("launched", launched).toString()

    private fun notify(context: Context, request: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return reply(context, request, false, error = "notifications unavailable")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "HG2Gui API", NotificationManager.IMPORTANCE_DEFAULT))
        // The id is confined to a private range (clampNotificationId): an arbitrary caller id could
        // otherwise overwrite the foreground-service / MCP / update notifications with its own text.
        manager.notify(clampNotificationId(request.getIntExtra(EXTRA_ID, 1)), NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(request.getStringExtra(EXTRA_TITLE) ?: "HG2Gui")
            .setContentText(request.getStringExtra(EXTRA_TEXT).orEmpty())
            .build())
        reply(context, request, true)
    }

    private fun launchDialog(context: Context, request: Intent) {
        val callback = callback(request)
        val caller = Hg2ApiReplies.callerPackageOf(callback)
        launchOrNotify(context, Intent(context, Hg2ApiDialogActivity::class.java).apply {
            putExtra(EXTRA_TITLE, request.getStringExtra(EXTRA_TITLE))
            putExtra(EXTRA_TEXT, request.getStringExtra(EXTRA_TEXT))
            putExtra(EXTRA_REQUEST_ID, request.getStringExtra(EXTRA_REQUEST_ID))
            callback?.let { putExtra(EXTRA_REPLY, it) }
        }, request.getStringExtra(EXTRA_TITLE) ?: "HG2Gui dialog", caller)
    }

    private fun share(context: Context, request: Intent) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = request.getStringExtra(EXTRA_MIME) ?: "text/plain"
            putExtra(Intent.EXTRA_TEXT, request.getStringExtra(EXTRA_TEXT).orEmpty())
        }
        // Success is only reported once the chooser actually started (or was posted for the
        // user to tap) - a failed start used to be acknowledged as success regardless.
        runCatching { launchOrNotify(context, Intent.createChooser(intent, request.getStringExtra(EXTRA_TITLE) ?: "Share"), "Share from HG2Gui") }
            .onSuccess { reply(context, request, true, data = launchResult(it)) }
            .onFailure { reply(context, request, false, error = it.message ?: "share failed") }
    }

    private fun open(context: Context, request: Intent) {
        val uri = request.getStringExtra(EXTRA_URI)?.let(Uri::parse) ?: return reply(context, request, false, error = "missing uri")
        runCatching { launchOrNotify(context, Intent(Intent.ACTION_VIEW, uri), "Open link from HG2Gui") }
            .onSuccess { reply(context, request, true, data = launchResult(it)) }
            .onFailure { reply(context, request, false, error = it.message ?: "no app can open uri") }
    }

    private fun launchPicker(context: Context, request: Intent, directory: Boolean) {
        val callback = callback(request)
        val caller = Hg2ApiReplies.callerPackageOf(callback)
        launchOrNotify(context, Intent(context, Hg2ApiPickerActivity::class.java).apply {
            putExtra(EXTRA_PICK_DIRECTORY, directory)
            putExtra(EXTRA_MIME, request.getStringExtra(EXTRA_MIME))
            putExtra(EXTRA_REQUEST_ID, request.getStringExtra(EXTRA_REQUEST_ID))
            callback?.let { putExtra(EXTRA_REPLY, it) }
        }, if (directory) "Pick a folder for an app" else "Pick a file for an app", caller)
    }

    /**
     * Android 10+ hands a background app an empty clip instead of an error, so a read is only
     * attempted - and only reported as a success - when it can really see the clipboard.
     */
    private fun clipboardGet(context: Context, request: Intent) {
        if (!clipboardReadPossible(Build.VERSION.SDK_INT, isAppInForeground(context))) {
            return reply(context, request, false, error = "clipboard reads need HG2Gui in the foreground (Android 10+ blocks background reads)")
        }
        val service = clipboard(context) ?: return reply(context, request, false, error = "clipboard unavailable")
        val text = service.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        reply(context, request, true, data = text)
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
        ACTION_EXECUTE, ACTION_PACKAGE, ACTION_LIFECYCLE, ACTION_AUTHORITY -> "command-result"
        ACTION_DEVICE_INFO -> "device-info"
        ACTION_CLIPBOARD_GET -> "text"
        ACTION_PICK_FILE, ACTION_PICK_DIRECTORY -> "uri"
        else -> "ack"
    }

    private fun clipboard(context: Context): ClipboardManager? = context.getSystemService(ClipboardManager::class.java)

    private fun callback(request: Intent): PendingIntent? = Hg2ApiReplies.callbackOf(request)

    private fun reply(context: Context, request: Intent, success: Boolean, data: String? = null, error: String? = null) {
        val callback = callback(request)
        Hg2ApiAuditStore.record(
            context = context,
            action = request.action.orEmpty(),
            callerPackage = Hg2ApiReplies.callerPackageOf(callback),
            success = success,
            error = error,
            phase = Hg2ApiAuditStore.PHASE_RESULT,
            requestId = request.getStringExtra(EXTRA_REQUEST_ID)
        )
        Hg2ApiReplies.send(context, callback, resultType(request.action), success, data, error)
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

        /** Internal: id the receiver stamps on each request so its audit rows (and the activities' decision rows) correlate. */
        const val EXTRA_REQUEST_ID = "request_id"

        /** Internal: creator package of the caller's reply PendingIntent, passed on to the approval activity. */
        const val EXTRA_CALLER_PACKAGE = "caller_package"
        const val EXTRA_PICK_DIRECTORY = "pick_directory"
        private val SAFE_WORD = Regex("[A-Za-z0-9_.+-]+")
        private val SAFE_PACKAGE = Regex("[A-Za-z0-9@._+=][A-Za-z0-9@._+=-]*")
        private const val CHANNEL = "hg2gui-api"
        private const val MAX_OUTPUT = 128_000
        private const val MAX_DETAIL_FIELD = 80

        /** Without a foreground service the process lives about as long as the broadcast (10s foreground / 60s background); stay inside the short end. */
        private const val BROADCAST_BUDGET_MS = 9_000L

        /** A command holding the foreground service open this long is treated as runaway. */
        private const val FOREGROUND_SERVICE_CAP_MS = 60L * 60 * 1000
        private const val DEFAULT_METADATA_TTL = 30_000L
        private const val PROMPT_NOTIFICATION_BASE = 0x4A000
        @Volatile private var nextPromptId = 0
        private val LONG_RUNNING_ACTIONS = setOf(ACTION_EXECUTE, ACTION_PACKAGE, ACTION_LIFECYCLE)
    }
}
