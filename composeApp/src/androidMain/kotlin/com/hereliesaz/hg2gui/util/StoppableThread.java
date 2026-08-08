package com.hereliesaz.hg2gui.util;

/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

public class StoppableThread extends Thread {

    private volatile boolean stopped = false;
    public StoppableThread() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Utils.log(e);
            Utils.toFile(e);
            System.exit(1);
        });
    }

    @Override
    public void interrupt() {
        super.interrupt();

        synchronized (this) {
            stopped = true;
        }
    }

    @Override
    public boolean isInterrupted() {
        boolean b;
        synchronized (this) {
            b = stopped;
        }
        return b || super.isInterrupted();
    }
}
