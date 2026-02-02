package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;

import java.util.Random;

import ohi.andre.consolelauncher.MagnetMenuActivity;
import ohi.andre.consolelauncher.MitosisMenuActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

public class menu implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        Random random = new Random();
        Class<?> activityClass = random.nextBoolean() ? MitosisMenuActivity.class : MagnetMenuActivity.class;

        Intent intent = new Intent(pack.context, activityClass);
        pack.context.startActivity(intent);

        return null;
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
    public int helpRes() {
        return R.string.help_menu;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }
}
