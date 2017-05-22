/*
 * Globals.java
 * Copyright (C) 2003
 * 
 * $Id: Globals.java,v 1.6 2008-03-02 20:21:12 kbrussel Exp $
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
package jake2;

import jake2.client.*;
import jake2.game.*;
import jake2.qcommon.FileSystem;
import jake2.qcommon.TSizeBuffer;
import jake2.qcommon.netadr_t;
import jake2.render.DummyRenderer;
import jake2.render.model_t;

import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.Random;

/**
 * Globals ist the collection of global variables and constants.
 * It is more elegant to use these vars by inheritance to separate 
 * it with eclipse refactoring later.
 * 
 * As consequence you dont have to touch that much code this time. 
 */
public class Globals implements Defines {

	public static final String __DATE__ = "2003";

	public static final float VERSION = 3.21f;

	public static final String BASEDIRNAME = "baseq2";

	public static FileSystem fileSystem = null;

	/*
	 * global variables
	 */
	public static int curtime = 0;
	public static boolean cmd_wait;

	public static int alias_count;
	public static int c_traces;
	public static int c_brush_traces;
	public static int c_pointcontents;
	public static int server_state;

	public static TVar cl_add_blend;
	public static TVar cl_add_entities;
	public static TVar cl_add_lights;
	public static TVar cl_add_particles;
	public static TVar cl_anglespeedkey;
	public static TVar cl_autoskins;
	public static TVar cl_footsteps;
	public static TVar cl_forwardspeed;
	public static TVar cl_gun;
	public static TVar cl_maxfps;
	public static TVar cl_noskins;
	public static TVar cl_pitchspeed;
	public static TVar cl_predict;
	public static TVar cl_run;
	public static TVar cl_sidespeed;
	public static TVar cl_stereo;
	public static TVar cl_stereo_separation;
	public static TVar cl_timedemo = new TVar();
	public static TVar cl_timeout;
	public static TVar cl_upspeed;
	public static TVar cl_yawspeed;
	public static TVar dedicated;
	public static TVar developer;
	public static TVar fixedtime;
	public static TVar freelook;
	public static TVar host_speeds;
	public static TVar log_stats;
	public static TVar logfile_active;
	public static TVar lookspring;
	public static TVar lookstrafe;
	public static TVar nostdout;
	public static TVar sensitivity;
	public static TVar showtrace;
	public static TVar timescale;
	public static TVar in_mouse;
	public static TVar in_joystick;


	public static TSizeBuffer net_message = new TSizeBuffer();

	/*
	=============================================================================
	
							COMMAND BUFFER
	
	=============================================================================
	*/

	public static byte defer_text_buf[] = new byte[8192];

	public static cmdalias_t cmd_alias;

	//=============================================================================

	public static byte[] net_message_buffer = new byte[MAX_MSGLEN];

	public static int time_before_game;
	public static int time_after_game;
	public static int time_before_ref;
	public static int time_after_ref;

	public static FileWriter log_stats_file = null;

	public static TVar m_pitch;
	public static TVar m_yaw;
	public static TVar m_forward;
	public static TVar m_side;

	public static TVar cl_lightlevel;

	//
	//	   userinfo
	//
	public static TVar info_password;
	public static TVar info_spectator;
	public static TVar name;
	public static TVar skin;
	public static TVar rate;
	public static TVar fov;
	public static TVar msg;
	public static TVar hand;
	public static TVar gender;
	public static TVar gender_auto;

	public static TVar cl_vwep;

	public static client_static_t cls = new client_static_t();
	public static client_state_t cl = new client_state_t();

	public static centity_t cl_entities[] = new centity_t[Defines.MAX_EDICTS];
	static {
		for (int i = 0; i < cl_entities.length; i++) {
			cl_entities[i] = new centity_t();
		}
	}

	public static entity_state_t cl_parse_entities[] = new entity_state_t[Defines.MAX_PARSE_ENTITIES];
	
