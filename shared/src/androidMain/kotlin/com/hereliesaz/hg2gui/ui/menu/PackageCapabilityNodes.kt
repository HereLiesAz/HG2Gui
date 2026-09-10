package com.hereliesaz.hg2gui.ui.menu

import android.content.Context
import com.hereliesaz.hg2gui.terminal.PackageCapabilityPolicy
import com.hereliesaz.hg2gui.terminal.PackageLifecycleStore

/** Native package capability-policy controls backed by hg2package's interactive broker. */
object PackageCapabilityNodes {
    fun root(context: Context, pkg: PackageLifecycleStore.InstalledPackage): MenuNode = MenuNode(
        id = "packages/${pkg.key}/capabilities",
        label = "Capabilities",
        cap = "explicit",
        emitsToken = false,
        resolveChildren = {
            PackageCapabilityPolicy.Capability.entries.map { capability ->
                val mode = PackageCapabilityPolicy.mode(context, pkg.key, capability)
                val modeLabel = mode.name.lowercase().replace('_', '-')
                val prefix = "hg2package capability ${pkg.manager} ${shellQuote(pkg.name)} ${capability.wireName}"
                MenuNode(
                    id = "packages/${pkg.key}/capabilities/${capability.wireName}",
                    label = capability.wireName,
                    cap = modeLabel,
                    emitsToken = false,
                    children = listOf(
                        MenuNode(
                            id = "packages/${pkg.key}/capabilities/${capability.wireName}/ask",
                            label = "Ask every time",
                            cap = if (mode == PackageCapabilityPolicy.Mode.ASK) "current" else null,
                            value = "$prefix ask"
                        ),
                        MenuNode(
                            id = "packages/${pkg.key}/capabilities/${capability.wireName}/once",
                            label = "Allow once",
                            cap = if (mode == PackageCapabilityPolicy.Mode.ALLOW_ONCE) "armed" else "one request",
                            value = "$prefix allow-once"
                        ),
                        MenuNode(
                            id = "packages/${pkg.key}/capabilities/${capability.wireName}/deny",
                            label = "Deny",
                            cap = if (mode == PackageCapabilityPolicy.Mode.DENY) "current" else null,
                            value = "$prefix deny"
                        ),
                        MenuNode(
                            id = "packages/${pkg.key}/capabilities/${capability.wireName}/request",
                            label = "Test request",
                            cap = "broker",
                            value = "hg2package capability-request ${pkg.manager} ${shellQuote(pkg.name)} ${capability.wireName}"
                        )
                    )
                )
            }
        }
    )

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
