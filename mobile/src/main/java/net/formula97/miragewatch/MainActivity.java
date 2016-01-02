package net.formula97.miragewatch;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.batteryLevel)
    TextView batteryLevel;

    @Bind(R.id.currentCondition)
    TextView currentCondition;

    @Bind(R.id.enableOnBoot)
    CheckBox enableOnBoot;

    @Bind(R.id.descOnBoot)
    TextView descOnBoot;

    @Bind(R.id.startServiceBtn)
    Button startServiceBtn;

    private boolean mSvcStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnCheckedChanged(R.id.enableOnBoot)
    void changedCheck(boolean checked) {
        setDescMsg(checked);
    }

    private void setDescMsg(boolean checked) {
        int descId = checked ? R.string.start_correction : R.string.correct_manually;
        descOnBoot.setText(descId);
    }

    @OnClick(R.id.startServiceBtn)
    void startServiceBtnClicked() {
        Intent intent = new Intent(this, ReceiverEnablerService.class);

        if (mSvcStarted) {

        } else {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // バッテリー情報の収集
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batt = registerReceiver(null, filter);

        if (batt != null) {
            int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

            batteryLevel.setText(String.valueOf(level));
        }

        // Service開始状況
        int runningId = isServiceRunning() ? R.string.running : R.string.stopped;
        currentCondition.setText(runningId);

        // ボタンキャプションの初期設定
        int btnCaptionId = isServiceRunning() ? R.string.stop_service : R.string.start_service;
        startServiceBtn.setText(btnCaptionId);

        // チェック状態の復元
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean checked = preferences.getBoolean(AppConst.PrefKeys.ENABLE_ON_BOOT, false);
        enableOnBoot.setChecked(checked);

        setDescMsg(checked);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // チェック状態の保存
        boolean checked = enableOnBoot.isChecked();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(AppConst.PrefKeys.ENABLE_ON_BOOT, checked);
        editor.apply();
    }

    private boolean isServiceRunning() {
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo info : services) {
            if (WatchdogService.class.getName().equals(info.service.getClassName())) {
                isRunning = true;
                break;
            }
        }

        return isRunning;
    }
}
