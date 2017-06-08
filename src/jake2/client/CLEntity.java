/*
 * java
 * Copyright (C) 2004
 * 
 * $Id: CL_ents.java,v 1.10 2005-02-06 19:17:03 salomo Exp $
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
import jake2.game.*;
import jake2.io.FileSystem;
import jake2.qcommon.*;
import jake2.util.Math3D;

/**
 * CLEntity
 */
//	   cl_ents.c -- entity parsing and management
/*
 * =========================================================================
 * 
 * FRAME PARSING
 * 
 * =========================================================================
 */
public class CLEntity {

	static int bitcounts[] = new int[32]; /// just for protocol profiling

	static int bfg_lightramp[] = { 300, 400, 600, 300, 150, 75 };

	/*
	 * ================= CL_ParseEntityBits
	 * 
	 * Returns the entity number and the header bits =================
	 */
	public static int ParseEntityBits(int bits[]) {
		int b, total;
		int i;
		int number;

		total = TSizeBuffer.ReadByte(Context.net_message);
		if ((total & Defines.U_MOREBITS1) != 0) {
		    
			b = TSizeBuffer.ReadByte(Context.net_message);
			total |= b << 8;
		}
		if ((total & Defines.U_MOREBITS2) != 0) {
		    
			b = TSizeBuffer.ReadByte(Context.net_message);
			total |= b << 16;
		}
		if ((total & Defines.U_MOREBITS3) != 0) {
		    
			b = TSizeBuffer.ReadByte(Context.net_message);
			total |= b << 24;
		}

		// count the bits for net profiling
		for (i = 0; i < 32; i++)
			if ((total & (1 << i)) != 0)
				bitcounts[i]++;

		if ((total & Defines.U_NUMBER16) != 0)
			number = TSizeBuffer.ReadShort(Context.net_message);
		else
			number = TSizeBuffer.ReadByte(Context.net_message);

		bits[0] = total;

		return number;
	}

	/*
	 * ================== CL_ParseDelta
	 * 
	 * Can go from either a baseline or a previous packet_entity
	 * ==================
	 */
	public static void ParseDelta(TEntityState from, TEntityState to, int number, int bits) {
		// set everything to the state we are delta'ing from
		to.set(from);

		Math3D.VectorCopy(from.origin, to.old_origin);
		to.number = number;

		if ((bits & Defines.U_MODEL) != 0)
			to.modelIndex = TSizeBuffer.ReadByte(Context.net_message);
		if ((bits & Defines.U_MODEL2) != 0)
			to.modelindex2 = TSizeBuffer.ReadByte(Context.net_message);
		if ((bits & Defines.U_MODEL3) != 0)
			to.modelindex3 = TSizeBuffer.ReadByte(Context.net_message);
		if ((bits & Defines.U_MODEL4) != 0)
			to.modelindex4 = TSizeBuffer.ReadByte(Context.net_message);

		if ((bits & Defines.U_FRAME8) != 0)
			to.frame = TSizeBuffer.ReadByte(Context.net_message);
		if ((bits & Defines.U_FRAME16) != 0)
			to.frame = TSizeBuffer.ReadShort(Context.net_message);

		if ((bits & Defines.U_SKIN8) != 0 && (bits & Defines.U_SKIN16) != 0) //used
																			 // for
																			 // laser
																			 // colors
			to.skinnum = TSizeBuffer.ReadLong(Context.net_message);
		else if ((bits & Defines.U_SKIN8) != 0)
			to.skinnum = TSizeBuffer.ReadByte(Context.net_message);
		else if ((bits & Defines.U_SKIN16) != 0)
			to.skinnum = TSizeBuffer.ReadShort(Context.net_message);

		if ((bits & (Defines.U_EFFECTS8 | Defines.U_EFFECTS16)) == (Defines.U_EFFECTS8 | Defines.U_EFFECTS16))
			to.effects = TSizeBuffer.ReadLong(Context.net_message);
		else if ((bits & Defines.U_EFFECTS8) != 0)
			to.effects = TSizeBuffer.ReadByte(Context.net_message);
		else if ((bits & Defines.U_EFFECTS16) != 0)
			to.effects = TSizeBuffer.ReadShort(Context.net_message);

		if ((bits & (Defines.U_RENDERFX8 | Defines.U_RENDERFX16)) == (Defines.U_RENDERFX8 | Defines.U_RENDERFX16))
			to.renderfx = TSizeBuffer.ReadLong(Context.net_message);
		else if ((bits & Defines.U_RENDERFX8) != 0)
			to.renderfx = TSizeBuffer.ReadByte(Context.net_message);
		else if ((bits & Defines.U_RENDERFX16) != 0)
			to.renderfx = TSizeBuffer.ReadShort(Context.net_message);

		if ((bits & Defines.U_ORIGIN1) != 0)
			to.origin[0] = TSizeBuffer.ReadCoord(Context.net_message);
		if ((bits & Defines.U_ORIGIN2) != 0)
			to.origin[1] = TSizeBuffer.ReadCoord(Context.net_message);
		if ((bits & Defines.U_ORIGIN3) != 0)
			to.origin[2] = TSizeBuffer.ReadCoord(Context.net_message);

		if ((bits & Defines.U_ANGLE1) != 0)
			to.angles[0] = TSizeBuffer.ReadAngle(Context.net_message);
		if ((bits & Defines.U_ANGLE2) != 0)
			to.angles[1] = TSizeBuffer.ReadAngle(Context.net_message);
		if ((bits & Defines.U_ANGLE3) != 0)
			to.angles[2] = TSizeBuffer.ReadAngle(Context.net_message);

		if ((bits & Defines.U_OLDORIGIN) != 0)
			TSizeBuffer.ReadPos(Context.net_message, to.old_origin);

		if ((bits & Defines.U_SOUND) != 0)
			to.sound = TSizeBuffer.ReadByte(Context.net_message);

		if ((bits & Defines.U_EVENT) != 0)
			to.event = TSizeBuffer.ReadByte(Context.net_message);
		else
			to.event = 0;

		if ((bits & Defines.U_SOLID) != 0)
			to.solid = TSizeBuffer.ReadShort(Context.net_message);
	}

