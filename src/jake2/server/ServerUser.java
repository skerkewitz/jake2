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

// Created on 17.01.2004 by RST.

package jake2.server;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.*;
import jake2.io.FileSystem;
import jake2.qcommon.*;
import jake2.util.Lib;

import java.io.IOException;

public class ServerUser {

    static TEntityDict sv_player;

    public static class TUserCommand {
        final String name;
        final Runnable r;

        public TUserCommand(String n, Runnable r) {
            name = n;
            this.r = r;
        }
    }

    static TUserCommand u1 = new TUserCommand("new", ServerUser::funcNew);

    static TUserCommand ucmds[] = {
    // auto issued
            new TUserCommand("new", ServerUser::funcNew),
            new TUserCommand("configstrings", () -> ServerUser.SV_Configstrings_f()),
            new TUserCommand("baselines", () -> ServerUser.SV_Baselines_f()),
            new TUserCommand("begin", () -> ServerUser.SV_Begin_f()),
            new TUserCommand("nextserver", () -> ServerUser.SV_Nextserver_f()),
            new TUserCommand("disconnect", () -> ServerUser.SV_Disconnect_f()),

            // issued by hand at client consoles
            new TUserCommand("info", () -> ServerUser.SV_ShowServerinfo_f()),
            new TUserCommand("download", () -> ServerUser.SV_BeginDownload_f()),
            new TUserCommand("nextdl", () -> ServerUser.SV_NextDownload_f())
    };

    public static final int MAX_STRINGCMDS = 8;

    /*
     * ============================================================
     * 
     * USER STRINGCMD EXECUTION
     * 
     * sv_client and sv_player will be valid.
     * ============================================================
     */

    /*
     * ================== SV_BeginDemoServer ==================
     */
    public static void SV_BeginDemoserver() {
        String name;

        name = "demos/" + ServerInit.sv.name;
        try {
            ServerInit.sv.demofile = FileSystem.openfile(name);
        } catch (IOException e) {
            Command.Error(Defines.ERR_DROP, "Couldn't open " + name + "\n");
        }
        if (ServerInit.sv.demofile == null)
            Command.Error(Defines.ERR_DROP, "Couldn't open " + name + "\n");
    }

