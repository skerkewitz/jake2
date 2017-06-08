/*
 * CL_view.java Copyright (C) 2004
 * 
 * $Id: CL_view.java,v 1.5 2008-03-02 14:56:22 cawe Exp $
 */
/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */
package jake2.client;

import jake2.Defines;
import jake2.qcommon.CM;
import jake2.qcommon.Command;

import java.util.StringTokenizer;

public class ClientView {

    static int num_cl_weaponmodels;

    static String[] cl_weaponmodels = new String[Defines.MAX_CLIENTWEAPONMODELS];

    /*
     * =================
     * 
     * CL_PrepRefresh
     * 
     * Call before entering a new level, or after changing dlls
     * =================
     */
    static void PrepRefresh() {
        String mapname;
        int i;
        String name;
        float rotate;
        float[] axis = new float[3];

        if ((i = Context.cl.configstrings[Defines.CS_MODELS + 1].length()) == 0)
            return; // no map loaded

        SCR.AddDirtyPoint(0, 0);
        SCR.AddDirtyPoint(Context.viddef.getWidth() - 1, Context.viddef.getHeight() - 1);

        // let the render dll load the map
        mapname = Context.cl.configstrings[Defines.CS_MODELS + 1].substring(5,
                i - 4); // skip "maps/"
        // cut off ".bsp"

        // register models, pics, and skins
        Command.Printf("Map: " + mapname + "\r");
        SCR.UpdateScreen();
        Context.re.BeginRegistration(mapname);
        Command.Printf("                                     \r");

        // precache status bar pics
        Command.Printf("pics\r");
        SCR.UpdateScreen();
        SCR.TouchPics();
        Command.Printf("                                     \r");

        CL_tent.RegisterTEntModels();

        num_cl_weaponmodels = 1;
        cl_weaponmodels[0] = "weapon.md2";

        for (i = 1; i < Defines.MAX_MODELS
                && Context.cl.configstrings[Defines.CS_MODELS + i].length() != 0; i++) {
            name = new String(Context.cl.configstrings[Defines.CS_MODELS + i]);
            if (name.length() > 37)
                name = name.substring(0, 36);

            if (name.charAt(0) != '*')
                Command.Printf(name + "\r");

            SCR.UpdateScreen();
            Key.SendKeyEvents(); // pump message loop
            if (name.charAt(0) == '#') {
                // special player weapon model
                if (num_cl_weaponmodels < Defines.MAX_CLIENTWEAPONMODELS) {
                    cl_weaponmodels[num_cl_weaponmodels] = Context.cl.configstrings[Defines.CS_MODELS
                            + i].substring(1);
                    num_cl_weaponmodels++;
                }
            } else {
                Context.cl.model_draw[i] = Context.re
                        .RegisterModel(Context.cl.configstrings[Defines.CS_MODELS
                                + i]);
                if (name.charAt(0) == '*')
                    Context.cl.model_clip[i] = CM
                            .InlineModel(Context.cl.configstrings[Defines.CS_MODELS
                                    + i]);
                else
                    Context.cl.model_clip[i] = null;
            }
            if (name.charAt(0) != '*')
                Command.Printf("                                     \r");
        }

        Command.Printf("images\r");
        SCR.UpdateScreen();
        for (i = 1; i < Defines.MAX_IMAGES
                && Context.cl.configstrings[Defines.CS_IMAGES + i].length() > 0; i++) {
            Context.cl.image_precache[i] = Context.re
                    .RegisterPic(Context.cl.configstrings[Defines.CS_IMAGES + i]);
            Key.SendKeyEvents(); // pump message loop
        }

        Command.Printf("                                     \r");
        for (i = 0; i < Defines.MAX_CLIENTS; i++) {
            if (Context.cl.configstrings[Defines.CS_PLAYERSKINS + i].length() == 0)
                continue;
            Command.Printf("client " + i + '\r');
            SCR.UpdateScreen();
            Key.SendKeyEvents(); // pump message loop
            CL_parse.ParseClientinfo(i);
            Command.Printf("                                     \r");
        }

        CL_parse.LoadClientinfo(Context.cl.baseclientinfo,
                "unnamed\\male/grunt");

        // set sky textures and speed
        Command.Printf("sky\r");
        SCR.UpdateScreen();
        rotate = Float
                .parseFloat(Context.cl.configstrings[Defines.CS_SKYROTATE]);
        StringTokenizer st = new StringTokenizer(
                Context.cl.configstrings[Defines.CS_SKYAXIS]);
        axis[0] = Float.parseFloat(st.nextToken());
        axis[1] = Float.parseFloat(st.nextToken());
        axis[2] = Float.parseFloat(st.nextToken());
        Context.re.SetSky(Context.cl.configstrings[Defines.CS_SKY], rotate,
                axis);
        Command.Printf("                                     \r");

        // the renderer can now free unneeded stuff
        Context.re.EndRegistration();

        // clear any lines of console text
        Console.ClearNotify();

        SCR.UpdateScreen();
        Context.cl.refresh_prepped = true;
        Context.cl.force_refdef = true; // make sure we have a valid refdef
    }

    public static void AddNetgraph() {
        int i;
        int in;
        int ping;

        // if using the debuggraph for something else, don't
        // add the net lines
        if (SCR.scr_debuggraph.value == 0.0f || SCR.scr_timegraph.value == 0.0f)
            return;

        for (i = 0; i < Context.cls.getNetchan().dropped; i++)
            SCR.DebugGraph(30, 0x40);

        for (i = 0; i < Context.cl.surpressCount; i++)
            SCR.DebugGraph(30, 0xdf);

        // see what the latency was on this packet
        in = Context.cls.getNetchan().incoming_acknowledged
                & (Defines.CMD_BACKUP - 1);
        ping = Context.cls.getRealtime() - Context.cl.cmd_time[in];
        ping /= 30;
        if (ping > 30)
            ping = 30;
        SCR.DebugGraph(ping, 0xd0);
    }
}