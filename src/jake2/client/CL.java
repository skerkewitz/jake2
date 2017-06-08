/*
 * CL.java
 * Copyright (C) 2004
 * 
 * $Id: CL.java,v 1.30 2007-05-14 23:38:15 cawe Exp $
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
import jake2.client.ui.Menu;
import jake2.game.*;
import jake2.io.FileSystem;
import jake2.network.Netchan;
import jake2.network.TNetAddr;
import jake2.qcommon.*;
import jake2.server.ServerMain;
import jake2.sound.Sound;
import jake2.sys.*;
import jake2.util.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CL
 */
public final class CL {
    
    static int precache_check; // for autodownload of precache items

    static int precache_spawncount;

    static int precache_tex;

    static int precache_model_skin;

    static byte precache_model[]; // used for skin checking in alias models

    public static final int PLAYER_MULT = 5;

    public static class cheatvar_t {
        String name;

        String value;

        TVar var;
    }

    public static String cheatvarsinfo[][] = { { "timescale", "1" },
            { "timedemo", "0" }, { "r_drawworld", "1" },
            { "cl_testlights", "0" }, { "r_fullbright", "0" },
            { "r_drawflat", "0" }, { "paused", "0" }, { "fixedtime", "0" },
            { "sw_draworder", "0" }, { "gl_lightmap", "0" },
            { "gl_saturatelighting", "0" }, { null, null } };

    public static cheatvar_t cheatvars[];

    static {
        cheatvars = new cheatvar_t[cheatvarsinfo.length];
        for (int n = 0; n < cheatvarsinfo.length; n++) {
            cheatvars[n] = new cheatvar_t();
            cheatvars[n].name = cheatvarsinfo[n][0];
            cheatvars[n].value = cheatvarsinfo[n][1];
        }
    }

    static int numcheatvars;

    /**
     * Stop_f
     * 
     * Stop recording a demo.
     */
    static TXCommand Stop_f = new TXCommand() {
        public void execute() {
            try {

                int len;

                if (!Context.cls.getDemorecording()) {
                    Command.Printf("Not recording a demo.\n");
                    return;
                }

                //	   finish up
                len = -1;
                Context.cls.getDemofile().writeInt(EndianHandler.swapInt(len));
                Context.cls.getDemofile().close();
                Context.cls.setDemofile(null);
                Context.cls.setDemorecording(false);
                Command.Printf("Stopped demo.\n");
            } catch (IOException e) {
            }
        }
    };

    static TEntityState nullstate = new TEntityState(null);

    /**
     * Record_f
     * 
     * record &lt;demoname&gt;
     * Begins recording a demo from the current position.
     */
    static TXCommand Record_f = new TXCommand() {
        public void execute() {
            try {
                String name;
                byte buf_data[] = new byte[Defines.MAX_MSGLEN];
                TSizeBuffer buf = new TSizeBuffer();
                int i;
                TEntityState ent;

                if (Cmd.Argc() != 2) {
                    Command.Printf("record <demoname>\n");
                    return;
                }

                if (Context.cls.getDemorecording()) {
                    Command.Printf("Already recording.\n");
                    return;
                }

                if (Context.cls.getState() != Defines.ca_active) {
                    Command.Printf("You must be in a level to record.\n");
                    return;
                }

                //
                // open the demo file
                //
                name = FileSystem.gamedir() + "/demos/" + Cmd.Argv(1) + ".dm2";

                Command.Printf("recording to " + name + ".\n");
                FileSystem.CreatePath(name);
                Context.cls.setDemofile(new RandomAccessFile(name, "rw"));
                if (Context.cls.getDemofile() == null) {
                    Command.Printf("ERROR: couldn't open.\n");
                    return;
                }
                Context.cls.setDemorecording(true);

                // don't start saving messages until a non-delta compressed
                // message is received
                Context.cls.setDemowaiting(true);

                //
                // write out messages to hold the startup information
                //
                buf.init(buf_data, Defines.MAX_MSGLEN);

                // send the serverdata
                buf.writeByte(Defines.svc_serverdata);
                buf.writeInt(Defines.PROTOCOL_VERSION);
                buf.writeInt(0x10000 + Context.cl.servercount);
                buf.writeByte(1); // demos are always attract loops
                buf.writeString(Context.cl.gamedir);
                buf.writeShort(Context.cl.playernum);

                buf.writeString(Context.cl.configstrings[Defines.CS_NAME]);

                // configstrings
                for (i = 0; i < Defines.MAX_CONFIGSTRINGS; i++) {
                    if (Context.cl.configstrings[i].length() > 0) {
                        if (buf.cursize + Context.cl.configstrings[i].length()
                                + 32 > buf.maxsize) { 
                            // write it out
                            Context.cls.getDemofile().writeInt(EndianHandler.swapInt(buf.cursize));
                            Context.cls.getDemofile()
                                    .write(buf.data, 0, buf.cursize);
                            buf.cursize = 0;
                        }

                        buf.writeByte(Defines.svc_configstring);
                        buf.writeShort(i);
                        buf.writeString(Context.cl.configstrings[i]);
                    }

                }

                // baselines
                nullstate.clear();
                for (i = 0; i < Defines.MAX_EDICTS; i++) {
                    ent = Context.cl_entities[i].baseline;
                    if (ent.modelIndex == 0)
                        continue;

                    if (buf.cursize + 64 > buf.maxsize) { // write it out
                        Context.cls.getDemofile().writeInt(EndianHandler.swapInt(buf.cursize));
                        Context.cls.getDemofile().write(buf.data, 0, buf.cursize);
                        buf.cursize = 0;
                    }

                    buf.writeByte(Defines.svc_spawnbaseline);
                    buf.writeDeltaEntity(nullstate, Context.cl_entities[i].baseline, true, true);
                }

                buf.writeByte(Defines.svc_stufftext);
                buf.writeString("precache\n");

                // write it to the demo file
                Context.cls.getDemofile().writeInt(EndianHandler.swapInt(buf.cursize));
                Context.cls.getDemofile().write(buf.data, 0, buf.cursize);
                // the rest of the demo file will be individual frames

            } catch (IOException e) {
            }
        }
    };

