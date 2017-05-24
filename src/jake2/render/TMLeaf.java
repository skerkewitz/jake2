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

// Created on 20.11.2003 by RST.
// $Id: TMLeaf.java,v 1.1 2004-07-07 19:59:35 hzi Exp $

package jake2.render;

public class TMLeaf extends TMNode {

	//	common with node
	/*
	public int contents; // wil be a negative contents number
	public int visframe; // node needs to be traversed if current

	public float minmaxs[] = new float[6]; // for bounding box culling

	public TMNode parent;
	*/

	//	leaf specific
	public int cluster;
	public int area;

	//public TMapSurface firstmarksurface;
	public int nummarksurfaces;
	
	// added by cwei
	int markIndex;
	TMapSurface[] markSurfaces;
	
	public void setMarkSurface(int markIndex, TMapSurface[] markSurfaces) {
		this.markIndex = markIndex;
		this.markSurfaces = markSurfaces;
	}

	public TMapSurface getMarkSurface(int index) {
		assert (index >= 0 && index <= nummarksurfaces) : "mleaf: markSurface bug (index = " + index +"; num = " + nummarksurfaces + ")";
		// TODO code in Surf.R_RecursiveWorldNode aendern (der Pointer wird wie in C zu weit gezaehlt)
		return (index < nummarksurfaces) ? markSurfaces[markIndex + index] : null;
	}

}
