/*
 * VID.java
 * Copyright (C) 2003
 *
 * $Id: VID.java,v 1.21 2008-03-02 14:56:22 cawe Exp $
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
package jake2.client;

import jake2.Defines;
import jake2.client.ui.*;
import jake2.common.Dimension;
import jake2.game.Cmd;
import jake2.game.TVar;
import jake2.qcommon.*;
import jake2.render.opengl.RendererFactory;
import jake2.sound.Sound;
import jake2.sys.MouseInput;
import jake2.sys.KeyboardInput;

import static jake2.Defines.MTYPE_ACTION;
import static jake2.Defines.MTYPE_SLIDER;
import static jake2.Defines.MTYPE_SPINCONTROL;
import static jake2.client.Context.re;
import static jake2.client.Context.viddef;
import static jake2.client.Key.*;


/**
 * VID is a video driver.
 * 
 * source: client/vid.h linux/vid_so.c
 * 
 * @author cwei
 */
public class VID {

	public static final int PRINT_ALL = 0;
	public static final int PRINT_DEVELOPER = 1; // only print when "developer 1"
	public static final int PRINT_ALERT = 2;

	//	   RenderMain windowed and fullscreen graphics interface module. This module
	//	   is used for both the software and OpenGL rendering versions of the
	//	   Quake refresh engine.

	// Global variables used internally by this module
	// Context.viddef
	// global video state; used by other modules

	// Structure containing functions exported from refresh DLL
	// Context.re;

	// Console variables that we need to access from this module
	public static TVar vid_gamma;
	static TVar vid_ref;			// Name of Refresh DLL loaded
	static TVar vid_xpos;			// X coordinate of window position
	static TVar vid_ypos;			// Y coordinate of window position
	static TVar vid_width;
	static TVar vid_height;
	public static TVar vid_fullscreen;

	// Global variables used internally by this module
	// void *reflib_library;		// Handle to refresh DLL 
	static boolean reflib_active = false;
	// const char so_file[] = "/etc/quake2.conf";

	/*
	==========================================================================

	DLL GLUE

	==========================================================================
	*/


	public static void Printf(int print_level, String fmt, Object ... vargs) {
		if (print_level == PRINT_ALL)
			Command.Printf(fmt, vargs);
		else
			Command.DPrintf(fmt, vargs);
	}

	// ==========================================================================

	/*
	** VID_GetModeInfo
	*/
	static vidmode_t vid_modes[] =
		{
			new vidmode_t("Mode 0: 320x240", 320, 240, 0),
			new vidmode_t("Mode 1: 400x300", 400, 300, 1),
			new vidmode_t("Mode 2: 512x384", 512, 384, 2),
			new vidmode_t("Mode 3: 640x480", 640, 480, 3),
			new vidmode_t("Mode 4: 800x600", 800, 600, 4),
			new vidmode_t("Mode 5: 960x720", 960, 720, 5),
			new vidmode_t("Mode 6: 1024x768", 1024, 768, 6),
			new vidmode_t("Mode 7: 1152x864", 1152, 864, 7),
			new vidmode_t("Mode 8: 1280x1024", 1280, 1024, 8),
			new vidmode_t("Mode 9: 1600x1200", 1600, 1200, 9),
			new vidmode_t("Mode 10: 2048x1536", 2048, 1536, 10),
			new vidmode_t("Mode 11: user", 640, 480, 11)};
	static vidmode_t fs_modes[];

	public static boolean GetModeInfo(Dimension dim, int mode) {
		if (fs_modes == null) initModeList();

		vidmode_t[] modes = vid_modes;
		if (vid_fullscreen.value != 0.0f) modes = fs_modes;
		
		if (mode < 0 || mode >= modes.length) 
			return false;
			
		dim.width = modes[mode].width;
		dim.height = modes[mode].height;
		
		return true;
	}

	/*
	** VID_NewWindow
	*/
	public static void NewWindow(int width, int height) {
		viddef.setSize(width, height);
	}

