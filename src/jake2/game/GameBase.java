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

// Created on 30.11.2003 by RST.

// $Id: GameBase.java,v 1.13 2006-01-21 21:53:31 salomo Exp $

/** Father of all GameObjects. */

package jake2.game;

import java.util.StringTokenizer;

import jake2.*;
import jake2.client.Monster;
import jake2.qcommon.Command;
import jake2.server.*;
import jake2.util.*;

public class GameBase {
    public static cplane_t dummyplane = new cplane_t();

    public static game_locals_t game = new game_locals_t();

    public static level_locals_t level = new level_locals_t();

    public static TGameImport gi = new TGameImport();

    public static spawn_temp_t st = new spawn_temp_t();

    public static int sm_meat_index;

    public static int snd_fry;

    public static int meansOfDeath;

    public static int num_edicts;

    public static TEntityDict entityDicts[] = new TEntityDict[Defines.MAX_EDICTS];
    static {
        for (int n = 0; n < Defines.MAX_EDICTS; n++)
            entityDicts[n] = new TEntityDict(n);
    }

    public static TVar deathmatch = new TVar();

    public static TVar coop = new TVar();

    public static TVar dmflags = new TVar();

    public static TVar skill; // = new TVar();

    public static TVar fraglimit = new TVar();

    public static TVar timelimit = new TVar();

    public static TVar password = new TVar();

    public static TVar spectator_password = new TVar();

    public static TVar needpass = new TVar();

    public static TVar maxclients = new TVar();

    public static TVar maxspectators = new TVar();

    public static TVar maxentities = new TVar();

    public static TVar g_select_empty = new TVar();

    public static TVar filterban = new TVar();

    public static TVar sv_maxvelocity = new TVar();

    public static TVar sv_gravity = new TVar();

    public static TVar sv_rollspeed = new TVar();

    public static TVar sv_rollangle = new TVar();

    public static TVar gun_x = new TVar();

    public static TVar gun_y = new TVar();

    public static TVar gun_z = new TVar();

    public static TVar run_pitch = new TVar();

    public static TVar run_roll = new TVar();

    public static TVar bob_up = new TVar();

    public static TVar bob_pitch = new TVar();

    public static TVar bob_roll = new TVar();

    public static TVar sv_cheats = new TVar();

    public static TVar flood_msgs = new TVar();

    public static TVar flood_persecond = new TVar();

    public static TVar flood_waitdelay = new TVar();

    public static TVar sv_maplist = new TVar();

    public final static float STOP_EPSILON = 0.1f;

    /**
     * Slide off of the impacting object returns the blocked flags (1 = floor, 2 =
     * step / wall).
     */
    public static int ClipVelocity(float[] in, float[] normal, float[] out,
            float overbounce) {
        float backoff;
        float change;
        int i, blocked;

        blocked = 0;
        if (normal[2] > 0)
            blocked |= 1; // floor
        if (normal[2] == 0.0f)
            blocked |= 2; // step

        backoff = Math3D.DotProduct(in, normal) * overbounce;

        for (i = 0; i < 3; i++) {
            change = normal[i] * backoff;
            out[i] = in[i] - change;
            if (out[i] > -STOP_EPSILON && out[i] < STOP_EPSILON)
                out[i] = 0;
        }

        return blocked;
    }


    /**
     * Searches all active entities for the next one that holds the matching
     * string at fieldofs (use the FOFS() macro) in the structure.
     * 
     * Searches beginning at the edict after from, or the beginning if null null
     * will be returned if the end of the list is reached.
     * 
     */

    public static EdictIterator G_Find(EdictIterator from, EdictFindFilter eff,
            String s) {

        if (from == null)
            from = new EdictIterator(0);
        else
            from.i++;

        for (; from.i < num_edicts; from.i++) {
            from.o = entityDicts[from.i];
            if (from.o.classname == null) {
                Command.Printf("edict with classname = null" + from.o.index);
            }

            if (!from.o.inUse)
                continue;

            if (eff.matches(from.o, s))
                return from;
        }

        return null;
    }

    // comfort version (rst)
    public static TEntityDict G_FindEdict(EdictIterator from, EdictFindFilter eff,
                                          String s) {
        EdictIterator ei = G_Find(from, eff, s);
        if (ei == null)
            return null;
        else
            return ei.o;
    }

