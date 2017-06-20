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
// Created on 07.01.2000 by RST.

package jake2.server;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.*;
import jake2.qcommon.CM;
import jake2.qcommon.Command;
import jake2.util.Math3D;

/**
 * world.c -- world query functions
 */
public class ServerWorld {

    private static final int MAX_TOTAL_ENT_LEAFS = 128;

    //
    //ENTITY AREA CHECKING
    //
    //FIXME: this use of "area" is different from the bsp file use
    //===============================================================================
    private static TAreaNode sv_areanodes[] = new TAreaNode[Defines.AREA_NODES];
    private static int sv_numareanodes;

    private static float area_mins[], area_maxs[];

    private static TEntityDict area_list[];

    private static int area_count, area_maxcount;

    private static int area_type;

    private static int leafs[] = new int[MAX_TOTAL_ENT_LEAFS];
    private static int clusters[] = new int[MAX_TOTAL_ENT_LEAFS];

    //===========================================================================
    static TEntityDict touch[] = new TEntityDict[Defines.MAX_EDICTS];

    //===========================================================================
    static TEntityDict touchlist[] = new TEntityDict[Defines.MAX_EDICTS];

    static {
        ServerWorld.initNodes();
    }

    private static void initNodes() {
        for (int n = 0; n < Defines.AREA_NODES; n++)
            ServerWorld.sv_areanodes[n] = new TAreaNode();
    }

    /**
     * Builds a uniformly subdivided tree for the given world size.
     */
    private static TAreaNode createAreaNode(int depth, float[] mins, float[] maxs) {

        float[] size = { 0, 0, 0 };
        float[] mins1 = { 0, 0, 0 }, maxs1 = { 0, 0, 0 }, mins2 = { 0, 0, 0 }, maxs2 = { 0, 0, 0 };

        TAreaNode anode = ServerWorld.sv_areanodes[ServerWorld.sv_numareanodes];
        // just for debugging (rst)
//        Math3D.VectorCopy(mins, anode.mins_rst);
//        Math3D.VectorCopy(maxs, anode.maxs_rst);

        ServerWorld.sv_numareanodes++;
        TLink.ClearLink(anode.trigger_edicts);
        TLink.ClearLink(anode.solid_edicts);
        if (depth == Defines.AREA_DEPTH) {
            anode.axis = -1;
            anode.children[0] = anode.children[1] = null;
            return anode;
        }
        Math3D.VectorSubtract(maxs, mins, size);
        anode.axis = size[0] > size[1] ? 0 : 1;
        anode.dist = 0.5f * (maxs[anode.axis] + mins[anode.axis]);
        Math3D.VectorCopy(mins, mins1);
        Math3D.VectorCopy(mins, mins2);
        Math3D.VectorCopy(maxs, maxs1);
        Math3D.VectorCopy(maxs, maxs2);
        maxs1[anode.axis] = mins2[anode.axis] = anode.dist;
        anode.children[0] = createAreaNode(depth + 1, mins2, maxs2);
        anode.children[1] = createAreaNode(depth + 1, mins1, maxs1);
        return anode;
    }

    /*
     * =============== SV_ClearWorld
     * 
     * ===============
     */
    public static void SV_ClearWorld() {
        initNodes();
        ServerWorld.sv_numareanodes = 0;
        createAreaNode(0, ServerInit.sv.models[1].mins,
                ServerInit.sv.models[1].maxs);
        /*
         * Command.p("areanodes:" + sv_numareanodes + " (sollten 32 sein)."); for
         * (int n = 0; n < sv_numareanodes; n++) { Command.Printf( "|%3i|%2i|%8.2f
         * |%8.2f|%8.2f|%8.2f| %8.2f|%8.2f|%8.2f|\n", new Vargs() .add(n)
         * .add(sv_areanodes[n].axis) .add(sv_areanodes[n].dist)
         * .add(sv_areanodes[n].mins_rst[0]) .add(sv_areanodes[n].mins_rst[1])
         * .add(sv_areanodes[n].mins_rst[2]) .add(sv_areanodes[n].maxs_rst[0])
         * .add(sv_areanodes[n].maxs_rst[1]) .add(sv_areanodes[n].maxs_rst[2])); }
         */
    }

    /*
     * =============== unlinkEdict ===============
     */
    public static void unlinkEdict(TEntityDict ent) {
        if (ent.area.prev == null) {
            return; // not linked in anywhere
        }

        ent.area.remove();
        ent.area.prev = ent.area.next = null;
    }

