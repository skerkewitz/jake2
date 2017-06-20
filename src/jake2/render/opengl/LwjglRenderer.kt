/*
 * LwjglRenderer.java
 * Copyright (C) 2004
 *
 * $Id: LwjglRenderer.java,v 1.5 2007-01-11 23:20:40 cawe Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.render.opengl

import jake2.Defines
import jake2.client.DisplayMode
import jake2.client.TRefDef
import jake2.client.VID
import jake2.common.Dimension
import jake2.common.render.TRenderExport
import jake2.qcommon.TXCommand
import jake2.render.Base
import jake2.render.TImage
import jake2.render.TModel
import jake2.sys.GlfwKeyboardImpl
import jake2.sys.Keyboard
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWWindowFocusCallbackI
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11


/**
 * LwjglRenderer

 * @author dsanders/cwei
 */
internal class LwjglRenderer private constructor() : GLDriver, TRenderExport, Ref {

    // is assign from RendererFactory factory
    private var impl: RenderAPI? = null

    private var keyboard: Keyboard = GlfwKeyboardImpl() //new LWJGLKBD();

    //	private DisplayMode oldDisplayMode;

    // ============================================================================
    // public interface for RendererFactory implementations
    //
    // TRenderExport (ref.h)
    // ============================================================================

    /**
     * @see TRenderExport.init
     */
    override fun init(vid_xpos: Int, vid_ypos: Int): Boolean {
//        super.init(vid_xpos, vid_ypos)
        // init the OpenGL drivers
        impl!!.setGLDriver(this)

        // pre init
        if (!impl!!.R_Init(vid_xpos, vid_ypos)) return false
        // post init
        return impl!!.R_Init2()
    }

    /**
     * @see TRenderExport.Shutdown
     */
    override fun Shutdown() {
        impl!!.R_Shutdown()
    }

    /**
     * @see TRenderExport.beginRegistration
     */
    override fun beginRegistration(map: String) {
        impl!!.R_BeginRegistration(map)
    }

    /**
     * @see TRenderExport.registerModel
     */
    override fun registerModel(name: String): TModel? {
        return impl!!.R_RegisterModel(name)
    }

    /**
     * @see TRenderExport.registerSkin
     */
    override fun registerSkin(name: String): TImage {
        return impl!!.R_RegisterSkin(name)
    }

    /**
     * @see TRenderExport.registerPic
     */
    override fun registerPic(name: String): TImage {
        return impl!!.Draw_FindPic(name)
    }

    /**
     * @see TRenderExport.setSky
     */
    override fun setSky(name: String, rotate: Float, axis: FloatArray) {
        impl!!.R_SetSky(name, rotate, axis)
    }

    /**
     * @see TRenderExport.endRegistration
     */
    override fun endRegistration() {
        impl!!.R_EndRegistration()
    }

    /**
     * @see TRenderExport.renderFrame
     */
    override fun renderFrame(fd: TRefDef) {
        impl!!.R_RenderFrame(fd)
    }


    override fun DrawGetPicSize(dim: Dimension, name: String) {
        impl!!.Draw_GetPicSize(dim, name)
    }

    /**
     * @see TRenderExport.DrawPic
     */
    override fun DrawPic(x: Int, y: Int, name: String) {
        impl!!.Draw_Pic(x, y, name)
    }

    /**
     * @see TRenderExport.DrawStretchPic
     */
    override fun DrawStretchPic(x: Int, y: Int, w: Int, h: Int, name: String) {
        impl!!.Draw_StretchPic(x, y, w, h, name)
    }

    /**
     * @see TRenderExport.DrawChar
     */
    override fun DrawChar(x: Int, y: Int, num: Int) {
        impl!!.Draw_Char(x, y, num)
    }

    /**
     * @see TRenderExport.DrawTileClear
     */
    override fun DrawTileClear(x: Int, y: Int, w: Int, h: Int, name: String) {
        impl!!.Draw_TileClear(x, y, w, h, name)
    }

