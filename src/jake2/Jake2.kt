/*
 * Jake2.java
 * Copyright (C)  2003
 * 
 * $Id: Jake2.java,v 1.9 2005-12-03 19:43:15 salomo Exp $
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
package jake2

import jake2.qcommon.Com
import jake2.qcommon.Cvar
import jake2.qcommon.Qcommon
import jake2.render.opengl.LwjglDriver
import jake2.sys.Timer
import org.lwjgl.opengl.GL

import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose


/**
 * Jake2 is the main class of Quake2 for Java.
 */
object Jake2 {

    //public static Q2DataDialog Q2Dialog;

    /**
     * main is used to start the game. Quake2 for Java supports the following
     * command line arguments:

     * @param args
     */
    @JvmStatic fun main(args: Array<String>) {

        var dedicated = false

        //    	Configuration.DEBUG.set(true);

        // check if we are in dedicated mode to hide the java dialog.
        var n = 0
        while (n < args.size) {
            if (args[n] == "+set") {
                if (n++ >= args.size)
                    break

                if (args[n] != "dedicated") {
                    n++
                    continue
                }

                if (n++ >= args.size)
                    break

                if (args[n] == "1" || args[n] == "\"1\"") {
                    Com.Printf("Starting in dedicated mode.\n")
                    dedicated = true
                }
            }
            n++
        }

        // TODO: check if dedicated is set in config file

        Globals.dedicated = Cvar.Get("dedicated", "0", Globals.CVAR_NOSET)

        if (dedicated)
            Globals.dedicated.value = 1.0f


        //    	// open the q2dialog, if we are not in dedicated mode.
        //    	if (Globals.dedicated.value != 1.0f)
        //    	{
        //    		Q2Dialog = new Q2DataDialog();
        //    		Locale.setDefault(Locale.US);
        //    		Q2Dialog.setVisible(true);
        //    	}

        // in C the first arg is the filename
        val argc = if (args == null) 1 else args.size + 1
        val c_args = arrayOfNulls<String>(argc)
        c_args[0] = "Jake2"
        if (argc > 1) {
            System.arraycopy(args, 0, c_args, 1, argc - 1)
        }
        Qcommon.Init(c_args)

        Globals.nostdout = Cvar.Get("nostdout", "0", 0)

        var oldtime = Timer.Milliseconds()
        var newtime: Int
        var time: Int
        while (!glfwWindowShouldClose(LwjglDriver.window)) {

            GL.createCapabilities()

            // find time spending rendering last frame
            newtime = Timer.Milliseconds()
            time = newtime - oldtime

            if (time > 0) {
                Qcommon.Frame(time)
            }

            oldtime = newtime
            glfwPollEvents()
        }
    }
}