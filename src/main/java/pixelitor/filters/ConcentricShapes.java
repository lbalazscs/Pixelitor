/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.Colors;
import pixelitor.filters.gui.ColorListParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.Geometry;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

/**
 * Render Concentric Shapes filter
 */
public class ConcentricShapes extends ParametrizedFilter {
    public static final String NAME = "Concentric Shapes";

    @Serial
    private static final long serialVersionUID = 1L;

    enum ConcentricShapeType {
        CIRCLES("Circles") {
            @Override
            Shape createShape(double cx, double cy, double r) {
                return Shapes.createCircle(cx, cy, r);
            }
        }, SQUARES("Squares") {
            @Override
            Shape createShape(double cx, double cy, double r) {
                return Shapes.createSquare(cx, cy, r);
            }
        }, HEXAGONS("Hexagons") {
            @Override
            Shape createShape(double cx, double cy, double r) {
                return Shapes.createHexagon(cx, cy, r);
            }
        };

        private final String guiName;

        ConcentricShapeType(String guiName) {
            this.guiName = guiName;
        }

        abstract Shape createShape(double cx, double cy, double r);

        @Override
        public String toString() {
            return guiName;
        }
    }

    private final EnumParam<ConcentricShapeType> type = new EnumParam<>("Type", ConcentricShapeType.class);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam distanceParam = new RangeParam("Distance", 1, 20, 100);
    private final ColorListParam colorsParam = new ColorListParam("Colors",
        2, 2, WHITE, BLACK, Colors.CW_RED, Colors.CW_GREEN, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET);
//    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500);
//    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public ConcentricShapes() {
        super(false);

        setParams(
            type,
            center,
            distanceParam,
            colorsParam
//            scale
//            rotate
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        ConcentricShapeType shapeType = type.getSelected();
        double maxDist = calcMaxDistance(cx, cy, width, height);
        if (shapeType == ConcentricShapeType.HEXAGONS) {
            maxDist = maxDist * 1.3;
        }

        double distance = distanceParam.getValueAsDouble();
        int numRings = 1 + (int) (maxDist / distance);

        Color[] colors = colorsParam.getColors();
        int numColors = colors.length;
        for (int ring = numRings; ring > 0; ring--) {
            double r = ring * distance;
            g.setColor(colorsParam.getColor((ring - 1) % numColors));
            g.fill(shapeType.createShape(cx, cy, r));
        }

        g.dispose();
        return dest;
    }

    private static double calcMaxDistance(double cx, double cy, double width, double height) {
        double d = correctSqDistance(0, cx, cy, 0, 0);
        d = correctSqDistance(d, cx, cy, width, 0);
        d = correctSqDistance(d, cx, cy, 0, height);
        d = correctSqDistance(d, cx, cy, width, height);

        return Math.sqrt(d);
    }

    private static double correctSqDistance(double d, double x1, double y1, double x2, double y2) {
        double newDist = Geometry.calcSquaredDistance(x1, y1, x2, y2);
        return Math.max(newDist, d);
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}