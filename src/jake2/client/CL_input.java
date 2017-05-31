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
import jake2.game.TVar;
import jake2.game.usercmd_t;
import jake2.network.Netchan;
import jake2.qcommon.*;
import jake2.sys.IN;
import jake2.util.Lib;
import jake2.util.Math3D;

/**
 * CL_input
 */
public class CL_input {

	static long frame_msec;

	static long old_sys_frame_time;

	static TVar cl_nodelta;

	/*
	 * ===============================================================================
	 * 
	 * KEY BUTTONS
	 * 
	 * Continuous button event tracking is complicated by the fact that two
	 * different input sources (say, mouse button 1 and the control key) can
	 * both press the same button, but the button should only be released when
	 * both of the pressing key have been released.
	 * 
	 * When a key event issues a button command (+forward, +attack, etc), it
	 * appends its key number as a parameter to the command so it can be matched
	 * up with the release.
	 * 
	 * state bit 0 is the current state of the key state bit 1 is edge triggered
	 * on the up to down transition state bit 2 is edge triggered on the down to
	 * up transition
	 * 
	 * 
	 * Key_Event (int key, qboolean down, unsigned time);
	 * 
	 * +mlook src time
	 * 
	 * ===============================================================================
	 */

	static kbutton_t in_klook = new kbutton_t();

	static kbutton_t in_left = new kbutton_t();

	static kbutton_t in_right = new kbutton_t();

	static kbutton_t in_forward = new kbutton_t();

	static kbutton_t in_back = new kbutton_t();

	static kbutton_t in_lookup = new kbutton_t();

	static kbutton_t in_lookdown = new kbutton_t();

	static kbutton_t in_moveleft = new kbutton_t();

	static kbutton_t in_moveright = new kbutton_t();

	public static kbutton_t in_strafe = new kbutton_t();

	static kbutton_t in_speed = new kbutton_t();

	static kbutton_t in_use = new kbutton_t();

	static kbutton_t in_attack = new kbutton_t();

	static kbutton_t in_up = new kbutton_t();

	static kbutton_t in_down = new kbutton_t();

	static int in_impulse;

	static void KeyDown(kbutton_t b) {
		int k;
		String c;

		c = Cmd.Argv(1);
		if (c.length() > 0)
			k = Lib.atoi(c);
		else
			k = -1; // typed manually at the console for continuous down

		if (k == b.down[0] || k == b.down[1])
			return; // repeating key

		if (b.down[0] == 0)
			b.down[0] = k;
		else if (b.down[1] == 0)
			b.down[1] = k;
		else {
			Command.Printf("Three keys down for a button!\n");
			return;
		}

		if ((b.state & 1) != 0)
			return; // still down

		// save timestamp
		c = Cmd.Argv(2);
		b.downtime = Lib.atoi(c);
		if (b.downtime == 0)
			b.downtime = Context.sys_frame_time - 100;

		b.state |= 3; // down + impulse down
	}

