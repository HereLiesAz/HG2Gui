package com.hereliesaz.hg2gui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
 * Foreground service that keeps the process alive while a long piece of work started from a
 * BroadcastReceiver (an app-update download, a Hg2Api command) runs to completion. A receiver's
 * own goAsync() window is ~10s and a bare Thread gets nothing - the process can be reaped
 * mid-download. Work is handed over in-process via [launch]; the service stops itself once no
 * task remains.
 */
class LongTaskService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(EXTRA_TASK_ID, -1) ?: -1
        val task = tasks.remove(id)
        ensureChannel(this)
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(task?.title ?: "HG2Gui")
                .setContentText("Working in the background…")
                .setOngoing(true)
                .build()
        )
        if (task == null) {
            if (active.get() == 0) stopSelf(startId)
            return START_NOT_STICKY
        }
        active.incrementAndGet()
        scope.launch {
            try {
                task.block()
            } catch (e: Exception) {
                Log.w(TAG, "background task '${task.title}' failed", e)
            } finally {
                if (active.decrementAndGet() == 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private class Task(val title: String, val block: suspend () -> Unit)

    companion object {
        private const val TAG = "LongTaskService"
        private const val CHANNEL_ID = "hg2gui_background_work"
        private const val NOTIFICATION_ID = 2101
        private const val EXTRA_TASK_ID = "task_id"
        private val nextId = AtomicInteger(1)
        private val tasks = ConcurrentHashMap<Int, Task>()

        /**
         * Runs [block] inside this foreground service. Returns false - without running it - when
         * the platform refuses a foreground-service start right now (Android 12+ background-start
         * limits), so the caller can fall back to its own goAsync()/thread path.
         */
        fun launch(context: Context, title: String, block: suspend () -> Unit): Boolean {
            val id = nextId.getAndIncrement()
            tasks[id] = Task(title, block)
            val intent = Intent(context, LongTaskService::class.java).putExtra(EXTRA_TASK_ID, id)
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
                true
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException / IllegalStateException.
                tasks.remove(id)
                false
            }
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
