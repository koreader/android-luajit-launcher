package org.koreader.test.eink;

import java.util.Locale;

import android.content.Intent;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.koreader.launcher.device.freescale.NTXEPDController;
import org.koreader.launcher.device.rockchip.RK30xxEPDController;
import org.koreader.launcher.device.rockchip.RK33xxEPDController;


public class MainActivity extends android.app.Activity {

    // tests
    private static final int RK30xx = 1;
    private static final int RK33xx = 2;
    private static final int NTX_NEW= 3;

    // device id.
    private static final String MANUFACTURER = android.os.Build.MANUFACTURER;
    private static final String BRAND = android.os.Build.BRAND;
    private static final String MODEL = android.os.Build.MODEL;
    private static final String PRODUCT = android.os.Build.PRODUCT;
    private static final String HARDWARE = android.os.Build.HARDWARE;

    // device platform
    private String platform;

    // text view with device info
    private TextView info;

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        info = findViewById(R.id.info);
        TextView readmeReport = findViewById(R.id.readmeReport);

        TextView rk30xx_description = findViewById(R.id.rk30xxText);
        TextView rk33xx_description = findViewById(R.id.rk33xxText);
        TextView ntx_new_description = findViewById(R.id.ntxNewText);

        Button rk30xx_button = findViewById(R.id.rk30xxButton);
        Button rk33xx_button = findViewById(R.id.rk33xxButton);
        Button ntx_new_button = findViewById(R.id.ntxNewButton);
        Button share_button = findViewById(R.id.shareButton);

        /* current device info */
        info.append("Manufacturer: " + MANUFACTURER + "\n");
        info.append("Brand: " + BRAND + "\n");
        info.append("Model: " + MODEL + "\n");
        info.append("Product: " + PRODUCT + "\n");
        info.append("Hardware: " + HARDWARE + "\n");

        /* add platform if available */
        try {
            platform = (String) Class.forName("android.os.SystemProperties").getMethod(
                "get", String.class).invoke(null, "ro.board.platform");
            if (platform.length() < 2) {
                platform = "unknown";
            }
        } catch (Exception e) {
            platform = "unknown";
        } finally {
            info.append("Platform: " + platform + "\n");
        }

        readmeReport.setText("Did you see a flashing black to white eink update? Cool\n\n");
        readmeReport.append("Go to github.com/koreader/koreader/issues/4551 ");
        readmeReport.append("and share the following information with us");

        /* rockchip rk30xx */
        rk30xx_description.setText("This button should invoke a full refresh of Boyue T61/T62 clones.");

        rk30xx_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK30xx);
            }
        });

        /* rockchip rk33xx */
        rk33xx_description.setText("This button should work on boyue rk3368 clones.");

        rk33xx_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(RK33xx);
            }
        });

        /* freescale/ntx - Newer Tolino/Nook devices */
        ntx_new_description.setText("This button should work on modern Tolinos/Nooks and other ntx boards");

        ntx_new_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runEinkTest(NTX_NEW);
            }
        });

        /* share button */
        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareText(info.getText().toString());
            }
        });
    }

    private void runEinkTest(int test) {
        boolean success = false;
        info.append(String.format(Locale.US,"run test #%d -> ", test));
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
                if (NTXEPDController.requestEpdMode(v,
                    34, 50, 0, 0, width, height))
                    success = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        startActivity(Intent.createChooser(i, "e-ink test results"));
    }
}

