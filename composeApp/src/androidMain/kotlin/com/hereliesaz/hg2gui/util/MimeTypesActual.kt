package com.hereliesaz.hg2gui.util

import android.webkit.MimeTypeMap

actual fun getSystemMimeType(extension: String): String? {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}
