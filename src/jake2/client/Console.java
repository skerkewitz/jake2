/*
 * Con.java
 * Copyright (C) 2003
 * 
 * $Id: Console.java,v 1.9 2008-03-02 16:43:18 cawe Exp $
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

import jake2.Defines;
import jake2.game.Cmd;
import jake2.io.FileSystem;
import jake2.qcommon.Cbuf;
import jake2.qcommon.Command;
import jake2.qcommon.ConsoleVar;
import jake2.qcommon.TXCommand;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import static jake2.Defines.*;
import static jake2.client.Context.*;

/**
 * Console
 */
public final class Console {

    public static TXCommand ToggleConsole_f = () -> {
        SCR.EndLoadingPlaque(); // get rid of loading plaque

        if (Context.cl.attractloop) {
            Cbuf.AddText("killserver\n");
            return;
        }

        if (cls.state == Defines.ca_disconnected) {
            // start the demo loop again
            Cbuf.AddText("d1\n");
            return;
        }

        Key.ClearTyping();
        Console.ClearNotify();

        if (cls.key_dest == key_console) {
            Menu.ForceMenuOff();
            ConsoleVar.Set("paused", "0");
        } else {
            Menu.ForceMenuOff();
            cls.key_dest = key_console;

            if (ConsoleVar.VariableValue("maxclients") == 1
                    && Context.server_state != 0)
                ConsoleVar.Set("paused", "1");
        }
    };

    public static TXCommand Clear_f = () -> Arrays.fill(console.text, (byte) ' ');

    public static TXCommand Dump_f = () -> {

        int l, x;
        int line;
        RandomAccessFile f;
        byte[] buffer = new byte[1024];
        String name;

        if (Cmd.Argc() != 2) {
            Command.Printf("usage: condump <filename>\n");
            return;
        }

        // Com_sprintf (name, sizeof(name), "%s/%s.txt", FS_Gamedir(),
        // Cmd_Argv(1));
        name = FileSystem.Gamedir() + "/" + Cmd.Argv(1) + ".txt";

        Command.Printf("Dumped console text to " + name + ".\n");
        FileSystem.CreatePath(name);
        f = Lib.fopen(name, "rw");
        if (f == null) {
            Command.Printf("ERROR: couldn't open.\n");
            return;
        }

        // skip empty lines
        for (l = console.current - console.totallines + 1; l <= console.current; l++) {
            line = (l % console.totallines) * console.linewidth;
            for (x = 0; x < console.linewidth; x++)
                if (console.text[line + x] != ' ')
                    break;
            if (x != console.linewidth)
                break;
        }

        // write the remaining lines
        buffer[console.linewidth] = 0;
        for (; l <= console.current; l++) {
            line = (l % console.totallines) * console.linewidth;
            // strncpy (buffer, line, console.linewidth);
            System.arraycopy(console.text, line, buffer, 0, console.linewidth);
            for (x = console.linewidth - 1; x >= 0; x--) {
                if (buffer[x] == ' ')
                    buffer[x] = 0;
                else
                    break;
            }
            for (x = 0; buffer[x] != 0; x++)
                buffer[x] &= 0x7f;

            buffer[x] = '\n';
            // fprintf (f, "%s\n", buffer);
            try {
                f.write(buffer, 0, x + 1);
            } catch (IOException e) {
            }
        }

        Lib.fclose(f);

    };

    /**
     *
     */
    public static void Init() {
        console.linewidth = -1;

        CheckResize();

        Command.Printf("Console initialized.\n");

        //
        // register our commands
        //
        Context.con_notifytime = ConsoleVar.Get("con_notifytime", "3", 0);

        Cmd.AddCommand("toggleconsole", ToggleConsole_f);
        Cmd.AddCommand("togglechat", ToggleChat_f);
        Cmd.AddCommand("messagemode", MessageMode_f);
        Cmd.AddCommand("messagemode2", MessageMode2_f);
        Cmd.AddCommand("clear", Clear_f);
        Cmd.AddCommand("condump", Dump_f);
        console.initialized = true;
    }

    /**
     * If the line width has changed, reformat the buffer.
     */
    public static void CheckResize() {

        int width = (Context.viddef.getWidth() >> 3) - 2;
        if (width > Defines.MAXCMDLINE)
            width = Defines.MAXCMDLINE;

        if (width == console.linewidth)
            return;

        if (width < 1) { // video hasn't been initialized yet
            width = 38;
            console.linewidth = width;
            console.totallines = Defines.CON_TEXTSIZE
                    / console.linewidth;
            Arrays.fill(console.text, (byte) ' ');
        } else {
            int oldwidth = console.linewidth;
            console.linewidth = width;
            int oldtotallines = console.totallines;
            console.totallines = Defines.CON_TEXTSIZE
                    / console.linewidth;
            int numlines = oldtotallines;

            if (console.totallines < numlines)
                numlines = console.totallines;

            int numchars = oldwidth;

            if (console.linewidth < numchars)
                numchars = console.linewidth;

            byte[] tbuf = new byte[Defines.CON_TEXTSIZE];
            System
                    .arraycopy(console.text, 0, tbuf, 0,
                            Defines.CON_TEXTSIZE);
            Arrays.fill(console.text, (byte) ' ');

            for (int i = 0; i < numlines; i++) {
                for (int j = 0; j < numchars; j++) {
                    console.text[(console.totallines - 1 - i)
                            * console.linewidth + j] = tbuf[((console.current
                            - i + oldtotallines) % oldtotallines)
                            * oldwidth + j];
                }
            }

            Console.ClearNotify();
        }

        console.current = console.totallines - 1;
        console.display = console.current;
    }

