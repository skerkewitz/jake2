/*
 * CL_parse.java
 * Copyright (C) 2004
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
import jake2.game.Cmd;
import jake2.game.TEntityState;
import jake2.io.FileSystem;
import jake2.qcommon.*;
import jake2.render.TModel;
import jake2.sound.Sound;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * CL_parse
 */
public class CL_parse {

    //// cl_parse.c -- parse a message received from the server

    public static String svc_strings[] = { "svc_bad", "svc_muzzleflash",
            "svc_muzzlflash2", "svc_temp_entity", "svc_layout",
            "svc_inventory", "svc_nop", "svc_disconnect", "svc_reconnect",
            "svc_sound", "svc_print", "svc_stufftext", "svc_serverdata",
            "svc_configstring", "svc_spawnbaseline", "svc_centerprint",
            "svc_download", "svc_playerinfo", "svc_packetentities",
            "svc_deltapacketentities", "svc_frame" };

    //	  =============================================================================

    public static String DownloadFileName(String fn) {
        return FileSystem.gamedir() + "/" + fn;
    }

    /**
     * CL_CheckOrDownloadFile returns true if the file exists, 
     * otherwise it attempts to start a
     * download from the server.
     */
    public static boolean CheckOrDownloadFile(String filename) {
        if (filename.indexOf("..") != -1) {
            Command.Printf("Refusing to download a path with ..\n");
            return true;
        }

        if (FileSystem.FileLength(filename) > 0) {
            // it exists, no need to download
            return true;
        }

        Context.cls.setDownloadname(filename);

        // download to a temp name, and only rename
        // to the real name when done, so if interrupted
        // a runt file wont be left
        Context.cls.setDownloadtempname(Command
                .StripExtension(Context.cls.getDownloadname()));
        Context.cls.setDownloadtempname(Context.cls.getDownloadtempname() + ".tmp");

        //	  ZOID
        // check to see if we already have a tmp for this file, if so, try to
        // resume
        // open the file if not opened yet
        String name = DownloadFileName(Context.cls.getDownloadtempname());

        RandomAccessFile fp = Lib.fopen(name, "r+b");
        
        if (fp != null) { 
            
            // it exists
            long len = 0;

            try {
                len = fp.length();
            } 
            catch (IOException e) {
            }
            

            Context.cls.setDownload(fp);

            // give the server an offset to start the download
            Command.Printf("Resuming " + Context.cls.getDownloadname() + "\n");
            Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
            Context.cls.getNetchan().message.writeString("download "
                        + Context.cls.getDownloadname() + " " + len);
        } else {
            Command.Printf("Downloading " + Context.cls.getDownloadname() + "\n");
            Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
            Context.cls.getNetchan().message.writeString("download "
                        + Context.cls.getDownloadname());
        }

        Context.cls.setDownloadnumber(Context.cls.getDownloadnumber() + 1);

        return false;
    }

    /*
     * =============== CL_Download_f
     * 
     * Request a download from the server ===============
     */
    public static TXCommand Download_f = new TXCommand() {
        public void execute() {
            String filename;

            if (Cmd.Argc() != 2) {
                Command.Printf("Usage: download <filename>\n");
                return;
            }

            filename = Cmd.Argv(1);

            if (filename.indexOf("..") != -1) {
                Command.Printf("Refusing to download a path with ..\n");
                return;
            }

            if (FileSystem.loadFile(filename) != null) { // it exists, no need to
                // download
                Command.Printf("File already exists.\n");
                return;
            }

            Context.cls.setDownloadname(filename);
            Command.Printf("Downloading " + Context.cls.getDownloadname() + "\n");

            // download to a temp name, and only rename
            // to the real name when done, so if interrupted
            // a runt file wont be left
            Context.cls.setDownloadtempname(Command
                    .StripExtension(Context.cls.getDownloadname()));
            Context.cls.setDownloadtempname(Context.cls.getDownloadtempname() + ".tmp");

            Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
            Context.cls.getNetchan().message.writeString("download "
                        + Context.cls.getDownloadname());

            Context.cls.setDownloadnumber(Context.cls.getDownloadnumber() + 1);
        }
    };

