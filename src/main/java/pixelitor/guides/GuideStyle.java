package pixelitor.guides;

import java.awt.Color;
import java.awt.Stroke;

/**
 * Guide style consisting of colors and stroke type.
 */
public final class GuideStyle {
    private GuideStrokeType strokeType = GuideStrokeType.DASHED;
    private Color colorA = Color.BLACK;
    private Color colorB = Color.WHITE;

    public GuideStrokeType getStrokeType() {
        return strokeType;
    }

    public void setStrokeType(GuideStrokeType strokeType) {
        this.strokeType = strokeType;
    }

    public Stroke getStrokeA() {
        return strokeType.getStrokeA();
    }

    public Stroke getStrokeB() {
        return strokeType.getStrokeB();
    }

    public Color getColorA() {
        return colorA;
    }

    public void setColorA(Color colorA) {
        this.colorA = colorA;
    }

    public Color getColorB() {
        return colorB;
    }

    public void setColorB(Color colorB) {
        this.colorB = colorB;
    }
}
