package com.hereliesaz.hg2gui.commands.main.raw;

import android.content.Intent;
import com.hereliesaz.hg2gui.GuideActivity;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;
import com.hereliesaz.hg2gui.commands.main.MainPack;

public class guide implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Intent intent = new Intent(((MainPack) pack).androidContext, GuideActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ((MainPack) pack).androidContext.startActivity(intent);
        return null;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 0;
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
