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
// $Id: TServer.java,v 1.2 2004-09-22 19:22:12 salomo Exp $
package jake2.server;

import jake2.Defines;
import jake2.game.TCModel;
import jake2.game.TEntityState;
import jake2.qcommon.TSizeBuffer;

import java.io.RandomAccessFile;

public class TServer {

    public TServer() {
        models = new TCModel[Defines.MAX_MODELS];
        for (int n = 0; n < Defines.MAX_MODELS; n++)
            models[n] = new TCModel();

        for (int n = 0; n < Defines.MAX_EDICTS; n++)
            baselines[n] = new TEntityState(null);
    }

    int state; // precache commands are only valid during load

    boolean attractloop; // running cinematics and demos for the local system
                         // only

    boolean loadgame; // client begins should reuse existing entity

    int time; // always sv.framenum * 100 msec

    int framenum;

    String name = ""; // map name, or cinematic name

    TCModel models[];

    String configstrings[] = new String[Defines.MAX_CONFIGSTRINGS];

    TEntityState baselines[] = new TEntityState[Defines.MAX_EDICTS];

    // the multicast buffer is used to send a message to a set of clients
    // it is only used to marshall data until SV_Multicast is called
    TSizeBuffer multicast = new TSizeBuffer();

    byte multicast_buf[] = new byte[Defines.MAX_MSGLEN];

    // demo server information
    RandomAccessFile demofile;

    boolean timedemo; // don't time sync
}