/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import org.jdesktop.swingx.geom.Star2D;
import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.io.IO;
import pixelitor.utils.Geometry;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        CIRCLES("Circles", false, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n) {
                return Shapes.createCircle(cx, cy, r);
            }
        }, POLYGONS("Polygons", true, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n) {
                return Shapes.createCircumscribedPolygon(n, cx, cy, r);
            }
            // the number of rings multiplier corresponds to the max/min radius ratio
        }, STARS("Stars", true, 2.0, 2.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n) {
                return new Star2D(cx, cy, r / 2.0, r, n);
            }
        }, BATS("Bats", false, 2.0, 2.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n) {
                return Shapes.createBat(cx - r, cy - r, 2 * r, 2 * r);
            }
        }, HEARTS("Hearts", false, 1.0, 1.5) {
            @Override
            Shape createShape(double cx, double cy, double r, int n) {
                return Shapes.createHeart(cx - r, cy - r, 2 * r, 2 * r);
            }
        }, FLOWERS("Flowers", true, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n) {
                return Shapes.createFlower(n, cx, cy, r);
            }
        };

        private final String guiName;
        private final boolean hasSides;
        private final double distMultiplier;
        private final double ringsMultiplier;

        ConcentricShapeType(String guiName, boolean hasSides, double distMultiplier, double ringsMultiplier) {
            this.guiName = guiName;
            this.hasSides = hasSides;
            this.distMultiplier = distMultiplier;
            this.ringsMultiplier = ringsMultiplier;
        }

        abstract Shape createShape(double cx, double cy, double r, int n);

        public boolean hasSides() {
            return hasSides;
        }

        public double getDistMultiplier() {
            return distMultiplier;
        }

        public double getRingsMultiplier() {
            return ringsMultiplier;
        }

        @Override
        public String toString() {
            return guiName;
        }
    }

    private final EnumParam<ConcentricShapeType> type = new EnumParam<>("Type", ConcentricShapeType.class);
    private final RangeParam sidesParam = new RangeParam("Sides", 3, 7, 25);
    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final RangeParam distanceParam = new RangeParam("Distance", 1, 20, 100);
    private final ColorListParam colorsParam = new ColorListParam("Colors",
        2, 2, WHITE, BLACK, Colors.CW_RED, Colors.CW_GREEN, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET);
    private final RangeParam randomnessParam = new RangeParam("Randomness", 0, 0, 100);
    private final AngleParam rotateParam = new AngleParam("Rotate", 0);

    private record ColoredShape(Color color, Shape shape) {
    }

    public ConcentricShapes() {
        super(false);

        FilterButtonModel reseedAction = paramSet.createReseedAction("", "Reseed Randomness");
        type.setupEnableOtherIf(sidesParam, ConcentricShapeType::hasSides);
        type.setupEnableOtherIf(rotateParam, t -> t != ConcentricShapeType.CIRCLES);

        setParams(
            type,
            sidesParam,
            center,
            distanceParam,
            colorsParam,
            rotateParam,
            randomnessParam.withAction(reseedAction)
        ).withAction(new FilterButtonModel("Export SVG...", this::exportSVG,
            null, "Export the current image to an SVG file",
            null, false));

        randomnessParam.setupEnableOtherIfNotZero(reseedAction);
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Random random = paramSet.getLastSeedRandom();
        List<ColoredShape> shapes = createShapes(dest.getWidth(), dest.getHeight(), random, rotateParam.getValueInRadians());
        for (ColoredShape shape : shapes) {
            g.setColor(shape.color());
            g.fill(shape.shape());
        }

        g.dispose();
        return dest;
    }

    private List<ColoredShape> createShapes(int width, int height, Random rng, double angle) {
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        ConcentricShapeType shapeType = type.getSelected();
        double maxDist = calcMaxDistance(cx, cy, width, height);

        int randomness = randomnessParam.getValue();
        double rndMultiplier = randomness / 400.0;
        maxDist += rndMultiplier * maxDist;

        double distance = distanceParam.getValueAsDouble();
        int numRings = 1 + (int) (maxDist / distance);

        distance *= shapeType.getDistMultiplier();
        numRings *= shapeType.getRingsMultiplier();

        Color[] colors = colorsParam.getColors();
        int numColors = colors.length;
        List<ColoredShape> retVal = new ArrayList<>(numRings);

        for (int ring = numRings; ring > 0; ring--) {
            double r = ring * distance;
            Color color = colorsParam.getColor((ring - 1) % numColors);
            Shape shape = shapeType.createShape(cx, cy, r, sidesParam.getValue());
            if (randomness > 0) {
                shape = Shapes.randomize(shape, rng, rndMultiplier * r);
            }
            if (angle != 0) {
                shape = Shapes.rotate(shape, angle, cx, cy);
            }
            retVal.add(new ColoredShape(color, shape));
        }
        return retVal;
    }

    private void exportSVG() {
        StringBuilder content = new StringBuilder();
        content.append(IO.createSVGElement());
        content.append("\n");

        Canvas canvas = Views.getActiveComp().getCanvas();
        List<ColoredShape> coloredShapes = createShapes(canvas.getWidth(), canvas.getHeight(), paramSet.getLastSeedRandom(), rotateParam.getValueInRadians());
        for (ColoredShape coloredShape : coloredShapes) {
            String svgPath = Shapes.toSVGPath(coloredShape.shape());
            String svgColor = Colors.toHTMLHex(coloredShape.color(), false);
            content.append("<path d=\"%s\" fill=\"#%s\"/>\n".formatted(svgPath, svgColor));
        }
        content.append("</svg>");

        IO.saveSVG(content.toString(), "concentric.svg");
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