/*
 * V.java
 * Copyright (C) 2003
 * 
 * $Id: V.java,v 1.5 2005-07-01 14:20:50 hzi Exp $
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

import jake2.common.TDynamicLight;
import jake2.game.Cmd;
import jake2.game.TVar;
import jake2.qcommon.*;
import jake2.sys.Timer;
import jake2.util.Math3D;

import java.io.IOException;
import java.nio.FloatBuffer;

import static jake2.Defines.*;
import static jake2.client.Context.*;

/**
 * V
 */
public final class V {

    private static TVar cl_testblend;

    private static TVar cl_testparticles;

    private static TVar cl_testentities;

    private static TVar cl_testlights;

    private static TVar cl_stats;

    private static int r_numdlights;

    private static TDynamicLight[] r_dlights = new TDynamicLight[MAX_DLIGHTS];

    private static int r_numentities;

    private static TEntity[] r_entities = new TEntity[MAX_ENTITIES];

    private static int r_numparticles;

    //static TParticle[] r_particles = new TParticle[MAX_PARTICLES];

    private static TLightStyle[] r_lightstyles = new TLightStyle[MAX_LIGHTSTYLES];
    static {
        for (int i = 0; i < r_dlights.length; i++)
            r_dlights[i] = new TDynamicLight();
        for (int i = 0; i < r_entities.length; i++)
            r_entities[i] = new TEntity();
        for (int i = 0; i < r_lightstyles.length; i++)
            r_lightstyles[i] = new TLightStyle();
    }

    /*
     * ==================== V_ClearScene
     * 
     * Specifies the model that will be used as the world ====================
     */
    private static void clearScene() {
        r_numdlights = 0;
        r_numentities = 0;
        r_numparticles = 0;
    }

    /*
     * ===================== V_AddEntity
     * 
     * =====================
     */
    static void AddEntity(TEntity ent) {
        if (r_numentities >= MAX_ENTITIES)
            return;
        r_entities[r_numentities++].set(ent);
    }

    /*
     * ===================== V_AddParticle
     * 
     * =====================
     */
    static void AddParticle(float[] org, int color, float alpha) {
        if (r_numparticles >= MAX_PARTICLES)
            return;

        int i = r_numparticles++;

        int c = TParticle.colorTable[color];
        c |= (int) (alpha * 255) << 24;
        TParticle.colorArray.put(i, c);

        i *= 3;
        FloatBuffer vertexBuf = TParticle.vertexArray;
        vertexBuf.put(i++, org[0]);
        vertexBuf.put(i++, org[1]);
        vertexBuf.put(i++, org[2]);
    }

    /*
     * ===================== V_AddLight
     * 
     * =====================
     */
    static void AddLight(float[] org, float intensity, float r, float g, float b) {

        if (r_numdlights >= MAX_DLIGHTS)
            return;
        TDynamicLight dl = r_dlights[r_numdlights++];
        Math3D.VectorCopy(org, dl.origin);
        dl.intensity = intensity;
        dl.color[0] = r;
        dl.color[1] = g;
        dl.color[2] = b;
    }

    /*
     * ===================== V_AddLightStyle
     * 
     * =====================
     */
    static void AddLightStyle(int style, float r, float g, float b) {
        if (style < 0 || style > MAX_LIGHTSTYLES)
            Command.Error(ERR_DROP, "Bad light style " + style);

        TLightStyle ls = r_lightstyles[style];
        ls.white = r + g + b;
        ls.rgb[0] = r;
        ls.rgb[1] = g;
        ls.rgb[2] = b;
    }

    // stack variable
    private static final float[] origin = { 0, 0, 0 };
    /*
     * ================ V_TestParticles
     * 
     * If cl_testparticles is set, create 4096 particles in the view
     * ================
     */
    static void TestParticles() {

        r_numparticles = 0;
        for (int i = 0; i < MAX_PARTICLES; i++) {
            float d = i * 0.25f;
            float r = 4 * ((i & 7) - 3.5f);
            float u = 4 * (((i >> 3) & 7) - 3.5f);

            for (int j = 0; j < 3; j++) {
                origin[j] = cl.refdef.vieworg[j] + cl.v_forward[j] * d + cl.v_right[j] * r + cl.v_up[j] * u;
            }

            AddParticle(origin, 8, cl_testparticles.value);
        }
    }