    public static void linkEdict(TEntityDict ent) {

        int num_leafs;
        int j, k;
        int area;
        int topnode = 0;

        if (ent.area.prev != null) {
            unlinkEdict(ent); // unlink from old position
        }

        if (ent == GameBase.entityDicts[0]) {
            return; // don't add the world
        }

        if (!ent.inUse) {
            return;
        }
        // assign the size
        Math3D.VectorSubtract(ent.maxs, ent.mins, ent.size);
        // encode the size into the entity_state for client prediction
        if (ent.solid == Defines.SOLID_BBOX && 0 == (ent.svflags & Defines.SVF_DEADMONSTER)) {
            // assume that x/y are equal and symetric
            int i = (int) (ent.maxs[0] / 8);
            if (i < 1)
                i = 1;
            if (i > 31)
                i = 31;
            // z is not symetric
            j = (int) ((-ent.mins[2]) / 8);
            if (j < 1)
                j = 1;
            if (j > 31)
                j = 31;
            // and z maxs can be negative...
            k = (int) ((ent.maxs[2] + 32) / 8);
            if (k < 1)
                k = 1;
            if (k > 63)
                k = 63;
            ent.entityState.solid = (k << 10) | (j << 5) | i;
        } else if (ent.solid == Defines.SOLID_BSP) {
            ent.entityState.solid = 31; // a solid_bbox will never create this value
        } else
            ent.entityState.solid = 0;
        // assign the abs box
        if (ent.solid == Defines.SOLID_BSP
                && (ent.entityState.angles[0] != 0 || ent.entityState.angles[1] != 0 || ent.entityState.angles[2] != 0)) {
            // expand for rotation
            float max, v;
            max = 0;
            for (int i = 0; i < 3; i++) {
                v = Math.abs(ent.mins[i]);
                if (v > max)
                    max = v;
                v = Math.abs(ent.maxs[i]);
                if (v > max)
                    max = v;
            }
            for (int i = 0; i < 3; i++) {
                ent.absmin[i] = ent.entityState.origin[i] - max;
                ent.absmax[i] = ent.entityState.origin[i] + max;
            }
        } else {
            // normal
            Math3D.VectorAdd(ent.entityState.origin, ent.mins, ent.absmin);
            Math3D.VectorAdd(ent.entityState.origin, ent.maxs, ent.absmax);
        }
        // because movement is clipped an epsilon away from an actual edge,
        // we must fully check even when bounding boxes don't quite touch
        ent.absmin[0]--;
        ent.absmin[1]--;
        ent.absmin[2]--;
        ent.absmax[0]++;
        ent.absmax[1]++;
        ent.absmax[2]++;
        // link to PVS leafs
        ent.num_clusters = 0;
        ent.areanum = 0;
        ent.areanum2 = 0;
        // get all leafs, including solids
        int iw[] = { topnode };
        num_leafs = CM.CM_BoxLeafnums(ent.absmin, ent.absmax, ServerWorld.leafs,
                ServerWorld.MAX_TOTAL_ENT_LEAFS, iw);
        topnode = iw[0];
        // assign areas
        for (int i = 0; i < num_leafs; i++) {
            ServerWorld.clusters[i] = CM.CM_LeafCluster(ServerWorld.leafs[i]);
            area = CM.CM_LeafArea(ServerWorld.leafs[i]);
            if (area != 0) {
                // doors may legally straggle two areas,
                // but nothing should evern need more than that
                if (ent.areanum != 0 && ent.areanum != area) {
                    if (ent.areanum2 != 0 && ent.areanum2 != area
                            && ServerInit.sv.state == Defines.ss_loading)
                        Command.DPrintf("Object touching 3 areas at "
                                + ent.absmin[0] + " " + ent.absmin[1] + " "
                                + ent.absmin[2] + "\n");
                    ent.areanum2 = area;
                } else
                    ent.areanum = area;
            }
        }
        if (num_leafs >= ServerWorld.MAX_TOTAL_ENT_LEAFS) {
            // assume we missed some leafs, and mark by headnode
            ent.num_clusters = -1;
            ent.headnode = topnode;
        } else {
            ent.num_clusters = 0;
            for (int i = 0; i < num_leafs; i++) {
                if (ServerWorld.clusters[i] == -1)
                    continue; // not a visible leaf
                for (j = 0; j < i; j++)
                    if (ServerWorld.clusters[j] == ServerWorld.clusters[i])
                        break;
                if (j == i) {
                    if (ent.num_clusters == Defines.MAX_ENT_CLUSTERS) {
                        // assume we missed some leafs, and mark by headnode
                        ent.num_clusters = -1;
                        ent.headnode = topnode;
                        break;
                    }
                    ent.clusternums[ent.num_clusters++] = ServerWorld.clusters[i];
                }
            }
        }
        // if first time, make sure old_origin is valid
        if (0 == ent.linkCount) {
            Math3D.VectorCopy(ent.entityState.origin, ent.entityState.old_origin);
        }
        ent.linkCount++;
        if (ent.solid == Defines.SOLID_NOT)
            return;

        // find the first node that the entityDict'entityState box crosses
        TAreaNode node = ServerWorld.sv_areanodes[0];
        while (true) {
            if (node.axis == -1)
                break;
            if (ent.absmin[node.axis] > node.dist)
                node = node.children[0];
            else if (ent.absmax[node.axis] < node.dist)
                node = node.children[1];
            else
                break; // crosses the node
        }
        // link it in
        if (ent.solid == Defines.SOLID_TRIGGER)
            ent.area.insertBefore(node.trigger_edicts);
        else
            ent.area.insertBefore(node.solid_edicts);
    }

