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

/* A test activity for EPD routines. It can be called from lua using the android.einkTest() function
   If the device in question doesn't play nice with the main Activity it can be called from
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

        binding.ntxNewButton.setOnClickListener {
            runEinkTest(TEST_NTX)
        }

        binding.qualcommButton.setOnClickListener {
            runEinkTest(TEST_QUALCOMM)
        }

        binding.rk30xxButton.setOnClickListener {
            runEinkTest(TEST_RK30XX)
        }

        binding.rk33xxButton.setOnClickListener {
            runEinkTest(TEST_RK33XX)
        }

        binding.shareButton.setOnClickListener {
            val intent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, binding.info.text.toString())
                type = "text/plain"
            }
            startActivity(Intent.createChooser(intent,
                resources.getString(R.string.common_share_rationale)))
        }
    }

    @Suppress("DEPRECATION")
    private fun runEinkTest(test: Int) {
        var success = false
        try {
            val v = window.decorView.findViewById<View>(android.R.id.content)
            when (test) {
                TEST_NTX,
                TEST_QUALCOMM -> {
                    val display = windowManager.defaultDisplay
                    val size = Point()
                    display.getSize(size)
                    if (test == TEST_NTX) {
                        binding.info.append("run test for tolino: ")
                        if (NTXEPDController.requestEpdMode(v, 34, 50, 0, 0, size.x, size.y))
                            success = true
                    } else if (test == TEST_QUALCOMM) {
                        binding.info.append("run test for qualcomm: ")
                        if (QualcommEPDController.requestEpdMode(v, 98, 50, 0, 0, size.x, size.y))
                            success = true
                    }
                }

                TEST_RK30XX -> {
                    binding.info.append("run test for rk30xx: ")
                    if (RK30xxEPDController.requestEpdMode(v, "EPD_FULL", true))
                        success = true
                }

                TEST_RK33XX -> {
                    binding.info.append("run test for rk33xx: ")
                    if (RK33xxEPDController.requestEpdMode("EPD_FULL"))
                        success = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (success)
                binding.info.append("ok\n")
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
