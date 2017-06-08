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

// Created on 18.01.2004 by RST.
// $Id: ServerCommands.java,v 1.16 2007-06-07 10:31:10 cawe Exp $

package jake2.server;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.*;
import jake2.io.FileSystem;
import jake2.io.QuakeFile;
import jake2.network.Netchan;
import jake2.network.TNetAddr;
import jake2.qcommon.CM;
import jake2.qcommon.Command;
import jake2.qcommon.ConsoleVar;
import jake2.qcommon.TSizeBuffer;
import jake2.sys.Network;
import jake2.util.Lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;

public class ServerCommands {

	/*
    ===============================================================================
	
	OPERATOR CONSOLE ONLY COMMANDS
	
	These commands can only be entered from stdin or by a remote operator datagram
	===============================================================================
	*/

    /*
    ====================
    funcSetMaster

    Specify a list of master servers
    ====================
    */
    private static void funcSetMaster() {
        int i, slot;

        // only dedicated servers send heartbeats
        if (Context.dedicated.value == 0) {
            Command.Printf("Only dedicated servers use masters.\n");
            return;
        }

        // make sure the server is listed public
        ConsoleVar.Set("public", "1");

        for (i = 1; i < Defines.MAX_MASTERS; i++)
            ServerMain.master_adr[i] = new TNetAddr();

        slot = 1; // slot 0 will always contain the id master
        for (i = 1; i < Cmd.Argc(); i++) {
            if (slot == Defines.MAX_MASTERS)
                break;

            if (!Network.StringToAdr(Cmd.Argv(i), ServerMain.master_adr[i])) {
                Command.Printf("Bad address: " + Cmd.Argv(i) + "\n");
                continue;
            }
            if (ServerMain.master_adr[slot].port == 0)
                ServerMain.master_adr[slot].port = Defines.PORT_MASTER;

            Command.Printf("Master server at " + Network.AdrToString(ServerMain.master_adr[slot]) + "\n");
            Command.Printf("Sending a ping.\n");

            Netchan.OutOfBandPrint(Defines.NS_SERVER, ServerMain.master_adr[slot], "ping");

            slot++;
        }

        ServerInit.svs.last_heartbeat = -9999999;
    }

    /*
    ==================
    SV_SetPlayer

    Sets sv_client and sv_player to the player with idnum Cmd.valueAt(1)
    ==================
    */
    private static boolean SV_SetPlayer() {
        TClient cl;
        int i;
        int idnum;
        String s;

        if (Cmd.Argc() < 2)
            return false;

        s = Cmd.Argv(1);

        // numeric values are just slot numbers
        if (s.charAt(0) >= '0' && s.charAt(0) <= '9') {
            idnum = Lib.atoi(Cmd.Argv(1));
            if (idnum < 0 || idnum >= ServerMain.maxclients.value) {
                Command.Printf("Bad client slot: " + idnum + "\n");
                return false;
            }

            ServerMain.sv_client = ServerInit.svs.clients[idnum];
            ServerUser.sv_player = ServerMain.sv_client.edict;
            if (0 == ServerMain.sv_client.state) {
                Command.Printf("Client " + idnum + " is not active\n");
                return false;
            }
            return true;
        }

        // check for a name match
        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            if (0 == cl.state)
                continue;
            if (0 == Lib.strcmp(cl.name, s)) {
                ServerMain.sv_client = cl;
                ServerUser.sv_player = ServerMain.sv_client.edict;
                return true;
            }
        }

