/*
 * java
 * Copyright (C) 2004
 * 
 * $Id: CL_fx.java,v 1.9 2005-02-06 19:18:10 salomo Exp $
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
import jake2.game.TEntityState;
import jake2.game.monsters.M_Flash;
import jake2.qcommon.Command;
import jake2.qcommon.TBuffer;
import jake2.sound.Sound;
import jake2.util.Lib;
import jake2.util.Math3D;

/**
 * Client Graphics Effects.
 */
public class CLEffects {

    static TCParticle[] particles = new TCParticle[Defines.MAX_PARTICLES];

    static {
        for (int i = 0; i < particles.length; i++)
            particles[i] = new TCParticle();
    }

    static int cl_numparticles = Defines.MAX_PARTICLES;

    static final float INSTANT_PARTICLE = -10000.0f;

    static float[][] avelocities = new float[Defines.NUMVERTEXNORMALS][3];

    static TClLightStyle[] cl_lightstyle = new TClLightStyle[Defines.MAX_LIGHTSTYLES];

    static int lastofs;

    static {
        for (int i = 0; i < cl_lightstyle.length; i++) {
            cl_lightstyle[i] = new TClLightStyle();
        }
    }

    static TClDynamicLight[] cl_dlights = new TClDynamicLight[Defines.MAX_DLIGHTS];

    static {
        for (int i = 0; i < cl_dlights.length; i++)
            cl_dlights[i] = new TClDynamicLight();
    }

    /*
     * ================ CL_RunLightStyles ================
     */
    static void RunLightStyles() {
        TClLightStyle ls;

        int ofs = Context.cl.time / 100;
        if (ofs == lastofs)
            return;
        lastofs = ofs;

        for (int i = 0; i < cl_lightstyle.length; i++) {
            ls = cl_lightstyle[i];
            if (ls.getLength() == 0) {
                ls.getValue()[0] = ls.getValue()[1] = ls.getValue()[2] = 1.0f;
                continue;
            }
            if (ls.getLength() == 1)
                ls.getValue()[0] = ls.getValue()[1] = ls.getValue()[2] = ls.getMap()[0];
            else
                ls.getValue()[0] = ls.getValue()[1] = ls.getValue()[2] = ls.getMap()[ofs % ls.getLength()];
        }
    }

    static void SetLightstyle(int i) {
        String s;
        int j, k;

        s = Context.cl.configstrings[i + Defines.CS_LIGHTS];

        j = s.length();
        if (j >= Defines.MAX_QPATH)
            Command.Error(Defines.ERR_DROP, "svc_lightstyle length=" + j);

        cl_lightstyle[i].setLength(j);

        for (k = 0; k < j; k++)
            cl_lightstyle[i].getMap()[k] = (float) (s.charAt(k) - 'a') / (float) ('m' - 'a');
    }

    /*
     * ================ CL_AddLightStyles ================
     */
    static void AddLightStyles() {
        TClLightStyle ls;

        for (int i = 0; i < cl_lightstyle.length; i++) {
            ls = cl_lightstyle[i];
            V.AddLightStyle(i, ls.getValue()[0], ls.getValue()[1], ls.getValue()[2]);
        }
    }

    /*
     * =============== CL_AllocDlight
     *
     * ===============
     */
    static TClDynamicLight AllocDlight(int key) {
        int i;
        TClDynamicLight dl;

        //	   first look for an exact key match
        if (key != 0) {
            for (i = 0; i < Defines.MAX_DLIGHTS; i++) {
                dl = cl_dlights[i];
                if (dl.getKey() == key) {
                    //memset (dl, 0, sizeof(*dl));
                    dl.clear();
                    dl.setKey(key);
                    return dl;
                }
            }
        }

        //	   then look for anything else
        for (i = 0; i < Defines.MAX_DLIGHTS; i++) {
            dl = cl_dlights[i];
            if (dl.getDie() < Context.cl.time) {
                //memset (dl, 0, sizeof(*dl));
                dl.clear();
                dl.setKey(key);
                return dl;
            }
        }

        //dl = &cl_dlights[0];
        //memset (dl, 0, sizeof(*dl));
        dl = cl_dlights[0];
        dl.clear();
        dl.setKey(key);
        return dl;
    }


    /*
     * ===============
     * CL_RunDLights
     * ===============
     */
    static void RunDLights() {
        TClDynamicLight dl;

        for (int i = 0; i < Defines.MAX_DLIGHTS; i++) {
            dl = cl_dlights[i];
            if (dl.getRadius() == 0.0f)
                continue;

            if (dl.getDie() < Context.cl.time) {
                dl.setRadius(0.0f);
                return;
            }
        }
    }

    // stack variable
    private static final float[] fv = {0, 0, 0};
    private static final float[] rv = {0, 0, 0};