	static void FreeReflib()
	{
		if (re != null) {
			KeyboardInput keyboardInputHandler = re.getKeyboardHandler();
			if (keyboardInputHandler != null) {
				keyboardInputHandler.Close();
			}
			MouseInput.Shutdown();
		}

		re = null;
		reflib_active = false;
	}

	/*
	==============
	VID_LoadRefresh
	==============
	*/
	static boolean LoadRefresh( String name, boolean fast )
	{

		if ( reflib_active )
		{
			re.getKeyboardHandler().Close();
			MouseInput.Shutdown();

			re.Shutdown();
			FreeReflib();
		}

		Command.Printf( "------- Loading " + name + " -------\n");
		
		boolean found = false;
		
		String[] driverNames = RendererFactory.getDriverNames();
		for (int i = 0; i < driverNames.length; i++) {
			if (driverNames[i].equals(name)) {
				found = true;
				break;
			} 	
		}

		if (!found) {
			Command.Printf( "LoadLibrary(\"" + name +"\") failed\n");
			return false;
		}

		Command.Printf( "LoadLibrary(\"" + name +"\")\n" );
		re = RendererFactory.getDriver(name);
		
		if (re == null)
		{
			Command.Error(Defines.ERR_FATAL, name + " can't load but registered");
		}

		if (re.apiVersion() != Defines.API_VERSION)
		{
			FreeReflib();
			Command.Error(Defines.ERR_FATAL, name + " has incompatible api_version");
		}

		MouseInput.Real_IN_Init();

		if ( !re.init((int)vid_xpos.value, (int)vid_ypos.value) )
		{
			re.Shutdown();
			FreeReflib();
			return false;
		}

		/* init KeyboardInput */
		re.getKeyboardHandler().Init();

		Command.Printf( "------------------------------------\n");
		reflib_active = true;
		return true;
	}

	/*
	============
	VID_CheckChanges

	This function gets called once just before drawing each frame, and it'entityState sole purpose in life
	is to check to see if any of the video mode parameters have changed, and if they have to 
	update the rendering DLL and/or video mode to match.
	============
	*/
	public static void CheckChanges()
	{
	    viddef.update();
	    
		if ( vid_ref.modified )
		{
			Sound.StopAllSounds();
		}

		while (vid_ref.modified)
		{
			/*
			** refresh has changed
			*/
			vid_ref.modified = false;
			vid_fullscreen.modified = true;
			Context.cl.refresh_prepped = false;
			Context.cls.setDisableScreen(1.0f); // true;

			
			if ( !LoadRefresh( vid_ref.string, true ) )
			{
				String renderer;
				if (vid_ref.string.equals(RendererFactory.getPreferedName())) {
				    // try the default renderer as fallback after prefered
				    renderer = RendererFactory.getDefaultName();
				} else {
				    // try the prefered renderer as first fallback
				    renderer = RendererFactory.getPreferedName();
				}
				if ( vid_ref.string.equals(RendererFactory.getDefaultName())) {
				    renderer = vid_ref.string;
					Command.Printf("Refresh failed\n");
					gl_mode = ConsoleVar.get( "gl_mode", "0", 0 );
					if (gl_mode.value != 0.0f) {
						Command.Printf("Trying mode 0\n");
						ConsoleVar.SetValue("gl_mode", 0);
						if ( !LoadRefresh( vid_ref.string, false ) )
							Command.Error(Defines.ERR_FATAL, "Couldn't fall back to " + renderer +" refresh!");
					} else
						Command.Error(Defines.ERR_FATAL, "Couldn't fall back to " + renderer +" refresh!");
				}

				ConsoleVar.Set("vid_ref", renderer);

				/*
				 * drop the console if we fail to load a refresh
				 */
				if ( Context.cls.getKey_dest() != Defines.key_console )
				{
					try {
						Console.ToggleConsole_f.execute();
					} catch (Exception e) {
					}
				}
			}
			Context.cls.setDisableScreen(0.0f); //false;
		}
	}

