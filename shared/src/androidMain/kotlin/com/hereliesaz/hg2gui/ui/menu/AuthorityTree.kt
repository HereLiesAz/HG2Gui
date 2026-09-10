package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.terminal.AdbEndpointStore
import com.hereliesaz.hg2gui.terminal.ExecutionAuthority

/** Explicit device-authority choices. Nothing here is inherited by ordinary/package commands. */
object AuthorityTree {
    fun root(context: Context): MenuNode = MenuNode(
        id = "authority",
        label = "Authority",
        cap = "explicit",
        emitsToken = false,
        resolveChildren = { children(context) }
    )

    private fun children(context: Context): List<MenuNode> {
        val availability = ExecutionAuthority.availability(context)
        val adbChildren = if (availability.adbAvailable) {
            buildList {
                add(MenuNode("authority/adb/devices", "Devices", value = "hg2auth adb devices"))
                add(MenuNode("authority/adb/pair", "Pair…", cap = "host:port + code", value = "hg2auth adb pair"))
                add(MenuNode("authority/adb/connect", "Connect…", cap = "host:port", value = "hg2auth adb connect"))
                add(
                    MenuNode(
                        id = "authority/adb/saved",
                        label = "Saved endpoints",
                        cap = AdbEndpointStore.list(context).size.toString(),
                        emitsToken = false,
                        resolveChildren = { savedEndpointNodes(context) }
                    )
                )
                add(MenuNode("authority/adb/disconnect", "Disconnect", value = "hg2auth adb disconnect"))
                add(
                    MenuNode(
                        id = "authority/adb/device",
                        label = "Device capabilities",
                        cap = "explicit",
                        children = adbCapabilityNodes(),
                        emitsToken = false
                    )
                )
                add(MenuNode("authority/adb/shell", "Raw shell command…", cap = "elevated", value = "hg2auth adb shell"))
            }
        } else {
            listOf(
                MenuNode(
                    id = "authority/adb/install",
                    label = "Install ADB tools",
                    cap = "android-tools",
                    value = "pkg install android-tools"
                ),
                MenuNode(
                    id = "authority/adb/help",
                    label = "Then pair Wireless Debugging",
                    cap = "required",
                    emitsToken = false
                )
            )
        }

        val rootChildren = if (availability.rootAvailable) {
            listOf(
                MenuNode("authority/root/test", "Test root", cap = "asks", value = "hg2auth root test"),
                MenuNode("authority/root/shell", "Root command…", cap = "asks", value = "hg2auth root shell")
            )
        } else {
            listOf(
                MenuNode(
                    id = "authority/root/none",
                    label = "No su provider detected",
                    cap = "unavailable",
                    emitsToken = false
                )
            )
        }

        return listOf(
            MenuNode(
                id = "authority/status",
                label = "Status",
                cap = "inspect",
                value = "hg2auth status"
            ),
            MenuNode(
                id = "authority/app",
                label = "App",
                cap = "default",
                children = listOf(
                    MenuNode(
                        id = "authority/app/info",
                        label = "Normal HG2Gui authority",
                        cap = "current",
                        emitsToken = false
                    )
                ),
                emitsToken = false
            ),
            MenuNode(
                id = "authority/adb",
                label = "ADB shell",
                cap = if (availability.adbAvailable) "available" else "setup",
                children = adbChildren,
                emitsToken = false
            ),
            MenuNode(
                id = "authority/root",
                label = "Root",
                cap = if (availability.rootAvailable) "available" else "unavailable",
                children = rootChildren,
                emitsToken = false
            )
        )
    }

    private fun savedEndpointNodes(context: Context): List<MenuNode> {
        val endpoints = AdbEndpointStore.list(context)
        if (endpoints.isEmpty()) {
            return listOf(MenuNode("authority/adb/saved/none", "No saved endpoints", "0", emitsToken = false))
        }
        return endpoints.map { endpoint ->
            val id = endpoint.replace(Regex("[^A-Za-z0-9._-]"), "_")
            MenuNode(
                id = "authority/adb/saved/$id",
                label = endpoint,
                cap = "remembered",
                children = listOf(
                    MenuNode(
                        id = "authority/adb/saved/$id/connect",
                        label = "Reconnect",
                        value = "hg2auth adb connect '$endpoint'"
                    ),
                    MenuNode(
                        id = "authority/adb/saved/$id/forget",
                        label = "Forget",
                        cap = "revoke",
                        value = "hg2auth adb forget '$endpoint'"
                    )
                ),
                emitsToken = false
            )
        } + MenuNode(
            id = "authority/adb/saved/forget-all",
            label = "Forget all endpoints",
            cap = "revoke",
            value = "hg2auth adb forget-all"
        )
    }

    private fun adbCapabilityNodes(): List<MenuNode> = listOf(
        MenuNode(
            id = "authority/adb/device/packages",
            label = "Packages",
            cap = "pm",
            children = listOf(
                MenuNode("authority/adb/device/packages/list", "List packages", value = "hg2auth adb shell pm list packages"),
                MenuNode("authority/adb/device/packages/path", "Package path…", value = "hg2auth adb shell pm path"),
                MenuNode("authority/adb/device/packages/permissions", "Permissions…", value = "hg2auth adb shell pm list permissions")
            ),
            emitsToken = false
        ),
        MenuNode(
            id = "authority/adb/device/activity",
            label = "Activities",
            cap = "am",
            children = listOf(
                MenuNode("authority/adb/device/activity/start", "Start…", value = "hg2auth adb shell am start"),
                MenuNode("authority/adb/device/activity/force-stop", "Force-stop…", value = "hg2auth adb shell am force-stop"),
                MenuNode("authority/adb/device/activity/broadcast", "Broadcast…", value = "hg2auth adb shell am broadcast")
            ),
            emitsToken = false
        ),
        MenuNode(
            id = "authority/adb/device/services",
            label = "System services",
            cap = "cmd",
            children = listOf(
                MenuNode("authority/adb/device/services/list", "List services", value = "hg2auth adb shell cmd -l"),
                MenuNode("authority/adb/device/services/call", "Service command…", value = "hg2auth adb shell cmd")
            ),
            emitsToken = false
        ),
        MenuNode(
            id = "authority/adb/device/settings",
            label = "Settings",
            cap = "settings",
            children = listOf(
                MenuNode("authority/adb/device/settings/list", "List namespace…", value = "hg2auth adb shell settings list"),
                MenuNode("authority/adb/device/settings/get", "Read value…", value = "hg2auth adb shell settings get"),
                MenuNode("authority/adb/device/settings/put", "Write value…", cap = "mutates", value = "hg2auth adb shell settings put")
            ),
            emitsToken = false
        ),
        MenuNode(
            id = "authority/adb/device/diagnostics",
            label = "Diagnostics",
            cap = "inspect",
            children = listOf(
                MenuNode("authority/adb/device/diagnostics/dumpsys", "dumpsys…", value = "hg2auth adb shell dumpsys"),
                MenuNode("authority/adb/device/diagnostics/logcat", "logcat", value = "hg2auth adb shell logcat")
            ),
            emitsToken = false
        )
    )
}
