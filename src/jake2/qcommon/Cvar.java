/*
 * Cvar.java
 * Copyright (C) 2003
 * 
 * $Id: Cvar.java,v 1.10 2007-05-14 22:29:30 cawe Exp $
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

import jake2.Globals;
import jake2.game.Cmd;
import jake2.game.Info;
import jake2.game.TVar;
import jake2.util.Lib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * Cvar implements console variables. The original code is located in cvar.c
 */
public class Cvar {

    public static List<TVar> cvar_vars = new LinkedList<>();

    /**
     * @param name
     * @param value
     * @param flags
     * @return
     */
    public static TVar Get(String name, String value, int flags) {

        if ((flags & (TVar.CVAR_FLAG_USERINFO | TVar.CVAR_FLAG_SERVERINFO)) != 0) {
            if (!InfoValidate(name)) {
                Com.Printf("invalid info cvar name\n");
                return null;
            }
        }

        TVar var = Cvar.FindVar(name);
        if (var != null) {
            var.flags |= flags;
            return var;
        }

        if (value == null)
            return null;

        if ((flags & (TVar.CVAR_FLAG_USERINFO | TVar.CVAR_FLAG_SERVERINFO)) != 0) {
            if (!InfoValidate(value)) {
                Com.Printf("invalid info cvar value\n");
                return null;
            }
        }
        var = new TVar();
        var.name = new String(name);
        var.string = new String(value);
        var.modified = true;
        var.value = Lib.atof(var.string);
        var.flags = flags;

        // link the variable in
        cvar_vars.add(var);

        return var;
    }

    static void Init() {
        Cmd.AddCommand("set", Set_f);
        Cmd.AddCommand("cvarlist", List_f);
    }

    public static String VariableString(String var_name) {
        TVar var;
        var = FindVar(var_name);
        return (var == null) ? "" : var.string;
    }

    static TVar FindVar(String var_name) {

        for (TVar var : cvar_vars) {
            if (var_name.equals(var.name))
                return var;
        }

        return null;
    }

    /**
     * Creates a variable if not found and sets their value, the parsed float value and their flags.
     */
    public static TVar FullSet(String var_name, String value, int flags) {
        TVar var;

        var = Cvar.FindVar(var_name);
        if (null == var) { // create it
            return Cvar.Get(var_name, value, flags);
        }

        var.modified = true;

        if ((var.flags & TVar.CVAR_FLAG_USERINFO) != 0)
            Globals.userinfo_modified = true; // transmit at next oportunity

        var.string = value;
        var.value = Lib.atof(var.string);
        var.flags = flags;

        return var;
    }

    /** 
     * Sets the value of the variable without forcing. 
     */
    public static TVar Set(String var_name, String value) {
        return Set2(var_name, value, false);
    }

    /** 
     * Sets the value of the variable with forcing. 
     */
    public static TVar ForceSet(String var_name, String value) {
        return Cvar.Set2(var_name, value, true);
    }
    
    /**
     * Gereric set function, sets the value of the variable, with forcing its even possible to 
     * override the variables write protection. 
     */
    static TVar Set2(String var_name, String value, boolean force) {

        TVar var = Cvar.FindVar(var_name);
        if (var == null) { 
        	// create it
            return Cvar.Get(var_name, value, 0);
        }

        if ((var.flags & (TVar.CVAR_FLAG_USERINFO | TVar.CVAR_FLAG_SERVERINFO)) != 0) {
            if (!InfoValidate(value)) {
                Com.Printf("invalid info cvar value\n");
                return var;
            }
        }

        if (!force) {
            if ((var.flags & TVar.CVAR_FLAG_NOSET) != 0) {
                Com.Printf(var_name + " is write protected.\n");
                return var;
            }

            if ((var.flags & TVar.CVAR_FLAG_LATCH) != 0) {
                if (var.latched_string != null) {
                    if (value.equals(var.latched_string))
                        return var;
                    var.latched_string = null;
                } else {
                    if (value.equals(var.string))
                        return var;
                }

                if (Globals.server_state != 0) {
                    Com.Printf(var_name + " will be changed for next game.\n");
                    var.latched_string = value;
                } else {
                    var.string = value;
                    var.value = Lib.atof(var.string);
                    if (var.name.equals("game")) {
                        FileSystem.SetGamedir(var.string);
                        FileSystem.ExecAutoexec();
                    }
                }
                return var;
            }
        } else {
            if (var.latched_string != null) {
                var.latched_string = null;
            }
        }

        if (value.equals(var.string))
            return var; // not changed

        var.modified = true;

        if ((var.flags & TVar.CVAR_FLAG_USERINFO) != 0)
            Globals.userinfo_modified = true; // transmit at next oportunity

        var.string = value;
        try {
            var.value = Float.parseFloat(var.string);
        } catch (Exception e) {
            var.value = 0.0f;
        }

        return var;
    }

    /** 
     * Set command, sets variables.
     */
    
