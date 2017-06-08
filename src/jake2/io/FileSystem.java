/*
 * FS.java
 * Copyright (C) 2003
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
package jake2.io;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.Cmd;
import jake2.game.TVar;
import jake2.qcommon.Cbuf;
import jake2.qcommon.Command;
import jake2.qcommon.ConsoleVar;
import jake2.qcommon.Qcommon;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * QUAKE FILESYSTEM
 *
 * @author cwei
 */
public final class FileSystem {

    private static String gameDir;
    private static int fileindex;
    private static String findbase;
    private static String findpattern;
    private static File[] fdir;

    private static String fs_userdir;

    private static TVar varBaseDir;

    private static TVar varCdDir;

    public static TVar varGameDir;

    //ok!
    private static File[] findAll(String path, int musthave, int canthave) {

        int index = path.lastIndexOf('/');

        if (index != -1) {
            findbase = path.substring(0, index);
            findpattern = path.substring(index + 1, path.length());
        } else {
            findbase = path;
            findpattern = "*";
        }

        if (findpattern.equals("*.*")) {
            findpattern = "*";
        }

        File fdir = new File(findbase);

        if (!fdir.exists())
            return null;

        FilenameFilter filter = new FileFilter(findpattern, musthave, canthave);

        return fdir.listFiles(filter);
    }

    // ok.
    public static File FindFirst(String path, int musthave, int canthave) {

        if (fdir != null) {
            Qcommon.Error("Sys_BeginFind without close");
        }

        //	COM_FilePath (path, findbase);

        fdir = findAll(path, canthave, musthave);
        fileindex = 0;

        if (fdir == null)
            return null;

        return findNext();
    }

    public static File findNext() {

        if (fileindex >= fdir.length)
            return null;

        return fdir[fileindex++];
    }

    public static void findClose() {
        fdir = null;
    }

    private static class TFileLink {
        String from;

        int fromlength;

        String to;
    }

    /**
     * with TFileLink entries
     */
    private static List<TFileLink> fileLinks = new LinkedList<>();

    private static class TSearchPath {
        String filename;

        TPack pack; // only one of filename or pack will be used

        TSearchPath next;
    }

    private static TSearchPath searchPath;

    /**
     * Without gamedirs
     */
    private static TSearchPath baseSearchPaths;

    /*
     * All of Quake'entityState data access is through a hierchal file system, but the
     * contents of the file system can be transparently merged from several
     * sources.
     * 
     * The "base directory" is the path to the directory holding the quake.exe
     * and all game directories. The sys_* files pass this to host_init in
     * quakeparms_t->basedir. This can be overridden with the "-basedir" command
     * line parm to allow code debugging in a different directory. The base
     * directory is only used during filesystem initialization.
     * 
     * The "game directory" is the first tree on the search path and directory
     * that all generated files (savegames, screenshots, demos, config files)
     * will be saved to. This can be overridden with the "-game" command line
     * parameter. The game directory can never be changed while quake is
     * executing. This is a precacution against having a malicious server
     * instruct clients to write files over areas they shouldn't.
     *  
     */

    /*
     * InitFilesystem
     */
    public FileSystem() {
        Cmd.AddCommand("path", FileSystem::funcPath);
        Cmd.AddCommand("link", () -> Link_f());
        Cmd.AddCommand("dir", () -> Dir_f());

        fs_userdir = java.lang.System.getProperty("user.home") + "/.jake2";
        FileSystem.CreatePath(fs_userdir + "/");
        FileSystem.AddGameDirectory(fs_userdir);

        //
        // basedir <path>
        // allows the game to run from outside the data tree
        //
        varBaseDir = ConsoleVar.Get("basedir", ".", TVar.CVAR_FLAG_NOSET);

        //
        // cddir <path>
        // Logically concatenates the cddir after the basedir for
        // allows the game to run from outside the data tree
        //

        setCDDir();

        //
        // start up with baseq2 by default
        //
        AddGameDirectory(varBaseDir.string + '/' + Context.BASEDIRNAME);

        // any set gamedirs will be freed up to here
        markBaseSearchPaths();

        // check for game override
        varGameDir = ConsoleVar.Get("game", "", TVar.CVAR_FLAG_LATCH | TVar.CVAR_FLAG_SERVERINFO);

        if (varGameDir.string.length() > 0)
            SetGamedir(varGameDir.string);
    }


