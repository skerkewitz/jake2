/*
 * sizebuf_t.java
 * Copyright (C) 2003
 * 
 * $Id: sizebuf_t.java,v 1.1 2004-07-07 19:59:34 hzi Exp $
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
package jake2.qcommon;

import jake2.Defines;
import jake2.Globals;
import jake2.game.entity_state_t;
import jake2.game.usercmd_t;
import jake2.util.Lib;
import jake2.util.Math3D;

import java.util.Arrays;

/**
 * TSizeBuffer
 */
public final class TSizeBuffer {

    // 2k read buffer.
    public boolean allowoverflow = false;
    public boolean overflowed = false;
    public byte[] data = null;
    public int maxsize = 0;
    public int cursize = 0;
    public int readcount = 0;

    //should be ok.
    public static void ReadDir(TSizeBuffer sb, float[] dir) {
        int b;

        b = ReadByte(sb);
        if (b >= Defines.NUMVERTEXNORMALS)
            Com.Error(Defines.ERR_DROP, "MSF_ReadDir: out of range");
        Math3D.VectorCopy(Globals.bytedirs[b], dir);
    }

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

    public static String ReadString(TSizeBuffer msg_read) {
        byte[] readbuf = new byte[2048];

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
        byte[] readbuf = new byte[2048];
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
        if ((bits & Defines.CM_ANGLE1) != 0)
            move.angles[0] = ReadShort(msg_read);
        if ((bits & Defines.CM_ANGLE2) != 0)
            move.angles[1] = ReadShort(msg_read);
        if ((bits & Defines.CM_ANGLE3) != 0)
            move.angles[2] = ReadShort(msg_read);

        // read movement
        if ((bits & Defines.CM_FORWARD) != 0)
            move.forwardmove = ReadShort(msg_read);
        if ((bits & Defines.CM_SIDE) != 0)
            move.sidemove = ReadShort(msg_read);
        if ((bits & Defines.CM_UP) != 0)
            move.upmove = ReadShort(msg_read);

        // read buttons
        if ((bits & Defines.CM_BUTTONS) != 0)
            move.buttons = (byte) ReadByte(msg_read);

        if ((bits & Defines.CM_IMPULSE) != 0)
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


    public void writeDeltaUsercmd(usercmd_t from, usercmd_t cmd) {
        int bits;

        //
        // send the movement message
        //
        bits = 0;
        if (cmd.angles[0] != from.angles[0])
            bits |= Defines.CM_ANGLE1;
        if (cmd.angles[1] != from.angles[1])
            bits |= Defines.CM_ANGLE2;
        if (cmd.angles[2] != from.angles[2])
            bits |= Defines.CM_ANGLE3;
        if (cmd.forwardmove != from.forwardmove)
            bits |= Defines.CM_FORWARD;
        if (cmd.sidemove != from.sidemove)
            bits |= Defines.CM_SIDE;
        if (cmd.upmove != from.upmove)
            bits |= Defines.CM_UP;
        if (cmd.buttons != from.buttons)
            bits |= Defines.CM_BUTTONS;
        if (cmd.impulse != from.impulse)
            bits |= Defines.CM_IMPULSE;

        this.writeByte(bits);

        if ((bits & Defines.CM_ANGLE1) != 0)
            this.writeShort((int) cmd.angles[0]);
        if ((bits & Defines.CM_ANGLE2) != 0)
            this.writeShort((int) cmd.angles[1]);
        if ((bits & Defines.CM_ANGLE3) != 0)
            this.writeShort((int) cmd.angles[2]);

        if ((bits & Defines.CM_FORWARD) != 0)
            this.writeShort((int) cmd.forwardmove);
        if ((bits & Defines.CM_SIDE) != 0)
            this.writeShort((int) cmd.sidemove);
        if ((bits & Defines.CM_UP) != 0)
            this.writeShort((int) cmd.upmove);

        if ((bits & Defines.CM_BUTTONS) != 0)
            this.writeByte(cmd.buttons);
        if ((bits & Defines.CM_IMPULSE) != 0)
            this.writeByte(cmd.impulse);

        this.writeByte(cmd.msec);
        this.writeByte(cmd.lightlevel);
    }

    //should be ok.
    public void writeDir(float[] dir) {
        int i, best;
        float d, bestd;

        if (dir == null) {
            this.writeByte(0);
            return;
        }

        bestd = 0;
        best = 0;
        for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
            d = Math3D.DotProduct(dir, Globals.bytedirs[i]);
            if (d > bestd) {
                bestd = d;
                best = i;
            }
        }
        this.writeByte(best);
    }