	/*
	 * ================== CL_DeltaEntity
	 * 
	 * Parses deltas from the given base and adds the resulting entity to the
	 * current frame ==================
	 */
	public static void DeltaEntity(TFrame frame, int newnum, TEntityState old, int bits) {
		TClEentity ent;
		TEntityState state;

		ent = Context.cl_entities[newnum];

		state = Context.cl_parse_entities[Context.cl.parse_entities & (Defines.MAX_PARSE_ENTITIES - 1)];
		Context.cl.parse_entities++;
		frame.num_entities++;

		ParseDelta(old, state, newnum, bits);

		// some data changes will force no lerping
		if (state.modelIndex != ent.current.modelIndex || state.modelindex2 != ent.current.modelindex2
				|| state.modelindex3 != ent.current.modelindex3 || state.modelindex4 != ent.current.modelindex4
				|| Math.abs(state.origin[0] - ent.current.origin[0]) > 512 || Math.abs(state.origin[1] - ent.current.origin[1]) > 512
				|| Math.abs(state.origin[2] - ent.current.origin[2]) > 512 || state.event == Defines.EV_PLAYER_TELEPORT
				|| state.event == Defines.EV_OTHER_TELEPORT) {
			ent.serverframe = -99;
		}

		if (ent.serverframe != Context.cl.frame.serverframe - 1) { // wasn't in
																   // last
																   // update, so
																   // initialize
																   // some
																   // things
			ent.trailcount = 1024; // for diminishing rocket / grenade trails
			// duplicate the current state so lerping doesn't hurt anything
			ent.prev.set(state);
			if (state.event == Defines.EV_OTHER_TELEPORT) {
				Math3D.VectorCopy(state.origin, ent.prev.origin);
				Math3D.VectorCopy(state.origin, ent.lerp_origin);
			} else {
				Math3D.VectorCopy(state.old_origin, ent.prev.origin);
				Math3D.VectorCopy(state.old_origin, ent.lerp_origin);
			}
		} else { // shuffle the last state to previous
			// Copy !
			ent.prev.set(ent.current);
		}

		ent.serverframe = Context.cl.frame.serverframe;
		// Copy !
		ent.current.set(state);
	}