    /*
     * CreatePath
     * 
     * Creates any directories needed to store the given filename.
     */
    public static void CreatePath(String path) {
        int index = path.lastIndexOf('/');
        // -1 if not found and 0 means write to root
        if (index > 0) {
            File f = new File(path.substring(0, index));
            if (!f.mkdirs() && !f.isDirectory()) {
                Command.Printf("can't create path \"" + path + '"' + "\n");
            }
        }
    }

    /*
     * FCloseFile
     * 
     * For some reason, other dll'entityState can't just call fclose() on files returned
     * by FS_FOpenFile...
     */
    public static void FCloseFile(RandomAccessFile file) throws IOException {
        file.close();
    }

    public static void FCloseFile(InputStream stream) throws IOException {
        stream.close();
    }

    public static int FileLength(String filename) {
        TSearchPath search;
        String netpath;
        TPack pak;
        TFileLink link;

        file_from_pak = 0;

        // check for links first
        for (Iterator it = fileLinks.iterator(); it.hasNext(); ) {
            link = (TFileLink) it.next();

            if (filename.regionMatches(0, link.from, 0, link.fromlength)) {
                netpath = link.to + filename.substring(link.fromlength);
                File file = new File(netpath);
                if (file.canRead()) {
                    Command.DPrintf("link file: " + netpath + '\n');
                    return (int) file.length();
                }
                return -1;
            }
        }

        // search through the path, one element at a time

        for (search = searchPath; search != null; search = search.next) {
            // is the element a pak file?
            if (search.pack != null) {
                // look through all the pak file elements
                pak = search.pack;
                filename = filename.toLowerCase();
                TPackfile entry = (TPackfile) pak.files.get(filename);

                if (entry != null) {
                    // found it!
                    file_from_pak = 1;
                    Command.DPrintf("PackFile: " + pak.filename + " : " + filename
                            + '\n');
                    // open a new file on the pakfile
                    File file = new File(pak.filename);
                    if (!file.canRead()) {
                        Command.Error(Defines.ERR_FATAL, "Couldn't reopen "
                                + pak.filename);
                    }
                    return entry.filelen;
                }
            } else {
                // check a file in the directory tree
                netpath = search.filename + '/' + filename;

                File file = new File(netpath);
                if (!file.canRead())
                    continue;

                Command.DPrintf("FindFile: " + netpath + '\n');

                return (int) file.length();
            }
        }
        Command.DPrintf("FindFile: can't find " + filename + '\n');
        return -1;
    }

    public static int file_from_pak = 0;