    /**
     * ForwardToServer_f
     */
    static TXCommand ForwardToServer_f = new TXCommand() {
        public void execute() {
            if (Context.cls.getState() != Defines.ca_connected
                    && Context.cls.getState() != Defines.ca_active) {
                Command.Printf("Can't \"" + Cmd.Argv(0) + "\", not connected\n");
                return;
            }

            // don't forward the first argument
            if (Cmd.Argc() > 1) {
                Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
                Context.cls.getNetchan().message.print(Cmd.Args());
            }
        }
    };

    /**
     * Pause_f
     */
    static TXCommand Pause_f = new TXCommand() {
        public void execute() {
            // never pause in multiplayer

            if (ConsoleVar.VariableValue("maxclients") > 1
                    || Context.server_state == 0) {
                ConsoleVar.SetValue("paused", 0);
                return;
            }

            ConsoleVar.SetValue("paused", Context.cl_paused.value);
        }
    };

    /**
     * Quit_f
     */
    public static TXCommand Quit_f = () -> {
        Disconnect();
        Command.Quit();
    };

    /**
     * Connect_f
     */
    static TXCommand Connect_f = new TXCommand() {
        public void execute() {
            String server;

            if (Cmd.Argc() != 2) {
                Command.Printf("usage: connect <server>\n");
                return;
            }

            if (Context.server_state != 0) {
                // if running a local server, kill it and reissue
                ServerMain.SV_Shutdown("Server quit\n", false);
            } else {
                Disconnect();
            }

            server = Cmd.Argv(1);

            Network.Config(true); // allow remote

            Disconnect();

            Context.cls.setState(Defines.ca_connecting);
            //strncpy (cls.servername, server, sizeof(cls.servername)-1);
            Context.cls.setServername(server);
            Context.cls.setConnectTime(-99999);
            // CL_CheckForResend() will fire immediately
        }
    };

    /**
     * Rcon_f
     * 
     * Send the rest of the command line over as an unconnected command.
     */
    static TXCommand Rcon_f = new TXCommand() {
        public void execute() {

            if (Context.rcon_client_password.string.length() == 0) {
                Command.Printf("You must set 'rcon_password' before\nissuing an rcon command.\n");
                return;
            }

            StringBuffer message = new StringBuffer(1024);

            // connection less packet
            message.append('\u00ff');
            message.append('\u00ff');
            message.append('\u00ff');
            message.append('\u00ff');

            // allow remote
            Network.Config(true);

            message.append("rcon ");
            message.append(Context.rcon_client_password.string);
            message.append(" ");

            for (int i = 1; i < Cmd.Argc(); i++) {
                message.append(Cmd.Argv(i));
                message.append(" ");
            }

            TNetAddr to = new TNetAddr();

            if (Context.cls.getState() >= Defines.ca_connected)
                to = Context.cls.getNetchan().remote_address;
            else {
                if (Context.rcon_address.string.length() == 0) {
                    Command.Printf("You must either be connected,\nor set the 'rcon_address' cvar\nto issue rcon commands\n");
                    return;
                }
                Network.StringToAdr(Context.rcon_address.string, to);
                if (to.port == 0) to.port = Defines.PORT_SERVER;
            }
            message.append('\0');
            String b = message.toString();
            Network.SendPacket(Defines.NS_CLIENT, b.length(), Lib.stringToBytes(b), to);
        }
    };

    static TXCommand Disconnect_f = new TXCommand() {
        public void execute() {
            Command.Error(Defines.ERR_DROP, "Disconnected from server");
        }
    };

    /**
     * Changing_f
     * 
     * Just sent as a hint to the client that they should drop to full console.
     */
    static TXCommand Changing_f = new TXCommand() {
        public void execute() {
            //ZOID
            //if we are downloading, we don't change!
            // This so we don't suddenly stop downloading a map

            if (Context.cls.getDownload() != null)
                return;

            SCR.BeginLoadingPlaque();
            Context.cls.setState(Defines.ca_connected); // not active anymore, but
                                                      // not disconnected
            Command.Printf("\nChanging map...\n");
        }
    };

    /**
     * Reconnect_f
     * 
     * The server is changing levels.
     */
    static TXCommand Reconnect_f = new TXCommand() {
        public void execute() {
            //ZOID
            //if we are downloading, we don't change! This so we don't suddenly
            // stop downloading a map
            if (Context.cls.getDownload() != null)
                return;

            Sound.StopAllSounds();
            if (Context.cls.getState() == Defines.ca_connected) {
                Command.Printf("reconnecting...\n");
                Context.cls.setState(Defines.ca_connected);
                Context.cls.getNetchan().message.writeChar(Defines.clc_stringcmd);
                Context.cls.getNetchan().message.writeString("new");
                return;
            }

            if (Context.cls.getServername() != null) {
                if (Context.cls.getState() >= Defines.ca_connected) {
                    Disconnect();
                    Context.cls.setConnectTime(Context.cls.getRealtime() - 1500);
                } else
                    Context.cls.setConnectTime(-99999); // fire immediately

                Context.cls.setState(Defines.ca_connecting);
                Command.Printf("reconnecting...\n");
            }
        }
    };

    /**
     * PingServers_f
     */
    public static TXCommand PingServers_f = () -> {
        int i;
        TNetAddr adr = new TNetAddr();
        //char name[32];
        String name;
        String adrstring;
        TVar noudp;
        TVar noipx;

        Network.Config(true); // allow remote

        // send a broadcast packet
        Command.Printf("pinging broadcast...\n");

        noudp = ConsoleVar.Get("noudp", "0", TVar.CVAR_FLAG_NOSET);
        if (noudp.value == 0.0f) {
            adr.type = Defines.NA_BROADCAST;
            adr.port = Defines.PORT_SERVER;
            //adr.port = BigShort(PORT_SERVER);
            Netchan.OutOfBandPrint(Defines.NS_CLIENT, adr, "info "
                    + Defines.PROTOCOL_VERSION);
        }

        // we use no IPX
        noipx = ConsoleVar.Get("noipx", "1", TVar.CVAR_FLAG_NOSET);
        if (noipx.value == 0.0f) {
            adr.type = Defines.NA_BROADCAST_IPX;
            //adr.port = BigShort(PORT_SERVER);
            adr.port = Defines.PORT_SERVER;
            Netchan.OutOfBandPrint(Defines.NS_CLIENT, adr, "info "
                    + Defines.PROTOCOL_VERSION);
        }

        // send a packet to each address book entry
        for (i = 0; i < 16; i++) {
            //Com_sprintf (name, sizeof(name), "adr%i", i);
            name = "adr" + i;
            adrstring = ConsoleVar.VariableString(name);
            if (adrstring == null || adrstring.length() == 0)
                continue;

            Command.Printf("pinging " + adrstring + "...\n");
            if (!Network.StringToAdr(adrstring, adr)) {
                Command.Printf("Bad address: " + adrstring + "\n");
                continue;
            }
            if (adr.port == 0)
                //adr.port = BigShort(PORT_SERVER);
                adr.port = Defines.PORT_SERVER;
            Netchan.OutOfBandPrint(Defines.NS_CLIENT, adr, "info "
                    + Defines.PROTOCOL_VERSION);
        }
    };

