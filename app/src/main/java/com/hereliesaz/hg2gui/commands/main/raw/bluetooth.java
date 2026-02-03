package com.hereliesaz.hg2gui.commands.main.raw;

import android.bluetooth.BluetoothAdapter;

import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;
import com.hereliesaz.hg2gui.commands.main.MainPack;

public class bluetooth implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(adapter == null) return info.context.getString(R.string.output_bluetooth_unavailable);

        if(adapter.isEnabled()) {
            adapter.disable();
            return info.context.getString(R.string.output_bluetooth) + " false";
        } else {
            adapter.enable();
            return info.context.getString(R.string.output_bluetooth) + " true";
        }
    }

    @Override
    public int helpRes() {
        return R.string.help_bluetooth;
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
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

}
