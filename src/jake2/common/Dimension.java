package jake2.common;

/**
 * Created by tropper on 19.05.17.
 */
public class Dimension {
    public int width;
    public int height;

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Dimension() {
        this(0,0);
    }
}
