package net.formula97.miragewatch;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class ReceiverEnablerService extends Service {

    private final String sTag = this.getClass().getSimpleName();

    private IBinder mBinder = new ReceiverEnablerServiceBinder();

    /**
     * 相方をバインドしたかどうかを判断するフラグ
     */
    private boolean mPairBound = false;

    /**
     * WatchdogServiceに接続した時の処理
     */
    private ServiceConnection mWatchdogServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(sTag, "バインドされた");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(sTag, "相方から切断された");
            startAndBind();
        }
    };
    /**
     * バッテリー状態更新BroadcastをキャッチするためのBroadcastReceiver。
     */
    private BroadcastReceiver mBatteryStatusReceived = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();

                if (!TextUtils.isEmpty(action) && Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    int battLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                    BattLevelCaptureService.startCaptureAction(context, battLevel);
                }
            }
        }
    };

    public ReceiverEnablerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(sTag, "ReceiverEnablerServiceの起動開始");

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryStatusReceived, filter);

        startAndBind();

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(sTag, "ReceiverEnablerServiceの終了開始");

        if (mPairBound) {
            unbindService(mWatchdogServiceConnection);
            mPairBound = false;

            Intent intent = new Intent(this, WatchdogService.class);
            stopService(intent);
        }

        unregisterReceiver(mBatteryStatusReceived);
    }

    private void startAndBind() {
        Intent intent = new Intent(this, WatchdogService.class);
        if (mPairBound) {
            unbindService(mWatchdogServiceConnection);
            mPairBound = false;
        }

        // 相方が停止していないかを判断
        boolean isRunning = false;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo info : services) {
            if (WatchdogService.class.getName().equals(info.service.getClassName())) {
                isRunning = true;
                break;
            }
        }

        if (isRunning) {
            stopService(intent);
        }
        startService(intent);
        mPairBound = bindService(intent, mWatchdogServiceConnection, BIND_AUTO_CREATE);
    }

    public class ReceiverEnablerServiceBinder extends Binder {
        ReceiverEnablerService getService() {
            return ReceiverEnablerService.this;
        }
    }
}
