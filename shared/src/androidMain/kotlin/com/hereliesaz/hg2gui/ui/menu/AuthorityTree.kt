package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
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
            listOf(
                MenuNode("authority/adb/devices", "Devices", value = "hg2auth adb devices"),
                MenuNode("authority/adb/pair", "Pair…", cap = "host:port + code", value = "hg2auth adb pair"),
                MenuNode("authority/adb/connect", "Connect…", cap = "host:port", value = "hg2auth adb connect"),
                MenuNode("authority/adb/disconnect", "Disconnect", value = "hg2auth adb disconnect"),
                MenuNode("authority/adb/shell", "Shell command…", cap = "elevated", value = "hg2auth adb shell")
            )
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
}