    /**
     * Skins_f
     * 
     * Load or download any custom player skins and models.
     */
    static TXCommand Skins_f = new TXCommand() {
        public void execute() {
            int i;

            for (i = 0; i < Defines.MAX_CLIENTS; i++) {
                if (Context.cl.configstrings[Defines.CS_PLAYERSKINS + i] == null)
                    continue;
                Command.Printf("client " + i + ": "
                        + Context.cl.configstrings[Defines.CS_PLAYERSKINS + i]
                        + "\n");
                SCR.UpdateScreen();
                Key.SendKeyEvents(); // pump message loop
                CL_parse.ParseClientinfo(i);
            }
        }
    };

    /**
     * Userinfo_f
     */
    public static TXCommand Userinfo_f = () -> {
        Command.Printf("User info settings:\n");
        Info.Print(ConsoleVar.Userinfo());
    };

    /**
     * Snd_Restart_f
     * 
     * Restart the sound subsystem so it can pick up new parameters and flush
     * all sounds.
     */
    public static TXCommand Snd_Restart_f = () -> {
        Sound.Shutdown();
        Sound.Init();
        CL_parse.RegisterSounds();
    };

    //	   ENV_CNT is map load, ENV_CNT+1 is first env map
    public static final int ENV_CNT = (Defines.CS_PLAYERSKINS + Defines.MAX_CLIENTS
            * CL.PLAYER_MULT);

    public static final int TEXTURE_CNT = (ENV_CNT + 13);

    static String env_suf[] = { "rt", "bk", "lf", "ft", "up", "dn" };

    /**
     * The server will send this command right before allowing the client into
     * the server.
     */
    static TXCommand Precache_f = new TXCommand() {
        public void execute() {
            // Yet another hack to let old demos work the old precache sequence.
            if (Cmd.Argc() < 2) {

                int iw[] = { 0 }; // for detecting cheater maps

                CM.CM_LoadMap(Context.cl.configstrings[Defines.CS_MODELS + 1],
                        true, iw);

                CL_parse.RegisterSounds();
                ClientView.PrepRefresh();
                return;
            }

            CL.precache_check = Defines.CS_MODELS;
            CL.precache_spawncount = Lib.atoi(Cmd.Argv(1));
            CL.precache_model = null;
            CL.precache_model_skin = 0;

            RequestNextDownload();
        }
    };

    private static int extratime;

    //	  ============================================================================

    /**
     * Shutdown
     * 
     * FIXME: this is a callback from Sys_Quit and Com_Error. It would be better
     * to run quit through here before the final handoff to the sys code.
     */
    static boolean isdown = false;

    /**
     * WriteDemoMessage
     * 
     * Dumps the current net message, prefixed by the length
     */
    static void WriteDemoMessage() {
        int swlen;

        // the first eight bytes are just packet sequencing stuff
        swlen = Context.net_message.cursize - 8;

        try {
            Context.cls.getDemofile().writeInt(EndianHandler.swapInt(swlen));
            Context.cls.getDemofile().write(Context.net_message.data, 8, swlen);
        } catch (IOException e) {
        }

    }

    /**
     * SendConnectPacket
     * 
     * We have gotten a challenge from the server, so try and connect.
     */
    static void SendConnectPacket() {
        TNetAddr adr = new TNetAddr();
        int port;

        if (!Network.StringToAdr(Context.cls.getServername(), adr)) {
            Command.Printf("Bad server address\n");
            Context.cls.setConnectTime(0);
            return;
        }
        if (adr.port == 0)
            adr.port = Defines.PORT_SERVER;
        //			adr.port = BigShort(PORT_SERVER);

        port = (int) ConsoleVar.VariableValue("qport");
        Context.userinfo_modified = false;

        Netchan.OutOfBandPrint(Defines.NS_CLIENT, adr, "connect "
                + Defines.PROTOCOL_VERSION + " " + port + " "
                + Context.cls.getChallenge() + " \"" + ConsoleVar.Userinfo() + "\"\n");
    }

    /**
     * CheckForResend
     * 
     * Resend a connect message if the last one has timed out.
     */
    static void CheckForResend() {
        // if the local server is running and we aren't
        // then connect
        if (Context.cls.getState() == Defines.ca_disconnected
                && Context.server_state != 0) {
            Context.cls.setState(Defines.ca_connecting);
            Context.cls.setServername("localhost");
            // we don't need a challenge on the localhost
            SendConnectPacket();
            return;
        }

        // resend if we haven't gotten a reply yet
        if (Context.cls.getState() != Defines.ca_connecting)
            return;

        if (Context.cls.getRealtime() - Context.cls.getConnectTime() < 3000)
            return;

        TNetAddr adr = new TNetAddr();
        if (!Network.StringToAdr(Context.cls.getServername(), adr)) {
            Command.Printf("Bad server address\n");
            Context.cls.setState(Defines.ca_disconnected);
            return;
        }
        if (adr.port == 0)
            adr.port = Defines.PORT_SERVER;

        // for retransmit requests
        Context.cls.setConnectTime(Context.cls.getRealtime());

        Command.Printf("Connecting to " + Context.cls.getServername() + "...\n");

        Netchan.OutOfBandPrint(Defines.NS_CLIENT, adr, "getchallenge\n");
    }

    /**
     * ClearState
     * 
     */
    static void ClearState() {
        Sound.StopAllSounds();
        CLEffects.ClearEffects();
        CL_tent.ClearTEnts();

        // wipe the entire cl structure

        Context.cl = new TClientState();
        for (int i = 0; i < Context.cl_entities.length; i++) {
            Context.cl_entities[i] = new TClEentity();
        }

        Context.cls.getNetchan().message.clear();
    }