    /*
     * openfile
     * 
     * Finds the file in the search path. returns a RadomAccesFile. Used for
     * streaming data out of either a pak file or a seperate file.
     */
    public static RandomAccessFile openfile(String filename) throws IOException {

        file_from_pak = 0;

        // check for links first
        for (TFileLink link : fileLinks) {
            //			if (!strncmp (filename, link->from, link->fromlength))
            if (filename.regionMatches(0, link.from, 0, link.fromlength)) {
                String netpath = link.to + filename.substring(link.fromlength);
                File file = new File(netpath);
                if (file.canRead()) {
                    //Command.DPrintf ("link file: " + netpath +'\n');
                    return new RandomAccessFile(file, "r");
                }
                return null;
            }
        }

        //
        // search through the path, one element at a time
        //
        for (TSearchPath search = searchPath; search != null; search = search.next) {
            // is the element a pak file?
            if (search.pack != null) {
                // look through all the pak file elements
                TPack pak = search.pack;
                filename = filename.toLowerCase();
                TPackfile entry = (TPackfile) pak.files.get(filename);

                if (entry != null) {
                    // found it!
                    file_from_pak = 1;
                    //Command.DPrintf ("PackFile: " + pak.filename + " : " +
                    // filename + '\n');
                    File file = new File(pak.filename);
                    if (!file.canRead())
                        Command.Error(Defines.ERR_FATAL, "Couldn't reopen "
                                + pak.filename);
                    if (pak.handle == null || !pak.handle.getFD().valid()) {
                        // hold the pakfile handle open
                        pak.handle = new RandomAccessFile(pak.filename, "r");
                    }
                    // open a new file on the pakfile

                    RandomAccessFile raf = new RandomAccessFile(file, "r");
                    raf.seek(entry.filepos);

                    return raf;
                }
            } else {
                // check a file in the directory tree
                String netpath = search.filename + '/' + filename;

                File file = new File(netpath);
                if (!file.canRead())
                    continue;

                //Command.DPrintf("FindFile: " + netpath +'\n');

                return new RandomAccessFile(file, "r");
            }
        }
        //Command.DPrintf ("FindFile: can't find " + filename + '\n');
        return null;
    }

    // read in blocks of 64k
    public static final int MAX_READ = 0x10000;

    /**
     * Read
     * <p>
     * Properly handles partial reads
     */
    public static void Read(byte[] buffer, int len, RandomAccessFile f) {

        int offset = 0;
        int read = 0;
        // read in chunks for progress bar
        int remaining = len;
        int block;

        while (remaining != 0) {
            block = Math.min(remaining, MAX_READ);
            try {
                read = f.read(buffer, offset, block);
            } catch (IOException e) {
                Command.Error(Defines.ERR_FATAL, e.toString());
            }

            if (read == 0) {
                Command.Error(Defines.ERR_FATAL, "FS_Read: 0 bytes read");
            } else if (read == -1) {
                Command.Error(Defines.ERR_FATAL, "FS_Read: -1 bytes read");
            }
            //
            // do some progress bar thing here...
            //
            remaining -= read;
            offset += read;
        }
    }

    /**
     * Filename are relative to the quake search path a null buffer will just
     * return the file content as byte[]
     */
    public static byte[] loadFile(String path) {

        // TODO hack for bad strings (fuck \0)
        int index = path.indexOf('\0');
        if (index != -1)
            path = path.substring(0, index);

        // look for it in the filesystem or pack files
        int len = FileLength(path);
        if (len < 1) {
            return null;
        }

        byte[] buf = null;
        try {
            RandomAccessFile file = openfile(path);
            //Read(buf = new byte[len], len, h);
            buf = new byte[len];
            file.readFully(buf);
            file.close();
        } catch (IOException e) {
            Command.Error(Defines.ERR_FATAL, e.toString());
        }
        return buf;
    }

