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
// $Id: ServerGame.java,v 1.10 2006-01-21 21:53:32 salomo Exp $
package jake2.server;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.*;
import jake2.qcommon.*;
import jake2.util.Math3D;

public class ServerGame {

    /**
     * pfUnicast
     * <p>
     * Sends the contents of the mutlicast buffer to a single client.
     */
    public static void pfUnicast(TEntityDict ent, boolean reliable) {

        if (ent == null) {
            return;
        }

        int p = ent.index;
        if (p < 1 || p > ServerMain.maxclients.value) {
            return;
        }

        final TClient client = ServerInit.svs.clients[p - 1];

        if (reliable) {
            client.netchan.message.write(ServerInit.sv.multicast.data, ServerInit.sv.multicast.cursize);
        } else {
            client.datagram.write(ServerInit.sv.multicast.data, ServerInit.sv.multicast.cursize);
        }

        ServerInit.sv.multicast.clear();
    }


    /**
     * Centerprintf for critical messages.
     */
    public static void PF_cprintfhigh(TEntityDict ent, String fmt) {
        PF_cprintf(ent, Defines.PRINT_HIGH, fmt);
    }

    /**
     * PF_cprintf
     * <p>
     * print to a single client.
     */
    public static void PF_cprintf(TEntityDict ent, int level, String fmt) {

        int n = 0;

        if (ent != null) {
            n = ent.index;
            if (n < 1 || n > ServerMain.maxclients.value)
                Command.Error(Defines.ERR_DROP, "cprintf to a non-client");
        }

        if (ent != null)
            SV_SEND.SV_ClientPrintf(ServerInit.svs.clients[n - 1], level, fmt);
        else
            Command.Printf(fmt);
    }

    /**
     * PF_centerprintf
     * <p>
     * centerprint to a single client.
     */
    public static void PF_centerprintf(TEntityDict ent, String fmt) {
        int n;

        n = ent.index;
        if (n < 1 || n > ServerMain.maxclients.value)
            return; // Com_Error (ERR_DROP, "centerprintf to a non-client");

        ServerInit.sv.multicast.writeByte(Defines.svc_centerprint);
        ServerInit.sv.multicast.writeString(fmt);
        pfUnicast(ent, true);
    }

    /**
     * pfSetModel
     * <p>
     * Also sets mins and maxs for inline bmodels.
     */
    public static void pfSetModel(TEntityDict ent, String name) {

        if (name == null) {
            Command.Error(Defines.ERR_DROP, "pfSetModel: NULL");
        }

        ent.entityState.modelIndex = ServerInit.modelIndexOf(name);

        // if it is an inline model, get the size information for it
        if (name.startsWith("*")) {
            TCModel mod = CM.InlineModel(name);
            Math3D.VectorCopy(mod.mins, ent.mins);
            Math3D.VectorCopy(mod.maxs, ent.maxs);
            ServerWorld.linkEdict(ent);
        }
    }

    /**
     * PF_Configstring
     */
    public static void PF_Configstring(int index, String val) {
        if (index < 0 || index >= Defines.MAX_CONFIGSTRINGS)
            Command.Error(Defines.ERR_DROP, "configstring: bad index " + index
                    + "\n");

        if (val == null)
            val = "";

        // change the string in sv
        ServerInit.sv.configstrings[index] = val;

        if (ServerInit.sv.state != Defines.ss_loading) { // send the update to
            // everyone
            ServerInit.sv.multicast.clear();
            ServerInit.sv.multicast.writeChar(Defines.svc_configstring);
            ServerInit.sv.multicast.writeShort(index);
            ServerInit.sv.multicast.writeString(val);

            SV_SEND.SV_Multicast(Context.vec3_origin, Defines.MULTICAST_ALL_R);
        }
    }

    public static void PF_WriteChar(int c) {
        ServerInit.sv.multicast.writeChar(c);
    }

    public static void PF_WriteByte(int c) {
        ServerInit.sv.multicast.writeByte(c);
    }

    public static void PF_WriteShort(int c) {
        ServerInit.sv.multicast.writeShort(c);
    }

    public static void PF_WriteLong(int c) {
        ServerInit.sv.multicast.writeLong(c);
    }

    public static void PF_WriteFloat(float f) {
        ServerInit.sv.multicast.writeFloat(Float.floatToIntBits(f));
    }

    public static void PF_WriteString(String s) {
        ServerInit.sv.multicast.writeString(s);
    }

    public static void PF_WritePos(float[] pos) {
        ServerInit.sv.multicast.writePos(pos);
    }

    public static void PF_WriteDir(float[] dir) {
        ServerInit.sv.multicast.writeDir(dir);
    }

    public static void PF_WriteAngle(float f) {
        ServerInit.sv.multicast.writeAngle(f);
    }

    /**
     * PF_inPVS
     * <p>
     * Also checks portalareas so that doors block sight.
     */
    public static boolean PF_inPVS(float[] p1, float[] p2) {
        int leafnum;
        int cluster;
        int area1, area2;
        byte mask[];

        leafnum = CM.CM_PointLeafnum(p1);
        cluster = CM.CM_LeafCluster(leafnum);
        area1 = CM.CM_LeafArea(leafnum);
        mask = CM.CM_ClusterPVS(cluster);

        leafnum = CM.CM_PointLeafnum(p2);
        cluster = CM.CM_LeafCluster(leafnum);
        area2 = CM.CM_LeafArea(leafnum);

        // quake2 bugfix
        if (cluster == -1)
            return false;
        if (mask != null && (0 == (mask[cluster >>> 3] & (1 << (cluster & 7)))))
            return false;

        return CM.CM_AreasConnected(area1, area2);
    }

    /**
     * PF_inPHS.
     * <p>
     * Also checks portalareas so that doors block sound.
     */
    public static boolean PF_inPHS(float[] p1, float[] p2) {
        int leafnum;
        int cluster;
        int area1, area2;
        byte mask[];

        leafnum = CM.CM_PointLeafnum(p1);
        cluster = CM.CM_LeafCluster(leafnum);
        area1 = CM.CM_LeafArea(leafnum);
        mask = CM.CM_ClusterPHS(cluster);

        leafnum = CM.CM_PointLeafnum(p2);
        cluster = CM.CM_LeafCluster(leafnum);
        area2 = CM.CM_LeafArea(leafnum);

        // quake2 bugfix
        if (cluster == -1)
            return false;
        if (mask != null && (0 == (mask[cluster >> 3] & (1 << (cluster & 7)))))
            return false; // more than one bounce away
        return CM.CM_AreasConnected(area1, area2);
    }

    public static void PF_StartSound(TEntityDict entity, int channel,
                                     int sound_num, float volume, float attenuation, float timeofs) {

        if (null == entity)
            return;
        SV_SEND.SV_StartSound(null, entity, channel, sound_num, volume,
                attenuation, timeofs);

    }


    /**
     * SV_ShutdownGameProgs
     * <p>
     * Called when either the entire server is being killed, or it is changing
     * to a different game directory.
     */
    public static void SV_ShutdownGameProgs() {
        GameBase.ShutdownGame();
    }

    /**
     * SV_InitGameProgs
     * <p>
     * init the game subsystem for a new map.
     */

    public static void SV_InitGameProgs() {

        // unload anything we have now
        SV_ShutdownGameProgs();

        TGameImport gimport = new TGameImport();

        // all functions set in game_export_t (rst)
        GameBase.GetGameApi(gimport);

        GameSave.InitGame();
    }
}