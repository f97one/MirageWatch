package net.formula97.miragewatch;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearableMsgReceiverService extends WearableListenerService {

    private final String sTag = this.getClass().getSimpleName();
    private GoogleApiClient mApiClient;
    /**
     * Google API Clientで接続成功した時のコールバック。
     */
    private GoogleApiClient.ConnectionCallbacks mConnectionCallback = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    // バッテリーレベル要求に応答を返す
                    Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    String battLevel = String.valueOf(i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));

                    NodeApi.GetConnectedNodesResult connectedNodesResult = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                    for (Node node : connectedNodesResult.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mApiClient,
                                node.getId(),
                                "/BatteryLevel",
                                battLevel.getBytes())
                                .await();

                        if (result.getStatus().isSuccess()) {
                            Log.d(sTag, "バッテリーレベルの送信に成功");
                        } else {
                            Log.d(sTag, "バッテリーレベルの送信に失敗 (" + result.getStatus().getStatusMessage() + ")");
                        }
                    }
                }
            }).start();
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

    public WearableMsgReceiverService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        String path = messageEvent.getPath();

        switch (path) {
            case "/RequestBatteryLevel":
                Log.d(sTag, "バッテリーレベル要求を受信");

                mApiClient = new GoogleApiClient.Builder(
                        getApplicationContext(), mConnectionCallback, mConnFailedCallback)
                        .addApi(Wearable.API)
                        .build();
                mApiClient.connect();
                break;
        }
    }

}
