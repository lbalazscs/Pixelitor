package pixelitor.filters.truchets;

import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.Tool;
import pixelitor.tools.gui.ToolButton;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public enum TileType {
    TRIANGE(4, false, false, (tileSize, lineWidth) -> {
        Path2D triangle = new Path2D.Double();
        double firsts = Math.floor(tileSize / 2d);
        double thirds = Math.ceil(3 * tileSize / 2d);
        triangle.moveTo(thirds, firsts);
        triangle.lineTo(thirds, thirds);
        triangle.lineTo(firsts, thirds);
        triangle.closePath();
        return triangle;
    }),
    SQUARE(1, true, true, (tileSize, lineWidth) ->
        new Rectangle2D.Double(Math.floor(tileSize / 2d), Math.floor(tileSize / 2d), tileSize, tileSize)),
    BLANK(1, true, true, (tileSize, lineWidth) -> new Path2D.Double()),
    QUARTER_CIRCLE(2, false, false, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();
        BasicStroke stroke = new BasicStroke(lineWidth);

        Rectangle2D bounds1 = new Rectangle2D.Double(
            tileSize, 0, tileSize, tileSize);
        Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.OPEN);
        path.append(stroke.createStrokedShape(arc1), false);

        Rectangle2D bounds2 = new Rectangle2D.Double(
            0, tileSize, tileSize, tileSize);
        Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.OPEN);
        path.append(stroke.createStrokedShape(arc2), false);

        return path;
    }),
    PLUS(1, true, true, (tileSize, lineWidth) -> {
        Path2D lines = new Path2D.Double();
        lines.moveTo(tileSize, tileSize / 2d);
        lines.lineTo(tileSize, 3 * tileSize / 2d);
        lines.moveTo(tileSize / 2d, tileSize);
        lines.lineTo(3 * tileSize / 2d, tileSize);
        return new BasicStroke(lineWidth).createStrokedShape(lines);
    }),
    CIRCLE_CROSS(1, true, true, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();

        Rectangle2D bounds1 = new Rectangle2D.Double(
            tileSize, 0, tileSize, tileSize);
        Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.OPEN);
        path.append((arc1), false);

        Rectangle2D bounds2 = new Rectangle2D.Double(
            0, tileSize, tileSize, tileSize);
        Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.OPEN);
        path.append((arc2), false);

        Rectangle2D bounds3 = new Rectangle2D.Double(
            0, 0, tileSize, tileSize);
        Arc2D arc3 = new Arc2D.Double(bounds3, 270, 90, Arc2D.OPEN);
        path.append((arc3), false);

        Rectangle2D bounds4 = new Rectangle2D.Double(
            tileSize, tileSize, tileSize, tileSize);
        Arc2D arc4 = new Arc2D.Double(bounds4, 90, 90, Arc2D.OPEN);
        path.append((arc4), false);

        return new BasicStroke(lineWidth).createStrokedShape(path);
    }),
    DIAGONAL(2, false, false, (tileSize, lineWidth) -> {
        Line2D line = new Line2D.Double(tileSize / 2d, tileSize / 2d, 3 * tileSize / 2d, 3 * tileSize / 2d);
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        return stroke.createStrokedShape(line);
    }),
    LINE(2, true, true, (tileSize, lineWidth) -> {
        double firsts = Math.floor(tileSize / 2d);
        double thirds = Math.ceil(3 * tileSize / 2d);
        Path2D lines = new Path2D.Double();
        lines.moveTo(firsts, firsts);
        lines.lineTo(firsts, thirds);
        lines.moveTo(thirds, firsts);
        lines.lineTo(thirds, thirds);
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        return stroke.createStrokedShape(lines);
    }),
    LINE_DOWN(2, true, true, (tileSize, lineWidth) -> {
        double firsts = Math.floor(tileSize / 2d);
        double thirds = Math.ceil(3 * tileSize / 2d);
        Path2D lines = new Path2D.Double();
        lines.moveTo(firsts, firsts);
        lines.lineTo(thirds, firsts);
        lines.moveTo(firsts, thirds);
        lines.lineTo(thirds, thirds);
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        return stroke.createStrokedShape(lines);
    }),
    ;

    public final int rotationalDegree; // Number of 90deg rotations to get the original shape back
    public final boolean isSymmetricAboutHorizontal;
    public final boolean isSymmetricAboutVertical;
    public final DrawAction drawAction;

    TileType(int rotationalDegree, boolean isSymmetricAboutHorizontal, boolean isSymmetricAboutVertical, DrawAction drawAction) {
        this.rotationalDegree = rotationalDegree;
        this.isSymmetricAboutHorizontal = isSymmetricAboutHorizontal;
        this.isSymmetricAboutVertical = isSymmetricAboutVertical;
        this.drawAction = drawAction;
    }

    public Shape create(int tileSize, int lineWidth) {
        return drawAction.create(tileSize, lineWidth);
    }

    public void draw(Graphics2D g, int tileSize, int lineWidth) {
        g.translate(-tileSize / 2, -tileSize / 2);
        g.fill(drawAction.create(tileSize, lineWidth));
        g.translate(tileSize / 2, tileSize / 2);
    }

    public void draw(Graphics2D g, int tileSize, int lineWidth, int rotation, boolean flipAboutHorizontal, boolean flipAboutVertical) {
        if (flipAboutHorizontal) {
            g.scale(1, -1);
            g.translate(0, -tileSize);
        }
        if (flipAboutVertical) {
            g.scale(-1, 1);
            g.translate(-tileSize, 0);
        }
        if (rotation != 0) {
            g.rotate(rotation * Math.PI / 2, tileSize / 2d, tileSize / 2d);
        }
        draw(g, tileSize, lineWidth);
        if (rotation != 0) {
            g.rotate(-rotation * Math.PI / 2, tileSize / 2d, tileSize / 2d);
        }
        if (flipAboutHorizontal) {
            g.scale(1, -1);
            g.translate(0, -tileSize);
        }
        if (flipAboutVertical) {
            g.scale(-1, 1);
            g.translate(-tileSize, 0);
        }
    }

    @Override
    public String toString() {
        return Utils.screamingSnakeCaseToSentenceCase(super.toString());
    }

    public VectorIcon createIcon() {
        return new Tool.ToolIcon() {
            @Override
            protected void paintIcon(Graphics2D g) {
                g.translate(4, 4);
                draw(g, ToolButton.TOOL_ICON_SIZE-8, 5);
                g.translate(-4, -4);
            }
        };
    }

    public interface DrawAction {
        Shape create(int tileSize, int lineWidth);
    }
}
