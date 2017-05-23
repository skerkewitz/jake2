package jake2.io;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Hashtable;

/**
 * Created by tropper on 22.05.17.
 */
public class TPack {
    public String filename;

    public RandomAccessFile handle;

    public ByteBuffer backbuffer;

    public int numfiles;

    public Hashtable files; // with packfile_t entries
}