        Command.Printf("Userid " + s + " is not on the server\n");
        return false;
    }
	/*
	===============================================================================
	
	SAVEGAME FILES
	
	===============================================================================
	*/

    public static void remove(String name) {
        try {
            new File(name).delete();
        } catch (Exception e) {
        }
    }

    /**
     * Delete save files save/(number)/.
     */
    private static void wipeSavegame(String savename) {

        Command.DPrintf("SV_WipeSaveGame(" + savename + ")\n");

        String name = FileSystem.gamedir() + "/save/" + savename + "/server.ssv";
        remove(name);

        name = FileSystem.gamedir() + "/save/" + savename + "/game.ssv";
        remove(name);

        name = FileSystem.gamedir() + "/save/" + savename + "/*.sav";

        File f = FileSystem.FindFirst(name, 0, 0);
        while (f != null) {
            f.delete();
            f = FileSystem.findNext();
        }
        FileSystem.findClose();

        name = FileSystem.gamedir() + "/save/" + savename + "/*.sv2";

        f = FileSystem.FindFirst(name, 0, 0);

        while (f != null) {
            f.delete();
            f = FileSystem.findNext();
        }
        FileSystem.findClose();
    }

    /*
    ================
    CopyFile
    ================
    */
    private static void CopyFile(String src, String dst) {
        RandomAccessFile f1, f2;
        int l = -1;
        byte buffer[] = new byte[65536];

        //Command.DPrintf("CopyFile (" + src + ", " + dst + ")\n");
        try {
            f1 = new RandomAccessFile(src, "r");
        } catch (Exception e) {
            return;
        }
        try {
            f2 = new RandomAccessFile(dst, "rw");
        } catch (Exception e) {
            try {
                f1.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }

        while (true) {

            try {
                l = f1.read(buffer, 0, 65536);
            } catch (IOException e1) {

                e1.printStackTrace();
            }
            if (l == -1)
                break;
            try {
                f2.write(buffer, 0, l);
            } catch (IOException e2) {

                e2.printStackTrace();
            }
        }

        try {
            f1.close();
        } catch (IOException e1) {

            e1.printStackTrace();
        }
        try {
            f2.close();
        } catch (IOException e2) {

            e2.printStackTrace();
        }
    }

    /*
    ================
    SV_CopySaveGame
    ================
    */
    private static void SV_CopySaveGame(String src, String dst) {

        Command.DPrintf("SV_CopySaveGame(" + src + "," + dst + ")\n");

        wipeSavegame(dst);

        // copy the savegame over
        String name = FileSystem.gamedir() + "/save/" + src + "/server.ssv";
        String name2 = FileSystem.gamedir() + "/save/" + dst + "/server.ssv";
        FileSystem.CreatePath(name2);
        CopyFile(name, name2);

        name = FileSystem.gamedir() + "/save/" + src + "/game.ssv";
        name2 = FileSystem.gamedir() + "/save/" + dst + "/game.ssv";
        CopyFile(name, name2);

        String name1 = FileSystem.gamedir() + "/save/" + src + "/";
        name = FileSystem.gamedir() + "/save/" + src + "/*.sav";

        File found = FileSystem.FindFirst(name, 0, 0);

        while (found != null) {
            name = name1 + found.getName();
            name2 = FileSystem.gamedir() + "/save/" + dst + "/" + found.getName();

            CopyFile(name, name2);

            // change sav to sv2
            name = name.substring(0, name.length() - 3) + "sv2";
            name2 = name2.substring(0, name2.length() - 3) + "sv2";

            CopyFile(name, name2);

            found = FileSystem.findNext();
        }
        FileSystem.findClose();
    }

    /*
    ==============
    SV_WriteLevelFile

    ==============
    */
    private static void SV_WriteLevelFile() {

        String name;
        QuakeFile f;

        Command.DPrintf("SV_WriteLevelFile()\n");

        name = FileSystem.gamedir() + "/save/current/" + ServerInit.sv.name + ".sv2";

        try {
            f = new QuakeFile(name, "rw");

            for (int i = 0; i < Defines.MAX_CONFIGSTRINGS; i++)
                f.writeString(ServerInit.sv.configstrings[i]);

            CM.CM_WritePortalState(f);
            f.close();
        } catch (Exception e) {
            Command.Printf("Failed to open " + name + "\n");
            e.printStackTrace();
        }

        name = FileSystem.gamedir() + "/save/current/" + ServerInit.sv.name + ".sav";
        GameSave.WriteLevel(name);
    }

    /*
    ==============
    SV_ReadLevelFile

    ==============
    */
    public static void SV_ReadLevelFile() {
        //char name[MAX_OSPATH];
        String name;
        QuakeFile f;

        Command.DPrintf("SV_ReadLevelFile()\n");

        name = FileSystem.gamedir() + "/save/current/" + ServerInit.sv.name + ".sv2";
        try {
            f = new QuakeFile(name, "r");

            for (int n = 0; n < Defines.MAX_CONFIGSTRINGS; n++)
                ServerInit.sv.configstrings[n] = f.readString();

            CM.CM_ReadPortalState(f);

            f.close();
        } catch (IOException e1) {
            Command.Printf("Failed to open " + name + "\n");
            e1.printStackTrace();
        }

        name = FileSystem.gamedir() + "/save/current/" + ServerInit.sv.name + ".sav";
        GameSave.ReadLevel(name);
    }

    /*
    ==============
    SV_WriteServerFile

    ==============
    */
    private static void SV_WriteServerFile(boolean autosave) {

        String name, string, comment;
        Command.DPrintf("SV_WriteServerFile(" + (autosave ? "true" : "false") + ")\n");

        String filename = FileSystem.gamedir() + "/save/current/server.ssv";
        try {
            QuakeFile f = new QuakeFile(filename, "rw");

            if (!autosave) {
                Calendar c = Calendar.getInstance();
                comment =
                        Command.sprintf(
                                "%2i:%2i %2i/%2i  ",
                                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.MONTH) + 1,
                                c.get(Calendar.DAY_OF_MONTH));
                comment += ServerInit.sv.configstrings[Defines.CS_NAME];
            } else {
                // autosaved
                comment = "ENTERING " + ServerInit.sv.configstrings[Defines.CS_NAME];
            }

            f.writeString(comment);
            f.writeString(ServerInit.svs.mapcmd);

            // write the mapcmd

            // write all CVAR_FLAG_LATCH cvars
            // these will be things like coop, skill, deathmatch, etc
            for (TVar var : ConsoleVar.cvar_vars) {
                if (0 == (var.flags & TVar.CVAR_FLAG_LATCH))
                    continue;
                if (var.name.length() >= Defines.MAX_OSPATH - 1 || var.string.length() >= 128 - 1) {
                    Command.Printf("ConsoleVar too long: " + var.name + " = " + var.string + "\n");
                    continue;
                }

                name = var.name;
                string = var.string;
                try {
                    f.writeString(name);
                    f.writeString(string);
                } catch (IOException e2) {
                }

            }
            // rst: for termination.
            f.writeString(null);
            f.close();
        } catch (Exception e) {
            Command.Printf("Couldn't write " + filename + "\n");
        }

        // write game state
        filename = FileSystem.gamedir() + "/save/current/game.ssv";
        GameSave.WriteGame(filename, autosave);
    }

    /*
    ==============
    SV_ReadServerFile

    ==============
    */
    private static void SV_ReadServerFile() {
        String filename = "", name = "", string, mapcmd;
        try {
            QuakeFile f;

            mapcmd = "";

            Command.DPrintf("SV_ReadServerFile()\n");

            filename = FileSystem.gamedir() + "/save/current/server.ssv";

            f = new QuakeFile(filename, "r");

            // read the comment field but ignore
            f.readString();

            // read the mapcmd
            mapcmd = f.readString();

            // read all CVAR_FLAG_LATCH cvars
            // these will be things like coop, skill, deathmatch, etc
            while (true) {
                name = f.readString();
                if (name == null)
                    break;
                string = f.readString();

                Command.DPrintf("Set " + name + " = " + string + "\n");
                ConsoleVar.ForceSet(name, string);
            }

            f.close();

            // start a new game fresh with new cvars
            ServerInit.initGame();

            ServerInit.svs.mapcmd = mapcmd;

            // read game state
            filename = FileSystem.gamedir() + "/save/current/game.ssv";
            GameSave.ReadGame(filename);
        } catch (Exception e) {
            Command.Printf("Couldn't read file " + filename + "\n");
            e.printStackTrace();
        }
    }
    //=========================================================

    /*
    ==================
    funcDemoMap

    Puts the server in demo mode on a specific map/cinematic
    ==================
    */
    private static void funcDemoMap() {
        ServerInit.SV_Map(true, Cmd.Argv(1), false);
    }

    /*
    ==================
    funcGameMap

    Saves the state of the map just being exited and goes to a new map.

    If the initial character of the map string is '*', the next map is
    in a new unit, so the current savegame directory is cleared of
    map files.

    Example:

    *inter.cin+jail

    Clears the archived maps, plays the inter.cin cinematic, then
    goes to map jail.bsp.
    ==================
    */
    private static void funcGameMap() {

        if (Cmd.Argc() != 2) {
            Command.Printf("USAGE: gamemap <map>\n");
            return;
        }

        Command.DPrintf("SV_GameMap(" + Cmd.Argv(1) + ")\n");

        FileSystem.CreatePath(FileSystem.gamedir() + "/save/current/");

        // check for clearing the current savegame
        String map = Cmd.Argv(1);
        if (map.charAt(0) == '*') {
            // wipe all the *.sav files
            wipeSavegame("current");
        } else { // save the map just exited
            if (ServerInit.sv.state == Defines.ss_game) {
                // clear all the client inUse flags before saving so that
                // when the level is re-entered, the clients will spawn
                // at spawn points instead of occupying body shells
                TClient cl;
                boolean[] savedInuse = new boolean[(int) ServerMain.maxclients.value];
                for (int i = 0; i < ServerMain.maxclients.value; i++) {
                    cl = ServerInit.svs.clients[i];
                    savedInuse[i] = cl.edict.inUse;
                    cl.edict.inUse = false;
                }

                SV_WriteLevelFile();

                // we must restore these for clients to transfer over correctly
                for (int i = 0; i < ServerMain.maxclients.value; i++) {
                    cl = ServerInit.svs.clients[i];
                    cl.edict.inUse = savedInuse[i];

                }
                savedInuse = null;
            }
        }

        // start up the next map
        ServerInit.SV_Map(false, Cmd.Argv(1), false);

        // archive server state
        ServerInit.svs.mapcmd = Cmd.Argv(1);

        // copy off the level to the autosave slot
        if (0 == Context.dedicated.value) {
            SV_WriteServerFile(true);
            SV_CopySaveGame("current", "save0");
        }
    }

    /**
     * Goes directly to a given map without any savegame archiving.
     * For development work
     */
    private static void funcMap() {

        // if not a pcx, demo, or cinematic, check to make sure the level exists
        String map = Cmd.Argv(1);
        if (!map.contains(".")) {
            String expanded = "maps/" + map + ".bsp";
            if (FileSystem.loadFile(expanded) == null) {
                Command.Printf("Can't find " + expanded + "\n");
                return;
            }
        }

        ServerInit.sv.state = Defines.ss_dead; // don't save current level when changing

        wipeSavegame("current");
        funcGameMap();
    }

    private static void funcLoadGame() {

        if (Cmd.Argc() != 2) {
            Command.Printf("USAGE: loadgame <directory>\n");
            return;
        }

        Command.Printf("Loading game...\n");

        String dir = Cmd.Argv(1);
        if ((dir.indexOf("..") > -1) || (dir.indexOf("/") > -1) || (dir.indexOf("\\") > -1)) {
            Command.Printf("Bad savedir.\n");
        }

        // make sure the server.ssv file exists
        String name = FileSystem.gamedir() + "/save/" + Cmd.Argv(1) + "/server.ssv";
        RandomAccessFile f;
        try {
            f = new RandomAccessFile(name, "r");
        } catch (FileNotFoundException e) {
            Command.Printf("No such savegame: " + name + "\n");
            return;
        }

        try {
            f.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        SV_CopySaveGame(Cmd.Argv(1), "current");
        SV_ReadServerFile();

        // go to the map
        ServerInit.sv.state = Defines.ss_dead; // don't save current level when changing
        ServerInit.SV_Map(false, ServerInit.svs.mapcmd, true);
    }

    private static void funcSaveGame() {
        String dir;

        if (ServerInit.sv.state != Defines.ss_game) {
            Command.Printf("You must be in a game to save.\n");
            return;
        }

        if (Cmd.Argc() != 2) {
            Command.Printf("USAGE: savegame <directory>\n");
            return;
        }

        if (ConsoleVar.VariableValue("deathmatch") != 0) {
            Command.Printf("Can't savegame in a deathmatch\n");
            return;
        }

        if (0 == Lib.strcmp(Cmd.Argv(1), "current")) {
            Command.Printf("Can't save to 'current'\n");
            return;
        }

        if (ServerMain.maxclients.value == 1 && ServerInit.svs.clients[0].edict.client.ps.stats[Defines.STAT_HEALTH] <= 0) {
            Command.Printf("\nCan't savegame while dead!\n");
            return;
        }

        dir = Cmd.Argv(1);
        if ((dir.indexOf("..") > -1) || (dir.indexOf("/") > -1) || (dir.indexOf("\\") > -1)) {
            Command.Printf("Bad savedir.\n");
        }

        Command.Printf("Saving game...\n");

        // archive current level, including all client edicts.
        // when the level is reloaded, they will be shells awaiting
        // a connecting client
        SV_WriteLevelFile();

        // save server state
        try {
            SV_WriteServerFile(false);
        } catch (Exception e) {
            Command.Printf("IOError in SV_WriteServerFile: " + e);
        }

        // copy it off
        SV_CopySaveGame("current", dir);
        Command.Printf("Done.\n");
    }


    /** Kick a user off of the server. */
    private static void funcKick() {
        if (!ServerInit.svs.initialized) {
            Command.Printf("No server running.\n");
            return;
        }

        if (Cmd.Argc() != 2) {
            Command.Printf("Usage: kick <userid>\n");
            return;
        }

        if (!SV_SetPlayer())
            return;

        SV_SEND.SV_BroadcastPrintf(Defines.PRINT_HIGH, ServerMain.sv_client.name + " was kicked\n");
        // print directly, because the dropped client won't get the
        // SV_BroadcastPrintf message
        SV_SEND.SV_ClientPrintf(ServerMain.sv_client, Defines.PRINT_HIGH, "You were kicked from the game\n");
        ServerMain.SV_DropClient(ServerMain.sv_client);
        ServerMain.sv_client.lastmessage = ServerInit.svs.realtime; // min case there is a funny zombie
    }

    private static void funcStatus() {
        int i, j, l;
        TClient cl;
        String s;
        int ping;
        if (ServerInit.svs.clients == null) {
            Command.Printf("No server running.\n");
            return;
        }
        Command.Printf("map              : " + ServerInit.sv.name + "\n");

        Command.Printf("num score ping name            lastmsg address               qport \n");
        Command.Printf("--- ----- ---- --------------- ------- --------------------- ------\n");
        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            if (0 == cl.state)
                continue;

            Command.Printf("%3i ", i);
            Command.Printf("%5i ", cl.edict.client.ps.stats[Defines.STAT_FRAGS]);

            if (cl.state == Defines.cs_connected)
                Command.Printf("CNCT ");
            else if (cl.state == Defines.cs_zombie)
                Command.Printf("ZMBI ");
            else {
                ping = cl.ping < 9999 ? cl.ping : 9999;
                Command.Printf("%4i ", ping);
            }

            Command.Printf("%entityState", cl.name);
            l = 16 - cl.name.length();
            for (j = 0; j < l; j++)
                Command.Printf(" ");

            Command.Printf("%7i ", ServerInit.svs.realtime - cl.lastmessage);

            s = Network.AdrToString(cl.netchan.remote_address);
            Command.Printf(s);
            l = 22 - s.length();
            for (j = 0; j < l; j++)
                Command.Printf(" ");

            Command.Printf("%5i", cl.netchan.qport);

            Command.Printf("\n");
        }
        Command.Printf("\n");
    }

    private static void funcConSay() {
        TClient client;
        int j;
        String p;
        String text; // char[1024];

        if (Cmd.Argc() < 2)
            return;

        text = "console: ";
        p = Cmd.Args();

        if (p.charAt(0) == '"') {
            p = p.substring(1, p.length() - 1);
        }

        text += p;

        for (j = 0; j < ServerMain.maxclients.value; j++) {
            client = ServerInit.svs.clients[j];
            if (client.state != Defines.cs_spawned)
                continue;
            SV_SEND.SV_ClientPrintf(client, Defines.PRINT_CHAT, text + "\n");
        }
    }

    private static void funcHeartbeat() {
        ServerInit.svs.last_heartbeat = -9999999;
    }

    /** Examine or change the serverinfo string. */
    private static void funcServerInfo() {
        Command.Printf("Server info settings:\n");
        Info.Print(ConsoleVar.Serverinfo());
    }

    /** Examine all a users info strings. */
    private static void funcDumpUser() {
        if (Cmd.Argc() != 2) {
            Command.Printf("Usage: info <userid>\n");
            return;
        }

        if (!SV_SetPlayer())
            return;

        Command.Printf("userinfo\n");
        Command.Printf("--------\n");
        Info.Print(ServerMain.sv_client.userinfo);

    }

    /**
     * Begins server demo recording.  Every entity and every message will be
     * recorded, but no playerinfo will be stored.  Primarily for demo merging.
     */
    private static void funcServerRecord() {
        //char	name[MAX_OSPATH];
        String name;
        byte buf_data[] = new byte[32768];
        TSizeBuffer buf = new TSizeBuffer();
        int len;
        int i;

        if (Cmd.Argc() != 2) {
            Command.Printf("serverrecord <demoname>\n");
            return;
        }

        if (ServerInit.svs.demofile != null) {
            Command.Printf("Already recording.\n");
            return;
        }

        if (ServerInit.sv.state != Defines.ss_game) {
            Command.Printf("You must be in a level to record.\n");
            return;
        }

        //
        // open the demo file
        //
        name = FileSystem.gamedir() + "/demos/" + Cmd.Argv(1) + ".dm2";

        Command.Printf("recording to " + name + ".\n");
        FileSystem.CreatePath(name);
        try {
            ServerInit.svs.demofile = new RandomAccessFile(name, "rw");
        } catch (Exception e) {
            Command.Printf("ERROR: couldn't open.\n");
            return;
        }

        // setup a buffer to catch all multicasts
        ServerInit.svs.demo_multicast.init(ServerInit.svs.demo_multicast_buf, ServerInit.svs.demo_multicast_buf.length);

        //
        // write a single giant fake message with all the startup info
        //
        buf.init(buf_data, buf_data.length);

        //
        // serverdata needs to go over for all types of servers
        // to make sure the protocol is right, and to set the gamedir
        //
        // send the serverdata
        buf.writeByte(Defines.svc_serverdata);
        buf.writeLong(Defines.PROTOCOL_VERSION);
        buf.writeLong(ServerInit.svs.spawncount);
        // 2 means server demo
        buf.writeByte(2); // demos are always attract loops
        buf.writeString(ConsoleVar.VariableString("gamedir"));
        buf.writeShort(-1);
        // send full levelname
        buf.writeString(ServerInit.sv.configstrings[Defines.CS_NAME]);

        for (i = 0; i < Defines.MAX_CONFIGSTRINGS; i++)
            if (ServerInit.sv.configstrings[i].length() == 0) {
                buf.writeByte(Defines.svc_configstring);
                buf.writeShort(i);
                buf.writeString(ServerInit.sv.configstrings[i]);
            }

        // write it to the demo file
        Command.DPrintf("signon message length: " + buf.cursize + "\n");
        len = EndianHandler.swapInt(buf.cursize);
        //fwrite(len, 4, 1, svs.demofile);
        //fwrite(buf.data, buf.cursize, 1, svs.demofile);
        try {
            ServerInit.svs.demofile.writeInt(len);
            ServerInit.svs.demofile.write(buf.data, 0, buf.cursize);
        } catch (IOException e1) {
            // TODO: do quake2 error handling!
            e1.printStackTrace();
        }

        // the rest of the demo file will be individual frames
    }

    /** Ends server demo recording */
    private static void funcServerStop() {
        if (ServerInit.svs.demofile == null) {
            Command.Printf("Not doing a serverrecord.\n");
            return;
        }
        try {
            ServerInit.svs.demofile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerInit.svs.demofile = null;
        Command.Printf("Recording completed.\n");
    }

    /** Kick everyone off, possibly in preparation for a new game. */
    private static void funcKillServer() {
        if (!ServerInit.svs.initialized)
            return;
        ServerMain.SV_Shutdown("Server was killed.\n", false);
        Network.Config(false); // close network sockets
    }

    /** Let the game dll handle a command. */
    private static void funcServerCommands() {
        GameSVCmds.ServerCommand();
    }

    public static void registerOperatorCommands() {
        Cmd.AddCommand("heartbeat", ServerCommands::funcHeartbeat);
        Cmd.AddCommand("kick", ServerCommands::funcKick);
        Cmd.AddCommand("status", ServerCommands::funcStatus);
        Cmd.AddCommand("serverinfo", ServerCommands::funcServerInfo);
        Cmd.AddCommand("dumpuser", ServerCommands::funcDumpUser);

        Cmd.AddCommand("map", ServerCommands::funcMap);
        Cmd.AddCommand("demomap", ServerCommands::funcDemoMap);
        Cmd.AddCommand("gamemap", ServerCommands::funcGameMap);
        Cmd.AddCommand("setmaster", ServerCommands::funcSetMaster);

        if (Context.dedicated.value != 0) {
            Cmd.AddCommand("say", ServerCommands::funcConSay);
        }

        Cmd.AddCommand("serverrecord", ServerCommands::funcServerRecord);
        Cmd.AddCommand("serverstop", ServerCommands::funcServerStop);

        Cmd.AddCommand("save", ServerCommands::funcSaveGame);
        Cmd.AddCommand("load", ServerCommands::funcLoadGame);

        Cmd.AddCommand("killserver", ServerCommands::funcKillServer);

        Cmd.AddCommand("sv", ServerCommands::funcServerCommands);
    }
}
