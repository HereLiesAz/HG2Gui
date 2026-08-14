package com.hereliesaz.hg2gui.terminal

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hereliesaz.hg2gui.PermissionCodes
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.managers.flashlight.TorchManager
import com.hereliesaz.hg2gui.util.CalculationEngine
import com.hereliesaz.hg2gui.util.libsuperuser.Shell
import java.io.File

/**
 * The fixed set of commands the real Termux shell has no path to: Android system toggles
 * (there is no shell binary for airplane mode), the sandboxed VFS, and the in-app editor.
 * Dispatched by verb, no reflection - there are exactly these eleven, by design, not by convention.
 */
object Builtins {

    val NAMES = setOf(
        "wifi", "bluetooth", "airplane", "flash", "volume", "brightness",
        "call", "contacts", "vfs", "edit", "calc"
    )

    fun run(context: Context, line: String): String {
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val verb = parts.firstOrNull() ?: return ""
        val args = parts.drop(1)

        return when (verb) {
            "wifi" -> wifi(context)
            "bluetooth" -> bluetooth(context)
            "airplane" -> airplane(context)
            "flash" -> flash(context)
            "volume" -> volume(context, args)
            "brightness" -> brightness(context, args)
            "call" -> call(context, args)
            "contacts" -> contacts(context, args)
            "vfs" -> vfs(context, args)
            "edit" -> edit(context, args)
            "calc" -> calc(args)
            else -> "Unknown builtin: $verb"
        }
    }

    /** Releases the resources this dispatcher lazily created - call once, from the owning
     *  Activity's teardown. */
    fun destroy(context: Context) {
        ContactManagerHolder.instance?.destroy(context)
    }

    // --- System toggles -----------------------------------------------------------------

    private fun wifi(context: Context): String {
        val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val enabled = manager.isWifiEnabled
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // setWifiEnabled() is a silent no-op for third-party apps from Q onward - the quick
            // panel is the only thing that actually changes anything.
            context.startActivity(Intent(Settings.Panel.ACTION_WIFI))
            "Wi-Fi is currently ${if (enabled) "ON" else "OFF"} — opening the quick panel to change it."
        } else {
            @Suppress("DEPRECATION")
            manager.isWifiEnabled = !enabled
            "Wi-Fi ${if (!enabled) "enabled" else "disabled"}"
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun bluetooth(context: Context): String {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return "This device has no Bluetooth adapter."

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PermissionCodes.COMMAND_REQUEST_PERMISSION
            )
            return "Requesting Bluetooth permission — run \"bluetooth\" again once granted."
        }

