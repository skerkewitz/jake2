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

// Created on 09.01.2004 by RST.

package jake2.game;

import jake2.qcommon.Command;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public abstract class SuperAdapter {

	/** Adapter repository. */
	private static Map<String, SuperAdapter> adapters= new HashMap<>();

	/** Constructor, does the adapter registration. */
	public SuperAdapter() {
		register(this, getID());
	}

	/** Adapter registration. */
	private static void register(SuperAdapter sa, String id) {
		adapters.put(id, sa);
	}

	/** Returns the adapter from the repository given by its ID. */
	public static SuperAdapter getFromID(String key) {
		final SuperAdapter sa = adapters.get(key);

		// try to create the adapter
		if (sa == null) {
			Command.DPrintf("SuperAdapter.getFromID():adapter not found->" + key + "\n");
		}

		return sa;
	}

	/** Returns the Adapter-ID. */
	public abstract String getID();
}