    /*
     * ================ funcNew
     * 
     * Sends the first message from the server to a connected client. This will
     * be sent on the initial connection and upon each server load.
     * ================
     */
    private static void funcNew() {

        Command.DPrintf("New() from " + ServerMain.sv_client.name + "\n");

        if (ServerMain.sv_client.state != Defines.cs_connected) {
            Command.Printf("New not valid -- already spawned\n");
            return;
        }

        // demo servers just dump the file message
        if (ServerInit.sv.state == Defines.ss_demo) {
            SV_BeginDemoserver();
            return;
        }

        //
        // serverdata needs to go over for all types of servers
        // to make sure the protocol is right, and to set the gamedir
        //
        String gamedir = ConsoleVar.VariableString("gamedir");

        // send the serverdata
        ServerMain.sv_client.netchan.message.writeByte(Defines.svc_serverdata);
        ServerMain.sv_client.netchan.message.writeInt(Defines.PROTOCOL_VERSION);

        ServerMain.sv_client.netchan.message.writeLong(ServerInit.svs.spawncount);
        ServerMain.sv_client.netchan.message.writeByte(ServerInit.sv.attractloop ? 1 : 0);
        ServerMain.sv_client.netchan.message.writeString(gamedir);

        int playernum;
        if (ServerInit.sv.state == Defines.ss_cinematic || ServerInit.sv.state == Defines.ss_pic) {
            playernum = -1;
        } else {
            //playernum = sv_client - svs.clients;
            playernum = ServerMain.sv_client.serverindex;
        }

        ServerMain.sv_client.netchan.message.writeShort(playernum);

        // send full levelname
        ServerMain.sv_client.netchan.message.writeString(ServerInit.sv.configstrings[Defines.CS_NAME]);

        //
        // game server
        // 
        if (ServerInit.sv.state == Defines.ss_game) {
            // set up the entity for the client
            TEntityDict ent = GameBase.entityDicts[playernum + 1];
            ent.entityState.number = playernum + 1;
            ServerMain.sv_client.edict = ent;
            ServerMain.sv_client.lastcmd = new usercmd_t();

            // begin fetching configstrings
            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_stufftext);
            ServerMain.sv_client.netchan.message.writeString("cmd configstrings " + ServerInit.svs.spawncount + " 0\n");
        }
        
    }

    /*
     * ================== SV_Configstrings_f ==================
     */
    public static void SV_Configstrings_f() {
        int start;

        Command.DPrintf("Configstrings() from " + ServerMain.sv_client.name + "\n");

        if (ServerMain.sv_client.state != Defines.cs_connected) {
            Command.Printf("configstrings not valid -- already spawned\n");
            return;
        }

        // handle the case of a level changing while a client was connecting
        if (Lib.atoi(Cmd.Argv(1)) != ServerInit.svs.spawncount) {
            Command.Printf("SV_Configstrings_f from different level\n");
            funcNew();
            return;
        }

        start = Lib.atoi(Cmd.Argv(2));

        // write a packet full of data

        while (ServerMain.sv_client.netchan.message.writeHeadPosition < Defines.MAX_MSGLEN / 2
                && start < Defines.MAX_CONFIGSTRINGS) {
            if (ServerInit.sv.configstrings[start] != null
                    && ServerInit.sv.configstrings[start].length() != 0) {
                ServerMain.sv_client.netchan.message.writeByte(Defines.svc_configstring);
                ServerMain.sv_client.netchan.message.writeShort(start);
                ServerMain.sv_client.netchan.message.writeString(ServerInit.sv.configstrings[start]);
            }
            start++;
        }

        // send next command

        if (start == Defines.MAX_CONFIGSTRINGS) {
            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_stufftext);
            ServerMain.sv_client.netchan.message.writeString("cmd baselines "
                        + ServerInit.svs.spawncount + " 0\n");
        } else {
            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_stufftext);
            ServerMain.sv_client.netchan.message.writeString("cmd configstrings " + ServerInit.svs.spawncount + " " + start
                                + "\n");
        }
    }

    /*
     * ================== SV_Baselines_f ==================
     */
    public static void SV_Baselines_f() {
        int start;
        TEntityState nullstate;
        TEntityState base;

        Command.DPrintf("Baselines() from " + ServerMain.sv_client.name + "\n");

        if (ServerMain.sv_client.state != Defines.cs_connected) {
            Command.Printf("baselines not valid -- already spawned\n");
            return;
        }

        // handle the case of a level changing while a client was connecting
        if (Lib.atoi(Cmd.Argv(1)) != ServerInit.svs.spawncount) {
            Command.Printf("SV_Baselines_f from different level\n");
            funcNew();
            return;
        }

        start = Lib.atoi(Cmd.Argv(2));

        //memset (&nullstate, 0, sizeof(nullstate));
        nullstate = new TEntityState(null);

        // write a packet full of data

        while (ServerMain.sv_client.netchan.message.writeHeadPosition < Defines.MAX_MSGLEN / 2
                && start < Defines.MAX_EDICTS) {
            base = ServerInit.sv.baselines[start];
            if (base.modelIndex != 0 || base.sound != 0 || base.effects != 0) {
                ServerMain.sv_client.netchan.message.writeByte(Defines.svc_spawnbaseline);
                ServerMain.sv_client.netchan.message.writeDeltaEntity(nullstate, base, true, true);
            }
            start++;
        }

        // send next command

        if (start == Defines.MAX_EDICTS) {
            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_stufftext);
            ServerMain.sv_client.netchan.message.writeString("precache "
                        + ServerInit.svs.spawncount + "\n");
        } else {
            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_stufftext);
            ServerMain.sv_client.netchan.message.writeString("cmd baselines "
                        + ServerInit.svs.spawncount + " " + start + "\n");
        }
    }

    /*
     * ================== SV_Begin_f ==================
     */
    public static void SV_Begin_f() {
        Command.DPrintf("Begin() from " + ServerMain.sv_client.name + "\n");

        // handle the case of a level changing while a client was connecting
        if (Lib.atoi(Cmd.Argv(1)) != ServerInit.svs.spawncount) {
            Command.Printf("SV_Begin_f from different level\n");
            funcNew();
            return;
        }

        ServerMain.sv_client.state = Defines.cs_spawned;

        // call the game begin function
        PlayerClient.ClientBegin(ServerUser.sv_player);

        CommandBuffer.insertFromDefer();
    }

    //=============================================================================

    /*
     * ================== SV_NextDownload_f ==================
     */
    public static void SV_NextDownload_f() {
        int r;
        int percent;
        int size;

        if (ServerMain.sv_client.download == null)
            return;

        r = ServerMain.sv_client.downloadsize - ServerMain.sv_client.downloadcount;
        if (r > 1024)
            r = 1024;

        ServerMain.sv_client.netchan.message.writeByte(Defines.svc_download);
        ServerMain.sv_client.netchan.message.writeShort(r);

        ServerMain.sv_client.downloadcount += r;
        size = ServerMain.sv_client.downloadsize;
        if (size == 0)
            size = 1;
        percent = ServerMain.sv_client.downloadcount * 100 / size;
        ServerMain.sv_client.netchan.message.writeByte(percent);
        ServerMain.sv_client.netchan.message.write(ServerMain.sv_client.download, ServerMain.sv_client.downloadcount - r, r);

        if (ServerMain.sv_client.downloadcount != ServerMain.sv_client.downloadsize) {
            return;
        }

        FileSystem.FreeFile(ServerMain.sv_client.download);
        ServerMain.sv_client.download = null;
    }

    /*
     * ================== SV_BeginDownload_f ==================
     */
    public static void SV_BeginDownload_f() {
        String name;
        int offset = 0;

        name = Cmd.Argv(1);

        if (Cmd.Argc() > 2)
            offset = Lib.atoi(Cmd.Argv(2)); // downloaded offset

        // hacked by zoid to allow more conrol over download
        // first off, no .. or global allow check

        if (name.indexOf("..") != -1
                || ServerMain.allow_download.value == 0 // leading dot is no good
                || name.charAt(0) == '.' // leading slash bad as well, must be
                                         // in subdir
                || name.charAt(0) == '/' // next up, skin check
                || (name.startsWith("players/") && 0 == ServerMain.allow_download_players.value) // now
                                                                                              // models
                || (name.startsWith("models/") && 0 == ServerMain.allow_download_models.value) // now
                                                                                            // sounds
                || (name.startsWith("sound/") && 0 == ServerMain.allow_download_sounds.value)
                // now maps (note special case for maps, must not be in pak)
                || (name.startsWith("maps/") && 0 == ServerMain.allow_download_maps.value) // MUST
                                                                                        // be
                                                                                        // in a
                                                                                        // subdirectory
                || name.indexOf('/') == -1) { // don't allow anything with ..
                                              // path
            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_download);
            ServerMain.sv_client.netchan.message.writeShort(-1);
            ServerMain.sv_client.netchan.message.writeByte(0);
            return;
        }

        if (ServerMain.sv_client.download != null)
            FileSystem.FreeFile(ServerMain.sv_client.download);

        ServerMain.sv_client.download = FileSystem.loadFile(name);
        
        // rst: this handles loading errors, no message yet visible 
        if (ServerMain.sv_client.download == null)
        {        	
        	return;
        }
        
        ServerMain.sv_client.downloadsize = ServerMain.sv_client.download.length;
        ServerMain.sv_client.downloadcount = offset;

        if (offset > ServerMain.sv_client.downloadsize)
            ServerMain.sv_client.downloadcount = ServerMain.sv_client.downloadsize;

        if (ServerMain.sv_client.download == null // special check for maps, if it
                                               // came from a pak file, don't
                                               // allow
                							   // download ZOID
                || (name.startsWith("maps/") && FileSystem.file_from_pak != 0)) {
            Command.DPrintf("Couldn't download " + name + " to "
                    + ServerMain.sv_client.name + "\n");
            if (ServerMain.sv_client.download != null) {
                FileSystem.FreeFile(ServerMain.sv_client.download);
                ServerMain.sv_client.download = null;
            }

            ServerMain.sv_client.netchan.message.writeByte(Defines.svc_download);
            ServerMain.sv_client.netchan.message.writeShort(-1);
            ServerMain.sv_client.netchan.message.writeByte(0);
            return;
        }

        SV_NextDownload_f();
        Command.DPrintf("Downloading " + name + " to " + ServerMain.sv_client.name
                + "\n");
    }

    //============================================================================

    /*
     * ================= SV_Disconnect_f
     * 
     * The client is going to disconnect, so remove the connection immediately
     * =================
     */
    public static void SV_Disconnect_f() {
        //	SV_EndRedirect ();
        ServerMain.SV_DropClient(ServerMain.sv_client);
    }

    /*
     * ================== SV_ShowServerinfo_f
     * 
     * Dumps the serverinfo info string ==================
     */
    public static void SV_ShowServerinfo_f() {
        Info.Print(ConsoleVar.Serverinfo());
    }

    public static void SV_Nextserver() {
        String v;

        //ZOID, ss_pic can be nextserver'd in coop mode
        if (ServerInit.sv.state == Defines.ss_game
                || (ServerInit.sv.state == Defines.ss_pic &&
                        0 == ConsoleVar.VariableValue("coop")))
            return; // can't nextserver while playing a normal game

        ServerInit.svs.spawncount++; // make sure another doesn't sneak in
        v = ConsoleVar.VariableString("nextserver");
        //if (!v[0])
        if (v.length() == 0)
            CommandBuffer.AddText("killserver\n");
        else {
            CommandBuffer.AddText(v);
            CommandBuffer.AddText("\n");
        }
        ConsoleVar.Set("nextserver", "");
    }

    /*
     * ================== SV_Nextserver_f
     * 
     * A cinematic has completed or been aborted by a client, so move to the
     * next server, ==================
     */
    public static void SV_Nextserver_f() {
        if (Lib.atoi(Cmd.Argv(1)) != ServerInit.svs.spawncount) {
            Command.DPrintf("Nextserver() from wrong level, from "
                    + ServerMain.sv_client.name + "\n");
            return; // leftover from last server
        }

        Command.DPrintf("Nextserver() from " + ServerMain.sv_client.name + "\n");

        SV_Nextserver();
    }

    /*
     * ================== SV_ExecuteUserCommand ==================
     */
    public static void SV_ExecuteUserCommand(String s) {
        
        Command.dprintln("SV_ExecuteUserCommand:" + s );
        TUserCommand u = null;

        Cmd.TokenizeString(s.toCharArray(), true);
        ServerUser.sv_player = ServerMain.sv_client.edict;

        //	SV_BeginRedirect (RD_CLIENT);

        int i = 0;
        for (; i < ServerUser.ucmds.length; i++) {
            u = ServerUser.ucmds[i];
            if (Cmd.Argv(0).equals(u.name)) {
                u.r.run();
                break;
            }
        }

        if (i == ServerUser.ucmds.length && ServerInit.sv.state == Defines.ss_game)
            Cmd.ClientCommand(ServerUser.sv_player);

        //	SV_EndRedirect ();
    }

    /*
     * ===========================================================================
     * 
     * USER CMD EXECUTION
     * 
     * ===========================================================================
     */

    public static void SV_ClientThink(TClient cl, usercmd_t cmd) {
        cl.commandMsec -= cmd.msec & 0xFF;

        if (cl.commandMsec < 0 && ServerMain.sv_enforcetime.value != 0) {
            Command.DPrintf("commandMsec underflow from " + cl.name + "\n");
            return;
        }

        PlayerClient.ClientThink(cl.edict, cmd);
    }

    /*
     * =================== SV_ExecuteClientMessage
     * 
     * The current net_message is parsed for the given client
     * ===================
     */
    public static void SV_ExecuteClientMessage(TClient cl) {
        int c;
        String s;

        usercmd_t nullcmd = new usercmd_t();
        usercmd_t oldest = new usercmd_t(), oldcmd = new usercmd_t(), newcmd = new usercmd_t();
        int net_drop;
        int stringCmdCount;
        int checksum, calculatedChecksum;
        int checksumIndex;
        boolean move_issued;
        int lastframe;

        ServerMain.sv_client = cl;
        ServerUser.sv_player = ServerMain.sv_client.edict;

        // only allow one move command
        move_issued = false;
        stringCmdCount = 0;

        while (true) {
            if (Context.net_message.readHeadPosition > Context.net_message.writeHeadPosition) {
                Command.Printf("SV_ReadClientMessage: bad read:\n");
                Command.Printf(Lib.hexDump(Context.net_message.data, 32, false));
                ServerMain.SV_DropClient(cl);
                return;
            }

            c = TBuffer.ReadByte(Context.net_message);
            if (c == -1)
                break;

            switch (c) {
            default:
                Command.Printf("SV_ReadClientMessage: unknown command char\n");
                ServerMain.SV_DropClient(cl);
                return;

            case Defines.clc_nop:
                break;

            case Defines.clc_userinfo:
                cl.userinfo = TBuffer.ReadString(Context.net_message);
                ServerMain.SV_UserinfoChanged(cl);
                break;

            case Defines.clc_move:
                if (move_issued)
                    return; // someone is trying to cheat...

                move_issued = true;
                checksumIndex = Context.net_message.readHeadPosition;
                checksum = TBuffer.ReadByte(Context.net_message);
                lastframe = TBuffer.ReadLong(Context.net_message);

                if (lastframe != cl.lastframe) {
                    cl.lastframe = lastframe;
                    if (cl.lastframe > 0) {
                        cl.frame_latency[cl.lastframe
                                & (Defines.LATENCY_COUNTS - 1)] = ServerInit.svs.realtime
                                - cl.frames[cl.lastframe & Defines.UPDATE_MASK].getSentTime();
                    }
                }

                //memset (nullcmd, 0, sizeof(nullcmd));
                nullcmd = new usercmd_t();
                TBuffer.ReadDeltaUsercmd(Context.net_message, nullcmd, oldest);
                TBuffer.ReadDeltaUsercmd(Context.net_message, oldest, oldcmd);
                TBuffer.ReadDeltaUsercmd(Context.net_message, oldcmd, newcmd);

                if (cl.state != Defines.cs_spawned) {
                    cl.lastframe = -1;
                    break;
                }

                // if the checksum fails, ignore the rest of the packet

                calculatedChecksum = Command.BlockSequenceCRCByte(
                        Context.net_message.data, checksumIndex + 1,
                        Context.net_message.readHeadPosition - checksumIndex - 1,
                        cl.netchan.incoming_sequence);

                if ((calculatedChecksum & 0xff) != checksum) {
                    Command.DPrintf("Failed command checksum for " + cl.name + " ("
                            + calculatedChecksum + " != " + checksum + ")/"
                            + cl.netchan.incoming_sequence + "\n");
                    return;
                }

                if (0 == ServerMain.sv_paused.value) {
                    net_drop = cl.netchan.dropped;
                    if (net_drop < 20) {

                        //if (net_drop > 2)

                        //	Command.Printf ("drop %i\n", net_drop);
                        while (net_drop > 2) {
                            SV_ClientThink(cl, cl.lastcmd);

                            net_drop--;
                        }
                        if (net_drop > 1)
                            SV_ClientThink(cl, oldest);

                        if (net_drop > 0)
                            SV_ClientThink(cl, oldcmd);

                    }
                    SV_ClientThink(cl, newcmd);
                }

                // copy.
                cl.lastcmd.set(newcmd);
                break;

            case Defines.clc_stringcmd:
                s = TBuffer.ReadString(Context.net_message);

                // malicious users may try using too many string commands
                if (++stringCmdCount < ServerUser.MAX_STRINGCMDS)
                    SV_ExecuteUserCommand(s);

                if (cl.state == Defines.cs_zombie)
                    return; // disconnect command
                break;
            }
        }
    }
}