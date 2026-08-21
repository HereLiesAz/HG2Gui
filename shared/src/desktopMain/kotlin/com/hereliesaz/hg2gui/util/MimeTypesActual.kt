package com.hereliesaz.hg2gui.util

// Dev-tooling only - see shared/build.gradle.kts's own comment on why this target exists at all.
// MimeTypes.getMimeType already falls back to its own extension table (and ultimately
// ALL_MIME_TYPES) whenever this returns null, so this platform lookup simply isn't needed here.
actual fun getSystemMimeType(extension: String): String? = null