    /*
     * ================ V_TestEntities
     * 
     * If cl_testentities is set, create 32 player models ================
     */
    static void TestEntities() {
        int i, j;
        float f, r;
        TEntity ent;

        r_numentities = 32;
        //memset (r_entities, 0, sizeof(r_entities));
        for (i = 0; i < r_entities.length; i++)
        	r_entities[i].clear();

        for (i = 0; i < r_numentities; i++) {
            ent = r_entities[i];

            r = 64 * ((i % 4) - 1.5f);
            f = 64 * (i / 4) + 128;

            for (j = 0; j < 3; j++)
                ent.origin[j] = cl.refdef.vieworg[j] + cl.v_forward[j] * f
                        + cl.v_right[j] * r;

            ent.model = cl.baseclientinfo.model;
            ent.skin = cl.baseclientinfo.skin;
        }
    }

    /*
     * ================ V_TestLights
     * 
     * If cl_testlights is set, create 32 lights models ================
     */
    static void TestLights() {
        int i, j;
        float f, r;
        TDynamicLight dl;

        r_numdlights = 32;
        //memset (r_dlights, 0, sizeof(r_dlights));
        for (i = 0; i < r_dlights.length; i++)
            r_dlights[i] = new TDynamicLight();

        for (i = 0; i < r_numdlights; i++) {
            dl = r_dlights[i];

            r = 64 * ((i % 4) - 1.5f);
            f = 64 * (i / 4) + 128;

            for (j = 0; j < 3; j++)
                dl.origin[j] = cl.refdef.vieworg[j] + cl.v_forward[j] * f
                        + cl.v_right[j] * r;
            dl.color[0] = ((i % 6) + 1) & 1;
            dl.color[1] = (((i % 6) + 1) & 2) >> 1;
            dl.color[2] = (((i % 6) + 1) & 4) >> 2;
            dl.intensity = 200;
        }
    }

    static TXCommand Gun_Next_f = () -> {
        gun_frame++;
        Command.Printf("frame " + gun_frame + "\n");
    };

    static TXCommand Gun_Prev_f = () -> {
        gun_frame--;
        if (gun_frame < 0)
            gun_frame = 0;
        Command.Printf("frame " + gun_frame + "\n");
    };

    static TXCommand Gun_Model_f = () -> {
        if (Cmd.Argc() != 2) {
            Context.gun_model = null;
            return;
        }
        String name = "models/" + Cmd.Argv(1) + "/tris.md2";
        Context.gun_model = re.RegisterModel(name);
    };

