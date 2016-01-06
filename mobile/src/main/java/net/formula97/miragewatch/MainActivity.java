package net.formula97.miragewatch;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_SERVICE_STATE_UPDATED = MainActivity.class.getName() + ".ACTION_SERVICE_STATE_UPDATED";
    private final String sTag = this.getClass().getSimpleName();
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
    private BroadcastReceiver mBatteryLevelUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action) && action.equals(BattLevelCaptureService.ACTION_BATTERY_LEVEL_RECEIVED)) {
                int level = intent.getIntExtra(BattLevelCaptureService.EXTRA_RECEIVED_BATTERY_LEVEL, 0);
                batteryLevel.setText(String.valueOf(level));
            }
        }
    };
    private BroadcastReceiver mServiceStateUpdated = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action) && action.equals(ACTION_SERVICE_STATE_UPDATED)) {
                setServiceCondition();
                setButtonCaption();
            }
        }
    };

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
            sendBroadcast(new Intent(WatchdogService.ACTION_START_SHUTDOWN));
            Log.d(sTag, "WatchdogのUnbind指示を送信");

            if (stopService(intent)) {
                mSvcStarted = false;
            }
        } else {
            if (startService(intent) != null) {
                mSvcStarted = true;
            }
        }

        setServiceCondition();
        setButtonCaption();
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
        setServiceCondition();

        // ボタンキャプションの初期設定
        setButtonCaption();

        // チェック状態の復元
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean checked = preferences.getBoolean(AppConst.PrefKeys.ENABLE_ON_BOOT, false);
        enableOnBoot.setChecked(checked);

        setDescMsg(checked);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.registerReceiver(mBatteryLevelUpdated, new IntentFilter(BattLevelCaptureService.ACTION_BATTERY_LEVEL_RECEIVED));

        registerReceiver(mServiceStateUpdated, new IntentFilter(ACTION_SERVICE_STATE_UPDATED));
    }

    private void setButtonCaption() {
        int btnCaptionId = isServiceRunning() ? R.string.stop_service : R.string.start_service;
        startServiceBtn.setText(btnCaptionId);
    }

    private void setServiceCondition() {
        int runningId = isServiceRunning() ? R.string.running : R.string.stopped;
        currentCondition.setText(runningId);
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

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
        manager.unregisterReceiver(mBatteryLevelUpdated);

        unregisterReceiver(mServiceStateUpdated);
    }

    private boolean isServiceRunning() {
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo info : services) {
            if (ReceiverEnablerService.class.getName().equals(info.service.getClassName())) {
                isRunning = true;
                break;
            }
        }

        return isRunning;
    }
}
