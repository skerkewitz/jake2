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

// Created on 13.11.2003 by RST.
// $Id: M_Hover.java,v 1.4 2005-11-20 22:18:33 salomo Exp $
package jake2.game.monsters;

import jake2.Defines;
import jake2.game.*;
import jake2.game.EntDieAdapter;
import jake2.game.EntInteractAdapter;
import jake2.game.EntPainAdapter;
import jake2.game.EntThinkAdapter;
import jake2.game.GameAI;
import jake2.game.GameBase;
import jake2.game.GameUtil;
import jake2.game.Monster;
import jake2.game.TEntityDict;
import jake2.game.mframe_t;
import jake2.game.mmove_t;
import jake2.util.Lib;
import jake2.util.Math3D;

public class M_Hover {

    //	This file generated by ModelGen - Do NOT Modify

    public final static int FRAME_stand01 = 0;

    public final static int FRAME_stand02 = 1;

    public final static int FRAME_stand03 = 2;

    public final static int FRAME_stand04 = 3;

    public final static int FRAME_stand05 = 4;

    public final static int FRAME_stand06 = 5;

    public final static int FRAME_stand07 = 6;

    public final static int FRAME_stand08 = 7;

    public final static int FRAME_stand09 = 8;

    public final static int FRAME_stand10 = 9;

    public final static int FRAME_stand11 = 10;

    public final static int FRAME_stand12 = 11;

    public final static int FRAME_stand13 = 12;

    public final static int FRAME_stand14 = 13;

    public final static int FRAME_stand15 = 14;

    public final static int FRAME_stand16 = 15;

    public final static int FRAME_stand17 = 16;

    public final static int FRAME_stand18 = 17;

    public final static int FRAME_stand19 = 18;

    public final static int FRAME_stand20 = 19;

    public final static int FRAME_stand21 = 20;

    public final static int FRAME_stand22 = 21;

    public final static int FRAME_stand23 = 22;

    public final static int FRAME_stand24 = 23;

    public final static int FRAME_stand25 = 24;

    public final static int FRAME_stand26 = 25;

    public final static int FRAME_stand27 = 26;

    public final static int FRAME_stand28 = 27;

    public final static int FRAME_stand29 = 28;

    public final static int FRAME_stand30 = 29;

    public final static int FRAME_forwrd01 = 30;

    public final static int FRAME_forwrd02 = 31;

    public final static int FRAME_forwrd03 = 32;

    public final static int FRAME_forwrd04 = 33;

    public final static int FRAME_forwrd05 = 34;

    public final static int FRAME_forwrd06 = 35;

    public final static int FRAME_forwrd07 = 36;

    public final static int FRAME_forwrd08 = 37;

    public final static int FRAME_forwrd09 = 38;

    public final static int FRAME_forwrd10 = 39;

    public final static int FRAME_forwrd11 = 40;

    public final static int FRAME_forwrd12 = 41;

    public final static int FRAME_forwrd13 = 42;

    public final static int FRAME_forwrd14 = 43;

    public final static int FRAME_forwrd15 = 44;

    public final static int FRAME_forwrd16 = 45;

    public final static int FRAME_forwrd17 = 46;

    public final static int FRAME_forwrd18 = 47;

    public final static int FRAME_forwrd19 = 48;

    public final static int FRAME_forwrd20 = 49;

    public final static int FRAME_forwrd21 = 50;

    public final static int FRAME_forwrd22 = 51;

    public final static int FRAME_forwrd23 = 52;

    public final static int FRAME_forwrd24 = 53;

    public final static int FRAME_forwrd25 = 54;

    public final static int FRAME_forwrd26 = 55;

    public final static int FRAME_forwrd27 = 56;

    public final static int FRAME_forwrd28 = 57;

    public final static int FRAME_forwrd29 = 58;

    public final static int FRAME_forwrd30 = 59;

    public final static int FRAME_forwrd31 = 60;

    public final static int FRAME_forwrd32 = 61;

    public final static int FRAME_forwrd33 = 62;

    public final static int FRAME_forwrd34 = 63;

    public final static int FRAME_forwrd35 = 64;

    public final static int FRAME_stop101 = 65;

    public final static int FRAME_stop102 = 66;

    public final static int FRAME_stop103 = 67;

