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

// Created on 27.11.2003 by RST.
// $Id: TClientStatic.java,v 1.1 2004-07-07 19:58:52 hzi Exp $


package jake2.client

import jake2.network.TNetChan
import java.io.RandomAccessFile

class TClientStatic {

    /** was enum connstate_t  */
    var state: Int = 0

    /** was enum keydest_t  */
    var key_dest: Int = 0

    var framecount: Int = 0

    /** always increasing, no clamping, etc  */
    var realtime: Int = 0

    /** seconds since last frame  */
    var frametime: Float = 0.toFloat()

    //	   screen rendering information
    var disableScreen: Float = 0.toFloat() // showing loading plaque between levels
    // or changing rendering dlls
    // if time gets > 30 seconds ahead, break it
    var disableServerCount: Int = 0 // when we receive a frame and cl.servercount
    // > cls.disableServerCount, clear disableScreen

    //	   connection information
    var servername = "" // name of server from original connect
    var connectTime: Float = 0.toFloat() // for connection retransmits

    var quakePort: Int = 0 // a 16 bit value that allows quake servers
    // to work around address translating routers
    var netchan = TNetChan()
    var serverProtocol: Int = 0 // in case we are doing some kind of version hack

    var challenge: Int = 0 // from the server to use for connecting

    var download: RandomAccessFile? = null // file transfer from server
    var downloadtempname = ""
    var downloadname = ""
    var downloadnumber: Int = 0

    // was enum dltype_t
    var downloadtype: Int = 0
    var downloadpercent: Int = 0

    //	   demo recording info must be here, so it isn't cleared on level change
    var demorecording: Boolean = false
    var demowaiting: Boolean = false // don't record until a non-delta message is received
    var demofile: RandomAccessFile? = null
}