	/*
	============
	VID_Init
	============
	*/
	public static void Init()
	{
		/* Create the video variables so we know how to start the graphics drivers */
		vid_ref = ConsoleVar.get("vid_ref", RendererFactory.getPreferedName(), TVar.CVAR_FLAG_ARCHIVE);
		vid_xpos = ConsoleVar.get("vid_xpos", "3", TVar.CVAR_FLAG_ARCHIVE);
		vid_ypos = ConsoleVar.get("vid_ypos", "22", TVar.CVAR_FLAG_ARCHIVE);
		vid_width = ConsoleVar.get("vid_width", "640", TVar.CVAR_FLAG_ARCHIVE);
		vid_height = ConsoleVar.get("vid_height", "480", TVar.CVAR_FLAG_ARCHIVE);
		vid_fullscreen = ConsoleVar.get("vid_fullscreen", "0", TVar.CVAR_FLAG_ARCHIVE);
		vid_gamma = ConsoleVar.get( "vid_gamma", "1", TVar.CVAR_FLAG_ARCHIVE );

		vid_modes[11].width = (int)vid_width.value;
		vid_modes[11].height = (int)vid_height.value;
		
		/* Add some console commands that we want to handle */
		Cmd.registerCommand("vid_restart", () -> {
			vid_modes[11].width = (int) vid_width.value;
			vid_modes[11].height = (int) vid_height.value;
			vid_ref.modified = true;
		});

		/* Disable the 3Dfx splash screen */
		// putenv("FX_GLIDE_NO_SPLASH=0");
		
		/* Start the graphics mode and load refresh DLL */
		CheckChanges();
	}

	/*
	============
	VID_Shutdown
	============
	*/
	public static void Shutdown()
	{
		if ( reflib_active )
		{
			re.getKeyboardHandler().Close();
			MouseInput.Shutdown();

			re.Shutdown();
			FreeReflib();
		}
	}

	// ==========================================================================
	// 
	//	vid_menu.c
	//
	// ==========================================================================

	static final int REF_OPENGL_JOGL = 0;
	static final int REF_OPENGL_FASTJOGL =1;
	static final int REF_OPENGL_LWJGL =2;

	static TVar gl_mode;
	static TVar gl_driver;
	static TVar gl_picmip;
	public static TVar gl_ext_palettedtexture;
	static TVar gl_swapinterval;

	/*
	====================================================================

	MENU INTERACTION

	====================================================================
	*/

	static TMenuFramework s_opengl_menu = new TMenuFramework();
	static TMenuFramework s_current_menu; // referenz

	static TMenuList s_mode_list = new TMenuList();

	static TMenuList s_ref_list = new TMenuList();

	static TMenuSlider s_tq_slider = new TMenuSlider();
	static TMenuSlider s_screensize_slider = new TMenuSlider();

	static TMenuSlider s_brightness_slider = new TMenuSlider();

	static TMenuList s_fs_box = new TMenuList();

	static TMenuList s_stipple_box = new TMenuList();
	static TMenuList s_paletted_texture_box = new TMenuList();
	static TMenuList s_vsync_box = new TMenuList();
	static TMenuList s_windowed_mouse = new TMenuList();
	static TMenuAction s_apply_action = new TMenuAction();

	static TMenuAction s_defaults_action= new TMenuAction();

	static void DriverCallback( Object unused )
	{
		s_current_menu = s_opengl_menu; // s_software_menu;
	}

	static void ScreenSizeCallback( Object s )
	{
		TMenuSlider slider = (TMenuSlider) s;

		ConsoleVar.SetValue( "viewsize", slider.curvalue * 10 );
	}

	static void BrightnessCallback( Object s )
	{
		TMenuSlider slider = (TMenuSlider) s;

		// if ( stricmp( vid_ref.string, "soft" ) == 0 ||
		//	stricmp( vid_ref.string, "softx" ) == 0 )
		if ( vid_ref.string.equalsIgnoreCase("soft") ||
			 vid_ref.string.equalsIgnoreCase("softx") )
		{
			float gamma = ( 0.8f - ( slider.curvalue/10.0f - 0.5f ) ) + 0.5f;

			ConsoleVar.SetValue( "vid_gamma", gamma );
		}
	}