    /*
     * ==================== areaEdictsRecursive
     * 
     * ====================
     */
    private static void areaEdictsRecursive(TAreaNode node) {
        TLink l, next, start;

        // touch linked edicts
        if (ServerWorld.area_type == Defines.AREA_SOLID) {
            start = node.solid_edicts;
        } else {
            start = node.trigger_edicts;
        }

        for (l = start.next; l != start; l = next) {
            next = l.next;
            TEntityDict check = (TEntityDict) l.o;
            if (check.solid == Defines.SOLID_NOT)
                continue; // deactivated
            if (check.absmin[0] > ServerWorld.area_maxs[0]
                    || check.absmin[1] > ServerWorld.area_maxs[1]
                    || check.absmin[2] > ServerWorld.area_maxs[2]
                    || check.absmax[0] < ServerWorld.area_mins[0]
                    || check.absmax[1] < ServerWorld.area_mins[1]
                    || check.absmax[2] < ServerWorld.area_mins[2])
                continue; // not touching
            if (ServerWorld.area_count == ServerWorld.area_maxcount) {
                Command.Printf("areaEdicts: MAXCOUNT\n");
                return;
            }
            ServerWorld.area_list[ServerWorld.area_count] = check;
            ServerWorld.area_count++;
        }

        if (node.axis == -1) {
            return; // terminal node
        }

        // recurse down both sides
        if (ServerWorld.area_maxs[node.axis] > node.dist) {
            areaEdictsRecursive(node.children[0]);
        }
        if (ServerWorld.area_mins[node.axis] < node.dist) {
            areaEdictsRecursive(node.children[1]);
        }
    }

    /*
     * ================ areaEdicts ================
     */
    public static int areaEdicts(float[] mins, float[] maxs, TEntityDict list[], int maxcount, int areatype) {
        ServerWorld.area_mins = mins;
        ServerWorld.area_maxs = maxs;
        ServerWorld.area_list = list;
        ServerWorld.area_count = 0;
        ServerWorld.area_maxcount = maxcount;
        ServerWorld.area_type = areatype;
        areaEdictsRecursive(ServerWorld.sv_areanodes[0]);
        return ServerWorld.area_count;
    }

    /*
     * ============= pointContents =============
     */
    public static int pointContents(float[] p) {

        // get base contents from world
        int contents = CM.PointContents(p, ServerInit.sv.models[1].headnode);
        // or in contents from all the other entities
        int num = areaEdicts(p, p, ServerWorld.touch, Defines.MAX_EDICTS, Defines.AREA_SOLID);
        for (int i = 0; i < num; i++) {
            TEntityDict hit = ServerWorld.touch[i];
            // might intersect, so do an exact clip
            int headnode = hullForEntity(hit);
            if (hit.solid != Defines.SOLID_BSP) {
	        }
            int c2 = CM.TransformedPointContents(p, headnode, hit.entityState.origin, hit.entityState.angles);
            contents |= c2;
        }
        return contents;
    }