	static {
		for (int i = 0; i < cl_parse_entities.length; i++)
		{
			cl_parse_entities[i] = new entity_state_t(null);
		}
	}

	public static TVar rcon_client_password;
	public static TVar rcon_address;

	public static TVar cl_shownet;
	public static TVar cl_showmiss;
	public static TVar cl_showclamp;

	public static TVar cl_paused;

	// client/anorms.h
	public static final float bytedirs[][] = { /**
								*/
		{ -0.525731f, 0.000000f, 0.850651f }, {
			-0.442863f, 0.238856f, 0.864188f }, {
			-0.295242f, 0.000000f, 0.955423f }, {
			-0.309017f, 0.500000f, 0.809017f }, {
			-0.162460f, 0.262866f, 0.951056f }, {
			0.000000f, 0.000000f, 1.000000f }, {
			0.000000f, 0.850651f, 0.525731f }, {
			-0.147621f, 0.716567f, 0.681718f }, {
			0.147621f, 0.716567f, 0.681718f }, {
			0.000000f, 0.525731f, 0.850651f }, {
			0.309017f, 0.500000f, 0.809017f }, {
			0.525731f, 0.000000f, 0.850651f }, {
			0.295242f, 0.000000f, 0.955423f }, {
			0.442863f, 0.238856f, 0.864188f }, {
			0.162460f, 0.262866f, 0.951056f }, {
			-0.681718f, 0.147621f, 0.716567f }, {
			-0.809017f, 0.309017f, 0.500000f }, {
			-0.587785f, 0.425325f, 0.688191f }, {
			-0.850651f, 0.525731f, 0.000000f }, {
			-0.864188f, 0.442863f, 0.238856f }, {
			-0.716567f, 0.681718f, 0.147621f }, {
			-0.688191f, 0.587785f, 0.425325f }, {
			-0.500000f, 0.809017f, 0.309017f }, {
			-0.238856f, 0.864188f, 0.442863f }, {
			-0.425325f, 0.688191f, 0.587785f }, {
			-0.716567f, 0.681718f, -0.147621f }, {
			-0.500000f, 0.809017f, -0.309017f }, {
			-0.525731f, 0.850651f, 0.000000f }, {
			0.000000f, 0.850651f, -0.525731f }, {
			-0.238856f, 0.864188f, -0.442863f }, {
			0.000000f, 0.955423f, -0.295242f }, {
			-0.262866f, 0.951056f, -0.162460f }, {
			0.000000f, 1.000000f, 0.000000f }, {
			0.000000f, 0.955423f, 0.295242f }, {
			-0.262866f, 0.951056f, 0.162460f }, {
			0.238856f, 0.864188f, 0.442863f }, {
			0.262866f, 0.951056f, 0.162460f }, {
			0.500000f, 0.809017f, 0.309017f }, {
			0.238856f, 0.864188f, -0.442863f }, {
			0.262866f, 0.951056f, -0.162460f }, {
			0.500000f, 0.809017f, -0.309017f }, {
			0.850651f, 0.525731f, 0.000000f }, {
			0.716567f, 0.681718f, 0.147621f }, {
			0.716567f, 0.681718f, -0.147621f }, {
			0.525731f, 0.850651f, 0.000000f }, {
			0.425325f, 0.688191f, 0.587785f }, {
			0.864188f, 0.442863f, 0.238856f }, {
			0.688191f, 0.587785f, 0.425325f }, {
			0.809017f, 0.309017f, 0.500000f }, {
			0.681718f, 0.147621f, 0.716567f }, {
			0.587785f, 0.425325f, 0.688191f }, {
			0.955423f, 0.295242f, 0.000000f }, {
			1.000000f, 0.000000f, 0.000000f }, {
			0.951056f, 0.162460f, 0.262866f }, {
			0.850651f, -0.525731f, 0.000000f }, {
			0.955423f, -0.295242f, 0.000000f }, {
			0.864188f, -0.442863f, 0.238856f }, {
			0.951056f, -0.162460f, 0.262866f }, {
			0.809017f, -0.309017f, 0.500000f }, {
			0.681718f, -0.147621f, 0.716567f }, {
			0.850651f, 0.000000f, 0.525731f }, {
			0.864188f, 0.442863f, -0.238856f }, {
			0.809017f, 0.309017f, -0.500000f }, {
			0.951056f, 0.162460f, -0.262866f }, {
			0.525731f, 0.000000f, -0.850651f }, {
			0.681718f, 0.147621f, -0.716567f }, {
			0.681718f, -0.147621f, -0.716567f }, {
			0.850651f, 0.000000f, -0.525731f }, {
			0.809017f, -0.309017f, -0.500000f }, {
			0.864188f, -0.442863f, -0.238856f }, {
			0.951056f, -0.162460f, -0.262866f }, {
			0.147621f, 0.716567f, -0.681718f }, {
			0.309017f, 0.500000f, -0.809017f }, {
			0.425325f, 0.688191f, -0.587785f }, {
			0.442863f, 0.238856f, -0.864188f }, {
			0.587785f, 0.425325f, -0.688191f }, {
			0.688191f, 0.587785f, -0.425325f }, {
			-0.147621f, 0.716567f, -0.681718f }, {
			-0.309017f, 0.500000f, -0.809017f }, {
			0.000000f, 0.525731f, -0.850651f }, {
			-0.525731f, 0.000000f, -0.850651f }, {
			-0.442863f, 0.238856f, -0.864188f }, {
			-0.295242f, 0.000000f, -0.955423f }, {
			-0.162460f, 0.262866f, -0.951056f }, {
			0.000000f, 0.000000f, -1.000000f }, {
			0.295242f, 0.000000f, -0.955423f }, {
			0.162460f, 0.262866f, -0.951056f }, {
			-0.442863f, -0.238856f, -0.864188f }, {
			-0.309017f, -0.500000f, -0.809017f }, {
			-0.162460f, -0.262866f, -0.951056f }, {
			0.000000f, -0.850651f, -0.525731f }, {
			-0.147621f, -0.716567f, -0.681718f }, {
			0.147621f, -0.716567f, -0.681718f }, {
			0.000000f, -0.525731f, -0.850651f }, {
			0.309017f, -0.500000f, -0.809017f }, {
			0.442863f, -0.238856f, -0.864188f }, {
			0.162460f, -0.262866f, -0.951056f }, {
			0.238856f, -0.864188f, -0.442863f }, {
			0.500000f, -0.809017f, -0.309017f }, {
			0.425325f, -0.688191f, -0.587785f }, {
			0.716567f, -0.681718f, -0.147621f }, {
			0.688191f, -0.587785f, -0.425325f }, {
			0.587785f, -0.425325f, -0.688191f }, {
			0.000000f, -0.955423f, -0.295242f }, {
			0.000000f, -1.000000f, 0.000000f }, {
			0.262866f, -0.951056f, -0.162460f }, {
			0.000000f, -0.850651f, 0.525731f }, {
			0.000000f, -0.955423f, 0.295242f }, {
			0.238856f, -0.864188f, 0.442863f }, {
			0.262866f, -0.951056f, 0.162460f }, {
			0.500000f, -0.809017f, 0.309017f }, {
			0.716567f, -0.681718f, 0.147621f }, {
			0.525731f, -0.850651f, 0.000000f }, {
			-0.238856f, -0.864188f, -0.442863f }, {
			-0.500000f, -0.809017f, -0.309017f }, {
			-0.262866f, -0.951056f, -0.162460f }, {
			-0.850651f, -0.525731f, 0.000000f }, {
			-0.716567f, -0.681718f, -0.147621f }, {
			-0.716567f, -0.681718f, 0.147621f }, {
			-0.525731f, -0.850651f, 0.000000f }, {
			-0.500000f, -0.809017f, 0.309017f }, {
			-0.238856f, -0.864188f, 0.442863f }, {
			-0.262866f, -0.951056f, 0.162460f }, {
			-0.864188f, -0.442863f, 0.238856f }, {
			-0.809017f, -0.309017f, 0.500000f }, {
			-0.688191f, -0.587785f, 0.425325f }, {
			-0.681718f, -0.147621f, 0.716567f }, {
			-0.442863f, -0.238856f, 0.864188f }, {
			-0.587785f, -0.425325f, 0.688191f }, {
			-0.309017f, -0.500000f, 0.809017f }, {
			-0.147621f, -0.716567f, 0.681718f }, {
			-0.425325f, -0.688191f, 0.587785f }, {
			-0.162460f, -0.262866f, 0.951056f }, {
			0.442863f, -0.238856f, 0.864188f }, {
			0.162460f, -0.262866f, 0.951056f }, {
			0.309017f, -0.500000f, 0.809017f }, {
			0.147621f, -0.716567f, 0.681718f }, {
			0.000000f, -0.525731f, 0.850651f }, {
			0.425325f, -0.688191f, 0.587785f }, {
			0.587785f, -0.425325f, 0.688191f }, {
			0.688191f, -0.587785f, 0.425325f }, {
			-0.955423f, 0.295242f, 0.000000f }, {
			-0.951056f, 0.162460f, 0.262866f }, {
			-1.000000f, 0.000000f, 0.000000f }, {
			-0.850651f, 0.000000f, 0.525731f }, {
			-0.955423f, -0.295242f, 0.000000f }, {
			-0.951056f, -0.162460f, 0.262866f }, {
			-0.864188f, 0.442863f, -0.238856f }, {
			-0.951056f, 0.162460f, -0.262866f }, {
			-0.809017f, 0.309017f, -0.500000f }, {
			-0.864188f, -0.442863f, -0.238856f }, {
			-0.951056f, -0.162460f, -0.262866f }, {
			-0.809017f, -0.309017f, -0.500000f }, {
			-0.681718f, 0.147621f, -0.716567f }, {
			-0.681718f, -0.147621f, -0.716567f }, {
			-0.850651f, 0.000000f, -0.525731f }, {
			-0.688191f, 0.587785f, -0.425325f }, {
			-0.587785f, 0.425325f, -0.688191f }, {
			-0.425325f, 0.688191f, -0.587785f }, {
			-0.425325f, -0.688191f, -0.587785f }, {
			-0.587785f, -0.425325f, -0.688191f }, {
			-0.688191f, -0.587785f, -0.425325f }
	};

	public static boolean userinfo_modified = false;

	public static final console_t con = new console_t();
	public static TVar con_notifytime;
	public static viddef_t viddef = new viddef_t();
	// Renderer interface used by VID, SCR, ...
	public static refexport_t re = new DummyRenderer();

	public static String[] keybindings = new String[256];
	public static boolean[] keydown = new boolean[256];
	public static boolean chat_team = false;
	public static String chat_buffer = "";
	public static byte[][] key_lines = new byte[32][];
	public static int key_linepos;
	static {
		for (int i = 0; i < key_lines.length; i++)
			key_lines[i] = new byte[Defines.MAXCMDLINE];
	}

	public static int edit_line;

	public static TVar crosshair;
	public static vrect_t scr_vrect = new vrect_t();
	public static int sys_frame_time;
	public static int chat_bufferlen = 0;
	public static int gun_frame;
	public static model_t gun_model;
	public static netadr_t net_from = new netadr_t();
	
	// logfile
	public static RandomAccessFile logfile = null;
	
	public static float vec3_origin[] = { 0.0f, 0.0f, 0.0f };

	public static TVar m_filter;
	public static int vidref_val = VIDREF_GL;
	
	public static Random rnd = new Random();
}