    public final static int FRAME_stop104 = 68;

    public final static int FRAME_stop105 = 69;

    public final static int FRAME_stop106 = 70;

    public final static int FRAME_stop107 = 71;

    public final static int FRAME_stop108 = 72;

    public final static int FRAME_stop109 = 73;

    public final static int FRAME_stop201 = 74;

    public final static int FRAME_stop202 = 75;

    public final static int FRAME_stop203 = 76;

    public final static int FRAME_stop204 = 77;

    public final static int FRAME_stop205 = 78;

    public final static int FRAME_stop206 = 79;

    public final static int FRAME_stop207 = 80;

    public final static int FRAME_stop208 = 81;

    public final static int FRAME_takeof01 = 82;

    public final static int FRAME_takeof02 = 83;

    public final static int FRAME_takeof03 = 84;

    public final static int FRAME_takeof04 = 85;

    public final static int FRAME_takeof05 = 86;

    public final static int FRAME_takeof06 = 87;

    public final static int FRAME_takeof07 = 88;

    public final static int FRAME_takeof08 = 89;

    public final static int FRAME_takeof09 = 90;

    public final static int FRAME_takeof10 = 91;

    public final static int FRAME_takeof11 = 92;

    public final static int FRAME_takeof12 = 93;

    public final static int FRAME_takeof13 = 94;

    public final static int FRAME_takeof14 = 95;

    public final static int FRAME_takeof15 = 96;

    public final static int FRAME_takeof16 = 97;

    public final static int FRAME_takeof17 = 98;

    public final static int FRAME_takeof18 = 99;

    public final static int FRAME_takeof19 = 100;

    public final static int FRAME_takeof20 = 101;

    public final static int FRAME_takeof21 = 102;

    public final static int FRAME_takeof22 = 103;

    public final static int FRAME_takeof23 = 104;

    public final static int FRAME_takeof24 = 105;

    public final static int FRAME_takeof25 = 106;

    public final static int FRAME_takeof26 = 107;

    public final static int FRAME_takeof27 = 108;

    public final static int FRAME_takeof28 = 109;

    public final static int FRAME_takeof29 = 110;

    public final static int FRAME_takeof30 = 111;

    public final static int FRAME_land01 = 112;

    public final static int FRAME_pain101 = 113;

    public final static int FRAME_pain102 = 114;

    public final static int FRAME_pain103 = 115;

    public final static int FRAME_pain104 = 116;

    public final static int FRAME_pain105 = 117;

    public final static int FRAME_pain106 = 118;

    public final static int FRAME_pain107 = 119;

    public final static int FRAME_pain108 = 120;

    public final static int FRAME_pain109 = 121;

    public final static int FRAME_pain110 = 122;

    public final static int FRAME_pain111 = 123;

    public final static int FRAME_pain112 = 124;

    public final static int FRAME_pain113 = 125;

    public final static int FRAME_pain114 = 126;

    public final static int FRAME_pain115 = 127;

    public final static int FRAME_pain116 = 128;

    public final static int FRAME_pain117 = 129;

    public final static int FRAME_pain118 = 130;

    public final static int FRAME_pain119 = 131;

    public final static int FRAME_pain120 = 132;

    public final static int FRAME_pain121 = 133;

    public final static int FRAME_pain122 = 134;

    public final static int FRAME_pain123 = 135;

    public final static int FRAME_pain124 = 136;

    public final static int FRAME_pain125 = 137;

    public final static int FRAME_pain126 = 138;

    public final static int FRAME_pain127 = 139;

    public final static int FRAME_pain128 = 140;

    public final static int FRAME_pain201 = 141;

    public final static int FRAME_pain202 = 142;

    public final static int FRAME_pain203 = 143;

    public final static int FRAME_pain204 = 144;

    public final static int FRAME_pain205 = 145;

    public final static int FRAME_pain206 = 146;

    public final static int FRAME_pain207 = 147;

    public final static int FRAME_pain208 = 148;

    public final static int FRAME_pain209 = 149;

    public final static int FRAME_pain210 = 150;

    public final static int FRAME_pain211 = 151;

    public final static int FRAME_pain212 = 152;

    public final static int FRAME_pain301 = 153;

    public final static int FRAME_pain302 = 154;

