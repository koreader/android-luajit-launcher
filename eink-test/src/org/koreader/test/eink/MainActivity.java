package org.koreader.test.eink;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends android.app.Activity {
    private static final String TAG = "eink-test";

    private static final int RK30xxNORMAL = 1;
    private static final int RK30xxFORCED = 2;
    private static final int RK33xxNORMAL = 3;
    private static final int FAKE = 999;

    private static final String MANUFACTURER = android.os.Build.MANUFACTURER;
    private static final String BRAND = android.os.Build.BRAND;
    private static final String MODEL = android.os.Build.MODEL;
    private static final String PRODUCT = android.os.Build.PRODUCT;
    private static final String HARDWARE = android.os.Build.HARDWARE;

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TextView overview = (TextView) findViewById(R.id.overview);
        TextView fakeDesc = (TextView) findViewById(R.id.fakeText);
        TextView rk30xx_description = (TextView) findViewById(R.id.rk30xxText);
        TextView rk33xx_description = (TextView) findViewById(R.id.rk33xxText);

        Button fake_button = (Button) findViewById(R.id.fakeButton);
        Button rk30xx_normal_button = (Button) findViewById(R.id.rk30xxNormalButton);
        Button rk30xx_forced_button = (Button) findViewById(R.id.rk30xxForcedButton);
        Button rk33xx_normal_button = (Button) findViewById(R.id.rk33xxNormalButton);

        /** current device overview */
        overview.setText("Manufacturer: " + MANUFACTURER);
        overview.append("\n Brand: " + BRAND);
        overview.append("\n Model: " + MODEL);
        overview.append("\n Product: " + PRODUCT);
        overview.append("\n Hardware: " + HARDWARE);
        overview.append("\n Platform: " + getBuildProperty("ro.board.platform"));

        /** fake eink */
        fakeDesc.setText("This button does nothing! It's just an example of an empty action. ");
        fakeDesc.append("If you don't see any difference between fake and real tests, your device ");
        fakeDesc.append("is not supported by any implemented epd controller.");

        fake_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(FAKE);
            }
        });

        /** rockchip rk30xx */
        rk30xx_description.setText("These buttons should invoke a full refresh of rockchip rk30xx devices. ");
        rk30xx_description.append("Both normal and forced modes should work.");

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

        /** rockchip rk33xx */
        rk33xx_description.setText("This button should work on boyue rk3368 clones.");

        rk33xx_normal_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK33xxNORMAL);
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
            } else if (test == RK33xxNORMAL) {
                Log.i(TAG, "rockchip 33xx normal update");
                RK33xxEPDController.requestEpdMode();
            } else if (test == FAKE) {
                Log.i(TAG, "fake update");
            } else {
                Log.e(TAG, "invalid test");
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private String getBuildProperty(String key) {
        String value;
        try {
            value = (String) Class.forName("android.os.SystemProperties").getMethod(
                "get", String.class).invoke(null, key);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            value = "unknown";
        }
        return value;
    }
}

