/**
 * This file was created by unw on 15. 3. 31 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device;

import android.view.View;

import org.koreader.device.rockchip.C67EPDController;
import org.koreader.device.rockchip.T61EPDController;
import org.koreader.device.rockchip.T62EPDController;
import org.koreader.device.rockchip.EnergyEPDController;


public class EPDFactory {

    public static EPDController getEPDController() {
        EPDController epdController = null;
        switch (DeviceInfo.CURRENT_DEVICE) {
            case EINK_BOYUE_T61 :
                epdController = new T61EPDController();
                break;
            case EINK_BOYUE_T62 :
                epdController = new T62EPDController();
                break;
            case EINK_ONYX_C67 :
                epdController = new C67EPDController();
                break;
            case EINK_ENERGY :
                epdController = new EnergyEPDController();
                break;
            case UNKNOWN :
                epdController = new FakeEPDController();
                break;

            default : break;
        }
        return epdController;
    }


    private static class FakeEPDController implements EPDController
    {
        @Override
        public void setEpdMode(View targetView, String epdMode) {
            /**
             * Do not apply epd mode on general devices.
             */
        }
    }
}
