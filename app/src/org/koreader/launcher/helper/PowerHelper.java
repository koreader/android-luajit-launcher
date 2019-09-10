package org.koreader.launcher.helper;


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import org.koreader.launcher.Logger;


public class PowerHelper extends BaseHelper {

    public int getWakelockDuration() {
        return wakelock_duration;
    }

    public void setWakelockDuration(final int milliseconds) {
        wakelock_duration = milliseconds;
    }

    public void setWakelockEnabled(final boolean enable) {
        if (enable) {
            wakelockAcquire();
        } else {
            wakelockRelease();
        }
    }
}
