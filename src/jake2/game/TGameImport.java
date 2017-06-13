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

// Created on 31.10.2003 by RST.
// $Id: TGameImport.java,v 1.7 2006-01-21 21:53:31 salomo Exp $
package jake2.game;

import jake2.Defines;
import jake2.qcommon.*;
import jake2.server.*;

//
//	collection of functions provided by the renderMain engine
//
public class TGameImport {
    // special messages
    public void bprintf(int printlevel, String s) {
        SV_SEND.SV_BroadcastPrintf(printlevel, s);
    }

    public void dprintf(String s) {
        Command.Printf(s);
    }

    public void cprintf(TEntityDict ent, int printlevel, String s) {
        ServerGame.PF_cprintf(ent, printlevel, s);
    }

    public void centerprintf(TEntityDict ent, String s) {
        ServerGame.PF_centerprintf(ent, s);
    }

    public void sound(TEntityDict ent, int channel, int soundindex, float volume,
                      float attenuation, float timeofs) {
        ServerGame.PF_StartSound(ent, channel, soundindex, volume, attenuation,
                timeofs);
    }

    public void positioned_sound(float[] origin, TEntityDict ent, int channel,
                                 int soundinedex, float volume, float attenuation, float timeofs) {

        SV_SEND.SV_StartSound(origin, ent, channel, soundinedex, volume,
                attenuation, timeofs);
    }

    // config strings hold all the index strings, the lightstyles,
    // and misc data like the sky definition and cdtrack.
    // All of the current configstrings are sent to clients when
    // they connect, and changes are sent to all connected clients.
    public void configstring(int num, String string) {
        ServerGame.PF_Configstring(num, string);
    }

    public void error(String err) {
        Command.Error(Defines.ERR_FATAL, err);
    }

    public void error(int level, String err) {
        Command.Error(level, err);
    }

    // the *index functions create configstrings and some internal server state
    public int modelindex(String name) {
        return ServerInit.modelIndexOf(name);
    }

    public int soundindex(String name) {
        return ServerInit.soundIndexOf(name);
    }

    public int imageindex(String name) {
        return ServerInit.imageIndexOf(name);
    }

    public void setmodel(TEntityDict ent, String name) {
        ServerGame.pfSetModel(ent, name);
    }

    // collision detection
    public TTrace trace(float[] start, float[] mins, float[] maxs,
                        float[] end, TEntityDict passent, int contentmask) {
        return ServerWorld.trace(start, mins, maxs, end, passent, contentmask);
    }

    public pmove_t.PointContentsAdapter pointcontents = new pmove_t.PointContentsAdapter() {
        public int pointcontents(float[] o) {
            return 0;
        }
    };

    public boolean inPHS(float[] p1, float[] p2) {
        return ServerGame.PF_inPHS(p1, p2);
    }

    public void SetAreaPortalState(int portalnum, boolean open) {
        CM.CM_SetAreaPortalState(portalnum, open);
    }

    public boolean AreasConnected(int area1, int area2) {
        return CM.CM_AreasConnected(area1, area2);
    }

    // an entity will never be sent to a client or used for collision
    // if it is not passed to linkentity. If the size, position, or
    // solidity changes, it must be relinked.
    public void linkentity(TEntityDict ent) {
        ServerWorld.linkEdict(ent);
    }

    public void unlinkentity(TEntityDict ent) {
        ServerWorld.unlinkEdict(ent);
    }

    // call before removing an interactive edict
    public int BoxEdicts(float[] mins, float[] maxs, TEntityDict list[],
            int maxcount, int areatype) {
        return ServerWorld.areaEdicts(mins, maxs, list, maxcount, areatype);
    }

    public void Pmove(pmove_t pmove) {
        PMove.Pmove(pmove);
    }

    // player movement code common with client prediction
    // network messaging
    public void multicast(float[] origin, int to) {
        SV_SEND.SV_Multicast(origin, to);
    }

    public void unicast(TEntityDict ent, boolean reliable) {
        ServerGame.pfUnicast(ent, reliable);
    }


    public void WriteByte(int c) {
        ServerGame.PF_WriteByte(c);
    }

    public void WriteShort(int c) {
        ServerGame.PF_WriteShort(c);
    }

    public void WriteString(String s) {
        ServerGame.PF_WriteString(s);
    }

    public void WritePosition(float[] pos) {
        ServerGame.PF_WritePos(pos);
    }

    // some fractional bits
    public void WriteDir(float[] pos) {
        ServerGame.PF_WriteDir(pos);
    }

    // console variable interaction
    public TVar cvar(String var_name, String value, int flags) {
        return ConsoleVar.get(var_name, value, flags);
    }

    // console variable interaction
    public TVar cvar_set(String var_name, String value) {
        return ConsoleVar.Set(var_name, value);
    }

    // console variable interaction
    public TVar cvar_forceset(String var_name, String value) {
        return ConsoleVar.ForceSet(var_name, value);
    }

    // ClientCommand and ServerCommand parameter access
    public int argc() {
        return Cmd.Argc();
    }


    public String argv(int n) {
        return Cmd.Argv(n);
    }

    // concatenation of all argv >= 1
    public String args() {
        return Cmd.Args();
    }

    // add commands to the server console as if they were typed in
    // for map changing, etc
    public void AddCommandString(String text) {
        CommandBuffer.AddText(text);
    }

}