    /**
      * Writes part of a packetentities message. Can delta from either a baseline
      * or a previous packet_entity ==================
      */
    public void writeDeltaEntity(entity_state_t from, entity_state_t to, boolean force, boolean newentity) {
        int bits;

        if (0 == to.number)
            Com.Error(Defines.ERR_FATAL, "Unset entity number");
        if (to.number >= Defines.MAX_EDICTS)
            Com.Error(Defines.ERR_FATAL, "Entity number >= MAX_EDICTS");

        // send an update
        bits = 0;

        if (to.number >= 256)
            bits |= Defines.U_NUMBER16; // number8 is implicit otherwise

        if (to.origin[0] != from.origin[0])
            bits |= Defines.U_ORIGIN1;
        if (to.origin[1] != from.origin[1])
            bits |= Defines.U_ORIGIN2;
        if (to.origin[2] != from.origin[2])
            bits |= Defines.U_ORIGIN3;

        if (to.angles[0] != from.angles[0])
            bits |= Defines.U_ANGLE1;
        if (to.angles[1] != from.angles[1])
            bits |= Defines.U_ANGLE2;
        if (to.angles[2] != from.angles[2])
            bits |= Defines.U_ANGLE3;

        if (to.skinnum != from.skinnum) {
            if (to.skinnum < 256)
                bits |= Defines.U_SKIN8;
            else if (to.skinnum < 0x10000)
                bits |= Defines.U_SKIN16;
            else
                bits |= (Defines.U_SKIN8 | Defines.U_SKIN16);
        }

        if (to.frame != from.frame) {
            if (to.frame < 256)
                bits |= Defines.U_FRAME8;
            else
                bits |= Defines.U_FRAME16;
        }

        if (to.effects != from.effects) {
            if (to.effects < 256)
                bits |= Defines.U_EFFECTS8;
            else if (to.effects < 0x8000)
                bits |= Defines.U_EFFECTS16;
            else
                bits |= Defines.U_EFFECTS8 | Defines.U_EFFECTS16;
        }

        if (to.renderfx != from.renderfx) {
            if (to.renderfx < 256)
                bits |= Defines.U_RENDERFX8;
            else if (to.renderfx < 0x8000)
                bits |= Defines.U_RENDERFX16;
            else
                bits |= Defines.U_RENDERFX8 | Defines.U_RENDERFX16;
        }

        if (to.solid != from.solid)
            bits |= Defines.U_SOLID;

        // event is not delta compressed, just 0 compressed
        if (to.event != 0)
            bits |= Defines.U_EVENT;

        if (to.modelindex != from.modelindex)
            bits |= Defines.U_MODEL;
        if (to.modelindex2 != from.modelindex2)
            bits |= Defines.U_MODEL2;
        if (to.modelindex3 != from.modelindex3)
            bits |= Defines.U_MODEL3;
        if (to.modelindex4 != from.modelindex4)
            bits |= Defines.U_MODEL4;

        if (to.sound != from.sound)
            bits |= Defines.U_SOUND;

        if (newentity || (to.renderfx & Defines.RF_BEAM) != 0)
            bits |= Defines.U_OLDORIGIN;

        //
        // write the message
        //
        if (bits == 0 && !force)
            return; // nothing to send!

        //----------

        if ((bits & 0xff000000) != 0)
            bits |= Defines.U_MOREBITS3 | Defines.U_MOREBITS2 | Defines.U_MOREBITS1;
        else if ((bits & 0x00ff0000) != 0)
            bits |= Defines.U_MOREBITS2 | Defines.U_MOREBITS1;
        else if ((bits & 0x0000ff00) != 0)
            bits |= Defines.U_MOREBITS1;

        this.writeByte(bits & 255);

        if ((bits & 0xff000000) != 0) {
            this.writeByte((bits >>> 8) & 255);
            this.writeByte((bits >>> 16) & 255);
            this.writeByte((bits >>> 24) & 255);
        } else if ((bits & 0x00ff0000) != 0) {
            this.writeByte((bits >>> 8) & 255);
            this.writeByte((bits >>> 16) & 255);
        } else if ((bits & 0x0000ff00) != 0) {
            this.writeByte((bits >>> 8) & 255);
        }

        //----------

        if ((bits & Defines.U_NUMBER16) != 0)
            this.writeShort(to.number);
        else
            this.writeByte(to.number);

        if ((bits & Defines.U_MODEL) != 0)
            this.writeByte(to.modelindex);
        if ((bits & Defines.U_MODEL2) != 0)
            this.writeByte(to.modelindex2);
        if ((bits & Defines.U_MODEL3) != 0)
            this.writeByte(to.modelindex3);
        if ((bits & Defines.U_MODEL4) != 0)
            this.writeByte(to.modelindex4);

        if ((bits & Defines.U_FRAME8) != 0)
            this.writeByte(to.frame);
        if ((bits & Defines.U_FRAME16) != 0)
            this.writeShort(to.frame);

        if ((bits & Defines.U_SKIN8) != 0 && (bits & Defines.U_SKIN16) != 0) //used for laser
                                                             // colors
            this.writeInt(to.skinnum);
        else if ((bits & Defines.U_SKIN8) != 0)
            this.writeByte(to.skinnum);
        else if ((bits & Defines.U_SKIN16) != 0)
            this.writeShort(to.skinnum);

        if ((bits & (Defines.U_EFFECTS8 | Defines.U_EFFECTS16)) == (Defines.U_EFFECTS8 | Defines.U_EFFECTS16))
            this.writeInt(to.effects);
        else if ((bits & Defines.U_EFFECTS8) != 0)
            this.writeByte(to.effects);
        else if ((bits & Defines.U_EFFECTS16) != 0)
            this.writeShort(to.effects);

        if ((bits & (Defines.U_RENDERFX8 | Defines.U_RENDERFX16)) == (Defines.U_RENDERFX8 | Defines.U_RENDERFX16))
            this.writeInt(to.renderfx);
        else if ((bits & Defines.U_RENDERFX8) != 0)
            this.writeByte(to.renderfx);
        else if ((bits & Defines.U_RENDERFX16) != 0)
            this.writeShort(to.renderfx);

        if ((bits & Defines.U_ORIGIN1) != 0)
            this.writeCoord(to.origin[0]);
        if ((bits & Defines.U_ORIGIN2) != 0)
            this.writeCoord(to.origin[1]);
        if ((bits & Defines.U_ORIGIN3) != 0)
            this.writeCoord(to.origin[2]);

        if ((bits & Defines.U_ANGLE1) != 0)
            this.writeAngle(to.angles[0]);
        if ((bits & Defines.U_ANGLE2) != 0)
            this.writeAngle(to.angles[1]);
        if ((bits & Defines.U_ANGLE3) != 0)
            this.writeAngle(to.angles[2]);

        if ((bits & Defines.U_OLDORIGIN) != 0) {
            this.writeCoord(to.old_origin[0]);
            this.writeCoord(to.old_origin[1]);
            this.writeCoord(to.old_origin[2]);
        }

        if ((bits & Defines.U_SOUND) != 0)
            this.writeByte(to.sound);
        if ((bits & Defines.U_EVENT) != 0)
            this.writeByte(to.event);
        if ((bits & Defines.U_SOLID) != 0)
            this.writeShort(to.solid);
    }