    /*
     * ================== V_RenderView
     * 
     * ==================
     */
    static void RenderView(float stereo_separation) {
        //		extern int entitycmpfnc( const TEntity *, const TEntity * );
        //
        if (cls.state != ca_active)
            return;

        if (!cl.refresh_prepped)
            return; // still loading

        if (cl_timedemo.value != 0.0f) {
            if (cl.timedemo_start == 0)
                cl.timedemo_start = Timer.Milliseconds();
            cl.timedemo_frames++;
        }

        // an invalid frame will just use the exact previous refdef
        // we can't use the old frame if the video mode has changed, though...
        if (cl.frame.valid && (cl.force_refdef || cl_paused.value == 0.0f)) {
            cl.force_refdef = false;

            V.clearScene();

            // build a refresh entity list and calc cl.sim*
            // this also calls CL_CalcViewValues which loads
            // v_forward, etc.
            CLEntity.AddEntities();

            if (cl_testparticles.value != 0.0f)
                TestParticles();
            if (cl_testentities.value != 0.0f)
                TestEntities();
            if (cl_testlights.value != 0.0f)
                TestLights();
            if (cl_testblend.value != 0.0f) {
                cl.refdef.blend[0] = 1.0f;
                cl.refdef.blend[1] = 0.5f;
                cl.refdef.blend[2] = 0.25f;
                cl.refdef.blend[3] = 0.5f;
            }

            // offset vieworg appropriately if we're doing stereo separation
            if (stereo_separation != 0) {
                float[] tmp = new float[3];

                Math3D.VectorScale(cl.v_right, stereo_separation, tmp);
                Math3D.VectorAdd(cl.refdef.vieworg, tmp, cl.refdef.vieworg);
            }

            // never let it sit exactly on a node line, because a water plane
            // can
            // dissapear when viewed with the eye exactly on it.
            // the server protocol only specifies to 1/8 pixel, so add 1/16 in
            // each axis
            cl.refdef.vieworg[0] += 1.0 / 16;
            cl.refdef.vieworg[1] += 1.0 / 16;
            cl.refdef.vieworg[2] += 1.0 / 16;

            cl.refdef.x = scr_vrect.getX();
            cl.refdef.y = scr_vrect.getY();
            cl.refdef.width = scr_vrect.getWidth();
            cl.refdef.height = scr_vrect.getHeight();
            cl.refdef.fov_y = Math3D.CalcFov(cl.refdef.fov_x, cl.refdef.width,
                    cl.refdef.height);
            cl.refdef.time = cl.time * 0.001f;

            cl.refdef.areabits = cl.frame.areabits;

            if (cl_add_entities.value == 0.0f)
                r_numentities = 0;
            if (cl_add_particles.value == 0.0f)
                r_numparticles = 0;
            if (cl_add_lights.value == 0.0f)
                r_numdlights = 0;
            if (cl_add_blend.value == 0) {
                Math3D.VectorClear(cl.refdef.blend);
            }

            cl.refdef.num_entities = r_numentities;
            cl.refdef.entities = r_entities;
            cl.refdef.num_particles = r_numparticles;
            cl.refdef.num_dlights = r_numdlights;
            cl.refdef.dlights = r_dlights;
            cl.refdef.lightstyles = r_lightstyles;

            cl.refdef.renderFlags = cl.frame.playerstate.rdflags;

            // sort entities for better cache locality
            // !!! useless in Java !!!
            //Arrays.sort(cl.refdef.entities, entitycmpfnc);
        }

        re.RenderFrame(cl.refdef);
        if (cl_stats.value != 0.0f)
            Command.Printf("ent:%i  lt:%i  part:%i\n", r_numentities, r_numdlights, r_numparticles);
        if (log_stats.value != 0.0f && (log_stats_file != null))
            try {
                log_stats_file.write(r_numentities + "," + r_numdlights + ","
                        + r_numparticles);
            } catch (IOException e) {
            }

        SCR.AddDirtyPoint(scr_vrect.getX(), scr_vrect.getY());
        SCR.AddDirtyPoint(scr_vrect.getX() + scr_vrect.getWidth() - 1, scr_vrect.getY()
                + scr_vrect.getHeight() - 1);

        SCR.DrawCrosshair();
    }

    /*
     * ============= V_Viewpos_f =============
     */
    static TXCommand Viewpos_f = () -> Command.Printf("(%i %i %i) : %i\n",
            (int) cl.refdef.vieworg[0],
            (int) cl.refdef.vieworg[1],
            (int) cl.refdef.vieworg[2],
            (int) cl.refdef.viewangles[YAW]);

    public static void Init() {
        Cmd.AddCommand("gun_next", Gun_Next_f);
        Cmd.AddCommand("gun_prev", Gun_Prev_f);
        Cmd.AddCommand("gun_model", Gun_Model_f);

        Cmd.AddCommand("viewpos", Viewpos_f);

        crosshair = ConsoleVar.Get("crosshair", "0", TVar.CVAR_FLAG_ARCHIVE);

        cl_testblend = ConsoleVar.Get("cl_testblend", "0", 0);
        cl_testparticles = ConsoleVar.Get("cl_testparticles", "0", 0);
        cl_testentities = ConsoleVar.Get("cl_testentities", "0", 0);
        cl_testlights = ConsoleVar.Get("cl_testlights", "0", 0);

        cl_stats = ConsoleVar.Get("cl_stats", "0", 0);
    }
}