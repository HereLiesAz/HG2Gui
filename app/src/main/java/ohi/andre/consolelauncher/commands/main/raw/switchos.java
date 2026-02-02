package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.managers.SystemContext;
import ohi.andre.consolelauncher.tuils.Tuils;

public class switchos implements CommandAbstraction {

    @Override
    public String exec(MainPack info) throws Exception {
        return "Usage: switch-os <ubuntu|macos|windows>";
    }

    @Override
    public String exec(MainPack info, String[] args) throws Exception {
        if (args.length != 1) {
            return "Usage: switch-os <ubuntu|macos|windows>";
        }

        String os = args[0].toLowerCase();
        SystemContext.OSType newOs = null;

        if (os.equals("ubuntu")) {
            newOs = SystemContext.OSType.UBUNTU;
        } else if (os.equals("macos")) {
            newOs = SystemContext.OSType.MACOS;
        } else if (os.equals("windows")) {
            newOs = SystemContext.OSType.WINDOWS;
        }

        if (newOs != null) {
            SystemContext.getInstance().setOs(newOs);
            return ">> SYSTEM REBOOT INITIALIZED...\n>> KERNEL SWITCHED TO: " + newOs.toString() + "\nMenu options updated.";
        } else {
            return "OS '" + os + "' NOT FOUND.";
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return 0; // No help resource yet
    }

    @Override
    public String onArgIsNotValid(MainPack mainPack, int i) {
        return null;
    }

    @Override
    public String onNotEnoughArgs(MainPack mainPack, int i) {
        return "Usage: switch-os <ubuntu|macos|windows>";
    }
}
