/*
 * java
 * Copyright (C) 2004
 * 
 * $Id: CL_input.java,v 1.7 2005-06-26 09:17:33 hzi Exp $
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
import jake2.game.TUserCmd;
import jake2.game.TVar;
import jake2.network.Netchan;
import jake2.qcommon.Command;
import jake2.qcommon.ConsoleVar;
import jake2.qcommon.TBuffer;
import jake2.sys.MouseInput;
import jake2.util.Lib;
import jake2.util.Math3D;

/**
 * ClientInput
 */
public class ClientInput {

    private long frame_msec;

    private long old_sys_frame_time;

    private TVar cl_nodelta;

    private TKButton in_klook = new TKButton();

    private TKButton in_left = new TKButton();

    private TKButton in_right = new TKButton();

    private TKButton in_forward = new TKButton();

    private TKButton in_back = new TKButton();

    private TKButton in_lookup = new TKButton();

    private TKButton in_lookdown = new TKButton();

    private TKButton in_moveleft = new TKButton();

    private TKButton in_moveright = new TKButton();

    public TKButton in_strafe = new TKButton();

    private TKButton in_speed = new TKButton();

    private TKButton in_use = new TKButton();

    private TKButton in_attack = new TKButton();

    private TKButton in_up = new TKButton();

    private TKButton in_down = new TKButton();

    private int in_impulse;

    private void KeyDown(TKButton b) {

        String c = Cmd.Argv(1);

        // typed manually at the console for continuous down
        int k = c.length() > 0 ? Lib.atoi(c) : -1;

        if (k == b.down[0] || k == b.down[1]) {
            return; // repeating key
        }

        if (b.down[0] == 0) {
            b.down[0] = k;
        } else if (b.down[1] == 0) {
            b.down[1] = k;
        } else {
            Command.Printf("Three keys down for a button!\n");
            return;
        }

        if ((b.state & 1) != 0) {
            return; // still down
        }

        // save timestamp
        String downtime = Cmd.Argv(2);
        b.downtime = Lib.atoi(downtime);
        if (b.downtime == 0)
            b.downtime = Context.sys_frame_time - 100;

        b.state |= 3; // down + impulse down
    }

    private void KeyUp(TKButton b) {
        int k;
        String c;
        int uptime;

        c = Cmd.Argv(1);
        if (c.length() > 0)
            k = Lib.atoi(c);
        else {
            // typed manually at the console, assume for unsticking, so clear
            // all
            b.down[0] = b.down[1] = 0;
            b.state = 4; // impulse up
            return;
        }

        if (b.down[0] == k)
            b.down[0] = 0;
        else if (b.down[1] == k)
            b.down[1] = 0;
        else
            return; // key up without coresponding down (menu pass through)
        if (b.down[0] != 0 || b.down[1] != 0)
            return; // some other key is still holding it down

        if ((b.state & 1) == 0)
            return; // still up (this should not happen)

        // save timestamp
        c = Cmd.Argv(2);
        uptime = Lib.atoi(c);
        if (uptime != 0)
            b.msec += uptime - b.downtime;
        else
            b.msec += 10;

        b.state &= ~1; // now up
        b.state |= 4; // impulse up
    }

    /*
     * Returns the fraction of the frame that the key was down
     */
    private float KeyState(TKButton key) {

        key.state &= 1; // clear impulses

        long msec = key.msec;
        key.msec = 0;

        if (key.state != 0) {
            // still down
            msec += Context.sys_frame_time - key.downtime;
            key.downtime = Context.sys_frame_time;
        }

        float val = (float) msec / frame_msec;
        if (val < 0)
            val = 0;
        if (val > 1)
            val = 1;

        return val;
    }

    /*
     * Moves the local angle positions ================
     */
    private void AdjustAngles() {
        float speed;
        float up, down;

        if ((in_speed.state & 1) != 0)
            speed = Context.cls.getFrametime() * Context.cl_anglespeedkey.value;
        else
            speed = Context.cls.getFrametime();

        if ((in_strafe.state & 1) == 0) {
            Context.cl.viewangles[Defines.YAW] -= speed * Context.cl_yawspeed.value * KeyState(in_right);
            Context.cl.viewangles[Defines.YAW] += speed * Context.cl_yawspeed.value * KeyState(in_left);
        }
        if ((in_klook.state & 1) != 0) {
            Context.cl.viewangles[Defines.PITCH] -= speed * Context.cl_pitchspeed.value * KeyState(in_forward);
            Context.cl.viewangles[Defines.PITCH] += speed * Context.cl_pitchspeed.value * KeyState(in_back);
        }

        up = KeyState(in_lookup);
        down = KeyState(in_lookdown);

        Context.cl.viewangles[Defines.PITCH] -= speed * Context.cl_pitchspeed.value * up;
        Context.cl.viewangles[Defines.PITCH] += speed * Context.cl_pitchspeed.value * down;
    }