    public void writeAngle(float f) {
        writeByte((int) (f * 256 / 360) & 255);
    }

    public void writeAngle16(float f) {
        writeShort(Math3D.ANGLE2SHORT(f));
    }

    public void writeLong(int c) {
        writeInt(c);
    }

    public void writeFloat(float f) {
        writeInt(Float.floatToIntBits(f));
    }

    // had a bug, now its ok.
    public void writeString(String s) {
        String x = s;

        if (s == null)
            x = "";

        write(Lib.stringToBytes(x));
        writeByte(0);
    }

    public void writeString(byte s[]) {
        writeString(new String(s).trim());
    }

    public void writeCoord(float f) {
        writeShort((int) (f * 8));
    }

    public void writePos(float[] pos) {
        assert (pos.length == 3) : "vec3_t bug";
        writeShort((int) (pos[0] * 8));
        writeShort((int) (pos[1] * 8));
        writeShort((int) (pos[2] * 8));
    }

    public void writeInt(int c) {
        int i = getSpace(4);
        data[i++] = (byte) ((c & 0xff));
        data[i++] = (byte) ((c >>> 8) & 0xff);
        data[i++] = (byte) ((c >>> 16) & 0xff);
        data[i++] = (byte) ((c >>> 24) & 0xff);
    }


