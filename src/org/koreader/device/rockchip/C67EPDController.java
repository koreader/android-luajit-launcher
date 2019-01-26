/**
 * This file was created by unw on 15. 3. 25 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device.rockchip;

import android.view.View;

import org.koreader.device.EPDController;


public class C67EPDController extends RK30xxEPDController implements EPDController
{
    @Override
    public void setEpdMode(View targetView, String epdMode) {
        requestEpdMode(targetView, epdMode, true);
    }
}