	static void ResetDefaults( Object unused )
	{
		MenuInit();
	}

	static void ApplyChanges( Object unused )
	{

		/*
		** invert sense so greater = brighter, and scale to a range of 0.5 to 1.3
		*/
		// the original was modified, because on CRTs it was too dark.
		// the slider range is [5; 13]
		// gamma: [1.1; 0.7]
		float gamma = ( 0.4f - ( s_brightness_slider.curvalue/20.0f - 0.25f ) ) + 0.7f;
		// modulate:  [1.0; 2.6]
		float modulate = s_brightness_slider.curvalue * 0.2f;

		ConsoleVar.SetValue( "vid_gamma", gamma );
		ConsoleVar.SetValue( "gl_modulate", modulate);
		ConsoleVar.SetValue( "gl_picmip", 3 - s_tq_slider.curvalue );
		ConsoleVar.SetValue( "vid_fullscreen", s_fs_box.curvalue );
		ConsoleVar.SetValue( "gl_swapinterval", s_vsync_box.curvalue);
		// set always true because of vid_ref or mode changes
		gl_swapinterval.modified = true;
		ConsoleVar.SetValue( "gl_ext_palettedtexture", s_paletted_texture_box.curvalue );
		ConsoleVar.SetValue( "gl_mode", s_mode_list.curvalue );

		ConsoleVar.Set( "vid_ref", drivers[s_ref_list.curvalue] );
		ConsoleVar.Set( "gl_driver", drivers[s_ref_list.curvalue] );
		if (gl_driver.modified)
			vid_ref.modified = true;
		
		Menu.forceMenuOff();
	}

	static final String[] resolutions = 
	{
		"[320 240  ]",
		"[400 300  ]",
		"[512 384  ]",
		"[640 480  ]",
		"[800 600  ]",
		"[960 720  ]",
		"[1024 768 ]",
		"[1152 864 ]",
		"[1280 1024]",
		"[1600 1200]",
		"[2048 1536]",
		"user mode",
	};
	static String[] fs_resolutions;
	static int mode_x;
	
	static String[] refs;
	static String[] drivers;
	
	static final String[] yesno_names =
	{
		"no",
		"yes",
	};

	static void initModeList() {
		DisplayMode[] modes = re.getModeList();
		fs_resolutions = new String[modes.length];
		fs_modes = new vidmode_t[modes.length];
		for (int i = 0; i < modes.length; i++) {
			DisplayMode m = modes[i];
			StringBuffer sb = new StringBuffer(18);
			sb.append('[');
			sb.append(m.getWidth());
			sb.append(' ');
			sb.append(m.getHeight());
			while (sb.length() < 10) sb.append(' ');
			sb.append(']');
			fs_resolutions[i] = sb.toString();
			sb.setLength(0);
			sb.append("Mode ");
			sb.append(i);
			sb.append(':');
			sb.append(m.getWidth());
			sb.append('x');
			sb.append(m.getHeight());
			fs_modes[i] = new vidmode_t(sb.toString(), m.getWidth(), m.getHeight(), i);
		}
	}
	
