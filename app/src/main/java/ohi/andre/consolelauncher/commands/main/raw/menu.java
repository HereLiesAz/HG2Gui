package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import java.util.Random;

import ohi.andre.consolelauncher.MitosisMenuActivity;
import ohi.andre.consolelauncher.OrigamiMenuActivity;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

public class menu implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        Random random = new Random();
        boolean useMitosis = random.nextBoolean();

        Intent intent;
        if (useMitosis) {
            intent = new Intent(info.context, MitosisMenuActivity.class);
        } else {
            intent = new Intent(info.context, OrigamiMenuActivity.class);
        }

        info.context.startActivity(intent);

        return Tuils.EMPTYSTRING;
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
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }
}
