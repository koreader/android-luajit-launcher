package org.koreader.device.generic;

import android.util.Log;
import android.view.View;

import org.koreader.device.EPDController;


@SuppressWarnings("unused, unchecked")
public class GenericEPDController implements EPDController {
    @Override
    public void setEpdMode(View targetView, String epdMode) {

        /** just invalidate current view */
        try {
            Class.forName("android.view.View").getMethod("postInvalidate",
                new Class[]{}).invoke(null, new Object[]{});
        } catch (Exception e) {
            Log.e("epd", e.toString());
        }
    }
}
