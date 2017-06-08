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

// Created on 17.12.2003 by RST.

package jake2.game;

import jake2.Defines;
import jake2.qcommon.Command;
import jake2.util.Lib;
import jake2.util.Math3D;

public class Monster {

    // FIXME monsters should call these with a totally accurate direction
    //	and we can mess it up based on skill. Spread should be for normal
    //	and we can tighten or loosen based on skill. We could muck with
    //	the damages too, but I'm not sure that'entityState such a good idea.
    public static void monster_fire_bullet(TEntityDict self, float[] start,
                                           float[] dir, int damage, int kick, int hspread, int vspread,
                                           int flashtype) {
        GameWeapon.fire_bullet(self, start, dir, damage, kick, hspread, vspread,
                Defines.MOD_UNKNOWN);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the shotgun. */
    public static void monster_fire_shotgun(TEntityDict self, float[] start,
                                            float[] aimdir, int damage, int kick, int hspread, int vspread,
                                            int count, int flashtype) {
        GameWeapon.fire_shotgun(self, start, aimdir, damage, kick, hspread, vspread,
                count, Defines.MOD_UNKNOWN);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the blaster. */
    public static void monster_fire_blaster(TEntityDict self, float[] start,
                                            float[] dir, int damage, int speed, int flashtype, int effect) {
        GameWeapon.fire_blaster(self, start, dir, damage, speed, effect, false);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the grenade. */
    public static void monster_fire_grenade(TEntityDict self, float[] start,
                                            float[] aimdir, int damage, int speed, int flashtype) {
        GameWeapon
                .fire_grenade(self, start, aimdir, damage, speed, 2.5f,
                        damage + 40);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the rocket. */
    public static void monster_fire_rocket(TEntityDict self, float[] start,
                                           float[] dir, int damage, int speed, int flashtype) {
        GameWeapon.fire_rocket(self, start, dir, damage, speed, damage + 20, damage);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the railgun. */
    public static void monster_fire_railgun(TEntityDict self, float[] start,
                                            float[] aimdir, int damage, int kick, int flashtype) {
        GameWeapon.fire_rail(self, start, aimdir, damage, kick);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /** The Moster fires the bfg. */
    public static void monster_fire_bfg(TEntityDict self, float[] start,
                                        float[] aimdir, int damage, int speed, int kick,
                                        float damage_radius, int flashtype) {
        GameWeapon.fire_bfg(self, start, aimdir, damage, speed, damage_radius);

        GameBase.gi.WriteByte(Defines.svc_muzzleflash2);
        GameBase.gi.WriteShort(self.index);
        GameBase.gi.WriteByte(flashtype);
        GameBase.gi.multicast(start, Defines.MULTICAST_PVS);
    }

    /*
     * ================ monster_death_use
     * 
     * When a monster dies, it fires all of its targets with the current enemy
     * as activator. ================
     */
    public static void monster_death_use(TEntityDict self) {
        self.flags &= ~(Defines.FL_FLY | Defines.FL_SWIM);
        self.monsterinfo.aiflags &= Defines.AI_GOOD_GUY;

        if (self.item != null) {
            GameItems.Drop_Item(self, self.item);
            self.item = null;
        }

        if (self.deathtarget != null)
            self.target = self.deathtarget;

        if (self.target == null)
            return;

        GameUtil.G_UseTargets(self, self.enemy);
    }

    // ============================================================================
    public static boolean monster_start(TEntityDict self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return false;
        }

        if ((self.spawnflags & 4) != 0
                && 0 == (self.monsterinfo.aiflags & Defines.AI_GOOD_GUY)) {
            self.spawnflags &= ~4;
            self.spawnflags |= 1;
            //		 gi.dprintf("fixed spawnflags on %entityState at %entityState\n", self.classname,
            // vtos(self.entityState.origin));
        }

        if (0 == (self.monsterinfo.aiflags & Defines.AI_GOOD_GUY))
            GameBase.level.total_monsters++;

        self.nextthink = GameBase.level.time + Defines.FRAMETIME;
        self.svflags |= Defines.SVF_MONSTER;
        self.entityState.renderfx |= Defines.RF_FRAMELERP;
        self.takedamage = Defines.DAMAGE_AIM;
        self.air_finished = GameBase.level.time + 12;
        self.use = GameUtil.monster_use;
        self.max_health = self.health;
        self.clipmask = Defines.MASK_MONSTERSOLID;

        self.entityState.skinnum = 0;
        self.deadflag = Defines.DEAD_NO;
        self.svflags &= ~Defines.SVF_DEADMONSTER;

        if (null == self.monsterinfo.checkattack)
            self.monsterinfo.checkattack = GameUtil.M_CheckAttack;
        Math3D.VectorCopy(self.entityState.origin, self.entityState.old_origin);

        if (GameBase.st.item != null && GameBase.st.item.length() > 0) {
            self.item = GameItems.FindItemByClassname(GameBase.st.item);
            if (self.item == null)
                GameBase.gi.dprintf("monster_start:" + self.classname + " at "
                        + Lib.vtos(self.entityState.origin) + " has bad item: "
                        + GameBase.st.item + "\n");
        }

        // randomize what frame they start on
        if (self.monsterinfo.currentmove != null)
            self.entityState.frame = self.monsterinfo.currentmove.firstframe
                    + (Lib.rand() % (self.monsterinfo.currentmove.lastframe
                            - self.monsterinfo.currentmove.firstframe + 1));

        return true;
    }

    public static void monster_start_go(TEntityDict self) {

        float[] v = { 0, 0, 0 };

        if (self.health <= 0)
            return;

        // check for target to combat_point and change to combattarget
        if (self.target != null) {
            boolean notcombat;
            boolean fixup;
            TEntityDict target = null;
            notcombat = false;
            fixup = false;
            /*
             * if (true) { Command.Printf("all entities:\n");
             * 
             * for (int n = 0; n < Game.globals.num_edicts; n++) { TEntityDict entityDict =
             * GameBase.entityDicts[n]; Command.Printf( "|%4i | %25s
             * |%8.2f|%8.2f|%8.2f||%8.2f|%8.2f|%8.2f||%8.2f|%8.2f|%8.2f|\n", new
             * Vargs().add(n).add(entityDict.classname).
             * add(entityDict.entityState.origin[0]).add(entityDict.entityState.origin[1]).add(entityDict.entityState.origin[2])
             * .add(entityDict.mins[0]).add(entityDict.mins[1]).add(entityDict.mins[2])
             * .add(entityDict.maxs[0]).add(entityDict.maxs[1]).add(entityDict.maxs[2])); }
             * sleep(10); }
             */

            EdictIterator edit = null;

            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    self.target)) != null) {
                target = edit.o;
                if (Lib.strcmp(target.classname, "point_combat") == 0) {
                    self.combattarget = self.target;
                    fixup = true;
                } else {
                    notcombat = true;
                }
            }
            if (notcombat && self.combattarget != null)
                GameBase.gi.dprintf(self.classname + " at "
                        + Lib.vtos(self.entityState.origin)
                        + " has target with mixed types\n");
            if (fixup)
                self.target = null;
        }

        // validate combattarget
        if (self.combattarget != null) {
            TEntityDict target = null;

            EdictIterator edit = null;
            while ((edit = GameBase.G_Find(edit, GameBase.findByTarget,
                    self.combattarget)) != null) {
                target = edit.o;

                if (Lib.strcmp(target.classname, "point_combat") != 0) {
                    GameBase.gi.dprintf(self.classname + " at "
                            + Lib.vtos(self.entityState.origin)
                            + " has bad combattarget " + self.combattarget
                            + " : " + target.classname + " at "
                            + Lib.vtos(target.entityState.origin));
                }
            }
        }

        if (self.target != null) {
            self.goalentity = self.movetarget = GameBase
                    .G_PickTarget(self.target);
            if (null == self.movetarget) {
                GameBase.gi
                        .dprintf(self.classname + " can't find target "
                                + self.target + " at "
                                + Lib.vtos(self.entityState.origin) + "\n");
                self.target = null;
                self.monsterinfo.pausetime = 100000000;
                self.monsterinfo.stand.think(self);
            } else if (Lib.strcmp(self.movetarget.classname, "path_corner") == 0) {
                Math3D.VectorSubtract(self.goalentity.entityState.origin, self.entityState.origin,
                        v);
                self.ideal_yaw = self.entityState.angles[Defines.YAW] = Math3D
                        .vectoyaw(v);
                self.monsterinfo.walk.think(self);
                self.target = null;
            } else {
                self.goalentity = self.movetarget = null;
                self.monsterinfo.pausetime = 100000000;
                self.monsterinfo.stand.think(self);
            }
        } else {
            self.monsterinfo.pausetime = 100000000;
            self.monsterinfo.stand.think(self);
        }

        self.think = jake2.game.Monster.monster_think;
        self.nextthink = GameBase.level.time + Defines.FRAMETIME;
    }

    public static EntThinkAdapter monster_think = new EntThinkAdapter() {
        public String getID() { return "monster_think";}
        public boolean think(TEntityDict self) {

            jake2.client.Monster.M_MoveFrame(self);
            if (self.linkCount != self.monsterinfo.linkcount) {
                self.monsterinfo.linkcount = self.linkCount;
                jake2.client.Monster.M_CheckGround(self);
            }
            jake2.client.Monster.M_CatagorizePosition(self);
            jake2.client.Monster.M_WorldEffects(self);
            jake2.client.Monster.M_SetEffects(self);
            return true;
        }
    };

    public static EntThinkAdapter monster_triggered_spawn = new EntThinkAdapter() {
        public String getID() { return "monster_trigger_spawn";}
        public boolean think(TEntityDict self) {

            self.entityState.origin[2] += 1;
            GameUtil.KillBox(self);

            self.solid = Defines.SOLID_BBOX;
            self.movetype = Defines.MOVETYPE_STEP;
            self.svflags &= ~Defines.SVF_NOCLIENT;
            self.air_finished = GameBase.level.time + 12;
            GameBase.gi.linkentity(self);

            jake2.game.Monster.monster_start_go(self);

            if (self.enemy != null && 0 == (self.spawnflags & 1)
                    && 0 == (self.enemy.flags & Defines.FL_NOTARGET)) {
                GameUtil.FoundTarget(self);
            } else {
                self.enemy = null;
            }
            return true;
        }
    };

    //	we have a one frame delay here so we don't telefrag the guy who activated
    // us
    public static EntUseAdapter monster_triggered_spawn_use = new EntUseAdapter() {
        public String getID() { return "monster_trigger_spawn_use";}
        public void use(TEntityDict self, TEntityDict other, TEntityDict activator) {
            self.think = monster_triggered_spawn;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            if (activator.client != null)
                self.enemy = activator;
            self.use = GameUtil.monster_use;
        }
    };

    public static EntThinkAdapter monster_triggered_start = new EntThinkAdapter() {
        public String getID() { return "monster_triggered_start";}
        public boolean think(TEntityDict self) {
            if (self.index == 312)
                Command.Printf("monster_triggered_start\n");
            self.solid = Defines.SOLID_NOT;
            self.movetype = Defines.MOVETYPE_NONE;
            self.svflags |= Defines.SVF_NOCLIENT;
            self.nextthink = 0;
            self.use = monster_triggered_spawn_use;
            return true;
        }
    };
}