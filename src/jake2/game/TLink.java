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

// Created on 31.10.2003 by RST 
// $Id: TLink.java,v 1.1 2004-07-07 19:59:25 hzi Exp $
// simple linked structure often used in quake.

package jake2.game;

public class TLink<T> {

	public TLink<T> prev, next;
	public final T o;

	public TLink(T o) {
		this.o = o;
	}

	// ClearLink is used for new headnodes
    public static void ClearLink(TLink l) {
        l.prev = l.next = l;
    }

	public static void RemoveLink(TLink l) {
        l.next.prev = l.prev;
        l.prev.next = l.next;
    }

	public static void InsertLinkBefore(TLink l, TLink before) {
        l.next = before;
        l.prev = before.prev;
        l.prev.next = l;
        l.next.prev = l;
    }
}
