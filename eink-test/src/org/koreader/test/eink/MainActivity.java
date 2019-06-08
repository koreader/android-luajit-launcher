package org.koreader.test.eink;

import android.content.Intent;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.lang.StringBuilder;

public class MainActivity extends android.app.Activity {
    private static final String TAG = "epd";

    private static final int RK30xx = 1;
    private static final int RK33xx = 2;
    private static final int NTX_TOLINO = 3;
    private static final int NTX_NOOK = 4;
    private static final int FAKE = 999;

    private static final String MANUFACTURER = android.os.Build.MANUFACTURER;
    private static final String BRAND = android.os.Build.BRAND;
    private static final String MODEL = android.os.Build.MODEL;
    private static final String PRODUCT = android.os.Build.PRODUCT;
    private static final String HARDWARE = android.os.Build.HARDWARE;

    private StringBuilder info;
    private TextView deviceId;

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        info = new StringBuilder();

        TextView readmeReport = (TextView) findViewById(R.id.readmeReport);
        deviceId = (TextView) findViewById(R.id.deviceId);

        TextView rk30xx_description = (TextView) findViewById(R.id.rk30xxText);
        TextView rk33xx_description = (TextView) findViewById(R.id.rk33xxText);
        TextView ntx_tolino_description = (TextView) findViewById(R.id.ntxTolinoText);
        TextView ntx_nook_description = (TextView) findViewById(R.id.ntxNookText);

        Button rk30xx_button = (Button) findViewById(R.id.rk30xxButton);
        Button rk33xx_button = (Button) findViewById(R.id.rk33xxButton);
        Button ntx_tolino_button = (Button) findViewById(R.id.ntxTolinoButton);
        Button ntx_nook_button = (Button) findViewById(R.id.ntxNookButton);
        Button share_button = (Button) findViewById(R.id.shareButton);

        /** current device info */
        info.append("Manufacturer: " + MANUFACTURER + "\n");
        info.append("Brand: " + BRAND + "\n");
        info.append("Model: " + MODEL + "\n");
        info.append("Product: " + PRODUCT + "\n");
        info.append("Hardware: " + HARDWARE + "\n");

        /** add platform if available */
        String platform = getBuildProperty("ro.board.platform");
        if ((platform != new String()) && (platform != null)) {
            info.append("Platform: " + platform + "\n");
        }

        readmeReport.setText("Did they work? Cool");
        readmeReport.append("\n Go to github.com/koreader/koreader/issues/4551");
        readmeReport.append(" and share your device information with us and which button did work for you");

        /** rockchip rk30xx */
        rk30xx_description.setText("This button should invoke a full refresh of Boyue T61/T62 clones.");

        rk30xx_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK30xx);
            }
        });

        /** rockchip rk33xx */
        rk33xx_description.setText("This button should work on boyue rk3368 clones.");

        rk33xx_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK33xx);
            }
        });

        /** freescale/ntx - tolino fw11+ */
        ntx_tolino_description.setText("This button should work on modern Tolinos and other ntx boards");

        ntx_tolino_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(NTX_TOLINO);
            }
        });

        /** freescale/ntx - nook 4.4 */
        ntx_nook_description.setText("This button should work on modern Nooks and other ntx boards");

        ntx_nook_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(NTX_NOOK);
            }
        });


        /** share button */
        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareText(info.toString());
            }
        });
    }

    private void runEinkTest(int test) {
        info.append(String.format("run test #%d -> ", test));
        try {
            View v = getWindow().getDecorView().findViewById(android.R.id.content);
	    if (test == RK30xx) {
	        info.append("rk30xx\n");
                // force a flashing black->white update
	        RK30xxEPDController.requestEpdMode(v, "EPD_FULL", true);
            } else if (test == RK33xx) {
                info.append("rk33xx\n");
                RK33xxEPDController.requestEpdMode("EPD_FULL");
            } else if (test == NTX_TOLINO) {
                // get screen width and height
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                info.append("tolino\n");
                NTXEPDController.requestEpdMode(v, 34, 50, 0, 0, width, height);
            } else if (test == NTX_NOOK) {
                info.append("nook\n");
                NookEPDController.requestEpdMode(v);
            } else {
                info.append("error: invalid test\n");
            }
        } catch (Exception e) {
        }
    }

    private void shareText(String text) {
        Intent i = new Intent(android.content.Intent.ACTION_SEND);
        i.putExtra(android.content.Intent.EXTRA_TEXT, text);
        i.setType("text/plain");
        startActivity(Intent.createChooser(i, "einkTest results"));
    }

    private String getBuildProperty(String key) {
        String value = new String();
        try {
            value = (String) Class.forName("android.os.SystemProperties").getMethod(
                "get", String.class).invoke(null, key);
            if (value.length() < 2) value = "unknown";
        } catch (Exception e) {
            value = "unknown";
        } finally {
            return value;
        }
    }
}

