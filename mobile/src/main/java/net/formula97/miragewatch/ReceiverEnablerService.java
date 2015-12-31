package net.formula97.miragewatch;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.IBinder;
import android.text.TextUtils;

public class ReceiverEnablerService extends Service {
    public ReceiverEnablerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * バッテリー状態更新BroadcastをキャッチするためのBroadcastReceiver。
     */
    public class BattStatusReceiver extends BroadcastReceiver {

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
    }
}
