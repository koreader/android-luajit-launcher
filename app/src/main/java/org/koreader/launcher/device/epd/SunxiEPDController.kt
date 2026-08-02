/* EPD controller for Allwinner/Sunxi based Android devices, like the Moaan W7.
 *
 * Waveform control fallback ladder, tried in order:
 *  1. ViewRootImpl.setRefreshMode (works on the Nook GL4 Plus Emperor platform)
 *  2. SurfaceControl.setRefreshMode (AOSP Android 10+)
 *  3. Window.forceGlobalRefresh (works on the inkPalm 5)
 *  4. android.eink.force.refresh broadcast
 *  5. LayoutParams.refreshMode
 *
 * Constants from sunxi-kobo.h.
 */

package org.koreader.launcher.device.epd

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.WindowManager
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import org.koreader.launcher.device.EPDInterface

class SunxiEPDController : EPDInterface {

    // Cached view for resume(), which has no view parameter.
    private var lastView = WeakReference<View>(null)

    companion object {
        private const val TAG = "EPD"

        const val SUNXI_EINK_DU_MODE = 0x02
        const val SUNXI_EINK_GC16_MODE = 0x04
        const val SUNXI_EINK_GL16_MODE = 0x20
        const val SUNXI_EINK_GLR16_MODE = 0x40
        const val SUNXI_EINK_GU16_MODE = 0x84
        const val SUNXI_EINK_NO_MERGE = Integer.MIN_VALUE // 0x80000000

        private const val FORCE_REFRESH_ACTION = "android.eink.force.refresh"

        // Reflection objects — initialized once, null if the path is unavailable.
        @Volatile private var reflectionReady = false
        private var getViewRootImplFn: Method? = null
        private var surfaceControlField: Field? = null
        private var setRefreshModeFn: Method? = null
        private var forceGlobalRefreshFn: Method? = null
        private var layoutParamsRefreshModeField: Field? = null

        @SuppressLint("BlockedPrivateApi")
        private fun initReflection() {
            if (reflectionReady) return
            try {
                getViewRootImplFn = View::class.java.getMethod("getViewRootImpl")
                val vriClass = Class.forName("android.view.ViewRootImpl")
                try {
                    val method = vriClass.getDeclaredMethod("setRefreshMode", Int::class.javaPrimitiveType)
                    method.isAccessible = true
                    setRefreshModeFn = method
                    Log.i(TAG, "Sunxi EPD: ViewRootImpl.setRefreshMode path active")
                } catch (_: NoSuchMethodException) {
                    try {
                        val scClass = Class.forName("android.view.SurfaceControl")
                        val field = vriClass.getDeclaredField("mSurfaceControl")
                        field.isAccessible = true
                        surfaceControlField = field
                        setRefreshModeFn = scClass.getMethod("setRefreshMode", Int::class.javaPrimitiveType)
                        Log.i(TAG, "Sunxi EPD: SurfaceControl path active")
                    } catch (_: NoSuchMethodException) {
                        Log.i(TAG, "Sunxi EPD: no setRefreshMode, will use forceGlobalRefresh")
                    } catch (_: NoSuchFieldException) {
                        Log.i(TAG, "Sunxi EPD: no setRefreshMode, will use forceGlobalRefresh")
                    }
                }

                try {
                    val fn = Class.forName("android.view.Window").getMethod(
                        "forceGlobalRefresh", Boolean::class.javaPrimitiveType)
                    fn.isAccessible = true
                    forceGlobalRefreshFn = fn
                    Log.i(TAG, "Sunxi EPD: forceGlobalRefresh path active")
                } catch (_: NoSuchMethodException) {
                    Log.w(TAG, "Sunxi EPD: no forceGlobalRefresh on Window")
                }

                try {
                    val field = WindowManager.LayoutParams::class.java.getDeclaredField("refreshMode")
                    field.isAccessible = true
                    layoutParamsRefreshModeField = field
                    Log.i(TAG, "Sunxi EPD: LayoutParams.refreshMode field found")
                } catch (_: NoSuchFieldException) {
                    Log.w(TAG, "Sunxi EPD: no refreshMode field on LayoutParams")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sunxi EPD: reflection path unavailable: $e")
            }
            reflectionReady = true
        }

        // PATH 1/2: set the refresh mode on the surface of the given view.
        private fun surfaceSetRefreshMode(view: View, mode: Int): Boolean {
            initReflection()
            val getFn = getViewRootImplFn ?: return false
            val setFn = setRefreshModeFn ?: return false
            return try {
                val vri = getFn.invoke(view.rootView) ?: return false
                val target = surfaceControlField?.get(vri) ?: vri
                setFn.invoke(target, mode)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Sunxi EPD: setRefreshMode failed: $e")
                false
            }
        }

        // PATH 3: force a full refresh of the next composition.
        private fun windowForceGlobalRefresh(view: View, force: Boolean): Boolean {
            initReflection()
            val fn = forceGlobalRefreshFn ?: return false
            return try {
                val window = windowOf(view)
                fn.invoke(window, force)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Sunxi EPD: forceGlobalRefresh failed: $e")
                false
            }
        }

        // PATH 4: broadcast the framework-native full refresh signal.
        private fun broadcastForceRefresh(view: View): Boolean {
            return try {
                view.context.sendBroadcast(Intent(FORCE_REFRESH_ACTION))
                Log.i(TAG, "Sunxi EPD: sent $FORCE_REFRESH_ACTION")
                true
            } catch (e: Exception) {
                Log.w(TAG, "Sunxi EPD: force refresh broadcast failed: $e")
                false
            }
        }

        // PATH 5: set Moaan's per-window refresh mode.
        private fun layoutParamsSetRefreshMode(view: View, mode: Int): Boolean {
            initReflection()
            val field = layoutParamsRefreshModeField ?: return false
            return try {
                val window = windowOf(view)
                val attributes = window.attributes
                field.setInt(attributes, mode)
                window.attributes = attributes
                true
            } catch (e: Exception) {
                Log.w(TAG, "Sunxi EPD: LayoutParams.refreshMode failed: $e")
                false
            }
        }

        private fun windowOf(view: View): android.view.Window {
            var ctx = view.context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx.window
                ctx = ctx.baseContext
            }
            if (ctx is Activity) return ctx.window
            throw IllegalStateException("no Window available for view")
        }
    }

