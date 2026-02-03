package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.managers.SystemContext;

public class switchos implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Object[] args = pack.args;
        if (args == null || args.length != 1) {
            return "Usage: switch-os <ubuntu|macos|windows>";
        }

        String os = ((String) args[0]).toLowerCase();
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
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "Usage: switch-os <ubuntu|macos|windows>";
    }
}