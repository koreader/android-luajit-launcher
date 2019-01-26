package org.koreader.device.rockchip;

import android.view.View;

import org.koreader.device.EPDController;


public class EnergyEPDController extends RK30xxEPDController implements EPDController
{
    @Override
    public void setEpdMode(View targetView, String epdMode) {
        requestEpdMode(targetView, epdMode, true);
    }
}
