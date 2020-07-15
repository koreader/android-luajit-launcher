package org.koreader.launcher.surfaces

import java.util.Locale
import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.koreader.launcher.utils.Logger

class NativeSurfaceView(context: Context): SurfaceView(context),
    SurfaceHolder.Callback {
    val tag = "NativeSurfaceView"
    init { holder.addCallback(this) }
    override fun surfaceCreated(holder: SurfaceHolder) {
        Logger.v(tag, "surface created")
        setWillNotDraw(false)
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Logger.v(tag, String.format(Locale.US,
            "surface changed {\n  width:  %d\n  height: %d\n}", width, height))
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Logger.v(tag, "surface destroyed")
    }
}
