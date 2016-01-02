package net.formula97.miragewatch;

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

public class ReceiverEnablerService extends Service {
    private IBinder mBinder = new ReceiverEnablerServiceBinder();
    /**
     * WatchdogServiceに接続した時の処理
     */
    private ServiceConnection mWatchdogServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

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

        return super.onStartCommand(intent, flags, startId);
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
}
