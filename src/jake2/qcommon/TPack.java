package jake2.qcommon;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Hashtable;

/**
 * Created by tropper on 22.05.17.
 */
public class TPack {
    String filename;

    RandomAccessFile handle;

    ByteBuffer backbuffer;

    int numfiles;

    Hashtable files; // with packfile_t entries
}
