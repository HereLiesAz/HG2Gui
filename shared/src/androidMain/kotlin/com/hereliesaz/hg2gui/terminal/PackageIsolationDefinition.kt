package com.hereliesaz.hg2gui.terminal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Machine-readable description of the isolation boundary HG2Gui actually applies. */
object PackageIsolationDefinition {
    const val SCHEMA_VERSION = 1

    fun json(context: Context, pkg: PackageLifecycleStore.InstalledPackage): String {
        val dependencyClosure = if (pkg.manager == "pkg") {
            DpkgCatalog.dependencyView(DistroManager.prefixDir(context), pkg.name).dependencyClosure
        } else {
            emptyList()
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
                .put("seedStrategy", "installed-prefix-copy")
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
                .put("maskedTools", JSONArray(listOf("adb", "su", "tsu", "magisk", "proot"))))
            .put("observation", JSONObject()
                .put("filesystemDiff", true)
                .put("procSampler", true)
                .put("networkVisibility", "sampled")
                .put("syscallComplete", false))
            .put("network", JSONObject()
                .put("policy", "visible-not-blocked")
                .put("enforced", false))
            .toString(2)
    }
}
