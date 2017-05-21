/*
 * cvar_t.java
 * Copyright (C) 2003
 * 
 * $Id: cvar_t.java,v 1.2 2004-07-08 15:58:44 hzi Exp $
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
package jake2.game;

/**
 * TVar implements the struct TVar of the C version
 */
public final class TVar {

    final public static int CVAR_FLAG_ARCHIVE = 1; // set to cause it to be saved to vars.rc
    final public static int CVAR_FLAG_USERINFO = 2; // added to userinfo when changed
    final public static int CVAR_FLAG_SERVERINFO = 4; // added to serverinfo when changed
    final public static int CVAR_FLAG_NOSET = 8; // don't allow change from console at all, but can be set from the command line
    final public static int CVAR_FLAG_LATCH = 16; // save changes until server restart

    public String name;
    public String string;
    public String latched_string;
    public int flags = 0;
    public boolean modified = false;
    public float value = 0.0f;
}