    public void writeShort(int c) {
        int i = getSpace(2);
        data[i++] = (byte) (c & 0xff);
        data[i] = (byte) ((c >>> 8) & 0xFF);
    }

    //ok.
    public void writeByte(int c) {
        data[getSpace(1)] = (byte) (c & 0xFF);
    }

    //ok.
    public void writeByte(float c) {
        writeByte((int) c);
    }

    //ok.
    public void writeChar(int c) {
        data[getSpace(1)] = (byte) (c & 0xFF);
    }

    //ok.
    public void writeChar(float c) {
        writeChar((int) c);
    }

    public void init(byte data[], int length) {
        // TODO check this. cwei
        this.readcount = 0;

        this.data = data;
        this.maxsize = length;
        this.cursize = 0;
        this.allowoverflow = this.overflowed = false;
    }

    /**
     * Ask for the pointer using TSizeBuffer.cursize (RST)
     */
    private int getSpace(int length) {
        int oldsize;

        if (this.cursize + length > this.maxsize) {
            if (!this.allowoverflow)
                Com.Error(Defines.ERR_FATAL, "SZ_GetSpace: overflow without allowoverflow set");

            if (length > this.maxsize)
                Com.Error(Defines.ERR_FATAL, "SZ_GetSpace: " + length + " is > full buffer size");

            Com.Printf("SZ_GetSpace: overflow\n");
            this.clear();
            this.overflowed = true;
        }

        oldsize = this.cursize;
        this.cursize += length;

        return oldsize;
    }

    public void write(byte data[], int length) {
        //memcpy(SZ_GetSpace(buf, length), data, length);
        System.arraycopy(data, 0, this.data, this.getSpace(length), length);
    }

    public void write(byte data[], int offset, int length) {
        System.arraycopy(data, offset, this.data, this.getSpace(length), length);
    }

    public void write(byte data[]) {
        int length = data.length;
        //memcpy(SZ_GetSpace(buf, length), data, length);
        System.arraycopy(data, 0, this.data, this.getSpace(length), length);
    }

    //
    public void print(String data) {
        Com.dprintln("SZ.print():<" + data + ">");
        int length = data.length();
        byte str[] = Lib.stringToBytes(data);

        if (this.cursize != 0) {

            if (this.data[this.cursize - 1] != 0) {
                //memcpy( SZ_GetSpace(buf, len), data, len); // no trailing 0
                System.arraycopy(str, 0, this.data, this.getSpace(length + 1), length);
            } else {
                System.arraycopy(str, 0, this.data, this.getSpace(length) - 1, length);
                //memcpy(SZ_GetSpace(buf, len - 1) - 1, data, len); // write over trailing 0
            }
        } else
            // first print.
            System.arraycopy(str, 0, this.data, this.getSpace(length), length);
        //memcpy(SZ_GetSpace(buf, len), data, len);

        this.data[this.cursize - 1] = 0;
    }


    public void clear() {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
        cursize = 0;
        overflowed = false;
    }
}