	// call by reference
	private static final int[] iw = {0};
	/*
	 * ================== CL_ParsePacketEntities
	 * 
	 * An svc_packetentities has just been parsed, deal with the rest of the
	 * data stream. ==================
	 */
	public static void ParsePacketEntities(TFrame oldframe, TFrame newframe) {
		int newnum;
		int bits = 0;

		TEntityState oldstate = null;
		int oldnum;

		newframe.parse_entities = Context.cl.parse_entities;
		newframe.num_entities = 0;

		// delta from the entities present in oldframe
		int oldindex = 0;
		if (oldframe == null)
			oldnum = 99999;
		else {
			// oldindex == 0. hoz
			//			if (oldindex >= oldframe.num_entities)
			//				oldnum = 99999;
			//			else {
			oldstate = Context.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Defines.MAX_PARSE_ENTITIES - 1)];
			oldnum = oldstate.number;
			//			}
		}

		while (true) {
			//int iw[] = { bits };
			iw[0] = bits;
			newnum = ParseEntityBits(iw);
			bits = iw[0];

			if (newnum >= Defines.MAX_EDICTS)
				Command.Error(Defines.ERR_DROP, "CL_ParsePacketEntities: bad number:" + newnum);

			if (Context.net_message.readcount > Context.net_message.cursize)
				Command.Error(Defines.ERR_DROP, "CL_ParsePacketEntities: end of message");

			if (0 == newnum)
				break;

			while (oldnum < newnum) { // one or more entities from the old
									  // packet are unchanged
				if (Context.cl_shownet.value == 3)
					Command.Printf("   unchanged: " + oldnum + "\n");
				DeltaEntity(newframe, oldnum, oldstate, 0);

				oldindex++;

				if (oldindex >= oldframe.num_entities)
					oldnum = 99999;
				else {
					oldstate = Context.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Defines.MAX_PARSE_ENTITIES - 1)];
					oldnum = oldstate.number;
				}
			}

			if ((bits & Defines.U_REMOVE) != 0) { // the entity present in
												  // oldframe is not in the
												  // current frame
				if (Context.cl_shownet.value == 3)
					Command.Printf("   remove: " + newnum + "\n");
				if (oldnum != newnum)
					Command.Printf("U_REMOVE: oldnum != newnum\n");

				oldindex++;

				if (oldindex >= oldframe.num_entities)
					oldnum = 99999;
				else {
					oldstate = Context.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Defines.MAX_PARSE_ENTITIES - 1)];
					oldnum = oldstate.number;
				}
				continue;
			}

			if (oldnum == newnum) { // delta from previous state
				if (Context.cl_shownet.value == 3)
					Command.Printf("   delta: " + newnum + "\n");
				DeltaEntity(newframe, newnum, oldstate, bits);

				oldindex++;

				if (oldindex >= oldframe.num_entities)
					oldnum = 99999;
				else {
					oldstate = Context.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Defines.MAX_PARSE_ENTITIES - 1)];
					oldnum = oldstate.number;
				}
				continue;
			}

			if (oldnum > newnum) { // delta from baseline
				if (Context.cl_shownet.value == 3)
					Command.Printf("   baseline: " + newnum + "\n");
				DeltaEntity(newframe, newnum, Context.cl_entities[newnum].baseline, bits);
				continue;
			}

		}

		// any remaining entities in the old frame are copied over
		while (oldnum != 99999) { // one or more entities from the old packet
								  // are unchanged
			if (Context.cl_shownet.value == 3)
				Command.Printf("   unchanged: " + oldnum + "\n");
			DeltaEntity(newframe, oldnum, oldstate, 0);

			oldindex++;

			if (oldindex >= oldframe.num_entities)
				oldnum = 99999;
			else {
				oldstate = Context.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Defines.MAX_PARSE_ENTITIES - 1)];
				oldnum = oldstate.number;
			}
		}
	}

	/*
	 * =================== CL_ParsePlayerstate ===================
	 */
	public static void ParsePlayerstate(TFrame oldframe, TFrame newframe) {
		int flags;
		TPlayerState state;
		int i;
		int statbits;

		state = newframe.playerstate;

		// clear to old value before delta parsing
		if (oldframe != null)
			state.set(oldframe.playerstate);
		else
			//memset (state, 0, sizeof(*state));
			state.clear();

		flags = TSizeBuffer.ReadShort(Context.net_message);

		//
		// parse the pmove_state_t
		//
		if ((flags & Defines.PS_M_TYPE) != 0)
			state.pmove.pm_type = TSizeBuffer.ReadByte(Context.net_message);

		if ((flags & Defines.PS_M_ORIGIN) != 0) {
			state.pmove.origin[0] = TSizeBuffer.ReadShort(Context.net_message);
			state.pmove.origin[1] = TSizeBuffer.ReadShort(Context.net_message);
			state.pmove.origin[2] = TSizeBuffer.ReadShort(Context.net_message);
		}

		if ((flags & Defines.PS_M_VELOCITY) != 0) {
			state.pmove.velocity[0] = TSizeBuffer.ReadShort(Context.net_message);
			state.pmove.velocity[1] = TSizeBuffer.ReadShort(Context.net_message);
			state.pmove.velocity[2] = TSizeBuffer.ReadShort(Context.net_message);
		}

		if ((flags & Defines.PS_M_TIME) != 0) {
			state.pmove.pm_time = (byte) TSizeBuffer.ReadByte(Context.net_message);
		}

		if ((flags & Defines.PS_M_FLAGS) != 0)
			state.pmove.pm_flags = (byte) TSizeBuffer.ReadByte(Context.net_message);

		if ((flags & Defines.PS_M_GRAVITY) != 0)
			state.pmove.gravity = TSizeBuffer.ReadShort(Context.net_message);

		if ((flags & Defines.PS_M_DELTA_ANGLES) != 0) {
			state.pmove.delta_angles[0] = TSizeBuffer.ReadShort(Context.net_message);
			state.pmove.delta_angles[1] = TSizeBuffer.ReadShort(Context.net_message);
			state.pmove.delta_angles[2] = TSizeBuffer.ReadShort(Context.net_message);
		}

		if (Context.cl.attractloop)
			state.pmove.pm_type = Defines.PM_FREEZE; // demo playback

		//
		// parse the rest of the TPlayerState
		//
		if ((flags & Defines.PS_VIEWOFFSET) != 0) {
			state.viewoffset[0] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.viewoffset[1] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.viewoffset[2] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
		}

		if ((flags & Defines.PS_VIEWANGLES) != 0) {
			state.viewangles[0] = TSizeBuffer.ReadAngle16(Context.net_message);
			state.viewangles[1] = TSizeBuffer.ReadAngle16(Context.net_message);
			state.viewangles[2] = TSizeBuffer.ReadAngle16(Context.net_message);
		}

		if ((flags & Defines.PS_KICKANGLES) != 0) {

			state.kick_angles[0] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.kick_angles[1] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.kick_angles[2] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;

		}

		if ((flags & Defines.PS_WEAPONINDEX) != 0) {
			state.gunindex = TSizeBuffer.ReadByte(Context.net_message);
		}

		if ((flags & Defines.PS_WEAPONFRAME) != 0) {
			state.gunframe = TSizeBuffer.ReadByte(Context.net_message);
			state.gunoffset[0] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.gunoffset[1] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.gunoffset[2] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.gunangles[0] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.gunangles[1] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
			state.gunangles[2] = TSizeBuffer.ReadChar(Context.net_message) * 0.25f;
		}

		if ((flags & Defines.PS_BLEND) != 0) {
			state.blend[0] = TSizeBuffer.ReadByte(Context.net_message) / 255.0f;
			state.blend[1] = TSizeBuffer.ReadByte(Context.net_message) / 255.0f;
			state.blend[2] = TSizeBuffer.ReadByte(Context.net_message) / 255.0f;
			state.blend[3] = TSizeBuffer.ReadByte(Context.net_message) / 255.0f;
		}

		if ((flags & Defines.PS_FOV) != 0)
			state.fov = TSizeBuffer.ReadByte(Context.net_message);

		if ((flags & Defines.PS_RDFLAGS) != 0)
			state.rdflags = TSizeBuffer.ReadByte(Context.net_message);

		// parse stats
		statbits = TSizeBuffer.ReadLong(Context.net_message);
		for (i = 0; i < Defines.MAX_STATS; i++)
			if ((statbits & (1 << i)) != 0)
				state.stats[i] = TSizeBuffer.ReadShort(Context.net_message);
	}

	/*
	 * ================== CL_FireEntityEvents
	 * 
	 * ==================
	 */
	public static void FireEntityEvents(TFrame frame) {
		TEntityState s1;
		int pnum, num;

		for (pnum = 0; pnum < frame.num_entities; pnum++) {
			num = (frame.parse_entities + pnum) & (Defines.MAX_PARSE_ENTITIES - 1);
			s1 = Context.cl_parse_entities[num];
			if (s1.event != 0)
				CLEffects.EntityEvent(s1);

			// EF_TELEPORTER acts like an event, but is not cleared each frame
			if ((s1.effects & Defines.EF_TELEPORTER) != 0)
				CLEffects.TeleporterParticles(s1);
		}
	}

	/*
	 * ================ CL_ParseFrame ================
	 */
	public static void ParseFrame() {
		int cmd;
		int len;
		TFrame old;

		//memset( cl.frame, 0, sizeof(cl.frame));
		Context.cl.frame.reset();

		Context.cl.frame.serverframe = TSizeBuffer.ReadLong(Context.net_message);
		Context.cl.frame.deltaframe = TSizeBuffer.ReadLong(Context.net_message);
		Context.cl.frame.servertime = Context.cl.frame.serverframe * 100;

		// BIG HACK to let old demos continue to work
		if (Context.cls.getServerProtocol() != 26)
			Context.cl.surpressCount = TSizeBuffer.ReadByte(Context.net_message);

		if (Context.cl_shownet.value == 3)
			Command.Printf("   frame:" + Context.cl.frame.serverframe + "  delta:" + Context.cl.frame.deltaframe + "\n");

		// If the frame is delta compressed from data that we
		// no longer have available, we must suck up the rest of
		// the frame, but not use it, then ask for a non-compressed
		// message
		if (Context.cl.frame.deltaframe <= 0) {
			Context.cl.frame.valid = true; // uncompressed frame
			old = null;
			Context.cls.setDemowaiting(false); // we can start recording now
		} else {
			old = Context.cl.frames[Context.cl.frame.deltaframe & Defines.UPDATE_MASK];
			if (!old.valid) { // should never happen
				Command.Printf("Delta from invalid frame (not supposed to happen!).\n");
			}
			if (old.serverframe != Context.cl.frame.deltaframe) { // The frame
																  // that the
																  // server did
																  // the delta
																  // from
				// is too old, so we can't reconstruct it properly.
				Command.Printf("Delta frame too old.\n");
			} else if (Context.cl.parse_entities - old.parse_entities > Defines.MAX_PARSE_ENTITIES - 128) {
				Command.Printf("Delta parse_entities too old.\n");
			} else
				Context.cl.frame.valid = true; // valid delta parse
		}

		// clamp time
		if (Context.cl.time > Context.cl.frame.servertime)
			Context.cl.time = Context.cl.frame.servertime;
		else if (Context.cl.time < Context.cl.frame.servertime - 100)
			Context.cl.time = Context.cl.frame.servertime - 100;

		// read areaBits
		len = TSizeBuffer.ReadByte(Context.net_message);
		TSizeBuffer.ReadData(Context.net_message, Context.cl.frame.areabits, len);

		// read playerinfo
		cmd = TSizeBuffer.ReadByte(Context.net_message);
		CL_parse.SHOWNET(CL_parse.svc_strings[cmd]);
		if (cmd != Defines.svc_playerinfo)
			Command.Error(Defines.ERR_DROP, "CL_ParseFrame: not playerinfo");
		ParsePlayerstate(old, Context.cl.frame);

		// read packet entities
		cmd = TSizeBuffer.ReadByte(Context.net_message);
		CL_parse.SHOWNET(CL_parse.svc_strings[cmd]);
		if (cmd != Defines.svc_packetentities)
			Command.Error(Defines.ERR_DROP, "CL_ParseFrame: not packetentities");

		ParsePacketEntities(old, Context.cl.frame);

		// save the frame off in the backup array for later delta comparisons
		Context.cl.frames[Context.cl.frame.serverframe & Defines.UPDATE_MASK].set(Context.cl.frame);

		if (Context.cl.frame.valid) {
			// getting a valid frame message ends the connection process
			if (Context.cls.getState() != Defines.ca_active) {
				Context.cls.setState(Defines.ca_active);
				Context.cl.force_refdef = true;

				Context.cl.predicted_origin[0] = Context.cl.frame.playerstate.pmove.origin[0] * 0.125f;
				Context.cl.predicted_origin[1] = Context.cl.frame.playerstate.pmove.origin[1] * 0.125f;
				Context.cl.predicted_origin[2] = Context.cl.frame.playerstate.pmove.origin[2] * 0.125f;

				Math3D.VectorCopy(Context.cl.frame.playerstate.viewangles, Context.cl.predicted_angles);
				if (Context.cls.getDisableServerCount() != Context.cl.servercount && Context.cl.refresh_prepped)
					SCR.EndLoadingPlaque(); // get rid of loading plaque
			}
			Context.cl.sound_prepped = true; // can start mixing ambient sounds

			// fire entity events
			FireEntityEvents(Context.cl.frame);
			CL_pred.CheckPredictionError();
		}
	}

	/*
	 * ==========================================================================
	 * 
	 * INTERPOLATE BETWEEN FRAMES TO GET RENDERING PARMS
	 * 
	 * ==========================================================================
	 */

	// stack variable
	private static final TEntity ent = new TEntity();
	/*
	 * =============== 
	 * CL_AddPacketEntities
	 * ===============
	 */
	static void AddPacketEntities(TFrame frame) {
		TEntityState s1;
		float autorotate;
		int i;
		int pnum;
		TClEentity cent;
		int autoanim;
		TClientInfo ci;
		int effects, renderfx;

		// bonus items rotate at a fixed rate
		autorotate = Math3D.anglemod(Context.cl.time / 10);

		// brush models can auto animate their frames
		autoanim = 2 * Context.cl.time / 1000;

		//memset( entityDict, 0, sizeof(entityDict));
		ent.clear();

		for (pnum = 0; pnum < frame.num_entities; pnum++) {
			s1 = Context.cl_parse_entities[(frame.parse_entities + pnum) & (Defines.MAX_PARSE_ENTITIES - 1)];

			cent = Context.cl_entities[s1.number];

			effects = s1.effects;
			renderfx = s1.renderfx;

			// set frame
			if ((effects & Defines.EF_ANIM01) != 0)
				ent.frame = autoanim & 1;
			else if ((effects & Defines.EF_ANIM23) != 0)
				ent.frame = 2 + (autoanim & 1);
			else if ((effects & Defines.EF_ANIM_ALL) != 0)
				ent.frame = autoanim;
			else if ((effects & Defines.EF_ANIM_ALLFAST) != 0)
				ent.frame = Context.cl.time / 100;
			else
				ent.frame = s1.frame;

			// quad and pent can do different things on client
			if ((effects & Defines.EF_PENT) != 0) {
				effects &= ~Defines.EF_PENT;
				effects |= Defines.EF_COLOR_SHELL;
				renderfx |= Defines.RF_SHELL_RED;
			}

			if ((effects & Defines.EF_QUAD) != 0) {
				effects &= ~Defines.EF_QUAD;
				effects |= Defines.EF_COLOR_SHELL;
				renderfx |= Defines.RF_SHELL_BLUE;
			}
			//	  ======
			//	   PMM
			if ((effects & Defines.EF_DOUBLE) != 0) {
				effects &= ~Defines.EF_DOUBLE;
				effects |= Defines.EF_COLOR_SHELL;
				renderfx |= Defines.RF_SHELL_DOUBLE;
			}

			if ((effects & Defines.EF_HALF_DAMAGE) != 0) {
				effects &= ~Defines.EF_HALF_DAMAGE;
				effects |= Defines.EF_COLOR_SHELL;
				renderfx |= Defines.RF_SHELL_HALF_DAM;
			}
			//	   pmm
			//	  ======
			ent.oldframe = cent.prev.frame;
			ent.backlerp = 1.0f - Context.cl.lerpfrac;

			if ((renderfx & (Defines.RF_FRAMELERP | Defines.RF_BEAM)) != 0) {
				// step origin discretely, because the frames
				// do the animation properly
				Math3D.VectorCopy(cent.current.origin, ent.origin);
				Math3D.VectorCopy(cent.current.old_origin, ent.oldorigin);
			} else { // interpolate origin
				for (i = 0; i < 3; i++) {
					ent.origin[i] = ent.oldorigin[i] = cent.prev.origin[i] + Context.cl.lerpfrac
							* (cent.current.origin[i] - cent.prev.origin[i]);
				}
			}

			// create a new entity

			// tweak the color of beams
			if ((renderfx & Defines.RF_BEAM) != 0) { // the four beam colors are
													 // encoded in 32 bits of
													 // skinnum (hack)
				ent.alpha = 0.30f;
				ent.skinnum = (s1.skinnum >> ((Context.rnd.nextInt(4)) * 8)) & 0xff;
				Math.random();
				ent.model = null;
			} else {
				// set skin
				if (s1.modelIndex == 255) { // use custom player skin
					ent.skinnum = 0;
					ci = Context.cl.clientinfo[s1.skinnum & 0xff];
					ent.skin = ci.skin;
					ent.model = ci.model;
					if (null == ent.skin || null == ent.model) {
						ent.skin = Context.cl.baseclientinfo.skin;
						ent.model = Context.cl.baseclientinfo.model;
					}

					//	  ============
					//	  PGM
					if ((renderfx & Defines.RF_USE_DISGUISE) != 0) {
						if (ent.skin.name.startsWith("players/male")) {
							ent.skin = Context.re.RegisterSkin("players/male/disguise.pcx");
							ent.model = Context.re.RegisterModel("players/male/tris.md2");
						} else if (ent.skin.name.startsWith("players/female")) {
							ent.skin = Context.re.RegisterSkin("players/female/disguise.pcx");
							ent.model = Context.re.RegisterModel("players/female/tris.md2");
						} else if (ent.skin.name.startsWith("players/cyborg")) {
							ent.skin = Context.re.RegisterSkin("players/cyborg/disguise.pcx");
							ent.model = Context.re.RegisterModel("players/cyborg/tris.md2");
						}
					}
					//	  PGM
					//	  ============
				} else {
					ent.skinnum = s1.skinnum;
					ent.skin = null;
					ent.model = Context.cl.model_draw[s1.modelIndex];
				}
			}

			// only used for black hole model right now, FIXME: do better
			if (renderfx == Defines.RF_TRANSLUCENT)
				ent.alpha = 0.70f;

			// render effects (fullbright, translucent, etc)
			if ((effects & Defines.EF_COLOR_SHELL) != 0)
				ent.flags = 0; // renderfx go on color shell entity
			else
				ent.flags = renderfx;

			// calculate angles
			if ((effects & Defines.EF_ROTATE) != 0) { // some bonus items
													  // auto-rotate
				ent.angles[0] = 0;
				ent.angles[1] = autorotate;
				ent.angles[2] = 0;
			}
			// RAFAEL
			else if ((effects & Defines.EF_SPINNINGLIGHTS) != 0) {
				ent.angles[0] = 0;
				ent.angles[1] = Math3D.anglemod(Context.cl.time / 2) + s1.angles[1];
				ent.angles[2] = 180;
				{
					float[] forward = { 0, 0, 0 };
					float[] start = { 0, 0, 0 };

					Math3D.AngleVectors(ent.angles, forward, null, null);
					Math3D.VectorMA(ent.origin, 64, forward, start);
					V.AddLight(start, 100, 1, 0, 0);
				}
			} else { // interpolate angles
				float a1, a2;

				for (i = 0; i < 3; i++) {
					a1 = cent.current.angles[i];
					a2 = cent.prev.angles[i];
					ent.angles[i] = Math3D.LerpAngle(a2, a1, Context.cl.lerpfrac);
				}
			}

			if (s1.number == Context.cl.playernum + 1) {
				ent.flags |= Defines.RF_VIEWERMODEL; // only draw from mirrors
				// FIXME: still pass to refresh

				if ((effects & Defines.EF_FLAG1) != 0)
					V.AddLight(ent.origin, 225, 1.0f, 0.1f, 0.1f);
				else if ((effects & Defines.EF_FLAG2) != 0)
					V.AddLight(ent.origin, 225, 0.1f, 0.1f, 1.0f);
				else if ((effects & Defines.EF_TAGTRAIL) != 0) //PGM
					V.AddLight(ent.origin, 225, 1.0f, 1.0f, 0.0f); //PGM
				else if ((effects & Defines.EF_TRACKERTRAIL) != 0) //PGM
					V.AddLight(ent.origin, 225, -1.0f, -1.0f, -1.0f); //PGM

				continue;
			}

			// if set to invisible, skip
			if (s1.modelIndex == 0)
				continue;

			if ((effects & Defines.EF_BFG) != 0) {
				ent.flags |= Defines.RF_TRANSLUCENT;
				ent.alpha = 0.30f;
			}

			// RAFAEL
			if ((effects & Defines.EF_PLASMA) != 0) {
				ent.flags |= Defines.RF_TRANSLUCENT;
				ent.alpha = 0.6f;
			}

			if ((effects & Defines.EF_SPHERETRANS) != 0) {
				ent.flags |= Defines.RF_TRANSLUCENT;
				// PMM - *sigh* yet more EF overloading
				if ((effects & Defines.EF_TRACKERTRAIL) != 0)
					ent.alpha = 0.6f;
				else
					ent.alpha = 0.3f;
			}
			//	  pmm

			// add to refresh list
			V.AddEntity(ent);

			// color shells generate a seperate entity for the main model
			if ((effects & Defines.EF_COLOR_SHELL) != 0) {
				/*
				 * PMM - at this point, all of the shells have been handled if
				 * we're in the rogue pack, set up the custom mixing, otherwise
				 * just keep going if(Developer_searchpath(2) == 2) { all of the
				 * solo colors are fine. we need to catch any of the
				 * combinations that look bad (double & half) and turn them into
				 * the appropriate color, and make double/quad something special
				 *  
				 */
				if ((renderfx & Defines.RF_SHELL_HALF_DAM) != 0) {
					if (FileSystem.Developer_searchpath(2) == 2) {
						// ditch the half damage shell if any of red, blue, or
						// double are on
						if ((renderfx & (Defines.RF_SHELL_RED | Defines.RF_SHELL_BLUE | Defines.RF_SHELL_DOUBLE)) != 0)
							renderfx &= ~Defines.RF_SHELL_HALF_DAM;
					}
				}

				if ((renderfx & Defines.RF_SHELL_DOUBLE) != 0) {
					if (FileSystem.Developer_searchpath(2) == 2) {
						// lose the yellow shell if we have a red, blue, or
						// green shell
						if ((renderfx & (Defines.RF_SHELL_RED | Defines.RF_SHELL_BLUE | Defines.RF_SHELL_GREEN)) != 0)
							renderfx &= ~Defines.RF_SHELL_DOUBLE;
						// if we have a red shell, turn it to purple by adding
						// blue
						if ((renderfx & Defines.RF_SHELL_RED) != 0)
							renderfx |= Defines.RF_SHELL_BLUE;
						// if we have a blue shell (and not a red shell), turn
						// it to cyan by adding green
						else if ((renderfx & Defines.RF_SHELL_BLUE) != 0)
							// go to green if it'entityState on already, otherwise do cyan
							// (flash green)
							if ((renderfx & Defines.RF_SHELL_GREEN) != 0)
								renderfx &= ~Defines.RF_SHELL_BLUE;
							else
								renderfx |= Defines.RF_SHELL_GREEN;
					}
				}
				//				}
				// pmm
				ent.flags = renderfx | Defines.RF_TRANSLUCENT;
				ent.alpha = 0.30f;
				V.AddEntity(ent);
			}

			ent.skin = null; // never use a custom skin on others
			ent.skinnum = 0;
			ent.flags = 0;
			ent.alpha = 0;

			// duplicate for linked models
			if (s1.modelindex2 != 0) {
				if (s1.modelindex2 == 255) { // custom weapon
					ci = Context.cl.clientinfo[s1.skinnum & 0xff];
					i = (s1.skinnum >> 8); // 0 is default weapon model
					if (0 == Context.cl_vwep.value || i > Defines.MAX_CLIENTWEAPONMODELS - 1)
						i = 0;
					ent.model = ci.weaponmodel[i];
					if (null == ent.model) {
						if (i != 0)
							ent.model = ci.weaponmodel[0];
						if (null == ent.model)
							ent.model = Context.cl.baseclientinfo.weaponmodel[0];
					}
				} else
					ent.model = Context.cl.model_draw[s1.modelindex2];

				// PMM - check for the defender sphere shell .. make it
				// translucent
				// replaces the previous version which used the high bit on
				// modelindex2 to determine transparency
				if (Context.cl.configstrings[Defines.CS_MODELS + (s1.modelindex2)].equalsIgnoreCase("models/items/shell/tris.md2")) {
					ent.alpha = 0.32f;
					ent.flags = Defines.RF_TRANSLUCENT;
				}
				// pmm

				V.AddEntity(ent);

				//PGM - make sure these get reset.
				ent.flags = 0;
				ent.alpha = 0;
				//PGM
			}
			if (s1.modelindex3 != 0) {
				ent.model = Context.cl.model_draw[s1.modelindex3];
				V.AddEntity(ent);
			}
			if (s1.modelindex4 != 0) {
				ent.model = Context.cl.model_draw[s1.modelindex4];
				V.AddEntity(ent);
			}

			if ((effects & Defines.EF_POWERSCREEN) != 0) {
				ent.model = CL_tent.cl_mod_powerscreen;
				ent.oldframe = 0;
				ent.frame = 0;
				ent.flags |= (Defines.RF_TRANSLUCENT | Defines.RF_SHELL_GREEN);
				ent.alpha = 0.30f;
				V.AddEntity(ent);
			}

			// add automatic particle trails
			if ((effects & ~Defines.EF_ROTATE) != 0) {
				if ((effects & Defines.EF_ROCKET) != 0) {
					CLEffects.RocketTrail(cent.lerp_origin, ent.origin, cent);
					V.AddLight(ent.origin, 200, 1, 1, 0);
				}
				// PGM - Do not reorder EF_BLASTER and EF_HYPERBLASTER.
				// EF_BLASTER | EF_TRACKER is a special case for EF_BLASTER2...
				// Cheese!
				else if ((effects & Defines.EF_BLASTER) != 0) {
					//					CL_BlasterTrail (cent.lerp_origin, entityDict.origin);
					//	  PGM
					if ((effects & Defines.EF_TRACKER) != 0) // lame...
															 // problematic?
					{
						CLNewEffects.BlasterTrail2(cent.lerp_origin, ent.origin);
						V.AddLight(ent.origin, 200, 0, 1, 0);
					} else {
						CLEffects.BlasterTrail(cent.lerp_origin, ent.origin);
						V.AddLight(ent.origin, 200, 1, 1, 0);
					}
					//	  PGM
				} else if ((effects & Defines.EF_HYPERBLASTER) != 0) {
					if ((effects & Defines.EF_TRACKER) != 0) // PGM overloaded
															 // for blaster2.
						V.AddLight(ent.origin, 200, 0, 1, 0); // PGM
					else
						// PGM
						V.AddLight(ent.origin, 200, 1, 1, 0);
				} else if ((effects & Defines.EF_GIB) != 0) {
					CLEffects.DiminishingTrail(cent.lerp_origin, ent.origin, cent, effects);
				} else if ((effects & Defines.EF_GRENADE) != 0) {
					CLEffects.DiminishingTrail(cent.lerp_origin, ent.origin, cent, effects);
				} else if ((effects & Defines.EF_FLIES) != 0) {
					CLEffects.FlyEffect(cent, ent.origin);
				} else if ((effects & Defines.EF_BFG) != 0) {

					if ((effects & Defines.EF_ANIM_ALLFAST) != 0) {
						CLEffects.BfgParticles(ent);
						i = 200;
					} else {
						i = bfg_lightramp[s1.frame];
					}
					V.AddLight(ent.origin, i, 0, 1, 0);
				}
				// RAFAEL
				else if ((effects & Defines.EF_TRAP) != 0) {
					ent.origin[2] += 32;
					CLEffects.TrapParticles(ent);
					i = (Context.rnd.nextInt(100)) + 100;
					V.AddLight(ent.origin, i, 1, 0.8f, 0.1f);
				} else if ((effects & Defines.EF_FLAG1) != 0) {
					CLEffects.FlagTrail(cent.lerp_origin, ent.origin, 242);
					V.AddLight(ent.origin, 225, 1, 0.1f, 0.1f);
				} else if ((effects & Defines.EF_FLAG2) != 0) {
					CLEffects.FlagTrail(cent.lerp_origin, ent.origin, 115);
					V.AddLight(ent.origin, 225, 0.1f, 0.1f, 1);
				}
				//	  ======
				//	  ROGUE
				else if ((effects & Defines.EF_TAGTRAIL) != 0) {
					CLNewEffects.TagTrail(cent.lerp_origin, ent.origin, 220);
					V.AddLight(ent.origin, 225, 1.0f, 1.0f, 0.0f);
				} else if ((effects & Defines.EF_TRACKERTRAIL) != 0) {
					if ((effects & Defines.EF_TRACKER) != 0) {
						float intensity;

						intensity = (float) (50 + (500 * (Math.sin(Context.cl.time / 500.0) + 1.0)));
						// FIXME - check out this effect in rendition
						if (Context.vidref_val == Defines.VIDREF_GL)
							V.AddLight(ent.origin, intensity, -1.0f, -1.0f, -1.0f);
						else
							V.AddLight(ent.origin, -1.0f * intensity, 1.0f, 1.0f, 1.0f);
					} else {
						CLNewEffects.Tracker_Shell(cent.lerp_origin);
						V.AddLight(ent.origin, 155, -1.0f, -1.0f, -1.0f);
					}
				} else if ((effects & Defines.EF_TRACKER) != 0) {
					CLNewEffects.TrackerTrail(cent.lerp_origin, ent.origin, 0);
					// FIXME - check out this effect in rendition
					if (Context.vidref_val == Defines.VIDREF_GL)
						V.AddLight(ent.origin, 200, -1, -1, -1);
					else
						V.AddLight(ent.origin, -200, 1, 1, 1);
				}
				//	  ROGUE
				//	  ======
				// RAFAEL
				else if ((effects & Defines.EF_GREENGIB) != 0) {
					CLEffects.DiminishingTrail(cent.lerp_origin, ent.origin, cent, effects);
				}
				// RAFAEL
				else if ((effects & Defines.EF_IONRIPPER) != 0) {
					CLEffects.IonripperTrail(cent.lerp_origin, ent.origin);
					V.AddLight(ent.origin, 100, 1, 0.5f, 0.5f);
				}
				// RAFAEL
				else if ((effects & Defines.EF_BLUEHYPERBLASTER) != 0) {
					V.AddLight(ent.origin, 200, 0, 0, 1);
				}
				// RAFAEL
				else if ((effects & Defines.EF_PLASMA) != 0) {
					if ((effects & Defines.EF_ANIM_ALLFAST) != 0) {
						CLEffects.BlasterTrail(cent.lerp_origin, ent.origin);
					}
					V.AddLight(ent.origin, 130, 1, 0.5f, 0.5f);
				}
			}

			Math3D.VectorCopy(ent.origin, cent.lerp_origin);
		}
	}
	
	// stack variable
	private static final TEntity gun = new TEntity();
	/*
	 * ============== CL_AddViewWeapon ==============
	 */
	static void AddViewWeapon(TPlayerState ps, TPlayerState ops) {
		int i;

		// allow the gun to be completely removed
		if (0 == Context.cl_gun.value)
			return;

		// don't draw gun if in wide angle view
		if (ps.fov > 90)
			return;

		//memset( gun, 0, sizeof(gun));
		gun.clear();

		if (Context.gun_model != null)
			gun.model = Context.gun_model; // development tool
		else
			gun.model = Context.cl.model_draw[ps.gunindex];

		if (gun.model == null)
			return;

		// set up gun position
		for (i = 0; i < 3; i++) {
			gun.origin[i] = Context.cl.refdef.vieworg[i] + ops.gunoffset[i] + Context.cl.lerpfrac
					* (ps.gunoffset[i] - ops.gunoffset[i]);
			gun.angles[i] = Context.cl.refdef.viewangles[i] + Math3D.LerpAngle(ops.gunangles[i], ps.gunangles[i], Context.cl.lerpfrac);
		}

		if (Context.gun_frame != 0) {
			gun.frame = Context.gun_frame; // development tool
			gun.oldframe = Context.gun_frame; // development tool
		} else {
			gun.frame = ps.gunframe;
			if (gun.frame == 0)
				gun.oldframe = 0; // just changed weapons, don't lerp from old
			else
				gun.oldframe = ops.gunframe;
		}

		gun.flags = Defines.RF_MINLIGHT | Defines.RF_DEPTHHACK | Defines.RF_WEAPONMODEL;
		gun.backlerp = 1.0f - Context.cl.lerpfrac;
		Math3D.VectorCopy(gun.origin, gun.oldorigin); // don't lerp at all
		V.AddEntity(gun);
	}

	/*
	 * =============== CL_CalcViewValues
	 * 
	 * Sets cl.refdef view values ===============
	 */
	static void CalcViewValues() {
		int i;
		float lerp, backlerp;
		TFrame oldframe;
		TPlayerState ps, ops;

		// find the previous frame to interpolate from
		ps = Context.cl.frame.playerstate;

		i = (Context.cl.frame.serverframe - 1) & Defines.UPDATE_MASK;
		oldframe = Context.cl.frames[i];

		if (oldframe.serverframe != Context.cl.frame.serverframe - 1 || !oldframe.valid)
			oldframe = Context.cl.frame; // previous frame was dropped or
										 // involid
		ops = oldframe.playerstate;

		// see if the player entity was teleported this frame
		if (Math.abs(ops.pmove.origin[0] - ps.pmove.origin[0]) > 256 * 8
				|| Math.abs(ops.pmove.origin[1] - ps.pmove.origin[1]) > 256 * 8
				|| Math.abs(ops.pmove.origin[2] - ps.pmove.origin[2]) > 256 * 8)
			ops = ps; // don't interpolate

		lerp = Context.cl.lerpfrac;

		// calculate the origin
		if ((Context.cl_predict.value != 0) && 0 == (Context.cl.frame.playerstate.pmove.pm_flags & pmove_t.PMF_NO_PREDICTION)) { // use
																																 // predicted
																																 // values
			int delta;

			backlerp = 1.0f - lerp;
			for (i = 0; i < 3; i++) {
				Context.cl.refdef.vieworg[i] = Context.cl.predicted_origin[i] + ops.viewoffset[i] + Context.cl.lerpfrac
						* (ps.viewoffset[i] - ops.viewoffset[i]) - backlerp * Context.cl.prediction_error[i];
			}

			// smooth out stair climbing
			delta = Context.cls.getRealtime() - Context.cl.predicted_step_time;
			if (delta < 100)
				Context.cl.refdef.vieworg[2] -= Context.cl.predicted_step * (100 - delta) * 0.01;
		} else { // just use interpolated values
			for (i = 0; i < 3; i++)
				Context.cl.refdef.vieworg[i] = ops.pmove.origin[i] * 0.125f + ops.viewoffset[i] + lerp
						* (ps.pmove.origin[i] * 0.125f + ps.viewoffset[i] - (ops.pmove.origin[i] * 0.125f + ops.viewoffset[i]));
		}

		// if not running a demo or on a locked frame, add the local angle
		// movement
		if (Context.cl.frame.playerstate.pmove.pm_type < Defines.PM_DEAD) { // use
																			// predicted
																			// values
			for (i = 0; i < 3; i++)
				Context.cl.refdef.viewangles[i] = Context.cl.predicted_angles[i];
		} else { // just use interpolated values
			for (i = 0; i < 3; i++)
				Context.cl.refdef.viewangles[i] = Math3D.LerpAngle(ops.viewangles[i], ps.viewangles[i], lerp);
		}

		for (i = 0; i < 3; i++)
			Context.cl.refdef.viewangles[i] += Math3D.LerpAngle(ops.kick_angles[i], ps.kick_angles[i], lerp);

		Math3D.AngleVectors(Context.cl.refdef.viewangles, Context.cl.v_forward, Context.cl.v_right, Context.cl.v_up);

		// interpolate field of view
		Context.cl.refdef.fov_x = ops.fov + lerp * (ps.fov - ops.fov);

		// don't interpolate blend color
		for (i = 0; i < 4; i++)
			Context.cl.refdef.blend[i] = ps.blend[i];

		// add the weapon
		AddViewWeapon(ps, ops);
	}

	/*
	 * =============== CL_AddEntities
	 * 
	 * Emits all entities, particles, and lights to the refresh ===============
	 */
	static void AddEntities() {
		if (Context.cls.getState() != Defines.ca_active)
			return;

		if (Context.cl.time > Context.cl.frame.servertime) {
			if (Context.cl_showclamp.value != 0)
				Command.Printf("high clamp " + (Context.cl.time - Context.cl.frame.servertime) + "\n");
			Context.cl.time = Context.cl.frame.servertime;
			Context.cl.lerpfrac = 1.0f;
		} else if (Context.cl.time < Context.cl.frame.servertime - 100) {
			if (Context.cl_showclamp.value != 0)
				Command.Printf("low clamp " + (Context.cl.frame.servertime - 100 - Context.cl.time) + "\n");
			Context.cl.time = Context.cl.frame.servertime - 100;
			Context.cl.lerpfrac = 0;
		} else
			Context.cl.lerpfrac = 1.0f - (Context.cl.frame.servertime - Context.cl.time) * 0.01f;

		if (Context.cl_timedemo.value != 0)
			Context.cl.lerpfrac = 1.0f;

		/*
		 * is ok.. CL_AddPacketEntities (cl.frame); CL_AddTEnts ();
		 * CL_AddParticles (); CL_AddDLights (); CL_AddLightStyles ();
		 */

		CalcViewValues();
		// PMM - moved this here so the heat beam has the right values for the
		// vieworg, and can lock the beam to the gun
		AddPacketEntities(Context.cl.frame);

		CL_tent.AddTEnts();
		CLEffects.AddParticles();
		CLEffects.AddDLights();
		CLEffects.AddLightStyles();
	}

	/*
	 * =============== CL_GetEntitySoundOrigin
	 * 
	 * Called to get the sound spatialization origin ===============
	 */
	public static void GetEntitySoundOrigin(int ent, float[] org) {
		TClEentity old;

		if (ent < 0 || ent >= Defines.MAX_EDICTS)
			Command.Error(Defines.ERR_DROP, "CL_GetEntitySoundOrigin: bad entityDict");
		old = Context.cl_entities[ent];
		Math3D.VectorCopy(old.lerp_origin, org);

		// FIXME: bmodel issues...
	}
}