/*
 * Qcommon.java
 * Copyright 2003
 * 
 * $Id: Qcommon.java,v 1.23 2006-08-20 21:46:07 salomo Exp $
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
package jake2.qcommon

import jake2.client.Context
import jake2.client.*
import jake2.client.Context.fileSystem
import jake2.game.Cmd
import jake2.game.TVar
import jake2.io.FileSystem
import jake2.network.Netchan
import jake2.render.opengl.LwjglRenderer
import jake2.server.ServerMain
import jake2.sys.*
import jake2.sys.Network
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.opengl.GL

import java.io.FileWriter
import java.io.IOException

/**
 * Engine contains some  basic routines for the game engine
 * namely initialization, shutdown and frame generation.
 */
class Engine {
    companion object {

        val BUILDSTRING = "Java " + System.getProperty("java.version")
        val CPUSTRING = System.getProperty("os.arch")

        var commandLineOptions: CommandLineOptions? = null

        /**
         * This function initializes the different subsystems of
         * the game engine. The setjmp/longjmp mechanism of the original
         * was replaced with exceptions.
         * @param commandLineOptions the original unmodified command line arguments
         */
        @JvmStatic fun init(commandLineOptions: CommandLineOptions) {
            try {

                // prepare enough of the subsystems to handle
                // cvar and command buffer management
                this.commandLineOptions = commandLineOptions

                CommandBuffer.Init()

                Cmd.Init()
                ConsoleVar.Init()

                Key.Init()

                // we need to add the early commands twice, because
                // a basedir or cddir needs to be assign before execing
                // config files, but we want other parms to override
                // the settings of the config files
                CommandBuffer.AddEarlyCommands(false)
                CommandBuffer.execute()

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.setStatus("initializing filesystem...");

                fileSystem = FileSystem()

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.setStatus("loading config...");

                reconfigure(false)

                fileSystem.setCDDir() // use cddir from config.cfg
                fileSystem.markBaseSearchPaths() // mark the default search paths

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.testQ2Data(); // test for valid baseq2

                reconfigure(true) // reload default.cfg and config.cfg

                //
                // init commands and vars
                //
                Cmd.registerCommand("error", Command.Error_f)

                Context.host_speeds = ConsoleVar.get("host_speeds", "0", 0)
                Context.log_stats = ConsoleVar.get("log_stats", "0", 0)
                Context.developer = ConsoleVar.get("developer", "0", TVar.CVAR_FLAG_ARCHIVE)
                Context.timescale = ConsoleVar.get("timescale", "0", 0)
                Context.fixedtime = ConsoleVar.get("fixedtime", "0", 0)
                Context.logfile_active = ConsoleVar.get("logfile", "0", 0)
                Context.showtrace = ConsoleVar.get("showtrace", "0", 0)
                Context.dedicated = ConsoleVar.get("dedicated", "0", TVar.CVAR_FLAG_NOSET)
                val s = Command.sprintf("%4.2f %s %s %s", Context.VERSION, CPUSTRING, Context.__DATE__, BUILDSTRING)

                ConsoleVar.get("version", s, TVar.CVAR_FLAG_SERVERINFO or TVar.CVAR_FLAG_NOSET)

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.setStatus("initializing network subsystem...");

                Network.Init()    //ok
                Netchan.Netchan_Init()    //ok

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.setStatus("initializing server subsystem...");
                ServerMain.SV_Init()    //ok

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.setStatus("initializing client subsystem...");

                val home = System.getProperty("user.home")
                val sep = System.getProperty("file.separator")

                val dir = home + sep + "Jake2" + sep + "baseq2"
                ConsoleVar.Set("cddir", dir)
                fileSystem.setCDDir()

                Client.Init()

                // add + commands from command line
                if (!CommandBuffer.AddLateCommands()) {
                    // if the user didn't give any commands, run default action
                    if (Context.dedicated.value == 0f)
                        CommandBuffer.AddText("d1\n")
                    else
                        CommandBuffer.AddText("dedicated_start\n")

                    CommandBuffer.execute()
                } else {
                    // the user asked for something explicit
                    // so drop the loading plaque
                    SCR.EndLoadingPlaque()
                }

                Command.Printf("====== Quake2 Initialized ======\n\n")

                // save config when configuration is completed
                Client.WriteConfiguration()

                //			if (Context.dedicated.value != 1.0f)
                //				Jake2.Q2Dialog.dispose();

            } catch (e: QuakeException) {
                Engine.Error("Error during initialization")
            }

        }

        /**
         * Trigger generation of a frame for the given time. The setjmp/longjmp
         * mechanism of the original was replaced with exceptions.
         * @param msec the current game time
         */
        fun Frame(msec: Int) {

            GL.createCapabilities()

            var msec = msec
            try {

                if (Context.log_stats.modified) {
                    Context.log_stats.modified = false

                    if (Context.log_stats.value != 0.0f) {

                        if (Context.log_stats_file != null) {
                            try {
                                Context.log_stats_file.close()
                            } catch (e: IOException) {
                            }

                            Context.log_stats_file = null
                        }

                        try {
                            Context.log_stats_file = FileWriter("stats.log")
                        } catch (e: IOException) {
                            Context.log_stats_file = null
                        }

                        if (Context.log_stats_file != null) {
                            try {
                                Context.log_stats_file.write("entities,dlights,parts,frame time\n")
                            } catch (e: IOException) {
                            }

                        }

                    } else {

                        if (Context.log_stats_file != null) {
                            try {
                                Context.log_stats_file.close()
                            } catch (e: IOException) {
                            }

                            Context.log_stats_file = null
                        }
                    }
                }

                if (Context.fixedtime.value != 0.0f) {
                    msec = Context.fixedtime.value.toInt()
                } else if (Context.timescale.value != 0.0f) {
                    msec *= Context.timescale.value.toInt()
                    if (msec < 1)
                        msec = 1
                }

                if (Context.showtrace.value != 0.0f) {
                    Command.Printf("%4i traces  %4i points\n", 2, Context.c_traces, Context.c_pointcontents)


                    Context.c_traces = 0
                    Context.c_brush_traces = 0
                    Context.c_pointcontents = 0
                }

                CommandBuffer.execute()

                var time_before = 0
                var time_between = 0
                var time_after = 0

                if (Context.host_speeds.value != 0.0f)
                    time_before = Timer.Milliseconds()

                Command.debugContext = "Server:"
                ServerMain.SV_Frame(msec.toLong())

                if (Context.host_speeds.value != 0.0f)
                    time_between = Timer.Milliseconds()

                Command.debugContext = "Client:"
                Client.Frame(msec)

                if (Context.host_speeds.value != 0.0f) {
                    time_after = Timer.Milliseconds()

                    val all = time_after - time_before
                    var sv = time_between - time_before
                    var cl = time_after - time_between
                    val gm = Context.time_after_game - Context.time_before_game
                    val rf = Context.time_after_ref - Context.time_before_ref
                    sv -= gm
                    cl -= rf

                    Command.Printf("all:%3i sv:%3i gm:%3i cl:%3i rf:%3i\n", 5, all, sv, gm, cl, rf)
                }

            } catch (e: QuakeException) {
                Command.DPrintf("longjmp exception:" + e)
            }

        }

        private fun reconfigure(clear: Boolean) {
            val dir = ConsoleVar.get("cddir", "", TVar.CVAR_FLAG_ARCHIVE)!!.string
            CommandBuffer.AddText("exec default.cfg\n")
            CommandBuffer.AddText("bind MWHEELUP weapnext\n")
            CommandBuffer.AddText("bind MWHEELDOWN weapprev\n")
            CommandBuffer.AddText("bind w +forward\n")
            CommandBuffer.AddText("bind s +back\n")
            CommandBuffer.AddText("bind a +moveleft\n")
            CommandBuffer.AddText("bind d +moveright\n")
            CommandBuffer.execute()
            ConsoleVar.Set("vid_fullscreen", "0")
            CommandBuffer.AddText("exec config.cfg\n")

            CommandBuffer.AddEarlyCommands(clear)
            CommandBuffer.execute()
            if ("" != dir) ConsoleVar.Set("cddir", dir)
        }

        @JvmStatic fun Error(error: String) {
            Client.Shutdown()
            //StackTrace();
            Exception(error).printStackTrace()
            java.lang.System.exit(1)
        }

        @JvmStatic fun Quit() {
            Client.Shutdown()

            java.lang.System.exit(0)
        }

        fun pumpEvents() {
            GLFW.glfwPollEvents()
        }

        fun shouldClose(): Boolean {
            return glfwWindowShouldClose(LwjglRenderer.window)
        }
    }
}
