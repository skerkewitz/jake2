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

// Created on 27.11.2003 by RST.
// $Id: TNetAddr.java,v 1.6 2005-10-26 12:37:58 cawe Exp $
package jake2.network;

import jake2.Defines;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TNetAddr {

    public int type;

    public int port;

    public byte ip[];

    public TNetAddr() {
        this.type = Defines.NA_LOOPBACK;
        this.port = 0; // any
        try {
        	// localhost / 127.0.0.1
            this.ip = InetAddress.getByName(null).getAddress();
        } catch (UnknownHostException e) {
        }
    }

    /**
     * Compares ip address and port.
     */
    public boolean compareAdr(TNetAddr b) {
        return (this.ip[0] == b.ip[0] && this.ip[1] == b.ip[1] && this.ip[2] == b.ip[2]
                && this.ip[3] == b.ip[3] && this.port == b.port);
    }

    /**
     * Returns a string holding ip address and port like "ip0.ip1.ip2.ip3:port".
     */
    public String adrToString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.ip[0] & 0xFF).append('.').append(this.ip[1] & 0xFF);
        sb.append('.');
        sb.append(this.ip[2] & 0xFF).append('.').append(this.ip[3] & 0xFF);
        sb.append(':').append(this.port);
        return sb.toString();
    }

    /**
     * Compares ip address without the port.
     */
    public boolean compareBaseAdr(TNetAddr b) {
        if (this.type != b.type)
            return false;

        if (this.type == Defines.NA_LOOPBACK)
            return true;

        if (this.type == Defines.NA_IP) {
            return (this.ip[0] == b.ip[0] && this.ip[1] == b.ip[1]
                    && this.ip[2] == b.ip[2] && this.ip[3] == b.ip[3]);
        }
        return false;
    }

    public InetAddress getInetAddress() throws UnknownHostException {
        switch (type) {
        case Defines.NA_BROADCAST:
            return InetAddress.getByName("255.255.255.255");
        case Defines.NA_LOOPBACK:
        	// localhost / 127.0.0.1
            return InetAddress.getByName(null);
        case Defines.NA_IP:
            return InetAddress.getByAddress(ip);
        default:
            return null;
        }
    }

    public void set(TNetAddr from) {
        type = from.type;
        port = from.port;
        ip[0] = from.ip[0];
        ip[1] = from.ip[1];
        ip[2] = from.ip[2];
        ip[3] = from.ip[3];
    }

    public String toString() {
        return (type == Defines.NA_LOOPBACK) ? "loopback" : this
                .adrToString();
    }
}