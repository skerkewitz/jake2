package jake2.client.ui;

/**
 * Created by tropper on 08.06.17.
 */
public class TMenuFramework {
    public int x, y;

    public int cursor;

    public int nitems;

    int nslots;

    TMenuCommon items[] = new TMenuCommon[64];

    String statusbar;

    //void (*cursordraw)( struct _tag_menuframework *m );
    TMCallback cursordraw;

}
