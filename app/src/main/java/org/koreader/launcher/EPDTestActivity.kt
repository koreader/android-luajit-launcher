package org.koreader.launcher

import android.content.Intent
import android.graphics.Point
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.koreader.launcher.databinding.EpdTestBinding
import org.koreader.launcher.device.DeviceInfo
import org.koreader.launcher.device.epd.freescale.NTXEPDController
import org.koreader.launcher.device.epd.qualcomm.QualcommEPDController
import org.koreader.launcher.device.epd.rockchip.RK30xxEPDController
import org.koreader.launcher.device.epd.rockchip.RK33xxEPDController
import java.util.Locale

/* A test activity for EPD routines. It can be called from lua using the android.einkTest() function
   If the device in question doesn't play nice with the main NativeActivity it can be called from
   commandline using `adb shell am start -n org.koreader.launcher/.EPDTestActivity` */

class EPDTestActivity : AppCompatActivity() {
    private lateinit var binding: EpdTestBinding

    companion object {
        private const val TEST_RK30XX = 1
        private const val TEST_RK33XX = 2
        private const val TEST_NTX = 3
        private const val TEST_QUALCOMM = 4
    }

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        binding = EpdTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.info.append("Manufacturer: ${DeviceInfo.MANUFACTURER}\n")
        binding.info.append("Brand: ${DeviceInfo.BRAND}\n")
        binding.info.append("Model: ${DeviceInfo.MODEL}\n")
        binding.info.append("Device: ${DeviceInfo.DEVICE}\n")
        binding.info.append("Product: ${DeviceInfo.PRODUCT}\n")
        binding.info.append("Hardware: ${DeviceInfo.HARDWARE}\n")

        getBuildProp("ro.board.platform")?.let {
            binding.info.append("Platform: $it\n")
        }

        binding.rk30xxButton.setOnClickListener {
            runEinkTest(TEST_RK30XX)
        }

        binding.rk33xxButton.setOnClickListener {
            runEinkTest(TEST_RK33XX)
        }

        binding.ntxNewButton.setOnClickListener {
            runEinkTest(TEST_NTX)
        }

        binding.qualcommButton.setOnClickListener {
            runEinkTest(TEST_QUALCOMM)
        }

        binding.shareButton.setOnClickListener {
            val intent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, binding.info.text.toString())
                type = "text/plain"
            }
            startActivity(Intent.createChooser(intent,
                resources.getString(R.string.epd_test_share_button)))
        }
    }

    @Suppress("DEPRECATION")
    private fun runEinkTest(test: Int) {
        var success = false
        binding.info.append(String.format(Locale.US, "run test #%d -> ", test))
        try {
            val v = window.decorView.findViewById<View>(android.R.id.content)
            when (test) {
                TEST_RK30XX -> {
                    binding.info.append("rk30xx: ")
                    if (RK30xxEPDController.requestEpdMode(v, "EPD_FULL", true))
                        success = true
                }

                TEST_RK33XX -> {
                    binding.info.append("rk33xx: ")
                    if (RK33xxEPDController.requestEpdMode("EPD_FULL"))
                        success = true
                }

                TEST_NTX,
                TEST_QUALCOMM -> {
                    // get screen width and height
                    val display = windowManager.defaultDisplay
                    val size = Point()
                    display.getSize(size)
                    val width = size.x
                    val height = size.y

                    if (test == TEST_NTX) {
                        binding.info.append("tolino: ")
                        if (NTXEPDController.requestEpdMode(v, 34, 50, 0, 0, width, height))
                            success = true
                    } else if (test == TEST_QUALCOMM) {
                        binding.info.append("qualcomm: ")
                        if (QualcommEPDController.requestEpdMode(v, 98, 50, 0, 0, width, height))
                            success = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (success)
                binding.info.append("pass\n")
            else
                binding.info.append("fail\n")
        }
    }

    @Suppress("SameParameterValue")
    private fun getBuildProp(id: String): String? {
        return try {
            Class.forName("android.os.SystemProperties").getMethod(
                "get", String::class.java).invoke(null, id)
                as String?
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