    /*
     * LoadMappedFile
     * 
     * Filename are reletive to the quake search path a null buffer will just
     * return the file content as ByteBuffer (memory mapped)
     */
    public static ByteBuffer LoadMappedFile(String filename) {
        TSearchPath search;
        String netpath;
        TPack pak;
        TFileLink link;
        File file = null;

        int fileLength = 0;
        FileChannel channel = null;
        FileInputStream input = null;
        ByteBuffer buffer = null;

        file_from_pak = 0;

        try {
            // check for links first
            for (Iterator it = fileLinks.iterator(); it.hasNext(); ) {
                link = (TFileLink) it.next();

                if (filename.regionMatches(0, link.from, 0, link.fromlength)) {
                    netpath = link.to + filename.substring(link.fromlength);
                    file = new File(netpath);
                    if (file.canRead()) {
                        input = new FileInputStream(file);
                        channel = input.getChannel();
                        fileLength = (int) channel.size();
                        buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0,
                                fileLength);
                        input.close();
                        return buffer;
                    }
                    return null;
                }
            }

            //
            // search through the path, one element at a time
            //
            for (search = searchPath; search != null; search = search.next) {
                // is the element a pak file?
                if (search.pack != null) {
                    // look through all the pak file elements
                    pak = search.pack;
                    filename = filename.toLowerCase();
                    TPackfile entry = (TPackfile) pak.files.get(filename);

                    if (entry != null) {
                        // found it!
                        file_from_pak = 1;
                        //Command.DPrintf ("PackFile: " + pak.filename + " : " +
                        // filename + '\n');
                        file = new File(pak.filename);
                        if (!file.canRead())
                            Command.Error(Defines.ERR_FATAL, "Couldn't reopen "
                                    + pak.filename);
                        if (pak.handle == null || !pak.handle.getFD().valid()) {
                            // hold the pakfile handle open
                            pak.handle = new RandomAccessFile(pak.filename, "r");
                        }
                        // open a new file on the pakfile
                        if (pak.backbuffer == null) {
                            channel = pak.handle.getChannel();
                            pak.backbuffer = channel.map(
                                    FileChannel.MapMode.READ_ONLY, 0,
                                    pak.handle.length());
                            channel.close();
                        }
                        pak.backbuffer.position(entry.filepos);
                        buffer = pak.backbuffer.slice();
                        buffer.limit(entry.filelen);
                        return buffer;
                    }
                } else {
                    // check a file in the directory tree
                    netpath = search.filename + '/' + filename;

                    file = new File(netpath);
                    if (!file.canRead())
                        continue;

                    //Command.DPrintf("FindFile: " + netpath +'\n');
                    input = new FileInputStream(file);
                    channel = input.getChannel();
                    fileLength = (int) channel.size();
                    buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0,
                            fileLength);
                    input.close();
                    return buffer;
                }
            }
        } catch (Exception e) {
        }
        try {
            if (input != null)
                input.close();
            else if (channel != null && channel.isOpen())
                channel.close();
        } catch (IOException ioe) {
        }
        return null;
    }

    /*
     * FreeFile
     */
    public static void FreeFile(byte[] buffer) {
        buffer = null;
    }

    static final int IDPAKHEADER = (('K' << 24) + ('C' << 16) + ('A' << 8) + 'P');

    static class dpackheader_t {
        int ident; // IDPAKHEADER

        int dirofs;

        int dirlen;
    }

    static final int MAX_FILES_IN_PACK = 4096;

    // buffer for C-Strings char[56]
    static byte[] tmpText = new byte[TPackfile.NAME_SIZE];

    /*
     * LoadPackFile
     * 
     * Takes an explicit (not game tree related) path to a pak file.
     * 
     * Loads the header and directory, adding the files at the beginning of the
     * list so they override previous pack files.
     */
    static TPack LoadPackFile(String packfile) {

        dpackheader_t header;
        Hashtable newfiles;
        RandomAccessFile file;
        int numpackfiles = 0;
        TPack pack = null;
        //		unsigned checksum;
        //
        try {
            file = new RandomAccessFile(packfile, "r");
            FileChannel fc = file.getChannel();
            ByteBuffer packhandle = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            packhandle.order(ByteOrder.LITTLE_ENDIAN);

            fc.close();

            if (packhandle == null || packhandle.limit() < 1)
                return null;
            //
            header = new dpackheader_t();
            header.ident = packhandle.getInt();
            header.dirofs = packhandle.getInt();
            header.dirlen = packhandle.getInt();

            if (header.ident != IDPAKHEADER)
                Command.Error(Defines.ERR_FATAL, packfile + " is not a packfile");

            numpackfiles = header.dirlen / TPackfile.SIZE;

            if (numpackfiles > MAX_FILES_IN_PACK)
                Command.Error(Defines.ERR_FATAL, packfile + " has " + numpackfiles
                        + " files");

            newfiles = new Hashtable(numpackfiles);

            packhandle.position(header.dirofs);

            // parse the directory
            TPackfile entry = null;

            for (int i = 0; i < numpackfiles; i++) {
                packhandle.get(tmpText);

                entry = new TPackfile();
                entry.name = new String(tmpText).trim();
                entry.filepos = packhandle.getInt();
                entry.filelen = packhandle.getInt();

                newfiles.put(entry.name.toLowerCase(), entry);
            }

        } catch (IOException e) {
            Command.DPrintf(e.getMessage() + '\n');
            return null;
        }

        pack = new TPack();
        pack.filename = new String(packfile);
        pack.handle = file;
        pack.numfiles = numpackfiles;
        pack.files = newfiles;

        Command.Printf("Added packfile " + packfile + " (" + numpackfiles
                + " files)\n");

        return pack;
    }

    /*
     * AddGameDirectory
     * 
     * Sets gameDir, adds the directory to the head of the path, then loads
     * and adds pak1.pak pak2.pak ...
     */
    static void AddGameDirectory(String dir) {
        int i;
        TSearchPath search;
        TPack pak;
        String pakfile;

        gameDir = new String(dir);

        //
        // add the directory to the search path
        // ensure fs_userdir is first in searchpath
        search = new TSearchPath();
        search.filename = new String(dir);
        if (searchPath != null) {
            search.next = searchPath.next;
            searchPath.next = search;
        } else {
            searchPath = search;
        }

        //
        // add any pak files in the format pak0.pak pak1.pak, ...
        //
        for (i = 0; i < 10; i++) {
            pakfile = dir + "/pak" + i + ".pak";
            if (!(new File(pakfile).canRead()))
                continue;

            pak = LoadPackFile(pakfile);
            if (pak == null)
                continue;

            search = new TSearchPath();
            search.pack = pak;
            search.filename = "";
            search.next = searchPath;
            searchPath = search;
        }
    }

    /*
     * gamedir
     * 
     * Called to find where to write a file (demos, savegames, etc)
     * this is modified to <user.home>/.jake2 
     */
    public static String gamedir() {
        return (fs_userdir != null) ? fs_userdir : Context.BASEDIRNAME;
    }

    /*
     * BaseGamedir
     * 
     * Called to find where to write a downloaded file
     */
    public static String BaseGamedir() {
        return (gameDir != null) ? gameDir : Context.BASEDIRNAME;
    }

    /*
     * ExecAutoexec
     */
    public static void ExecAutoexec() {
        String dir = fs_userdir;

        String name;
        if (dir != null && dir.length() > 0) {
            name = dir + "/autoexec.cfg";
        } else {
            name = varBaseDir.string + '/' + Context.BASEDIRNAME
                    + "/autoexec.cfg";
        }

        int canthave = Defines.SFF_SUBDIR | Defines.SFF_HIDDEN
                | Defines.SFF_SYSTEM;

        if (findAll(name, 0, canthave) != null) {
            Cbuf.AddText("exec autoexec.cfg\n");
        }
    }

    /*
     * SetGamedir
     * 
     * Sets the gamedir and path to a different directory.
     */
    public static void SetGamedir(String dir) {
        TSearchPath next;

        if (dir.indexOf("..") != -1 || dir.indexOf("/") != -1
                || dir.indexOf("\\") != -1 || dir.indexOf(":") != -1) {
            Command.Printf("gamedir should be a single filename, not a path\n");
            return;
        }

        //
        // free up any current game dir info
        //
        while (searchPath != baseSearchPaths) {
            if (searchPath.pack != null) {
                try {
                    searchPath.pack.handle.close();
                } catch (IOException e) {
                    Command.DPrintf(e.getMessage() + '\n');
                }
                // clear the hashtable
                searchPath.pack.files.clear();
                searchPath.pack.files = null;
                searchPath.pack = null;
            }
            next = searchPath.next;
            searchPath = null;
            searchPath = next;
        }

        //
        // flush all data, so it will be forced to reload
        //
        if ((Context.dedicated != null) && (Context.dedicated.value == 0.0f))
            Cbuf.AddText("vid_restart\nsnd_restart\n");

        gameDir = varBaseDir.string + '/' + dir;

        if (dir.equals(Context.BASEDIRNAME) || (dir.length() == 0)) {
            ConsoleVar.FullSet("gamedir", "", TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_NOSET);
            ConsoleVar.FullSet("game", "", TVar.CVAR_FLAG_LATCH | TVar.CVAR_FLAG_SERVERINFO);
        } else {
            ConsoleVar.FullSet("gamedir", dir, TVar.CVAR_FLAG_SERVERINFO | TVar.CVAR_FLAG_NOSET);
            if (varCdDir.string != null && varCdDir.string.length() > 0)
                AddGameDirectory(varCdDir.string + '/' + dir);

            AddGameDirectory(varBaseDir.string + '/' + dir);
        }
    }

    /*
     * Link_f
     * 
     * Creates a TFileLink
     */
    public static void Link_f() {

        if (Cmd.Argc() != 3) {
            Command.Printf("USAGE: link <from> <to>\n");
            return;
        }

        // see if the link already exists
        for (Iterator it = fileLinks.iterator(); it.hasNext(); ) {
            TFileLink entry = (TFileLink) it.next();

            if (entry.from.equals(Cmd.Argv(1))) {
                if (Cmd.Argv(2).length() < 1) {
                    // delete it
                    it.remove();
                    return;
                }
                entry.to = new String(Cmd.Argv(2));
                return;
            }
        }

        // create a new link if the <to> is not empty
        if (Cmd.Argv(2).length() > 0) {
            TFileLink entry = new TFileLink();
            entry.from = new String(Cmd.Argv(1));
            entry.fromlength = entry.from.length();
            entry.to = new String(Cmd.Argv(2));
            fileLinks.add(entry);
        }
    }

    /*
     * ListFiles
     */
    public static String[] ListFiles(String findname, int musthave, int canthave) {
        String[] list = null;

        File[] files = findAll(findname, musthave, canthave);

        if (files != null) {
            list = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                list[i] = files[i].getPath();
            }
        }

        return list;
    }

    /*
     * Dir_f
     */
    public static void Dir_f() {
        String path = null;
        String findname = null;
        String wildcard = "*.*";
        String[] dirnames;

        if (Cmd.Argc() != 1) {
            wildcard = Cmd.Argv(1);
        }

        while ((path = NextPath(path)) != null) {
            String tmp = findname;

            findname = path + '/' + wildcard;

            if (tmp != null)
                tmp.replaceAll("\\\\", "/");

            Command.Printf("Directory of " + findname + '\n');
            Command.Printf("----\n");

            dirnames = ListFiles(findname, 0, 0);

            if (dirnames != null) {
                int index = 0;
                for (int i = 0; i < dirnames.length; i++) {
                    if ((index = dirnames[i].lastIndexOf('/')) > 0) {
                        Command.Printf(dirnames[i].substring(index + 1, dirnames[i]
                                .length()) + '\n');
                    } else {
                        Command.Printf(dirnames[i] + '\n');
                    }
                }
            }

            Command.Printf("\n");
        }
    }

    private static void funcPath() {

        Command.Printf("Current search path:\n");

        for (TSearchPath s = searchPath; s != null; s = s.next) {
            if (s == baseSearchPaths)
                Command.Printf("----------\n");
            if (s.pack != null) {
                Command.Printf(s.pack.filename + " (" + s.pack.numfiles + " files)\n");
            } else {
                Command.Printf(s.filename + '\n');
            }
        }

        Command.Printf("\nLinks:\n");
        for (Object fs_link : fileLinks) {
            TFileLink link = (TFileLink) fs_link;
            Command.Printf(link.from + " : " + link.to + '\n');
        }
    }

    /*
     * NextPath
     * 
     * Allows enumerating all of the directories in the search path
     */
    public static String NextPath(String prevpath) {
        TSearchPath s;
        String prev;

        if (prevpath == null || prevpath.length() == 0)
            return gameDir;

        prev = gameDir;
        for (s = searchPath; s != null; s = s.next) {
            if (s.pack != null)
                continue;

            if (prevpath == prev)
                return s.filename;

            prev = s.filename;
        }

        return null;
    }

    /**
     * set baseq2 directory
     */
    public void setCDDir() {
        varCdDir = ConsoleVar.Get("cddir", "", TVar.CVAR_FLAG_ARCHIVE);
        if (varCdDir.string.length() > 0)
            AddGameDirectory(varCdDir.string);
    }

    public void markBaseSearchPaths() {
        // any set gamedirs will be freed up to here
        baseSearchPaths = searchPath;
    }

    //	RAFAEL
    /*
     * Developer_searchpath
     */
    public static int Developer_searchpath(int who) {

        // PMM - warning removal
        //	 char *start;
        TSearchPath s;

        for (s = searchPath; s != null; s = s.next) {
            if (s.filename.indexOf("xatrix") != -1)
                return 1;

            if (s.filename.indexOf("rogue") != -1)
                return 2;
        }

        return 0;
    }

    /**
     * Match the pattern findpattern against the filename.
     * <p>
     * In the pattern string, `*' matches any sequence of characters, `?'
     * matches any character, [SET] matches any character in the specified set,
     * [!SET] matches any character not in the specified set. A set is composed
     * of characters or ranges; a range looks like character hyphen character
     * (as in 0-9 or A-Z). [0-9a-zA-Z_] is the set of characters allowed in C
     * identifiers. Any other character in the pattern must be matched exactly.
     * To suppress the special syntactic significance of any of `[]*?!-\', and
     * match the character exactly, precede it with a `\'.
     */
    static class FileFilter implements FilenameFilter {

        String regexpr;

        int musthave, canthave;

        FileFilter(String findpattern, int musthave, int canthave) {
            this.regexpr = convert2regexpr(findpattern);
            this.musthave = musthave;
            this.canthave = canthave;

        }

        /*
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            if (name.matches(regexpr)) {
                return CompareAttributes(dir, musthave, canthave);
            }
            return false;
        }

        String convert2regexpr(String pattern) {

            StringBuffer sb = new StringBuffer();

            char c;
            boolean escape = false;

            String subst;

            // convert pattern
            for (int i = 0; i < pattern.length(); i++) {
                c = pattern.charAt(i);
                subst = null;
                switch (c) {
                    case '*':
                        subst = (!escape) ? ".*" : "*";
                        break;
                    case '.':
                        subst = (!escape) ? "\\." : ".";
                        break;
                    case '!':
                        subst = (!escape) ? "^" : "!";
                        break;
                    case '?':
                        subst = (!escape) ? "." : "?";
                        break;
                    case '\\':
                        escape = !escape;
                        break;
                    default:
                        escape = false;
                }
                if (subst != null) {
                    sb.append(subst);
                    escape = false;
                } else
                    sb.append(c);
            }

            // the converted pattern
            String regexpr = sb.toString();

            //Command.DPrintf("pattern: " + pattern + " regexpr: " + regexpr +
            // '\n');
            try {
                Pattern.compile(regexpr);
            } catch (PatternSyntaxException e) {
                Command.Printf("invalid file pattern ( *.* is used instead )\n");
                return ".*"; // the default
            }
            return regexpr;
        }

        boolean CompareAttributes(File dir, int musthave, int canthave) {
            // . and .. never match
            String name = dir.getName();

            return !(name.equals(".") || name.equals(".."));
        }

    }
}