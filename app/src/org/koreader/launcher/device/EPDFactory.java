/* generic EPD Controller for Android devices,
 * based on https://github.com/unwmun/refreshU */

package org.koreader.launcher.device;

import java.util.Locale;

import org.koreader.launcher.Logger;
import org.koreader.launcher.device.rockchip.RK3026EPDController;
import org.koreader.launcher.device.rockchip.RK3066EPDController;
import org.koreader.launcher.device.rockchip.RK3368EPDController;
import org.koreader.launcher.device.freescale.NTXNewEPDController;


public class EPDFactory {

    public static EPDController getEPDController() {
        EPDController epdController = null;
        String controllerName = null;

        switch (DeviceInfo.EINK) {

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
            Logger.i(String.format(Locale.US,
                "[EPDFactory]: Using %s EPD Controller", controllerName));
        }

        return epdController;
    }

    private static class FakeEPDController implements EPDController {
        @Override
        public void setEpdMode(android.view.View targetView,
                               int mode, long delay,
                               int x, int y, int width, int height, String epdMode)
        {
            Logger.w("[EPDController]: Unknown device, ignoring epd update");
        }
    }
}
