package net.formula97.miragewatch;

import android.app.Application;

/**
 * Created by f97one on 15/12/31.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        mCurrentRemainLevel = 0.0;
    }

    private double mCurrentRemainLevel;

    /**
     * 保存されている残存バッテリーレベルを取得する。
     *
     * @return 残存バッテリーレベル
     */
    public double getCurrentRemainLevel() {
        return mCurrentRemainLevel;
    }

    public void setCurrentRemainLevel(double currentRemainLevel) {
        mCurrentRemainLevel = currentRemainLevel;
    }
}
