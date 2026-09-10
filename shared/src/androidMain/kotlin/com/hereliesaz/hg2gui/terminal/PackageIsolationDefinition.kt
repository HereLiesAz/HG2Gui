package com.hereliesaz.hg2gui.terminal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Machine-readable description of the isolation boundary HG2Gui actually applies. */
object PackageIsolationDefinition {
    const val SCHEMA_VERSION = 3

    fun json(context: Context, pkg: PackageLifecycleStore.InstalledPackage): String {
        val dependencyClosure = if (pkg.manager == "pkg") {
            DpkgCatalog.dependencyView(DistroManager.prefixDir(context), pkg.name).dependencyClosure
        } else {
            emptyList()
        }
        val policies = JSONObject().apply {
            PackageCapabilityPolicy.summary(context, pkg.key).forEach { (capability, mode) ->
                put(capability.wireName, mode.name.lowercase().replace('_', '-'))
            }
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("package", JSONObject()
                .put("manager", pkg.manager)
                .put("name", pkg.name)
                .put("version", pkg.version))
            .put("execution", JSONObject()
                .put("backend", "PROOT_ISOLATED")
                .put("failClosed", true)
                .put("root", PackageIsolation.root(context, pkg).absolutePath)
                .put("seedStrategy", if (pkg.manager == "pkg") "dependency-closure-manifest-with-safe-full-prefix-fallback" else "installed-prefix-copy")
                .put("dependencyClosure", JSONArray(dependencyClosure)))
            .put("state", JSONObject()
                .put("privateHome", true)
                .put("privateXdgConfig", true)
                .put("privateXdgCache", true)
                .put("privateXdgData", true)
                .put("privateXdgState", true)
                .put("privateTmp", true))
            .put("authority", JSONObject()
                .put("ambientAdb", false)
                .put("ambientRoot", false)
                .put("maskedTools", JSONArray(listOf("adb", "su", "tsu", "magisk", "proot")))
                .put("capabilityPolicies", policies)
                .put("allowOncePersistent", false)
                .put("askFailsClosedWithoutForegroundHuman", true))
            .put("observation", JSONObject()
                .put("filesystemDiff", true)
                .put("procSampler", true)
                .put("networkVisibility", "sampled")
                .put("syscallComplete", false))
            .put("network", JSONObject()
                .put("policy", "one-run-launch-grant")
                .put("launchGateEnforced", true)
                .put("offlineConfinement", false)
                .put("reason", "unprivileged PRoot cannot create a separate Android network namespace; without a one-run grant HG2Gui refuses to launch the isolated process"))
            .toString(2)
    }
}
