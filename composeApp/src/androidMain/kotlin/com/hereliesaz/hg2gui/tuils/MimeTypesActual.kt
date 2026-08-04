package com.hereliesaz.hg2gui.tuils

import android.webkit.MimeTypeMap

actual fun getSystemMimeType(extension: String): String? {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}
