package net.formula97.miragewatch;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ReceiverEnablerService extends Service {
    public ReceiverEnablerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
