/*
 * Cmd.java
 * Copyright (C) 2003
 * 
 * $Id: Cmd.java,v 1.18 2006-01-21 21:53:32 salomo Exp $
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

import jake2.Defines;
import jake2.client.Context;
import jake2.game.monsters.M_Player;
import jake2.io.FileSystem;
import jake2.qcommon.*;
import jake2.server.ServerGame;
import jake2.util.Lib;

import java.util.*;

/**
 * Cmd
 */
public final class Cmd {
    private static final int MAX_STRING_TOKENS = 80; // max tokens resulting from Cmd_TokenizeString
    private static final int ALIAS_LOOP_COUNT = 16;

    private static int cmd_argc;
    private static String[] cmd_argv = new String[MAX_STRING_TOKENS];

    public static TXCommand Wait_f = () -> Context.cmd_wait = true;

    public static Comparator PlayerSort = (o1, o2) -> {
        int anum = ((Integer) o1).intValue();
        int bnum = ((Integer) o2).intValue();

        int anum1 = GameBase.game.clients[anum].ps.stats[Defines.STAT_FRAGS];
        int bnum1 = GameBase.game.clients[bnum].ps.stats[Defines.STAT_FRAGS];

        if (anum1 < bnum1)
            return -1;
        if (anum1 > bnum1)
            return 1;
        return 0;
    };

    private static Map<String, TCmdFunction> cmd_functions = new HashMap<>();


    static TXCommand List_f = () -> {
        int i = 0;
        for (TCmdFunction cmd : cmd_functions.values()) {
            Command.Printf(cmd.name + '\n');
            i++;
        }
        Command.Printf(i + " commands\n");
    };

    static TXCommand Exec_f = () -> {
        if (Cmd.Argc() != 2) {
            Command.Printf("exec <filename> : execute a script file\n");
            return;
        }

        byte[] f = null;
        String filename = Cmd.Argv(1);
        f = FileSystem.loadFile(filename);
        if (f == null) {
            Command.Printf("couldn't exec " + filename + "\n");
            return;
        }
        Command.Printf("execing " + filename + "\n");

        CommandBuffer.InsertText(new String(f));

        FileSystem.FreeFile(f);
    };
    static TXCommand Echo_f = () -> {
        for (int i = 1; i < Cmd.Argc(); i++) {
            Command.Printf(Cmd.Argv(i) + " ");
        }
        Command.Printf("'\n");
    };
    static TXCommand Alias_f = () -> {
        cmdalias_t a = null;
        if (Cmd.Argc() == 1) {
            Command.Printf("Current alias commands:\n");
            for (a = Context.cmd_alias; a != null; a = a.next) {
                Command.Printf(a.name + " : " + a.value);
            }
            return;
        }

        String s = Cmd.Argv(1);
        if (s.length() > Defines.MAX_ALIAS_NAME) {
            Command.Printf("Alias name is too long\n");
            return;
        }

        // if the alias already exists, reuse it
        for (a = Context.cmd_alias; a != null; a = a.next) {
            if (s.equalsIgnoreCase(a.name)) {
                a.value = null;
                break;
            }
        }

        if (a == null) {
            a = new cmdalias_t();
            a.next = Context.cmd_alias;
            Context.cmd_alias = a;
        }
        a.name = s;

        // copy the rest of the command line
        String cmd = "";
        int c = Cmd.Argc();
        for (int i = 2; i < c; i++) {
            cmd = cmd + Cmd.Argv(i);
            if (i != (c - 1))
                cmd = cmd + " ";
        }
        cmd = cmd + "\n";

        a.value = cmd;
    };
    private static String cmd_args;
    private static char expanded[] = new char[Defines.MAX_STRING_CHARS];

    private static char temporary[] = new char[Defines.MAX_STRING_CHARS];

    /**
     * Register our commands.
     */
    public static void Init() {

        Cmd.registerCommand("exec", Exec_f);
        Cmd.registerCommand("echo", Echo_f);
        Cmd.registerCommand("cmdlist", List_f);
        Cmd.registerCommand("alias", Alias_f);
        Cmd.registerCommand("wait", Wait_f);
    }

