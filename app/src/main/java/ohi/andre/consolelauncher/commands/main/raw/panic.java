package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.main.MainPack;

public class panic implements CommandAbstraction {

    @Override
    public String exec(MainPack info) throws Exception {
        LocalBroadcastManager.getInstance(info.context).sendBroadcast(new Intent(UIManager.ACTION_PANIC));
        return null;
    }

    @Override
    public String exec(MainPack info, String[] args) throws Exception {
        return exec(info);
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
    public String onArgIsNotValid(MainPack mainPack, int i) {
        return null;
    }

    @Override
    public String onNotEnoughArgs(MainPack mainPack, int i) {
        return null;
    }
}
