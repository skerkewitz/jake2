/*
 * IN.java
 * Copyright (C) 2003
 * 
 * $Id: IN.java,v 1.8 2006-12-12 15:20:30 cawe Exp $
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
package jake2.sys;

import jake2.client.CL_input;
import jake2.client.Context;
import jake2.client.Key;
import jake2.game.Cmd;
import jake2.game.TVar;
import jake2.game.usercmd_t;
import jake2.qcommon.ConsoleVar;
import jake2.util.Math3D;

import static jake2.Defines.*;
import static jake2.client.Context.cl;
import static jake2.client.Context.cls;

/**
 * Input
 */
public final class Input {

    static boolean mouse_avail = true;

    static boolean mouse_active = false;

    static boolean ignorefirst = false;

    static int mouse_buttonstate;

    static int mouse_oldbuttonstate;

    static int old_mouse_x;

    static int old_mouse_y;

    static boolean mlooking;

    private static void ActivateMouse() {
        if (!mouse_avail)
            return;
        if (!mouse_active) {
            Keyboard.mx = Keyboard.my = 0; // don't spazz
            install_grabs();
            mouse_active = true;
        }
    }

    private static void DeactivateMouse() {
        // if (!mouse_avail || c == null) return;
        if (mouse_active) {
            uninstall_grabs();
            mouse_active = false;
        }
    }

    private static void install_grabs() {
		Context.re.getKeyboardHandler().installGrabs();
		ignorefirst = true;
    }

    private static void uninstall_grabs() {
		Context.re.getKeyboardHandler().uninstallGrabs();
    }

    public static void toggleMouse() {
        if (mouse_avail) {
            mouse_avail = false;
            DeactivateMouse();
        } else {
            mouse_avail = true;
            ActivateMouse();
        }
    }

    public static void Init() {
        Context.in_mouse = ConsoleVar.Get("in_mouse", "1", TVar.CVAR_FLAG_ARCHIVE);
        Context.in_joystick = ConsoleVar.Get("in_joystick", "0", TVar.CVAR_FLAG_ARCHIVE);
    }

    public static void Shutdown() {
        mouse_avail = false;
    }

    public static void Real_IN_Init() {
        // mouse variables
        Context.m_filter = ConsoleVar.Get("m_filter", "0", 0);
        Context.in_mouse = ConsoleVar.Get("in_mouse", "1", TVar.CVAR_FLAG_ARCHIVE);
        Context.freelook = ConsoleVar.Get("freelook", "1", 0);
        Context.lookstrafe = ConsoleVar.Get("lookstrafe", "0", 0);
        Context.sensitivity = ConsoleVar.Get("sensitivity", "3", 0);
        Context.m_pitch = ConsoleVar.Get("m_pitch", "0.022", 0);
        Context.m_yaw = ConsoleVar.Get("m_yaw", "0.022", 0);
        Context.m_forward = ConsoleVar.Get("m_forward", "1", 0);
        Context.m_side = ConsoleVar.Get("m_side", "0.8", 0);

        Cmd.AddCommand("+mlook", Input::MLookDown);
        Cmd.AddCommand("-mlook", Input::MLookUp);
        Cmd.AddCommand("force_centerview", Input::Force_CenterView_f);
        Cmd.AddCommand("togglemouse", Input::toggleMouse);

        Input.mouse_avail = true;
    }

    public static void Commands() {

		if (!Input.mouse_avail) {
            return;
        }
	
		Keyboard keyboard = Context.re.getKeyboardHandler();
		for (int i=0 ; i<3 ; i++) {
			if ( (Input.mouse_buttonstate & (1<<i)) != 0 && (Input.mouse_oldbuttonstate & (1<<i)) == 0 )
				keyboard.Do_Key_Event(Key.K_MOUSE1 + i, true);
	
			if ( (Input.mouse_buttonstate & (1<<i)) == 0 && (Input.mouse_oldbuttonstate & (1<<i)) != 0 )
				keyboard.Do_Key_Event(Key.K_MOUSE1 + i, false);
		}
		Input.mouse_oldbuttonstate = Input.mouse_buttonstate;
    }

    public static void Frame() {

        if (!cl.cinematicpalette_active
                && (!cl.refresh_prepped || cls.getKey_dest() == key_console || cls.getKey_dest() == key_menu)) {
            DeactivateMouse();
        } else {
            ActivateMouse();
        }
    }

    public static void CenterView() {
        cl.viewangles[PITCH] = -Math3D.SHORT2ANGLE(cl.frame.playerstate.pmove.delta_angles[PITCH]);
    }

    public static void Move(usercmd_t cmd) {
        if (!Input.mouse_avail)
            return;

        if (Context.m_filter.value != 0.0f) {
            Keyboard.mx = (Keyboard.mx + Input.old_mouse_x) / 2;
            Keyboard.my = (Keyboard.my + Input.old_mouse_y) / 2;
        }

        Input.old_mouse_x = Keyboard.mx;
        Input.old_mouse_y = Keyboard.my;

        Keyboard.mx = (int) (Keyboard.mx * Context.sensitivity.value);
        Keyboard.my = (int) (Keyboard.my * Context.sensitivity.value);

        // add mouse X/Y movement to cmd
        if ((CL_input.in_strafe.state & 1) != 0
                || ((Context.lookstrafe.value != 0) && Input.mlooking)) {
            cmd.sidemove += Context.m_side.value * Keyboard.mx;
        } else {
            cl.viewangles[YAW] -= Context.m_yaw.value * Keyboard.mx;
        }

        if ((Input.mlooking || Context.freelook.value != 0.0f)
                && (CL_input.in_strafe.state & 1) == 0) {
            cl.viewangles[PITCH] += Context.m_pitch.value * Keyboard.my;
        } else {
            cmd.forwardmove -= Context.m_forward.value * Keyboard.my;
        }
        Keyboard.mx = Keyboard.my = 0;
    }

    private static void MLookDown() {
        mlooking = true;
    }

    private static void MLookUp() {
        mlooking = false;
        CenterView();
    }

    private static void Force_CenterView_f() {
        cl.viewangles[PITCH] = 0;
    }

}