    /**
     * Cmd_MacroExpandString.
     */
    public static char[] MacroExpandString(char text[], int len) {
        int i, j, count;
        boolean inquote;

        char scan[];

        String token;
        inquote = false;

        scan = text;

        if (len >= Defines.MAX_STRING_CHARS) {
            Command.Printf("Line exceeded " + Defines.MAX_STRING_CHARS
                    + " chars, discarded.\n");
            return null;
        }

        count = 0;

        for (i = 0; i < len; i++) {
            if (scan[i] == '"')
                inquote = !inquote;

            if (inquote)
                continue; // don't expand inside quotes

            if (scan[i] != '$')
                continue;

            // scan out the complete macro, without $
            Command.ParseHelp ph = new Command.ParseHelp(text, i + 1);
            token = Command.Parse(ph);

            if (ph.data == null)
                continue;

            token = ConsoleVar.VariableString(token);

            j = token.length();

            len += j;

            if (len >= Defines.MAX_STRING_CHARS) {
                Command.Printf("Expanded line exceeded " + Defines.MAX_STRING_CHARS
                        + " chars, discarded.\n");
                return null;
            }

            System.arraycopy(scan, 0, temporary, 0, i);
            System.arraycopy(token.toCharArray(), 0, temporary, i, token.length());
            System.arraycopy(ph.data, ph.index, temporary, i + j, len - ph.index - j);

            System.arraycopy(temporary, 0, expanded, 0, 0);
            scan = expanded;
            i--;
            if (++count == 100) {
                Command.Printf("Macro expansion loop, discarded.\n");
                return null;
            }
        }

        if (inquote) {
            Command.Printf("Line has unmatched quote, discarded.\n");
            return null;
        }

        return scan;
    }

    /**
     * Cmd_TokenizeString
     * <p>
     * Parses the given string into command line tokens. $Cvars will be expanded
     * unless they are in a quoted token.
     */
    public static void TokenizeString(char text[], boolean macroExpand) {


        cmd_argc = 0;
        cmd_args = "";

        int len = Lib.strlen(text);

        // macro expand the text
        if (macroExpand)
            text = MacroExpandString(text, len);

        if (text == null)
            return;

        len = Lib.strlen(text);

        Command.ParseHelp ph = new Command.ParseHelp(text);

        while (true) {

            // skip whitespace up to a /n
            char c = ph.skipwhitestoeol();

            if (c == '\n') { // a newline seperates commands in the buffer
                c = ph.nextchar();
                break;
            }

            if (c == 0)
                return;

            // assign cmd_args to everything after the first arg
            if (cmd_argc == 1) {
                cmd_args = new String(text, ph.index, len - ph.index);
                cmd_args.trim();
            }

            String com_token = Command.Parse(ph);

            if (ph.data == null)
                return;

            if (cmd_argc < MAX_STRING_TOKENS) {
                cmd_argv[cmd_argc] = com_token;
                cmd_argc++;
            }
        }
    }

    public static void registerCommand(String name, TXCommand function) {

        //Command.DPrintf("Cmd_AddCommand: " + cmd_name + "\n");
        // fail if the command is a variable name
        if ((ConsoleVar.VariableString(name)).length() > 0) {
            Command.Printf("Cmd_AddCommand: " + name + " already defined as a var\n");
            return;
        }

        // fail if the command already exists
        if (cmd_functions.containsKey(name)) {
            Command.Printf("Cmd_AddCommand: " + name + " already defined\n");
            return;
        }

        cmd_functions.put(name, new TCmdFunction(name, function));
    }

    /**
     * Cmd_RemoveCommand
     */
    public static void removeCommand(String cmd_name) {

        if (cmd_functions.remove(cmd_name) == null) {
            Command.Printf("Cmd_RemoveCommand: " + cmd_name + " not added\n");
        }
    }

    /**
     * Cmd_Exists
     */
    public static boolean Exists(String cmd_name) {
        return cmd_functions.containsKey(cmd_name);
    }

    public static int Argc() {
        return cmd_argc;
    }

    public static String Argv(int i) {
        if (i < 0 || i >= cmd_argc)
            return "";
        return cmd_argv[i];
    }

    public static String Args() {
        return new String(cmd_args);
    }

