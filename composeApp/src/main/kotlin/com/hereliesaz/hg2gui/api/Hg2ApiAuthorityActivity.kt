package com.hereliesaz.hg2gui.api

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.hereliesaz.hg2gui.LongTaskService
import com.hereliesaz.hg2gui.mcp.CommandOutputAccumulator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground approval boundary for API requests that ask for ADB-shell or root authority.
 *
 * What this screen guarantees:
 *  - it shows the command that will run, verbatim and complete, in a scrollable bounded body
 *    (Hg2ApiReceiver already refused any command the engine could not carry unchanged - see
 *    [Hg2ApiAuthorityCommand]) - and RUN only auto-answers the engine's own confirmation prompt when
 *    that prompt names exactly this command;
 *  - it names the requesting app (the creator of the reply PendingIntent, recorded by the system);
 *  - taps are dropped while another window covers the dialog (filterTouchesWhenObscured);
 *  - the caller always gets a reply: RUN, CANCEL, back, tap-away and leaving the screen (this
 *    activity is `noHistory`, so navigating away finishes it) each answer with a denial unless a
 *    decision was already made;
 *  - an approved command runs in a foreground service independent of this screen, so leaving the
 *    screen never cancels a root/ADB command that is already running.
 * Every decision and result is written to the API audit log under the request's id.
 */
class Hg2ApiAuthorityActivity : Activity() {
    private val decided = AtomicBoolean(false)
    private var dialog: AlertDialog? = null
    private var authority = ""
    private var command = ""
    private var requestId: String? = null
    private var callerPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authority = intent.getStringExtra(Hg2ApiReceiver.EXTRA_AUTHORITY).orEmpty().lowercase()
        command = intent.getStringExtra(Hg2ApiReceiver.EXTRA_COMMAND).orEmpty().trim()
        requestId = intent.getStringExtra(Hg2ApiReceiver.EXTRA_REQUEST_ID)
        callerPackage = Hg2ApiReplies.callerPackageOf(Hg2ApiReplies.callbackOf(intent))
            ?: intent.getStringExtra(Hg2ApiReceiver.EXTRA_CALLER_PACKAGE)
        val line = Hg2ApiAuthorityCommand.lossless(authority, command)
        if (line == null) {
            deny("invalid authority request")
            return
        }

