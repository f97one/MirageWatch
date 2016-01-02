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

import java.util.List;

public class ReceiverEnablerService extends Service {

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

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryStatusReceived, filter);

        startAndBind();

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mBatteryStatusReceived);
    }

    public class ReceiverEnablerServiceBinder extends Binder {
        ReceiverEnablerService getService() {
            return ReceiverEnablerService.this;
        }
    }

    private void startAndBind() {
        Intent intent = new Intent(this, WatchdogService.class);
        if (mPairBound) {
            unbindService(mWatchdogServiceConnection);
            mPairBound = false;
        }

        // 相方が手思惟していないかを判断
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
}