    /**
     * Disconnect
     * 
     * Goes from a connected state to full screen console state Sends a
     * disconnect message to the server This is also called on Com_Error, so it
     * shouldn't cause any errors.
     */
    static void Disconnect() {

        String fin;

        if (Context.cls.getState() == Defines.ca_disconnected)
            return;

        if (Context.cl_timedemo != null && Context.cl_timedemo.value != 0.0f) {
            int time;

            time = Timer.Milliseconds() - Context.cl.timedemo_start;
            if (time > 0)
                Command.Printf("%i frames, %3.1f seconds: %3.1f fps\n",
                        Context.cl.timedemo_frames, time / 1000.0,  Context.cl.timedemo_frames * 1000.0 / time);
        }

        Math3D.VectorClear(Context.cl.refdef.blend);
        
        Context.re.CinematicSetPalette(null);

        Menu.forceMenuOff();

        Context.cls.setConnectTime(0);

        SCR.StopCinematic();

        if (Context.cls.getDemorecording())
            Stop_f.execute();

        // send a disconnect message to the server
        fin = (char) Defines.clc_stringcmd + "disconnect";
        Netchan.Transmit(Context.cls.getNetchan(), fin.length(), Lib.stringToBytes(fin));
        Netchan.Transmit(Context.cls.getNetchan(), fin.length(), Lib.stringToBytes(fin));
        Netchan.Transmit(Context.cls.getNetchan(), fin.length(), Lib.stringToBytes(fin));

        ClearState();

        // stop download
        if (Context.cls.getDownload() != null) {
            Lib.fclose(Context.cls.getDownload());
            Context.cls.setDownload(null);
        }

        Context.cls.setState(Defines.ca_disconnected);
    }

    /**
     * ParseStatusMessage
     * 
     * Handle a reply from a ping.
     */
    static void ParseStatusMessage() {
        String s;

        s = TSizeBuffer.ReadString(Context.net_message);

        Command.Printf(s + "\n");
        Menu.AddToServerList(Context.net_from, s);
    }

    /**
     * ConnectionlessPacket
     * 
     * Responses to broadcasts, etc
     */
    static void ConnectionlessPacket() {
        String s;
        String c;

        TSizeBuffer.BeginReading(Context.net_message);
        TSizeBuffer.ReadLong(Context.net_message); // skip the -1

        s = TSizeBuffer.ReadStringLine(Context.net_message);

        Cmd.TokenizeString(s.toCharArray(), false);

        c = Cmd.Argv(0);
        
        Command.Println(Context.net_from.toString() + ": " + c);

        // server connection
        if (c.equals("client_connect")) {
            if (Context.cls.getState() == Defines.ca_connected) {
                Command.Printf("Dup connect received.  Ignored.\n");
                return;
            }
            Netchan.Setup(Defines.NS_CLIENT, Context.cls.getNetchan(),
                    Context.net_from, Context.cls.getQuakePort());
            Context.cls.getNetchan().message.writeChar(Defines.clc_stringcmd);
            Context.cls.getNetchan().message.writeString("new");
            Context.cls.setState(Defines.ca_connected);
            return;
        }

        // server responding to a status broadcast
        if (c.equals("info")) {
            ParseStatusMessage();
            return;
        }

        // remote command from gui front end
        if (c.equals("cmd")) {
            if (!Network.IsLocalAddress(Context.net_from)) {
                Command.Printf("Command packet from remote host.  Ignored.\n");
                return;
            }
            s = TSizeBuffer.ReadString(Context.net_message);
            Cbuf.AddText(s);
            Cbuf.AddText("\n");
            return;
        }
        // print command from somewhere
        if (c.equals("print")) {
            s = TSizeBuffer.ReadString(Context.net_message);
            if (s.length() > 0)
            	Command.Printf(s);
            return;
        }

        // ping from somewhere
        if (c.equals("ping")) {
            Netchan.OutOfBandPrint(Defines.NS_CLIENT, Context.net_from, "ack");
            return;
        }

        // challenge from the server we are connecting to
        if (c.equals("challenge")) {
            Context.cls.setChallenge(Lib.atoi(Cmd.Argv(1)));
            SendConnectPacket();
            return;
        }

        // echo request from server
        if (c.equals("echo")) {
            Netchan.OutOfBandPrint(Defines.NS_CLIENT, Context.net_from, Cmd
                    .Argv(1));
            return;
        }

        Command.Printf("Unknown command.\n");
    }


    /**
     * ReadPackets
     */
    static void ReadPackets() {
        while (Network.GetPacket(Defines.NS_CLIENT, Context.net_from,
                Context.net_message)) {

            //
            // remote command packet
            //		
            if (Context.net_message.data[0] == -1
                    && Context.net_message.data[1] == -1
                    && Context.net_message.data[2] == -1
                    && Context.net_message.data[3] == -1) {
                //			if (*(int *)net_message.data == -1)
                ConnectionlessPacket();
                continue;
            }

            if (Context.cls.getState() == Defines.ca_disconnected
                    || Context.cls.getState() == Defines.ca_connecting)
                continue; // dump it if not connected

            if (Context.net_message.cursize < 8) {
                Command.Printf(Network.AdrToString(Context.net_from)
                        + ": Runt packet\n");
                continue;
            }

            //
            // packet from server
            //
            if (!Network.CompareAdr(Context.net_from,
                    Context.cls.getNetchan().remote_address)) {
                Command.DPrintf(Network.AdrToString(Context.net_from)
                        + ":sequenced packet without connection\n");
                continue;
            }
            if (!Netchan.Process(Context.cls.getNetchan(), Context.net_message))
                continue; // wasn't accepted for some reason
            CL_parse.ParseServerMessage();
        }

        //
        // check timeout
        //
        if (Context.cls.getState() >= Defines.ca_connected
                && Context.cls.getRealtime() - Context.cls.getNetchan().last_received > Context.cl_timeout.value * 1000) {
            if (++Context.cl.timeoutcount > 5) // timeoutcount saves debugger
            {
                Command.Printf("\nServer connection timed out.\n");
                Disconnect();
                return;
            }
        } else
            Context.cl.timeoutcount = 0;
    }

    //	  =============================================================================

