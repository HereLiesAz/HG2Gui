package com.hereliesaz.hg2gui.managers.flashlight;

import android.content.Context;

/**
 * Created by francescoandreuzzi on 20/08/2017.
 *
 * Process-wide torch state. minSdk is above M, so the camera2-based [Flashlight2] is the only
 * implementation. All methods are synchronized: the builtin may be invoked from more than one
 * thread and the torch instance is lazily created.
 */
public class TorchManager {

    private static TorchManager mInstance;
    private Torch mTorch;
    private String torchType;

    private TorchManager() {
        this.setTorchType(Constants.ID_DEVICE_OUTPUT_TORCH_FLASH);
    }

    public static synchronized TorchManager getInstance() {
        if (mInstance == null) {
            mInstance = new TorchManager();
        }
        return mInstance;
    }

    /** True only when the torch actually turned on - a failed camera call leaves it off. */
    public synchronized boolean isOn() {
        return this.mTorch != null && this.mTorch.getStatus();
    }

    public synchronized void setTorchType(String torchType) {
        this.torchType = torchType;
    }

    public synchronized void turnOn(Context context) {
        if (this.mTorch == null && this.torchType.equals(Flashlight.TYPE)) {
            this.mTorch = new Flashlight2(context.getApplicationContext());
        }
        if (this.mTorch == null) return;
        this.mTorch.setEnabled(true);
        this.mTorch.start(true);
        if (!this.mTorch.getStatus()) {
            // The camera refused (no flash unit, camera in use, access error): don't report on.
            this.mTorch = null;
        }
    }

    public synchronized void turnOff() {
        if (this.mTorch != null) {
            this.mTorch.start(false);
            this.mTorch = null;
        }
    }

    public synchronized void toggle(Context context) {
        if (this.isOn()) {
            this.turnOff();
        } else {
            this.turnOn(context);
        }
    }
}
