/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.device;

import org.koreader.device.rockchip.RK3026EPDController;
import org.koreader.device.rockchip.RK3066EPDController;
import org.koreader.device.rockchip.RK3368EPDController;
import org.koreader.device.freescale.NTXNewEPDController;

import android.util.Log;


@SuppressWarnings("unused")
public class EPDFactory {

    public static EPDController getEPDController(final String tag) {
        EPDController epdController = null;
        String controllerName = null;

        switch (DeviceInfo.CURRENT_DEVICE) {

            /* Supported rk3026 devices */
            case BOYUE_T61:
            case BOYUE_T80S:
            case ONYX_C67:
            case ENERGY:
            case INKBOOK:
                controllerName = "Rockchip RK3026";
                epdController = new RK3026EPDController();
                break;

            /* supported rk3066 devices */
            case BOYUE_T62:
                controllerName = "Rockchip RK3066";
                epdController = new RK3066EPDController();
                break;

            /* supported rk3368 devices */
            case BOYUE_T80D:
            case BOYUE_T78D:
            case BOYUE_T103D:
                controllerName = "Rockchip RK3368";
                epdController = new RK3368EPDController();
                break;

            /* devices using imx/ntx platform */
            case CREMA:
            case TOLINO:
            case NOOK_V520:
                controllerName = "Freescale NTX";
                epdController = new NTXNewEPDController();
                break;

            /* unsupported devices */
            case UNKNOWN:
                epdController = new FakeEPDController();
                break;

            default : break;
        }

        if (controllerName != null) {
            Log.i(tag, String.format("Using %s EPD Controller", controllerName));
        }

        return epdController;
    }

    private static class FakeEPDController implements EPDController {
        @Override
        public void setEpdMode(android.view.View targetView, int mode, long delay, int x, int y, int width, int height, String epdMode) {
            /* do nothing */
        }
    }
}
