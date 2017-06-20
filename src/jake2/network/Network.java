/*
 * NET.java Copyright (C) 2003
 */
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
package jake2.network;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.TVar;
import jake2.qcommon.Command;
import jake2.qcommon.ConsoleVar;
import jake2.network.TNetAddr;
import jake2.qcommon.TBuffer;
import jake2.util.Lib;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public final class Network {

    private final static int MAX_LOOPBACK = 4;

    /** Local loopback adress. */
    private static TNetAddr net_local_adr = new TNetAddr();

    private static class TLoopMsg {
        byte data[] = new byte[Defines.MAX_MSGLEN];
        int datalen;
    }

    private static class TLoopback {
        TLoopMsg msgs[];
        int get, send;

        public TLoopback() {
            msgs = new TLoopMsg[MAX_LOOPBACK];
            for (int n = 0; n < MAX_LOOPBACK; n++) {
                msgs[n] = new TLoopMsg();
            }
        }
    }

    private static TLoopback loopbacks[] = new TLoopback[2];

    static {
        loopbacks[0] = new TLoopback();
        loopbacks[1] = new TLoopback();
    }

    private static DatagramChannel[] ip_channels = { null, null };

    private static DatagramSocket[] ip_sockets = { null, null };

    /**
     * Returns IP address without the port as string.
     */
    public static String BaseAdrToString(TNetAddr a) {
        StringBuffer sb = new StringBuffer();
        sb.append(a.ip[0] & 0xFF).append('.').append(a.ip[1] & 0xFF);
        sb.append('.');
        sb.append(a.ip[2] & 0xFF).append('.').append(a.ip[3] & 0xFF);
        return sb.toString();
    }

    /**
     * Creates an TNetAddr from an string.
     */
    public static boolean StringToAdr(String s, TNetAddr a) {
        if (s.equalsIgnoreCase("localhost") || s.equalsIgnoreCase("loopback")) {
            a.set(net_local_adr);
            return true;
        }
        try {
            String[] address = s.split(":");
            InetAddress ia = InetAddress.getByName(address[0]);
            a.ip = ia.getAddress();
            a.type = Defines.NA_IP;
            if (address.length == 2)
                a.port = Lib.atoi(address[1]);
            return true;
        } catch (Exception e) {
            Command.Println(e.getMessage());
            return false;
        }
    }

    /**
     * Seems to return true, if the address is is on 127.0.0.1.
     */
    public static boolean IsLocalAddress(TNetAddr adr) {
        return adr.compareAdr(net_local_adr);
    }

    /*
     * ==================================================
     * 
     * LOOPBACK BUFFERS FOR LOCAL PLAYER
     * 
     * ==================================================
     */

    /**
     * Gets a packet from internal loopback.
     */
    private static boolean GetLoopPacket(int sock, TNetAddr net_from, TBuffer net_message) {
	
        TLoopback loop = loopbacks[sock];

        if (loop.send - loop.get > MAX_LOOPBACK)
            loop.get = loop.send - MAX_LOOPBACK;

        if (loop.get >= loop.send)
            return false;

        int i = loop.get & (MAX_LOOPBACK - 1);
        loop.get++;

        System.arraycopy(loop.msgs[i].data, 0, net_message.data, 0, loop.msgs[i].datalen);
        net_message.writeHeadPosition = loop.msgs[i].datalen;

        net_from.set(net_local_adr);
        return true;
    }

    /**
     * Sends a packet via internal loopback.
     */
    private static void SendLoopPacket(int sock, int length, byte[] data, TNetAddr to) {

        TLoopback loop = loopbacks[sock ^ 1];

        // modulo 4
        int i = loop.send & (MAX_LOOPBACK - 1);
        loop.send++;

        System.arraycopy(data, 0, loop.msgs[i].data, 0, length);
        loop.msgs[i].datalen = length;
    }

    /**
     * Gets a packet from a network channel
     */
    public static boolean GetPacket(int sock, TNetAddr net_from, TBuffer net_message) {

        if (GetLoopPacket(sock, net_from, net_message)) {
            return true;
        }

        if (ip_sockets[sock] == null) {
            return false;
        }

        try {
            ByteBuffer receiveBuffer = ByteBuffer.wrap(net_message.data);

            InetSocketAddress srcSocket = (InetSocketAddress) ip_channels[sock]
                    .receive(receiveBuffer);
            if (srcSocket == null)
                return false;

            net_from.ip = srcSocket.getAddress().getAddress();
            net_from.port = srcSocket.getPort();
            net_from.type = Defines.NA_IP;

            int packetLength = receiveBuffer.position();

            if (packetLength > net_message.maxsize) {
                Command.Println("Oversize packet from " + net_from.adrToString());
                return false;
            }

            // assign the size
            net_message.writeHeadPosition = packetLength;
            // assign the sentinel
            net_message.data[packetLength] = 0;
            return true;

        } catch (IOException e) {
            Command.DPrintf("NET_GetPacket: " + e + " from " + net_from.adrToString() + "\n");
            return false;
        }
    }

    /**
     * Sends a Packet.
     */
    public static void SendPacket(int sock, int length, byte[] data, TNetAddr to) {
        if (to.type == Defines.NA_LOOPBACK) {
            SendLoopPacket(sock, length, data, to);
            return;
        }

        if (ip_sockets[sock] == null)
            return;

        if (to.type != Defines.NA_BROADCAST && to.type != Defines.NA_IP) {
            Command.Error(Defines.ERR_FATAL, "NET_SendPacket: bad address type");
            return;
        }

        try {
            SocketAddress dstSocket = new InetSocketAddress(to.getInetAddress(), to.port);
            ip_channels[sock].send(ByteBuffer.wrap(data, 0, length), dstSocket);
        } catch (Exception e) {
            Command.Println("NET_SendPacket ERROR: " + e + " to " + to.adrToString());
        }
    }

    /**
     * OpenIP, creates the network sockets. 
     */
    private static void OpenIP() {
        TVar port = ConsoleVar.get("port", "" + Defines.PORT_SERVER, TVar.CVAR_FLAG_NOSET);
        TVar ip = ConsoleVar.get("ip", "localhost", TVar.CVAR_FLAG_NOSET);
        TVar clientport = ConsoleVar.get("clientport", "" + Defines.PORT_CLIENT, TVar.CVAR_FLAG_NOSET);
        
        if (ip_sockets[Defines.NS_SERVER] == null) {
            ip_sockets[Defines.NS_SERVER] = Socket(Defines.NS_SERVER, ip.string, (int) port.value);
        }
        
        if (ip_sockets[Defines.NS_CLIENT] == null) {
            ip_sockets[Defines.NS_CLIENT] = Socket(Defines.NS_CLIENT, ip.string, (int) clientport.value);
        }

        if (ip_sockets[Defines.NS_CLIENT] == null) {
            ip_sockets[Defines.NS_CLIENT] = Socket(Defines.NS_CLIENT, ip.string, Defines.PORT_ANY);
        }
    }

    /**
     * Config multi or singlepalyer - A single player game will only use the loopback code.
     */
    public static void Config(boolean multiplayer) {
        if (!multiplayer) {
            // shut down any existing sockets
            for (int i = 0; i < 2; i++) {
                if (ip_sockets[i] != null) {
                    ip_sockets[i].close();
                    ip_sockets[i] = null;
                }
            }
        } else {
            // open sockets
            OpenIP();
        }
    }

    /**
     * init
     */
    public static void Init() {
        // nothing to do
    }

    /*
     * Socket
     */
    private static DatagramSocket Socket(int sock, String ip, int port) {

        DatagramSocket newsocket = null;
        try {
            if (ip_channels[sock] == null || !ip_channels[sock].isOpen())
                ip_channels[sock] = DatagramChannel.open();

            if (ip == null || ip.length() == 0 || ip.equals("localhost")) {
                if (port == Defines.PORT_ANY) {
                    newsocket = ip_channels[sock].socket();
                    newsocket.bind(new InetSocketAddress(0));
                } else {
                    newsocket = ip_channels[sock].socket();
                    newsocket.bind(new InetSocketAddress(port));
                }
            } else {
                InetAddress ia = InetAddress.getByName(ip);
                newsocket = ip_channels[sock].socket();
                newsocket.bind(new InetSocketAddress(ia, port));
            }

            // nonblocking channel
            ip_channels[sock].configureBlocking(false);
            // the socket have to be broadcastable
            newsocket.setBroadcast(true);
        } catch (Exception e) {
            Command.Println("Error: " + e.toString());
            newsocket = null;
        }
        return newsocket;
    }

    /**
     * shutdown - closes the sockets
     */
    public static void Shutdown() {
        // close sockets
        Config(false);
    }

    /** Sleeps msec or until net socket is ready. */
    public static void Sleep(int msec) {
        if (ip_sockets[Defines.NS_SERVER] == null
                || (Context.dedicated != null && Context.dedicated.value == 0))
            return; // we're not a server, just run full speed

        try {
            //TODO: check for timeout
            Thread.sleep(msec);
        } catch (InterruptedException e) {
        }
        //ip_sockets[NS_SERVER].

        // this should wait up to 100ms until a packet
        /*
         * struct timeval timeout; 
         * fd_set fdset; 
         * extern TVar *dedicated;
         * extern qboolean stdin_active;
         * 
         * if (!ip_sockets[NS_SERVER] || (dedicated && !dedicated.value))
         * 		return; // we're not a server, just run full speed
         * 
         * FD_ZERO(&fdset);
         *  
         * if (stdin_active) 
         * 		FD_SET(0, &fdset); // stdin is processed too 
         * 
         * FD_SET(ip_sockets[NS_SERVER], &fdset); // network socket 
         * 
         * timeout.tv_sec = msec/1000; 
         * timeout.tv_usec = (msec%1000)*1000; 
         * 
         * select(ip_sockets[NS_SERVER]+1, &fdset, NULL, NULL, &timeout);
         */
    }
}