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
 * CommandBuffer
 */
public final class CommandBuffer {

    private  static byte[] defer_text_buf = new byte[8192];
    private static TBuffer cmd_text = TBuffer.createWithSize(8192);

    /**
     *  
     */
    public static void Init() {

    }

    public static void InsertText(String text) {
        final byte[] tmp = new byte[8192];


        // copy off any commands still remaining in the exec buffer
        int templen = cmd_text.writeHeadPosition;
        if (templen != 0) {
            System.arraycopy(cmd_text.data, 0, tmp, 0, templen);
            cmd_text.clear();
        }

        // add the entire text of the file
        CommandBuffer.AddText(text);

        // add the copied off data
        if (templen != 0) {
            cmd_text.write(tmp, templen);
        }
    }

    /**
     * @param clear
     */
    static void AddEarlyCommands(boolean clear) {

        CommandLineOptions commandLineOptions = Engine.Companion.getCommandLineOptions();
        for (int i = 0; i < commandLineOptions.count(); i++) {
            String s = commandLineOptions.valueAt(i);
            if (!s.equals("+set"))
                continue;
            CommandBuffer.AddText("set " + commandLineOptions.valueAt(i + 1) + " " + commandLineOptions.valueAt(i + 2) + "\n");
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
        int argc = Engine.Companion.getCommandLineOptions().count();
        for (i = 1; i < argc; i++) {
            s += Engine.Companion.getCommandLineOptions().valueAt(i).length();
        }
        if (s == 0)
            return false;

        String text = "";
        for (i = 1; i < argc; i++) {
            text += Engine.Companion.getCommandLineOptions().valueAt(i);
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
            CommandBuffer.AddText(build);

        text = null;
        build = null;

        return ret;
    }

    /**
     * @param text
     */
    public static void AddText(String text) {
        int l = text.length();

        if (cmd_text.writeHeadPosition + l >= cmd_text.maxsize) {
            Command.Printf("Cbuf_AddText: overflow\n");
            return;
        }
        cmd_text.write(Lib.stringToBytes(text), l);
    }

    /**
     *  
     */
    public static void execute() {

        final byte[] line = new byte[1024];
        Context.alias_count = 0; // don't allow infinite alias loops

        while (cmd_text.writeHeadPosition != 0) {
            // find a \n or ; line break
            byte[] text = cmd_text.data;

            int quotes = 0;
            int i;

            for (i = 0; i < cmd_text.writeHeadPosition; i++) {
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

            if (i == cmd_text.writeHeadPosition)
                cmd_text.writeHeadPosition = 0;
            else {
                i++;
                cmd_text.writeHeadPosition -= i;
                //byte[] tmp = new byte[Context.cmd_text.writeHeadPosition];
                final byte[] tmp = new byte[8192];
                System.arraycopy(text, i, tmp, 0, cmd_text.writeHeadPosition);
                System.arraycopy(tmp, 0, text, 0, cmd_text.writeHeadPosition);
                text[cmd_text.writeHeadPosition] = '\0';

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
            CommandBuffer.InsertText(text);
            break;
        case Defines.EXEC_APPEND:
            CommandBuffer.AddText(text);
            break;
        default:
            Command.Error(Defines.ERR_FATAL, "Cbuf_ExecuteText: bad exec_when");
        }
    }

    /*
     * ============ Cbuf_CopyToDefer ============
     */
    public static void copyToDefer() {
        System.arraycopy(cmd_text.getBuffer(), 0, defer_text_buf, 0, cmd_text.writeHeadPosition);
        defer_text_buf[cmd_text.writeHeadPosition] = 0;
        cmd_text.writeHeadPosition = 0;
    }

    /*
     * ============ Cbuf_InsertFromDefer ============
     */
    public static void insertFromDefer() {
        InsertText(new String(defer_text_buf).trim());
        defer_text_buf[0] = 0;
    }

}