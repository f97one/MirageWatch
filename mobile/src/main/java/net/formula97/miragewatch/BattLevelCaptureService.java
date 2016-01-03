package net.formula97.miragewatch;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class BattLevelCaptureService extends IntentService {

    public static final String ACTION_BATTERY_LEVEL_RECEIVED = "ACTION_BATTERY_LEVEL_RECEIVED";
    public static final String EXTRA_RECEIVED_BATTERY_LEVEL = "EXTRA_RECEIVED_BATTERY_LEVEL";
    private static final String ACTION_CAPTURE_BATTERY_STATUS = "net.formula97.miragewatch.ACTION_CAPTURE_BATTERY_STATUS";
    private static final String EXTRA_BATTERY_LEVEL = "EXTRA_BATTERY_LEVEL";
    private GoogleApiClient mApiClient;
    /**
     * Google API Clientで接続成功した時のコールバック。
     */
    private GoogleApiClient.ConnectionCallbacks mConnectionCallback = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            MyApplication app = (MyApplication) getApplication();
            String battLevelString = String.valueOf(app.getCurrentRemainLevel());

            String tag = BattLevelCaptureService.class.getSimpleName();

            NodeApi.GetConnectedNodesResult connectedNodesResult = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
            for (Node node : connectedNodesResult.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mApiClient,
                        node.getId(),
                        "/BatteryLevel",
                        battLevelString.getBytes())
                        .await();

                if (result.getStatus().isSuccess()) {
                    Log.d(tag, "バッテリーレベルの送信に成功");
                } else {
                    Log.d(tag, "バッテリーレベルの送信に失敗 (" + result.getStatus().getStatusMessage() + ")");
                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };
    /**
     * Google API Clientで接続失敗した時のコールバック。
     */
    private GoogleApiClient.OnConnectionFailedListener mConnFailedCallback = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    };

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

                if (!app.isSentBatteryLevel()) {
                    app.setSentBatteryLevel(true);

                    mApiClient = new GoogleApiClient.Builder(
                            getApplicationContext(), mConnectionCallback, mConnFailedCallback)
                            .addApi(Wearable.API)
                            .build();
                    mApiClient.connect();
                }

                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getApplicationContext());
                Intent i = new Intent(ACTION_BATTERY_LEVEL_RECEIVED);
                i.putExtra(EXTRA_RECEIVED_BATTERY_LEVEL, battLevel);
                manager.sendBroadcast(i);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // IntentService破棄のタイミングで永続化
        MyApplication app = (MyApplication) getApplication();
        app.storeBattLevel();

        app.setSentBatteryLevel(false);
    }
}
