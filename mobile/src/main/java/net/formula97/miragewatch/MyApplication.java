package net.formula97.miragewatch;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by f97one on 15/12/31.
 */
public class MyApplication extends Application {
    /**
     * バッテリーレベルをPreferenceに永続化するときのキー
     */
    private final String mPrefKeyBatteryLevel = "mPrefKeyBatteryLevel";
    /**
     * 現在のバッテリーレベル
     */
    private int mCurrentRemainLevel;
    private boolean mSentBatteryLevel;

    @Override
    public void onCreate() {
        super.onCreate();

        mCurrentRemainLevel = -1;
        mSentBatteryLevel = false;
    }

    /**
     * 保存されている残存バッテリーレベルを取得する。
     *
     * @return 残存バッテリーレベル
     */
    public int getCurrentRemainLevel() {
        // 初期値のままの場合は、Preferenceの値の取得を試みる
        if (mCurrentRemainLevel == -1) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            mCurrentRemainLevel = pref.getInt(mPrefKeyBatteryLevel, 0);
        }

        return mCurrentRemainLevel;
    }

    /**
     * バッテリーレベルをインメモリで保持する。
     *
     * @param currentRemainLevel 現在のバッテリーレベル
     */
    public void setCurrentRemainLevel(int currentRemainLevel) {
        mCurrentRemainLevel = currentRemainLevel;
    }

    public void storeBattLevel() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        editor.putInt(mPrefKeyBatteryLevel, mCurrentRemainLevel);
        editor.apply();
    }

    public boolean isSentBatteryLevel() {
        return mSentBatteryLevel;
    }

    public void setSentBatteryLevel(boolean mSentBatteryLevel) {
        this.mSentBatteryLevel = mSentBatteryLevel;
    }
}
