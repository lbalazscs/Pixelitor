package pixelitor.tools.guidelines;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * Crop guidelines renderer
 */
public class RectGuideline {

    private Stroke outerStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5, 2}, 0);
    private Stroke innerStroke = new BasicStroke(3);
    private Graphics2D g2;

    public void draw(Rectangle2D rect, RectGuidelineType type, Graphics2D g2)
    {
        if (type == RectGuidelineType.NONE) {
            return;
        }

        this.g2 = g2;
        if (type == RectGuidelineType.RULE_OF_THIRDS) {
            drawRuleOfThirds(rect);
        } else if (type == RectGuidelineType.GOLDEN_SECTIONS) {
            drawGoldenSections(rect);
        } else if (type == RectGuidelineType.DIAGONALS) {
            drawDiagonals(rect);
        }
    }

    private void drawLines(Line2D.Double[] lines)
    {
        g2.setStroke(innerStroke);
        g2.setColor(BLACK);
        for (Line2D.Double line : lines) {
            g2.draw(line);
        }

        g2.setStroke(outerStroke);
        g2.setColor(WHITE);
        for (Line2D.Double line : lines) {
            g2.draw(line);
        }
    }

    private void drawSections(Rectangle2D rect, double phi)
    {
        double sectionWidth = rect.getWidth() / phi;
        double sectionHeight = rect.getHeight() / phi;
        double x1, x2, y1, y2;
        Line2D.Double[] lines = new Line2D.Double[4];

        // vertical lines
        x1 = (rect.getX() + sectionWidth);
        x2 = (rect.getX() + rect.getWidth() - sectionWidth);
        y1 = rect.getY();
        y2 = (rect.getY() + rect.getHeight());
        lines[0] = new Line2D.Double(x1, y1, x1, y2);
        lines[1] = new Line2D.Double(x2, y1, x2, y2);

        // horizontal lines
        x1 = rect.getX();
        x2 = (rect.getX() + rect.getWidth());
        y1 = (rect.getY() + sectionHeight);
        y2 = (rect.getY() + rect.getHeight() - sectionHeight);
        lines[2] = new Line2D.Double(x1, y1, x2, y1);
        lines[3] = new Line2D.Double(x1, y2, x2, y2);

        drawLines(lines);
    }

    private void drawRuleOfThirds(Rectangle2D rect)
    {
        drawSections(rect, 3);
    }

    private void drawGoldenSections(Rectangle2D rect)
    {
        drawSections(rect, 1.618);
    }

    private void drawDiagonals(Rectangle2D rect)
    {
        double x1, x2, y1, y2;
        Line2D.Double[] lines = new Line2D.Double[4];

        if (rect.getWidth() >= rect.getHeight()) {
            y1 = rect.getY();
            y2 = (rect.getY() + rect.getHeight());

            // from left
            x1 = rect.getX();
            x2 = (rect.getX() + rect.getHeight());
            lines[0] = new Line2D.Double(x1, y1, x2, y2);
            lines[1] = new Line2D.Double(x1, y2, x2, y1);

            // from right
            x1 = (rect.getX() + rect.getWidth());
            x2 = (rect.getX() + rect.getWidth() - rect.getHeight());
            lines[2] = new Line2D.Double(x1, y1, x2, y2);
            lines[3] = new Line2D.Double(x1, y2, x2, y1);
        } else {
            x1 = rect.getX();
            x2 = (rect.getX() + rect.getWidth());

            // from top
            y1 = rect.getY();
            y2 = (rect.getY() + rect.getWidth());
            lines[0] = new Line2D.Double(x1, y1, x2, y2);
            lines[1] = new Line2D.Double(x1, y2, x2, y1);

            // from bottom
            y1 = (rect.getY() + rect.getHeight());
            y2 = (rect.getY() + rect.getHeight() - rect.getWidth());
            lines[2] = new Line2D.Double(x1, y1, x2, y2);
            lines[3] = new Line2D.Double(x1, y2, x2, y1);
        }

        drawLines(lines);
    }
}
