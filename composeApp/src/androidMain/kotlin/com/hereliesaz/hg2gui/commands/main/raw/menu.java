package com.hereliesaz.hg2gui.commands.main.raw;

import android.content.Intent;
import java.util.Random;

import com.hereliesaz.hg2gui.MagnetMenuActivity;
import com.hereliesaz.hg2gui.MitosisMenuActivity;
import com.hereliesaz.hg2gui.OrigamiMenuActivity;
import com.hereliesaz.hg2gui.SnakeMenuActivity;
import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;

public class menu implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Random random = new Random();
        int pick = random.nextInt(4);
        Class<?> activityClass;
        switch (pick) {
            case 0: activityClass = SnakeMenuActivity.class; break;
            case 1: activityClass = MitosisMenuActivity.class; break;
            case 2: activityClass = MagnetMenuActivity.class; break;
            case 3: activityClass = OrigamiMenuActivity.class; break;
            default: activityClass = MitosisMenuActivity.class;
        }

        Intent intent = new Intent(pack.context, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pack.context.startActivity(intent);

        return null;
    }

    @Override
    public int helpRes() {
        return R.string.help_menu;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 4;
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