    /*
     * ====================== CL_RegisterSounds ======================
     */
    public static void RegisterSounds() {
        Sound.BeginRegistration();
        CL_tent.RegisterTEntSounds();
        for (int i = 1; i < Defines.MAX_SOUNDS; i++) {
            if (Context.cl.configstrings[Defines.CS_SOUNDS + i] == null
                    || Context.cl.configstrings[Defines.CS_SOUNDS + i]
                            .equals("")
                    || Context.cl.configstrings[Defines.CS_SOUNDS + i]
                            .equals("\0"))
                break;
            Context.cl.sound_precache[i] = Sound
                    .RegisterSound(Context.cl.configstrings[Defines.CS_SOUNDS
                            + i]);
            Key.SendKeyEvents(); // pump message loop
        }
        Sound.EndRegistration();
    }

    /*
     * ===================== CL_ParseDownload
     * 
     * A download message has been received from the server
     * =====================
     */
    public static void ParseDownload() {

        // read the data
        int size = TSizeBuffer.ReadShort(Context.net_message);
        int percent = TSizeBuffer.ReadByte(Context.net_message);
        if (size == -1) {
            Command.Printf("Server does not have this file.\n");
            if (Context.cls.getDownload() != null) {
                // if here, we tried to resume a file but the server said no
                try {
                    Context.cls.getDownload().close();
                } catch (IOException e) {
                }
                Context.cls.setDownload(null);
            }
            CL.RequestNextDownload();
            return;
        }

        // open the file if not opened yet
        if (Context.cls.getDownload() == null) {
            String name = DownloadFileName(Context.cls.getDownloadtempname()).toLowerCase();

            FileSystem.CreatePath(name);

            Context.cls.setDownload(Lib.fopen(name, "rw"));
            if (Context.cls.getDownload() == null) {
                Context.net_message.readcount += size;
                Command.Printf("Failed to open " + Context.cls.getDownloadtempname()
                        + "\n");
                CL.RequestNextDownload();
                return;
            }
        }


        try {
            Context.cls.getDownload().write(Context.net_message.data,
                    Context.net_message.readcount, size);
        } 
        catch (Exception e) {
        }
        Context.net_message.readcount += size;

        if (percent != 100) {
            // request next block
            //	   change display routines by zoid
            Context.cls.setDownloadpercent(percent);
            Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
            Context.cls.getNetchan().message.print("nextdl");
        } else {
            try {
                Context.cls.getDownload().close();
            } 
            catch (IOException e) {
            }

            // rename the temp file to it'entityState final name
            String oldn = DownloadFileName(Context.cls.getDownloadtempname());
            String newn = DownloadFileName(Context.cls.getDownloadname());
            int r = Lib.rename(oldn, newn);
            if (r != 0)
                Command.Printf("failed to rename.\n");

            Context.cls.setDownload(null);
            Context.cls.setDownloadpercent(0);

            // get another file if needed

            CL.RequestNextDownload();
        }
    }

    /*
     * =====================================================================
     * 
     * SERVER CONNECTING MESSAGES
     * 
     * =====================================================================
     */

