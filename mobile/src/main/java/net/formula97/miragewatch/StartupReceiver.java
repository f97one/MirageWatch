package net.formula97.miragewatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class StartupReceiver extends BroadcastReceiver {
    public StartupReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean permitStart = pref.getBoolean(AppConst.PrefKeys.ENABLE_ON_BOOT, false);

        if (permitStart) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action) && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent i = new Intent(context, ReceiverEnablerService.class);
                context.startService(i);
            }
        }
    }
}
