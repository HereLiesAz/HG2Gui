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
// MCP-2: any app with just INTERNET can open the loopback socket and send nothing - without a
// read timeout, the pre-auth readLine() blocks forever, and since v1 handles one client at a
// time, that one silent connection wedges the server shut for everyone else permanently.
private const val SOCKET_READ_TIMEOUT_MS = 15_000
// MCP-3: readLine() has no size cap of its own - an attacker-controlled line with no newline
// would otherwise buffer unbounded in memory, on the same pre-auth path, before the token is
// ever checked.
private const val MAX_LINE_CHARS = 1 shl 16

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
    // MCP-4: stopServer() only ever closed the listening socket, never the one client actually
    // connected at the time - its in-flight readLine() could still resolve and dispatch one more
    // tool call after the user had already tapped off.
    private var currentClient: Socket? = null
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

    // MCP-9: isRunning only flips true once the bind coroutine actually runs, on Dispatchers.IO -
    // onStartCommand itself runs synchronously on the main thread, so a second START intent
    // (a double-tap on the UI toggle) can land before that coroutine has had a chance to set it,
    // sail past the isRunning check below, and build a second TerminalEngine that immediately
    // loses the port race. This flag closes that window without waiting on the coroutine at all.
    private var starting = false

    private fun startServer() {
        if (isRunning.value || starting) return
        starting = true

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
                starting = false
                while (isRunning.value) {
                    val client = try {
                        socket.accept()
                    } catch (e: Exception) {
                        break
                    }
                    currentClient = client
                    try {
                        // One bad client (a malformed line, a timeout) must only cost that one
                        // connection - letting it escape here used to fall into the outer catch
                        // below, which tears the whole server down as though the bind itself had
                        // failed.
                        handleClient(client, generatedToken)
                    } catch (e: Exception) {
                        // Already logged nothing, intentionally - a client-triggered exception
                        // isn't an operator error worth surfacing, just a connection to drop.
                    } finally {
                        currentClient = null
                    }
                }
            } catch (e: Exception) {
                // MCP-5: the engine/tools were already constructed above - a bind failure (most
                // likely the port already taken) must not leave that shell alive with nothing
                // pointing at it, and must not leave the "running" notification up for a server
                // that never actually started.
                isRunning.value = false
                starting = false
                token.value = null
                engine?.destroy()
                engine = null
                tools = null
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    /** One client at a time for v1: the accept loop above calls this inline rather than
     *  launching it concurrently, so a second connection simply queues at the OS accept backlog
     *  until this one closes. */
    private suspend fun handleClient(client: Socket, expectedToken: String) {
        // A connection that never sends anything - the whole point of MCP-2 - now times out
        // instead of blocking the single-client accept loop forever.
        client.soTimeout = SOCKET_READ_TIMEOUT_MS
        client.use { socket ->
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            // The pairing token rides as one bespoke line before any JSON-RPC traffic - layered
            // under MCP's own newline-delimited framing rather than baked into it, so a stock
            // stdio MCP client speaking plain JSON-RPC still works once paired.
            val authLine = reader.readLineBounded(MAX_LINE_CHARS) ?: return
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
                val line = reader.readLineBounded(MAX_LINE_CHARS) ?: break
                if (line.isBlank()) continue
                val response = McpJsonRpc.handleLine(line, currentTools)
                writer.write(response)
                writer.write("\n")
                writer.flush()
            }
        }
    }

    /** Same contract as [BufferedReader.readLine] - null at end of stream, otherwise the next
     *  line with its terminator stripped - except a line longer than [maxChars] throws instead of
     *  growing an internal buffer without limit; the caller (an unauthenticated socket, in the
     *  worst case) controls how much gets sent before a newline ever arrives. */
    private fun BufferedReader.readLineBounded(maxChars: Int): String? {
        val sb = StringBuilder()
        while (true) {
            val c = read()
            if (c == -1) return if (sb.isEmpty()) null else sb.toString()
            if (c == '\n'.code) return sb.toString()
            if (c != '\r'.code) sb.append(c.toChar())
            if (sb.length > maxChars) throw java.io.IOException("line exceeds $maxChars chars")
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
        try {
            currentClient?.close()
        } catch (e: Exception) {
            // Already closed - nothing to clean up.
        }
        currentClient = null
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
