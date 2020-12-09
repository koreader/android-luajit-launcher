package org.koreader.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.koreader.launcher.device.epd.freescale.NTXEPDController
import org.koreader.launcher.device.epd.qualcomm.QualcommEPDController
import org.koreader.launcher.device.epd.rockchip.RK30xxEPDController
import org.koreader.launcher.device.epd.rockchip.RK33xxEPDController
import java.util.*

/* A test activity for EPD routines. It can be called from lua using the android.einkTest() function
   If the device in question doesn't play nice with the main NativeActivity it can be called from
   commandline using `adb shell am start -n org.koreader.launcher/.EPDTestActivity` */

@Suppress("DEPRECATION")
class EPDTestActivity : Activity() {
    private lateinit var info: TextView

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.epdtest)
        info = findViewById(R.id.info)
        val readmeReport: TextView = findViewById(R.id.readmeReport)
        val rk30xxDescription: TextView = findViewById(R.id.rk30xxText)
        val rk33xxDescription: TextView = findViewById(R.id.rk33xxText)
        val ntxNewDescription: TextView = findViewById(R.id.ntxNewText)
        val qualcommDescription: TextView = findViewById(R.id.qualcommText)
        val rk30xxButton: Button = findViewById(R.id.rk30xxButton)
        val rk33xxButton: Button = findViewById(R.id.rk33xxButton)
        val ntxNewButton: Button = findViewById(R.id.ntxNewButton)
        val qualcommButton: Button = findViewById(R.id.qualcommButton)
        val shareButton: Button = findViewById(R.id.shareButton)

        /* current device info */
        info.append("Manufacturer: $MANUFACTURER\n")
        info.append("Brand: $BRAND\n")
        info.append("Model: $MODEL\n")
        info.append("Product: $PRODUCT\n")
        info.append("Hardware: $HARDWARE\n")

        /* add platform if available */
        var platform: String? = "unknown"
        try {
            platform = Class.forName("android.os.SystemProperties").getMethod(
                    "get", String::class.java).invoke(null, "ro.board.platform")
                as String?
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            platform?.let { info.append("Platform: $it\n") }
        }

        readmeReport.text = resources.getString(R.string.epdtest_about)

        /* rockchip rk30xx */
        rk30xxDescription.text = resources.getString(R.string.epdtest_rk30xx)
        rk30xxButton.setOnClickListener { runEinkTest(RK30xxTEST) }

        /* rockchip rk33xx */
        rk33xxDescription.text = resources.getString(R.string.epdtest_rk33xx)
        rk33xxButton.setOnClickListener { runEinkTest(RK33xxTEST) }

        /* freescale/ntx - Newer Tolino/Nook devices */
        ntxNewDescription.text = resources.getString(R.string.epdtest_ntx)
        ntxNewButton.setOnClickListener { runEinkTest(NTXTEST) }

        /* qualcomm - At least Onyx Boox Nova 2 */
        qualcommDescription.text = resources.getString(R.string.epdtest_qualcomm)
        qualcommButton.setOnClickListener { runEinkTest(QUALCOMMTEST) }

        /* share button */
        shareButton.setOnClickListener { shareText(info.text.toString()) }
    }

    private fun runEinkTest(test: Int) {
        var success = false
        info.append(String.format(Locale.US, "run test #%d -> ", test))
        try {
            val v = window.decorView.findViewById<View>(android.R.id.content)
            if (test == RK30xxTEST) {
                info.append("rk30xx: ")
                // force a flashing black->white update
                if (RK30xxEPDController.requestEpdMode(v, "EPD_FULL", true)) success = true
            } else if (test == RK33xxTEST) {
                info.append("rk33xx: ")
                if (RK33xxEPDController.requestEpdMode("EPD_FULL")) success = true
            } else if (test == NTXTEST || test == QUALCOMMTEST) {
                // get screen width and height
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                val width = size.x
                val height = size.y
                if (test == NTXTEST) {
                    info.append("tolino: ")
                    if (NTXEPDController.requestEpdMode(v, 34, 50, 0, 0, width, height)) success = true
                } else if (test == QUALCOMMTEST) {
                    info.append("qualcomm: ")
                    if (QualcommEPDController.requestEpdMode(v, 98, 50, 0, 0, width, height)) success = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (success)
            info.append("pass\n")
        else
            info.append("fail\n")
    }

    private fun shareText(text: String) {
        val i = Intent(Intent.ACTION_SEND)
        i.putExtra(Intent.EXTRA_TEXT, text)
        i.type = "text/plain"
        startActivity(Intent.createChooser(i, "e-ink test results"))
    }

    companion object {
        private const val RK30xxTEST = 1
        private const val RK33xxTEST = 2
        private const val NTXTEST = 3
        private const val QUALCOMMTEST = 4
        private val MANUFACTURER = android.os.Build.MANUFACTURER
        private val BRAND = android.os.Build.BRAND
        private val MODEL = android.os.Build.MODEL
        private val PRODUCT = android.os.Build.PRODUCT
        private val HARDWARE = android.os.Build.HARDWARE
    }
}
