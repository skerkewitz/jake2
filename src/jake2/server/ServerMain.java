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

// Created on 13.01.2004 by RST.
// $Id: ServerMain.java,v 1.16 2006-01-20 22:44:07 salomo Exp $
package jake2.server;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.*;
import jake2.io.FileSystem;
import jake2.network.Netchan;
import jake2.network.TNetAddr;
import jake2.qcommon.*;
import jake2.sys.Network;
import jake2.sys.Timer;
import jake2.util.Lib;

import java.io.IOException;

public class ServerMain {

	/** Addess of group servers.*/ 
    public static TNetAddr master_adr[] = new TNetAddr[Defines.MAX_MASTERS];
                                                                            
                                                                            
                                                                            
    static {
        for (int i = 0; i < Defines.MAX_MASTERS; i++) {
            master_adr[i] = new TNetAddr();
        }
    }

    public static TClient sv_client; // current client

    public static TVar sv_paused;

    public static TVar sv_timedemo;

    public static TVar sv_enforcetime;

    public static TVar timeout; // seconds without any message

    public static TVar zombietime; // seconds to sink messages after
                                     // disconnect

    public static TVar rcon_password; // password for remote server commands

    public static TVar allow_download;

    public static TVar allow_download_players;

    public static TVar allow_download_models;

    public static TVar allow_download_sounds;

    public static TVar allow_download_maps;

    public static TVar sv_airaccelerate;

    public static TVar sv_noreload; // don't reload level state when
                                      // reentering

    public static TVar maxclients; // FIXME: rename sv_maxclients

    public static TVar sv_showclamp;

    public static TVar hostname;

    public static TVar public_server; // should heartbeats be sent

    public static TVar sv_reconnect_limit; // minimum seconds between connect
                                             // messages

    /**
     * Send a message to the master every few minutes to let it know we are
     * alive, and log information.
     */
    public static final int HEARTBEAT_SECONDS = 300;

    /**
     * Called when the player is totally leaving the server, either willingly or
     * unwillingly. This is NOT called if the entire server is quiting or
     * crashing.
     */
    public static void SV_DropClient(TClient drop) {
        // add the disconnect
        drop.netchan.message.writeByte(Defines.svc_disconnect);

        if (drop.state == Defines.cs_spawned) {
            // call the prog function for removing a client
            // this will remove the body, among other things
            PlayerClient.ClientDisconnect(drop.edict);
        }

        if (drop.download != null) {
            FileSystem.FreeFile(drop.download);
            drop.download = null;
        }

        drop.state = Defines.cs_zombie; // become free in a few seconds
        drop.name = "";
    }

    
    /* ==============================================================================
     * 
     * CONNECTIONLESS COMMANDS
     * 
     * ==============================================================================*/
     
    /**
     * Builds the string that is sent as heartbeats and status replies.
     */
    public static String SV_StatusString() {
        String player;
        String status = "";
        int i;
        TClient cl;
        int statusLength;
        int playerLength;

        status = ConsoleVar.Serverinfo() + "\n";

        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            if (cl.state == Defines.cs_connected
                    || cl.state == Defines.cs_spawned) {
                player = "" + cl.edict.client.ps.stats[Defines.STAT_FRAGS]
                        + " " + cl.ping + "\"" + cl.name + "\"\n";

                playerLength = player.length();
                statusLength = status.length();

                if (statusLength + playerLength >= 1024)
                    break; // can't hold any more

                status += player;
            }
        }