    /**
     * FixUpGender_f
     */
    static void FixUpGender() {

        String sk;

        if (Context.gender_auto.value != 0.0f) {

            if (Context.gender.modified) {
                // was set directly, don't override the user
                Context.gender.modified = false;
                return;
            }

            sk = Context.skin.string;
            if (sk.startsWith("male") || sk.startsWith("cyborg"))
                ConsoleVar.Set("gender", "male");
            else if (sk.startsWith("female") || sk.startsWith("crackhor"))
                ConsoleVar.Set("gender", "female");
            else
                ConsoleVar.Set("gender", "none");
            Context.gender.modified = false;
        }
    }

    public static void RequestNextDownload() {
        int map_checksum = 0; // for detecting cheater maps
        //char fn[MAX_OSPATH];
        String fn;

        qfiles.dmdl_t pheader;

        if (Context.cls.getState() != Defines.ca_connected)
            return;

        if (ServerMain.allow_download.value == 0 && CL.precache_check < ENV_CNT)
            CL.precache_check = ENV_CNT;

        //	  ZOID
        if (CL.precache_check == Defines.CS_MODELS) { // confirm map
            CL.precache_check = Defines.CS_MODELS + 2; // 0 isn't used
            if (ServerMain.allow_download_maps.value != 0)
                if (!CL_parse
                        .CheckOrDownloadFile(Context.cl.configstrings[Defines.CS_MODELS + 1]))
                    return; // started a download
        }
        if (CL.precache_check >= Defines.CS_MODELS
                && CL.precache_check < Defines.CS_MODELS + Defines.MAX_MODELS) {
            if (ServerMain.allow_download_models.value != 0) {
                while (CL.precache_check < Defines.CS_MODELS
                        + Defines.MAX_MODELS
                        && Context.cl.configstrings[CL.precache_check].length() > 0) {
                    if (Context.cl.configstrings[CL.precache_check].charAt(0) == '*'
                            || Context.cl.configstrings[CL.precache_check]
                                    .charAt(0) == '#') {
                        CL.precache_check++;
                        continue;
                    }
                    if (CL.precache_model_skin == 0) {
                        if (!CL_parse
                                .CheckOrDownloadFile(Context.cl.configstrings[CL.precache_check])) {
                            CL.precache_model_skin = 1;
                            return; // started a download
                        }
                        CL.precache_model_skin = 1;
                    }

                    // checking for skins in the model
                    if (CL.precache_model == null) {

                        CL.precache_model = FileSystem
                                .loadFile(Context.cl.configstrings[CL.precache_check]);
                        if (CL.precache_model == null) {
                            CL.precache_model_skin = 0;
                            CL.precache_check++;
                            continue; // couldn't load it
                        }
                        ByteBuffer bb = ByteBuffer.wrap(CL.precache_model);
                        bb.order(ByteOrder.LITTLE_ENDIAN);

                        int header = bb.getInt();

                        if (header != qfiles.IDALIASHEADER) {
                            // not an alias model
                            FileSystem.FreeFile(CL.precache_model);
                            CL.precache_model = null;
                            CL.precache_model_skin = 0;
                            CL.precache_check++;
                            continue;
                        }
                        pheader = new qfiles.dmdl_t(ByteBuffer.wrap(
                                CL.precache_model).order(
                                ByteOrder.LITTLE_ENDIAN));
                        if (pheader.version != Defines.ALIAS_VERSION) {
                            CL.precache_check++;
                            CL.precache_model_skin = 0;
                            continue; // couldn't load it
                        }
                    }

                    pheader = new qfiles.dmdl_t(ByteBuffer.wrap(
                            CL.precache_model).order(ByteOrder.LITTLE_ENDIAN));

                    int num_skins = pheader.num_skins;

                    while (CL.precache_model_skin - 1 < num_skins) {
                        //Command.Printf("critical code section because of endian
                        // mess!\n");

                        String name = Lib.CtoJava(CL.precache_model,
                                pheader.ofs_skins
                                        + (CL.precache_model_skin - 1)
                                        * Defines.MAX_SKINNAME,
                                Defines.MAX_SKINNAME * num_skins);

                        if (!CL_parse.CheckOrDownloadFile(name)) {
                            CL.precache_model_skin++;
                            return; // started a download
                        }
                        CL.precache_model_skin++;
                    }
                    if (CL.precache_model != null) {
                        FileSystem.FreeFile(CL.precache_model);
                        CL.precache_model = null;
                    }
                    CL.precache_model_skin = 0;
                    CL.precache_check++;
                }
            }
            CL.precache_check = Defines.CS_SOUNDS;
        }
        if (CL.precache_check >= Defines.CS_SOUNDS
                && CL.precache_check < Defines.CS_SOUNDS + Defines.MAX_SOUNDS) {
            if (ServerMain.allow_download_sounds.value != 0) {
                if (CL.precache_check == Defines.CS_SOUNDS)
                    CL.precache_check++; // zero is blank
                while (CL.precache_check < Defines.CS_SOUNDS
                        + Defines.MAX_SOUNDS
                        && Context.cl.configstrings[CL.precache_check].length() > 0) {
                    if (Context.cl.configstrings[CL.precache_check].charAt(0) == '*') {
                        CL.precache_check++;
                        continue;
                    }
                    fn = "sound/"
                            + Context.cl.configstrings[CL.precache_check++];
                    if (!CL_parse.CheckOrDownloadFile(fn))
                        return; // started a download
                }
            }
            CL.precache_check = Defines.CS_IMAGES;
        }
        if (CL.precache_check >= Defines.CS_IMAGES
                && CL.precache_check < Defines.CS_IMAGES + Defines.MAX_IMAGES) {
            if (CL.precache_check == Defines.CS_IMAGES)
                CL.precache_check++; // zero is blank

            while (CL.precache_check < Defines.CS_IMAGES + Defines.MAX_IMAGES
                    && Context.cl.configstrings[CL.precache_check].length() > 0) {
                fn = "pics/" + Context.cl.configstrings[CL.precache_check++]
                        + ".pcx";
                if (!CL_parse.CheckOrDownloadFile(fn))
                    return; // started a download
            }
            CL.precache_check = Defines.CS_PLAYERSKINS;
        }
        // skins are special, since a player has three things to download:
        // model, weapon model and skin
        // so precache_check is now *3
        if (CL.precache_check >= Defines.CS_PLAYERSKINS
                && CL.precache_check < Defines.CS_PLAYERSKINS
                        + Defines.MAX_CLIENTS * CL.PLAYER_MULT) {
            if (ServerMain.allow_download_players.value != 0) {
                while (CL.precache_check < Defines.CS_PLAYERSKINS
                        + Defines.MAX_CLIENTS * CL.PLAYER_MULT) {

                    int i, n;
                    //char model[MAX_QPATH], skin[MAX_QPATH], * p;
                    String model, skin;

                    i = (CL.precache_check - Defines.CS_PLAYERSKINS)
                            / CL.PLAYER_MULT;
                    n = (CL.precache_check - Defines.CS_PLAYERSKINS)
                            % CL.PLAYER_MULT;

                    if (Context.cl.configstrings[Defines.CS_PLAYERSKINS + i]
                            .length() == 0) {
                        CL.precache_check = Defines.CS_PLAYERSKINS + (i + 1)
                                * CL.PLAYER_MULT;
                        continue;
                    }

                    int pos = Context.cl.configstrings[Defines.CS_PLAYERSKINS + i].indexOf('\\');
                    
                    if (pos != -1)
                        pos++;
                    else
                        pos = 0;

                    int pos2 = Context.cl.configstrings[Defines.CS_PLAYERSKINS + i].indexOf('\\', pos);
                    
                    if (pos2 == -1)
                        pos2 = Context.cl.configstrings[Defines.CS_PLAYERSKINS + i].indexOf('/', pos);
                    
                    
                    model = Context.cl.configstrings[Defines.CS_PLAYERSKINS + i]
                            .substring(pos, pos2);
                                        
                    skin = Context.cl.configstrings[Defines.CS_PLAYERSKINS + i].substring(pos2 + 1);
                    
                    switch (n) {
                    case 0: // model
                        fn = "players/" + model + "/tris.md2";
                        if (!CL_parse.CheckOrDownloadFile(fn)) {
                            CL.precache_check = Defines.CS_PLAYERSKINS + i
                                    * CL.PLAYER_MULT + 1;
                            return; // started a download
                        }
                        n++;
                    /* FALL THROUGH */

                    case 1: // weapon model
                        fn = "players/" + model + "/weapon.md2";
                        if (!CL_parse.CheckOrDownloadFile(fn)) {
                            CL.precache_check = Defines.CS_PLAYERSKINS + i
                                    * CL.PLAYER_MULT + 2;
                            return; // started a download
                        }
                        n++;
                    /* FALL THROUGH */

                    case 2: // weapon skin
                        fn = "players/" + model + "/weapon.pcx";
                        if (!CL_parse.CheckOrDownloadFile(fn)) {
                            CL.precache_check = Defines.CS_PLAYERSKINS + i
                                    * CL.PLAYER_MULT + 3;
                            return; // started a download
                        }
                        n++;
                    /* FALL THROUGH */

                    case 3: // skin
                        fn = "players/" + model + "/" + skin + ".pcx";
                        if (!CL_parse.CheckOrDownloadFile(fn)) {
                            CL.precache_check = Defines.CS_PLAYERSKINS + i
                                    * CL.PLAYER_MULT + 4;
                            return; // started a download
                        }
                        n++;
                    /* FALL THROUGH */

                    case 4: // skin_i
                        fn = "players/" + model + "/" + skin + "_i.pcx";
                        if (!CL_parse.CheckOrDownloadFile(fn)) {
                            CL.precache_check = Defines.CS_PLAYERSKINS + i
                                    * CL.PLAYER_MULT + 5;
                            return; // started a download
                        }
                        // move on to next model
                        CL.precache_check = Defines.CS_PLAYERSKINS + (i + 1)
                                * CL.PLAYER_MULT;
                    }
                }
            }
            // precache phase completed
            CL.precache_check = ENV_CNT;
        }

        if (CL.precache_check == ENV_CNT) {
            CL.precache_check = ENV_CNT + 1;

            int iw[] = { map_checksum };

            CM.CM_LoadMap(Context.cl.configstrings[Defines.CS_MODELS + 1],
                    true, iw);
            map_checksum = iw[0];

            if ((map_checksum ^ Lib
                    .atoi(Context.cl.configstrings[Defines.CS_MAPCHECKSUM])) != 0) {
                Command
                        .Error(
                                Defines.ERR_DROP,
                                "Local map version differs from server: "
                                        + map_checksum
                                        + " != '"
                                        + Context.cl.configstrings[Defines.CS_MAPCHECKSUM]
                                        + "'\n");
                return;
            }
        }

        if (CL.precache_check > ENV_CNT && CL.precache_check < TEXTURE_CNT) {
            if (ServerMain.allow_download.value != 0
                    && ServerMain.allow_download_maps.value != 0) {
                while (CL.precache_check < TEXTURE_CNT) {
                    int n = CL.precache_check++ - ENV_CNT - 1;

                    if ((n & 1) != 0)
                        fn = "env/" + Context.cl.configstrings[Defines.CS_SKY]
                                + env_suf[n / 2] + ".pcx";
                    else
                        fn = "env/" + Context.cl.configstrings[Defines.CS_SKY]
                                + env_suf[n / 2] + ".tga";
                    if (!CL_parse.CheckOrDownloadFile(fn))
                        return; // started a download
                }
            }
            CL.precache_check = TEXTURE_CNT;
        }

        if (CL.precache_check == TEXTURE_CNT) {
            CL.precache_check = TEXTURE_CNT + 1;
            CL.precache_tex = 0;
        }

        // confirm existance of textures, download any that don't exist
        if (CL.precache_check == TEXTURE_CNT + 1) {
            // from qcommon/cmodel.c
            // extern int numtexinfo;
            // extern mapsurface_t map_surfaces[];

            if (ServerMain.allow_download.value != 0
                    && ServerMain.allow_download_maps.value != 0) {
                while (CL.precache_tex < CM.numtexinfo) {
                    //char fn[MAX_OSPATH];

                    fn = "textures/" + CM.map_surfaces[CL.precache_tex++].rname
                            + ".wal";
                    if (!CL_parse.CheckOrDownloadFile(fn))
                        return; // started a download
                }
            }
            CL.precache_check = TEXTURE_CNT + 999;
        }

        //	  ZOID
        CL_parse.RegisterSounds();
        ClientView.PrepRefresh();

        Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
        Context.cls.getNetchan().message.writeString("begin "
                + CL.precache_spawncount + "\n");
    }

