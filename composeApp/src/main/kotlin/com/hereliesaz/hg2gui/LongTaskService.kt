package com.hereliesaz.hg2gui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground service that keeps the process alive - and the CPU awake, through a partial wake
 * lock - while long work runs. Two kinds of work hold it up:
 *
 * - **Tasks** started from a BroadcastReceiver (an app-update download, a Hg2Api command). A
 *   receiver's own goAsync() window is ~10s and a bare Thread gets nothing - the process can be
 *   reaped mid-download. Work is handed over in-process via [launch].
 * - **Interactive commands**: while any terminal tab has a command running (or a full-screen
 *   program is open), the UI reports [setInteractiveBusy]. Without this a `git clone`, a build or
 *   a `pkg install` is a plain child of a process that Android 12+ may freeze or phantom-kill the
 *   moment the app leaves the foreground - and a killed `pkg install` leaves dpkg half-mutated.
 *
 * The service stops itself once no task remains and the UI reports idle. If the user has denied
 * POST_NOTIFICATIONS the service still runs - only the notification is hidden - so nothing here
 * depends on that permission being granted.
 */
class LongTaskService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // All of the below is guarded by [lock]. Every decision to stop is made while holding it, and
    // stops with the latest start id, so a start request that races a stop either is seen here
    // (active > 0 / interactive) or makes the platform ignore the stop (m-2).
    private var active = 0
    private var latestStartId = 0
    private var stopping = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentTitle: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_TASK_ID, -1) ?: -1
        val task = tasks.remove(id)
        ensureChannel(this)
        synchronized(lock) {
            instance = this
            stopping = false
            latestStartId = startId
            if (task != null) {
                active++
                currentTitle = task.title
            }
        }
        // A foreground-service start must reach startForeground() even if this turns out to be a
        // stale start with nothing to do - skipping it is a platform crash.
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // ForegroundServiceStartNotAllowedException / type errors: give up cleanly rather
            // than crash (an unanswered foreground start is a crash of its own). The task, if
            // any, is dropped - the start itself was accepted, so launch()'s fallback cannot run.
            Log.w(TAG, "could not enter the foreground", e)
            synchronized(lock) {
                if (task != null) active--
                stopping = true
                stopSelf(startId)
            }
            return START_NOT_STICKY
        }
        synchronized(lock) { refreshLocked() }
        if (task != null) {
            scope.launch {
                try {
                    task.block()
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) { // arbitrary caller code must not kill the service
                    Log.w(TAG, "background task '${task.title}' failed", e)
                } finally {
                    synchronized(lock) {
                        active--
                        refreshLocked()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        synchronized(lock) {
            if (instance === this) instance = null
            releaseWakeLockLocked()
        }
        scope.cancel()
        super.onDestroy()
    }

    /** Re-evaluates whether the service should keep running; call with [lock] held. Takes the
     *  wake lock while there is work, and releases it and stops (with the latest start id, so a
     *  concurrently delivered start wins) once there is none. */
    private fun refreshLocked() {
        val wanted = active > 0 || interactiveWanted
        if (wanted) {
            acquireWakeLockLocked()
            // Keep the notification text in step with what is being held up.
            getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
        } else if (!stopping) {
            stopping = true
            releaseWakeLockLocked()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(latestStartId)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle((if (active > 0) currentTitle else null) ?: getString(R.string.app_name))
        .setContentText(
            getString(if (active > 0) R.string.background_work_running else R.string.background_command_running)
        )
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, TerminalActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun acquireWakeLockLocked() {
        val held = wakeLock
        if (held != null && held.isHeld) return
        try {
            val lock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            lock.setReferenceCounted(false)
            // A generous ceiling rather than none: a leaked lock must not drain the battery
            // forever, and anything running longer than this re-takes it on the next refresh.
            lock.acquire(WAKE_LOCK_TIMEOUT_MS)
            wakeLock = lock
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.w(TAG, "could not take the wake lock", e)
        }
    }

    private fun releaseWakeLockLocked() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.w(TAG, "could not release the wake lock", e)
        }
        wakeLock = null
    }

    private class Task(val title: String, val block: suspend () -> Unit)

    companion object {
        private const val TAG = "LongTaskService"
        private const val CHANNEL_ID = "hg2gui_background_work"
        private const val NOTIFICATION_ID = 2101
        private const val EXTRA_TASK_ID = "task_id"
        private const val WAKE_LOCK_TAG = "HG2Gui:LongTask"
        private const val WAKE_LOCK_TIMEOUT_MS = 6L * 60 * 60 * 1000
        private val nextId = AtomicInteger(1)
        private val tasks = ConcurrentHashMap<Int, Task>()

        // Guards every service-instance field above plus the two below. Static, so the activity's
        // "idle" report and the service's own stop decision are ordered against each other.
        private val lock = Any()
        private var instance: LongTaskService? = null
        private var interactiveWanted = false

        /**
         * Runs [block] inside this foreground service. Returns false - without running it - when
         * the platform refuses a foreground-service start right now (Android 12+ background-start
         * limits), so the caller can fall back to its own goAsync()/thread path.
         */
        fun launch(context: Context, title: String, block: suspend () -> Unit): Boolean {
            val id = nextId.getAndIncrement()
            tasks[id] = Task(title, block)
            return if (startService(context, Intent(context, LongTaskService::class.java).putExtra(EXTRA_TASK_ID, id))) {
                true
            } else {
                Log.w(TAG, "could not start long-task service for '$title'")
                tasks.remove(id)
                false
            }
        }

        /**
         * Reports whether any terminal tab has a command running. While true the service is
         * foreground and holds a partial wake lock; when it goes false the service stops (unless
         * a [launch]ed task still needs it). Cheap and idempotent - call it on every change.
         *
         * Returns false only when [busy] is true and the platform refused to start the service
         * (the app was in the background); the command then just runs unprotected, as before.
         * Reporting idle never starts anything, so it is safe from the background.
         */
        fun setInteractiveBusy(context: Context, busy: Boolean): Boolean {
            val running: LongTaskService?
            synchronized(lock) {
                interactiveWanted = busy
                // A service that already decided to stop cannot be relied on to stay up.
                running = instance?.takeIf { !it.stopping }
                if (running != null) running.refreshLocked()
            }
            if (!busy || running != null) return true
            return startService(context, Intent(context, LongTaskService::class.java))
        }

        private fun startService(context: Context, intent: Intent): Boolean = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
            true
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // ForegroundServiceStartNotAllowedException / IllegalStateException.
            Log.w(TAG, "foreground-service start refused", e)
            false
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Background work", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }
}
