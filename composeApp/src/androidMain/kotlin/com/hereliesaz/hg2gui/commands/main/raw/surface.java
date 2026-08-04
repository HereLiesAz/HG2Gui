package com.hereliesaz.hg2gui.commands.main.raw;

import android.content.Intent;

import com.hereliesaz.hg2gui.MitosisMenuActivity;
import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;
import com.hereliesaz.hg2gui.commands.main.MainPack;
import com.hereliesaz.hg2gui.tuils.Tuils;

public class surface implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        Intent intent = new Intent(info.context, MitosisMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        info.context.startActivity(intent);

        return Tuils.EMPTYSTRING;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return R.string.help_surface;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }
}
