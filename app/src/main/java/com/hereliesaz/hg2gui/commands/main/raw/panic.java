package com.hereliesaz.hg2gui.commands.main.raw;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.hereliesaz.hg2gui.UIManager;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;

public class panic implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        LocalBroadcastManager.getInstance(pack.context).sendBroadcast(new Intent(UIManager.ACTION_PANIC));
        return null;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }
}
