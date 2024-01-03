package org.koreader.launcher

import android.graphics.Point
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import org.koreader.launcher.databinding.TestBinding
import org.koreader.launcher.device.Device
import org.koreader.launcher.device.DeviceInfo
import org.koreader.launcher.device.EPDInterface
import org.koreader.launcher.device.LightsInterface
import org.koreader.launcher.device.epd.OnyxEPDController
import org.koreader.launcher.device.epd.RK3026EPDController
import org.koreader.launcher.device.epd.RK3368EPDController
import org.koreader.launcher.device.epd.TolinoEPDController
import org.koreader.launcher.device.lights.OnyxC67Controller
import org.koreader.launcher.device.lights.OnyxColorController
import org.koreader.launcher.device.lights.OnyxSdkLightsController
import org.koreader.launcher.device.lights.OnyxWarmthController
import org.koreader.launcher.device.lights.TolinoRootController
import org.koreader.launcher.device.lights.TolinoNtxController
import org.koreader.launcher.device.lights.TolinoNtxNoWarmthController
import org.koreader.launcher.device.lights.BoyueS62RootController
import org.koreader.launcher.dialog.LightDialog

class TestActivity: AppCompatActivity() {
    private val tag = this::class.java.simpleName

    private val epdMap = HashMap<String, EPDInterface>()
    private val lightsMap = HashMap<String, LightsInterface>()
    private val reportPath = String.format("%s%s%s", MainApp.storage_path, File.separator, "test.log")

    private lateinit var binding: TestBinding
    private lateinit var device: Device
    private var supported = false

    companion object {
        private const val MARKER_BEGIN = "kotest begin"
        private const val MARKER_END = "kotest end"
    }

    public override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.i(tag, MARKER_BEGIN)
        device = Device(this)
        supported = device.epd.getPlatform() != "none" || device.lights.getPlatform() != "generic"

        if (supported) {
            binding.currentState.append("Device already supported\n")
            binding.currentState.append("EPD: ${device.epd.getPlatform()}\n")
            binding.currentState.append("Lights: ${device.lights.getPlatform()}\n")
        } else {
            binding.currentState.append("Unsupported device\n")
        }

        // EPD drivers
        epdMap["Onyx/Qualcomm"] = OnyxEPDController()
        epdMap["Rockchip RK3026"] = RK3026EPDController()
        epdMap["Rockchip RK3368"] = RK3368EPDController()
        epdMap["Freescale/NTX"] = TolinoEPDController()

        // Lights drivers
        lightsMap["Boyue S62 Root"] = BoyueS62RootController()
        lightsMap["Onyx C67"] = OnyxC67Controller()
        lightsMap["Onyx Color"] = OnyxColorController()
        lightsMap["Onyx SDK (lights)"] = OnyxSdkLightsController()
        lightsMap["Onyx (warmth)"] = OnyxWarmthController()
        lightsMap["Tolino Root"] = TolinoRootController()
        lightsMap["Tolino Ntx"] = TolinoNtxController()
        lightsMap["Tolino Ntx (no warmth)"] = TolinoNtxNoWarmthController()

        // Device ID
        binding.info.append("Manufacturer: ${DeviceInfo.MANUFACTURER}\n")
        binding.info.append("Brand: ${DeviceInfo.BRAND}\n")
        binding.info.append("Model: ${DeviceInfo.MODEL}\n")
        binding.info.append("Device: ${DeviceInfo.DEVICE}\n")
        binding.info.append("Product: ${DeviceInfo.PRODUCT}\n")
        binding.info.append("Hardware: ${DeviceInfo.HARDWARE}\n")

        try {
            Class.forName("android.os.SystemProperties").getMethod(
                "get", String::class.java).invoke(null, "ro.board.platform")
                as String?
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }?.let {
            if (it.isNotEmpty())
                binding.info.append("Platform: $it\n")
        }

        val epdOptions: ArrayList<String> = ArrayList()
        for ((key, _) in epdMap) {
            epdOptions.add(key)
        }

        val lightsOptions: ArrayList<String> = ArrayList()
        for ((key, _) in lightsMap) {
            lightsOptions.add(key)
        }

        val epdAdapter: ArrayAdapter<String> = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, epdOptions)

        epdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEpd.adapter = epdAdapter

        val lightsAdapter: ArrayAdapter<String> = ArrayAdapter(this,
            android.R.layout.simple_spinner_item, lightsOptions)
        lightsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLights.adapter = lightsAdapter

        binding.buttonTryEpd.setOnClickListener {
            runEpd(binding.spinnerEpd.selectedItem.toString())
        }
        binding.buttonTryLights.setOnClickListener {
            runLights(binding.spinnerLights.selectedItem.toString())
        }

        binding.shareButton.setOnClickListener {
            val msg = if (dumpTestLogs()) {
                String.format("Report saved to %s", reportPath)
            } else {
                String.format("Error saving report.\nPlease give storage permissions first")
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }

        binding.instructions.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(tag, MARKER_END)
    }
    private fun runLights(id: String) {
        lightsMap[id]?.let { driver ->
            Log.i(tag, String.format("running lights test: %s", id))
            val dialog = LightDialog()
            val title = String.format("Test %s", id)
            dialog.show(
                this, driver, title,
                "dim", "warmth", "ok", "cancel"
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun runEpd(id: String) {
        try {
            Log.i(tag, String.format("running epd test: %s", id))
            val v = binding.root
            epdMap[id]?.let { driver ->
                when (id) {
                    "Freescale/NTX",
                    "Onyx/Qualcomm" -> {
                        val display = windowManager.defaultDisplay
                        val size = Point()
                        display.getSize(size)
                        if (id == "Freescale/NTX") {
                            driver.setEpdMode(v, 34, 50, 0, 0, size.x, size.y, null)
                        } else {
                            driver.setEpdMode(v, 98, 50, 0, 0, size.x, size.y, null)
                        }
                    }

                    "Rockchip RK3026",
                    "Rockchip RK3368" -> {
                        driver.setEpdMode(v, 0, 0, 0, 0, 0, 0, "EPD_FULL")
                    }
                    else -> {
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dumpTestLogs(): Boolean {
        var isTestLog = false
        return File(reportPath).let {
            if (it.exists()) it.delete()
            try {
                it.printWriter().use { log ->
                    log.append("Device info:\n")
                        .append(binding.info.text.toString())
                        .append("\n\nTest logs:\n")

                    val proc = Runtime.getRuntime().exec("logcat -d -v time")
                    val buffer = BufferedReader(InputStreamReader(proc.inputStream))
                    while (true) {
                        buffer.readLine()?.let { line ->
                            when {
                                line.contains(MARKER_BEGIN, ignoreCase = true) -> {
                                    isTestLog = true
                                }
                                line.contains(MARKER_END, ignoreCase = true) -> {
                                    isTestLog = false
                                }
                                isTestLog -> {
                                    log.append("$line\n")
                                }
                                else -> {
                                }
                            }
                        } ?: break
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