    /*
     * ================== CL_ParseServerData ==================
     */
    //checked once, was ok.
    public static void ParseServerData() {
        Command.DPrintf("ParseServerData():Serverdata packet received.\n");
        //
        //	   wipe the TClientState struct
        //
        CL.ClearState();
        Context.cls.setState(Defines.ca_connected);

        //	   parse protocol version number
        int i = TSizeBuffer.ReadLong(Context.net_message);
        Context.cls.setServerProtocol(i);

        // BIG HACK to let demos from release work with the 3.0x patch!!!
        if (Context.server_state != 0 && Defines.PROTOCOL_VERSION == 34) {
        } else if (i != Defines.PROTOCOL_VERSION)
            Command.Error(Defines.ERR_DROP, "Server returned version " + i
                    + ", not " + Defines.PROTOCOL_VERSION);

        Context.cl.servercount = TSizeBuffer.ReadLong(Context.net_message);
        Context.cl.attractloop = TSizeBuffer.ReadByte(Context.net_message) != 0;

        // game directory
        String str = TSizeBuffer.ReadString(Context.net_message);
        Context.cl.gamedir = str;
        Command.dprintln("gamedir=" + str);

        // set gamedir
        if (str.length() > 0
                && (FileSystem.varGameDir.string == null
                        || FileSystem.varGameDir.string.length() == 0 || FileSystem.varGameDir.string
                        .equals(str))
                || (str.length() == 0 && (FileSystem.varGameDir.string != null || FileSystem.varGameDir.string
                        .length() == 0)))
            ConsoleVar.Set("game", str);

        // parse player entity number
        Context.cl.playernum = TSizeBuffer.ReadShort(Context.net_message);
        Command.dprintln("numplayers=" + Context.cl.playernum);
        // get the full level name
        str = TSizeBuffer.ReadString(Context.net_message);
        Command.dprintln("levelname=" + str);

        if (Context.cl.playernum == -1) { // playing a cinematic or showing a
            // pic, not a level
            SCR.playCinematic(str);
        } else {
            // seperate the printfs so the server message can have a color
            //			Command.Printf(
            //				"\n\n\35\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\36\37\n\n");
            //			Command.Printf('\02' + str + "\n");
            Command.Printf("Levelname:" + str + "\n");
            // need to prep refresh at next oportunity
            Context.cl.refresh_prepped = false;
        }
    }

    /*
     * ================== CL_ParseBaseline ==================
     */
    public static void ParseBaseline() {
        TEntityState nullstate = new TEntityState(null);
        //memset(nullstate, 0, sizeof(nullstate));
        int bits[] = { 0 };
        int newnum = CLEntity.ParseEntityBits(bits);
        TEntityState es = Context.cl_entities[newnum].baseline;
        CLEntity.ParseDelta(nullstate, es, newnum, bits[0]);
    }