    /**
     * Returns entities that have origins within a spherical area.
     */
    public static EdictIterator findradius(EdictIterator from, float[] org,
            float rad) {
        float[] eorg = { 0, 0, 0 };
        int j;

        if (from == null)
            from = new EdictIterator(0);
        else
            from.i++;

        for (; from.i < num_edicts; from.i++) {
            from.o = entityDicts[from.i];
            if (!from.o.inUse)
                continue;

            if (from.o.solid == Defines.SOLID_NOT)
                continue;

            for (j = 0; j < 3; j++)
                eorg[j] = org[j]
                        - (from.o.entityState.origin[j] + (from.o.mins[j] + from.o.maxs[j]) * 0.5f);

            if (Math3D.VectorLength(eorg) > rad)
                continue;
            return from;
        }

        return null;
    }

    /**
     * Searches all active entities for the next one that holds the matching
     * string at fieldofs (use the FOFS() macro) in the structure.
     * 
     * Searches beginning at the edict after from, or the beginning if null null
     * will be returned if the end of the list is reached.
     */

    public static int MAXCHOICES = 8;

    public static TEntityDict G_PickTarget(String targetname) {
        int num_choices = 0;
        TEntityDict choice[] = new TEntityDict[MAXCHOICES];

        if (targetname == null) {
            gi.dprintf("G_PickTarget called with null targetname\n");
            return null;
        }

        EdictIterator es = null;

        while ((es = G_Find(es, findByTarget, targetname)) != null) {
            choice[num_choices++] = es.o;
            if (num_choices == MAXCHOICES)
                break;
        }

        if (num_choices == 0) {
            gi.dprintf("G_PickTarget: target " + targetname + " not found\n");
            return null;
        }

        return choice[Lib.rand() % num_choices];
    }

    public static float[] VEC_UP = { 0, -1, 0 };

    public static float[] MOVEDIR_UP = { 0, 0, 1 };

    public static float[] VEC_DOWN = { 0, -2, 0 };

    public static float[] MOVEDIR_DOWN = { 0, 0, -1 };

    public static void G_SetMovedir(float[] angles, float[] movedir) {
        if (Math3D.VectorEquals(angles, VEC_UP)) {
            Math3D.VectorCopy(MOVEDIR_UP, movedir);
        } else if (Math3D.VectorEquals(angles, VEC_DOWN)) {
            Math3D.VectorCopy(MOVEDIR_DOWN, movedir);
        } else {
            Math3D.AngleVectors(angles, movedir, null, null);
        }

        Math3D.VectorClear(angles);
    }

    public static String G_CopyString(String in) {
        return new String(in);
    }

    /**
     * G_TouchTriggers
     */

    static TEntityDict touch[] = new TEntityDict[Defines.MAX_EDICTS];

    public static void G_TouchTriggers(TEntityDict ent) {
        int i, num;
        TEntityDict hit;

        // dead things don't activate triggers!
        if ((ent.client != null || (ent.svflags & Defines.SVF_MONSTER) != 0)
                && (ent.health <= 0))
            return;

        num = gi.BoxEdicts(ent.absmin, ent.absmax, touch, Defines.MAX_EDICTS,
                Defines.AREA_TRIGGERS);

        // be careful, it is possible to have an entity in this
        // list removed before we get to it (killtriggered)
        for (i = 0; i < num; i++) {
            hit = touch[i];

            if (!hit.inUse)
                continue;

            if (hit.touch == null)
                continue;

            hit.touch.touch(hit, ent, dummyplane, null);
        }
    }

    public static pushed_t pushed[] = new pushed_t[Defines.MAX_EDICTS];
    static {
        for (int n = 0; n < Defines.MAX_EDICTS; n++)
            pushed[n] = new pushed_t();
    }

    public static int pushed_p;

    public static TEntityDict obstacle;

    public static int c_yes, c_no;

    public static int STEPSIZE = 18;