        val built = AlertDialog.Builder(this)
            .setTitle(if (authority == "root") "RUN AS ROOT?" else "RUN THROUGH ADB SHELL?")
            .setView(buildBody())
            .setPositiveButton("RUN") { _, _ -> approve(line) }
            .setNegativeButton("CANCEL") { _, _ -> deny("authority request denied") }
            .setOnCancelListener { deny("authority request denied") }
            .create()
        built.setCanceledOnTouchOutside(false)
        built.show()
        // A window drawn over this one (a tapjacking overlay, on API < 31 where overlays may stay
        // touchable) must not be able to deliver the RUN tap: drop touches while obscured.
        built.window?.decorView?.filterTouchesWhenObscured = true
        built.getButton(AlertDialog.BUTTON_POSITIVE)?.filterTouchesWhenObscured = true
        built.getButton(AlertDialog.BUTTON_NEGATIVE)?.filterTouchesWhenObscured = true
        dialog = built
    }

    /** Requester, authority, then the exact command in a monospace body that scrolls instead of growing off-screen. */
    private fun buildBody(): View {
        val pad = dp(20)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(8), pad, 0)
            filterTouchesWhenObscured = true
        }
        column.addView(TextView(this).apply {
            text = requesterLine()
            setTypeface(typeface, Typeface.BOLD)
        })
        column.addView(TextView(this).apply {
            text = if (authority == "root") "This command will run as root:" else "This command will run through the ADB shell:"
            setPadding(0, dp(8), 0, dp(4))
        })
        val scroll = BoundedScrollView(this, (resources.displayMetrics.heightPixels * MAX_BODY_HEIGHT_FRACTION).toInt()).apply {
            isFillViewport = false
        }
        scroll.addView(TextView(this).apply {
            text = command
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(false)
        })
        column.addView(scroll)
        return column
    }

    private fun requesterLine(): String {
        val pkg = callerPackage ?: return "Requested by: unknown app (the request carried no reply channel)"
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
        return if (label != null && label != pkg) "Requested by: $label ($pkg)" else "Requested by: $pkg"
    }

    private fun approve(line: String) {
        if (!decided.compareAndSet(false, true)) return
        val appContext = applicationContext
        val callback = Hg2ApiReplies.callbackOf(intent)
        val request = Request(authority, command, line, callback, requestId, callerPackage)
        audit(appContext, request, phase = Hg2ApiAuditStore.PHASE_DECISION, success = true, error = null)
        // Run independent of this screen. The activity is noHistory and finishes the moment the user
        // leaves it; an approved root/ADB command must not die with it. The foreground service keeps
        // the process alive; if the platform refuses it, a process-wide scope is the best that is left.
        val work: suspend () -> Unit = { runApproved(appContext, request) }
        if (!LongTaskService.launch(appContext, "HG2Gui ${request.authority} command", work)) {
            processScope.launch { work() }
        }
        finish()
    }

    private fun deny(reason: String) {
        if (!decided.compareAndSet(false, true)) {
            finishQuietly()
            return
        }
        val appContext = applicationContext
        val request = Request(authority, command, "", Hg2ApiReplies.callbackOf(intent), requestId, callerPackage)
        audit(appContext, request, phase = Hg2ApiAuditStore.PHASE_DECISION, success = false, error = reason)
        Hg2ApiReplies.send(appContext, request.callback, RESULT_TYPE, success = false, error = reason)
        audit(appContext, request, phase = Hg2ApiAuditStore.PHASE_RESULT, success = false, error = reason)
        finishQuietly()
    }

    private fun finishQuietly() {
        runOnUiThread { if (!isFinishing) finish() }
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        // noHistory: leaving the screen (home, recents, another app) destroys it with no button
        // pressed. Without this the caller never heard back. A rotation recreates the dialog instead.
        if (!isChangingConfigurations) deny("authority request dismissed without a decision")
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** Everything the background run needs, captured so it never touches this (possibly destroyed) activity. */
    private class Request(
        val authority: String,
        val command: String,
        val line: String,
        val callback: PendingIntent?,
        val requestId: String?,
        val callerPackage: String?
    )

    /** A ScrollView that never grows taller than [maxHeightPx], so a long command scrolls inside the dialog. */
    private class BoundedScrollView(context: Context, private val maxHeightPx: Int) : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST))
        }
    }

    companion object {
        private const val MAX_OUTPUT = 128_000
        private const val MAX_BODY_HEIGHT_FRACTION = 0.5
        private const val RESULT_TYPE = "command-result"
        private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun audit(context: Context, request: Request, phase: String, success: Boolean, error: String?) {
            Hg2ApiAuditStore.record(
                context = context,
                action = Hg2ApiReceiver.ACTION_AUTHORITY,
                callerPackage = request.callerPackage,
                success = success,
                error = error,
                phase = phase,
                requestId = request.requestId,
                command = request.command,
                detail = "authority=${request.authority} command=${auditPreview(request.command)}"
            )
        }

        /**
         * Executes the approved command and replies. The engine asks its own confirmation before
         * running (`Run as root? <command> [y/N]`); that is answered "y" only when the prompt names
         * exactly the command the user approved. Any other prompt - including a confirmation for a
         * different command string - is answered "" and the run is reported as refused.
         */
        private suspend fun runApproved(context: Context, request: Request) {
            val engine = interactiveEngine(context)
            val output = CommandOutputAccumulator(MAX_OUTPUT)
            var exitCode: Int? = null
            var refusedMismatch = false
            val expected = Hg2ApiAuthorityCommand.expectedPrompt(request.authority, request.command).trim()
            try {
                engine.run(
                    request.line,
                    onNeedInput = { prompt ->
                        val t = prompt.trim()
                        when {
                            t == expected -> "y"
                            // hg2auth's own confirmation for some OTHER command text: never auto-accept.
                            t.startsWith("Run as root?", ignoreCase = true) ||
                                t.startsWith("Run through ADB shell?", ignoreCase = true) -> {
                                refusedMismatch = true
                                ""
                            }
                            // A nested interactive program's own prompt: no human is answering it here.
                            else -> ""
                        }
                    },
                    onExit = { exitCode = it },
                    onStyledOutput = { output.markFinalSnapshot() }
                ).collect { chunk -> output.add(chunk) }
                if (refusedMismatch) {
                    reportResult(context, request, success = false, data = null, error = "refused: the command the engine prepared differs from the one approved")
                } else {
                    reportResult(
                        context, request,
                        success = exitCode == 0,
                        data = JSONObject().put("output", output.text()).put("exitCode", exitCode).toString(),
                        error = if (exitCode == 0) null else "elevated command exited with ${exitCode ?: -1}"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                reportResult(context, request, success = false, data = null, error = e.message ?: "authority execution failed")
            } finally {
                engine.destroy()
            }
        }

        private fun reportResult(context: Context, request: Request, success: Boolean, data: String?, error: String?) {
            audit(context, request, phase = Hg2ApiAuditStore.PHASE_RESULT, success = success, error = error)
            Hg2ApiReplies.send(context, request.callback, RESULT_TYPE, success, data, error)
        }
    }
}
