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

import pixelitor.filters.gui.*;
import pixelitor.utils.Geometry;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import static java.util.stream.Collectors.toList;

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
            boolean equals = type == that.type
                && abs(x - that.x) < DIST_EPSILON
                && abs(y - that.y) < DIST_EPSILON
                && abs(angle - that.angle) < ANGLE_EPSILON;

            return equals;
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

    // The two types of tiles in a Penrose P2 pattern
    enum Type {
        Kite, Dart
    }

    private static final double G = Geometry.GOLDEN_RATIO;
    private static final double T = toRadians(36); // theta

    public Penrose() {
        super(false);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Penrose_tiling");

        initParams(
            startType,
            iterations,
            colors,
            edgeWidth,
            zoom
        );
    }

    @Override
    protected BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Graphics2D g = dest.createGraphics();

        g.setColor(Color.WHITE);
        g.drawRect(0, 0, src.getWidth(), src.getHeight());
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        // this can be used to check that duplicate tiles are removed
        // g.setComposite(AlphaComposite.SrcOver.derive(0.5f));

        double scale = zoom.getPercentage();

        List<Tile> tiles = createPrototiles(src.getWidth(), src.getHeight(), scale);

        tiles = deflateTiles(tiles, iterations.getValue());

        drawTiles(tiles, g, edgeWidth.getValue() * scale,
            colors.getColor(0), colors.getColor(1), colors.getColor(2));

        g.dispose();
        return dest;
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

    // recursively subdivides tiles using Penrose deflation rules
    private static List<Tile> deflateTiles(List<Tile> tiles, int generation) {
        // base case: return tiles when generation count reaches 0
        if (generation <= 0) {
            return tiles;
        }

        List<Tile> next = new ArrayList<>();

        // process each tile according to deflation rules
        for (Tile tile : tiles) {
            double x = tile.x, y = tile.y, a = tile.angle, nx, ny;
            // new tiles are smaller by golden ratio
            double size = tile.size / G;

            if (tile.type == Type.Dart) {
                // deflation rules for dart tiles
                next.add(new Tile(Type.Kite, x, y, a + 5 * T, size));

                for (int i = 0, sign = 1; i < 2; i++, sign *= -1) {
                    nx = x + cos(a - 4 * T * sign) * G * tile.size;
                    ny = y - sin(a - 4 * T * sign) * G * tile.size;
                    next.add(new Tile(Type.Dart, nx, ny, a - 4 * T * sign, size));
                }
            } else {
                // deflation rules for kite tiles
                for (int i = 0, sign = 1; i < 2; i++, sign *= -1) {
                    next.add(new Tile(Type.Dart, x, y, a - 4 * T * sign, size));

                    nx = x + cos(a - T * sign) * G * tile.size;
                    ny = y - sin(a - T * sign) * G * tile.size;
                    next.add(new Tile(Type.Kite, nx, ny, a + 3 * T * sign, size));
                }
            }
        }

        // remove duplicate tiles and continue deflation
        tiles = next.stream().distinct().collect(toList());
        return deflateTiles(tiles, generation - 1);
    }

    private static void drawTiles(List<Tile> tiles, Graphics2D g, double edgeWidth,
                                  Color kiteColor, Color dartColor, Color edgeColor) {
        // distance ratios for vertices of kite and dart shapes
        double[][] dist = {{G, G, G}, {-G, -1, -G}};

        for (Tile tile : tiles) {
            double angle = tile.angle - T;
            Path2D tileShape = new Path2D.Double();
            tileShape.moveTo(tile.x, tile.y);

            int ord = tile.type.ordinal();
            for (int i = 0; i < 3; i++) {
                double x = tile.x + dist[ord][i] * tile.size * cos(angle);
                double y = tile.y - dist[ord][i] * tile.size * sin(angle);
                tileShape.lineTo(x, y);
                angle += T;
            }
            tileShape.closePath();

            g.setColor(ord != 0 ? kiteColor : dartColor);
            g.fill(tileShape);

            if (edgeWidth > 0) {
                g.setStroke(new BasicStroke((float) edgeWidth));
                g.setColor(edgeColor);
                g.draw(tileShape);
            }
        }
    }
}