    private static void runEntity(TEntityDict ent) {

        if (ent.prethink != null)
            ent.prethink.think(ent);

        switch (ent.movetype) {
        case Defines.MOVETYPE_PUSH:
        case Defines.MOVETYPE_STOP:
            Server.physicsPusher(ent);
            break;
        case Defines.MOVETYPE_NONE:
            Server.physicsNone(ent);
            break;
        case Defines.MOVETYPE_NOCLIP:
            Server.physicsNoClip(ent);
            break;
        case Defines.MOVETYPE_STEP:
            Server.physicsStep(ent);
            break;
        case Defines.MOVETYPE_TOSS:
        case Defines.MOVETYPE_BOUNCE:
        case Defines.MOVETYPE_FLY:
        case Defines.MOVETYPE_FLYMISSILE:
            Server.physicsToss(ent);
            break;
        default:
            gi.error("SV_Physics: bad movetype " + ent.movetype);
        }
    }

    public static void ClearBounds(float[] mins, float[] maxs) {
        mins[0] = mins[1] = mins[2] = 99999;
        maxs[0] = maxs[1] = maxs[2] = -99999;
    }

    public static void AddPointToBounds(float[] v, float[] mins, float[] maxs) {
        int i;
        float val;

        for (i = 0; i < 3; i++) {
            val = v[i];
            if (val < mins[i])
                mins[i] = val;
            if (val > maxs[i])
                maxs[i] = val;
        }
    }

    public static EdictFindFilter findByTarget = new EdictFindFilter() {
        public boolean matches(TEntityDict e, String s) {
            if (e.targetname == null)
                return false;
            return e.targetname.equalsIgnoreCase(s);
        }
    };

    public static EdictFindFilter findByClass = new EdictFindFilter() {
        public boolean matches(TEntityDict e, String s) {
            return e.classname.equalsIgnoreCase(s);
        }
    };

    public static void ShutdownGame() {
        gi.dprintf("==== ShutdownGame ====\n");
    }

    private static void clientEndServerFrames() {
        // calc the player views now that all pushing
        // and damage has been added
        for (int i = 0; i < maxclients.value; i++) {
            TEntityDict ent = entityDicts[1 + i];
            if (!ent.inUse || null == ent.client) {
                continue;
            }
            PlayerView.ClientEndServerFrame(ent);
        }

    }

    /**
     * Returns the created target changelevel.
     */
    public static TEntityDict CreateTargetChangeLevel(String map) {
        TEntityDict ent;

        ent = GameUtil.G_Spawn();
        ent.classname = "target_changelevel";
        level.nextmap = map;
        ent.map = level.nextmap;
        return ent;
    }

