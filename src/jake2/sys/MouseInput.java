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

import jake2.client.Client;
import jake2.client.Context;
import jake2.client.Key;
import jake2.game.Cmd;
import jake2.game.TVar;
import jake2.game.TUserCmd;
import jake2.qcommon.ConsoleVar;
import jake2.render.opengl.LwjglRenderer;
import jake2.util.Math3D;

import static jake2.Defines.*;
import static jake2.client.Context.cl;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPos;

/**
 * MouseInput
 */
public final class MouseInput {

    // motion values
    private static int mx = 0;
    private static int my = 0;
    private static TVar m_filter;
    private static boolean mouse_avail = true;

    static boolean mouse_active = false;

    private static boolean ignorefirst = false;

    private static int mouse_buttonstate;

    private static int mouse_oldbuttonstate;

    private static int old_mouse_x;

    private static int old_mouse_y;

    private static boolean mlooking;

    public static void ActivateMouse() {
        if (!mouse_avail)
            return;
        if (!mouse_active) {
            mx = my = 0; // don't spazz
            install_grabs();
            mouse_active = true;
        }
    }

    public static void DeactivateMouse() {
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

    public static void Shutdown() {
        mouse_avail = false;
    }

    public static void Real_IN_Init() {
        // mouse variables
        m_filter = ConsoleVar.get("m_filter", "0", 0);
        Context.in_mouse = ConsoleVar.get("in_mouse", "1", TVar.CVAR_FLAG_ARCHIVE);
        Context.freelook = ConsoleVar.get("freelook", "1", 0);
        Context.lookstrafe = ConsoleVar.get("lookstrafe", "0", 0);
        Context.sensitivity = ConsoleVar.get("sensitivity", "3", 0);
        Context.m_pitch = ConsoleVar.get("m_pitch", "0.022", 0);
        Context.m_yaw = ConsoleVar.get("m_yaw", "0.022", 0);
        Context.m_forward = ConsoleVar.get("m_forward", "1", 0);
        Context.m_side = ConsoleVar.get("m_side", "0.8", 0);

        Cmd.registerCommand("+mlook", () -> mlooking = true);
        Cmd.registerCommand("-mlook", () -> {
            mlooking = false;
            CenterView();
        });
        Cmd.registerCommand("force_centerview", () -> cl.viewangles[PITCH] = 0);
        Cmd.registerCommand("togglemouse", () -> {
            if (mouse_avail) {
                mouse_avail = false;
                DeactivateMouse();
            } else {
                mouse_avail = true;
                ActivateMouse();
            }
        });

        MouseInput.mouse_avail = true;
    }

    public static void Commands() {

		if (!MouseInput.mouse_avail) {
            return;
        }
	
		KeyboardInput keyboardInput = Context.re.getKeyboardHandler();
		for (int i=0 ; i<3 ; i++) {
			if ( (MouseInput.mouse_buttonstate & (1<<i)) != 0 && (MouseInput.mouse_oldbuttonstate & (1<<i)) == 0 )
				keyboardInput.Do_Key_Event(Key.K_MOUSE1 + i, true);
	
			if ( (MouseInput.mouse_buttonstate & (1<<i)) == 0 && (MouseInput.mouse_oldbuttonstate & (1<<i)) != 0 )
				keyboardInput.Do_Key_Event(Key.K_MOUSE1 + i, false);
		}
		MouseInput.mouse_oldbuttonstate = MouseInput.mouse_buttonstate;
    }

    public static void CenterView() {
        cl.viewangles[PITCH] = -Math3D.SHORT2ANGLE(cl.frame.playerstate.pmove.delta_angles[PITCH]);
    }

    public static void Move(TUserCmd cmd) {
        if (!MouseInput.mouse_avail)
            return;

        if (m_filter.value != 0.0f) {
            mx = (mx + MouseInput.old_mouse_x) / 2;
            my = (my + MouseInput.old_mouse_y) / 2;
        }

        MouseInput.old_mouse_x = mx;
        MouseInput.old_mouse_y = my;

        mx = (int) (mx * Context.sensitivity.value);
        my = (int) (my * Context.sensitivity.value);

        // add mouse X/Y movement to cmd
        if ((Client.clientInput.in_strafe.state & 1) != 0
                || ((Context.lookstrafe.value != 0) && MouseInput.mlooking)) {
            cmd.sidemove += Context.m_side.value * mx;
        } else {
            cl.viewangles[YAW] -= Context.m_yaw.value * mx;
        }

        if ((MouseInput.mlooking || Context.freelook.value != 0.0f)
                && (Client.clientInput.in_strafe.state & 1) == 0) {
            cl.viewangles[PITCH] += Context.m_pitch.value * my;
        } else {
            cmd.forwardmove -= Context.m_forward.value * my;
        }
        mx = my = 0;
    }

    static void update() {
        double[] l_mx = new double[1];
        double[] l_my = new double[1];

        glfwGetCursorPos(LwjglRenderer.Companion.getWindow(), l_mx, l_my);

        if (mouse_active) {
            mx = (int) ((l_mx[0] - 640 / 2) * 2);
            my = (int) ((l_my[0] - 480 / 2) * 2);
        } else {
            mx = 0;
            my = 0;
        }

        glfwSetCursorPos(LwjglRenderer.Companion.getWindow(), 320, 240);
    }
}