    /*
     * ================ CL_BaseMove
     *
     * Send the intended movement message to the server ================
     */
    private void BaseMove(TUserCmd cmd) {
        AdjustAngles();

        //memset (cmd, 0, sizeof(*cmd));
        cmd.clear();

        Math3D.VectorCopy(Context.cl.viewangles, cmd.angles);
        if ((in_strafe.state & 1) != 0) {
            cmd.sidemove += Context.cl_sidespeed.value * KeyState(in_right);
            cmd.sidemove -= Context.cl_sidespeed.value * KeyState(in_left);
        }

        cmd.sidemove += Context.cl_sidespeed.value * KeyState(in_moveright);
        cmd.sidemove -= Context.cl_sidespeed.value * KeyState(in_moveleft);

        cmd.upmove += Context.cl_upspeed.value * KeyState(in_up);
        cmd.upmove -= Context.cl_upspeed.value * KeyState(in_down);

        if ((in_klook.state & 1) == 0) {
            cmd.forwardmove += Context.cl_forwardspeed.value * KeyState(in_forward);
            cmd.forwardmove -= Context.cl_forwardspeed.value * KeyState(in_back);
        }

        //
        //	   adjust for speed key / running
        //
        if (((in_speed.state & 1) ^ (int) (Context.cl_run.value)) != 0) {
            cmd.forwardmove *= 2;
            cmd.sidemove *= 2;
            cmd.upmove *= 2;
        }

    }

    private void ClampPitch() {

        float pitch;

        pitch = Math3D.SHORT2ANGLE(Context.cl.frame.playerstate.pmove.delta_angles[Defines.PITCH]);
        if (pitch > 180)
            pitch -= 360;

        if (Context.cl.viewangles[Defines.PITCH] + pitch < -360)
            Context.cl.viewangles[Defines.PITCH] += 360; // wrapped
        if (Context.cl.viewangles[Defines.PITCH] + pitch > 360)
            Context.cl.viewangles[Defines.PITCH] -= 360; // wrapped

        if (Context.cl.viewangles[Defines.PITCH] + pitch > 89)
            Context.cl.viewangles[Defines.PITCH] = 89 - pitch;
        if (Context.cl.viewangles[Defines.PITCH] + pitch < -89)
            Context.cl.viewangles[Defines.PITCH] = -89 - pitch;
    }

    /*
     * ============== CL_FinishMove ==============
     */
    private void FinishMove(TUserCmd cmd) {
        int ms;
        int i;

        //
        //	   figure button bits
        //
        if ((in_attack.state & 3) != 0)
            cmd.buttons |= Defines.BUTTON_ATTACK;
        in_attack.state &= ~2;

        if ((in_use.state & 3) != 0)
            cmd.buttons |= Defines.BUTTON_USE;
        in_use.state &= ~2;

        if (Key.anykeydown != 0 && Context.cls.getKey_dest() == Defines.key_game)
            cmd.buttons |= Defines.BUTTON_ANY;

        // send milliseconds of time to apply the move
        ms = (int) (Context.cls.getFrametime() * 1000);
        if (ms > 250)
            ms = 100; // time was unreasonable
        cmd.msec = (byte) ms;

        ClampPitch();
        for (i = 0; i < 3; i++)
            cmd.angles[i] = (short) Math3D.ANGLE2SHORT(Context.cl.viewangles[i]);

        cmd.impulse = (byte) in_impulse;
        in_impulse = 0;

        // send the ambient light level at the player's current position
        cmd.lightlevel = (byte) Context.cl_lightlevel.value;
    }


    private void createCmd(TUserCmd cmd) {
        //TUserCmd cmd = new TUserCmd();

        frame_msec = Context.sys_frame_time - old_sys_frame_time;
        if (frame_msec < 1)
            frame_msec = 1;
        if (frame_msec > 200)
            frame_msec = 200;

        // get basic movement from keyboard
        BaseMove(cmd);

        // allow mice or other external controllers to add to the move
        MouseInput.Move(cmd);

        FinishMove(cmd);

        old_sys_frame_time = Context.sys_frame_time;

        //return cmd;
    }