    override fun getPlatform(): String = "sunxi"
    override fun getMode(): String {
        initReflection()
        return if (setRefreshModeFn != null) "all" else "full-only"
    }

    override fun getWaveformFull(): Int = SUNXI_EINK_NO_MERGE + SUNXI_EINK_GC16_MODE
    override fun getWaveformPartial(): Int = SUNXI_EINK_GU16_MODE
    override fun getWaveformFullUi(): Int = SUNXI_EINK_NO_MERGE + SUNXI_EINK_GLR16_MODE
    override fun getWaveformPartialUi(): Int = SUNXI_EINK_GU16_MODE
    override fun getWaveformFast(): Int = SUNXI_EINK_GU16_MODE

    override fun getWaveformDelay(): Int = 0
    override fun getWaveformDelayUi(): Int = 0
    override fun getWaveformDelayFast(): Int = 0

    override fun needsView(): Boolean = false

    override fun setEpdMode(
        targetView: View,
        mode: Int, delay: Long,
        x: Int, y: Int, width: Int, height: Int, epdMode: String?
    ) {
        lastView = WeakReference(targetView)
        // Translate the KOReader waveform (which includes the NO_MERGE bit)
        // into the raw refresh mode understood by setRefreshMode.
        val isFull = epdMode == "EPD_FULL" || mode == getWaveformFull() || mode == getWaveformFullUi()
        val rawMode = when {
            epdMode == "EPD_FULL" || mode == getWaveformFull() -> SUNXI_EINK_GC16_MODE
            mode == getWaveformFullUi() -> SUNXI_EINK_GLR16_MODE
            else -> SUNXI_EINK_GU16_MODE
        }

        if (surfaceSetRefreshMode(targetView, rawMode)) {
            Log.i(TAG, String.format(Locale.US,
                "Sunxi EPD: setRefreshMode=0x%x waveform=0x%x", rawMode, mode))
            return
        }

        if (isFull) {
            if (windowForceGlobalRefresh(targetView, true)) {
                Log.i(TAG, String.format(Locale.US,
                    "Sunxi EPD: forceGlobalRefresh waveform=0x%x", mode))
                return
            }
            if (broadcastForceRefresh(targetView)) {
                return
            }
            if (layoutParamsSetRefreshMode(targetView, rawMode)) {
                Log.i(TAG, String.format(Locale.US,
                    "Sunxi EPD: LayoutParams.refreshMode=0x%x waveform=0x%x", rawMode, mode))
                return
            }
        }
    }

    override fun resume() {
        val view = lastView.get()
        if (view == null) return
        if (surfaceSetRefreshMode(view, SUNXI_EINK_GU16_MODE)) return
        windowForceGlobalRefresh(view, false)
    }

    override fun pause() {}
}
