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

// Created on 14.01.2004 by RST.
// $Id: ServerInit.java,v 1.17 2006-01-20 22:44:07 salomo Exp $
package jake2.server;

import jake2.Defines;
import jake2.client.CL;
import jake2.client.Context;
import jake2.client.SCR;
import jake2.game.*;
import jake2.io.FileSystem;
import jake2.qcommon.*;
import jake2.sys.NET;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.io.IOException;
import java.io.RandomAccessFile;

public class ServerInit {

    public static TServerStatic svs = new TServerStatic(); // persistant
    public static TServer sv = new TServer(); // local server
    private static String firstmap = "";

    private static int findIndexOf(String name, int start, int max, boolean create) {

        if (name == null || name.length() == 0) {
            return 0;
        }

        int i;
        for (i = 1; i < max && sv.configstrings[start + i] != null; i++) {
            if (0 == Lib.strcmp(sv.configstrings[start + i], name)) {
                return i;
            }
        }

        if (!create) {
            return 0;
        }

        if (i == max) {
            Command.Error(Defines.ERR_DROP, "*Index: overflow");
        }

        sv.configstrings[start + i] = name;

        if (sv.state != Defines.ss_loading) {
            // send the update to everyone
            sv.multicast.clear();
            sv.multicast.writeChar(Defines.svc_configstring);
            sv.multicast.writeShort(start + i);
            sv.multicast.writeString(name);
            SV_SEND.SV_Multicast(Context.vec3_origin, Defines.MULTICAST_ALL_R);
        }

        return i;
    }

    public static int modelIndexOf(String name) {
        return findIndexOf(name, Defines.CS_MODELS, Defines.MAX_MODELS, true);
    }

    public static int soundIndexOf(String name) {
        return findIndexOf(name, Defines.CS_SOUNDS, Defines.MAX_SOUNDS, true);
    }

    public static int imageIndexOf(String name) {
        return findIndexOf(name, Defines.CS_IMAGES, Defines.MAX_IMAGES, true);
    }

    /**
     * createBaseline
     *
     * Entity baselines are used to compress the update messages to the clients --
     * only the fields that differ from the baseline will be transmitted.
     */
    private static void createBaseline() {

        for (int entnum = 1; entnum < GameBase.num_edicts; entnum++) {
            TEntityDict svent = GameBase.g_edicts[entnum];

            if (!svent.inuse)
                continue;
            if (0 == svent.s.modelindex && 0 == svent.s.sound
                    && 0 == svent.s.effects)
                continue;

            svent.s.number = entnum;

            // take current state as baseline
            Math3D.VectorCopy(svent.s.origin, svent.s.old_origin);
            sv.baselines[entnum].set(svent.s);
        }
    }

