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
package jake2.render;

import jake2.Defines;
import jake2.common.render.TRenderExport;
import jake2.common.Dimension;
import jake2.client.DisplayMode;
import jake2.client.TRefDef;
import jake2.render.opengl.LwjglDriver;
import jake2.sys.GlfwKeyboardImpl;
import jake2.sys.Keyboard;


/**
 * LwjglRenderer
 * 
 * @author dsanders/cwei
 */
final class LwjglRenderer extends LwjglDriver implements TRenderExport, Ref {
	
    	public static final String DRIVER_NAME = "lwjgl";

    	// FIX ME SKerkewitzas
//    	private Keyboard keyboard = null; //new LWJGLKBD();
    	private Keyboard keyboard = new GlfwKeyboardImpl(); //new LWJGLKBD();

    	// is set from Renderer factory
    	private RenderAPI impl;

	static {
		Renderer.register(new LwjglRenderer());
	}

    private LwjglRenderer() {
	}



	// ============================================================================
	// public interface for Renderer implementations
	//
	// TRenderExport (ref.h)
	// ============================================================================

	/**
	 * @see TRenderExport#Init()
	 */
	public boolean init(int vid_xpos, int vid_ypos) {
        // init the OpenGL drivers
        impl.setGLDriver(this);

		// pre init
		if (!impl.R_Init(vid_xpos, vid_ypos)) return false;
		// post init
		return impl.R_Init2();
	}

	/**
	 * @see TRenderExport#shutdown()
	 */
	public void Shutdown() {
		impl.R_Shutdown();
	}

	/**
	 * @see TRenderExport#beginRegistration(java.lang.String)
	 */
	public final void beginRegistration(String map) {
		impl.R_BeginRegistration(map);
	}

	/**
	 * @see TRenderExport#registerModel(java.lang.String)
	 */
	public final TModel registerModel(String name) {
		return impl.R_RegisterModel(name);
	}

	/**
	 * @see TRenderExport#registerSkin(java.lang.String)
	 */
	public final TImage registerSkin(String name) {
		return impl.R_RegisterSkin(name);
	}

	/**
	 * @see TRenderExport#registerPic(java.lang.String)
	 */
	public final TImage registerPic(String name) {
		return impl.Draw_FindPic(name);
	}
	/**
	 * @see TRenderExport#setSky(java.lang.String, float, float[])
	 */
	public final void setSky(String name, float rotate, float[] axis) {
		impl.R_SetSky(name, rotate, axis);
	}

	/**
	 * @see TRenderExport#endRegistration()
	 */
	public final void endRegistration() {
		impl.R_EndRegistration();
	}

	/**
	 * @see TRenderExport#renderFrame(TRefDef)
	 */
	public final void renderFrame(TRefDef fd) {
		impl.R_RenderFrame(fd);
	}


	public final void DrawGetPicSize(Dimension dim, String name) {
		impl.Draw_GetPicSize(dim, name);
	}

	/**
	 * @see TRenderExport#DrawPic(int, int, java.lang.String)
	 */
	public final void DrawPic(int x, int y, String name) {
		impl.Draw_Pic(x, y, name);
	}

	/**
	 * @see TRenderExport#DrawStretchPic(int, int, int, int, java.lang.String)
	 */
	public final void DrawStretchPic(int x, int y, int w, int h, String name) {
		impl.Draw_StretchPic(x, y, w, h, name);
	}

	/**
	 * @see TRenderExport#DrawChar(int, int, int)
	 */
	public final void DrawChar(int x, int y, int num) {
		impl.Draw_Char(x, y, num);
	}

	/**
	 * @see TRenderExport#DrawTileClear(int, int, int, int, java.lang.String)
	 */
	public final void DrawTileClear(int x, int y, int w, int h, String name) {
		impl.Draw_TileClear(x, y, w, h, name);
	}

	/**
	 * @see TRenderExport#DrawFill(int, int, int, int, int)
	 */
	public final void DrawFill(int x, int y, int w, int h, int c) {
		impl.Draw_Fill(x, y, w, h, c);
	}

	/**
	 * @see TRenderExport#DrawFadeScreen()
	 */
	public final void DrawFadeScreen() {
		impl.Draw_FadeScreen();
	}

	/**
	 * @see TRenderExport#DrawStretchRaw(int, int, int, int, int, int, byte[])
	 */
	public final void DrawStretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
		impl.Draw_StretchRaw(x, y, w, h, cols, rows, data);
	}

	/**
	 * @see TRenderExport#CinematicSetPalette(byte[])
	 */
	public final void CinematicSetPalette(byte[] palette) {
		impl.R_SetPalette(palette);
	}

	/**
	 * @see TRenderExport#beginFrame(float)
	 */
	public final void BeginFrame(float camera_separation) {
		impl.R_BeginFrame(camera_separation);
	}

	/**
	 * @see TRenderExport#endFrame()
	 */
	public final void EndFrame() {
		endFrame();
	}

	/**
	 * @see TRenderExport#appActivate(boolean)
	 */
	public final void appActivate(boolean activate) {
	    appActivate(activate);
	}

	public void screenshot() {
    	    impl.GL_ScreenShot_f();
	}

	public final int apiVersion() {
		return Defines.API_VERSION;
	}

	@Override
	public DisplayMode[] getModeList() {
		return new DisplayMode[0];
	}

	public Keyboard getKeyboardHandler() {
		return keyboard;
	}

	// ============================================================================
	// Ref interface
	// ============================================================================

	public final String getName() {
		return DRIVER_NAME;
	}

	public final String toString() {
		return DRIVER_NAME;
	}

	public final TRenderExport GetRefAPI(RenderAPI renderer) {
        	this.impl = renderer;
		return this;
	}
}