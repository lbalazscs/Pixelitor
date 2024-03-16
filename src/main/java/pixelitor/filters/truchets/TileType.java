package pixelitor.filters.truchets;

import pixelitor.gui.utils.VectorIcon;
import pixelitor.tools.Tool;
import pixelitor.tools.gui.ToolButton;
import pixelitor.utils.Utils;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.*;

public enum TileType {
    TRIANGLE(4, false, false, (tileSize, lineWidth) -> {
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
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);

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

    WELL(1, true, true, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();

        path.moveTo(tileSize / 2d, tileSize);
        path.lineTo(3 * tileSize / 4d, tileSize);

        path.moveTo(tileSize, tileSize / 2d);
        path.lineTo(tileSize, 3 * tileSize / 4d);

        path.append(new Ellipse2D.Double(3 * tileSize / 4d, 3 * tileSize / 4d, tileSize / 2d, tileSize / 2d), false);

        path.moveTo(3 * tileSize / 2d, tileSize);
        path.lineTo(5 * tileSize / 4d, tileSize);

        path.moveTo(tileSize, 3 * tileSize / 2d);
        path.lineTo(tileSize, 5 * tileSize / 4d);

        return new BasicStroke(lineWidth).createStrokedShape(path);
    }),
    JUMP(4, false, true, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();

        path.moveTo(tileSize, tileSize / 2d);
        path.lineTo(tileSize, 3 * tileSize / 4d);

        path.append(new Ellipse2D.Double(3 * tileSize / 4d, 3 * tileSize / 4d, tileSize / 2d, tileSize / 2d), false);

        Rectangle2D jump = new Rectangle2D.Double(
            tileSize / 2d, tileSize / 2d, tileSize, tileSize);
        path.append(new Arc2D.Double(jump, 180, 180, Arc2D.OPEN), false);

        return new BasicStroke(lineWidth).createStrokedShape(path);
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

    PLUS(1, true, true, (tileSize, lineWidth) -> {
        Path2D lines = new Path2D.Double();
        lines.moveTo(tileSize, tileSize / 2d);
        lines.lineTo(tileSize, 3 * tileSize / 2d);
        lines.moveTo(tileSize / 2d, tileSize);
        lines.lineTo(3 * tileSize / 2d, tileSize);
        return new BasicStroke(lineWidth).createStrokedShape(lines);
    }),

    DIAGONAL(2, false, false, (tileSize, lineWidth) -> {
//        Line2D line = new Line2D.Double(3 * tileSize / 2d, tileSize / 2d, tileSize / 2d, 3 * tileSize / 2d);
        Line2D line = new Line2D.Double(3 * tileSize / 2d, 3 * tileSize / 2d, tileSize / 2d, tileSize / 2d);
        BasicStroke stroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        return stroke.createStrokedShape(line);
    }),
    PARALLEL(2, true, true, (tileSize, lineWidth) -> {
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
    CORNER(2, false, false, (tileSize, lineWidth) -> {
        Path2D lines = new Path2D.Double();
        lines.moveTo(tileSize / 2d, tileSize);
        lines.lineTo(3d * tileSize / 4d, tileSize);
        lines.lineTo(tileSize, 3d * tileSize / 4d);
        lines.lineTo(tileSize, tileSize / 2d);
        lines.moveTo(3d * tileSize / 2d, tileSize);
        lines.lineTo(5d * tileSize / 4d, tileSize);
        lines.lineTo(tileSize, 5d * tileSize / 4d);
        lines.lineTo(tileSize, 3d * tileSize / 2d);
        return new BasicStroke(lineWidth).createStrokedShape(lines);
    }),
    FLAT_JOIN(2, false, false, (tileSize, lineWidth) -> {
        Path2D lines = new Path2D.Double();
        lines.moveTo(tileSize / 2d, tileSize);
        lines.lineTo(tileSize, tileSize / 2d);
        lines.moveTo(3d * tileSize / 2d, tileSize);
        lines.lineTo(tileSize, 3d * tileSize / 2d);
        return new BasicStroke(lineWidth).createStrokedShape(lines);
    }),
    DIVIDE(2, true, true, (tileSize, lineWidth) -> {
        Path2D lines = new Path2D.Double();

//        lines.moveTo(tileSize, tileSize / 2d);
//        // TODO: Does lineWidth scale not match with tileSize? For the moment 1 has been applied as a rough fix
////        lines.lineTo(tileSize, tileSize / 2d + lineWidth);
//        lines.lineTo(tileSize, tileSize / 2d + 1);

        lines.moveTo(tileSize / 2d, tileSize);
        lines.lineTo(3 * tileSize / 2d, tileSize);

//        lines.moveTo(tileSize, 3 * tileSize / 2d);
//        lines.lineTo(tileSize, 3 * tileSize / 2d - 1);

        return new BasicStroke(lineWidth).createStrokedShape(lines);
    }),
    THREE_WAY(4, false, true, (tileSize, lineWidth) -> {
        Path2D lines = new Path2D.Double();

//        lines.moveTo(tileSize, tileSize / 2d);
//        lines.lineTo(tileSize, tileSize / 2d + 1);

        lines.moveTo(tileSize / 2d, tileSize);
        lines.lineTo(3 * tileSize / 2d, tileSize);

        lines.moveTo(tileSize, tileSize);
        lines.lineTo(tileSize, 3 * tileSize / 2d);

        return new BasicStroke(lineWidth).createStrokedShape(lines);
    }),
    FILLED_QUARTERS(2, false, false, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();

        Rectangle2D bounds1 = new Rectangle2D.Double(tileSize, 0, tileSize, tileSize);
        Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.PIE);
        path.append(arc1, false);

        Rectangle2D bounds2 = new Rectangle2D.Double(0, tileSize, tileSize, tileSize);
        Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.PIE);
        path.append(arc2, false);

        return path;
    }),
    CORNER_BOID(2, false, false, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();

        Rectangle2D bounds1 = new Rectangle2D.Double(tileSize, 0, tileSize, tileSize);
        Arc2D arc1 = new Arc2D.Double(bounds1, 180, 90, Arc2D.OPEN);
        path.append(arc1, false);

        path.lineTo(3d * tileSize / 2d, 3d * tileSize / 2d);

        Rectangle2D bounds2 = new Rectangle2D.Double(0, tileSize, tileSize, tileSize);
        Arc2D arc2 = new Arc2D.Double(bounds2, 0, 90, Arc2D.OPEN);
        path.append(arc2, true);

        path.lineTo(tileSize / 2d, tileSize / 2d);
        path.closePath();

        return path;
    }),
    THREE_STRINGS(1, false, false, (tileSize, lineWidth) -> {
        Path2D path = new Path2D.Double();

        double spacing = 0.3333 * tileSize;

        int a = (int) (3 * Math.random()) + 3;
        int b = (Math.random() < .5 ? a - 1 : a + 1) % 3;
        a %= 3;
        int c = 3 - a - b;

        path.moveTo(tileSize - spacing, tileSize / 2d);
        path.curveTo(
            tileSize - spacing, 1.666 * tileSize / 2d,
            tileSize - spacing + a * spacing, 2.333 * tileSize / 2d,
            tileSize - spacing + a * spacing, 3 * tileSize / 2d
        );

        path.moveTo(tileSize, tileSize / 2d);
        path.curveTo(
            tileSize, 1.666 * tileSize / 2d,
            tileSize - spacing + b * spacing, 2.333 * tileSize / 2d,
            tileSize - spacing + b * spacing, 3 * tileSize / 2d
        );

        path.moveTo(tileSize + spacing, tileSize / 2d);
        path.curveTo(
            tileSize + spacing, 1.666 * tileSize / 2d,
            tileSize - spacing + c * spacing, 2.333 * tileSize / 2d,
            tileSize - spacing + c * spacing, 3 * tileSize / 2d
        );

        return new BasicStroke(lineWidth).createStrokedShape(path);
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
        g.translate(-tileSize / 2d, -tileSize / 2d);
        g.fill(drawAction.create(tileSize, lineWidth));
        g.translate(tileSize / 2d, tileSize / 2d);
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
                draw(g, ToolButton.TOOL_ICON_SIZE - 8, 5);
                g.translate(-4, -4);
            }
        };
    }

    public interface DrawAction {
        Shape create(int tileSize, int lineWidth);
    }
}
