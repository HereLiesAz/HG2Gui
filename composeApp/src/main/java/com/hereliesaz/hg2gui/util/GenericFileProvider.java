package com.hereliesaz.hg2gui.util;

import androidx.core.content.FileProvider;

public class GenericFileProvider extends FileProvider {
    public static final String PROVIDER_NAME = "com.hereliesaz.hg2gui" + ".FILE_PROVIDER";
    // Repair and update-check calls removed: ContentProvider.onCreate() runs on the main thread
    // before Application.onCreate(), so any blocking I/O here causes ANR on cold start.
    // Both operations are triggered from appropriate async entry points elsewhere.
}
