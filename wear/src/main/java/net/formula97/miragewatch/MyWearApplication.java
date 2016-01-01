package net.formula97.miragewatch;

import android.app.Application;

/**
 * Created by f97one on 16/01/01.
 */
public class MyWearApplication extends Application {

    private int mBatteryLevel;

    @Override
    public void onCreate() {
        super.onCreate();

        mBatteryLevel = -1;
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        mBatteryLevel = batteryLevel;
    }
}
