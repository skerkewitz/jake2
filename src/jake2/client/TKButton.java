/*
 * kbutton_t.java
 * Copyright (C) 2004
 * 
 * $Id: kbutton_t.java,v 1.1 2004-07-07 19:58:52 hzi Exp $
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

/**
 *
 * KEY BUTTONS
 *
 * Continuous button event tracking is complicated by the fact that two
 * different input sources (say, mouse button 1 and the control key) can
 * both press the same button, but the button should only be released when
 * both of the pressing key have been released.
 *
 * When a key event issues a button command (+forward, +attack, etc), it
 * appends its key number as a parameter to the command so it can be matched
 * up with the release.
 *
 * state bit 0 is the current state of the key state bit 1 is edge triggered
 * on the up to down transition state bit 2 is edge triggered on the down to
 * up transition
 *
 *
 * Key_Event (int key, qboolean down, unsigned time);
 *
 * +mlook src time
 *
 * ===============================================================================
 */
public class TKButton {
	int[] down = new int[2];	// key nums holding it down
	long downtime;				// msec timestamp
	long msec;					// msec down this frame
	public int state;
}