        return status;
    }

    /**
     * Responds with all the info that qplug or qspy can see
     */
    public static void SVC_Status() {
        Netchan.OutOfBandPrint(Defines.NS_SERVER, Context.net_from, "print\n"
                + SV_StatusString());
    }

    /**
     *  SVC_Ack
     */
    public static void SVC_Ack() {
        Command.Printf("Ping acknowledge from " + Network.AdrToString(Context.net_from)
                + "\n");
    }

    /**
     * SVC_Info, responds with short info for broadcast scans The second parameter should
     * be the current protocol version number.
     */
    public static void SVC_Info() {
        String string;
        int i, count;
        int version;

        if (ServerMain.maxclients.value == 1)
            return; // ignore in single player

        version = Lib.atoi(Cmd.Argv(1));

        if (version != Defines.PROTOCOL_VERSION)
            string = ServerMain.hostname.string + ": wrong version\n";
        else {
            count = 0;
            for (i = 0; i < ServerMain.maxclients.value; i++)
                if (ServerInit.svs.clients[i].state >= Defines.cs_connected)
                    count++;

            string = ServerMain.hostname.string + " " + ServerInit.sv.name + " "
                    + count + "/" + (int) ServerMain.maxclients.value + "\n";
        }

        Netchan.OutOfBandPrint(Defines.NS_SERVER, Context.net_from, "info\n"
                + string);
    }

    /**
     * SVC_Ping, Just responds with an acknowledgement.
     */
    public static void SVC_Ping() {
        Netchan.OutOfBandPrint(Defines.NS_SERVER, Context.net_from, "ack");
    }

    /** 
     * Returns a challenge number that can be used in a subsequent
     * client_connect command. We do this to prevent denial of service attacks
     * that flood the server with invalid connection IPs. With a challenge, they
     * must give a valid IP address.
     */
    public static void SVC_GetChallenge() {
        int i;
        int oldest;
        int oldestTime;

        oldest = 0;
        oldestTime = 0x7fffffff;

        // see if we already have a challenge for this ip
        for (i = 0; i < Defines.MAX_CHALLENGES; i++) {
            if (Network.CompareBaseAdr(Context.net_from,
                    ServerInit.svs.challenges[i].adr))
                break;
            if (ServerInit.svs.challenges[i].time < oldestTime) {
                oldestTime = ServerInit.svs.challenges[i].time;
                oldest = i;
            }
        }

        if (i == Defines.MAX_CHALLENGES) {
            // overwrite the oldest
            ServerInit.svs.challenges[oldest].challenge = Lib.rand() & 0x7fff;
            ServerInit.svs.challenges[oldest].adr = Context.net_from;
            ServerInit.svs.challenges[oldest].time = Context.curtime;
            i = oldest;
        }

        // send it back
        Netchan.OutOfBandPrint(Defines.NS_SERVER, Context.net_from,
                "challenge " + ServerInit.svs.challenges[i].challenge);
    }

    /**
     * A connection request that did not come from the master.
     */
    public static void SVC_DirectConnect() {
        String userinfo;
        TNetAddr adr;
        int i;
        TClient cl;

        int version;
        int qport;

        adr = Context.net_from;

        Command.DPrintf("SVC_DirectConnect ()\n");

        version = Lib.atoi(Cmd.Argv(1));
        if (version != Defines.PROTOCOL_VERSION) {
            Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                    "print\nServer is version " + Context.VERSION + "\n");
            Command.DPrintf("    rejected connect from version " + version + "\n");
            return;
        }

        qport = Lib.atoi(Cmd.Argv(2));
        int challenge = Lib.atoi(Cmd.Argv(3));
        userinfo = Cmd.Argv(4);

        // force the IP key/value pair so the game can filter based on ip
        userinfo = Info.Info_SetValueForKey(userinfo, "ip", Network.AdrToString(Context.net_from));

        // attractloop servers are ONLY for local clients
        if (ServerInit.sv.attractloop) {
            if (!Network.IsLocalAddress(adr)) {
                Command.Printf("Remote connect in attract loop.  Ignored.\n");
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                        "print\nConnection refused.\n");
                return;
            }
        }

        // see if the challenge is valid
        if (!Network.IsLocalAddress(adr)) {
            for (i = 0; i < Defines.MAX_CHALLENGES; i++) {
                if (Network.CompareBaseAdr(Context.net_from,
                        ServerInit.svs.challenges[i].adr)) {
                    if (challenge == ServerInit.svs.challenges[i].challenge)
                        break; // good
                    Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                            "print\nBad challenge.\n");
                    return;
                }
            }
            if (i == Defines.MAX_CHALLENGES) {
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                        "print\nNo challenge for address.\n");
                return;
            }
        }

        // if there is already a slot for this ip, reuse it
        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];

            if (cl.state == Defines.cs_free)
                continue;
            if (Network.CompareBaseAdr(adr, cl.netchan.remote_address)
                    && (cl.netchan.qport == qport || adr.port == cl.netchan.remote_address.port)) {
                if (!Network.IsLocalAddress(adr)
                        && (ServerInit.svs.realtime - cl.lastconnect) < ((int) ServerMain.sv_reconnect_limit.value * 1000)) {
                    Command.DPrintf(Network.AdrToString(adr)
                            + ":reconnect rejected : too soon\n");
                    return;
                }
                Command.Printf(Network.AdrToString(adr) + ":reconnect\n");

                gotnewcl(i, challenge, userinfo, adr, qport);
                return;
            }
        }

        // find a client slot
        //newcl = null;
        int index = -1;
        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            if (cl.state == Defines.cs_free) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                    "print\nServer is full.\n");
            Command.DPrintf("Rejected a connection.\n");
            return;
        }
        gotnewcl(index, challenge, userinfo, adr, qport);
    }

    /**
     * Initializes player structures after successfull connection.
     */
    public static void gotnewcl(int i, int challenge, String userinfo,
                                TNetAddr adr, int qport) {
        // build a new connection
        // accept the new client
        // this is the only place a TClient is ever initialized

        ServerMain.sv_client = ServerInit.svs.clients[i];
        
        int edictnum = i + 1;
        
        TEntityDict ent = GameBase.entityDicts[edictnum];
        ServerInit.svs.clients[i].edict = ent;
        
        // save challenge for checksumming
        ServerInit.svs.clients[i].challenge = challenge;
        
        

        // get the game a chance to reject this connection or modify the
        // userinfo
        if (!(PlayerClient.ClientConnect(ent, userinfo))) {
            if (Info.Info_ValueForKey(userinfo, "rejmsg") != null)
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr, "print\n"
                        + Info.Info_ValueForKey(userinfo, "rejmsg")
                        + "\nConnection refused.\n");
            else
                Netchan.OutOfBandPrint(Defines.NS_SERVER, adr,
                        "print\nConnection refused.\n");
            Command.DPrintf("Game rejected a connection.\n");
            return;
        }

        // parse some info from the info strings
        ServerInit.svs.clients[i].userinfo = userinfo;
        SV_UserinfoChanged(ServerInit.svs.clients[i]);

        // send the connect packet to the client
        Netchan.OutOfBandPrint(Defines.NS_SERVER, adr, "client_connect");

        Netchan.Setup(Defines.NS_SERVER, ServerInit.svs.clients[i].netchan, adr, qport);

        ServerInit.svs.clients[i].state = Defines.cs_connected;

        ServerInit.svs.clients[i].datagram.init(ServerInit.svs.clients[i].datagram_buf, ServerInit.svs.clients[i].datagram_buf.length);
        
        ServerInit.svs.clients[i].datagram.allowoverflow = true;
        ServerInit.svs.clients[i].lastmessage = ServerInit.svs.realtime; // don't timeout
        ServerInit.svs.clients[i].lastconnect = ServerInit.svs.realtime;
        Command.DPrintf("new client added.\n");
    }

    
    /** 
     * Checks if the rcon password is corect.
     */
    public static int Rcon_Validate() {
        if (0 == ServerMain.rcon_password.string.length())
            return 0;

        if (0 != Lib.strcmp(Cmd.Argv(1), ServerMain.rcon_password.string))
            return 0;

        return 1;
    }

    /**
     * A client issued an rcon command. Shift down the remaining args Redirect
     * all printfs fromt hte server to the client.
     */
    public static void SVC_RemoteCommand() {
        int i;
        String remaining;

        i = Rcon_Validate();

        String msg = Lib.CtoJava(Context.net_message.data, 4, 1024);

        if (i == 0)
            Command.Printf("Bad rcon from " + Network.AdrToString(Context.net_from)
                    + ":\n" + msg + "\n");
        else
            Command.Printf("Rcon from " + Network.AdrToString(Context.net_from) + ":\n"
                    + msg + "\n");

        Command.BeginRedirect(Defines.RD_PACKET, SV_SEND.sv_outputbuf,
                Defines.SV_OUTPUTBUF_LENGTH, new Command.RD_Flusher() {
                    public void rd_flush(int target, StringBuffer buffer) {
                        SV_SEND.SV_FlushRedirect(target, Lib.stringToBytes(buffer.toString()));
                    }
                });

        if (0 == Rcon_Validate()) {
            Command.Printf("Bad rcon_password.\n");
        } else {
            remaining = "";

            for (i = 2; i < Cmd.Argc(); i++) {
                remaining += Cmd.Argv(i);
                remaining += " ";
            }

            Cmd.ExecuteString(remaining);
        }

        Command.EndRedirect();
    }

    /**
     * A connectionless packet has four leading 0xff characters to distinguish
     * it from a game channel. Clients that are in the game can still send
     * connectionless packets. It is used also by rcon commands.
     */
    public static void SV_ConnectionlessPacket() {
        String s;
        String c;

        TSizeBuffer.BeginReading(Context.net_message);
        TSizeBuffer.ReadLong(Context.net_message); // skip the -1 marker

        s = TSizeBuffer.ReadStringLine(Context.net_message);

        Cmd.TokenizeString(s.toCharArray(), false);

        c = Cmd.Argv(0);
        
        //for debugging purposes 
        //Command.Printf("Packet " + Network.AdrToString(Netchan.net_from) + " : " + c + "\n");
        //Command.Printf(Lib.hexDump(net_message.data, 64, false) + "\n");

        if (0 == Lib.strcmp(c, "ping"))
            SVC_Ping();
        else if (0 == Lib.strcmp(c, "ack"))
            SVC_Ack();
        else if (0 == Lib.strcmp(c, "status"))
            SVC_Status();
        else if (0 == Lib.strcmp(c, "info"))
            SVC_Info();
        else if (0 == Lib.strcmp(c, "getchallenge"))
            SVC_GetChallenge();
        else if (0 == Lib.strcmp(c, "connect"))
            SVC_DirectConnect();
        else if (0 == Lib.strcmp(c, "rcon"))
            SVC_RemoteCommand();
        else {
            Command.Printf("bad connectionless packet from "
                    + Network.AdrToString(Context.net_from) + "\n");
            Command.Printf("[" + s + "]\n");
            Command.Printf("" + Lib.hexDump(Context.net_message.data, 128, false));
        }
    }

    /**
     * Updates the cl.ping variables.
     */
    public static void SV_CalcPings() {
        int i, j;
        TClient cl;
        int total, count;

        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            if (cl.state != Defines.cs_spawned)
                continue;

            total = 0;
            count = 0;
            for (j = 0; j < Defines.LATENCY_COUNTS; j++) {
                if (cl.frame_latency[j] > 0) {
                    count++;
                    total += cl.frame_latency[j];
                }
            }
            if (0 == count)
                cl.ping = 0;
            else
                cl.ping = total / count;

            // let the game dll know about the ping
            cl.edict.client.ping = cl.ping;
        }
    }

    /**
     * Every few frames, gives all clients an allotment of milliseconds for
     * their command moves. If they exceed it, assume cheating.
     */
    public static void SV_GiveMsec() {
        int i;
        TClient cl;

        if ((ServerInit.sv.framenum & 15) != 0)
            return;

        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            if (cl.state == Defines.cs_free)
                continue;

            cl.commandMsec = 1800; // 1600 + some slop
        }
    }

    /**
     * Reads packets from the network or loopback.
     */
    public static void SV_ReadPackets() {
        int i;
        TClient cl;
        int qport = 0;

        while (Network.GetPacket(Defines.NS_SERVER, Context.net_from,
                Context.net_message)) {

            // check for connectionless packet (0xffffffff) first
            if ((Context.net_message.data[0] == -1)
                    && (Context.net_message.data[1] == -1)
                    && (Context.net_message.data[2] == -1)
                    && (Context.net_message.data[3] == -1)) {
                SV_ConnectionlessPacket();
                continue;
            }

            // read the qport out of the message so we can fix up
            // stupid address translating routers
            TSizeBuffer.BeginReading(Context.net_message);
            TSizeBuffer.ReadLong(Context.net_message); // sequence number
            TSizeBuffer.ReadLong(Context.net_message); // sequence number
            qport = TSizeBuffer.ReadShort(Context.net_message) & 0xffff;

            // check for packets from connected clients
            for (i = 0; i < ServerMain.maxclients.value; i++) {
                cl = ServerInit.svs.clients[i];
                if (cl.state == Defines.cs_free)
                    continue;
                if (!Network.CompareBaseAdr(Context.net_from,
                        cl.netchan.remote_address))
                    continue;
                if (cl.netchan.qport != qport)
                    continue;
                if (cl.netchan.remote_address.port != Context.net_from.port) {
                    Command.Printf("SV_ReadPackets: fixing up a translated port\n");
                    cl.netchan.remote_address.port = Context.net_from.port;
                }

                if (Netchan.Process(cl.netchan, Context.net_message)) {
                    // this is a valid, sequenced packet, so process it
                    if (cl.state != Defines.cs_zombie) {
                        cl.lastmessage = ServerInit.svs.realtime; // don't timeout
                        ServerUser.SV_ExecuteClientMessage(cl);
                    }
                }
                break;
            }

            if (i != ServerMain.maxclients.value)
                continue;
        }
    }

    /**
     * If a packet has not been received from a client for timeout.value
     * seconds, drop the conneciton. Server frames are used instead of realtime
     * to avoid dropping the local client while debugging.
     * 
     * When a client is normally dropped, the TClient goes into a zombie state
     * for a few seconds to make sure any final reliable message gets resent if
     * necessary.
     */
    public static void SV_CheckTimeouts() {
        int i;
        TClient cl;
        int droppoint;
        int zombiepoint;

        droppoint = (int) (ServerInit.svs.realtime - 1000 * ServerMain.timeout.value);
        zombiepoint = (int) (ServerInit.svs.realtime - 1000 * ServerMain.zombietime.value);

        for (i = 0; i < ServerMain.maxclients.value; i++) {
            cl = ServerInit.svs.clients[i];
            // message times may be wrong across a changelevel
            if (cl.lastmessage > ServerInit.svs.realtime)
                cl.lastmessage = ServerInit.svs.realtime;

            if (cl.state == Defines.cs_zombie && cl.lastmessage < zombiepoint) {
                cl.state = Defines.cs_free; // can now be reused
                continue;
            }
            if ((cl.state == Defines.cs_connected || cl.state == Defines.cs_spawned)
                    && cl.lastmessage < droppoint) {
                SV_SEND.SV_BroadcastPrintf(Defines.PRINT_HIGH, cl.name
                        + " timed out\n");
                SV_DropClient(cl);
                cl.state = Defines.cs_free; // don't bother with zombie state
            }
        }
    }

    /**
     * SV_PrepWorldFrame
     * 
     * This has to be done before the world logic, because player processing
     * happens outside RunWorldFrame.
     */
    public static void SV_PrepWorldFrame() {
        TEntityDict ent;
        int i;

        for (i = 0; i < GameBase.num_edicts; i++) {
            ent = GameBase.entityDicts[i];
            // events only last for a single message
            ent.entityState.event = 0;
        }

    }

    /**
     * SV_RunGameFrame.
     */
    public static void SV_RunGameFrame() {
        if (Context.host_speeds.value != 0)
            Context.time_before_game = Timer.Milliseconds();

        // we always need to bump framenum, even if we
        // don't run the world, otherwise the delta
        // compression can get confused when a client
        // has the "current" frame
        ServerInit.sv.framenum++;
        ServerInit.sv.time = ServerInit.sv.framenum * 100;

        // don't run if paused
        if (0 == ServerMain.sv_paused.value || ServerMain.maxclients.value > 1) {
            GameBase.runFrame();

            // never get more than one tic behind
            if (ServerInit.sv.time < ServerInit.svs.realtime) {
                if (ServerMain.sv_showclamp.value != 0)
                    Command.Printf("sv highclamp\n");
                ServerInit.svs.realtime = ServerInit.sv.time;
            }
        }

        if (Context.host_speeds.value != 0)
            Context.time_after_game = Timer.Milliseconds();

    }

    /**
     * SV_Frame.
     */
    public static void SV_Frame(long msec) {
        Context.time_before_game = Context.time_after_game = 0;

        // if server is not active, do nothing
        if (!ServerInit.svs.initialized)
            return;

        ServerInit.svs.realtime += msec;

        // keep the random time dependent
        Lib.rand();

        // check timeouts
        SV_CheckTimeouts();

        // get packets from clients
        SV_ReadPackets();

        //if (Game.entityDicts[1] !=null)
        //	Command.p("player at:" + Lib.vtofsbeaty(Game.entityDicts[1].entityState.origin ));

        // move autonomous things around if enough time has passed
        if (0 == ServerMain.sv_timedemo.value
                && ServerInit.svs.realtime < ServerInit.sv.time) {
            // never let the time get too far off
            if (ServerInit.sv.time - ServerInit.svs.realtime > 100) {
                if (ServerMain.sv_showclamp.value != 0)
                    Command.Printf("sv lowclamp\n");
                ServerInit.svs.realtime = ServerInit.sv.time - 100;
            }
            Network.Sleep(ServerInit.sv.time - ServerInit.svs.realtime);
            return;
        }

        // update ping based on the last known frame from all clients
        SV_CalcPings();

        // give the clients some timeslices
        SV_GiveMsec();

        // let everything in the world think and move
        SV_RunGameFrame();

        // send messages back to the clients that had packets read this frame
        SV_SEND.SV_SendClientMessages();

        // save the entire world state if recording a serverdemo
        SV_ENTS.SV_RecordDemoMessage();

        // send a heartbeat to the master if needed
        Master_Heartbeat();

        // clear teleport flags, etc for next frame
        SV_PrepWorldFrame();

    }

    public static void Master_Heartbeat() {
        String string;
        int i;

        // pgm post3.19 change, cvar pointer not validated before dereferencing
        if (Context.dedicated == null || 0 == Context.dedicated.value)
            return; // only dedicated servers send heartbeats

        // pgm post3.19 change, cvar pointer not validated before dereferencing
        if (null == ServerMain.public_server || 0 == ServerMain.public_server.value)
            return; // a private dedicated game

        // check for time wraparound
        if (ServerInit.svs.last_heartbeat > ServerInit.svs.realtime)
            ServerInit.svs.last_heartbeat = ServerInit.svs.realtime;

        if (ServerInit.svs.realtime - ServerInit.svs.last_heartbeat < ServerMain.HEARTBEAT_SECONDS * 1000)
            return; // not time to send yet

        ServerInit.svs.last_heartbeat = ServerInit.svs.realtime;

        // send the same string that we would give for a status OOB command
        string = SV_StatusString();

        // send to group master
        for (i = 0; i < Defines.MAX_MASTERS; i++)
            if (ServerMain.master_adr[i].port != 0) {
                Command.Printf("Sending heartbeat to "
                        + Network.AdrToString(ServerMain.master_adr[i]) + "\n");
                Netchan.OutOfBandPrint(Defines.NS_SERVER,
                        ServerMain.master_adr[i], "heartbeat\n" + string);
            }
    }
    

    /**
     * Master_Shutdown, Informs all masters that this server is going down.
     */
    public static void Master_Shutdown() {
        int i;

        // pgm post3.19 change, cvar pointer not validated before dereferencing
        if (null == Context.dedicated || 0 == Context.dedicated.value)
            return; // only dedicated servers send heartbeats

        // pgm post3.19 change, cvar pointer not validated before dereferencing
        if (null == ServerMain.public_server || 0 == ServerMain.public_server.value)
            return; // a private dedicated game

        // send to group master
        for (i = 0; i < Defines.MAX_MASTERS; i++)
            if (ServerMain.master_adr[i].port != 0) {
                if (i > 0)
                    Command.Printf("Sending heartbeat to "
                            + Network.AdrToString(ServerMain.master_adr[i]) + "\n");
                Netchan.OutOfBandPrint(Defines.NS_SERVER,
                        ServerMain.master_adr[i], "shutdown");
            }
    }
    

    /**
     * Pull specific info from a newly changed userinfo string into a more C
     * freindly form.
     */
    public static void SV_UserinfoChanged(TClient cl) {
        String val;
        int i;

        // call prog code to allow overrides
        PlayerClient.ClientUserinfoChanged(cl.edict, cl.userinfo);

        // name for C code
        cl.name = Info.Info_ValueForKey(cl.userinfo, "name");

        // mask off high bit
        //TODO: masking for german umlaute
        //for (i=0 ; i<sizeof(cl.name) ; i++)
        //	cl.name[i] &= 127;

        // rate command
        val = Info.Info_ValueForKey(cl.userinfo, "rate");
        if (val.length() > 0) {
            i = Lib.atoi(val);
            cl.rate = i;
            if (cl.rate < 100)
                cl.rate = 100;
            if (cl.rate > 15000)
                cl.rate = 15000;
        } else
            cl.rate = 5000;

        // msg command
        val = Info.Info_ValueForKey(cl.userinfo, "msg");
        if (val.length() > 0) {
            cl.messagelevel = Lib.atoi(val);
        }

    }

    /**
     * Only called at quake2.exe startup, not for each game
     */
    public static void SV_Init() {
        ServerCommands.registerOperatorCommands(); //ok.

        ServerMain.rcon_password = ConsoleVar.Get("rcon_password", "", 0);
        ConsoleVar.Get("skill", "1", 0);
        ConsoleVar.Get("deathmatch", "0", TVar.CVAR_FLAG_LATCH);
        ConsoleVar.Get("coop", "0", TVar.CVAR_FLAG_LATCH);
        ConsoleVar.Get("dmflags", "" + Defines.DF_INSTANT_ITEMS, TVar.CVAR_FLAG_SERVERINFO);
        ConsoleVar.Get("fraglimit", "0", TVar.CVAR_FLAG_SERVERINFO);
        ConsoleVar.Get("timelimit", "0", TVar.CVAR_FLAG_SERVERINFO);
        ConsoleVar.Get("cheats", "0", TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_LATCH);
        ConsoleVar.Get("protocol", "" + Defines.PROTOCOL_VERSION, TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_NOSET);

        ServerMain.maxclients = ConsoleVar.Get("maxclients", "1", TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_LATCH);
        ServerMain.hostname = ConsoleVar.Get("hostname", "noname",TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_ARCHIVE);
        ServerMain.timeout = ConsoleVar.Get("timeout", "125", 0);
        ServerMain.zombietime = ConsoleVar.Get("zombietime", "2", 0);
        ServerMain.sv_showclamp = ConsoleVar.Get("showclamp", "0", 0);
        ServerMain.sv_paused = ConsoleVar.Get("paused", "0", 0);
        ServerMain.sv_timedemo = ConsoleVar.Get("timedemo", "0", 0);
        ServerMain.sv_enforcetime = ConsoleVar.Get("sv_enforcetime", "0", 0);

        ServerMain.allow_download = ConsoleVar.Get("allow_download", "1", TVar.CVAR_FLAG_ARCHIVE);
        ServerMain.allow_download_players = ConsoleVar.Get("allow_download_players","0", TVar.CVAR_FLAG_ARCHIVE);
        ServerMain.allow_download_models = ConsoleVar.Get("allow_download_models", "1", TVar.CVAR_FLAG_ARCHIVE);
        ServerMain.allow_download_sounds = ConsoleVar.Get("allow_download_sounds", "1", TVar.CVAR_FLAG_ARCHIVE);
        ServerMain.allow_download_maps = ConsoleVar.Get("allow_download_maps", "1", TVar.CVAR_FLAG_ARCHIVE);

        ServerMain.sv_noreload = ConsoleVar.Get("sv_noreload", "0", 0);
        ServerMain.sv_airaccelerate = ConsoleVar.Get("sv_airaccelerate", "0", TVar.CVAR_FLAG_LATCH);
        ServerMain.public_server = ConsoleVar.Get("public", "0", 0);
        ServerMain.sv_reconnect_limit = ConsoleVar.Get("sv_reconnect_limit", "3", TVar.CVAR_FLAG_ARCHIVE);

        Context.net_message.init(Context.net_message_buffer, Context.net_message_buffer.length);
    }

    /**
     * Used by SV_Shutdown to send a final message to all connected clients
     * before the server goes down. The messages are sent immediately, not just
     * stuck on the outgoing message list, because the server is going to
     * totally exit after returning from this function.
     */
    public static void SV_FinalMessage(String message, boolean reconnect) {
        int i;
        TClient cl;

        Context.net_message.clear();
        Context.net_message.writeByte(Defines.svc_print);
        Context.net_message.writeByte(Defines.PRINT_HIGH);
        Context.net_message.writeString(message);

        Context.net_message.writeByte(reconnect ? Defines.svc_reconnect : Defines.svc_disconnect);

        // send it twice
        // stagger the packets to crutch operating system limited buffers

        for (i = 0; i < ServerInit.svs.clients.length; i++) {
            cl = ServerInit.svs.clients[i];
            if (cl.state >= Defines.cs_connected)
                Netchan.Transmit(cl.netchan, Context.net_message.cursize,
                        Context.net_message.data);
        }
        for (i = 0; i < ServerInit.svs.clients.length; i++) {
            cl = ServerInit.svs.clients[i];
            if (cl.state >= Defines.cs_connected)
                Netchan.Transmit(cl.netchan, Context.net_message.cursize,
                        Context.net_message.data);
        }
    }

    /**
     * Called when each game quits, before Sys_Quit or Sys_Error.
     */
    public static void SV_Shutdown(String finalmsg, boolean reconnect) {
        if (ServerInit.svs.clients != null)
            SV_FinalMessage(finalmsg, reconnect);

        Master_Shutdown();

        ServerGame.SV_ShutdownGameProgs();

        // free current level
        if (ServerInit.sv.demofile != null)
            try {
                ServerInit.sv.demofile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        ServerInit.sv = new TServer();

        Context.server_state = ServerInit.sv.state;

        if (ServerInit.svs.demofile != null)
            try {
                ServerInit.svs.demofile.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        ServerInit.svs = new TServerStatic();
    }
}