    /*
     * ================ CL_LoadClientinfo
     * 
     * ================
     */
    public static void LoadClientinfo(TClientInfo ci, String s) {
        //char model_name[MAX_QPATH];
        //char skin_name[MAX_QPATH];
        //char model_filename[MAX_QPATH];
        //char skin_filename[MAX_QPATH];
        //char weapon_filename[MAX_QPATH];

        String model_name, skin_name, model_filename, skin_filename, weapon_filename;

        ci.cinfo = s;
        //ci.cinfo[sizeof(ci.cinfo) - 1] = 0;

        // isolate the player'entityState name
        ci.name = s;
        //ci.name[sizeof(ci.name) - 1] = 0;

        int t = s.indexOf('\\');
        //t = strstr(entityState, "\\");

        if (t != -1) {
            ci.name = s.substring(0, t);
            s = s.substring(t + 1, s.length());
            //entityState = t + 1;
        }

        if (Context.cl_noskins.value != 0 || s.length() == 0) {

            model_filename = ("players/male/tris.md2");
            weapon_filename = ("players/male/weapon.md2");
            skin_filename = ("players/male/grunt.pcx");
            ci.iconname = ("/players/male/grunt_i.pcx");

            ci.model = Context.re.RegisterModel(model_filename);

            ci.weaponmodel = new TModel[Defines.MAX_CLIENTWEAPONMODELS];
            ci.weaponmodel[0] = Context.re.RegisterModel(weapon_filename);
            ci.skin = Context.re.RegisterSkin(skin_filename);
            ci.icon = Context.re.RegisterPic(ci.iconname);
            
        } else {
            // isolate the model name

            int pos = s.indexOf('/');

            if (pos == -1)
                pos = s.indexOf('/');
            if (pos == -1) {
                pos = 0;
                Command.Error(Defines.ERR_FATAL, "Invalid model name:" + s);
            }

            model_name = s.substring(0, pos);

            // isolate the skin name
            skin_name = s.substring(pos + 1, s.length());

            // model file
            model_filename = "players/" + model_name + "/tris.md2";
            ci.model = Context.re.RegisterModel(model_filename);

            if (ci.model == null) {
                model_name = "male";
                model_filename = "players/male/tris.md2";
                ci.model = Context.re.RegisterModel(model_filename);
            }

            // skin file
            skin_filename = "players/" + model_name + "/" + skin_name + ".pcx";
            ci.skin = Context.re.RegisterSkin(skin_filename);

            // if we don't have the skin and the model wasn't male,
            // see if the male has it (this is for CTF'entityState skins)
            if (ci.skin == null && !model_name.equalsIgnoreCase("male")) {
                // change model to male
                model_name = "male";
                model_filename = "players/male/tris.md2";
                ci.model = Context.re.RegisterModel(model_filename);

                // see if the skin exists for the male model
                skin_filename = "players/" + model_name + "/" + skin_name
                        + ".pcx";
                ci.skin = Context.re.RegisterSkin(skin_filename);
            }

            // if we still don't have a skin, it means that the male model
            // didn't have
            // it, so default to grunt
            if (ci.skin == null) {
                // see if the skin exists for the male model
                skin_filename = "players/" + model_name + "/grunt.pcx";
                ci.skin = Context.re.RegisterSkin(skin_filename);
            }

            // weapon file
            for (int i = 0; i < ClientView.num_cl_weaponmodels; i++) {
                weapon_filename = "players/" + model_name + "/"
                        + ClientView.cl_weaponmodels[i];
                ci.weaponmodel[i] = Context.re.RegisterModel(weapon_filename);
                if (null == ci.weaponmodel[i] && model_name.equals("cyborg")) {
                    // try male
                    weapon_filename = "players/male/"
                            + ClientView.cl_weaponmodels[i];
                    ci.weaponmodel[i] = Context.re
                            .RegisterModel(weapon_filename);
                }
                if (0 == Context.cl_vwep.value)
                    break; // only one when vwep is off
            }

            // icon file
            ci.iconname = "/players/" + model_name + "/" + skin_name + "_i.pcx";
            ci.icon = Context.re.RegisterPic(ci.iconname);
        }

        // must have loaded all data types to be valud
        if (ci.skin == null || ci.icon == null || ci.model == null
                || ci.weaponmodel[0] == null) {
            ci.skin = null;
            ci.icon = null;
            ci.model = null;
            ci.weaponmodel[0] = null;
            return;
        }
    }

    /*
     * ================ CL_ParseClientinfo
     * 
     * Load the skin, icon, and model for a client ================
     */
    public static void ParseClientinfo(int player) {
        String s = Context.cl.configstrings[player + Defines.CS_PLAYERSKINS];

        TClientInfo ci = Context.cl.clientinfo[player];

        LoadClientinfo(ci, s);
    }

