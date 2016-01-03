package net.formula97.miragewatch;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by HAJIME on 2016/01/04.
 */
public class HandheldBattLevelReceiverService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        if (messageEvent.getPath().equals("/BatteryLevel")) {
            String s = new String(messageEvent.getData());

            MyWearApplication app = (MyWearApplication) getApplication();
            try {
                app.setBatteryLevel(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}