    /**
     * @see TRenderExport.DrawFill
     */
    override fun DrawFill(x: Int, y: Int, w: Int, h: Int, c: Int) {
        impl!!.Draw_Fill(x, y, w, h, c)
    }

    /**
     * @see TRenderExport.DrawFadeScreen
     */
    override fun DrawFadeScreen() {
        impl!!.Draw_FadeScreen()
    }

    /**
     * @see TRenderExport.DrawStretchRaw
     */
    override fun DrawStretchRaw(x: Int, y: Int, w: Int, h: Int, cols: Int, rows: Int, data: ByteArray) {
        impl!!.Draw_StretchRaw(x, y, w, h, cols, rows, data)
    }

    /**
     * @see TRenderExport.CinematicSetPalette
     */
    override fun CinematicSetPalette(palette: ByteArray?) {
        impl!!.R_SetPalette(palette)
    }

    /**
     * @see TRenderExport.BeginFrame
     */
    override fun BeginFrame(camera_separation: Float) {
        impl!!.R_BeginFrame(camera_separation)
    }

    /**
     * @see TRenderExport.EndFrame
     */
    override fun EndFrame() {
        endFrame()
    }

    /**
     * @see TRenderExport.appActivate
     */
    override fun appActivate(activate: Boolean) {
        appActivate(activate)
    }

    override fun screenshot() {
        impl!!.GL_ScreenShot_f()
    }

    override fun apiVersion(): Int {
        return Defines.API_VERSION
    }

    override fun getModeList(): Array<DisplayMode> {
        return arrayOf()
    }

    override fun getKeyboardHandler(): Keyboard {
        return keyboard
    }

    // ============================================================================
    // Ref interface
    // ============================================================================

    override fun getName(): String {
        return DRIVER_NAME
    }

    override fun toString(): String {
        return DRIVER_NAME
    }

    override fun GetRefAPI(renderer: RenderAPI): TRenderExport {
        this.impl = renderer
        return this
    }

    companion object {
        val DRIVER_NAME = "lwjgl"
        var window: Long = 0

        init {
            RendererFactory.register(LwjglRenderer())
        }
    }


    //	private java.awt.DisplayMode toAwtDisplayMode(DisplayMode m) {
    //		return new java.awt.DisplayMode(m.getWidth(), m.getHeight(), m
    //						.getBitsPerPixel(), m.getFrequency());
    //	}

