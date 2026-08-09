package com.hereliesaz.hg2gui.mcp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.hereliesaz.hg2gui.TerminalActivity
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom

private const val PREFS_NAME = "hg2gui_mcp_prefs"
private const val PREF_SHELL_EXEC_ENABLED = "shell_exec_enabled"
private const val CHANNEL_ID = "mcp_server"
private const val NOTIFICATION_ID = 4201
private const val DEFAULT_PORT = 4827

/**
 * A loopback-only JSON-RPC 2.0 server (newline-delimited, matching MCP's own stdio framing) an
 * external AI agent can pair with to drive HG2Gui's sandboxed filesystem and, once separately
 * enabled, its real shell - see McpJsonRpc/McpTools for the protocol and tool surface, and
 * McpServerScreen for the UI this is started/stopped from. Foreground so the socket survives
 * backgrounding while running; started only by explicit user action, never on its own.
 */
class McpServerService : Service() {

    companion object {
        val isRunning = MutableStateFlow(false)
        // Memory-only: regenerated fresh on every server start, never persisted or logged - a
        // loopback socket is reachable by any app on the device, not just via `adb forward`, so
        // this token is load-bearing against other installed apps, not just network defense.
        val token = MutableStateFlow<String?>(null)
        val port = MutableStateFlow(DEFAULT_PORT)

        private var shellExecFlow: MutableStateFlow<Boolean>? = null

        /** Must be called once (TerminalActivity.onCreate) before any UI reads [shellExecEnabled]
         *  or McpTools is constructed, so the persisted value is loaded before first use. */
        fun ensureInitialized(context: Context) {
            if (shellExecFlow == null) {
                shellExecFlow = MutableStateFlow(readShellExecPref(context))
            }
        }

        val shellExecEnabled: StateFlow<Boolean>
            get() = shellExecFlow?.asStateFlow() ?: MutableStateFlow(false).asStateFlow()

        /** Disabling never needs the biometric gate McpServerScreen applies to enabling - turning
         *  capability off is always safe to do immediately. */
        fun setShellExecEnabled(context: Context, enabled: Boolean) {
            ensureInitialized(context)
            shellExecFlow?.value = enabled
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean(PREF_SHELL_EXEC_ENABLED, enabled)
            }
        }

        private fun readShellExecPref(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_SHELL_EXEC_ENABLED, false)

        const val ACTION_START = "com.hereliesaz.hg2gui.mcp.START"
        const val ACTION_STOP = "com.hereliesaz.hg2gui.mcp.STOP"
        /** Set on the notification's tap intent; TerminalActivity reads this to land on the MCP
         *  screen instead of the terminal when the user taps the running-server notification. */
        const val EXTRA_OPEN_MCP = "open_mcp"

        private fun generateToken(): String {
            val bytes = ByteArray(24)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private var engine: TerminalEngine? = null
    private var tools: McpTools? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureInitialized(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServer()
            stopSelf()
        } else {
            startServer()
        }
        return START_NOT_STICKY
    }

    private fun startServer() {
        if (isRunning.value) return

        val generatedToken = generateToken()
        val builtEngine = TerminalEngine(applicationContext)
        engine = builtEngine
        tools = McpTools(applicationContext, shellExecEnabled, builtEngine)

        startForeground(NOTIFICATION_ID, buildNotification())

        scope.launch {
            try {
                val socket = ServerSocket(port.value, 0, InetAddress.getByName("127.0.0.1"))
                serverSocket = socket
                token.value = generatedToken
                isRunning.value = true
                while (isRunning.value) {
                    val client = try {
                        socket.accept()
                    } catch (e: Exception) {
                        break
                    }
                    handleClient(client, generatedToken)
                }
            } catch (e: Exception) {
                // Most likely the port's already taken - fail closed, nothing partially running.
                isRunning.value = false
                token.value = null
            }
        }
    }

    /** One client at a time for v1: the accept loop above calls this inline rather than
     *  launching it concurrently, so a second connection simply queues at the OS accept backlog
     *  until this one closes. */
    private suspend fun handleClient(client: Socket, expectedToken: String) {
        client.use { socket ->
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            // The pairing token rides as one bespoke line before any JSON-RPC traffic - layered
            // under MCP's own newline-delimited framing rather than baked into it, so a stock
            // stdio MCP client speaking plain JSON-RPC still works once paired.
            val authLine = reader.readLine() ?: return
            val authorized = try {
                val obj = McpJsonRpc.json.parseToJsonElement(authLine) as? JsonObject
                (obj?.get("token") as? JsonPrimitive)?.content == expectedToken
            } catch (e: Exception) {
                false
            }

            if (!authorized) {
                writer.write("{\"authorized\":false}\n")
                writer.flush()
                return
            }
            writer.write("{\"authorized\":true}\n")
            writer.flush()

            val currentTools = tools ?: return
            while (isRunning.value) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue
                val response = McpJsonRpc.handleLine(line, currentTools)
                writer.write(response)
                writer.write("\n")
                writer.flush()
            }
        }
    }

    private fun stopServer() {
        isRunning.value = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Already closed or never opened - nothing to clean up.
        }
        serverSocket = null
        engine?.destroy()
        engine = null
        tools = null
        token.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopServer()
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "MCP server", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, TerminalActivity::class.java).apply {
            putExtra(EXTRA_OPEN_MCP, true)
        }
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HG2Gui MCP server running")
            .setContentText("Loopback only — port ${port.value}")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }
}
