package pixelitor.filters.curves.lx;

import java.awt.*;

public enum LxCurveType {
    RGB("RGB", 0, Color.BLACK),
    RED("Red", 1, Color.RED),
    GREEN("Green", 2, Color.GREEN),
    BLUE("Blue", 3, Color.BLUE);

    public final String name;
    public final int index;
    public final Color color;
    public final Color colorInactive;

    LxCurveType(String name, int index, Color color) {
        this.name = name;
        this.index = index;
        this.color = color;
        this.colorInactive = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }

    @Override
    public String toString() {
        return name;
    }
}