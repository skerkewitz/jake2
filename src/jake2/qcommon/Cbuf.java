/*
 * Cbuf.java
 * Copyright (C) 2003
 * 
 * $Id: Cbuf.java,v 1.8 2005-12-18 22:10:09 cawe Exp $
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
package jake2.qcommon;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.Cmd;
import jake2.util.Lib;

/**
 * Cbuf
 */
public final class Cbuf {

    private static final byte[] line = new byte[1024];
    private static final byte[] tmp = new byte[8192];

    private static TSizeBuffer cmd_text = new TSizeBuffer();
    private static byte[] cmd_text_buf = new byte[8192];

    /**
     *  
     */
    public static void Init() {
        cmd_text.init(cmd_text_buf, cmd_text_buf.length);
    }

    public static void InsertText(String text) {

        // copy off any commands still remaining in the exec buffer
        int templen = cmd_text.cursize;
        if (templen != 0) {
            System.arraycopy(cmd_text.data, 0, tmp, 0, templen);
            cmd_text.clear();
        }

        // add the entire text of the file
        Cbuf.AddText(text);

        // add the copied off data
        if (templen != 0) {
            cmd_text.write(tmp, templen);
        }
    }

    /**
     * @param clear
     */
    static void AddEarlyCommands(boolean clear) {

        CommandLineOptions commandLineOptions = Qcommon.Companion.getCommandLineOptions();
        for (int i = 0; i < commandLineOptions.count(); i++) {
            String s = commandLineOptions.valueAt(i);
            if (!s.equals("+set"))
                continue;
            Cbuf.AddText("set " + commandLineOptions.valueAt(i + 1) + " " + commandLineOptions.valueAt(i + 2) + "\n");
            if (clear) {
                commandLineOptions.clearValueAt(i);
                commandLineOptions.clearValueAt(i + 1);
                commandLineOptions.clearValueAt(i + 2);
            }
            i += 2;
        }
    }

    /**
     * @return
     */
    static boolean AddLateCommands() {
        int i;
        int j;
        boolean ret = false;

        // build the combined string to parse from
        int s = 0;
        int argc = Qcommon.Companion.getCommandLineOptions().count();
        for (i = 1; i < argc; i++) {
            s += Qcommon.Companion.getCommandLineOptions().valueAt(i).length();
        }
        if (s == 0)
            return false;

        String text = "";
        for (i = 1; i < argc; i++) {
            text += Qcommon.Companion.getCommandLineOptions().valueAt(i);
            if (i != argc - 1)
                text += " ";
        }

        // pull out the commands
        String build = "";
        for (i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '+') {
                i++;

                for (j = i; j < text.length() && (text.charAt(j) != '+') && (text.charAt(j) != '-'); j++);

                build += text.substring(i, j);
                build += "\n";

                i = j - 1;
            }
        }

        ret = (build.length() != 0);
        if (ret)
            Cbuf.AddText(build);

        text = null;
        build = null;

        return ret;
    }

    /**
     * @param text
     */
    public static void AddText(String text) {
        int l = text.length();

        if (cmd_text.cursize + l >= cmd_text.maxsize) {
            Command.Printf("Cbuf_AddText: overflow\n");
            return;
        }
        cmd_text.write(Lib.stringToBytes(text), l);
    }

    /**
     *  
     */
    public static void Execute() {

        byte[] text = null;

        Context.alias_count = 0; // don't allow infinite alias loops

        while (cmd_text.cursize != 0) {
            // find a \n or ; line break
            text = cmd_text.data;

            int quotes = 0;
            int i;

            for (i = 0; i < cmd_text.cursize; i++) {
                if (text[i] == '"')
                    quotes++;
                if (!(quotes % 2 != 0) && text[i] == ';')
                    break; // don't break if inside a quoted string
                if (text[i] == '\n')
                    break;
            }

            System.arraycopy(text, 0, line, 0, i);
            line[i] = 0;

            // delete the text from the command buffer and move remaining
            // commands down
            // this is necessary because commands (exec, alias) can insertBefore data
            // at the
            // beginning of the text buffer

            if (i == cmd_text.cursize)
                cmd_text.cursize = 0;
            else {
                i++;
                cmd_text.cursize -= i;
                //byte[] tmp = new byte[Context.cmd_text.cursize];

                System.arraycopy(text, i, tmp, 0, cmd_text.cursize);
                System.arraycopy(tmp, 0, text, 0, cmd_text.cursize);
                text[cmd_text.cursize] = '\0';

            }

            // execute the command line
            int len = Lib.strlen(line);

            String cmd = new String(line, 0, len);
            Cmd.ExecuteString(cmd);

            if (Context.cmd_wait) {
                // skip out while text still remains in buffer, leaving it
                // for next frame
                Context.cmd_wait = false;
                break;
            }
        }
    }

    public static void ExecuteText(int exec_when, String text) {
        switch (exec_when) {
        case Defines.EXEC_NOW:
            Cmd.ExecuteString(text);
            break;
        case Defines.EXEC_INSERT:
            Cbuf.InsertText(text);
            break;
        case Defines.EXEC_APPEND:
            Cbuf.AddText(text);
            break;
        default:
            Command.Error(Defines.ERR_FATAL, "Cbuf_ExecuteText: bad exec_when");
        }
    }

    /*
     * ============ Cbuf_CopyToDefer ============
     */
    public static void CopyToDefer() {
        System.arraycopy(cmd_text_buf, 0, Context.defer_text_buf, 0,
                cmd_text.cursize);
        Context.defer_text_buf[cmd_text.cursize] = 0;
        cmd_text.cursize = 0;
    }

    /*
     * ============ Cbuf_InsertFromDefer ============
     */
    public static void InsertFromDefer() {
        InsertText(new String(Context.defer_text_buf).trim());
        Context.defer_text_buf[0] = 0;
    }

}