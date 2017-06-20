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

// Created on 31.10.2003 by RST.
// $Id: TTrace.java,v 1.5 2005-01-14 16:09:42 cawe Exp $

package jake2.game;

import jake2.util.Math3D;

/**
 * A trace is returned when a box is swept through the world.
 */
public class TTrace {

    /**  True if plane is not valid. */
    public boolean allSolid;

    /** True if initial point was in a solid area. */
    public boolean startSolid;

    /** time completed, 1.0 = didn't hit anything. */
    public float fraction;

    /** Final position. */
    public float[] endpos = {0, 0, 0};
    // memory
    public cplane_t plane = new cplane_t(); // surface normal at impact
    // pointer
    public csurface_t surface; // surface hit
    public int contents; // contents on other side of surface hit
    // pointer
    public TEntityDict entityDict; // not assign by CM_*() functions

    public void set(TTrace from) {
        allSolid = from.allSolid;
        startSolid = from.allSolid;
        fraction = from.fraction;
        Math3D.VectorCopy(from.endpos, endpos);
        plane.set(from.plane);
        surface = from.surface;
        contents = from.contents;
        entityDict = from.entityDict;
    }

    public void clear() {
        allSolid = false;
        startSolid = false;
        fraction = 0;
        Math3D.VectorClear(endpos);
        plane.clear();
        surface = null;
        contents = 0;
        entityDict = null;
    }
}
