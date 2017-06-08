package jake2.client.ui;

/**
 * Created by tropper on 08.06.17.
 */
public class TMenuCommon {
    public int type;

    public String name = "";

    public int x, y;

    TMenuFramework parent;

    int cursor_offset;

    int localdata[] = {0, 0, 0, 0};

    int flags;

    int n = -1; //position in an array.

    String statusbar;

    public TMCallback callback;

    TMCallback statusbarfunc;

    TMCallback ownerdraw;

    TMCallback cursordraw;
}
