package net.formula97.miragewatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

public class WatchdogService extends Service {

    public static final String ACTION_START_SHUTDOWN = WatchdogService.class.getName() + ".ACTION_START_SHUTDOWN";

    private IBinder mBinder = new WatchdogServiceBinder();

    private boolean mPairBound = false;

    /**
     * ReceiverEnablerServiceに接続した時の処理
     */
    private ServiceConnection mReceiverEnablerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            startAndBind();
        }
    };

    public WatchdogService() {
    }

    public static IntentFilter getTriggerFilter() {
        return new IntentFilter(ACTION_START_SHUTDOWN);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent i = new Intent(this, ReceiverEnablerService.class);
        bindService(i, mReceiverEnablerServiceConnection, BIND_AUTO_CREATE);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPairBound) {
            unbindService(mReceiverEnablerServiceConnection);
            mPairBound = false;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    private void startAndBind() {
        if (mPairBound) {
            unbindService(mReceiverEnablerServiceConnection);
            mPairBound = false;
        }

        Intent intent = new Intent(this, ReceiverEnablerService.class);
        stopService(intent);
        startService(intent);
        mPairBound = bindService(intent, mReceiverEnablerServiceConnection, BIND_AUTO_CREATE);
    }

    public class WatchdogServiceBinder extends Binder {
        WatchdogService getService() {
            return WatchdogService.this;
        }
    }

    public class UnbindTriggerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action) && action.equals(ACTION_START_SHUTDOWN)) {
                // 本体ServiceのUnbindを行う
                unbindService(mReceiverEnablerServiceConnection);
            }

        }
    }
}