    /**
     * InitLocal
     */
    public static void InitLocal() {
        Context.cls.setState(Defines.ca_disconnected);
        Context.cls.setRealtime(Timer.Milliseconds());

        CL_input.InitInput();

        ConsoleVar.Get("adr0", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr1", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr2", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr3", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr4", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr5", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr6", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr7", "", TVar.CVAR_FLAG_ARCHIVE);
        ConsoleVar.Get("adr8", "", TVar.CVAR_FLAG_ARCHIVE);

        //
        // register our variables
        //
        Context.cl_stereo_separation = ConsoleVar.Get("cl_stereo_separation", "0.4",
                TVar.CVAR_FLAG_ARCHIVE);
        Context.cl_stereo = ConsoleVar.Get("cl_stereo", "0", 0);

        Context.cl_add_blend = ConsoleVar.Get("cl_blend", "1", 0);
        Context.cl_add_lights = ConsoleVar.Get("cl_lights", "1", 0);
        Context.cl_add_particles = ConsoleVar.Get("cl_particles", "1", 0);
        Context.cl_add_entities = ConsoleVar.Get("cl_entities", "1", 0);
        Context.cl_gun = ConsoleVar.Get("cl_gun", "1", 0);
        Context.cl_footsteps = ConsoleVar.Get("cl_footsteps", "1", 0);
        Context.cl_noskins = ConsoleVar.Get("cl_noskins", "0", 0);
        Context.cl_autoskins = ConsoleVar.Get("cl_autoskins", "0", 0);
        Context.cl_predict = ConsoleVar.Get("cl_predict", "1", 0);

        Context.cl_maxfps = ConsoleVar.Get("cl_maxfps", "90", 0);

        Context.cl_upspeed = ConsoleVar.Get("cl_upspeed", "200", 0);
        Context.cl_forwardspeed = ConsoleVar.Get("cl_forwardspeed", "200", 0);
        Context.cl_sidespeed = ConsoleVar.Get("cl_sidespeed", "200", 0);
        Context.cl_yawspeed = ConsoleVar.Get("cl_yawspeed", "140", 0);
        Context.cl_pitchspeed = ConsoleVar.Get("cl_pitchspeed", "150", 0);
        Context.cl_anglespeedkey = ConsoleVar.Get("cl_anglespeedkey", "1.5", 0);

        Context.cl_run = ConsoleVar.Get("cl_run", "0", TVar.CVAR_FLAG_ARCHIVE);
        Context.lookspring = ConsoleVar.Get("lookspring", "0", TVar.CVAR_FLAG_ARCHIVE);
        Context.lookstrafe = ConsoleVar.Get("lookstrafe", "0", TVar.CVAR_FLAG_ARCHIVE);
        Context.sensitivity = ConsoleVar
                .Get("sensitivity", "3", TVar.CVAR_FLAG_ARCHIVE);

        Context.m_pitch = ConsoleVar.Get("m_pitch", "0.022", TVar.CVAR_FLAG_ARCHIVE);
        Context.m_yaw = ConsoleVar.Get("m_yaw", "0.022", 0);
        Context.m_forward = ConsoleVar.Get("m_forward", "1", 0);
        Context.m_side = ConsoleVar.Get("m_side", "1", 0);

        Context.cl_shownet = ConsoleVar.Get("cl_shownet", "0", 0);
        Context.cl_showmiss = ConsoleVar.Get("cl_showmiss", "0", 0);
        Context.cl_showclamp = ConsoleVar.Get("showclamp", "0", 0);
        Context.cl_timeout = ConsoleVar.Get("cl_timeout", "120", 0);
        Context.cl_paused = ConsoleVar.Get("paused", "0", 0);
        Context.cl_timedemo = ConsoleVar.Get("timedemo", "0", 0);

        Context.rcon_client_password = ConsoleVar.Get("rcon_password", "", 0);
        Context.rcon_address = ConsoleVar.Get("rcon_address", "", 0);

        Context.cl_lightlevel = ConsoleVar.Get("r_lightlevel", "0", 0);

        //
        // userinfo
        //
        Context.info_password = ConsoleVar.Get("password", "", TVar.CVAR_FLAG_USERINFO);
        Context.info_spectator = ConsoleVar.Get("spectator", "0",
                TVar.CVAR_FLAG_USERINFO);
        Context.name = ConsoleVar.Get("name", "unnamed", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE);
        Context.skin = ConsoleVar.Get("skin", "male/grunt", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE);
        Context.rate = ConsoleVar.Get("rate", "25000", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE); // FIXME
        Context.msg = ConsoleVar.Get("msg", "1", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE);
        Context.hand = ConsoleVar.Get("hand", "0", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE);
        Context.fov = ConsoleVar.Get("fov", "90", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE);
        Context.gender = ConsoleVar.Get("gender", "male", TVar.CVAR_FLAG_USERINFO
                | TVar.CVAR_FLAG_ARCHIVE);
        Context.gender_auto = ConsoleVar
                .Get("gender_auto", "1", TVar.CVAR_FLAG_ARCHIVE);
        Context.gender.modified = false; // clear this so we know when user sets
                                         // it manually

        Context.cl_vwep = ConsoleVar.Get("cl_vwep", "1", TVar.CVAR_FLAG_ARCHIVE);

        //
        // register our commands
        //
        Cmd.AddCommand("cmd", ForwardToServer_f);
        Cmd.AddCommand("pause", Pause_f);
        Cmd.AddCommand("pingservers", PingServers_f);
        Cmd.AddCommand("skins", Skins_f);

        Cmd.AddCommand("userinfo", Userinfo_f);
        Cmd.AddCommand("snd_restart", Snd_Restart_f);

        Cmd.AddCommand("changing", Changing_f);
        Cmd.AddCommand("disconnect", Disconnect_f);
        Cmd.AddCommand("record", Record_f);
        Cmd.AddCommand("stop", Stop_f);

        Cmd.AddCommand("quit", Quit_f);

        Cmd.AddCommand("connect", Connect_f);
        Cmd.AddCommand("reconnect", Reconnect_f);

        Cmd.AddCommand("rcon", Rcon_f);

        Cmd.AddCommand("precache", Precache_f);

        Cmd.AddCommand("download", CL_parse.Download_f);

        //
        // forward to server commands
        //
        // the only thing this does is allow command completion
        // to work -- all unknown commands are automatically
        // forwarded to the server
        Cmd.AddCommand("wave", null);
        Cmd.AddCommand("inven", null);
        Cmd.AddCommand("kill", null);
        Cmd.AddCommand("use", null);
        Cmd.AddCommand("drop", null);
        Cmd.AddCommand("say", null);
        Cmd.AddCommand("say_team", null);
        Cmd.AddCommand("info", null);
        Cmd.AddCommand("prog", null);
        Cmd.AddCommand("give", null);
        Cmd.AddCommand("god", null);
        Cmd.AddCommand("notarget", null);
        Cmd.AddCommand("noclip", null);
        Cmd.AddCommand("invuse", null);
        Cmd.AddCommand("invprev", null);
        Cmd.AddCommand("invnext", null);
        Cmd.AddCommand("invdrop", null);
        Cmd.AddCommand("weapnext", null);
        Cmd.AddCommand("weapprev", null);

    }

    /**
     * WriteConfiguration
     * 
     * Writes key bindings and archived cvars to config.cfg.
     */
    public static void WriteConfiguration() {
        RandomAccessFile f;
        String path;

//        if (Context.cls.state == Defines.ca_uninitialized)
//            return;

        path = FileSystem.gamedir() + "/config.cfg";
        f = Lib.fopen(path, "rw");
        if (f == null) {
            Command.Printf("Couldn't write config.cfg.\n");
            return;
        }
        try {
            f.seek(0);
            f.setLength(0);
        } catch (IOException e1) {
        }
        try {
            f.writeBytes("// generated by quake, do not modify\n");
        } catch (IOException e) {
        }

        Key.WriteBindings(f);
        Lib.fclose(f);
        ConsoleVar.WriteVariables(path);
    }

    /**
     * FixCvarCheats
     */
    public static void FixCvarCheats() {
        int i;
        CL.cheatvar_t var;

        if ("1".equals(Context.cl.configstrings[Defines.CS_MAXCLIENTS])
                || 0 == Context.cl.configstrings[Defines.CS_MAXCLIENTS]
                        .length())
            return; // single player can cheat

        // find all the cvars if we haven't done it yet
        if (0 == CL.numcheatvars) {
            while (CL.cheatvars[CL.numcheatvars].name != null) {
                CL.cheatvars[CL.numcheatvars].var = ConsoleVar.Get(
                        CL.cheatvars[CL.numcheatvars].name,
                        CL.cheatvars[CL.numcheatvars].value, 0);
                CL.numcheatvars++;
            }
        }

        // make sure they are all set to the proper values
        for (i = 0; i < CL.numcheatvars; i++) {
            var = CL.cheatvars[i];
            if (!var.var.string.equals(var.value)) {
                ConsoleVar.Set(var.name, var.value);
            }
        }
    }

    //	  =============================================================

    /**
     * SendCommand
     */
    public static void SendCommand() {
        // get new key events
        Key.SendKeyEvents();

        // allow mice or other external controllers to add commands
        Input.Commands();

        // process console commands
        Cbuf.Execute();

        // fix any cheating cvars
        FixCvarCheats();

        // send intentions now
        CL_input.SendCmd();

        // resend a connection request if necessary
        CheckForResend();
    }

    //	private static int lasttimecalled;

    /**
     * Frame
     */
    public static void Frame(int msec) {
        
        if (Context.dedicated.value != 0)
            return;

        extratime += msec;

        if (Context.cl_timedemo.value == 0.0f) {
            if (Context.cls.getState() == Defines.ca_connected && extratime < 100) {
                return; // don't flood packets out while connecting
            }
            if (extratime < 1000 / Context.cl_maxfps.value) {
                return; // framerate is too high
            }
        }

        // let the mouse activate or deactivate
        Input.Frame();

        // decide the simulation time
        Context.cls.setFrametime(extratime / 1000.0f);
        Context.cl.time += extratime;
        Context.cls.setRealtime(Context.curtime);

        extratime = 0;

        if (Context.cls.getFrametime() > (1.0f / 5))
            Context.cls.setFrametime((1.0f / 5));

        // if in the debugger last frame, don't timeout
        if (msec > 5000)
            Context.cls.getNetchan().last_received = Timer.Milliseconds();

        // fetch results from server
        ReadPackets();

        // send a new command message to the server
        SendCommand();

        // predict all unacknowledged movements
        CL_pred.PredictMovement();

        // allow rendering DLL change
        VID.CheckChanges();
        if (!Context.cl.refresh_prepped
                && Context.cls.getState() == Defines.ca_active) {
            ClientView.PrepRefresh();
            // force GC after level loading
            // but not on playing a cinematic
            if (Context.cl.cinematictime == 0) java.lang.System.gc();
        }

        SCR.UpdateScreen();

        // update audio
        Sound.Update(Context.cl.refdef.vieworg, Context.cl.v_forward,
                Context.cl.v_right, Context.cl.v_up);

        // advance local effects for next frame
        CLEffects.RunDLights();
        CLEffects.RunLightStyles();
        SCR.runCinematic();
        SCR.RunConsole();

        Context.cls.setFramecount(Context.cls.getFramecount() + 1);
        if (Context.cls.getState() != Defines.ca_active
                || Context.cls.getKey_dest() != Defines.key_game) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Shutdown
     */
    public static void Shutdown() {

        if (isdown) {
            java.lang.System.out.print("recursive shutdown\n");
            return;
        }
        isdown = true;

        WriteConfiguration();

        Sound.Shutdown();
        Input.Shutdown();
        VID.Shutdown();
    }

    /**
     * Initialize client subsystem.
     */
    public static void Init() {
        if (Context.dedicated.value != 0.0f)
            return; // nothing running on the client

        // all archived variables will now be loaded

        Console.Init(); //ok

        Sound.Init(); //empty
        VID.Init();

        V.Init();

        Context.net_message.data = Context.net_message_buffer;
        Context.net_message.maxsize = Context.net_message_buffer.length;

        Menu.Init();

        SCR.Init();
        //Context.cls.disableScreen = 1.0f; // don't draw yet

        InitLocal();
        Input.Init();

        FileSystem.ExecAutoexec();
        Cbuf.Execute();
    }

    /**
     * Called after an ERR_DROP was thrown.
     */
    public static void Drop() {
        if (Context.cls.getState() == Defines.ca_uninitialized)
            return;
        if (Context.cls.getState() == Defines.ca_disconnected)
            return;

        Disconnect();

        // drop loading plaque unless this is the initial game start
        if (Context.cls.getDisableServerCount() != -1)
            SCR.EndLoadingPlaque(); // get rid of loading plaque
    }
}