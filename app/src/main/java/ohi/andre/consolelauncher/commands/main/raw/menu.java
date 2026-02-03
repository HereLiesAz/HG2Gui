package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import java.util.Random;

import ohi.andre.consolelauncher.MagnetMenuActivity;
import ohi.andre.consolelauncher.MitosisMenuActivity;
import ohi.andre.consolelauncher.OrigamiMenuActivity;
import ohi.andre.consolelauncher.SnakeMenuActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

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