	static void KeyUp(kbutton_t b) {
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

	static void IN_KLookDown() {
		KeyDown(in_klook);
	}

	static void IN_KLookUp() {
		KeyUp(in_klook);
	}

	static void IN_UpDown() {
		KeyDown(in_up);
	}

	static void IN_UpUp() {
		KeyUp(in_up);
	}

	static void IN_DownDown() {
		KeyDown(in_down);
	}

	static void IN_DownUp() {
		KeyUp(in_down);
	}

	static void IN_LeftDown() {
		KeyDown(in_left);
	}

	static void IN_LeftUp() {
		KeyUp(in_left);
	}

	static void IN_RightDown() {
		KeyDown(in_right);
	}

	static void IN_RightUp() {
		KeyUp(in_right);
	}

	static void IN_ForwardDown() {
		KeyDown(in_forward);
	}

	static void IN_ForwardUp() {
		KeyUp(in_forward);
	}

	static void IN_BackDown() {
		KeyDown(in_back);
	}

	static void IN_BackUp() {
		KeyUp(in_back);
	}

	static void IN_LookupDown() {
		KeyDown(in_lookup);
	}

	static void IN_LookupUp() {
		KeyUp(in_lookup);
	}

	static void IN_LookdownDown() {
		KeyDown(in_lookdown);
	}

	static void IN_LookdownUp() {
		KeyUp(in_lookdown);
	}

	static void IN_MoveleftDown() {
		KeyDown(in_moveleft);
	}

	static void IN_MoveleftUp() {
		KeyUp(in_moveleft);
	}

	static void IN_MoverightDown() {
		KeyDown(in_moveright);
	}

	static void IN_MoverightUp() {
		KeyUp(in_moveright);
	}

	static void IN_SpeedDown() {
		KeyDown(in_speed);
	}

	static void IN_SpeedUp() {
		KeyUp(in_speed);
	}

	static void IN_StrafeDown() {
		KeyDown(in_strafe);
	}

	static void IN_StrafeUp() {
		KeyUp(in_strafe);
	}

	static void IN_AttackDown() {
		KeyDown(in_attack);
	}

	static void IN_AttackUp() {
		KeyUp(in_attack);
	}

	static void IN_UseDown() {
		KeyDown(in_use);
	}

	static void IN_UseUp() {
		KeyUp(in_use);
	}

	static void IN_Impulse() {
		in_impulse = Lib.atoi(Cmd.Argv(1));
	}

	/*
	 * =============== CL_KeyState
	 * 
	 * Returns the fraction of the frame that the key was down ===============
	 */
	static float KeyState(kbutton_t key) {
		float val;
		long msec;

		key.state &= 1; // clear impulses

		msec = key.msec;
		key.msec = 0;

		if (key.state != 0) {
			// still down
			msec += Context.sys_frame_time - key.downtime;
			key.downtime = Context.sys_frame_time;
		}

		val = (float) msec / frame_msec;
		if (val < 0)
			val = 0;
		if (val > 1)
			val = 1;

		return val;
	}

	//	  ==========================================================================

	/*
	 * ================ CL_AdjustAngles
	 * 
	 * Moves the local angle positions ================
	 */
	static void AdjustAngles() {
		float speed;
		float up, down;

		if ((in_speed.state & 1) != 0)
			speed = Context.cls.frametime * Context.cl_anglespeedkey.value;
		else
			speed = Context.cls.frametime;

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
	static void BaseMove(usercmd_t cmd) {
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

	static void ClampPitch() {

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
	static void FinishMove(usercmd_t cmd) {
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

		if (Key.anykeydown != 0 && Context.cls.key_dest == Defines.key_game)
			cmd.buttons |= Defines.BUTTON_ANY;

		// send milliseconds of time to apply the move
		ms = (int) (Context.cls.frametime * 1000);
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

	/*
	 * ================= CL_CreateCmd =================
	 */
	static void CreateCmd(usercmd_t cmd) {
		//usercmd_t cmd = new usercmd_t();

		frame_msec = Context.sys_frame_time - old_sys_frame_time;
		if (frame_msec < 1)
			frame_msec = 1;
		if (frame_msec > 200)
			frame_msec = 200;

		// get basic movement from keyboard
		BaseMove(cmd);

		// allow mice or other external controllers to add to the move
		IN.Move(cmd);

		FinishMove(cmd);

		old_sys_frame_time = Context.sys_frame_time;

		//return cmd;
	}

	/*
	 * ============ CL_InitInput ============
	 */
	static void InitInput() {
		Cmd.AddCommand("centerview", () -> IN.CenterView());

		Cmd.AddCommand("+moveup", () -> IN_UpDown());
		Cmd.AddCommand("-moveup", () -> IN_UpUp());
		Cmd.AddCommand("+movedown", () -> IN_DownDown());
		Cmd.AddCommand("-movedown", () -> IN_DownUp());
		Cmd.AddCommand("+left", () -> IN_LeftDown());
		Cmd.AddCommand("-left", () -> IN_LeftUp());
		Cmd.AddCommand("+right", () -> IN_RightDown());
		Cmd.AddCommand("-right", () -> IN_RightUp());
		Cmd.AddCommand("+forward", () -> IN_ForwardDown());
		Cmd.AddCommand("-forward", () -> IN_ForwardUp());
		Cmd.AddCommand("+back", () -> IN_BackDown());
		Cmd.AddCommand("-back", () -> IN_BackUp());
		Cmd.AddCommand("+lookup", () -> IN_LookupDown());
		Cmd.AddCommand("-lookup", () -> IN_LookupUp());
		Cmd.AddCommand("+lookdown", () -> IN_LookdownDown());
		Cmd.AddCommand("-lookdown", () -> IN_LookdownUp());
		Cmd.AddCommand("+strafe", () -> IN_StrafeDown());
		Cmd.AddCommand("-strafe", () -> IN_StrafeUp());
		Cmd.AddCommand("+moveleft", () -> IN_MoveleftDown());
		Cmd.AddCommand("-moveleft", () -> IN_MoveleftUp());
		Cmd.AddCommand("+moveright", () -> IN_MoverightDown());
		Cmd.AddCommand("-moveright", () -> IN_MoverightUp());
		Cmd.AddCommand("+speed", () -> IN_SpeedDown());
		Cmd.AddCommand("-speed", () -> IN_SpeedUp());
		Cmd.AddCommand("+attack", () -> IN_AttackDown());
		Cmd.AddCommand("-attack", () -> IN_AttackUp());
		Cmd.AddCommand("+use", () -> IN_UseDown());
		Cmd.AddCommand("-use", () -> IN_UseUp());
		Cmd.AddCommand("impulse", () -> IN_Impulse());
		Cmd.AddCommand("+klook", () -> IN_KLookDown());
		Cmd.AddCommand("-klook", () -> IN_KLookUp());

		cl_nodelta = ConsoleVar.Get("cl_nodelta", "0", 0);
	}

	private static final TSizeBuffer buf = new TSizeBuffer();
	private static final byte[] data = new byte[128];
	private static final usercmd_t nullcmd = new usercmd_t();
	/*
	 * ================= CL_SendCmd =================
	 */
	static void SendCmd() {
		int i;
		usercmd_t cmd, oldcmd;
		int checksumIndex;

		// build a command even if not connected

		// save this command off for prediction
		i = Context.cls.netchan.outgoing_sequence & (Defines.CMD_BACKUP - 1);
		cmd = Context.cl.cmds[i];
		Context.cl.cmd_time[i] = Context.cls.realtime; // for netgraph
															 // ping calculation

		// fill the cmd
		CreateCmd(cmd);

		Context.cl.cmd.set(cmd);

		if (Context.cls.state == Defines.ca_disconnected || Context.cls.state == Defines.ca_connecting)
			return;

		if (Context.cls.state == Defines.ca_connected) {
			if (Context.cls.netchan.message.cursize != 0 || Context.curtime - Context.cls.netchan.last_sent > 1000)
				Netchan.Transmit(Context.cls.netchan, 0, new byte[0]);
			return;
		}

		// send a userinfo update if needed
		if (Context.userinfo_modified) {
			CL.FixUpGender();
			Context.userinfo_modified = false;
			Context.cls.netchan.message.writeByte(Defines.clc_userinfo);
			Context.cls.netchan.message.writeString(ConsoleVar.Userinfo());
		}

		buf.init(data, data.length);

		if (cmd.buttons != 0 && Context.cl.cinematictime > 0 && !Context.cl.attractloop
				&& Context.cls.realtime - Context.cl.cinematictime > 1000) { // skip
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
		checksumIndex = buf.cursize;
		buf.writeByte(0);

		// let the server know what the last frame we
		// got was, so the next message can be delta compressed
		if (cl_nodelta.value != 0.0f || !Context.cl.frame.valid || Context.cls.demowaiting)
			buf.writeLong(-1);
		else
			buf.writeLong(Context.cl.frame.serverframe);

		// send this and the previous cmds in the message, so
		// if the last packet was dropped, it can be recovered
		i = (Context.cls.netchan.outgoing_sequence - 2) & (Defines.CMD_BACKUP - 1);
		cmd = Context.cl.cmds[i];
		//memset (nullcmd, 0, sizeof(nullcmd));
		nullcmd.clear();

		buf.writeDeltaUsercmd(nullcmd, cmd);
		oldcmd = cmd;

		i = (Context.cls.netchan.outgoing_sequence - 1) & (Defines.CMD_BACKUP - 1);
		cmd = Context.cl.cmds[i];

		buf.writeDeltaUsercmd(oldcmd, cmd);
		oldcmd = cmd;

		i = (Context.cls.netchan.outgoing_sequence) & (Defines.CMD_BACKUP - 1);
		cmd = Context.cl.cmds[i];

		buf.writeDeltaUsercmd(oldcmd, cmd);

		// calculate a checksum over the move commands
		buf.data[checksumIndex] = Command.BlockSequenceCRCByte(buf.data, checksumIndex + 1, buf.cursize - checksumIndex - 1,
				Context.cls.netchan.outgoing_sequence);

		//
		// deliver the message
		//
		Netchan.Transmit(Context.cls.netchan, buf.cursize, buf.data);
	}
}