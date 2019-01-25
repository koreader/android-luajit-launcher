package com.unw.device.epdcontrol.rockchip;

import android.view.View;

import com.unw.device.epdcontrol.EPDController;

/**
 * Created by unw on 15. 3. 26..
 */
public class C67EPDController extends RK30xxEPDController implements EPDController
{
    @Override
    public void setEpdMode(View targetView, String epdMode) {
        requestEpdMode(targetView, epdMode, true);
    }
}
