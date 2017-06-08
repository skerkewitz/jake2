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

// Created on 09.12.2003 by RST.

package jake2.server;

import jake2.game.TLink;

public class TAreaNode {
    int axis; // -1 = leaf node
    float dist;
    TAreaNode children[] = new TAreaNode[2];
    TLink trigger_edicts = new TLink(this);
    TLink solid_edicts = new TLink(this);

    // used for debugging
//	float mins_rst[] = {0,0,0};
//	float maxs_rst[] = {0,0,0};
}