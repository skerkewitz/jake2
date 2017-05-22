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

// Created on 29.11.2003 by RST.
// $Id: MSG.java,v 1.8 2005-12-18 22:10:02 cawe Exp $
package jake2.qcommon;

import jake2.Globals;
import jake2.game.*;
import jake2.util.*;

public class MSG extends Globals {

    //
    // writing functions
    //

    //should be ok.
    public static void ReadDir(TSizeBuffer sb, float[] dir) {
        int b;

        b = ReadByte(sb);
        if (b >= NUMVERTEXNORMALS)
            Com.Error(ERR_DROP, "MSF_ReadDir: out of range");
        Math3D.VectorCopy(bytedirs[b], dir);
    }

    //============================================================

    //
    // reading functions
    //

    public static void BeginReading(TSizeBuffer msg) {
        msg.readcount = 0;
    }

    // returns -1 if no more characters are available, but also [-128 , 127]
    public static int ReadChar(TSizeBuffer msg_read) {
        int c;

        if (msg_read.readcount + 1 > msg_read.cursize)
            c = -1;
        else
            c = msg_read.data[msg_read.readcount];
        msg_read.readcount++;
        // kickangles bugfix (rst)
        return c;
    }

    public static int ReadByte(TSizeBuffer msg_read) {
        int c;

        if (msg_read.readcount + 1 > msg_read.cursize)
            c = -1;
        else
            c = msg_read.data[msg_read.readcount] & 0xff;
        
        msg_read.readcount++;

        return c;
    }

    public static short ReadShort(TSizeBuffer msg_read) {
        int c;

        if (msg_read.readcount + 2 > msg_read.cursize)
            c = -1;
        else
            c = (short) ((msg_read.data[msg_read.readcount] & 0xff) + (msg_read.data[msg_read.readcount + 1] << 8));

        msg_read.readcount += 2;

        return (short) c;
    }

    public static int ReadLong(TSizeBuffer msg_read) {
        int c;

        if (msg_read.readcount + 4 > msg_read.cursize) {
            Com.Printf("buffer underrun in ReadLong!");
            c = -1;
        }

        else
            c = (msg_read.data[msg_read.readcount] & 0xff)
                    | ((msg_read.data[msg_read.readcount + 1] & 0xff) << 8)
                    | ((msg_read.data[msg_read.readcount + 2] & 0xff) << 16)
                    | ((msg_read.data[msg_read.readcount + 3] & 0xff) << 24);

        msg_read.readcount += 4;

        return c;
    }

    public static float ReadFloat(TSizeBuffer msg_read) {
        int n = ReadLong(msg_read);
        return Float.intBitsToFloat(n);
    }

    // 2k read buffer.
    public static byte readbuf[] = new byte[2048];

    public static String ReadString(TSizeBuffer msg_read) {

        byte c;
        int l = 0;
        do {
            c = (byte) ReadByte(msg_read);
            if (c == -1 || c == 0)
                break;

            readbuf[l] = c;
            l++;
        } while (l < 2047);
        
        String ret = new String(readbuf, 0, l);
        // Com.dprintln("MSG.ReadString:[" + ret + "]");
        return ret;
    }

    public static String ReadStringLine(TSizeBuffer msg_read) {

        int l;
        byte c;

        l = 0;
        do {
            c = (byte) ReadChar(msg_read);
            if (c == -1 || c == 0 || c == 0x0a)
                break;
            readbuf[l] = c;
            l++;
        } while (l < 2047);
        
        String ret = new String(readbuf, 0, l).trim();
        Com.dprintln("MSG.ReadStringLine:[" + ret.replace('\0', '@') + "]");
        return ret;
    }

    public static float ReadCoord(TSizeBuffer msg_read) {
        return ReadShort(msg_read) * (1.0f / 8);
    }

    public static void ReadPos(TSizeBuffer msg_read, float pos[]) {
        assert (pos.length == 3) : "vec3_t bug";
        pos[0] = ReadShort(msg_read) * (1.0f / 8);
        pos[1] = ReadShort(msg_read) * (1.0f / 8);
        pos[2] = ReadShort(msg_read) * (1.0f / 8);
    }

    public static float ReadAngle(TSizeBuffer msg_read) {
        return ReadChar(msg_read) * (360.0f / 256);
    }

    public static float ReadAngle16(TSizeBuffer msg_read) {
        return Math3D.SHORT2ANGLE(ReadShort(msg_read));
    }

    public static void ReadDeltaUsercmd(TSizeBuffer msg_read, usercmd_t from,
                                        usercmd_t move) {
        int bits;

        //memcpy(move, from, sizeof(* move));
        // IMPORTANT!! copy without new
        move.set(from);
        bits = ReadByte(msg_read);

        // read current angles
        if ((bits & CM_ANGLE1) != 0)
            move.angles[0] = ReadShort(msg_read);
        if ((bits & CM_ANGLE2) != 0)
            move.angles[1] = ReadShort(msg_read);
        if ((bits & CM_ANGLE3) != 0)
            move.angles[2] = ReadShort(msg_read);

        // read movement
        if ((bits & CM_FORWARD) != 0)
            move.forwardmove = ReadShort(msg_read);
        if ((bits & CM_SIDE) != 0)
            move.sidemove = ReadShort(msg_read);
        if ((bits & CM_UP) != 0)
            move.upmove = ReadShort(msg_read);

        // read buttons
        if ((bits & CM_BUTTONS) != 0)
            move.buttons = (byte) ReadByte(msg_read);

        if ((bits & CM_IMPULSE) != 0)
            move.impulse = (byte) ReadByte(msg_read);

        // read time to run command
        move.msec = (byte) ReadByte(msg_read);

        // read the light level
        move.lightlevel = (byte) ReadByte(msg_read);

    }

    public static void ReadData(TSizeBuffer msg_read, byte data[], int len) {
        for (int i = 0; i < len; i++)
            data[i] = (byte) ReadByte(msg_read);
    }    
            
}