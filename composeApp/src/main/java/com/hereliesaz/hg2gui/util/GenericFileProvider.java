package com.hereliesaz.hg2gui.util;

import androidx.core.content.FileProvider;

import com.hereliesaz.hg2gui.terminal.TermuxRuntimeRepair;
import com.hereliesaz.hg2gui.update.AppUpdateChecker;

public class GenericFileProvider extends FileProvider {
    public static final String PROVIDER_NAME = "com.hereliesaz.hg2gui" + ".FILE_PROVIDER";

    @Override
    public boolean onCreate() {
        boolean created = super.onCreate();
        if (getContext() != null) {
            TermuxRuntimeRepair.INSTANCE.repair(getContext());
            AppUpdateChecker.INSTANCE.checkAndNotify(getContext());
        }
        return created;
    }
}