    void InitInput() {
        Cmd.registerCommand("centerview", () -> MouseInput.CenterView());

        Cmd.registerCommand("+moveup", () -> KeyDown(in_up));
        Cmd.registerCommand("-moveup", () -> KeyUp(in_up));
        Cmd.registerCommand("+movedown", () -> KeyDown(in_down));
        Cmd.registerCommand("-movedown", () -> KeyUp(in_down));
        Cmd.registerCommand("+left", () -> KeyDown(in_left));
        Cmd.registerCommand("-left", () -> KeyUp(in_left));
        Cmd.registerCommand("+right", () -> KeyDown(in_right));
        Cmd.registerCommand("-right", () -> KeyUp(in_right));
        Cmd.registerCommand("+forward", () -> KeyDown(in_forward));
        Cmd.registerCommand("-forward", () -> KeyUp(in_forward));
        Cmd.registerCommand("+back", () -> KeyDown(in_back));
        Cmd.registerCommand("-back", () -> KeyUp(in_back));
        Cmd.registerCommand("+lookup", () -> KeyDown(in_lookup));
        Cmd.registerCommand("-lookup", () -> KeyUp(in_lookup));
        Cmd.registerCommand("+lookdown", () -> KeyDown(in_lookdown));
        Cmd.registerCommand("-lookdown", () -> KeyUp(in_lookdown));
        Cmd.registerCommand("+strafe", () -> KeyDown(in_strafe));
        Cmd.registerCommand("-strafe", () -> KeyUp(in_strafe));
        Cmd.registerCommand("+moveleft", () -> KeyDown(in_moveleft));
        Cmd.registerCommand("-moveleft", () -> KeyUp(in_moveleft));
        Cmd.registerCommand("+moveright", () -> KeyDown(in_moveright));
        Cmd.registerCommand("-moveright", () -> KeyUp(in_moveright));
        Cmd.registerCommand("+speed", () -> KeyDown(in_speed));
        Cmd.registerCommand("-speed", () -> KeyUp(in_speed));
        Cmd.registerCommand("+attack", () -> KeyDown(in_attack));
        Cmd.registerCommand("-attack", () -> KeyUp(in_attack));
        Cmd.registerCommand("+use", () -> KeyDown(in_use));
        Cmd.registerCommand("-use", () -> KeyUp(in_use));
        Cmd.registerCommand("impulse", () -> in_impulse = Lib.atoi(Cmd.Argv(1)));
        Cmd.registerCommand("+klook", () -> KeyDown(in_klook));
        Cmd.registerCommand("-klook", () -> KeyUp(in_klook));

        cl_nodelta = ConsoleVar.get("cl_nodelta", "0", 0);
    }

    private final TBuffer buf = TBuffer.createWithSize(128);

    private final TUserCmd nullcmd = new TUserCmd();


    public void sendUserComand() {

        // build a command even if not connected

        // save this command off for prediction
        int i = Context.cls.getNetchan().outgoing_sequence & (Defines.CMD_BACKUP - 1);
        TUserCmd cmd = Context.cl.cmds[i];
        Context.cl.cmd_time[i] = Context.cls.getRealtime(); // for netgraph
        // ping calculation

        // fill the cmd
        createCmd(cmd);

        Context.cl.cmd.assign(cmd);

        if (Context.cls.getState() == Defines.ca_disconnected || Context.cls.getState() == Defines.ca_connecting) {
            return;
        }

        if (Context.cls.getState() == Defines.ca_connected) {
            if (Context.cls.getNetchan().message.writeHeadPosition != 0 || Context.curtime - Context.cls.getNetchan().last_sent > 1000) {
                Netchan.transmit(Context.cls.getNetchan(), 0, new byte[0]);
            }
            return;
        }

        // send a userinfo update if needed
        if (Context.userinfo_modified) {
            Client.FixUpGender();
            Context.userinfo_modified = false;
            Context.cls.getNetchan().message.writeByte(Defines.clc_userinfo);
            Context.cls.getNetchan().message.writeString(ConsoleVar.Userinfo());
        }

        buf.reset();

        if (cmd.buttons != 0 && Context.cl.cinematictime > 0 && !Context.cl.attractloop
                && Context.cls.getRealtime() - Context.cl.cinematictime > 1000) { // skip
            // the
            // rest
            // of
            // the
            // cinematic
            SCR.FinishCinematic();
        }

        // begin a client move command
        buf.writeByte(Defines.clc_move);

        // save the position for a checksum byte
        int checksumIndex = buf.writeHeadPosition;
        buf.writeByte(0);

        // let the server know what the last frame we
        // got was, so the next message can be delta compressed
        if (cl_nodelta.value != 0.0f || !Context.cl.frame.valid || Context.cls.getDemowaiting())
            buf.writeLong(-1);
        else
            buf.writeLong(Context.cl.frame.serverframe);

        // send this and the previous cmds in the message, so
        // if the last packet was dropped, it can be recovered
        i = (Context.cls.getNetchan().outgoing_sequence - 2) & (Defines.CMD_BACKUP - 1);
        cmd = Context.cl.cmds[i];
        //memset (nullcmd, 0, sizeof(nullcmd));
        nullcmd.clear();

        buf.writeDeltaUsercmd(nullcmd, cmd);
        TUserCmd oldcmd = cmd;

        i = (Context.cls.getNetchan().outgoing_sequence - 1) & (Defines.CMD_BACKUP - 1);
        cmd = Context.cl.cmds[i];

        buf.writeDeltaUsercmd(oldcmd, cmd);
        oldcmd = cmd;

        i = (Context.cls.getNetchan().outgoing_sequence) & (Defines.CMD_BACKUP - 1);
        cmd = Context.cl.cmds[i];

        buf.writeDeltaUsercmd(oldcmd, cmd);

        // calculate a checksum over the move commands
        buf.data[checksumIndex] = Command.BlockSequenceCRCByte(buf.data,
                checksumIndex + 1,
                buf.writeHeadPosition - checksumIndex - 1,
                Context.cls.getNetchan().outgoing_sequence);

        //
        // deliver the message
        //
        Netchan.transmit(Context.cls.getNetchan(), buf.writeHeadPosition, buf.data);
    }
}