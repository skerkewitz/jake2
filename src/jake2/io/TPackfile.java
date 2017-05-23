package jake2.io;

/**
 * Created by tropper on 22.05.17.
 */
public class TPackfile {
    static final int SIZE = 64;

    static final int NAME_SIZE = 56;

    public String name; // char name[56]

    public int filepos, filelen;

    public String toString() {
        return name + " [ length: " + filelen + " pos: " + filepos + " ]";
    }
}