	private static void initRefs() {
		drivers = RendererFactory.getDriverNames();
		refs = new String[drivers.length];
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < drivers.length; i++) {
			sb.setLength(0);
			sb.append("[OpenGL ").append(drivers[i]);
			while (sb.length() < 16) sb.append(" ");
			sb.append("]");
			refs[i] = sb.toString();
		}
	}

	/*
	** VID_MenuInit
	*/
	public static void MenuInit() {
		
		initRefs();
		
		if ( gl_driver == null )
			gl_driver = ConsoleVar.get( "gl_driver", RendererFactory.getPreferedName(), 0 );
		if ( gl_picmip == null )
			gl_picmip = ConsoleVar.get( "gl_picmip", "0", 0 );
		if ( gl_mode == null)
			gl_mode = ConsoleVar.get( "gl_mode", "3", 0 );
		if ( gl_ext_palettedtexture == null )
			gl_ext_palettedtexture = ConsoleVar.get( "gl_ext_palettedtexture", "1", TVar.CVAR_FLAG_ARCHIVE );

		if ( gl_swapinterval == null)
			gl_swapinterval = ConsoleVar.get( "gl_swapinterval", "0", TVar.CVAR_FLAG_ARCHIVE );

		s_mode_list.curvalue = (int)gl_mode.value;
		if (vid_fullscreen.value != 0.0f) {
			s_mode_list.itemnames = fs_resolutions;
			if (s_mode_list.curvalue >= fs_resolutions.length - 1) {
				s_mode_list.curvalue = 0;
			}
			mode_x = fs_modes[s_mode_list.curvalue].width;
		} else {
			s_mode_list.itemnames = resolutions;
			if (s_mode_list.curvalue >= resolutions.length - 1) {
				s_mode_list.curvalue = 0;
			}
			mode_x = vid_modes[s_mode_list.curvalue].width;
		}

		if ( SCR.scr_viewsize == null )
			SCR.scr_viewsize = ConsoleVar.get("viewsize", "100", TVar.CVAR_FLAG_ARCHIVE);

		s_screensize_slider.curvalue = (int)(SCR.scr_viewsize.value/10);

		for (int i = 0; i < drivers.length; i++) {
			if (vid_ref.string.equals(drivers[i])) {
				s_ref_list.curvalue = i;
			}
		}

		s_opengl_menu.x = (int)(viddef.getWidth() * 0.50f);
		s_opengl_menu.nitems = 0;
		
		s_ref_list.type = MTYPE_SPINCONTROL;
		s_ref_list.name = "driver";
		s_ref_list.x = 0;
		s_ref_list.y = 0;
		s_ref_list.callback = new TMCallback() {
			public void execute(Object self) {
				DriverCallback(self);
			}
		};
		s_ref_list.itemnames = refs;

		s_mode_list.type = MTYPE_SPINCONTROL;
		s_mode_list.name = "video mode";
		s_mode_list.x = 0;
		s_mode_list.y = 10;

		s_screensize_slider.type	= MTYPE_SLIDER;
		s_screensize_slider.x		= 0;
		s_screensize_slider.y		= 20;
		s_screensize_slider.name	= "screen size";
		s_screensize_slider.minvalue = 3;
		s_screensize_slider.maxvalue = 12;
		s_screensize_slider.callback = new TMCallback() {
			public void execute(Object self) {
				ScreenSizeCallback(self);
			}
		};
		s_brightness_slider.type	= MTYPE_SLIDER;
		s_brightness_slider.x	= 0;
		s_brightness_slider.y	= 30;
		s_brightness_slider.name	= "brightness";
		s_brightness_slider.callback =  new TMCallback() {
			public void execute(Object self) {
				BrightnessCallback(self);
			}
		};
		s_brightness_slider.minvalue = 5;
		s_brightness_slider.maxvalue = 13;
		s_brightness_slider.curvalue = ( 1.3f - vid_gamma.value + 0.5f ) * 10;

		s_fs_box.type = MTYPE_SPINCONTROL;
		s_fs_box.x	= 0;
		s_fs_box.y	= 40;
		s_fs_box.name	= "fullscreen";
		s_fs_box.itemnames = yesno_names;
		s_fs_box.curvalue = (int)vid_fullscreen.value;
		s_fs_box.callback = new TMCallback() {
			public void execute(Object o) {
				int fs = ((TMenuList)o).curvalue;
				if (fs == 0) {
					s_mode_list.itemnames = resolutions;
					int i = vid_modes.length - 2;
					while (i > 0 && vid_modes[i].width > mode_x) i--;
					s_mode_list.curvalue = i;
				} else {
					s_mode_list.itemnames = fs_resolutions;
					int i = fs_modes.length - 1;
					while (i > 0 && fs_modes[i].width > mode_x) i--;						
					s_mode_list.curvalue = i;						
				}
			}
		};

		s_tq_slider.type	= MTYPE_SLIDER;
		s_tq_slider.x		= 0;
		s_tq_slider.y		= 60;
		s_tq_slider.name	= "texture quality";
		s_tq_slider.minvalue = 0;
		s_tq_slider.maxvalue = 3;
		s_tq_slider.curvalue = 3 - gl_picmip.value;

		s_paletted_texture_box.type = MTYPE_SPINCONTROL;
		s_paletted_texture_box.x	= 0;
		s_paletted_texture_box.y	= 70;
		s_paletted_texture_box.name	= "8-bit textures";
		s_paletted_texture_box.itemnames = yesno_names;
		s_paletted_texture_box.curvalue = (int)gl_ext_palettedtexture.value;


		s_vsync_box.type = MTYPE_SPINCONTROL;
		s_vsync_box.x	= 0;
		s_vsync_box.y	= 80;
		s_vsync_box.name	= "sync every frame";
		s_vsync_box.itemnames = yesno_names;
		s_vsync_box.curvalue = (int) gl_swapinterval.value;

		s_defaults_action.type = MTYPE_ACTION;
		s_defaults_action.name = "reset to default";
		s_defaults_action.x    = 0;
		s_defaults_action.y    = 100;
		s_defaults_action.callback = new TMCallback() {
			public void execute(Object self) {
				ResetDefaults(self);
			}
		};

		s_apply_action.type = MTYPE_ACTION;
		s_apply_action.name = "apply";
		s_apply_action.x    = 0;
		s_apply_action.y    = 110;
		s_apply_action.callback = new TMCallback() {
			public void execute(Object self) {
				ApplyChanges(self);
			}
		};

		Menu.Menu_AddItem( s_opengl_menu, s_ref_list );
		Menu.Menu_AddItem( s_opengl_menu, s_mode_list );
		Menu.Menu_AddItem( s_opengl_menu, s_screensize_slider );
		Menu.Menu_AddItem( s_opengl_menu, s_brightness_slider );
		Menu.Menu_AddItem( s_opengl_menu, s_fs_box );

		Menu.Menu_AddItem( s_opengl_menu, s_tq_slider );
		Menu.Menu_AddItem( s_opengl_menu, s_paletted_texture_box );
		Menu.Menu_AddItem( s_opengl_menu, s_vsync_box );

		Menu.Menu_AddItem( s_opengl_menu, s_defaults_action );
		Menu.Menu_AddItem( s_opengl_menu, s_apply_action );

		Menu.Menu_Center( s_opengl_menu );
		s_opengl_menu.x -= 8;
	}

	/*
	================
	VID_MenuDraw
	================
	*/
	public static void MenuDraw()
	{
		s_current_menu = s_opengl_menu;

		/*
		** draw the banner
		*/
		Dimension dim = new Dimension();
		re.DrawGetPicSize( dim, "m_banner_video" );
		re.DrawPic( viddef.getWidth() / 2 - dim.width / 2, viddef.getHeight() /2 - 110, "m_banner_video" );

		/*
		** move cursor to a reasonable starting position
		*/
		Menu.Menu_AdjustCursor( s_current_menu, 1 );

		/*
		** draw the menu
		*/
		Menu.Menu_Draw( s_current_menu );
	}

	/*
	================
	VID_MenuKey
	================
	*/
	public static String MenuKey( int key )
	{
		TMenuFramework m = s_current_menu;
		final String sound = "misc/menu1.wav";

		switch ( key )
		{
		case K_ESCAPE:
			Menu.popMenu();
			return null;
		case K_UPARROW:
			m.cursor--;
			Menu.Menu_AdjustCursor( m, -1 );
			break;
		case K_DOWNARROW:
			m.cursor++;
			Menu.Menu_AdjustCursor( m, 1 );
			break;
		case K_LEFTARROW:
			Menu.Menu_SlideItem( m, -1 );
			break;
		case K_RIGHTARROW:
			Menu.Menu_SlideItem( m, 1 );
			break;
		case K_ENTER:
			Menu.Menu_SelectItem( m );
			break;
		}

		return sound;
	}

}