    public final static int FRAME_pain303 = 155;

    public final static int FRAME_pain304 = 156;

    public final static int FRAME_pain305 = 157;

    public final static int FRAME_pain306 = 158;

    public final static int FRAME_pain307 = 159;

    public final static int FRAME_pain308 = 160;

    public final static int FRAME_pain309 = 161;

    public final static int FRAME_death101 = 162;

    public final static int FRAME_death102 = 163;

    public final static int FRAME_death103 = 164;

    public final static int FRAME_death104 = 165;

    public final static int FRAME_death105 = 166;

    public final static int FRAME_death106 = 167;

    public final static int FRAME_death107 = 168;

    public final static int FRAME_death108 = 169;

    public final static int FRAME_death109 = 170;

    public final static int FRAME_death110 = 171;

    public final static int FRAME_death111 = 172;

    public final static int FRAME_backwd01 = 173;

    public final static int FRAME_backwd02 = 174;

    public final static int FRAME_backwd03 = 175;

    public final static int FRAME_backwd04 = 176;

    public final static int FRAME_backwd05 = 177;

    public final static int FRAME_backwd06 = 178;

    public final static int FRAME_backwd07 = 179;

    public final static int FRAME_backwd08 = 180;

    public final static int FRAME_backwd09 = 181;

    public final static int FRAME_backwd10 = 182;

    public final static int FRAME_backwd11 = 183;

    public final static int FRAME_backwd12 = 184;

    public final static int FRAME_backwd13 = 185;

    public final static int FRAME_backwd14 = 186;

    public final static int FRAME_backwd15 = 187;

    public final static int FRAME_backwd16 = 188;

    public final static int FRAME_backwd17 = 189;

    public final static int FRAME_backwd18 = 190;

    public final static int FRAME_backwd19 = 191;

    public final static int FRAME_backwd20 = 192;

    public final static int FRAME_backwd21 = 193;

    public final static int FRAME_backwd22 = 194;

    public final static int FRAME_backwd23 = 195;

    public final static int FRAME_backwd24 = 196;

    public final static int FRAME_attak101 = 197;

    public final static int FRAME_attak102 = 198;

    public final static int FRAME_attak103 = 199;

    public final static int FRAME_attak104 = 200;

    public final static int FRAME_attak105 = 201;

    public final static int FRAME_attak106 = 202;

    public final static int FRAME_attak107 = 203;

    public final static int FRAME_attak108 = 204;

    public final static float MODEL_SCALE = 1.000000f;

    static int sound_pain1;

    static int sound_pain2;

    static int sound_death1;

    static int sound_death2;

    static int sound_sight;

    static int sound_search1;

    static int sound_search2;

    static EntThinkAdapter hover_reattack = new EntThinkAdapter() {
    	public String getID() { return "hover_reattack"; }
        public boolean think(TEntityDict self) {
            if (self.enemy.health > 0)
                if (GameUtil.visible(self, self.enemy))
                    if (Lib.random() <= 0.6) {
                        self.monsterinfo.currentmove = hover_move_attack1;
                        return true;
                    }
            self.monsterinfo.currentmove = hover_move_end_attack;
            return true;
        }
    };