    //	public java.awt.DisplayMode[] getModeList() {
    //		DisplayMode[] modes;
    //		try {
    //			modes = Display.getAvailableDisplayModes();
    //		} catch (LWJGLException e) {
    //			Command.Println(e.getMessage());
    //			return new java.awt.DisplayMode[0];
    //		}
    //		LinkedList l = new LinkedList();
    //		l.add(toAwtDisplayMode(oldDisplayMode));
    //
    //		for (int i = 0; i < modes.length; i++) {
    //			DisplayMode m = modes[i];
    //
    //			if (m.getBitsPerPixel() != oldDisplayMode.getBitsPerPixel())
    //				continue;
    //			if (m.getFrequency() > oldDisplayMode.getFrequency())
    //				continue;
    //			if (m.getHeight() < 240 || m.getWidth() < 320)
    //				continue;
    //
    //			int j = 0;
    //			java.awt.DisplayMode ml = null;
    //			for (j = 0; j < l.size(); j++) {
    //				ml = (java.awt.DisplayMode) l.get(j);
    //				if (ml.getWidth() > m.getWidth())
    //					break;
    //				if (ml.getWidth() == m.getWidth()
    //								&& ml.getHeight() >= m.getHeight())
    //					break;
    //			}
    //			if (j == l.size()) {
    //				l.addLast(toAwtDisplayMode(m));
    //			} else if (ml.getWidth() > m.getWidth()
    //							|| ml.getHeight() > m.getHeight()) {
    //				l.add(j, toAwtDisplayMode(m));
    //			} else if (m.getFrequency() > ml.getRefreshRate()) {
    //				l.remove(j);
    //				l.add(j, toAwtDisplayMode(m));
    //			}
    //		}
    //		java.awt.DisplayMode[] ma = new java.awt.DisplayMode[l.size()];
    //		l.toArray(ma);
    //		return ma;
    //	}
    //
    //	public DisplayMode[] getLWJGLModeList() {
    //		DisplayMode[] modes;
    //		try {
    //			modes = Display.getAvailableDisplayModes();
    //		} catch (LWJGLException e) {
    //			Command.Println(e.getMessage());
    //			return new DisplayMode[0];
    //		}
    //
    //		LinkedList l = new LinkedList();
    //		l.add(oldDisplayMode);
    //
    //		for (int i = 0; i < modes.length; i++) {
    //			DisplayMode m = modes[i];
    //
    //			if (m.getBitsPerPixel() != oldDisplayMode.getBitsPerPixel())
    //				continue;
    //			if (m.getFrequency() > Math.max(60, oldDisplayMode.getFrequency()))
    //				continue;
    //			if (m.getHeight() < 240 || m.getWidth() < 320)
    //				continue;
    //			if (m.getHeight() > oldDisplayMode.getHeight() || m.getWidth() > oldDisplayMode.getWidth())
    //				continue;
    //
    //			int j = 0;
    //			DisplayMode ml = null;
    //			for (j = 0; j < l.size(); j++) {
    //				ml = (DisplayMode) l.get(j);
    //				if (ml.getWidth() > m.getWidth())
    //					break;
    //				if (ml.getWidth() == m.getWidth()
    //								&& ml.getHeight() >= m.getHeight())
    //					break;
    //			}
    //			if (j == l.size()) {
    //				l.addLast(m);
    //			} else if (ml.getWidth() > m.getWidth()
    //							|| ml.getHeight() > m.getHeight()) {
    //				l.add(j, m);
    //			} else if (m.getFrequency() > ml.getFrequency()) {
    //				l.remove(j);
    //				l.add(j, m);
    //			}
    //		}
    //		DisplayMode[] ma = new DisplayMode[l.size()];
    //		l.toArray(ma);
    //		return ma;
    //	}

    //	private DisplayMode findDisplayMode(Dimension dim) {
    //		DisplayMode mode = null;
    //		DisplayMode m = null;
    //		DisplayMode[] modes = getLWJGLModeList();
    //		int w = dim.width;
    //		int h = dim.height;
    //
    //		for (int i = 0; i < modes.length; i++) {
    //			m = modes[i];
    //			if (m.getWidth() == w && m.getHeight() == h) {
    //				mode = m;
    //				break;
    //			}
    //		}
    //		if (mode == null)
    //			mode = oldDisplayMode;
    //		return mode;
    //	}
    //
    //	String getModeString(DisplayMode m) {
    //		StringBuffer sb = new StringBuffer();
    //		sb.append(m.getWidth());
    //		sb.append('x');
    //		sb.append(m.getHeight());
    //		sb.append('x');
    //		sb.append(m.getBitsPerPixel());
    //		sb.append('@');
    //		sb.append(m.getFrequency());
    //		sb.append("Hz");
    //		return sb.toString();
    //	}