    /*
     * ================ CL_ParseConfigString ================
     */
    public static void ParseConfigString() {
        int i = TSizeBuffer.ReadShort(Context.net_message);

        if (i < 0 || i >= Defines.MAX_CONFIGSTRINGS)
            Command.Error(Defines.ERR_DROP, "configstring > MAX_CONFIGSTRINGS");

        String s = TSizeBuffer.ReadString(Context.net_message);

        String olds = Context.cl.configstrings[i];
        Context.cl.configstrings[i] = s;
        
        //Command.dprintln("ParseConfigString(): configstring[" + i + "]=<"+entityState+">");

        // do something apropriate

        if (i >= Defines.CS_LIGHTS
                && i < Defines.CS_LIGHTS + Defines.MAX_LIGHTSTYLES) {
            
            CLEffects.SetLightstyle(i - Defines.CS_LIGHTS);
            
        } else if (i >= Defines.CS_MODELS && i < Defines.CS_MODELS + Defines.MAX_MODELS) {
            if (Context.cl.refresh_prepped) {
                Context.cl.model_draw[i - Defines.CS_MODELS] = Context.re
                        .RegisterModel(Context.cl.configstrings[i]);
                if (Context.cl.configstrings[i].startsWith("*"))
                    Context.cl.model_clip[i - Defines.CS_MODELS] = CM
                            .InlineModel(Context.cl.configstrings[i]);
                else
                    Context.cl.model_clip[i - Defines.CS_MODELS] = null;
            }
        } else if (i >= Defines.CS_SOUNDS
                && i < Defines.CS_SOUNDS + Defines.MAX_MODELS) {
            if (Context.cl.refresh_prepped)
                Context.cl.sound_precache[i - Defines.CS_SOUNDS] = Sound
                        .RegisterSound(Context.cl.configstrings[i]);
        } else if (i >= Defines.CS_IMAGES
                && i < Defines.CS_IMAGES + Defines.MAX_MODELS) {
            if (Context.cl.refresh_prepped)
                Context.cl.image_precache[i - Defines.CS_IMAGES] = Context.re
                        .RegisterPic(Context.cl.configstrings[i]);
        } else if (i >= Defines.CS_PLAYERSKINS
                && i < Defines.CS_PLAYERSKINS + Defines.MAX_CLIENTS) {
            if (Context.cl.refresh_prepped && !olds.equals(s))
                ParseClientinfo(i - Defines.CS_PLAYERSKINS);
        }
    }

    /*
     * =====================================================================
     * 
     * ACTION MESSAGES
     * 
     * =====================================================================
     */

    private static final float[] pos_v = { 0, 0, 0 };
    /*
     * ================== CL_ParseStartSoundPacket ==================
     */
    public static void ParseStartSoundPacket() {
        int flags = TSizeBuffer.ReadByte(Context.net_message);
        int sound_num = TSizeBuffer.ReadByte(Context.net_message);

        float volume;
        if ((flags & Defines.SND_VOLUME) != 0)
            volume = TSizeBuffer.ReadByte(Context.net_message) / 255.0f;
        else
            volume = Defines.DEFAULT_SOUND_PACKET_VOLUME;

        float attenuation;
        if ((flags & Defines.SND_ATTENUATION) != 0)
            attenuation = TSizeBuffer.ReadByte(Context.net_message) / 64.0f;
        else
            attenuation = Defines.DEFAULT_SOUND_PACKET_ATTENUATION;

        float ofs;
        if ((flags & Defines.SND_OFFSET) != 0)
            ofs = TSizeBuffer.ReadByte(Context.net_message) / 1000.0f;
        else
            ofs = 0;

        int channel;
        int ent;
        if ((flags & Defines.SND_ENT) != 0) { // entity reletive
            channel = TSizeBuffer.ReadShort(Context.net_message);
            ent = channel >> 3;
            if (ent > Defines.MAX_EDICTS)
                Command.Error(Defines.ERR_DROP, "CL_ParseStartSoundPacket: entityDict = "
                        + ent);

            channel &= 7;
        } else {
            ent = 0;
            channel = 0;
        }

        float pos[];
        if ((flags & Defines.SND_POS) != 0) { // positioned in space
            TSizeBuffer.ReadPos(Context.net_message, pos_v);
            // is ok. sound driver copies
            pos = pos_v;
        } else
            // use entity number
            pos = null;

        if (null == Context.cl.sound_precache[sound_num])
            return;

        Sound.StartSound(pos, ent, channel, Context.cl.sound_precache[sound_num],
                volume, attenuation, ofs);
    }

    public static void SHOWNET(String s) {
        if (Context.cl_shownet.value >= 2)
            Command.Printf(Context.net_message.readcount - 1 + ":" + s + "\n");
    }