    /**
     * checkForSavegame.
     */
    private static void checkForSavegame() {

        if (ServerMain.sv_noreload.value != 0)
            return;

        if (ConsoleVar.VariableValue("deathmatch") != 0)
            return;

        String name = FileSystem.Gamedir() + "/save/current/" + sv.name + ".sav";

        RandomAccessFile f;
        try {
            f = new RandomAccessFile(name, "r");
        }
        catch (Exception e) {
            return;
        }

        try {
            f.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        ServerWorld.SV_ClearWorld();

        // get configstrings and areaportals
        SV_CCMDS.SV_ReadLevelFile();

        if (!sv.loadgame) {
            // coming back to a level after being in a different
            // level, so run it for ten seconds

            // rlava2 was sending too many lightstyles, and overflowing the
            // reliable data. temporarily changing the server state to loading
            // prevents these from being passed down.
            int previousState; // PGM

            previousState = sv.state; // PGM
            sv.state = Defines.ss_loading; // PGM
            for (int i = 0; i < 100; i++)
                GameBase.G_RunFrame();

            sv.state = previousState; // PGM
        }
    }
    
    /**
     * spawnServer.
     *
     * Change the server to a new map, taking all connected clients along with
     * it.
     */
    private static void spawnServer(String server, String spawnpoint,
                                    int serverstate, boolean attractloop, boolean loadgame) {
        int i;
        int checksum = 0;

        if (attractloop)
            ConsoleVar.Set("paused", "0");

        Command.Printf("------- Server Initialization -------\n");

        Command.DPrintf("SpawnServer: " + server + "\n");
        if (sv.demofile != null)
            try {
                sv.demofile.close();
            }
        	catch (Exception e) {
            }

        // any partially connected client will be restarted
        svs.spawncount++;

        sv.state = Defines.ss_dead;

        Context.server_state = sv.state;

        // wipe the entire per-level structure
        sv = new TServer();

        svs.realtime = 0;
        sv.loadgame = loadgame;
        sv.attractloop = attractloop;

        // save name for levels that don't set message
        sv.configstrings[Defines.CS_NAME] = server;

        if (ConsoleVar.VariableValue("deathmatch") != 0) {
            sv.configstrings[Defines.CS_AIRACCEL] = ""
                    + ServerMain.sv_airaccelerate.value;
            PMove.pm_airaccelerate = ServerMain.sv_airaccelerate.value;
        } else {
            sv.configstrings[Defines.CS_AIRACCEL] = "0";
            PMove.pm_airaccelerate = 0;
        }

        sv.multicast.init(sv.multicast_buf, sv.multicast_buf.length);

        sv.name = server;

        // leave slots at start for clients only
        for (i = 0; i < ServerMain.maxclients.value; i++) {
            // needs to reconnect
            if (svs.clients[i].state > Defines.cs_connected)
                svs.clients[i].state = Defines.cs_connected;
            svs.clients[i].lastframe = -1;
        }

        sv.time = 1000;

        sv.name = server;
        sv.configstrings[Defines.CS_NAME] = server;

        int iw[] = { checksum };

        if (serverstate != Defines.ss_game) {
            sv.models[1] = CM.CM_LoadMap("", false, iw); // no real map
        } else {
            sv.configstrings[Defines.CS_MODELS + 1] = "maps/" + server + ".bsp";
            sv.models[1] = CM.CM_LoadMap(
                    sv.configstrings[Defines.CS_MODELS + 1], false, iw);
        }
        checksum = iw[0];
        sv.configstrings[Defines.CS_MAPCHECKSUM] = "" + checksum;


        // clear physics interaction links

        ServerWorld.SV_ClearWorld();

        for (i = 1; i < CM.CM_NumInlineModels(); i++) {
            sv.configstrings[Defines.CS_MODELS + 1 + i] = "*" + i;

            // copy references
            sv.models[i + 1] = CM.InlineModel(sv.configstrings[Defines.CS_MODELS + 1 + i]);
        }


        // spawn the rest of the entities on the map

        // precache and static commands can be issued during
        // map initialization

        sv.state = Defines.ss_loading;
        Context.server_state = sv.state;

        // load and spawn all other entities
        GameSpawn.SpawnEntities(sv.name, CM.CM_EntityString(), spawnpoint);

        // run two frames to allow everything to settle
        GameBase.G_RunFrame();
        GameBase.G_RunFrame();

        // all precaches are complete
        sv.state = serverstate;
        Context.server_state = sv.state;

        // create a baseline for more efficient communications
        createBaseline();

        // check for a savegame
        checkForSavegame();

        // set serverinfo variable
        ConsoleVar.FullSet("mapname", sv.name, TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_NOSET);
    }

    /**
     * initGame.
     *
     * A brand new game has been started.
     */
    public static void initGame() {
        int i;
        TEntityDict ent;
        //char idmaster[32];
        String idmaster;

        if (svs.initialized) {
            // cause any connected clients to reconnect
            ServerMain.SV_Shutdown("Server restarted\n", true);
        } else {
            // make sure the client is down
            CL.Drop();
            SCR.BeginLoadingPlaque();
        }

        // get any latched variable changes (maxclients, etc)
        ConsoleVar.GetLatchedVars();

        svs.initialized = true;

        if (ConsoleVar.VariableValue("coop") != 0
                && ConsoleVar.VariableValue("deathmatch") != 0) {
            Command.Printf("Deathmatch and Coop both set, disabling Coop\n");
            ConsoleVar.FullSet("coop", "0", TVar.CVAR_FLAG_SERVERINFO
                    | TVar.CVAR_FLAG_LATCH);
        }

        // dedicated servers are can't be single player and are usually DM
        // so unless they explicity set coop, force it to deathmatch
        if (Context.dedicated.value != 0) {
            if (0 == ConsoleVar.VariableValue("coop"))
                ConsoleVar.FullSet("deathmatch", "1", TVar.CVAR_FLAG_SERVERINFO
                        | TVar.CVAR_FLAG_LATCH);
        }

        // init clients
        if (ConsoleVar.VariableValue("deathmatch") != 0) {
            if (ServerMain.maxclients.value <= 1)
                ConsoleVar.FullSet("maxclients", "8", TVar.CVAR_FLAG_SERVERINFO
                        | TVar.CVAR_FLAG_LATCH);
            else if (ServerMain.maxclients.value > Defines.MAX_CLIENTS)
                ConsoleVar.FullSet("maxclients", "" + Defines.MAX_CLIENTS,
                        TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_LATCH);
        } else if (ConsoleVar.VariableValue("coop") != 0) {
            if (ServerMain.maxclients.value <= 1 || ServerMain.maxclients.value > 4)
                ConsoleVar.FullSet("maxclients", "4", TVar.CVAR_FLAG_SERVERINFO
                        | TVar.CVAR_FLAG_LATCH);

        } else // non-deathmatch, non-coop is one player
        {
            ConsoleVar.FullSet("maxclients", "1", TVar.CVAR_FLAG_SERVERINFO
                    | TVar.CVAR_FLAG_LATCH);
        }

        svs.spawncount = Lib.rand();
        svs.clients = new client_t[(int) ServerMain.maxclients.value];
        for (int n = 0; n < svs.clients.length; n++) {
            svs.clients[n] = new client_t();
            svs.clients[n].serverindex = n;
        }
        svs.num_client_entities = ((int) ServerMain.maxclients.value)
                * Defines.UPDATE_BACKUP * 64; //ok.

        svs.client_entities = new TEntityState[svs.num_client_entities];
        for (int n = 0; n < svs.client_entities.length; n++)
            svs.client_entities[n] = new TEntityState(null);

        // init network stuff
        NET.Config((ServerMain.maxclients.value > 1));

        // heartbeats will always be sent to the id master
        svs.last_heartbeat = -99999; // send immediately
        idmaster = "192.246.40.37:" + Defines.PORT_MASTER;
        NET.StringToAdr(idmaster, ServerMain.master_adr[0]);

        // init game
        SV_GAME.SV_InitGameProgs();

        for (i = 0; i < ServerMain.maxclients.value; i++) {
            ent = GameBase.g_edicts[i + 1];
            svs.clients[i].edict = ent;
            svs.clients[i].lastcmd = new usercmd_t();
        }
    }
                                                               // server info

    /**
     * SV_Map
     *
     * the full syntax is:
     *
     * map [*] <map>$ <startspot>+ <nextserver>
     *
     * command from the console or progs. Map can also be a.cin, .pcx, or .dm2 file.
     *
     * Nextserver is used to allow a cinematic to play, then proceed to
     * another level:
     *
     * map tram.cin+jail_e3
     */
    public static void SV_Map(boolean attractloop, String levelstring, boolean loadgame) {

        int l;
        String level, ch, spawnpoint;

        sv.loadgame = loadgame;
        sv.attractloop = attractloop;

        if (sv.state == Defines.ss_dead && !sv.loadgame)
            initGame(); // the game is just starting

        level = levelstring; // bis hier her ok.

        // if there is a + in the map, set nextserver to the remainder

        int c = level.indexOf('+');
        if (c != -1) {
            ConsoleVar.Set("nextserver", "gamemap \"" + level.substring(c + 1) + "\"");
            level = level.substring(0, c);
        } else {
            ConsoleVar.Set("nextserver", "");
        }

        // rst: base1 works for full, damo1 works for demo, so we need to store first map.
        if (firstmap.length() == 0)
        {
        	if (!levelstring.endsWith(".cin") && !levelstring.endsWith(".pcx") && !levelstring.endsWith(".dm2"))
        	{
        		int pos = levelstring.indexOf('+');
        		firstmap = levelstring.substring(pos + 1);
        	}
        }

        // ZOID: special hack for end game screen in coop mode
        if (ConsoleVar.VariableValue("coop") != 0 && level.equals("victory.pcx"))
            ConsoleVar.Set("nextserver", "gamemap \"*" + firstmap + "\"");

        // if there is a $, use the remainder as a spawnpoint
        int pos = level.indexOf('$');
        if (pos != -1) {
            spawnpoint = level.substring(pos + 1);
            level = level.substring(0, pos);

        } else
            spawnpoint = "";

        // skip the end-of-unit flag * if necessary
        if (level.charAt(0) == '*')
            level = level.substring(1);

        l = level.length();
        if (l > 4 && level.endsWith(".cin")) {
            SCR.BeginLoadingPlaque(); // for local system
            SV_SEND.SV_BroadcastCommand("changing\n");
            spawnServer(level, spawnpoint, Defines.ss_cinematic,
                    attractloop, loadgame);
        } else if (l > 4 && level.endsWith(".dm2")) {
            SCR.BeginLoadingPlaque(); // for local system
            SV_SEND.SV_BroadcastCommand("changing\n");
            spawnServer(level, spawnpoint, Defines.ss_demo, attractloop,
                    loadgame);
        } else if (l > 4 && level.endsWith(".pcx")) {
            SCR.BeginLoadingPlaque(); // for local system
            SV_SEND.SV_BroadcastCommand("changing\n");
            spawnServer(level, spawnpoint, Defines.ss_pic, attractloop,
                    loadgame);
        } else {
            SCR.BeginLoadingPlaque(); // for local system
            SV_SEND.SV_BroadcastCommand("changing\n");
            SV_SEND.SV_SendClientMessages();
            spawnServer(level, spawnpoint, Defines.ss_game, attractloop,
                    loadgame);
            Cbuf.CopyToDefer();
        }

        SV_SEND.SV_BroadcastCommand("reconnect\n");
    }
}