    /**
     * @param dim
     * *
     * @param mode
     * *
     * @param fullscreen
     * *
     * @return enum rserr_t
     */
    override fun setMode(dim: Dimension, mode: Int, fullscreen: Boolean): Int {
        var dim = dim

        //		Dimension newDim = new Dimension();

        VID.Printf(VID.PRINT_ALL, "Initializing OpenGL display\n")

        VID.Printf(VID.PRINT_ALL, "...setting mode $mode:")

        // Setup an error callback. The default implementation
        // will print the error message in QSystem.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11.GL_TRUE)
//        glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
//        glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1)
        dim = Dimension(640, 480)

        window = GLFW.glfwCreateWindow(dim.width, dim.height, "Quake2", 0, 0)
        if (window == 0L)
            throw RuntimeException("Failed to create the GLFW window")


        GLFW.glfwSetWindowFocusCallback(window, GLFWWindowFocusCallbackI { window, focused ->
            System.out.println("Window got focus " + focused)
        })

        GLFW.glfwSetWindowPos(window, 100, 100)

        GLFW.glfwFocusWindow(window)
        GLFW.glfwMakeContextCurrent(window)

        GLFW.glfwSetInputMode(window, GLFW.GLFW_STICKY_KEYS, GLFW.GLFW_TRUE)

        // Make the window visible
        GLFW.glfwShowWindow(window)

        GLFW.glfwMakeContextCurrent(window)

        // Enable v-sync
        GLFW.glfwSwapInterval(1)

        // This line is critical for LWJGL'entityState interoperation with GLFW'entityState
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        //        /*
        //         * fullscreen handling
        //         */
        //		if (oldDisplayMode == null) {
        //			oldDisplayMode = Display.getDisplayMode();
        //		}

        //		if (!VID.GetModeInfo(newDim, mode)) {
        //			VID.Printf(Defines.PRINT_ALL, " invalid mode\n");
        //			return Base.rserr_invalid_mode;
        //		}

        val newDim = dim

        VID.Printf(VID.PRINT_ALL, " " + newDim.width + " " + newDim.height
                + '\n')

        // destroy the existing window
        shutdown()

        //		Display.setTitle("Jake2 (lwjgl)");
        //
        //		DisplayMode displayMode = findDisplayMode(newDim);
        //		newDim.width = displayMode.getWidth();
        //		newDim.height = displayMode.getHeight();

        //		if (fullscreen) {
        //			try {
        //				Display.setDisplayMode(displayMode);
        //			} catch (LWJGLException e) {
        //				return Base.rserr_invalid_mode;
        //			}
        //
        //			Display.setLocation(0, 0);
        //
        //			try {
        //				Display.setFullscreen(fullscreen);
        //			} catch (LWJGLException e) {
        //				return Base.rserr_invalid_fullscreen;
        //			}
        //
        //			VID.Printf(Defines.PRINT_ALL, "...setting fullscreen "
        //							+ getModeString(displayMode) + '\n');
        //
        //		} else {
        //			try {
        //				Display.setDisplayMode(displayMode);
        //			} catch (LWJGLException e) {
        //				return Base.rserr_invalid_mode;
        //			}
        //
        //			try {
        //				Display.setFullscreen(false);
        //			} catch (LWJGLException e) {
        //				return Base.rserr_invalid_fullscreen;
        //			}
        //			//Display.setLocation(window_xpos, window_ypos);
        //		}

        Base.setVid(newDim.width, newDim.height)

        //		vid.width = newDim.width;
        //		vid.height = newDim.height;

        //		try {
        //			Display.create();
        //		} catch (LWJGLException e) {
        //			return Base.rserr_unknown;
        //		}

        // let the sound and input subsystems know about the new window
        VID.NewWindow(newDim.width, newDim.height)
        return Base.rserr_ok
    }

    override fun shutdown() {
        //		if (oldDisplayMode != null && Display.isFullscreen()) {
        //			try {
        //				Display.setDisplayMode(oldDisplayMode);
        //			} catch (Exception e) {
        //				e.printStackTrace();
        //			}
        //		}
        //
        //		while (Display.isCreated()) {
        //			Display.destroy();
        //		}
    }

//    /**
//     * @return true
//     */
//    override fun init(xpos: Int, ypos: Int): Boolean {
//        // do nothing
//        window_xpos = xpos
//        window_ypos = ypos
//        return true
//    }

    override fun beginFrame(camera_separation: Float) {
        // do nothing
    }

    override fun endFrame() {
        GL11.glFlush()
        // swap buffers
        GLFW.glfwSwapBuffers(window)
        //Display.update();
    }

    override fun enableLogging(enable: Boolean) {
        // do nothing
    }

    override fun logNewFrame() {
        // do nothing
    }

    /**
     * this is a hack for jogl renderers.

     * @param callback
     */
    override fun updateScreen(callback: TXCommand) {
        callback.execute()
    }


}