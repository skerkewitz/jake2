package jake2.render.opengl

import jake2.client.Dimension
import jake2.qcommon.TXCommand

interface GLDriver {

    fun init(xpos: Int, ypos: Int): Boolean

    fun setMode(dim: Dimension, mode: Int, fullscreen: Boolean): Int

    fun shutdown()

    fun beginFrame(camera_separation: Float)

    fun endFrame()

    fun appActivate(activate: Boolean)

    fun enableLogging(enable: Boolean)

    fun logNewFrame()

    //    java.awt.DisplayMode[] getModeList();

    fun updateScreen(callback: TXCommand)

    fun screenshot()

}
