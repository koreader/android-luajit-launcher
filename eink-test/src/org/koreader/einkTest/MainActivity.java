package org.koreader.einkTest;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class MainActivity extends android.app.Activity {
    private static final String TAG = "eink-test";

    private static final int RK30xxNORMAL = 1;
    private static final int RK30xxFORCED = 2;
    private static final int FAKE = 999;

    /** current device overview */
    private TextView overview;

    private static final String MANUFACTURER = android.os.Build.MANUFACTURER;
    private static final String BRAND = android.os.Build.BRAND;
    private static final String MODEL = android.os.Build.MODEL;
    private static final String PRODUCT = android.os.Build.PRODUCT;
    private static final String HARDWARE = android.os.Build.HARDWARE;

    /** fake eink */
    private TextView fakeDesc;
    private Button fake_button;

    /** rockchip */
    private TextView rockchipDesc;
    private Button rk30xx_normal_button;
    private Button rk30xx_forced_button;

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        overview = (TextView) findViewById(R.id.overview);
        fakeDesc = (TextView) findViewById(R.id.fakeText);
        rockchipDesc = (TextView) findViewById(R.id.rockchipText);

        fake_button = (Button) findViewById(R.id.fakeButton);
        rk30xx_normal_button = (Button) findViewById(R.id.rockchipNormalButton);
        rk30xx_forced_button = (Button) findViewById(R.id.rockchipForcedButton);

        overview.setText("Manufacturer: " + MANUFACTURER);
        overview.append("\n Brand: " + BRAND);
        overview.append("\n Model: " + MODEL);
        overview.append("\n Product: " + PRODUCT);
        overview.append("\n Hardware: " + HARDWARE);

        fakeDesc.setText("This button does nothing! It's just an example of an empty action. ");
        fakeDesc.append("If you don't see any difference between fake and real tests, your device ");
        fakeDesc.append("is not supported by any implemented epd controller.");

        rockchipDesc.setText("These buttons should invoke a full refresh of rockchip rk30xx devices. ");
        rockchipDesc.append("Both normal and forced modes should work.");

        rk30xx_normal_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK30xxNORMAL);
            }
        });
        rk30xx_forced_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK30xxFORCED);
            }
        });
        fake_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(FAKE);
            }
        });
    }

    private void runEinkTest(int test) {
        try {
            View v = getWindow().getDecorView().findViewById(android.R.id.content);
	    if (test == RK30xxNORMAL) {
	        Log.i(TAG, "rockchip normal update");
	        RK30xxEPDController.requestEpdMode(v, "EPD_FULL");
            } else if (test == RK30xxFORCED) {
	        Log.i(TAG, "rockchip forced update");
	        RK30xxEPDController.requestEpdMode(v, "EPD_FULL", true);
            } else if (test == FAKE) {
                Log.i(TAG, "fake update");
            } else {
                Log.e(TAG, "invalid test");
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}