    static EntThinkAdapter hover_fire_blaster = new EntThinkAdapter() {
    	public String getID() { return "hover_fire_blaster"; }
        public boolean think(TEntityDict self) {
            float[] start = { 0, 0, 0 };
            float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 };
            float[] end = { 0, 0, 0 };
            float[] dir = { 0, 0, 0 };
            int effect;

            if (self.s.frame == FRAME_attak104)
                effect = Defines.EF_HYPERBLASTER;
            else
                effect = 0;

            Math3D.AngleVectors(self.s.angles, forward, right, null);
            Math3D.G_ProjectSource(self.s.origin,
                    M_Flash.monster_flash_offset[Defines.MZ2_HOVER_BLASTER_1],
                    forward, right, start);

            Math3D.VectorCopy(self.enemy.s.origin, end);
            end[2] += self.enemy.viewheight;
            Math3D.VectorSubtract(end, start, dir);

            Monster.monster_fire_blaster(self, start, dir, 1, 1000,
                    Defines.MZ2_HOVER_BLASTER_1, effect);
            return true;
        }
    };

    static EntThinkAdapter hover_stand = new EntThinkAdapter() {
    	public String getID() { return "hover_stand"; }
        public boolean think(TEntityDict self) {
            self.monsterinfo.currentmove = hover_move_stand;
            return true;
        }
    };

    static EntThinkAdapter hover_run = new EntThinkAdapter() {
    	public String getID() { return "hover_run"; }
        public boolean think(TEntityDict self) {
            if ((self.monsterinfo.aiflags & Defines.AI_STAND_GROUND) != 0)
                self.monsterinfo.currentmove = hover_move_stand;
            else
                self.monsterinfo.currentmove = hover_move_run;
            return true;
        }
    };

    static EntThinkAdapter hover_walk = new EntThinkAdapter() {
    	public String getID() { return "hover_walk"; }
        public boolean think(TEntityDict self) {
            self.monsterinfo.currentmove = hover_move_walk;
            return true;
        }
    };

    static EntThinkAdapter hover_start_attack = new EntThinkAdapter() {
    	public String getID() { return "hover_start_attack"; }
        public boolean think(TEntityDict self) {
            self.monsterinfo.currentmove = hover_move_start_attack;
            return true;
        }
    };

    static EntThinkAdapter hover_attack = new EntThinkAdapter() {
    	public String getID() { return "hover_attack"; }
        public boolean think(TEntityDict self) {
            self.monsterinfo.currentmove = hover_move_attack1;
            return true;
        }
    };

    static EntPainAdapter hover_pain = new EntPainAdapter() {
    	public String getID() { return "hover_pain"; }
        public void pain(TEntityDict self, TEntityDict other, float kick, int damage) {
            if (self.health < (self.max_health / 2))
                self.s.skinnum = 1;

            if (GameBase.level.time < self.pain_debounce_time)
                return;

            self.pain_debounce_time = GameBase.level.time + 3;

            if (GameBase.skill.value == 3)
                return; // no pain anims in nightmare

            if (damage <= 25) {
                if (Lib.random() < 0.5) {
                    GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_pain1, 1,
                            Defines.ATTN_NORM, 0);
                    self.monsterinfo.currentmove = hover_move_pain3;
                } else {
                    GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_pain2, 1,
                            Defines.ATTN_NORM, 0);
                    self.monsterinfo.currentmove = hover_move_pain2;
                }
            } else {
                GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_pain1, 1,
                        Defines.ATTN_NORM, 0);
                self.monsterinfo.currentmove = hover_move_pain1;
            }
        }
    };

    static EntThinkAdapter hover_deadthink = new EntThinkAdapter() {
    	public String getID() { return "hover_deadthink"; }
        public boolean think(TEntityDict self) {
            if (null == self.groundentity
                    && GameBase.level.time < self.timestamp) {
                self.nextthink = GameBase.level.time + Defines.FRAMETIME;
                return true;
            }
            GameMisc.BecomeExplosion1(self);
            return true;
        }
    };

    static EntThinkAdapter hover_dead = new EntThinkAdapter() {
    	public String getID() { return "hover_dead"; }
        public boolean think(TEntityDict self) {
            Math3D.VectorSet(self.mins, -16, -16, -24);
            Math3D.VectorSet(self.maxs, 16, 16, -8);
            self.movetype = Defines.MOVETYPE_TOSS;
            self.think = hover_deadthink;
            self.nextthink = GameBase.level.time + Defines.FRAMETIME;
            self.timestamp = GameBase.level.time + 15;
            GameBase.gi.linkentity(self);
            return true;
        }
    };

    static EntDieAdapter hover_die = new EntDieAdapter() {
    	public String getID() { return "hover_die"; }
        public void die(TEntityDict self, TEntityDict inflictor, TEntityDict attacker,
                        int damage, float[] point) {
            int n;

            //	check for gib
            if (self.health <= self.gib_health) {
                GameBase.gi
                        .sound(self, Defines.CHAN_VOICE, GameBase.gi
                                .soundindex("misc/udeath.wav"), 1,
                                Defines.ATTN_NORM, 0);
                for (n = 0; n < 2; n++)
                    GameMisc.ThrowGib(self, "models/objects/gibs/bone/tris.md2",
                            damage, Defines.GIB_ORGANIC);
                for (n = 0; n < 2; n++)
                    GameMisc.ThrowGib(self,
                            "models/objects/gibs/sm_meat/tris.md2", damage,
                            Defines.GIB_ORGANIC);
                GameMisc.ThrowHead(self, "models/objects/gibs/sm_meat/tris.md2",
                        damage, Defines.GIB_ORGANIC);
                self.deadflag = Defines.DEAD_DEAD;
                return;
            }

            if (self.deadflag == Defines.DEAD_DEAD)
                return;

            //	regular death
            if (Lib.random() < 0.5)
                GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_death1, 1,
                        Defines.ATTN_NORM, 0);
            else
                GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_death2, 1,
                        Defines.ATTN_NORM, 0);
            self.deadflag = Defines.DEAD_DEAD;
            self.takedamage = Defines.DAMAGE_YES;
            self.monsterinfo.currentmove = hover_move_death1;
        }
    };

    static EntInteractAdapter hover_sight = new EntInteractAdapter() {
    	public String getID() { return "hover_sight"; }
        public boolean interact(TEntityDict self, TEntityDict other) {
            GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_sight, 1,
                    Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static EntThinkAdapter hover_search = new EntThinkAdapter() {
    	public String getID() { return "hover_search"; }
        public boolean think(TEntityDict self) {
            if (Lib.random() < 0.5)
                GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_search1, 1,
                        Defines.ATTN_NORM, 0);
            else
                GameBase.gi.sound(self, Defines.CHAN_VOICE, sound_search2, 1,
                        Defines.ATTN_NORM, 0);
            return true;
        }
    };

    static mframe_t hover_frames_stand[] = new mframe_t[] {
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null),
            new mframe_t(GameAI.ai_stand, 0, null) };

    static mmove_t hover_move_stand = new mmove_t(FRAME_stand01, FRAME_stand30,
            hover_frames_stand, null);

    static mframe_t hover_frames_stop1[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_stop1 = new mmove_t(FRAME_stop101, FRAME_stop109,
            hover_frames_stop1, null);

    static mframe_t hover_frames_stop2[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_stop2 = new mmove_t(FRAME_stop201, FRAME_stop208,
            hover_frames_stop2, null);

    static mframe_t hover_frames_takeoff[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -2, null),
            new mframe_t(GameAI.ai_move, 5, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, -1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, -6, null),
            new mframe_t(GameAI.ai_move, -9, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_takeoff = new mmove_t(FRAME_takeof01,
            FRAME_takeof30, hover_frames_takeoff, null);

    static mframe_t hover_frames_pain3[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_pain3 = new mmove_t(FRAME_pain301, FRAME_pain309,
            hover_frames_pain3, hover_run);

    static mframe_t hover_frames_pain2[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_pain2 = new mmove_t(FRAME_pain201, FRAME_pain212,
            hover_frames_pain2, hover_run);

    static mframe_t hover_frames_pain1[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, -8, null),
            new mframe_t(GameAI.ai_move, -4, null),
            new mframe_t(GameAI.ai_move, -6, null),
            new mframe_t(GameAI.ai_move, -4, null),
            new mframe_t(GameAI.ai_move, -3, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 7, null),
            new mframe_t(GameAI.ai_move, 1, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 2, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 5, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 4, null) };

    static mmove_t hover_move_pain1 = new mmove_t(FRAME_pain101, FRAME_pain128,
            hover_frames_pain1, hover_run);

    static mframe_t hover_frames_land[] = new mframe_t[] { new mframe_t(
            GameAI.ai_move, 0, null) };

    static mmove_t hover_move_land = new mmove_t(FRAME_land01, FRAME_land01,
            hover_frames_land, null);

    static mframe_t hover_frames_forward[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_forward = new mmove_t(FRAME_forwrd01,
            FRAME_forwrd35, hover_frames_forward, null);

    static mframe_t hover_frames_walk[] = new mframe_t[] {
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null),
            new mframe_t(GameAI.ai_walk, 4, null) };

    static mmove_t hover_move_walk = new mmove_t(FRAME_forwrd01,
            FRAME_forwrd35, hover_frames_walk, null);

    static mframe_t hover_frames_run[] = new mframe_t[] {
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null),
            new mframe_t(GameAI.ai_run, 10, null) };

    static mmove_t hover_move_run = new mmove_t(FRAME_forwrd01, FRAME_forwrd35,
            hover_frames_run, null);

    static mframe_t hover_frames_death1[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, -10, null),
            new mframe_t(GameAI.ai_move, 3, null),
            new mframe_t(GameAI.ai_move, 5, null),
            new mframe_t(GameAI.ai_move, 4, null),
            new mframe_t(GameAI.ai_move, 7, null) };

    static mmove_t hover_move_death1 = new mmove_t(FRAME_death101,
            FRAME_death111, hover_frames_death1, hover_dead);

    static mframe_t hover_frames_backward[] = new mframe_t[] {
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null),
            new mframe_t(GameAI.ai_move, 0, null) };

    static mmove_t hover_move_backward = new mmove_t(FRAME_backwd01,
            FRAME_backwd24, hover_frames_backward, null);

    static mframe_t hover_frames_start_attack[] = new mframe_t[] {
            new mframe_t(GameAI.ai_charge, 1, null),
            new mframe_t(GameAI.ai_charge, 1, null),
            new mframe_t(GameAI.ai_charge, 1, null) };

    static mmove_t hover_move_start_attack = new mmove_t(FRAME_attak101,
            FRAME_attak103, hover_frames_start_attack, hover_attack);

    static mframe_t hover_frames_attack1[] = new mframe_t[] {
            new mframe_t(GameAI.ai_charge, -10, hover_fire_blaster),
            new mframe_t(GameAI.ai_charge, -10, hover_fire_blaster),
            new mframe_t(GameAI.ai_charge, 0, hover_reattack), };

    static mmove_t hover_move_attack1 = new mmove_t(FRAME_attak104,
            FRAME_attak106, hover_frames_attack1, null);

    static mframe_t hover_frames_end_attack[] = new mframe_t[] {
            new mframe_t(GameAI.ai_charge, 1, null),
            new mframe_t(GameAI.ai_charge, 1, null) };

    static mmove_t hover_move_end_attack = new mmove_t(FRAME_attak107,
            FRAME_attak108, hover_frames_end_attack, hover_run);

    /*
     * QUAKED monster_hover (1 .5 0) (-16 -16 -24) (16 16 32) Ambush
     * Trigger_Spawn Sight
     */
    public static void SP_monster_hover(TEntityDict self) {
        if (GameBase.deathmatch.value != 0) {
            GameUtil.G_FreeEdict(self);
            return;
        }

        sound_pain1 = GameBase.gi.soundindex("hover/hovpain1.wav");
        sound_pain2 = GameBase.gi.soundindex("hover/hovpain2.wav");
        sound_death1 = GameBase.gi.soundindex("hover/hovdeth1.wav");
        sound_death2 = GameBase.gi.soundindex("hover/hovdeth2.wav");
        sound_sight = GameBase.gi.soundindex("hover/hovsght1.wav");
        sound_search1 = GameBase.gi.soundindex("hover/hovsrch1.wav");
        sound_search2 = GameBase.gi.soundindex("hover/hovsrch2.wav");

        GameBase.gi.soundindex("hover/hovatck1.wav");

        self.s.sound = GameBase.gi.soundindex("hover/hovidle1.wav");

        self.movetype = Defines.MOVETYPE_STEP;
        self.solid = Defines.SOLID_BBOX;
        self.s.modelindex = GameBase.gi
                .modelindex("models/monsters/hover/tris.md2");
        Math3D.VectorSet(self.mins, -24, -24, -24);
        Math3D.VectorSet(self.maxs, 24, 24, 32);

        self.health = 240;
        self.gib_health = -100;
        self.mass = 150;

        self.pain = hover_pain;
        self.die = hover_die;

        self.monsterinfo.stand = hover_stand;
        self.monsterinfo.walk = hover_walk;
        self.monsterinfo.run = hover_run;
        //	 self.monsterinfo.dodge = hover_dodge;
        self.monsterinfo.attack = hover_start_attack;
        self.monsterinfo.sight = hover_sight;
        self.monsterinfo.search = hover_search;

        GameBase.gi.linkentity(self);

        self.monsterinfo.currentmove = hover_move_stand;
        self.monsterinfo.scale = MODEL_SCALE;

        GameAI.flymonster_start.think(self);
    }
}