    /*
     * ================ hullForEntity
     * 
     * Returns a headnode that can be used for testing or clipping an object of
     * mins/maxs size. Offset is filled in to contain the adjustment that must
     * be added to the testing object'entityState origin to get a point to use with the
     * returned hull. ================
     */
    private static int hullForEntity(TEntityDict ent) {

        // decide which clipping hull to use, based on the size
        if (ent.solid == Defines.SOLID_BSP) {
            // explicit hulls in the BSP model
            TCModel model = ServerInit.sv.models[ent.entityState.modelIndex];
            if (model == null) {
                Command.Error(Defines.ERR_FATAL, "MOVETYPE_PUSH with a non bsp model");
            }
            return model.headnode;
        }
        // create a temp hull from bounding box sizes
        return CM.HeadnodeForBox(ent.mins, ent.maxs);
    }

    private static void clipMoveToEntities(TMoveClip clip) {

        int num = areaEdicts(clip.boxmins, clip.boxmaxs, ServerWorld.touchlist, Defines.MAX_EDICTS, Defines.AREA_SOLID);
        // be careful, it is possible to have an entity in this
        // list removed before we get to it (killtriggered)
        for (int i = 0; i < num; i++) {
            TEntityDict touch = ServerWorld.touchlist[i];
            if (touch.solid == Defines.SOLID_NOT)
                continue;
            if (touch == clip.passedict)
                continue;
            if (clip.trace.allSolid)
                return;
            if (clip.passedict != null) {
                if (touch.owner == clip.passedict)
                    continue; // don't clip against own missiles
                if (clip.passedict.owner == touch)
                    continue; // don't clip against owner
            }
            if (0 == (clip.contentmask & Defines.CONTENTS_DEADMONSTER)
                    && 0 != (touch.svflags & Defines.SVF_DEADMONSTER))
                continue;
            // might intersect, so do an exact clip
            int headnode = hullForEntity(touch);
            float[] angles = touch.entityState.angles;
            if (touch.solid != Defines.SOLID_BSP) {
                angles = Context.vec3_origin; // boxes don't rotate
            }

            TTrace trace;
            if ((touch.svflags & Defines.SVF_MONSTER) != 0)
                trace = CM.TransformedBoxTrace(clip.start, clip.end,
                        clip.mins2, clip.maxs2, headnode, clip.contentmask,
                        touch.entityState.origin, angles);
            else
                trace = CM.TransformedBoxTrace(clip.start, clip.end, clip.mins,
                        clip.maxs, headnode, clip.contentmask, touch.entityState.origin,
                        angles);

            if (trace.allSolid || trace.startSolid || trace.fraction < clip.trace.fraction) {
                trace.entityDict = touch;
                if (clip.trace.startSolid) {
                    clip.trace = trace;
                    clip.trace.startSolid = true;
                } else {
                    clip.trace.set(trace);
                }
            } else if (trace.startSolid) {
                    clip.trace.startSolid = true;
            }
        }
    }

    /*
     * ================== traceBounds ==================
     */
    private static void traceBounds(float[] start, float[] mins, float[] maxs, float[] end, float[] boxmins, float[] boxmaxs) {
        for (int i = 0; i < 3; i++) {
            if (end[i] > start[i]) {
                boxmins[i] = start[i] + mins[i] - 1;
                boxmaxs[i] = end[i] + maxs[i] + 1;
            } else {
                boxmins[i] = end[i] + mins[i] - 1;
                boxmaxs[i] = start[i] + maxs[i] + 1;
            }
        }
    }

    /*
     * ================== trace
     * 
     * Moves the given mins/maxs volume through the world from start to end.
     * 
     * Passedict and edicts owned by passedict are explicitly not checked.
     * 
     * ==================
     */
    public static TTrace trace(float[] start, float[] mins, float[] maxs,
                               float[] end, TEntityDict passedict, int contentmask) {

        if (mins == null) {
            mins = Context.vec3_origin;
        }

        if (maxs == null) {
            maxs = Context.vec3_origin;
        }

        // clip to world
        TMoveClip clip = new TMoveClip();
        clip.trace = CM.BoxTrace(start, end, mins, maxs, 0, contentmask);
        clip.trace.entityDict = GameBase.entityDicts[0];
        if (clip.trace.fraction == 0) {
            return clip.trace; // blocked by the world
        }
        clip.contentmask = contentmask;
        clip.start = start;
        clip.end = end;
        clip.mins = mins;
        clip.maxs = maxs;
        clip.passedict = passedict;
        Math3D.VectorCopy(mins, clip.mins2);
        Math3D.VectorCopy(maxs, clip.maxs2);
        // create the bounding box of the entire move
        traceBounds(start, clip.mins2, clip.maxs2, end, clip.boxmins, clip.boxmaxs);
        // clip to other solid entities
        clipMoveToEntities(clip);
        return clip.trace;
    }
}