    public static void ClearNotify() {
        int i;
        for (i = 0; i < Defines.NUM_CON_TIMES; i++)
            console.times[i] = 0;
    }

    static void DrawString(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            Context.re.DrawChar(x, y, s.charAt(i));
            x += 8;
        }
    }

    static void DrawAltString(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            Context.re.DrawChar(x, y, s.charAt(i) ^ 0x80);
            x += 8;
        }
    }

    /*
     * ================ Con_ToggleChat_f ================
     */
    static TXCommand ToggleChat_f = new TXCommand() {
        public void execute() {
            Key.ClearTyping();

            if (cls.key_dest == key_console) {
                if (cls.state == ca_active) {
                    Menu.ForceMenuOff();
                    cls.key_dest = key_game;
                }
            } else
                cls.key_dest = key_console;

            ClearNotify();
        }
    };

    /*
     * ================ Con_MessageMode_f ================
     */
    static TXCommand MessageMode_f = new TXCommand() {
        public void execute() {
            Context.chat_team = false;
            cls.key_dest = key_message;
        }
    };

    /*
     * ================ Con_MessageMode2_f ================
     */
    static TXCommand MessageMode2_f = new TXCommand() {
        public void execute() {
            Context.chat_team = true;
            cls.key_dest = key_message;
        }
    };

    /*
     * =============== Con_Linefeed ===============
     */
    static void Linefeed() {
        console.x = 0;
        if (console.display == console.current)
            console.display++;
        console.current++;
        int i = (console.current % console.totallines)
                * console.linewidth;
        int e = i + console.linewidth;
        while (i++ < e)
            console.text[i] = (byte) ' ';
    }

    /*
     * ================ Con_Print
     * 
     * Handles cursor positioning, line wrapping, etc All console printing must
     * go through this in order to be logged to disk If no console is visible,
     * the text will appear at the top of the game window ================
     */
    private static int cr;

    public static void Print(String txt) {
        int y;
        int c, l;
        int mask;
        int txtpos = 0;

        if (!console.initialized)
            return;

        if (txt.charAt(0) == 1 || txt.charAt(0) == 2) {
            mask = 128; // go to colored text
            txtpos++;
        } else
            mask = 0;

        while (txtpos < txt.length()) {
            c = txt.charAt(txtpos);
            // count word length
            for (l = 0; l < console.linewidth && l < (txt.length() - txtpos); l++)
                if (txt.charAt(l + txtpos) <= ' ')
                    break;

            // word wrap
            if (l != console.linewidth && (console.x + l > console.linewidth))
                console.x = 0;

            txtpos++;

            if (cr != 0) {
                console.current--;
                cr = 0;
            }

            if (console.x == 0) {
                Console.Linefeed();
                // mark time for transparent overlay
                if (console.current >= 0)
                    console.times[console.current % NUM_CON_TIMES] = cls.realtime;
            }

            switch (c) {
                case '\n':
                    console.x = 0;
                    break;

                case '\r':
                    console.x = 0;
                    cr = 1;
                    break;

                default: // display character and advance
                    y = console.current % console.totallines;
                    console.text[y * console.linewidth + console.x] = (byte) (c | mask | console.ormask);
                    console.x++;
                    if (console.x >= console.linewidth)
                        console.x = 0;
                    break;
            }
        }
    }

    /*
     * ============== Con_CenteredPrint ==============
     */
    static void CenteredPrint(String text) {
        int l = text.length();
        l = (console.linewidth - l) / 2;
        if (l < 0)
            l = 0;

        StringBuffer sb = new StringBuffer(1024);
        for (int i = 0; i < l; i++)
            sb.append(' ');
        sb.append(text);
        sb.append('\n');

        sb.setLength(1024);

        Console.Print(sb.toString());
    }

    /*
     * ==============================================================================
     * 
     * DRAWING
     * 
     * ==============================================================================
     */

    /*
     * ================ Con_DrawInput
     * 
     * The input line scrolls horizontally if typing goes beyond the right edge
     * ================
     */
    static void DrawInput() {
        int i;
        byte[] text;
        int start = 0;

        if (cls.key_dest == key_menu)
            return;
        if (cls.key_dest != key_console && cls.state == ca_active)
            return; // don't draw anything (always draw if not active)

        text = key_lines[edit_line];

        // add the cursor frame
        text[Key.key_linepos] = (byte) (10 + (cls.realtime >> 8 & 1));

        // fill out remainder with spaces
        for (i = Key.key_linepos + 1; i < console.linewidth; i++)
            text[i] = ' ';

        // prestep if horizontally scrolling
        if (Key.key_linepos >= console.linewidth)
            start += 1 + Key.key_linepos - console.linewidth;

        // draw it
        // y = console.vislines-16;

        for (i = 0; i < console.linewidth; i++)
            re.DrawChar((i + 1) << 3, console.vislines - 22, text[i]);

        // remove cursor
        key_lines[edit_line][Key.key_linepos] = 0;
    }

    /*
     * ================ Con_DrawNotify
     * 
     * Draws the last few lines of output transparently over the game top
     * ================
     */
    static void DrawNotify() {
        int x, v;
        int text;
        int i;
        int time;
        String s;
        int skip;

        v = 0;
        for (i = console.current - NUM_CON_TIMES + 1; i <= console.current; i++) {
            if (i < 0)
                continue;

            time = (int) console.times[i % NUM_CON_TIMES];
            if (time == 0)
                continue;

            time = cls.realtime - time;
            if (time > con_notifytime.value * 1000)
                continue;

            text = (i % console.totallines) * console.linewidth;

            for (x = 0; x < console.linewidth; x++)
                re.DrawChar((x + 1) << 3, v, console.text[text + x]);

            v += 8;
        }

        if (cls.key_dest == key_message) {
            if (chat_team) {
                DrawString(8, v, "say_team:");
                skip = 11;
            } else {
                DrawString(8, v, "say:");
                skip = 5;
            }

            s = chat_buffer;
            if (chat_bufferlen > (viddef.getWidth() >> 3) - (skip + 1))
                s = s.substring(chat_bufferlen
                        - ((viddef.getWidth() >> 3) - (skip + 1)));

            for (x = 0; x < s.length(); x++) {
                re.DrawChar((x + skip) << 3, v, s.charAt(x));
            }
            re.DrawChar((x + skip) << 3, v,
                    10 + ((cls.realtime >> 8) & 1));
            v += 8;
        }

        if (v != 0) {
            SCR.AddDirtyPoint(0, 0);
            SCR.AddDirtyPoint(viddef.getWidth() - 1, v);
        }
    }

    /*
     * ================ Con_DrawConsole
     * 
     * Draws the console with the solid background ================
     */
    static void DrawConsole(float frac) {

        int width = viddef.getWidth();
        int height = viddef.getHeight();
        int lines = (int) (height * frac);
        if (lines <= 0)
            return;

        if (lines > height)
            lines = height;

        // draw the background
        re.DrawStretchPic(0, -height + lines, width, height, "conback");
        SCR.AddDirtyPoint(0, 0);
        SCR.AddDirtyPoint(width - 1, lines - 1);

        String version = Command.sprintf("v%4.2f", VERSION);
        for (int x = 0; x < 5; x++)
            re
                    .DrawChar(width - 44 + x * 8, lines - 12, 128 + version
                            .charAt(x));

        // draw the text
        console.vislines = lines;

        int rows = (lines - 22) >> 3; // rows of text to draw

        int y = lines - 30;

        // draw from the bottom up
        if (console.display != console.current) {
            // draw arrows to show the buffer is backscrolled
            for (int x = 0; x < console.linewidth; x += 4)
                re.DrawChar((x + 1) << 3, y, '^');

            y -= 8;
            rows--;
        }

        int i, j, x, n;

        int row = console.display;
        for (i = 0; i < rows; i++, y -= 8, row--) {
            if (row < 0)
                break;
            if (console.current - row >= console.totallines)
                break; // past scrollback wrap point

            int first = (row % console.totallines) * console.linewidth;

            for (x = 0; x < console.linewidth; x++)
                re.DrawChar((x + 1) << 3, y, console.text[x + first]);
        }

        // ZOID
        // draw the download bar
        // figure out width
        if (cls.download != null) {
            int text;
            if ((text = cls.downloadname.lastIndexOf('/')) != 0)
                text++;
            else
                text = 0;

            x = console.linewidth - ((console.linewidth * 7) / 40);
            y = x - (cls.downloadname.length() - text) - 8;
            i = console.linewidth / 3;
            StringBuffer dlbar = new StringBuffer(512);
            if (cls.downloadname.length() - text > i) {
                y = x - i - 11;
                int end = text + i - 1;
                dlbar.append(cls.downloadname.substring(text, end));
                dlbar.append("...");
            } else {
                dlbar.append(cls.downloadname.substring(text));
            }
            dlbar.append(": ");
            dlbar.append((char) 0x80);

            // where's the dot go?
            if (cls.downloadpercent == 0)
                n = 0;
            else
                n = y * cls.downloadpercent / 100;

            for (j = 0; j < y; j++) {
                if (j == n)
                    dlbar.append((char) 0x83);
                else
                    dlbar.append((char) 0x81);
            }
            dlbar.append((char) 0x82);
            dlbar.append((cls.downloadpercent < 10) ? " 0" : " ");
            dlbar.append(cls.downloadpercent).append('%');
            // draw it
            y = console.vislines - 12;
            for (i = 0; i < dlbar.length(); i++)
                re.DrawChar((i + 1) << 3, y, dlbar.charAt(i));
        }
        // ZOID

        // draw the input prompt, user text, and cursor if desired
        DrawInput();
    }
}