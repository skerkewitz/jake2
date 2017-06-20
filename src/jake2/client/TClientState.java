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

// Created on 27.11.2003 by RST.

package jake2.client;

import jake2.Defines;
import jake2.game.TCModel;
import jake2.game.TUserCmd;
import jake2.render.TImage;
import jake2.render.TModel;
import jake2.sound.TSound;

import java.nio.ByteBuffer;

public class TClientState {

	public TClientState() {
		for (int n = 0; n < Defines.CMD_BACKUP; n++)
			cmds[n] = new TUserCmd();
		for (int i = 0; i < frames.length; i++) {
			frames[i] = new TFrame();
		}

		for (int n = 0; n < Defines.MAX_CONFIGSTRINGS; n++)
			configstrings[n] = new String();
			
		for (int n=0; n < Defines.MAX_CLIENTS; n++)
			clientinfo[n] = new TClientInfo();
	}
	//
	//	   the TClientState structure is wiped completely at every
	//	   server map change
	//
	int timeoutcount;

	int timedemo_frames;
	int timedemo_start;

	public boolean refresh_prepped; // false if on new level or new ref dll
	public boolean sound_prepped; // ambient sounds can start
	boolean force_refdef; // vid has changed, so we can't use a paused refdef

	int parse_entities; // index (not anded off) into cl_parse_entities[]

	TUserCmd cmd = new TUserCmd();
	TUserCmd cmds[] = new TUserCmd[Defines.CMD_BACKUP]; // each mesage will send several old cmds

	int cmd_time[] = new int[Defines.CMD_BACKUP]; // time sent, for calculating pings
	short predicted_origins[][] = new short[Defines.CMD_BACKUP][3]; // for debug comparing against server

	float predicted_step; // for stair up smoothing
	int predicted_step_time;

	float[] predicted_origin ={0,0,0}; // generated by CL_PredictMovement
	float[] predicted_angles={0,0,0};
	float[] prediction_error={0,0,0};

	public TFrame frame = new TFrame(); // received from server
	int surpressCount; // number of messages rate supressed
	TFrame frames[] = new TFrame[Defines.UPDATE_BACKUP];

	// the client maintains its own idea of view angles, which are
	// sent to the server each frame.  It is cleared to 0 upon entering each level.
	// the server sends a delta each frame which is added to the locally
	// tracked view angles to account for standing on rotating objects,
	// and teleport direction changes
	public float[] viewangles = { 0, 0, 0 };

	public int time; // this is the time value that the client
	// is rendering at.  always <= cls.realtime
	float lerpfrac; // between oldframe and frame

	TRefDef refdef = new TRefDef();

	float[] v_forward = { 0, 0, 0 };
	float[] v_right = { 0, 0, 0 };
	float[] v_up = { 0, 0, 0 }; // assign when refdef.angles is assign

	//
	// transient data from server
	//

	String layout = ""; // general 2D overlay
	int inventory[] = new int[Defines.MAX_ITEMS];

	//
	// non-gameserver infornamtion
	// FIXME: move this cinematic stuff into the cin_t structure
	ByteBuffer cinematic_file;
	
	public int cinematictime; // cls.realtime for first cinematic frame
	int cinematicframe;
	byte cinematicpalette[] = new byte[768];
	public boolean cinematicpalette_active;

	//
	// server state information
	//
	public boolean attractloop; // running the attract loop, any key will menu
	public int servercount; // server identification for prespawns
	String gamedir ="";
	public int playernum;

	public String configstrings[] = new String[Defines.MAX_CONFIGSTRINGS];

	//
	// locally derived information from server state
	//
	TModel model_draw[] = new TModel[Defines.MAX_MODELS];
	TCModel model_clip[] = new TCModel[Defines.MAX_MODELS];

	public TSound sound_precache[] = new TSound[Defines.MAX_SOUNDS];
	TImage image_precache[] = new TImage[Defines.MAX_IMAGES];

	TClientInfo clientinfo[] = new TClientInfo[Defines.MAX_CLIENTS];
	TClientInfo baseclientinfo = new TClientInfo();

}
