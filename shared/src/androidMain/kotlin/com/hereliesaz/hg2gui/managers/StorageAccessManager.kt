package com.hereliesaz.hg2gui.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hereliesaz.hg2gui.PermissionCodes

/**
 * The single place that knows how "full device storage" is actually granted on this API level -
 * MANAGE_EXTERNAL_STORAGE's own Settings-page flow on API 30+ (there's no runtime dialog for
 * it), the ordinary READ/WRITE_EXTERNAL_STORAGE runtime dialog below that. [VfsManager] only
 * ever asks [hasFullAccess] before switching its root to real storage - it doesn't otherwise
 * care which permission model got it there.
 */
object StorageAccessManager {
    fun hasFullAccess(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Kicks off whichever grant flow this API level actually uses. On 30+ that's a Settings
     * page, not a callback dialog - there's no onRequestPermissionsResult for
     * MANAGE_EXTERNAL_STORAGE, so the caller has to re-check [hasFullAccess] itself (e.g. from
     * onResume) once the user comes back. Below 30, the ordinary runtime dialog applies and
     * the result does arrive via onRequestPermissionsResult, keyed on
     * [PermissionCodes.STORAGE_ACCESS_REQUEST_PERMISSION].
     */
    fun requestFullAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val perApp = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            val intent = if (perApp.resolveActivity(activity.packageManager) != null) {
                perApp
            } else {
                // Some OEM builds don't implement the per-app variant - the plain "manage all
                // files access" settings list is the one flow every 30+ device is required to
                // ship, so it's the safe universal fallback rather than leaving the tap inert.
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
            activity.startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PermissionCodes.STORAGE_ACCESS_REQUEST_PERMISSION
            )
        }
    }
}
