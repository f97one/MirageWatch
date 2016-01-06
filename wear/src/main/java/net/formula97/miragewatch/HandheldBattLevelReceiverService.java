package net.formula97.miragewatch;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by f97one on 2016/01/04.
 */
public class HandheldBattLevelReceiverService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals("/BatteryLevel")) {
            String s = new String(messageEvent.getData());

            Log.d(this.getClass().getSimpleName(), "受信したバッテリーレベル = " + s);

            MyWearApplication app = (MyWearApplication) getApplication();
            try {
                app.setBatteryLevel(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