    /**
     * The timelimit or fraglimit has been exceeded.
     */
    public static void EndDMLevel() {
        TEntityDict ent;
        //char * entityState, * t, * f;
        //static const char * seps = " ,\n\r";
        String s, t, f;
        String seps = " ,\n\r";

        // stay on same level flag
        if (((int) dmflags.value & Defines.DF_SAME_LEVEL) != 0) {
            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
            return;
        }

        // see if it'entityState in the map list
        if (sv_maplist.string.length() > 0) {
            s = sv_maplist.string;
            f = null;
            StringTokenizer tk = new StringTokenizer(s, seps);
            
            while (tk.hasMoreTokens()){
            	t = tk.nextToken();
     
            	// store first map
            	if (f == null)
            		f = t;
            	
                if (t.equalsIgnoreCase(level.mapname)) {
                    // it'entityState in the list, go to the next one
                	if (!tk.hasMoreTokens()) {
                		// end of list, go to first one
                        if (f == null) // there isn't a first one, same level
                            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
                        else
                            PlayerHud.BeginIntermission(CreateTargetChangeLevel(f));
                    } else
                        PlayerHud.BeginIntermission(CreateTargetChangeLevel(tk.nextToken()));
                    return;
                }
            }
        }

        //not in the map list
        if (level.nextmap.length() > 0) // go to a specific map
            PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.nextmap));
        else { // search for a changelevel
            EdictIterator edit = null;
            edit = G_Find(edit, findByClass, "target_changelevel");
            if (edit == null) { // the map designer didn't include a
                                // changelevel,
                // so create a fake entityDict that goes back to the same level
                PlayerHud.BeginIntermission(CreateTargetChangeLevel(level.mapname));
                return;
            }
            ent = edit.o;
            PlayerHud.BeginIntermission(ent);
        }
    }

    /**
     * CheckNeedPass.
     */
    public static void CheckNeedPass() {
        int need;

        // if password or spectator_password has changed, update needpass
        // as needed
        if (password.modified || spectator_password.modified) {
            password.modified = spectator_password.modified = false;

            need = 0;

            if ((password.string.length() > 0)
                    && 0 != Lib.Q_stricmp(password.string, "none"))
                need |= 1;
            if ((spectator_password.string.length() > 0)
                    && 0 != Lib.Q_stricmp(spectator_password.string, "none"))
                need |= 2;

            gi.cvar_set("needpass", "" + need);
        }
    }

    /**
     * checkDeathMatchRules.
     */
    private static void checkDeathMatchRules() {

        if (level.intermissiontime != 0) {
            return;
        }

        if (deathmatch.value == 0) {
            return;
        }

        if (timelimit.value != 0) {
            if (level.time >= timelimit.value * 60) {
                gi.bprintf(Defines.PRINT_HIGH, "Timelimit hit.\n");
                EndDMLevel();
                return;
            }
        }

        if (fraglimit.value != 0) {
            for (int i = 0; i < maxclients.value; i++) {
                gclient_t cl = game.clients[i];
                if (!entityDicts[i + 1].inUse) {
                    continue;
                }

                if (cl.resp.score >= fraglimit.value) {
                    gi.bprintf(Defines.PRINT_HIGH, "Fraglimit hit.\n");
                    EndDMLevel();
                    return;
                }
            }
        }
    }

    /**
     * Exits a level.
     */
    public static void ExitLevel() {
        int i;
        TEntityDict ent;

        String command = "gamemap \"" + level.changemap + "\"\n";
        gi.AddCommandString(command);
        level.changemap = null;
        level.exitintermission = false;
        level.intermissiontime = 0;
        clientEndServerFrames();

        // clear some things before going to next level
        for (i = 0; i < maxclients.value; i++) {
            ent = entityDicts[1 + i];
            if (!ent.inUse)
                continue;
            if (ent.health > ent.client.pers.max_health)
                ent.health = ent.client.pers.max_health;
        }
    }

    /**
     * runFrame
     *  
     * Advances the world by Defines.FRAMETIME (0.1) seconds.
     */
    public static void runFrame() {

        level.framenum++;
        level.time = level.framenum * Defines.FRAMETIME;

        /* choose a client for monsters to target this frame */
        GameAI.AI_SetSightClient();

        /* exit intermissions */
        if (level.exitintermission) {
            ExitLevel();
            return;
        }

        /* Treat each object in turn, even the world gets a chance to think. */
        for (int i = 0; i < num_edicts; i++) {
            final TEntityDict entityDict = entityDicts[i];
            if (!entityDict.inUse) {
                continue;
            }

            level.current_entity = entityDict;

            Math3D.VectorCopy(entityDict.entityState.origin, entityDict.entityState.old_origin);

            // if the ground entity moved, make sure we are still on it
            if ((entityDict.groundentity != null)
                    && (entityDict.groundentity.linkCount != entityDict.groundentity_linkcount)) {
                entityDict.groundentity = null;
                if (0 == (entityDict.flags & (Defines.FL_SWIM | Defines.FL_FLY))
                        && (entityDict.svflags & Defines.SVF_MONSTER) != 0) {
                    Monster.M_CheckGround(entityDict);
                }
            }

            if (i > 0 && i <= maxclients.value) {
                PlayerClient.ClientBeginServerFrame(entityDict);
                continue;
            }

            runEntity(entityDict);
        }

        /* see if it is time to end a deathmatch */
        checkDeathMatchRules();

        /* see if needpass needs updated */
        CheckNeedPass();

        /* build the playerstate_t structures for all players */
        clientEndServerFrames();
    }

    /**
     * This return a pointer to the structure with all entry points and global
     * variables. 
     */

    public static void GetGameApi(TGameImport imp) {
        gi = imp;
        gi.pointcontents = new pmove_t.PointContentsAdapter() {
            public int pointcontents(float[] o) {
                return ServerWorld.pointContents(o);
            }
        };
    }
}