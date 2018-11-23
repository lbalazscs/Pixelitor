package pixelitor;

/**
 * Margin of the canvas
 */
public class CanvasMargins {
    private final double top;
    private final double right;
    private final double bottom;
    private final double left;

    public final double getTop() {
        return top;
    }
    public final double getRight() {
        return right;
    }
    public final double getBottom() {
        return bottom;
    }
    public final double getLeft() {
        return left;
    }

    public CanvasMargins(double top, double right, double bottom, double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }
}
