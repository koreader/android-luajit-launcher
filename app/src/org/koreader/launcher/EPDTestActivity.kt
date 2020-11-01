package org.koreader.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.koreader.launcher.device.epd.freescale.NTXEPDController
import org.koreader.launcher.device.epd.rockchip.RK30xxEPDController
import org.koreader.launcher.device.epd.rockchip.RK33xxEPDController
import java.util.*

/* A test activity for EPD routines. It can be called from lua using the android.einkTest() function
   If the device in question doesn't play nice with the main NativeActivity it can be called from
   commandline using `adb shell am start -n org.koreader.launcher/.EPDTestActivity` */

class EPDTestActivity : Activity() {
    private lateinit var info: TextView

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        info = findViewById(R.id.info)
        val readmeReport: TextView = findViewById(R.id.readmeReport)
        val rk30xxDescription: TextView = findViewById(R.id.rk30xxText)
        val rk33xxDescription: TextView = findViewById(R.id.rk33xxText)
        val ntxNewDescription: TextView = findViewById(R.id.ntxNewText)
        val rk30xxButton: Button = findViewById(R.id.rk30xxButton)
        val rk33xxButton: Button = findViewById(R.id.rk33xxButton)
        val ntxNewButton: Button = findViewById(R.id.ntxNewButton)
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

        readmeReport.text = "Did you see a flashing black to white eink update? Cool\n\n"
        readmeReport.append("Go to github.com/koreader/koreader/issues/4551 ")
        readmeReport.append("and share the following information with us")

        /* rockchip rk30xx */
        rk30xxDescription.text = "This button should invoke a full refresh of Boyue T61/T62 clones."
        rk30xxButton.setOnClickListener { runEinkTest(RK30xxTEST) }

        /* rockchip rk33xx */
        rk33xxDescription.text = "This button should work on boyue rk3368 clones."
        rk33xxButton.setOnClickListener { runEinkTest(RK33xxTEST) }

        /* freescale/ntx - Newer Tolino/Nook devices */
        ntxNewDescription.text = "This button should work on modern Tolinos/Nooks and other ntx boards"
        ntxNewButton.setOnClickListener { runEinkTest(NTXTEST) }

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
            } else if (test == NTXTEST) {
                // get screen width and height
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                val width = size.x
                val height = size.y
                info.append("tolino: ")
                if (NTXEPDController.requestEpdMode(v, 34, 50, 0, 0, width, height)) success = true
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
        private val MANUFACTURER = android.os.Build.MANUFACTURER
        private val BRAND = android.os.Build.BRAND
        private val MODEL = android.os.Build.MODEL
        private val PRODUCT = android.os.Build.PRODUCT
        private val HARDWARE = android.os.Build.HARDWARE
    }
}