    /*
     * ===================== CL_ParseServerMessage =====================
     */
    public static void ParseServerMessage() {
        //
        //	   if recording demos, copy the message out
        //
        //if (cl_shownet.value == 1)
        //Command.Printf(net_message.cursize + " ");
        //else if (cl_shownet.value >= 2)
        //Command.Printf("------------------\n");

        //
        //	   parse the message
        //
        while (true) {
            if (Context.net_message.readcount > Context.net_message.cursize) {
                Command.Error(Defines.ERR_FATAL,
                        "CL_ParseServerMessage: Bad server message:");
                break;
            }

            int cmd = TSizeBuffer.ReadByte(Context.net_message);

            if (cmd == -1) {
                SHOWNET("END OF MESSAGE");
                break;
            }

            if (Context.cl_shownet.value >= 2) {
                if (null == svc_strings[cmd])
                    Command.Printf(Context.net_message.readcount - 1 + ":BAD CMD "
                            + cmd + "\n");
                else
                    SHOWNET(svc_strings[cmd]);
            }

            // other commands
            switch (cmd) {
            default:
                Command.Error(Defines.ERR_DROP,
                        "CL_ParseServerMessage: Illegible server message\n");
                break;

            case Defines.svc_nop:
                //				Command.Printf ("svc_nop\n");
                break;

            case Defines.svc_disconnect:
                Command.Error(Defines.ERR_DISCONNECT, "Server disconnected\n");
                break;

            case Defines.svc_reconnect:
                Command.Printf("Server disconnected, reconnecting\n");
                if (Context.cls.getDownload() != null) {
                    //ZOID, close download
                    try {
                        Context.cls.getDownload().close();
                    } catch (IOException e) {
                    }
                    Context.cls.setDownload(null);
                }
                Context.cls.setState(Defines.ca_connecting);
                Context.cls.setConnectTime(-99999); // CL_CheckForResend() will
                // fire immediately
                break;

            case Defines.svc_print:
                int i = TSizeBuffer.ReadByte(Context.net_message);
                if (i == Defines.PRINT_CHAT) {
                    Sound.StartLocalSound("misc/talk.wav");
                    Context.console.ormask = 128;
                }
                Command.Printf(TSizeBuffer.ReadString(Context.net_message));
                Context.console.ormask = 0;
                break;

            case Defines.svc_centerprint:
                SCR.CenterPrint(TSizeBuffer.ReadString(Context.net_message));
                break;

            case Defines.svc_stufftext:
                String s = TSizeBuffer.ReadString(Context.net_message);
                Command.DPrintf("stufftext: " + s + "\n");
                Cbuf.AddText(s);
                break;

            case Defines.svc_serverdata:
                Cbuf.Execute(); // make sure any stuffed commands are done
                ParseServerData();
                break;

            case Defines.svc_configstring:
                ParseConfigString();
                break;

            case Defines.svc_sound:
                ParseStartSoundPacket();
                break;

            case Defines.svc_spawnbaseline:
                ParseBaseline();
                break;

            case Defines.svc_temp_entity:
                CL_tent.ParseTEnt();
                break;

            case Defines.svc_muzzleflash:
                CLEffects.ParseMuzzleFlash();
                break;

            case Defines.svc_muzzleflash2:
                CLEffects.ParseMuzzleFlash2();
                break;

            case Defines.svc_download:
                ParseDownload();
                break;

            case Defines.svc_frame:
                CLEntity.ParseFrame();
                break;

            case Defines.svc_inventory:
                CL_inv.ParseInventory();
                break;

            case Defines.svc_layout:
        	Context.cl.layout = TSizeBuffer.ReadString(Context.net_message);
                break;

            case Defines.svc_playerinfo:
            case Defines.svc_packetentities:
            case Defines.svc_deltapacketentities:
                Command.Error(Defines.ERR_DROP, "Out of place frame data");
                break;
            }
        }

        ClientView.AddNetgraph();

        //
        // we don't know if it is ok to save a demo message until
        // after we have parsed the frame
        //
        if (Context.cls.getDemorecording() && !Context.cls.getDemowaiting())
            CL.WriteDemoMessage();
    }
}