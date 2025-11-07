/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.filters;

import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.filters.gui.*;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.Geometry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class Penrose extends ParametrizedFilter {
    public static final String NAME = "Penrose Tiling";

    private static final int START_SUN = 0;
    private static final int START_STAR = 1;
    private static final int START_ACE = 2;
    private static final int START_KING = 3;
    private static final int START_QUEEN = 4;
    private static final int START_JACK = 5;
    private static final int START_DEUCE = 6;

    private final IntChoiceParam startType = new IntChoiceParam("Starting Pattern",
        new IntChoiceParam.Item[]{
            new IntChoiceParam.Item("Sun", START_SUN),
            new IntChoiceParam.Item("Star", START_STAR),
            new IntChoiceParam.Item("Ace", START_ACE),
            new IntChoiceParam.Item("King", START_KING),
            new IntChoiceParam.Item("Queen", START_QUEEN),
            new IntChoiceParam.Item("Jack", START_JACK),
            new IntChoiceParam.Item("Deuce", START_DEUCE),
        });

    private final RangeParam iterations = new RangeParam("Iterations", 0, 0, 7);

    private static final int KITE_COLOR_INDEX = 0;
    private static final int DART_COLOR_INDEX = 1;
    private static final int EDGE_COLOR_INDEX = 2;
    private final GroupedColorsParam colors = new GroupedColorsParam("Colors",
        new String[]{"Kite", "Dart", "Edge"},
        new Color[]{
            new Color(143, 255, 115),
            new Color(252, 255, 75),
            Color.BLACK},
        TransparencyMode.OPAQUE_ONLY, false, false);

    private final RangeParam edgeWidth = new RangeParam("Edge Width", 0, 1, 5);
    private final RangeParam zoom = new RangeParam("Zoom", 10, 100, 200);

    // a single tile in the Penrose P2 pattern
    static final class Tile {
        final double x;
        final double y;
        final double angle;
        final double size;
        final Type type;
        private static final double DIST_EPSILON = 0.01;
        private static final double ANGLE_EPSILON = 0.1;

        Tile(Type t, double x, double y, double angle, double size) {
            type = t;
            this.x = x;
            this.y = y;
            this.angle = normalizeAngle(angle); // so that equals works
            this.size = size;
        }

        /**
         * Generates the geometric path for this tile.
         */
        public Path2D createPath() {
            return type.createPath(x, y, angle, size);
        }

        // Normalizes an angle to the range [0, 2 * PI)
        private static double normalizeAngle(double angle) {
            double twoPi = 2 * PI;
            angle = angle % twoPi;
            if (angle < 0) {
                angle += twoPi;
            }
            return angle;
        }

        // Override equals for tile comparison (used in duplicate removal)
        @Override
        public boolean equals(Object o) {
            Tile that = (Tile) o;
            return type == that.type
                && abs(x - that.x) < DIST_EPSILON
                && abs(y - that.y) < DIST_EPSILON
                && abs(angle - that.angle) < ANGLE_EPSILON;
        }

        @Override
        public int hashCode() {
            // Must be consistent with equals! Groups values that
            // are close enough under the same hash code.
            long xBits = Double.doubleToLongBits(Math.round(x / DIST_EPSILON) * DIST_EPSILON);
            long yBits = Double.doubleToLongBits(Math.round(y / DIST_EPSILON) * DIST_EPSILON);
            long angleBits = Double.doubleToLongBits(Math.round(angle / ANGLE_EPSILON) * ANGLE_EPSILON);

            return Objects.hash(type, xBits, yBits, angleBits);
        }

        @Override
        public String toString() {
            return String.format(
                "[Tile, type = %s, x = %.2f, y = %.2f, angle = %.2f]",
                type, x, y, angle);
        }
    }

    // the two types of tiles in a Penrose P2 pattern
    enum Type {
        Kite(G, G, G),
        Dart(-G, -1, -G);

        private final double[] dist;

        Type(double... dist) {
            this.dist = dist;
        }

        /**
         * Generates the geometric path for this tile type.
         */
        public Path2D createPath(double x, double y, double angle, double size) {
            Path2D path = new Path2D.Double();
            path.moveTo(x, y);

            double currentAngle = angle - T;

            for (double d : dist) {
                double px = x + d * size * cos(currentAngle);
                double py = y - d * size * sin(currentAngle);
                path.lineTo(px, py);
                currentAngle += T;
            }
            path.closePath();
            return path;
        }
    }

    private static final double G = Geometry.GOLDEN_RATIO;
    private static final double T = toRadians(36); // theta = 36°

    public Penrose() {
        super(false);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Penrose_tiling");

        initParams(
            startType,
            iterations,
            colors,
            edgeWidth,
            zoom
        ).withAction(FilterButtonModel.createExportSvg(this::exportSVG));
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Graphics2D g = dest.createGraphics();

//        g.setColor(Color.WHITE);
//        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // this can be used to check that duplicate tiles are removed
        // g.setComposite(AlphaComposite.SrcOver.derive(0.5f));

        List<ShapeWithColor> shapes = createShapes(src.getWidth(), src.getHeight());
        for (ShapeWithColor shapeWithColor : shapes) {
            shapeWithColor.fill(g);
        }

        double currentEdgeWidth = edgeWidth.getValue();
        if (currentEdgeWidth > 0) {
            double scale = zoom.getPercentage();
            g.setStroke(new BasicStroke((float) (currentEdgeWidth * scale)));
            g.setColor(colors.getColor(EDGE_COLOR_INDEX));
            for (ShapeWithColor shapeWithColor : shapes) {
                g.draw(shapeWithColor.shape());
            }
        }

        g.dispose();
        return dest;
    }

    /**
     * Creates a list of shapes with their associated colors.
     * Used both for drawing and SVG export.
     */
    private List<ShapeWithColor> createShapes(int width, int height) {
        double scale = zoom.getPercentage();

        Set<Tile> tiles = new HashSet<>(createPrototiles(width, height, scale));
        tiles = deflateTiles(tiles, iterations.getValue());

        Color kiteColor = colors.getColor(KITE_COLOR_INDEX);
        Color dartColor = colors.getColor(DART_COLOR_INDEX);

        List<ShapeWithColor> shapes = new ArrayList<>();

        for (Tile tile : tiles) {
            Path2D tileShape = tile.createPath();

            Color color = (tile.type == Type.Dart) ? dartColor : kiteColor;
            shapes.add(new ShapeWithColor(tileShape, color));
        }

        return shapes;
    }

    private List<Tile> createPrototiles(int width, int height, double scale) {
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double baseSize = scale * Math.min(width, height) / 2.5;

        return switch (startType.getValue()) {
            case START_SUN -> createSunPattern(centerX, centerY, baseSize);
            case START_STAR -> createStarPattern(centerX, centerY, baseSize);
            case START_ACE -> createAcePattern(centerX, centerY, baseSize);
            case START_KING -> createKingPattern(centerX, centerY, baseSize);
            case START_QUEEN -> createQueenPattern(centerX, centerY, baseSize);
            case START_JACK -> createJackPattern(centerX, centerY, baseSize);
            case START_DEUCE -> createDeucePattern(centerX, centerY, baseSize);
            default -> throw new IllegalStateException();
        };
    }

    private static List<Tile> createSunPattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();
        for (double angle = PI / 2 + T; angle < 2.5 * PI; angle += 2 * T) {
            proto.add(new Tile(Type.Kite, centerX, centerY, angle, baseSize));
        }
        return proto;
    }

    private static List<Tile> createStarPattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();
        for (double angle = PI / 2; angle < 2.5 * PI; angle += 2 * T) {
            proto.add(new Tile(Type.Dart, centerX, centerY, angle, baseSize));
        }
        return proto;
    }

    private static List<Tile> createAcePattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();

        // two kites at the bottom
        double kiteStartY = centerY + baseSize * G;
        proto.add(new Tile(Type.Kite, centerX, kiteStartY, PI / 2 + T, baseSize));
        proto.add(new Tile(Type.Kite, centerX, kiteStartY, PI / 2 - T, baseSize));

        // one dart at the top
        double dartStartY = centerY - baseSize;
        proto.add(new Tile(Type.Dart, centerX, dartStartY, PI / 2, baseSize));

        return proto;
    }

    private static List<Tile> createKingPattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();

        // two kites at the bottom
        double kiteStartY = centerY + baseSize / 2;
        double kiteXOffset = baseSize * G * cos(T / 2);
        proto.add(new Tile(Type.Kite, centerX + kiteXOffset, kiteStartY, PI + T / 2, baseSize));
        proto.add(new Tile(Type.Kite, centerX - kiteXOffset, kiteStartY, -T / 2, baseSize));

        // three darts at the top
        proto.add(new Tile(Type.Dart, centerX, centerY, -T / 2, baseSize));
        proto.add(new Tile(Type.Dart, centerX, centerY, -T / 2 - 2 * T, baseSize));
        proto.add(new Tile(Type.Dart, centerX, centerY, -T / 2 - 4 * T, baseSize));

        return proto;
    }

    private static List<Tile> createQueenPattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();

        // single dart at the top
        proto.add(new Tile(Type.Dart, centerX, centerY, PI * 1.5, baseSize));

        // two kites at the top
        double topKiteStartY = centerY - baseSize * G * cos(T);
        double topKiteXOffset = baseSize * cos(T / 2);
        proto.add(new Tile(Type.Kite, centerX - topKiteXOffset, topKiteStartY, PI * 1.5, baseSize));
        proto.add(new Tile(Type.Kite, centerX + topKiteXOffset, topKiteStartY, PI * 1.5, baseSize));

        // two kites at the bottom
        double boottomKiteStartY = centerY + baseSize * G;
        proto.add(new Tile(Type.Kite, centerX, boottomKiteStartY, 1.5 * T, baseSize));
        proto.add(new Tile(Type.Kite, centerX, boottomKiteStartY, 3.5 * T, baseSize));

        return proto;
    }

    private static List<Tile> createJackPattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();

        // two kites at the bottom
        proto.add(new Tile(Type.Kite, centerX, centerY, 6.5 * T, baseSize));
        proto.add(new Tile(Type.Kite, centerX, centerY, 8.5 * T, baseSize));

        // top kite
        double topKiteY = centerY - baseSize * G;
        proto.add(new Tile(Type.Kite, centerX, topKiteY, 7.5 * T, baseSize));

        // middle darts
        double xOffset = baseSize * G * cos(T / 2);
        double dartY = centerY + baseSize * G * sin(T / 2);
        proto.add(new Tile(Type.Dart, centerX - xOffset, dartY, 6.5 * T, baseSize));
        proto.add(new Tile(Type.Dart, centerX + xOffset, dartY, 8.5 * T, baseSize));

        return proto;
    }

    private static List<Tile> createDeucePattern(double centerX, double centerY, double baseSize) {
        List<Tile> proto = new ArrayList<>();

        // two darts at the top
        double dartY = centerY - baseSize * G;
        proto.add(new Tile(Type.Dart, centerX, dartY, PI / 2 + T, baseSize));
        proto.add(new Tile(Type.Dart, centerX, dartY, PI / 2 - T, baseSize));

        // two kites at the bottom
        double kiteY = centerY + baseSize * 0.5;
        double kiteXoffset = baseSize * G * cos(T / 2);
        proto.add(new Tile(Type.Kite, centerX - kiteXoffset, kiteY, PI / 2 - 2 * T, baseSize));
        proto.add(new Tile(Type.Kite, centerX + kiteXoffset, kiteY, PI / 2 + 2 * T, baseSize));

        return proto;
    }

    /**
     * Recursively subdivides tiles using Penrose deflation rules.
     */
    private static Set<Tile> deflateTiles(Set<Tile> tiles, int generation) {
        // base case: return tiles when generation count reaches 0
        if (generation <= 0) {
            return tiles;
        }

        Set<Tile> next = new HashSet<>();

        // process each tile according to deflation rules
        for (Tile tile : tiles) {
            double x = tile.x, y = tile.y, a = tile.angle;
            // new tiles are smaller by golden ratio
            double size = tile.size / G;

            if (tile.type == Type.Dart) {
                // deflation rules for dart tiles
                next.add(new Tile(Type.Kite, x, y, a + 5 * T, size));

                for (int i = 0, sign = 1; i < 2; i++, sign *= -1) {
                    double nx = x + cos(a - 4 * T * sign) * G * tile.size;
                    double ny = y - sin(a - 4 * T * sign) * G * tile.size;
                    next.add(new Tile(Type.Dart, nx, ny, a - 4 * T * sign, size));
                }
            } else {
                // deflation rules for kite tiles
                for (int i = 0, sign = 1; i < 2; i++, sign *= -1) {
                    next.add(new Tile(Type.Dart, x, y, a - 4 * T * sign, size));

                    double nx = x + cos(a - T * sign) * G * tile.size;
                    double ny = y - sin(a - T * sign) * G * tile.size;
                    next.add(new Tile(Type.Kite, nx, ny, a + 3 * T * sign, size));
                }
            }
        }

        // continue deflation with the new set of tiles
        //noinspection TailRecursion
        return deflateTiles(next, generation - 1);
    }

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        List<ShapeWithColor> shapes = createShapes(canvas.getWidth(), canvas.getHeight());
        double strokeWidth = edgeWidth.getValue() * zoom.getPercentage();
        Color strokeColor = colors.getColor(EDGE_COLOR_INDEX);

        String svgContent = ShapeWithColor.createSvgContent(shapes, canvas, null, strokeWidth, strokeColor);
        FileIO.saveSVG(svgContent, this);
    }
}
