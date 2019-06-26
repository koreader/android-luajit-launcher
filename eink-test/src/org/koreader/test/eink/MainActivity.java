package org.koreader.test.eink;

import android.content.Intent;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.koreader.device.freescale.NTXEPDController;
import org.koreader.device.rockchip.RK30xxEPDController;
import org.koreader.device.rockchip.RK33xxEPDController;

public class MainActivity extends android.app.Activity {

    private static final int RK30xx = 1;
    private static final int RK33xx = 2;
    private static final int NTX_NEW= 3;

    private static final String MANUFACTURER = android.os.Build.MANUFACTURER;
    private static final String BRAND = android.os.Build.BRAND;
    private static final String MODEL = android.os.Build.MODEL;
    private static final String PRODUCT = android.os.Build.PRODUCT;
    private static final String HARDWARE = android.os.Build.HARDWARE;

    private TextView info;

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        info = (TextView) findViewById(R.id.info);
        TextView readmeReport = (TextView) findViewById(R.id.readmeReport);

        TextView rk30xx_description = (TextView) findViewById(R.id.rk30xxText);
        TextView rk33xx_description = (TextView) findViewById(R.id.rk33xxText);
        TextView ntx_new_description = (TextView) findViewById(R.id.ntxNewText);

        Button rk30xx_button = (Button) findViewById(R.id.rk30xxButton);
        Button rk33xx_button = (Button) findViewById(R.id.rk33xxButton);
        Button ntx_new_button = (Button) findViewById(R.id.ntxNewButton);
        Button share_button = (Button) findViewById(R.id.shareButton);

        /** current device info */
        info.append("Manufacturer: " + MANUFACTURER + "\n");
        info.append("Brand: " + BRAND + "\n");
        info.append("Model: " + MODEL + "\n");
        info.append("Product: " + PRODUCT + "\n");
        info.append("Hardware: " + HARDWARE + "\n");

        /** add platform if available */
        String platform = getBuildProperty("ro.board.platform");
        if (!("".equals(platform) && (platform == null))) {
            info.append("Platform: " + platform + "\n");
        }

        readmeReport.setText("Did you see a flashing black to white eink update? Cool\n\n");
        readmeReport.append("Go to github.com/koreader/koreader/issues/4551 ");
        readmeReport.append("and share the following information with us");

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

        /** freescale/ntx - Newer Tolino/Nook devices */
        ntx_new_description.setText("This button should work on modern Tolinos/Nooks and other ntx boards");

        ntx_new_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(NTX_NEW);
            }
        });

        /** share button */
        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareText(info.getText().toString());
            }
        });
    }

    private void runEinkTest(int test) {
        boolean success = false;
        info.append(String.format("run test #%d -> ", test));
        try {
            View v = getWindow().getDecorView().findViewById(android.R.id.content);
	    if (test == RK30xx) {
	        info.append("rk30xx: ");
                // force a flashing black->white update
	        if (RK30xxEPDController.requestEpdMode(v, "EPD_FULL", true))
                    success = true;

            } else if (test == RK33xx) {
                info.append("rk33xx: ");
                if (RK33xxEPDController.requestEpdMode("EPD_FULL"))
                    success = true;

            } else if (test == NTX_NEW) {
                // get screen width and height
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                info.append("tolino: ");
                if (NTXEPDController.requestEpdMode(v, 34, 50, 0, 0, width, height))
                    success = true;
            }
        } catch (Exception e) {
        }

        if (success)
            info.append("pass\n");
        else
            info.append("fail\n");
    }

    private void shareText(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.putExtra(Intent.EXTRA_TEXT, text);
        i.setType("text/plain");
        startActivity(Intent.createChooser(i, "einkTest results"));
    }

    private String getBuildProperty(String key) {
        String value;
        try {
            value = (String) Class.forName("android.os.SystemProperties").getMethod(
                "get", String.class).invoke(null, key);
            if (value.length() < 2) value = "unknown";
        } catch (Exception e) {
            value = "unknown";
        }
        return value;
    }
}

