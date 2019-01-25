package com.unw.device.epdcontrol;

import android.view.View;

import com.unw.device.epdcontrol.rockchip.C67EPDController;
import com.unw.device.epdcontrol.rockchip.T61EPDController;
import com.unw.device.epdcontrol.rockchip.T62EPDController;

/**
 * Created by unw on 15. 3. 31..
 */
public class EPDFactory {

    public static EPDController getEPDController()
    {
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
            // 아무것도 안해줌
            // 일반 장비에서는 epd모드를 적용시키지 않음.
        }
    }

}
