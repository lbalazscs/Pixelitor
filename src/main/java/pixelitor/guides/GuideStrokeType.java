package pixelitor.guides;

import java.awt.BasicStroke;
import java.awt.Stroke;

import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.JOIN_BEVEL;

/**
 * Stroke types for guides.
 */
public enum GuideStrokeType {
    SOLID(
        "Solid",
        new BasicStroke(1),
        null
    ),
    DOTTED(
        "Dotted",
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{1, 2}, 0),
        null
    ),
    DASHED(
        "Dashed",
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{5, 2}, 0),
        null
    ),
    DASHED_DOUBLE(
        "Dashed with background",
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{5.0f, 2.0f}, 0),
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{2.0f, 5.0f}, 2)
    ),
    DASHED_BORDERED(
        "Dashed with border",
        new BasicStroke(3),
        new BasicStroke(1, CAP_BUTT, JOIN_BEVEL, 0, new float[]{5, 2}, 0)
    );

    private final String guiName;
    private final Stroke strokeA;
    private final Stroke strokeB;

    GuideStrokeType(String guiName, Stroke strokeA, Stroke strokeB) {
        this.guiName = guiName;
        this.strokeA = strokeA;
        this.strokeB = strokeB;
    }

    public Stroke getStrokeA() {
        return strokeA;
    }

    public Stroke getStrokeB() {
        return strokeB;
    }

    @Override
    public String toString() {
        return guiName;
    }
}
