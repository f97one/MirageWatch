package net.formula97.miragewatch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_SERVICE_STATE_UPDATED = MainActivity.class.getName() + ".ACTION_SERVICE_STATE_UPDATED";
    private final String sTag = this.getClass().getSimpleName();

//    @Bind(R.id.batteryLevel)
//    TextView batteryLevel;
//    @Bind(R.id.digitalModeBtn)
//    RadioButton digitalModeBtn;
//    @Bind(R.id.analogModeBtn)
//    RadioButton analogModeBtn;

    @Bind(R.id.adView)
    AdView adView;

    @Bind(R.id.licenseHolder)
    TextView licenseHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_beta);

        ButterKnife.bind(this);

        AdRequest.Builder builder = new AdRequest.Builder();
        if (BuildConfig.DEBUG) {
            builder.addTestDevice("8D958583B37665C802471B0DAD53C1E1");
        }
        adView.loadAd(builder.build());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // バッテリー情報の収集
//        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
//        Intent batt = registerReceiver(null, filter);
//
//        if (batt != null) {
//            int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//
//            batteryLevel.setText(String.valueOf(level));
//        }

        licenseHolder.setText(readFromAssets());

        adView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        adView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        adView.destroy();
    }

    private String readFromAssets() {
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        String txt = "";

        try {
            inputStream = getAssets().open("LICENSE");
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                txt += str + "\n";
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }

                if (inputStream != null) {
                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return txt;
    }
}