    /*
     * ==============
     *  CL_ParseMuzzleFlash
     * ==============
     */
    static void ParseMuzzleFlash() {
        float volume;
        String soundname;

        int i = TBuffer.ReadShort(Context.net_message);
        if (i < 1 || i >= Defines.MAX_EDICTS)
            Command.Error(Defines.ERR_DROP, "CL_ParseMuzzleFlash: bad entity");

        int weapon = TBuffer.ReadByte(Context.net_message);
        int silenced = weapon & Defines.MZ_SILENCED;
        weapon &= ~Defines.MZ_SILENCED;

        TClEentity pl = Context.cl_entities[i];

        TClDynamicLight dl = AllocDlight(i);
        Math3D.VectorCopy(pl.current.origin, dl.getOrigin());
        Math3D.AngleVectors(pl.current.angles, fv, rv, null);
        Math3D.VectorMA(dl.getOrigin(), 18, fv, dl.getOrigin());
        Math3D.VectorMA(dl.getOrigin(), 16, rv, dl.getOrigin());
        if (silenced != 0)
            dl.setRadius(100 + (Context.rnd.nextInt() & 31));
        else
            dl.setRadius(200 + (Context.rnd.nextInt() & 31));
        dl.setMinlight(32);
        dl.setDie(Context.cl.time); // + 0.1;

        if (silenced != 0)
            volume = 0.2f;
        else
            volume = 1;

        switch (weapon) {
            case Defines.MZ_BLASTER:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/blastf1a.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_BLUEHYPERBLASTER:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 0;
                dl.getColor()[2] = 1;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/hyprbf1a.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_HYPERBLASTER:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/hyprbf1a.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_MACHINEGUN:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_SHOTGUN:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/shotgf1b.wav"), volume, Defines.ATTN_NORM, 0);
                Sound.StartSound(null, i, Defines.CHAN_AUTO, Sound.RegisterSound("weapons/shotgr1b.wav"), volume, Defines.ATTN_NORM, 0.1f);
                break;
            case Defines.MZ_SSHOTGUN:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/sshotf1b.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_CHAINGUN1:
                dl.setRadius(200 + (Context.rnd.nextInt() & 31));
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.25f;
                dl.getColor()[2] = 0;
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_CHAINGUN2:
                dl.setRadius(225 + (Context.rnd.nextInt() & 31));
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 0.1f); // long delay
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0);
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0.05f);
                break;
            case Defines.MZ_CHAINGUN3:
                dl.setRadius(250 + (Context.rnd.nextInt() & 31));
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 0.1f); // long delay
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0);
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0.033f);
                //Com_sprintf(soundname, sizeof(soundname),
                // "weapons/machgf%ib.wav", (rand() % 5) + 1);
                soundname = "weapons/machgf" + ((Context.rnd.nextInt(5)) + 1) + "b.wav";
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), volume, Defines.ATTN_NORM, 0.066f);
                break;
            case Defines.MZ_RAILGUN:
                dl.getColor()[0] = 0.5f;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 1.0f;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/railgf1a.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_ROCKET:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0.2f;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/rocklf1a.wav"), volume, Defines.ATTN_NORM, 0);
                Sound.StartSound(null, i, Defines.CHAN_AUTO, Sound.RegisterSound("weapons/rocklr1b.wav"), volume, Defines.ATTN_NORM, 0.1f);
                break;
            case Defines.MZ_GRENADE:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/grenlf1a.wav"), volume, Defines.ATTN_NORM, 0);
                Sound.StartSound(null, i, Defines.CHAN_AUTO, Sound.RegisterSound("weapons/grenlr1b.wav"), volume, Defines.ATTN_NORM, 0.1f);
                break;
            case Defines.MZ_BFG:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/bfg__f1y.wav"), volume, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ_LOGIN:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 1.0f);
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/grenlf1a.wav"), 1, Defines.ATTN_NORM, 0);
                LogoutEffect(pl.current.origin, weapon);
                break;
            case Defines.MZ_LOGOUT:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 1.0f);
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/grenlf1a.wav"), 1, Defines.ATTN_NORM, 0);
                LogoutEffect(pl.current.origin, weapon);
                break;
            case Defines.MZ_RESPAWN:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 1.0f);
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/grenlf1a.wav"), 1, Defines.ATTN_NORM, 0);
                LogoutEffect(pl.current.origin, weapon);
                break;
            // RAFAEL
            case Defines.MZ_PHALANX:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0.5f;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/plasshot.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            // RAFAEL
            case Defines.MZ_IONRIPPER:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0.5f;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/rippfire.wav"), volume, Defines.ATTN_NORM, 0);
                break;

            //	   ======================
            //	   PGM
            case Defines.MZ_ETF_RIFLE:
                dl.getColor()[0] = 0.9f;
                dl.getColor()[1] = 0.7f;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/nail1.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_SHOTGUN2:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/shotg2.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_HEATBEAM:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 100);
                //			Sound.StartSound (null, i, CHAN_WEAPON,
                // Sound.RegisterSound("weapons/bfg__l1a.wav"), volume, ATTN_NORM, 0);
                break;
            case Defines.MZ_BLASTER2:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                // FIXME - different sound for blaster2 ??
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/blastf1a.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_TRACKER:
                // negative flashes handled the same in gl/soft until CL_AddDLights
                dl.getColor()[0] = -1;
                dl.getColor()[1] = -1;
                dl.getColor()[2] = -1;
                Sound.StartSound(null, i, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/disint2.wav"), volume, Defines.ATTN_NORM, 0);
                break;
            case Defines.MZ_NUKE1:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 100);
                break;
            case Defines.MZ_NUKE2:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 100);
                break;
            case Defines.MZ_NUKE4:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 0;
                dl.getColor()[2] = 1;
                dl.setDie(Context.cl.time + 100);
                break;
            case Defines.MZ_NUKE8:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 1;
                dl.setDie(Context.cl.time + 100);
                break;
            //	   PGM
            //	   ======================
        }
    }

    // stack variable
    private static final float[] origin = {0, 0, 0};
    private static final float[] forward = {0, 0, 0};
    private static final float[] right = {0, 0, 0};

    /*
     * ============== CL_ParseMuzzleFlash2 ==============
     */
    static void ParseMuzzleFlash2() {
        String soundname;

        int ent = TBuffer.ReadShort(Context.net_message);
        if (ent < 1 || ent >= Defines.MAX_EDICTS)
            Command.Error(Defines.ERR_DROP, "CL_ParseMuzzleFlash2: bad entity");

        int flash_number = TBuffer.ReadByte(Context.net_message);

        // locate the origin
        Math3D.AngleVectors(Context.cl_entities[ent].current.angles, forward, right, null);
        origin[0] = Context.cl_entities[ent].current.origin[0] + forward[0] * M_Flash.monster_flash_offset[flash_number][0] + right[0]
                * M_Flash.monster_flash_offset[flash_number][1];
        origin[1] = Context.cl_entities[ent].current.origin[1] + forward[1] * M_Flash.monster_flash_offset[flash_number][0] + right[1]
                * M_Flash.monster_flash_offset[flash_number][1];
        origin[2] = Context.cl_entities[ent].current.origin[2] + forward[2] * M_Flash.monster_flash_offset[flash_number][0] + right[2]
                * M_Flash.monster_flash_offset[flash_number][1] + M_Flash.monster_flash_offset[flash_number][2];

        TClDynamicLight dl = AllocDlight(ent);
        Math3D.VectorCopy(origin, dl.getOrigin());
        dl.setRadius(200 + (Context.rnd.nextInt() & 31));
        dl.setMinlight(32);
        dl.setDie(Context.cl.time); // + 0.1;

        switch (flash_number) {
            case Defines.MZ2_INFANTRY_MACHINEGUN_1:
            case Defines.MZ2_INFANTRY_MACHINEGUN_2:
            case Defines.MZ2_INFANTRY_MACHINEGUN_3:
            case Defines.MZ2_INFANTRY_MACHINEGUN_4:
            case Defines.MZ2_INFANTRY_MACHINEGUN_5:
            case Defines.MZ2_INFANTRY_MACHINEGUN_6:
            case Defines.MZ2_INFANTRY_MACHINEGUN_7:
            case Defines.MZ2_INFANTRY_MACHINEGUN_8:
            case Defines.MZ2_INFANTRY_MACHINEGUN_9:
            case Defines.MZ2_INFANTRY_MACHINEGUN_10:
            case Defines.MZ2_INFANTRY_MACHINEGUN_11:
            case Defines.MZ2_INFANTRY_MACHINEGUN_12:
            case Defines.MZ2_INFANTRY_MACHINEGUN_13:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("infantry/infatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_SOLDIER_MACHINEGUN_1:
            case Defines.MZ2_SOLDIER_MACHINEGUN_2:
            case Defines.MZ2_SOLDIER_MACHINEGUN_3:
            case Defines.MZ2_SOLDIER_MACHINEGUN_4:
            case Defines.MZ2_SOLDIER_MACHINEGUN_5:
            case Defines.MZ2_SOLDIER_MACHINEGUN_6:
            case Defines.MZ2_SOLDIER_MACHINEGUN_7:
            case Defines.MZ2_SOLDIER_MACHINEGUN_8:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("soldier/solatck3.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_GUNNER_MACHINEGUN_1:
            case Defines.MZ2_GUNNER_MACHINEGUN_2:
            case Defines.MZ2_GUNNER_MACHINEGUN_3:
            case Defines.MZ2_GUNNER_MACHINEGUN_4:
            case Defines.MZ2_GUNNER_MACHINEGUN_5:
            case Defines.MZ2_GUNNER_MACHINEGUN_6:
            case Defines.MZ2_GUNNER_MACHINEGUN_7:
            case Defines.MZ2_GUNNER_MACHINEGUN_8:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("gunner/gunatck2.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_ACTOR_MACHINEGUN_1:
            case Defines.MZ2_SUPERTANK_MACHINEGUN_1:
            case Defines.MZ2_SUPERTANK_MACHINEGUN_2:
            case Defines.MZ2_SUPERTANK_MACHINEGUN_3:
            case Defines.MZ2_SUPERTANK_MACHINEGUN_4:
            case Defines.MZ2_SUPERTANK_MACHINEGUN_5:
            case Defines.MZ2_SUPERTANK_MACHINEGUN_6:
            case Defines.MZ2_TURRET_MACHINEGUN: // PGM
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;

                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("infantry/infatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_BOSS2_MACHINEGUN_L1:
            case Defines.MZ2_BOSS2_MACHINEGUN_L2:
            case Defines.MZ2_BOSS2_MACHINEGUN_L3:
            case Defines.MZ2_BOSS2_MACHINEGUN_L4:
            case Defines.MZ2_BOSS2_MACHINEGUN_L5:
            case Defines.MZ2_CARRIER_MACHINEGUN_L1: // PMM
            case Defines.MZ2_CARRIER_MACHINEGUN_L2: // PMM
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;

                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("infantry/infatck1.wav"), 1, Defines.ATTN_NONE, 0);
                break;

            case Defines.MZ2_SOLDIER_BLASTER_1:
            case Defines.MZ2_SOLDIER_BLASTER_2:
            case Defines.MZ2_SOLDIER_BLASTER_3:
            case Defines.MZ2_SOLDIER_BLASTER_4:
            case Defines.MZ2_SOLDIER_BLASTER_5:
            case Defines.MZ2_SOLDIER_BLASTER_6:
            case Defines.MZ2_SOLDIER_BLASTER_7:
            case Defines.MZ2_SOLDIER_BLASTER_8:
            case Defines.MZ2_TURRET_BLASTER: // PGM
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("soldier/solatck2.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_FLYER_BLASTER_1:
            case Defines.MZ2_FLYER_BLASTER_2:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("flyer/flyatck3.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_MEDIC_BLASTER_1:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("medic/medatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_HOVER_BLASTER_1:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("hover/hovatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_FLOAT_BLASTER_1:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("floater/fltatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_SOLDIER_SHOTGUN_1:
            case Defines.MZ2_SOLDIER_SHOTGUN_2:
            case Defines.MZ2_SOLDIER_SHOTGUN_3:
            case Defines.MZ2_SOLDIER_SHOTGUN_4:
            case Defines.MZ2_SOLDIER_SHOTGUN_5:
            case Defines.MZ2_SOLDIER_SHOTGUN_6:
            case Defines.MZ2_SOLDIER_SHOTGUN_7:
            case Defines.MZ2_SOLDIER_SHOTGUN_8:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("soldier/solatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_TANK_BLASTER_1:
            case Defines.MZ2_TANK_BLASTER_2:
            case Defines.MZ2_TANK_BLASTER_3:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("tank/tnkatck3.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_TANK_MACHINEGUN_1:
            case Defines.MZ2_TANK_MACHINEGUN_2:
            case Defines.MZ2_TANK_MACHINEGUN_3:
            case Defines.MZ2_TANK_MACHINEGUN_4:
            case Defines.MZ2_TANK_MACHINEGUN_5:
            case Defines.MZ2_TANK_MACHINEGUN_6:
            case Defines.MZ2_TANK_MACHINEGUN_7:
            case Defines.MZ2_TANK_MACHINEGUN_8:
            case Defines.MZ2_TANK_MACHINEGUN_9:
            case Defines.MZ2_TANK_MACHINEGUN_10:
            case Defines.MZ2_TANK_MACHINEGUN_11:
            case Defines.MZ2_TANK_MACHINEGUN_12:
            case Defines.MZ2_TANK_MACHINEGUN_13:
            case Defines.MZ2_TANK_MACHINEGUN_14:
            case Defines.MZ2_TANK_MACHINEGUN_15:
            case Defines.MZ2_TANK_MACHINEGUN_16:
            case Defines.MZ2_TANK_MACHINEGUN_17:
            case Defines.MZ2_TANK_MACHINEGUN_18:
            case Defines.MZ2_TANK_MACHINEGUN_19:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                //Com_sprintf(soundname, sizeof(soundname), "tank/tnkatk2%c.wav",
                // 'a' + rand() % 5);
                soundname = "tank/tnkatk2" + (char) ('a' + Context.rnd.nextInt(5)) + ".wav";
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound(soundname), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_CHICK_ROCKET_1:
            case Defines.MZ2_TURRET_ROCKET: // PGM
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0.2f;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("chick/chkatck2.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_TANK_ROCKET_1:
            case Defines.MZ2_TANK_ROCKET_2:
            case Defines.MZ2_TANK_ROCKET_3:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0.2f;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("tank/tnkatck1.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_SUPERTANK_ROCKET_1:
            case Defines.MZ2_SUPERTANK_ROCKET_2:
            case Defines.MZ2_SUPERTANK_ROCKET_3:
            case Defines.MZ2_BOSS2_ROCKET_1:
            case Defines.MZ2_BOSS2_ROCKET_2:
            case Defines.MZ2_BOSS2_ROCKET_3:
            case Defines.MZ2_BOSS2_ROCKET_4:
            case Defines.MZ2_CARRIER_ROCKET_1:
                //		case MZ2_CARRIER_ROCKET_2:
                //		case MZ2_CARRIER_ROCKET_3:
                //		case MZ2_CARRIER_ROCKET_4:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0.2f;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("tank/rocket.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_GUNNER_GRENADE_1:
            case Defines.MZ2_GUNNER_GRENADE_2:
            case Defines.MZ2_GUNNER_GRENADE_3:
            case Defines.MZ2_GUNNER_GRENADE_4:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("gunner/gunatck3.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_GLADIATOR_RAILGUN_1:
                // PMM
            case Defines.MZ2_CARRIER_RAILGUN:
            case Defines.MZ2_WIDOW_RAIL:
                // pmm
                dl.getColor()[0] = 0.5f;
                dl.getColor()[1] = 0.5f;
                dl.getColor()[2] = 1.0f;
                break;

            //	   --- Xian'entityState shit starts ---
            case Defines.MZ2_MAKRON_BFG:
                dl.getColor()[0] = 0.5f;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0.5f;
                //Sound.StartSound (null, entityDict, CHAN_WEAPON,
                // Sound.RegisterSound("makron/bfg_fire.wav"), 1, ATTN_NORM, 0);
                break;

            case Defines.MZ2_MAKRON_BLASTER_1:
            case Defines.MZ2_MAKRON_BLASTER_2:
            case Defines.MZ2_MAKRON_BLASTER_3:
            case Defines.MZ2_MAKRON_BLASTER_4:
            case Defines.MZ2_MAKRON_BLASTER_5:
            case Defines.MZ2_MAKRON_BLASTER_6:
            case Defines.MZ2_MAKRON_BLASTER_7:
            case Defines.MZ2_MAKRON_BLASTER_8:
            case Defines.MZ2_MAKRON_BLASTER_9:
            case Defines.MZ2_MAKRON_BLASTER_10:
            case Defines.MZ2_MAKRON_BLASTER_11:
            case Defines.MZ2_MAKRON_BLASTER_12:
            case Defines.MZ2_MAKRON_BLASTER_13:
            case Defines.MZ2_MAKRON_BLASTER_14:
            case Defines.MZ2_MAKRON_BLASTER_15:
            case Defines.MZ2_MAKRON_BLASTER_16:
            case Defines.MZ2_MAKRON_BLASTER_17:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("makron/blaster.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_JORG_MACHINEGUN_L1:
            case Defines.MZ2_JORG_MACHINEGUN_L2:
            case Defines.MZ2_JORG_MACHINEGUN_L3:
            case Defines.MZ2_JORG_MACHINEGUN_L4:
            case Defines.MZ2_JORG_MACHINEGUN_L5:
            case Defines.MZ2_JORG_MACHINEGUN_L6:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("boss3/xfire.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_JORG_MACHINEGUN_R1:
            case Defines.MZ2_JORG_MACHINEGUN_R2:
            case Defines.MZ2_JORG_MACHINEGUN_R3:
            case Defines.MZ2_JORG_MACHINEGUN_R4:
            case Defines.MZ2_JORG_MACHINEGUN_R5:
            case Defines.MZ2_JORG_MACHINEGUN_R6:
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                break;

            case Defines.MZ2_JORG_BFG_1:
                dl.getColor()[0] = 0.5f;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0.5f;
                break;

            case Defines.MZ2_BOSS2_MACHINEGUN_R1:
            case Defines.MZ2_BOSS2_MACHINEGUN_R2:
            case Defines.MZ2_BOSS2_MACHINEGUN_R3:
            case Defines.MZ2_BOSS2_MACHINEGUN_R4:
            case Defines.MZ2_BOSS2_MACHINEGUN_R5:
            case Defines.MZ2_CARRIER_MACHINEGUN_R1: // PMM
            case Defines.MZ2_CARRIER_MACHINEGUN_R2: // PMM

                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;

                ParticleEffect(origin, Context.vec3_origin, 0, 40);
                CL_tent.SmokeAndFlash(origin);
                break;

            //	   ======
            //	   ROGUE
            case Defines.MZ2_STALKER_BLASTER:
            case Defines.MZ2_DAEDALUS_BLASTER:
            case Defines.MZ2_MEDIC_BLASTER_2:
            case Defines.MZ2_WIDOW_BLASTER:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP1:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP2:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP3:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP4:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP5:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP6:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP7:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP8:
            case Defines.MZ2_WIDOW_BLASTER_SWEEP9:
            case Defines.MZ2_WIDOW_BLASTER_100:
            case Defines.MZ2_WIDOW_BLASTER_90:
            case Defines.MZ2_WIDOW_BLASTER_80:
            case Defines.MZ2_WIDOW_BLASTER_70:
            case Defines.MZ2_WIDOW_BLASTER_60:
            case Defines.MZ2_WIDOW_BLASTER_50:
            case Defines.MZ2_WIDOW_BLASTER_40:
            case Defines.MZ2_WIDOW_BLASTER_30:
            case Defines.MZ2_WIDOW_BLASTER_20:
            case Defines.MZ2_WIDOW_BLASTER_10:
            case Defines.MZ2_WIDOW_BLASTER_0:
            case Defines.MZ2_WIDOW_BLASTER_10L:
            case Defines.MZ2_WIDOW_BLASTER_20L:
            case Defines.MZ2_WIDOW_BLASTER_30L:
            case Defines.MZ2_WIDOW_BLASTER_40L:
            case Defines.MZ2_WIDOW_BLASTER_50L:
            case Defines.MZ2_WIDOW_BLASTER_60L:
            case Defines.MZ2_WIDOW_BLASTER_70L:
            case Defines.MZ2_WIDOW_RUN_1:
            case Defines.MZ2_WIDOW_RUN_2:
            case Defines.MZ2_WIDOW_RUN_3:
            case Defines.MZ2_WIDOW_RUN_4:
            case Defines.MZ2_WIDOW_RUN_5:
            case Defines.MZ2_WIDOW_RUN_6:
            case Defines.MZ2_WIDOW_RUN_7:
            case Defines.MZ2_WIDOW_RUN_8:
                dl.getColor()[0] = 0;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("tank/tnkatck3.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_WIDOW_DISRUPTOR:
                dl.getColor()[0] = -1;
                dl.getColor()[1] = -1;
                dl.getColor()[2] = -1;
                Sound.StartSound(null, ent, Defines.CHAN_WEAPON, Sound.RegisterSound("weapons/disint2.wav"), 1, Defines.ATTN_NORM, 0);
                break;

            case Defines.MZ2_WIDOW_PLASMABEAM:
            case Defines.MZ2_WIDOW2_BEAMER_1:
            case Defines.MZ2_WIDOW2_BEAMER_2:
            case Defines.MZ2_WIDOW2_BEAMER_3:
            case Defines.MZ2_WIDOW2_BEAMER_4:
            case Defines.MZ2_WIDOW2_BEAMER_5:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_1:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_2:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_3:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_4:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_5:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_6:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_7:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_8:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_9:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_10:
            case Defines.MZ2_WIDOW2_BEAM_SWEEP_11:
                dl.setRadius(300 + (Context.rnd.nextInt() & 100));
                dl.getColor()[0] = 1;
                dl.getColor()[1] = 1;
                dl.getColor()[2] = 0;
                dl.setDie(Context.cl.time + 200);
                break;
            //	   ROGUE
            //	   ======

            //	   --- Xian'entityState shit ends ---

        }
    }

    /*
     * =============== CL_AddDLights
     *
     * ===============
     */
    static void AddDLights() {
        TClDynamicLight dl;

        //	  =====
        //	  PGM
        if (Context.vidref_val == Defines.VIDREF_GL) {
            for (int i = 0; i < Defines.MAX_DLIGHTS; i++) {
                dl = cl_dlights[i];
                if (dl.getRadius() == 0.0f)
                    continue;
                V.AddLight(dl.getOrigin(), dl.getRadius(), dl.getColor()[0], dl.getColor()[1], dl.getColor()[2]);
            }
        } else {
            for (int i = 0; i < Defines.MAX_DLIGHTS; i++) {
                dl = cl_dlights[i];
                if (dl.getRadius() == 0.0f)
                    continue;

                // negative light in software. only black allowed
                if ((dl.getColor()[0] < 0) || (dl.getColor()[1] < 0) || (dl.getColor()[2] < 0)) {
                    dl.setRadius(-(dl.getRadius()));
                    dl.getColor()[0] = 1;
                    dl.getColor()[1] = 1;
                    dl.getColor()[2] = 1;
                }
                V.AddLight(dl.getOrigin(), dl.getRadius(), dl.getColor()[0], dl.getColor()[1], dl.getColor()[2]);
            }
        }
        //	  PGM
        //	  =====
    }

    /*
     * =============== CL_ParticleEffect
     *
     * Wall impact puffs ===============
     */
    static void ParticleEffect(float[] org, float[] dir, int color, int count) {

        for (int i = 0; i < count; i++) {
            if (free_particles == null)
                return;
            TCParticle p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = color + (Lib.rand() & 7);

            float d = Lib.rand() & 31;
            for (int j = 0; j < 3; j++) {
                p.org[j] = org[j] + ((Lib.rand() & 7) - 4) + d * dir[j];
                p.vel[j] = Lib.crand() * 20;
            }

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    /*
     * =============== CL_ParticleEffect2 ===============
     */
    static void ParticleEffect2(float[] org, float[] dir, int color, int count) {
        int j;
        TCParticle p;
        float d;

        for (int i = 0; i < count; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = color;

            d = Lib.rand() & 7;
            for (j = 0; j < 3; j++) {
                p.org[j] = org[j] + ((Lib.rand() & 7) - 4) + d * dir[j];
                p.vel[j] = Lib.crand() * 20;
            }

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    //	   RAFAEL
	/*
	 * =============== CL_ParticleEffect3 ===============
	 */
    static void ParticleEffect3(float[] org, float[] dir, int color, int count) {
        int j;
        TCParticle p;
        float d;

        for (int i = 0; i < count; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = color;

            d = Lib.rand() & 7;
            for (j = 0; j < 3; j++) {
                p.org[j] = org[j] + ((Lib.rand() & 7) - 4) + d * dir[j];
                p.vel[j] = Lib.crand() * 20;
            }

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    /*
     * =============== CL_TeleporterParticles ===============
     */
    static void TeleporterParticles(TEntityState ent) {
        int j;
        TCParticle p;

        for (int i = 0; i < 8; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = 0xdb;

            for (j = 0; j < 2; j++) {
                p.org[j] = ent.origin[j] - 16 + (Lib.rand() & 31);
                p.vel[j] = Lib.crand() * 14;
            }

            p.org[2] = ent.origin[2] - 8 + (Lib.rand() & 7);
            p.vel[2] = 80 + (Lib.rand() & 7);

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -0.5f;
        }
    }

    /*
     * =============== CL_LogoutEffect
     *
     * ===============
     */
    static void LogoutEffect(float[] org, int type) {
        int j;
        TCParticle p;

        for (int i = 0; i < 500; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;

            if (type == Defines.MZ_LOGIN)
                p.color = 0xd0 + (Lib.rand() & 7); // green
            else if (type == Defines.MZ_LOGOUT)
                p.color = 0x40 + (Lib.rand() & 7); // red
            else
                p.color = 0xe0 + (Lib.rand() & 7); // yellow

            p.org[0] = org[0] - 16 + Context.rnd.nextFloat() * 32;
            p.org[1] = org[1] - 16 + Context.rnd.nextFloat() * 32;
            p.org[2] = org[2] - 24 + Context.rnd.nextFloat() * 56;

            for (j = 0; j < 3; j++)
                p.vel[j] = Lib.crand() * 20;

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    /*
     * =============== CL_ItemRespawnParticles
     *
     * ===============
     */
    static void ItemRespawnParticles(float[] org) {
        int j;
        TCParticle p;

        for (int i = 0; i < 64; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;

            p.color = 0xd4 + (Lib.rand() & 3); // green

            p.org[0] = org[0] + Lib.crand() * 8;
            p.org[1] = org[1] + Lib.crand() * 8;
            p.org[2] = org[2] + Lib.crand() * 8;

            for (j = 0; j < 3; j++)
                p.vel[j] = Lib.crand() * 8;

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY * 0.2f;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    /*
     * =============== CL_ExplosionParticles ===============
     */
    static void ExplosionParticles(float[] org) {
        int j;
        TCParticle p;

        for (int i = 0; i < 256; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = 0xe0 + (Lib.rand() & 7);

            for (j = 0; j < 3; j++) {
                p.org[j] = org[j] + ((Lib.rand() % 32) - 16);
                p.vel[j] = (Lib.rand() % 384) - 192;
            }

            p.accel[0] = p.accel[1] = 0.0f;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -0.8f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    static void BigTeleportParticles(float[] org) {
        TCParticle p;
        float angle, dist;

        for (int i = 0; i < 4096; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;

            p.color = colortable[Lib.rand() & 3];

            angle = (float) (Math.PI * 2 * (Lib.rand() & 1023) / 1023.0);
            dist = Lib.rand() & 31;
            p.org[0] = (float) (org[0] + Math.cos(angle) * dist);
            p.vel[0] = (float) (Math.cos(angle) * (70 + (Lib.rand() & 63)));
            p.accel[0] = (float) (-Math.cos(angle) * 100);

            p.org[1] = (float) (org[1] + Math.sin(angle) * dist);
            p.vel[1] = (float) (Math.sin(angle) * (70 + (Lib.rand() & 63)));
            p.accel[1] = (float) (-Math.sin(angle) * 100);

            p.org[2] = org[2] + 8 + (Lib.rand() % 90);
            p.vel[2] = -100 + (Lib.rand() & 31);
            p.accel[2] = PARTICLE_GRAVITY * 4;
            p.alpha = 1.0f;

            p.alphavel = -0.3f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    /*
     * =============== CL_BlasterParticles
     *
     * Wall impact puffs ===============
     */
    static void BlasterParticles(float[] org, float[] dir) {
        int j;
        TCParticle p;
        float d;

        int count = 40;
        for (int i = 0; i < count; i++) {
            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = 0xe0 + (Lib.rand() & 7);

            d = Lib.rand() & 15;
            for (j = 0; j < 3; j++) {
                p.org[j] = org[j] + ((Lib.rand() & 7) - 4) + d * dir[j];
                p.vel[j] = dir[j] * 30 + Lib.crand() * 40;
            }

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -1.0f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    // stack variable
    private static final float[] move = {0, 0, 0};
    private static final float[] vec = {0, 0, 0};

    /*
     * =============== CL_BlasterTrail
     *
     * ===============
     */
    static void BlasterTrail(float[] start, float[] end) {
        float len;
        int j;
        TCParticle p;
        int dec;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        len = Math3D.VectorNormalize(vec);

        dec = 5;
        Math3D.VectorScale(vec, 5, vec);

        // FIXME: this is a really silly way to have a loop
        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = Context.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (0.3f + Context.rnd.nextFloat() * 0.2f);
            p.color = 0xe0;
            for (j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand();
                p.vel[j] = Lib.crand() * 5;
                p.accel[j] = 0;
            }

            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // move, vec
	/*
	 * ===============
	 *  CL_FlagTrail
	 * ===============
	 */
    static void FlagTrail(float[] start, float[] end, float color) {
        float len;
        int j;
        TCParticle p;
        int dec;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        len = Math3D.VectorNormalize(vec);

        dec = 5;
        Math3D.VectorScale(vec, 5, vec);

        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = Context.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (0.8f + Context.rnd.nextFloat() * 0.2f);
            p.color = color;
            for (j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand() * 16;
                p.vel[j] = Lib.crand() * 5;
                p.accel[j] = 0;
            }

            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // move, vec
	/*
	 * =============== CL_DiminishingTrail
	 * 
	 * ===============
	 */
    static void DiminishingTrail(float[] start, float[] end, TClEentity old, int flags) {
        TCParticle p;
        float orgscale;
        float velscale;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        float len = Math3D.VectorNormalize(vec);

        float dec = 0.5f;
        Math3D.VectorScale(vec, dec, vec);

        if (old.trailcount > 900) {
            orgscale = 4;
            velscale = 15;
        } else if (old.trailcount > 800) {
            orgscale = 2;
            velscale = 10;
        } else {
            orgscale = 1;
            velscale = 5;
        }

        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;

            // drop less particles as it flies
            if ((Lib.rand() & 1023) < old.trailcount) {
                p = free_particles;
                free_particles = p.next;
                p.next = active_particles;
                active_particles = p;
                Math3D.VectorClear(p.accel);

                p.time = Context.cl.time;

                if ((flags & Defines.EF_GIB) != 0) {
                    p.alpha = 1.0f;
                    p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.4f);
                    p.color = 0xe8 + (Lib.rand() & 7);
                    for (int j = 0; j < 3; j++) {
                        p.org[j] = move[j] + Lib.crand() * orgscale;
                        p.vel[j] = Lib.crand() * velscale;
                        p.accel[j] = 0;
                    }
                    p.vel[2] -= PARTICLE_GRAVITY;
                } else if ((flags & Defines.EF_GREENGIB) != 0) {
                    p.alpha = 1.0f;
                    p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.4f);
                    p.color = 0xdb + (Lib.rand() & 7);
                    for (int j = 0; j < 3; j++) {
                        p.org[j] = move[j] + Lib.crand() * orgscale;
                        p.vel[j] = Lib.crand() * velscale;
                        p.accel[j] = 0;
                    }
                    p.vel[2] -= PARTICLE_GRAVITY;
                } else {
                    p.alpha = 1.0f;
                    p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.2f);
                    p.color = 4 + (Lib.rand() & 7);
                    for (int j = 0; j < 3; j++) {
                        p.org[j] = move[j] + Lib.crand() * orgscale;
                        p.vel[j] = Lib.crand() * velscale;
                    }
                    p.accel[2] = 20;
                }
            }

            old.trailcount -= 5;
            if (old.trailcount < 100)
                old.trailcount = 100;
            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // move, vec
	/*
	 * =============== CL_RocketTrail
	 * 
	 * ===============
	 */
    static void RocketTrail(float[] start, float[] end, TClEentity old) {
        float len;
        int j;
        TCParticle p;
        float dec;

        // smoke
        DiminishingTrail(start, end, old, Defines.EF_ROCKET);

        // fire
        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        len = Math3D.VectorNormalize(vec);

        dec = 1;
        Math3D.VectorScale(vec, dec, vec);

        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;

            if ((Lib.rand() & 7) == 0) {
                p = free_particles;
                free_particles = p.next;
                p.next = active_particles;
                active_particles = p;

                Math3D.VectorClear(p.accel);
                p.time = Context.cl.time;

                p.alpha = 1.0f;
                p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.2f);
                p.color = 0xdc + (Lib.rand() & 3);
                for (j = 0; j < 3; j++) {
                    p.org[j] = move[j] + Lib.crand() * 5;
                    p.vel[j] = Lib.crand() * 20;
                }
                p.accel[2] = -PARTICLE_GRAVITY;
            }
            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // move, vec
	/*
	 * =============== CL_RailTrail
	 * 
	 * ===============
	 */
    static void RailTrail(float[] start, float[] end) {
        float len;
        int j;
        TCParticle p;
        float dec;
        float[] right = new float[3];
        float[] up = new float[3];
        int i;
        float d, c, s;
        float[] dir = new float[3];
        byte clr = 0x74;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        len = Math3D.VectorNormalize(vec);

        Math3D.MakeNormalVectors(vec, right, up);

        for (i = 0; i < len; i++) {
            if (free_particles == null)
                return;

            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            Math3D.VectorClear(p.accel);

            d = i * 0.1f;
            c = (float) Math.cos(d);
            s = (float) Math.sin(d);

            Math3D.VectorScale(right, c, dir);
            Math3D.VectorMA(dir, s, up, dir);

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.2f);
            p.color = clr + (Lib.rand() & 7);
            for (j = 0; j < 3; j++) {
                p.org[j] = move[j] + dir[j] * 3;
                p.vel[j] = dir[j] * 6;
            }

            Math3D.VectorAdd(move, vec, move);
        }

        dec = 0.75f;
        Math3D.VectorScale(vec, dec, vec);
        Math3D.VectorCopy(start, move);

        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            Math3D.VectorClear(p.accel);

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (0.6f + Context.rnd.nextFloat() * 0.2f);
            p.color = 0x0 + Lib.rand() & 15;

            for (j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand() * 3;
                p.vel[j] = Lib.crand() * 3;
                p.accel[j] = 0;
            }

            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // move, vec
	/*
	 * =============== CL_IonripperTrail ===============
	 */
    static void IonripperTrail(float[] start, float[] ent) {
        float len;
        int j;
        TCParticle p;
        int dec;
        int left = 0;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(ent, start, vec);
        len = Math3D.VectorNormalize(vec);

        dec = 5;
        Math3D.VectorScale(vec, 5, vec);

        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = Context.cl.time;
            p.alpha = 0.5f;
            p.alphavel = -1.0f / (0.3f + Context.rnd.nextFloat() * 0.2f);
            p.color = 0xe4 + (Lib.rand() & 3);

            for (j = 0; j < 3; j++) {
                p.org[j] = move[j];
                p.accel[j] = 0;
            }
            if (left != 0) {
                left = 0;
                p.vel[0] = 10;
            } else {
                left = 1;
                p.vel[0] = -10;
            }

            p.vel[1] = 0;
            p.vel[2] = 0;

            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // move, vec
	/*
	 * =============== CL_BubbleTrail
	 * 
	 * ===============
	 */
    static void BubbleTrail(float[] start, float[] end) {
        float len;
        int i, j;
        TCParticle p;
        float dec;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        len = Math3D.VectorNormalize(vec);

        dec = 32;
        Math3D.VectorScale(vec, dec, vec);

        for (i = 0; i < len; i += dec) {
            if (free_particles == null)
                return;

            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            Math3D.VectorClear(p.accel);
            p.time = Context.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (1.0f + Context.rnd.nextFloat() * 0.2f);
            p.color = 4 + (Lib.rand() & 7);
            for (j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand() * 2;
                p.vel[j] = Lib.crand() * 5;
            }
            p.vel[2] += 6;

            Math3D.VectorAdd(move, vec, move);
        }
    }

    // stack variable
    // forward
	/*
	 * =============== CL_FlyParticles ===============
	 */
    static void FlyParticles(float[] origin, int count) {
        int i;
        TCParticle p;
        float angle;
        float sp, sy, cp, cy;
        float dist = 64;
        float ltime;

        if (count > Defines.NUMVERTEXNORMALS)
            count = Defines.NUMVERTEXNORMALS;

        if (avelocities[0][0] == 0.0f) {
            for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
                avelocities[i][0] = (Lib.rand() & 255) * 0.01f;
                avelocities[i][1] = (Lib.rand() & 255) * 0.01f;
                avelocities[i][2] = (Lib.rand() & 255) * 0.01f;
            }
        }

        ltime = Context.cl.time / 1000.0f;
        for (i = 0; i < count; i += 2) {
            angle = ltime * avelocities[i][0];
            sy = (float) Math.sin(angle);
            cy = (float) Math.cos(angle);
            angle = ltime * avelocities[i][1];
            sp = (float) Math.sin(angle);
            cp = (float) Math.cos(angle);
            angle = ltime * avelocities[i][2];

            forward[0] = cp * cy;
            forward[1] = cp * sy;
            forward[2] = -sp;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;

            dist = (float) Math.sin(ltime + i) * 64;
            p.org[0] = origin[0] + Context.bytedirs[i][0] * dist + forward[0] * BEAMLENGTH;
            p.org[1] = origin[1] + Context.bytedirs[i][1] * dist + forward[1] * BEAMLENGTH;
            p.org[2] = origin[2] + Context.bytedirs[i][2] * dist + forward[2] * BEAMLENGTH;

            Math3D.VectorClear(p.vel);
            Math3D.VectorClear(p.accel);

            p.color = 0;
            //p.colorvel = 0;

            p.alpha = 1;
            p.alphavel = -100;
        }
    }

    static void FlyEffect(TClEentity ent, float[] origin) {
        int n;
        int count;
        int starttime;

        if (ent.fly_stoptime < Context.cl.time) {
            starttime = Context.cl.time;
            ent.fly_stoptime = Context.cl.time + 60000;
        } else {
            starttime = ent.fly_stoptime - 60000;
        }

        n = Context.cl.time - starttime;
        if (n < 20000)
            count = (int) ((n * 162) / 20000.0);
        else {
            n = ent.fly_stoptime - Context.cl.time;
            if (n < 20000)
                count = (int) ((n * 162) / 20000.0);
            else
                count = 162;
        }

        FlyParticles(origin, count);
    }

    // stack variable
    private static final float[] v = {0, 0, 0};

    // forward
	/*
	 * =============== CL_BfgParticles ===============
	 */
    //#define BEAMLENGTH 16
    static void BfgParticles(TEntity ent) {
        int i;
        TCParticle p;
        float angle;
        float sp, sy, cp, cy;
        float dist = 64;
        float ltime;

        if (avelocities[0][0] == 0.0f) {
            for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
                avelocities[i][0] = (Lib.rand() & 255) * 0.01f;
                avelocities[i][1] = (Lib.rand() & 255) * 0.01f;
                avelocities[i][2] = (Lib.rand() & 255) * 0.01f;
            }
        }

        ltime = Context.cl.time / 1000.0f;
        for (i = 0; i < Defines.NUMVERTEXNORMALS; i++) {
            angle = ltime * avelocities[i][0];
            sy = (float) Math.sin(angle);
            cy = (float) Math.cos(angle);
            angle = ltime * avelocities[i][1];
            sp = (float) Math.sin(angle);
            cp = (float) Math.cos(angle);
            angle = ltime * avelocities[i][2];

            forward[0] = cp * cy;
            forward[1] = cp * sy;
            forward[2] = -sp;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;

            dist = (float) (Math.sin(ltime + i) * 64);
            p.org[0] = ent.origin[0] + Context.bytedirs[i][0] * dist + forward[0] * BEAMLENGTH;
            p.org[1] = ent.origin[1] + Context.bytedirs[i][1] * dist + forward[1] * BEAMLENGTH;
            p.org[2] = ent.origin[2] + Context.bytedirs[i][2] * dist + forward[2] * BEAMLENGTH;

            Math3D.VectorClear(p.vel);
            Math3D.VectorClear(p.accel);

            Math3D.VectorSubtract(p.org, ent.origin, v);
            dist = Math3D.VectorLength(v) / 90.0f;
            p.color = (float) Math.floor(0xd0 + dist * 7);
            //p.colorvel = 0;

            p.alpha = 1.0f - dist;
            p.alphavel = -100;
        }
    }

    // stack variable
    // move, vec
    private static final float[] start = {0, 0, 0};
    private static final float[] end = {0, 0, 0};

    /*
     * =============== CL_TrapParticles ===============
     */
    //	   RAFAEL
    static void TrapParticles(TEntity ent) {
        float len;
        int j;
        TCParticle p;
        int dec;

        ent.origin[2] -= 14;
        Math3D.VectorCopy(ent.origin, start);
        Math3D.VectorCopy(ent.origin, end);
        end[2] += 64;

        Math3D.VectorCopy(start, move);
        Math3D.VectorSubtract(end, start, vec);
        len = Math3D.VectorNormalize(vec);

        dec = 5;
        Math3D.VectorScale(vec, 5, vec);

        // FIXME: this is a really silly way to have a loop
        while (len > 0) {
            len -= dec;

            if (free_particles == null)
                return;
            p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;
            Math3D.VectorClear(p.accel);

            p.time = Context.cl.time;

            p.alpha = 1.0f;
            p.alphavel = -1.0f / (0.3f + Context.rnd.nextFloat() * 0.2f);
            p.color = 0xe0;
            for (j = 0; j < 3; j++) {
                p.org[j] = move[j] + Lib.crand();
                p.vel[j] = Lib.crand() * 15;
                p.accel[j] = 0;
            }
            p.accel[2] = PARTICLE_GRAVITY;

            Math3D.VectorAdd(move, vec, move);
        }

        int i, k;
        //TCParticle p;
        float vel;
        float[] dir = new float[3];
        float[] org = new float[3];

        ent.origin[2] += 14;
        Math3D.VectorCopy(ent.origin, org);

        for (i = -2; i <= 2; i += 4)
            for (j = -2; j <= 2; j += 4)
                for (k = -2; k <= 4; k += 4) {
                    if (free_particles == null)
                        return;
                    p = free_particles;
                    free_particles = p.next;
                    p.next = active_particles;
                    active_particles = p;

                    p.time = Context.cl.time;
                    p.color = 0xe0 + (Lib.rand() & 3);

                    p.alpha = 1.0f;
                    p.alphavel = -1.0f / (0.3f + (Lib.rand() & 7) * 0.02f);

                    p.org[0] = org[0] + i + ((Lib.rand() & 23) * Lib.crand());
                    p.org[1] = org[1] + j + ((Lib.rand() & 23) * Lib.crand());
                    p.org[2] = org[2] + k + ((Lib.rand() & 23) * Lib.crand());

                    dir[0] = j * 8;
                    dir[1] = i * 8;
                    dir[2] = k * 8;

                    Math3D.VectorNormalize(dir);
                    vel = 50 + Lib.rand() & 63;
                    Math3D.VectorScale(dir, vel, p.vel);

                    p.accel[0] = p.accel[1] = 0;
                    p.accel[2] = -PARTICLE_GRAVITY;
                }

    }

    /*
     * =============== CL_BFGExplosionParticles ===============
     */
    //	  FIXME combined with CL_ExplosionParticles
    static void BFGExplosionParticles(float[] org) {

        for (int i = 0; i < 256; i++) {
            if (free_particles == null)
                return;
            TCParticle p = free_particles;
            free_particles = p.next;
            p.next = active_particles;
            active_particles = p;

            p.time = Context.cl.time;
            p.color = 0xd0 + (Lib.rand() & 7);

            for (int j = 0; j < 3; j++) {
                p.org[j] = org[j] + ((Lib.rand() % 32) - 16);
                p.vel[j] = (Lib.rand() % 384) - 192;
            }

            p.accel[0] = p.accel[1] = 0;
            p.accel[2] = -PARTICLE_GRAVITY;
            p.alpha = 1.0f;

            p.alphavel = -0.8f / (0.5f + Context.rnd.nextFloat() * 0.3f);
        }
    }

    // stack variable
    private static final float[] dir = {0, 0, 0};

    /*
     * =============== CL_TeleportParticles
     *
     * ===============
     */
    static void TeleportParticles(float[] org) {

        for (int i = -16; i <= 16; i += 4)
            for (int j = -16; j <= 16; j += 4)
                for (int k = -16; k <= 32; k += 4) {
                    if (free_particles == null)
                        return;
                    TCParticle p = free_particles;
                    free_particles = p.next;
                    p.next = active_particles;
                    active_particles = p;

                    p.time = Context.cl.time;
                    p.color = 7 + (Lib.rand() & 7);

                    p.alpha = 1.0f;
                    p.alphavel = -1.0f / (0.3f + (Lib.rand() & 7) * 0.02f);

                    p.org[0] = org[0] + i + (Lib.rand() & 3);
                    p.org[1] = org[1] + j + (Lib.rand() & 3);
                    p.org[2] = org[2] + k + (Lib.rand() & 3);

                    dir[0] = j * 8;
                    dir[1] = i * 8;
                    dir[2] = k * 8;

                    Math3D.VectorNormalize(dir);
                    float vel = 50 + (Lib.rand() & 63);
                    Math3D.VectorScale(dir, vel, p.vel);

                    p.accel[0] = p.accel[1] = 0;
                    p.accel[2] = -PARTICLE_GRAVITY;
                }
    }

    // stack variable
    private static final float[] org = {0, 0, 0};

    /*
     * =============== CL_AddParticles ===============
     */
    static void AddParticles() {
        TCParticle p, next;
        float alpha;
        float time = 0.0f;
        float time2;
        int color;
        TCParticle active, tail;

        active = null;
        tail = null;

        for (p = active_particles; p != null; p = next) {
            next = p.next;

            // PMM - added INSTANT_PARTICLE handling for heat beam
            if (p.alphavel != INSTANT_PARTICLE) {
                time = (Context.cl.time - p.time) * 0.001f;
                alpha = p.alpha + time * p.alphavel;
                if (alpha <= 0) { // faded out
                    p.next = free_particles;
                    free_particles = p;
                    continue;
                }
            } else {
                alpha = p.alpha;
            }

            p.next = null;
            if (tail == null)
                active = tail = p;
            else {
                tail.next = p;
                tail = p;
            }

            if (alpha > 1.0)
                alpha = 1;
            color = (int) p.color;

            time2 = time * time;

            org[0] = p.org[0] + p.vel[0] * time + p.accel[0] * time2;
            org[1] = p.org[1] + p.vel[1] * time + p.accel[1] * time2;
            org[2] = p.org[2] + p.vel[2] * time + p.accel[2] * time2;

            V.AddParticle(org, color, alpha);
            // PMM
            if (p.alphavel == INSTANT_PARTICLE) {
                p.alphavel = 0.0f;
                p.alpha = 0.0f;
            }
        }

        active_particles = active;
    }

    /*
     * ============== CL_EntityEvent
     *
     * An entity has just been parsed that has an event value
     *
     * the female events are there for backwards compatability ==============
     */
    static void EntityEvent(TEntityState ent) {
        switch (ent.event) {
            case Defines.EV_ITEM_RESPAWN:
                Sound.StartSound(null, ent.number, Defines.CHAN_WEAPON, Sound.RegisterSound("items/respawn1.wav"), 1, Defines.ATTN_IDLE, 0);
                ItemRespawnParticles(ent.origin);
                break;
            case Defines.EV_PLAYER_TELEPORT:
                Sound.StartSound(null, ent.number, Defines.CHAN_WEAPON, Sound.RegisterSound("misc/tele1.wav"), 1, Defines.ATTN_IDLE, 0);
                TeleportParticles(ent.origin);
                break;
            case Defines.EV_FOOTSTEP:
                if (Context.cl_footsteps.value != 0.0f)
                    Sound.StartSound(null, ent.number, Defines.CHAN_BODY, CL_tent.cl_sfx_footsteps[Lib.rand() & 3], 1, Defines.ATTN_NORM, 0);
                break;
            case Defines.EV_FALLSHORT:
                Sound.StartSound(null, ent.number, Defines.CHAN_AUTO, Sound.RegisterSound("player/land1.wav"), 1, Defines.ATTN_NORM, 0);
                break;
            case Defines.EV_FALL:
                Sound.StartSound(null, ent.number, Defines.CHAN_AUTO, Sound.RegisterSound("*fall2.wav"), 1, Defines.ATTN_NORM, 0);
                break;
            case Defines.EV_FALLFAR:
                Sound.StartSound(null, ent.number, Defines.CHAN_AUTO, Sound.RegisterSound("*fall1.wav"), 1, Defines.ATTN_NORM, 0);
                break;
        }
    }

    /*
     * ============== CL_ClearEffects
     *
     * ==============
     */
    static void ClearEffects() {
        free_particles = particles[0];
        active_particles = null;

        for (int i2 = 0; i2 < particles.length - 1; i2++)
            particles[i2].next = particles[i2 + 1];
        particles[particles.length - 1].next = null;

        for (int i1 = 0; i1 < cl_dlights.length; i1++) {
            cl_dlights[i1].clear();
        }

        for (int i = 0; i < cl_lightstyle.length; i++)
            cl_lightstyle[i].clear();

        lastofs = -1;
    }

	/*
	 * ==============================================================
	 * 
	 * PARTICLE MANAGEMENT
	 * 
	 * ==============================================================
	 */

    static final int PARTICLE_GRAVITY = 40;

    static TCParticle active_particles, free_particles;

    /*
     * =============== CL_BigTeleportParticles ===============
     */
    private static int[] colortable = {2 * 8, 13 * 8, 21 * 8, 18 * 8};

    private static final int BEAMLENGTH = 16;

}