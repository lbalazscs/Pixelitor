package pixelitor.filters.curves;

import java.awt.Color;

/**
 * Curve type enum
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public enum ToneCurveType {
    RGB("RGB", Color.BLACK),
    RED("Red", Color.RED),
    GREEN("Green", Color.GREEN),
    BLUE("Blue", Color.BLUE);

    public final String name;
    public final Color color;
    public final Color colorInactive;

    ToneCurveType(String name, Color color) {
        this.name = name;
        this.color = color;
        this.colorInactive = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }

    @Override
    public String toString() {
        return name;
    }
}