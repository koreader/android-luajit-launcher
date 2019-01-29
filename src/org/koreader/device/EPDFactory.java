/**
 * This file was created by unw on 15. 3. 31 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device;

import android.view.View;

import org.koreader.device.rockchip.RK3026EPDController;
import org.koreader.device.rockchip.RK3066EPDController;


public class EPDFactory {

    public static EPDController getEPDController() {
        EPDController epdController = null;
        switch (DeviceInfo.CURRENT_DEVICE) {

            /** Supported rk3026 devices */
            case EINK_BOYUE_T61:
            case EINK_ONYX_C67:
            case EINK_ENERGY:
                epdController = new RK3026EPDController();
                break;

            /** supported rk3066 devices */
            case EINK_BOYUE_T62:
                epdController = new RK3066EPDController();
                break;

            /** unsupported devices */
            case UNKNOWN:
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
