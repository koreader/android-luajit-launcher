/**
 * This file was created by unw on 15. 3. 31 as part of
 * https://github.com/unwmun/refreshU
 */

package org.koreader.device;

import android.view.View;
import android.util.Log;

import org.koreader.device.rockchip.RK3026EPDController;
import org.koreader.device.rockchip.RK3066EPDController;
import org.koreader.device.rockchip.RK3368EPDController;

public class EPDFactory {

    public static EPDController getEPDController(final String TAG) {
        EPDController epdController = null;
        String controllerName = null;

        switch (DeviceInfo.CURRENT_DEVICE) {

            /** Supported rk3026 devices */
            case EINK_BOYUE_T61:
            case EINK_BOYUE_T80S:
            case EINK_ONYX_C67:
            case EINK_ENERGY:
            case EINK_INKBOOK:
                controllerName = "Rockchip RK3026";
                epdController = new RK3026EPDController();
                break;

            /** supported rk3066 devices */
            case EINK_BOYUE_T62:
                controllerName = "Rockchip RK3066";
                epdController = new RK3066EPDController();
                break;

            /** supported rk3368 devices */
            case EINK_BOYUE_T80D:
            case EINK_BOYUE_T78D:
            case EINK_BOYUE_T103D:
                controllerName = "Rockchip RK3368";
                epdController = new RK3368EPDController();
                break;

            /** unsupported devices */
            case UNKNOWN:
                epdController = new FakeEPDController();
                break;

            default : break;
        }

        if (controllerName != null) {
            Log.i(TAG, String.format("Using %s EPD Controller", controllerName));
        } else {
            Log.w(TAG, "Device does not have an eink screen or driver is unsupported");
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
