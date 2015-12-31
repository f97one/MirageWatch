package net.formula97.miragewatch;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class BattLevelCaptureService extends IntentService {

    private static final String ACTION_CAPTURE_BATTERY_STATUS = "net.formula97.miragewatch.ACTION_CAPTURE_BATTERY_STATUS";

    private static final String EXTRA_BATTERY_LEVEL = "EXTRA_BATTERY_LEVEL";

    public BattLevelCaptureService() {
        super("BattLevelCaptureService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startCaptureAction(Context context, int currentBatteryLevel) {
        Intent intent = new Intent(context, BattLevelCaptureService.class);
        intent.setAction(ACTION_CAPTURE_BATTERY_STATUS);
        intent.putExtra(EXTRA_BATTERY_LEVEL, currentBatteryLevel);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CAPTURE_BATTERY_STATUS.equals(action)) {
                final int battLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, 0);

                MyApplication app = (MyApplication) getApplication();
                app.setCurrentRemainLevel(battLevel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // IntentService破棄のタイミングで永続化
        MyApplication app = (MyApplication) getApplication();
        app.storeBattLevel();
    }
}