    static xcommand_t Set_f = new xcommand_t() {
        public void execute() {
            int c;
            int flags;

            c = Cmd.Argc();
            if (c != 3 && c != 4) {
                Com.Printf("usage: set <variable> <value> [u / s]\n");
                return;
            }

            if (c == 4) {
                if (Cmd.Argv(3).equals("u"))
                    flags = TVar.CVAR_FLAG_USERINFO;
                else if (Cmd.Argv(3).equals("s"))
                    flags = TVar.CVAR_FLAG_SERVERINFO;
                else {
                    Com.Printf("flags can only be 'u' or 's'\n");
                    return;
                }
                Cvar.FullSet(Cmd.Argv(1), Cmd.Argv(2), flags);
            } else
                Cvar.Set(Cmd.Argv(1), Cmd.Argv(2));

        }

    };


    /**
     * List command, lists all available commands.
     */
    static xcommand_t List_f = new xcommand_t() {
        public void execute() {
            int i = 0;
            for (TVar var : cvar_vars) {
                if ((var.flags & TVar.CVAR_FLAG_ARCHIVE) != 0)
                    Com.Printf("*");
                else
                    Com.Printf(" ");
                if ((var.flags & TVar.CVAR_FLAG_USERINFO) != 0)
                    Com.Printf("U");
                else
                    Com.Printf(" ");
                if ((var.flags & TVar.CVAR_FLAG_SERVERINFO) != 0)
                    Com.Printf("Sound");
                else
                    Com.Printf(" ");
                if ((var.flags & TVar.CVAR_FLAG_NOSET) != 0)
                    Com.Printf("-");
                else if ((var.flags & TVar.CVAR_FLAG_LATCH) != 0)
                    Com.Printf("L");
                else
                    Com.Printf(" ");
                Com.Printf(" " + var.name + " \"" + var.string + "\"\n");
                i += 1;
            }
            Com.Printf(i + " cvars\n");
        }
    };



    /** 
     * Sets a float value of a variable.
     * 
     * The overloading is very important, there was a problem with 
     * networt "rate" string --> 10000 became "10000.0" and that wasn't right.
     */
    public static void SetValue(String var_name, int value) {
        Cvar.Set(var_name, "" + value);
    }

    public static void SetValue(String var_name, float value) {
        if (value == (int)value) {
            Cvar.Set(var_name, "" + (int)value);
        } else {
            Cvar.Set(var_name, "" + value);
        }
    }

    /**
     * Returns the float value of a variable.
     */
    public static float VariableValue(String var_name) {
        TVar var = Cvar.FindVar(var_name);
        if (var == null)
            return 0;
        
        return Lib.atof(var.string);
    }

    /**
     * Handles variable inspection and changing from the console.
     */
    public static boolean Command() {
        TVar v;

        // check variables
        v = Cvar.FindVar(Cmd.Argv(0));
        if (v == null)
            return false;

        // perform a variable print or set
        if (Cmd.Argc() == 1) {
            Com.Printf("\"" + v.name + "\" is \"" + v.string + "\"\n");
            return true;
        }

        Cvar.Set(v.name, Cmd.Argv(1));
        return true;
    }

    public static String BitInfo(int bit) {

        String info = "";
        for(TVar var : cvar_vars) {
            if ((var.flags & bit) != 0) {
                info = Info.Info_SetValueForKey(info, var.name, var.string);
            }
        }
        return info;
    }

    /**
     * Returns an info string containing all the CVAR_FLAG_SERVERINFO cvars.
     */
    public static String Serverinfo() {
        return BitInfo(TVar.CVAR_FLAG_SERVERINFO);
    }

    
    /**
     * Any variables with latched values will be updated.
     */
    public static void GetLatchedVars() {

        for(TVar var : cvar_vars) {
            if (var.latched_string == null || var.latched_string.length() == 0)
                continue;
            var.string = var.latched_string;
            var.latched_string = null;
            var.value = Lib.atof(var.string);
            if (var.name.equals("game")) {
                FileSystem.SetGamedir(var.string);
                FileSystem.ExecAutoexec();
            }
        }
    }

    /**
     * Returns an info string containing all the CVAR_FLAG_USERINFO cvars.
     */
    public static String Userinfo() {
        return BitInfo(TVar.CVAR_FLAG_USERINFO);
    }
    
    /**
     * Appends lines containing \"set vaqriable value\" for all variables
     * with the archive flag set true. 
     */

    public static void WriteVariables(String path) {

        RandomAccessFile f;
        String buffer;

        f = Lib.fopen(path, "rw");
        if (f == null)
            return;

        try {
            f.seek(f.length());
        } catch (IOException e1) {
            Lib.fclose(f);
            return;
        }
        for(TVar var : cvar_vars) {
            if ((var.flags & TVar.CVAR_FLAG_ARCHIVE) != 0) {
                buffer = "set " + var.name + " \"" + var.string + "\"\n";
                try {
                    f.writeBytes(buffer);
                } catch (IOException e) {
                }
            }
        }
        Lib.fclose(f);
    }

    /**
     * Variable typing auto completition.
     */
    public static Vector CompleteVariable(String partial) {

        Vector vars = new Vector();

        // check match
        for(TVar var : cvar_vars) {
            if (var.name.startsWith(partial)) {
                vars.add(var.name);
            }
        }

        return vars;
    }

    /**
     * Some characters are invalid for info strings.
     */
    static boolean InfoValidate(String s) {
        if (s.indexOf("\\") != -1)
            return false;
        if (s.indexOf("\"") != -1)
            return false;
        return s.indexOf(";") == -1;
    }
}