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

// Created on 13.01.2004 by RST.

package jake2.server

import jake2.Defines
import jake2.game.TPlayerState

class TClientFrame {

    var areaBytes: Int = 0

    /** portalarea visibility bits  */
    var areaBits = ByteArray(Defines.MAX_MAP_AREAS / 8)
    var playerState = TPlayerState()
    var numEntities: Int = 0

    /** Into the circular sv_packet_entities[].  */
    var firstEntity: Int = 0

    /** For ping calculations.  */
    var sentTime: Int = 0
}
