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


@SuppressWarnings("unused")
public class EPDFactory {

    public static EPDController getEPDController(final String TAG) {
        EPDController epdController = null;
        String controllerName = null;

        switch (DeviceInfo.CURRENT_DEVICE) {

            /** Supported rk3026 devices */
            case BOYUE_T61:
            case BOYUE_T80S:
            case ONYX_C67:
            case ENERGY:
            case INKBOOK:
                controllerName = "Rockchip RK3026";
                epdController = new RK3026EPDController();
                break;

            /** supported rk3066 devices */
            case BOYUE_T62:
                controllerName = "Rockchip RK3066";
                epdController = new RK3066EPDController();
                break;

            /** supported rk3368 devices */
            case BOYUE_T80D:
            case BOYUE_T78D:
            case BOYUE_T103D:
                controllerName = "Rockchip RK3368";
                epdController = new RK3368EPDController();
                break;

            /** devices using generic view methods */
            case SONY_RP1:
            case NOOK_V520:
            case EMULATOR_X86:
                controllerName = "generic android surface";
                epdController = new GenericEPDController();
                break;

            /** unsupported devices */
            case UNKNOWN:
                epdController = new FakeEPDController();
                break;

            default : break;
        }

        if (controllerName != null) {
            Log.i(TAG, String.format("Using %s EPD Controller", controllerName));
        }

        return epdController;
    }

    private static class GenericEPDController implements EPDController {
        @Override
        public void setEpdMode(View targetView, String epdMode) {
            /** just invalidate current view */
            try {
                Class.forName("android.view.SurfaceView").getMethod("postInvalidate",
                    new Class[]{}).invoke(targetView, new Object[]{});
                Log.i("epd", "root view invalidated");
            } catch (Exception e) {
                Log.e("epd", e.toString());
            }
        }
    }

    private static class FakeEPDController implements EPDController {
        @Override
        public void setEpdMode(View targetView, String epdMode) {
            /** do nothing */
        }
    }
}