    /**
     * Cmd_ExecuteString
     * <p>
     * A complete command line has been parsed, so try to execute it
     * FIXME: lookupnoadd the token to speed search?
     */
    public static void ExecuteString(String text) {

        TokenizeString(text.toCharArray(), true);

        // execute the command line
        if (Argc() == 0)
            return; // no tokens

        // check functions
        TCmdFunction cmd = cmd_functions.get(cmd_argv[0]);
        if (cmd != null) {
            if (null == cmd.function) { // forward to server command
                Cmd.ExecuteString("cmd " + text);
            } else {
                cmd.function.execute();
            }
            return;
        }

        // check alias
        for (cmdalias_t a = Context.cmd_alias; a != null; a = a.next) {

            if (cmd_argv[0].equalsIgnoreCase(a.name)) {

                if (++Context.alias_count == ALIAS_LOOP_COUNT) {
                    Command.Printf("ALIAS_LOOP_COUNT\n");
                    return;
                }
                CommandBuffer.InsertText(a.value);
                return;
            }
        }

        // check cvars
        if (ConsoleVar.Command())
            return;

        // send it as a server command if we are connected
        Cmd.ForwardToServer();
    }

    /**
     * Cmd_Give_f
     * <p>
     * Give items to a client.
     */
    public static void Give_f(TEntityDict ent) {


        int index;
        int i;
        boolean give_all;
        TEntityDict it_ent;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent,
                    "You must run the server with '+assign cheats 1' to enable this command.\n");
            return;
        }

        String name = Cmd.Args();

        give_all = 0 == Lib.Q_stricmp(name, "all");

        if (give_all || 0 == Lib.Q_stricmp(Cmd.Argv(1), "health")) {
            if (Cmd.Argc() == 3)
                ent.health = Lib.atoi(Cmd.Argv(2));
            else
                ent.health = ent.max_health;
            if (!give_all)
                return;
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "weapons")) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                TGItem it = GameItemList.itemlist[i];
                if (null == it.pickup)
                    continue;
                if (0 == (it.flags & Defines.IT_WEAPON))
                    continue;
                ent.client.pers.inventory[i] += 1;
            }
            if (!give_all)
                return;
        }

        if (give_all || 0 == Lib.Q_stricmp(name, "ammo")) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                TGItem it = GameItemList.itemlist[i];
                if (null == it.pickup)
                    continue;
                if (0 == (it.flags & Defines.IT_AMMO))
                    continue;
                GameItems.Add_Ammo(ent, it, 1000);
            }
            if (!give_all)
                return;
        }

        if (give_all || Lib.Q_stricmp(name, "armor") == 0) {
            gitem_armor_t info;

            TGItem it = GameItems.FindItem("Jacket Armor");
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0;

            it = GameItems.FindItem("Combat Armor");
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = 0;

            it = GameItems.FindItem("Body Armor");
            info = it.info;
            ent.client.pers.inventory[GameItems.ITEM_INDEX(it)] = info.max_count;

            if (!give_all)
                return;
        }

        if (give_all || Lib.Q_stricmp(name, "Power Shield") == 0) {
            TGItem it = GameItems.FindItem("Power Shield");
            it_ent = GameUtil.G_Spawn();
            it_ent.classname = it.classname;
            GameItems.SpawnItem(it_ent, it);
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null);
            if (it_ent.inUse)
                GameUtil.G_FreeEdict(it_ent);

            if (!give_all)
                return;
        }

        if (give_all) {
            for (i = 1; i < GameBase.game.num_items; i++) {
                TGItem it = GameItemList.itemlist[i];
                if (it.pickup != null)
                    continue;
                if ((it.flags & (Defines.IT_ARMOR | Defines.IT_WEAPON | Defines.IT_AMMO)) != 0)
                    continue;
                ent.client.pers.inventory[i] = 1;
            }
            return;
        }

        TGItem it = GameItems.FindItem(name);
        if (it == null) {
            name = Cmd.Argv(1);
            it = GameItems.FindItem(name);
            if (it == null) {
                ServerGame.PF_cprintf(ent, Defines.PRINT_HIGH, "unknown item\n");
                return;
            }
        }

        if (it.pickup == null) {
            ServerGame.PF_cprintf(ent, Defines.PRINT_HIGH, "non-pickup item\n");
            return;
        }

        index = GameItems.ITEM_INDEX(it);

        if ((it.flags & Defines.IT_AMMO) != 0) {
            if (Cmd.Argc() == 3)
                ent.client.pers.inventory[index] = Lib.atoi(Cmd.Argv(2));
            else
                ent.client.pers.inventory[index] += it.quantity;
        } else {
            it_ent = GameUtil.G_Spawn();
            it_ent.classname = it.classname;
            GameItems.SpawnItem(it_ent, it);
            GameItems.Touch_Item(it_ent, ent, GameBase.dummyplane, null);
            if (it_ent.inUse)
                GameUtil.G_FreeEdict(it_ent);
        }
    }

    /**
     * Cmd_God_f
     * <p>
     * Sets client to godmode
     * <p>
     * argv(0) god
     */
    public static void God_f(TEntityDict ent) {
        String msg;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent,
                    "You must run the server with '+assign cheats 1' to enable this command.\n");
            return;
        }

        ent.flags ^= Defines.FL_GODMODE;
        if (0 == (ent.flags & Defines.FL_GODMODE))
            msg = "godmode OFF\n";
        else
            msg = "godmode ON\n";

        ServerGame.PF_cprintf(ent, Defines.PRINT_HIGH, msg);
    }

    /**
     * Cmd_Notarget_f
     * <p>
     * Sets client to notarget
     * <p>
     * argv(0) notarget.
     */
    public static void Notarget_f(TEntityDict ent) {
        String msg;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent,
                    "You must run the server with '+assign cheats 1' to enable this command.\n");
            return;
        }

        ent.flags ^= Defines.FL_NOTARGET;
        if (0 == (ent.flags & Defines.FL_NOTARGET))
            msg = "notarget OFF\n";
        else
            msg = "notarget ON\n";

        ServerGame.PF_cprintfhigh(ent, msg);
    }

    /**
     * Cmd_Noclip_f
     * <p>
     * argv(0) noclip.
     */
    public static void Noclip_f(TEntityDict ent) {
        String msg;

        if (GameBase.deathmatch.value != 0 && GameBase.sv_cheats.value == 0) {
            ServerGame.PF_cprintfhigh(ent,
                    "You must run the server with '+assign cheats 1' to enable this command.\n");
            return;
        }

        if (ent.movetype == Defines.MOVETYPE_NOCLIP) {
            ent.movetype = Defines.MOVETYPE_WALK;
            msg = "noclip OFF\n";
        } else {
            ent.movetype = Defines.MOVETYPE_NOCLIP;
            msg = "noclip ON\n";
        }

        ServerGame.PF_cprintfhigh(ent, msg);
    }

    /**
     * Cmd_Use_f
     * <p>
     * Use an inventory item.
     */
    public static void Use_f(TEntityDict ent) {
        int index;
        TGItem it;
        String s;

        s = Cmd.Args();

        it = GameItems.FindItem(s);
        Command.dprintln("using:" + s);
        if (it == null) {
            ServerGame.PF_cprintfhigh(ent, "unknown item: " + s + "\n");
            return;
        }
        if (it.use == null) {
            ServerGame.PF_cprintfhigh(ent, "Item is not usable.\n");
            return;
        }
        index = GameItems.ITEM_INDEX(it);
        if (0 == ent.client.pers.inventory[index]) {
            ServerGame.PF_cprintfhigh(ent, "Out of item: " + s + "\n");
            return;
        }

        it.use.use(ent, it);
    }

    /**
     * Cmd_Drop_f
     * <p>
     * Drop an inventory item.
     */
    public static void Drop_f(TEntityDict ent) {
        int index;
        TGItem it;
        String s;

        s = Cmd.Args();
        it = GameItems.FindItem(s);
        if (it == null) {
            ServerGame.PF_cprintfhigh(ent, "unknown item: " + s + "\n");
            return;
        }
        if (it.drop == null) {
            ServerGame.PF_cprintf(ent, Defines.PRINT_HIGH,
                    "Item is not dropable.\n");
            return;
        }
        index = GameItems.ITEM_INDEX(it);
        if (0 == ent.client.pers.inventory[index]) {
            ServerGame.PF_cprintfhigh(ent, "Out of item: " + s + "\n");
            return;
        }

        it.drop.drop(ent, it);
    }

    /**
     * Cmd_Inven_f.
     */
    public static void Inven_f(TEntityDict ent) {
        int i;
        gclient_t cl;

        cl = ent.client;

        cl.showscores = false;
        cl.showhelp = false;

        if (cl.showinventory) {
            cl.showinventory = false;
            return;
        }

        cl.showinventory = true;

        GameBase.gi.WriteByte(Defines.svc_inventory);
        for (i = 0; i < Defines.MAX_ITEMS; i++) {
            GameBase.gi.WriteShort(cl.pers.inventory[i]);
        }
        GameBase.gi.unicast(ent, true);
    }

    /**
     * Cmd_InvUse_f.
     */
    public static void InvUse_f(TEntityDict ent) {
        TGItem it;

        Cmd.ValidateSelectedItem(ent);

        if (ent.client.pers.selected_item == -1) {
            ServerGame.PF_cprintfhigh(ent, "No item to use.\n");
            return;
        }

        it = GameItemList.itemlist[ent.client.pers.selected_item];
        if (it.use == null) {
            ServerGame.PF_cprintfhigh(ent, "Item is not usable.\n");
            return;
        }
        it.use.use(ent, it);
    }

    /**
     * Cmd_WeapPrev_f.
     */
    public static void WeapPrev_f(TEntityDict ent) {
        gclient_t cl;
        int i, index;
        TGItem it;
        int selected_weapon;

        cl = ent.client;

        if (cl.pers.weapon == null)
            return;

        selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon);

        // scan for the next valid one
        for (i = 1; i <= Defines.MAX_ITEMS; i++) {
            index = (selected_weapon + i) % Defines.MAX_ITEMS;
            if (0 == cl.pers.inventory[index])
                continue;

            it = GameItemList.itemlist[index];
            if (it.use == null)
                continue;

            if (0 == (it.flags & Defines.IT_WEAPON))
                continue;
            it.use.use(ent, it);
            if (cl.pers.weapon == it)
                return; // successful
        }
    }

    /**
     * Cmd_WeapNext_f.
     */
    public static void WeapNext_f(TEntityDict ent) {
        gclient_t cl;
        int i, index;
        TGItem it;
        int selected_weapon;

        cl = ent.client;

        if (null == cl.pers.weapon)
            return;

        selected_weapon = GameItems.ITEM_INDEX(cl.pers.weapon);

        // scan for the next valid one
        for (i = 1; i <= Defines.MAX_ITEMS; i++) {
            index = (selected_weapon + Defines.MAX_ITEMS - i)
                    % Defines.MAX_ITEMS;
            //bugfix rst
            if (index == 0)
                index++;
            if (0 == cl.pers.inventory[index])
                continue;
            it = GameItemList.itemlist[index];
            if (null == it.use)
                continue;
            if (0 == (it.flags & Defines.IT_WEAPON))
                continue;
            it.use.use(ent, it);
            if (cl.pers.weapon == it)
                return; // successful
        }
    }

    /**
     * Cmd_WeapLast_f.
     */
    public static void WeapLast_f(TEntityDict ent) {
        gclient_t cl;
        int index;
        TGItem it;

        cl = ent.client;

        if (null == cl.pers.weapon || null == cl.pers.lastweapon)
            return;

        index = GameItems.ITEM_INDEX(cl.pers.lastweapon);
        if (0 == cl.pers.inventory[index])
            return;
        it = GameItemList.itemlist[index];
        if (null == it.use)
            return;
        if (0 == (it.flags & Defines.IT_WEAPON))
            return;
        it.use.use(ent, it);
    }

    /**
     * Cmd_InvDrop_f
     */
    public static void InvDrop_f(TEntityDict ent) {
        TGItem it;

        Cmd.ValidateSelectedItem(ent);

        if (ent.client.pers.selected_item == -1) {
            ServerGame.PF_cprintfhigh(ent, "No item to drop.\n");
            return;
        }

        it = GameItemList.itemlist[ent.client.pers.selected_item];
        if (it.drop == null) {
            ServerGame.PF_cprintfhigh(ent, "Item is not dropable.\n");
            return;
        }
        it.drop.drop(ent, it);
    }

    /**
     * Cmd_Score_f
     * <p>
     * Display the scoreboard.
     */
    public static void Score_f(TEntityDict ent) {
        ent.client.showinventory = false;
        ent.client.showhelp = false;

        if (0 == GameBase.deathmatch.value && 0 == GameBase.coop.value)
            return;

        if (ent.client.showscores) {
            ent.client.showscores = false;
            return;
        }

        ent.client.showscores = true;
        PlayerHud.DeathmatchScoreboard(ent);
    }

    /**
     * Cmd_Help_f
     * <p>
     * Display the current help message.
     */
    public static void Help_f(TEntityDict ent) {
        // this is for backwards compatability
        if (GameBase.deathmatch.value != 0) {
            Score_f(ent);
            return;
        }

        ent.client.showinventory = false;
        ent.client.showscores = false;

        if (ent.client.showhelp
                && (ent.client.pers.game_helpchanged == GameBase.game.helpchanged)) {
            ent.client.showhelp = false;
            return;
        }

        ent.client.showhelp = true;
        ent.client.pers.helpchanged = 0;
        PlayerHud.HelpComputer(ent);
    }

    /**
     * Cmd_Kill_f
     */
    public static void Kill_f(TEntityDict ent) {
        if ((GameBase.level.time - ent.client.respawn_time) < 5)
            return;
        ent.flags &= ~Defines.FL_GODMODE;
        ent.health = 0;
        GameBase.meansOfDeath = Defines.MOD_SUICIDE;
        PlayerClient.player_die.die(ent, ent, ent, 100000, Context.vec3_origin);
    }

    /**
     * Cmd_PutAway_f
     */
    public static void PutAway_f(TEntityDict ent) {
        ent.client.showscores = false;
        ent.client.showhelp = false;
        ent.client.showinventory = false;
    }

    /**
     * Cmd_Players_f
     */
    public static void Players_f(TEntityDict ent) {
        int i;
        int count;
        String small;
        String large;

        Integer index[] = new Integer[256];

        count = 0;
        for (i = 0; i < GameBase.maxclients.value; i++) {
            if (GameBase.game.clients[i].pers.connected) {
                index[count] = new Integer(i);
                count++;
            }
        }

        // sort by frags
        Arrays.sort(index, 0, count - 1, Cmd.PlayerSort);

        // print information
        large = "";

        for (i = 0; i < count; i++) {
            small = GameBase.game.clients[index[i].intValue()].ps.stats[Defines.STAT_FRAGS]
                    + " "
                    + GameBase.game.clients[index[i].intValue()].pers.netname
                    + "\n";

            if (small.length() + large.length() > 1024 - 100) {
                // can't print all of them in one packet
                large += "...\n";
                break;
            }
            large += small;
        }

        ServerGame.PF_cprintfhigh(ent, large + "\n" + count + " players\n");
    }

    /**
     * Cmd_Wave_f
     */
    public static void Wave_f(TEntityDict ent) {
        int i;

        i = Lib.atoi(Cmd.Argv(1));

        // can't wave when ducked
        if ((ent.client.ps.pmove.pm_flags & pmove_t.PMF_DUCKED) != 0)
            return;

        if (ent.client.anim_priority > Defines.ANIM_WAVE)
            return;

        ent.client.anim_priority = Defines.ANIM_WAVE;

        switch (i) {
            case 0:
                ServerGame.PF_cprintfhigh(ent, "flipoff\n");
                ent.entityState.frame = M_Player.FRAME_flip01 - 1;
                ent.client.anim_end = M_Player.FRAME_flip12;
                break;
            case 1:
                ServerGame.PF_cprintfhigh(ent, "salute\n");
                ent.entityState.frame = M_Player.FRAME_salute01 - 1;
                ent.client.anim_end = M_Player.FRAME_salute11;
                break;
            case 2:
                ServerGame.PF_cprintfhigh(ent, "taunt\n");
                ent.entityState.frame = M_Player.FRAME_taunt01 - 1;
                ent.client.anim_end = M_Player.FRAME_taunt17;
                break;
            case 3:
                ServerGame.PF_cprintfhigh(ent, "wave\n");
                ent.entityState.frame = M_Player.FRAME_wave01 - 1;
                ent.client.anim_end = M_Player.FRAME_wave11;
                break;
            case 4:
            default:
                ServerGame.PF_cprintfhigh(ent, "point\n");
                ent.entityState.frame = M_Player.FRAME_point01 - 1;
                ent.client.anim_end = M_Player.FRAME_point12;
                break;
        }
    }

    /**
     * Command to print the players own position.
     */
    public static void ShowPosition_f(TEntityDict ent) {
        ServerGame.PF_cprintfhigh(ent, "pos=" + Lib.vtofsbeaty(ent.entityState.origin) + "\n");
    }

    /**
     * Cmd_Say_f
     */
    public static void Say_f(TEntityDict ent, boolean team, boolean arg0) {

        int i, j;
        TEntityDict other;
        String text;
        gclient_t cl;

        if (Cmd.Argc() < 2 && !arg0)
            return;

        if (0 == ((int) (GameBase.dmflags.value) & (Defines.DF_MODELTEAMS | Defines.DF_SKINTEAMS)))
            team = false;

        if (team)
            text = "(" + ent.client.pers.netname + "): ";
        else
            text = "" + ent.client.pers.netname + ": ";

        if (arg0) {
            text += Cmd.Argv(0);
            text += " ";
            text += Cmd.Args();
        } else {
            if (Cmd.Args().startsWith("\""))
                text += Cmd.Args().substring(1, Cmd.Args().length() - 1);
            else
                text += Cmd.Args();
        }

        // don't let text be too long for malicious reasons
        if (text.length() > 150)
            //text[150] = 0;
            text = text.substring(0, 150);

        text += "\n";

        if (GameBase.flood_msgs.value != 0) {
            cl = ent.client;

            if (GameBase.level.time < cl.flood_locktill) {
                ServerGame.PF_cprintfhigh(ent, "You can't talk for "
                        + (int) (cl.flood_locktill - GameBase.level.time)
                        + " more seconds\n");
                return;
            }
            i = (int) (cl.flood_whenhead - GameBase.flood_msgs.value + 1);
            if (i < 0)
                i = (10) + i;
            if (cl.flood_when[i] != 0
                    && GameBase.level.time - cl.flood_when[i] < GameBase.flood_persecond.value) {
                cl.flood_locktill = GameBase.level.time + GameBase.flood_waitdelay.value;
                ServerGame.PF_cprintf(ent, Defines.PRINT_CHAT,
                        "Flood protection:  You can't talk for "
                                + (int) GameBase.flood_waitdelay.value
                                + " seconds.\n");
                return;
            }

            cl.flood_whenhead = (cl.flood_whenhead + 1) % 10;
            cl.flood_when[cl.flood_whenhead] = GameBase.level.time;
        }

        if (Context.dedicated.value != 0)
            ServerGame.PF_cprintf(null, Defines.PRINT_CHAT, "" + text + "");

        for (j = 1; j <= GameBase.game.maxclients; j++) {
            other = GameBase.entityDicts[j];
            if (!other.inUse)
                continue;
            if (other.client == null)
                continue;
            if (team) {
                if (!GameUtil.OnSameTeam(ent, other))
                    continue;
            }
            ServerGame.PF_cprintf(other, Defines.PRINT_CHAT, "" + text + "");
        }

    }

    /**
     * Returns the playerlist. TODO: The list is badly formatted at the moment.
     */
    public static void PlayerList_f(TEntityDict ent) {
        int i;
        String st;
        String text;
        TEntityDict e2;

        // connect time, ping, score, name
        text = "";

        for (i = 0; i < GameBase.maxclients.value; i++) {
            e2 = GameBase.entityDicts[1 + i];
            if (!e2.inUse)
                continue;

            st = ""
                    + (GameBase.level.framenum - e2.client.resp.enterframe)
                    / 600
                    + ":"
                    + ((GameBase.level.framenum - e2.client.resp.enterframe) % 600)
                    / 10 + " " + e2.client.ping + " " + e2.client.resp.score
                    + " " + e2.client.pers.netname + " "
                    + (e2.client.resp.spectator ? " (spectator)" : "") + "\n";

            if (text.length() + st.length() > 1024 - 50) {
                text += "And more...\n";
                ServerGame.PF_cprintfhigh(ent, "" + text + "");
                return;
            }
            text += st;
        }
        ServerGame.PF_cprintfhigh(ent, text);
    }

    /**
     * Adds the current command line as a clc_stringcmd to the client message.
     * things like godmode, noclip, etc, are commands directed to the server, so
     * when they are typed in at the console, they will need to be forwarded.
     */
    public static void ForwardToServer() {


        String cmd = Cmd.Argv(0);
        if (Context.cls.getState() <= Defines.ca_connected || cmd.charAt(0) == '-'
                || cmd.charAt(0) == '+') {
            Command.Printf("Unknown command \"" + cmd + "\"\n");
            return;
        }

        Context.cls.getNetchan().message.writeByte(Defines.clc_stringcmd);
        Context.cls.getNetchan().message.print(cmd);
        if (Cmd.Argc() > 1) {
            Context.cls.getNetchan().message.print(" ");
            Context.cls.getNetchan().message.print(Cmd.Args());
        }
    }

    /**
     * Cmd_CompleteCommand.
     */
    public static List<String> CompleteCommand(String partial) {
        List<String> cmds = new ArrayList<>();

        // check for match
        for (String cmd : cmd_functions.keySet()) {
            if (cmd.startsWith(partial)) {
                cmds.add(cmd);
            }
        }

        for (cmdalias_t a = Context.cmd_alias; a != null; a = a.next)
            if (a.name.startsWith(partial))
                cmds.add(a.name);

        return cmds;
    }

    /**
     * Processes the commands the player enters in the quake console.
     */
    public static void ClientCommand(TEntityDict ent) {

        if (ent.client == null) {
            return; // not fully in game yet
        }

        final String cmd = GameBase.gi.argv(0).toLowerCase();

        if (cmd.equals("players")) {
            Players_f(ent);
            return;
        }
        if (cmd.equals("say")) {
            Say_f(ent, false, false);
            return;
        }
        if (cmd.equals("say_team")) {
            Say_f(ent, true, false);
            return;
        }
        if (cmd.equals("score")) {
            Score_f(ent);
            return;
        }
        if (cmd.equals("help")) {
            Help_f(ent);
            return;
        }

        if (GameBase.level.intermissiontime != 0)
            return;

        if (cmd.equals("use"))
            Use_f(ent);
        else if (cmd.equals("drop"))
            Drop_f(ent);
        else if (cmd.equals("give"))
            Give_f(ent);
        else if (cmd.equals("god"))
            God_f(ent);
        else if (cmd.equals("notarget"))
            Notarget_f(ent);
        else if (cmd.equals("noclip"))
            Noclip_f(ent);
        else if (cmd.equals("inven"))
            Inven_f(ent);
        else if (cmd.equals("invnext"))
            GameItems.SelectNextItem(ent, -1);
        else if (cmd.equals("invprev"))
            GameItems.SelectPrevItem(ent, -1);
        else if (cmd.equals("invnextw"))
            GameItems.SelectNextItem(ent, Defines.IT_WEAPON);
        else if (cmd.equals("invprevw"))
            GameItems.SelectPrevItem(ent, Defines.IT_WEAPON);
        else if (cmd.equals("invnextp"))
            GameItems.SelectNextItem(ent, Defines.IT_POWERUP);
        else if (cmd.equals("invprevp"))
            GameItems.SelectPrevItem(ent, Defines.IT_POWERUP);
        else if (cmd.equals("invuse"))
            InvUse_f(ent);
        else if (cmd.equals("invdrop"))
            InvDrop_f(ent);
        else if (cmd.equals("weapprev"))
            WeapPrev_f(ent);
        else if (cmd.equals("weapnext"))
            WeapNext_f(ent);
        else if (cmd.equals("weaplast"))
            WeapLast_f(ent);
        else if (cmd.equals("kill"))
            Kill_f(ent);
        else if (cmd.equals("putaway"))
            PutAway_f(ent);
        else if (cmd.equals("wave"))
            Wave_f(ent);
        else if (cmd.equals("playerlist"))
            PlayerList_f(ent);
        else if (cmd.equals("showposition"))
            ShowPosition_f(ent);
        else
            // anything that doesn't match a command will be a chat
            Say_f(ent, false, true);
    }

    public static void ValidateSelectedItem(TEntityDict ent) {
        gclient_t cl = ent.client;

        if (cl.pers.inventory[cl.pers.selected_item] != 0)
            return; // valid

        GameItems.SelectNextItem(ent, -1);
    }
}