package pixelitor.guides;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;

/**
 * Renderer for guide lines (crop, guides).
 */
public class GuidesRenderer {
    private final GuideStyle guideStyle;

    public GuidesRenderer(GuideStyle guideStyle) {
        this.guideStyle = guideStyle;
    }

    public void draw(Graphics2D g2, List<Shape> shapes) {
        g2.setStroke(guideStyle.getStrokeA());
        g2.setColor(guideStyle.getColorA());
        for (Shape shape : shapes) {
            g2.draw(shape);
        }

        if (null != guideStyle.getStrokeB()) {
            g2.setStroke(guideStyle.getStrokeB());
            g2.setColor(guideStyle.getColorB());
            for (Shape shape : shapes) {
                g2.draw(shape);
            }
        }
    }
}