        return if (adapter.isEnabled) {
            // No public disable() for third-party apps since API 33 - settings is the only path.
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            "Bluetooth is on — opening settings to turn it off."
        } else {
            context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            "Requesting to enable Bluetooth…"
        }
    }

    private fun airplane(context: Context): String {
        // No third-party app can flip this directly on any supported version - settings is not
        // a fallback here, it's the only real option.
        context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
        return "Opening airplane mode settings…"
    }

    private fun flash(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(Manifest.permission.CAMERA), PermissionCodes.COMMAND_REQUEST_PERMISSION
            )
            return "Requesting camera permission — run \"flash\" again once granted."
        }
        TorchManager.getInstance().toggle(context)
        return "Flashlight ${if (TorchManager.getInstance().isOn) "on" else "off"}"
    }

    // --- Volume / brightness -------------------------------------------------------------

    private val VOLUME_STREAMS = mapOf(
        "call" to AudioManager.STREAM_VOICE_CALL,
        "system" to AudioManager.STREAM_SYSTEM,
        "ring" to AudioManager.STREAM_RING,
        "media" to AudioManager.STREAM_MUSIC,
        "alarm" to AudioManager.STREAM_ALARM,
        "notification" to AudioManager.STREAM_NOTIFICATION
    )

    private val RINGER_PROFILES = mapOf(
        "normal" to AudioManager.RINGER_MODE_NORMAL,
        "vibrate" to AudioManager.RINGER_MODE_VIBRATE,
        "silent" to AudioManager.RINGER_MODE_SILENT
    )

    /** Returns true if Do Not Disturb access is already granted; otherwise sends the user to
     *  the settings screen that grants it and returns false. */
    private fun hasNotificationPolicyAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) return true
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        return false
    }

    private fun volume(context: Context, args: List<String>): String {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        fun levelOf(name: String, stream: Int): String {
            val pct = manager.getStreamVolume(stream) * 100 / manager.getStreamMaxVolume(stream)
            return "$name: $pct%"
        }

        return when (args.getOrNull(0)?.lowercase()) {
            null, "get" -> {
                val which = args.getOrNull(1)?.lowercase()
                if (which == null) {
                    VOLUME_STREAMS.entries.joinToString("\n") { (name, stream) -> levelOf(name, stream) }
                } else {
                    val stream = VOLUME_STREAMS[which]
                        ?: return "Unknown stream \"$which\". Use one of: ${VOLUME_STREAMS.keys.joinToString()}"
                    levelOf(which, stream)
                }
            }
            "set" -> {
                val usage = "Usage: volume set <${VOLUME_STREAMS.keys.joinToString("|")}> <0-100>"
                val which = args.getOrNull(1)?.lowercase() ?: return usage
                val stream = VOLUME_STREAMS[which]
                    ?: return "Unknown stream \"$which\". Use one of: ${VOLUME_STREAMS.keys.joinToString()}"
                val percent = args.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 100) ?: return usage
                if (stream == AudioManager.STREAM_RING && !hasNotificationPolicyAccess(context)) {
                    return "Ring volume needs Do Not Disturb access — opening settings to grant it."
                }
                val target = percent * manager.getStreamMaxVolume(stream) / 100
                manager.setStreamVolume(stream, target, 0)
                levelOf(which, stream)
            }
            "profile" -> {
                if (!hasNotificationPolicyAccess(context)) {
                    return "Changing the ringer profile needs Do Not Disturb access — opening settings to grant it."
                }
                val name = args.getOrNull(1)?.lowercase()
                    ?: return "Usage: volume profile <${RINGER_PROFILES.keys.joinToString("|")}>"
                val mode = RINGER_PROFILES[name]
                    ?: return "Unknown profile \"$name\". Use one of: ${RINGER_PROFILES.keys.joinToString()}"
                manager.ringerMode = mode
                "Ringer profile: $name"
            }
            else -> "Usage: volume [get [stream]] | set <stream> <0-100> | profile <normal|vibrate|silent>"
        }
    }

    private fun brightness(context: Context, args: List<String>): String {
        if (!Settings.System.canWrite(context)) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            )
            return "Requesting permission to change display settings — run this again once granted."
        }

        val resolver = context.contentResolver
        val activity = context as? Activity

        if (args.getOrNull(0)?.lowercase() == "auto") {
            Settings.System.putInt(
                resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
            activity?.runOnUiThread {
                val lp = activity.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = lp
            }
            return "Brightness: auto"
        }

        val percent = args.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 100)
            ?: return "Usage: brightness <0-100> | auto"
        val level255 = percent * 255 / 100

        Settings.System.putInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, level255)

        activity?.runOnUiThread {
            val lp = activity.window.attributes
            // screenBrightness wants a 0f-1f fraction; SCREEN_BRIGHTNESS itself is 0-255. Mixing
            // the two scales up was the one real bug here - every level rendered as either
            // near-black or clipped to full.
            lp.screenBrightness = level255 / 255f
            activity.window.attributes = lp
        }
        return "Brightness: $percent%"
    }

    // --- Call / contacts -------------------------------------------------------------------

    private object ContactManagerHolder {
        @Volatile var instance: ContactManager? = null
        fun get(context: Context): ContactManager =
            instance ?: ContactManager(context).also { instance = it }
    }

    private fun call(context: Context, args: List<String>): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE),
                PermissionCodes.COMMAND_REQUEST_PERMISSION
            )
            return "Requesting call permissions — run \"call\" again once granted."
        }

        val query = args.joinToString(" ")
        if (query.isBlank()) return "Usage: call <name or number>"

        val number = if (query.any { it.isLetter() }) {
            ContactManagerHolder.get(context).findNumber(query) ?: return "No contact named \"$query\"."
        } else {
            query
        }

        val encoded = number.map { if (it == '#') Uri.encode("#") else it.toString() }.joinToString("")
        val uri = Uri.parse("tel:$encoded")

        return try {
            context.startActivity(Intent(Intent.ACTION_CALL, uri))
            "Calling $number…"
        } catch (e: SecurityException) {
            "No permission to place calls."
        }
    }

    private fun contacts(context: Context, args: List<String>): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(Manifest.permission.READ_CONTACTS), PermissionCodes.COMMAND_REQUEST_PERMISSION
            )
            return "Requesting contacts permission — run \"contacts\" again once granted."
        }

        val manager = ContactManagerHolder.get(context)
        val sub = args.getOrNull(0)?.lowercase() ?: "ls"
        val rest = args.drop(1).joinToString(" ")

        return when (sub) {
            "ls" -> manager.listNamesAndNumbers().joinToString("\n\n").ifBlank { "(no contacts)" }
            "add" -> {
                context.startActivity(
                    Intent(ContactsContract.Intents.Insert.ACTION).setType(ContactsContract.RawContacts.CONTENT_TYPE)
                )
                "Opening Contacts to add a new entry…"
            }
            "about" -> {
                if (rest.isBlank()) return "Usage: contacts about <number>"
                val about = manager.about(rest) ?: return "No contact with number $rest."
                "${about[ContactManager.NAME]}\n\t${about[ContactManager.NUMBERS]?.replace("\n", "\n\t")}\n" +
                    "Contacted ${about[ContactManager.TIME_CONTACTED]} time(s)"
            }
            "edit" -> {
                if (rest.isBlank()) return "Usage: contacts edit <number>"
                val uri = manager.fromPhone(rest) ?: return "No contact with number $rest."
                context.startActivity(Intent(Intent.ACTION_EDIT).setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE))
                "Opening editor for $rest…"
            }
            "rm" -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        context as Activity, arrayOf(Manifest.permission.WRITE_CONTACTS), PermissionCodes.COMMAND_REQUEST_PERMISSION
                    )
                    return "Requesting permission to edit contacts — run this again once granted."
                }
                if (rest.isBlank()) return "Usage: contacts rm <number>"
                if (manager.delete(rest)) "Removed $rest." else "No contact with number $rest."
            }
            else -> "Usage: contacts <ls|add|about|edit|rm> [number]"
        }
    }

    // --- VFS / editor ------------------------------------------------------------------------

    private const val VFS_USAGE = "Usage: vfs <ls|cd|pwd|mkdir|touch|cat|rm|mv|cp|mount> [args]"

    private fun vfs(context: Context, args: List<String>): String {
        val sub = args.getOrNull(0)?.lowercase() ?: return VFS_USAGE
        val rest = args.drop(1)
        return when (sub) {
            "ls" -> {
                val entries = VfsManager.list(context)
                if (entries.isEmpty()) "(empty)"
                else entries.joinToString("\n") { if (it.isDirectory) it.name + "/" else it.name }
            }
            "pwd" -> VfsManager.currentPath()
            "cd" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs cd <dir>"
                if (VfsManager.cd(context, name)) VfsManager.currentPath() else "No such directory: $name"
            }
            "mkdir" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs mkdir <name>"
                if (VfsManager.mkdir(context, name)) "Created $name" else "Could not create $name"
            }
            "touch" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs touch <name>"
                if (VfsManager.touch(context, name)) "Created $name" else "Could not create $name"
            }
            "cat" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs cat <name>"
                VfsManager.readText(context, name) ?: "No such file: $name"
            }
            "rm" -> {
                val name = rest.getOrNull(0) ?: return "Usage: vfs rm <name>"
                if (VfsManager.delete(context, name)) "Removed $name" else "Could not remove $name"
            }
            "mv" -> {
                if (rest.size < 2) return "Usage: vfs mv <from> <to>"
                if (VfsManager.move(context, rest[0], rest[1])) "Moved ${rest[0]} -> ${rest[1]}" else "Could not move"
            }
            "cp" -> {
                if (rest.size < 2) return "Usage: vfs cp <from> <to>"
                if (VfsManager.copy(context, rest[0], rest[1])) "Copied ${rest[0]} -> ${rest[1]}" else "Could not copy"
            }
            "mount" -> vfsMount(context, rest.getOrNull(0))
            else -> VFS_USAGE
        }
    }

    private fun vfsMount(context: Context, target: String?): String {
        if (!Shell.SU.available()) return "Root not available — vfs mount requires a rooted device (su)."
        if (target == null) return "Usage: vfs mount <real-path>"
        val root = VfsManager.init(context)
        val result = Shell.SU.run("mkdir -p '$target' && mount --bind '${root.absolutePath}' '$target'")
        return if (result != null) "Mounted ${root.absolutePath} at $target"
        else "Mount failed — check that su granted access and the target path is valid."
    }

    /** `edit`'s argument is a real filesystem path (from the Shell/file… picker, or the Files
     *  screen's own [VfsManager]-resolved absolute path) - not a name relative to the VFS
     *  sandbox, so this takes it literally rather than resolving it through [VfsManager]. */
    private fun edit(context: Context, args: List<String>): String {
        val path = args.joinToString(" ").trim()
        if (path.isBlank()) return "Usage: edit <file>"

        val file = File(path)
        if (file.isDirectory) return "\"$path\" is a directory."
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }

        val intent = Intent().setClassName(context, "com.hereliesaz.hg2gui.EditorActivity")
        intent.putExtra("path", file.absolutePath)
        context.startActivity(intent)
        return "Opening ${file.name}…"
    }

    private fun calc(args: List<String>): String {
        val expression = args.joinToString(" ")
        if (expression.isBlank()) return "Usage: calc <expression>"
        return try {
            CalculationEngine.eval(expression).toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
