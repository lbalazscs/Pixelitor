/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.io.FileIO;
import pixelitor.utils.Geometry;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Shapes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
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

    private enum ConcentricShapeType {
        CIRCLES("Circles", false, false, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return Shapes.createCircle(cx, cy, r);
            }
        }, POLYGONS("Polygons", true, true, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return Shapes.createCircumscribedPolygon(n, cx, cy, r, tuning);
            }
            // the number of rings multiplier corresponds to the max/min radius ratio
        }, STARS("Stars", true, true, 2.0, 2.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                double innerRadius = (0.5 + tuning * 0.5) * r;
                return new Star2D(cx, cy, innerRadius, r, n);
            }
        }, BATS("Bats", false, false, 2.0, 2.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return Shapes.createBat(cx - r, cy - r, 2 * r, 2 * r);
            }
        }, HEARTS("Hearts", false, false, 1.0, 1.5) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return Shapes.createHeart(cx - r, cy - r, 2 * r, 2 * r);
            }
        }, FLOWERS("Flowers", true, true, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return Shapes.createFlower(n, cx, cy, r, tuning);
            }
        };

        private final String displayName;
        private final boolean hasSides;
        private final boolean hasTuning;
        private final double distMultiplier;
        private final double ringsMultiplier;

        ConcentricShapeType(String displayName, boolean hasSides, boolean hasTuning, double distMultiplier, double ringsMultiplier) {
            this.displayName = displayName;
            this.hasSides = hasSides;
            this.hasTuning = hasTuning;
            this.distMultiplier = distMultiplier;
            this.ringsMultiplier = ringsMultiplier;
        }

        abstract Shape createShape(double cx, double cy, double r, int n, double tuning);

        public boolean hasSides() {
            return hasSides;
        }

        public boolean hasTuning() {
            return hasTuning;
        }

        public double getDistMultiplier() {
            return distMultiplier;
        }

        public double getRingsMultiplier() {
            return ringsMultiplier;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final EnumParam<ConcentricShapeType> type = new EnumParam<>("Type", ConcentricShapeType.class);
    private final RangeParam sides = new RangeParam("Sides", 3, 7, 25);
    private final RangeParam tuning = new RangeParam("Tuning", -100, 0, 100);
    private final RangeParam distanceParam = new RangeParam("Distance", 1, 20, 100);
    private final ColorListParam colorsParam = new ColorListParam("Colors",
        2, 2, WHITE, BLACK, Colors.CW_RED, Colors.CW_GREEN, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET);
    private final RangeParam randomnessParam = new RangeParam("Randomness", 0, 0, 100);

    private final ImagePositionParam center = new ImagePositionParam("Center");
    private final GroupedRangeParam scale = new GroupedRangeParam("Scale (%)", 1, 100, 500, false);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    private record ColoredShape(Color color, Shape shape) {
    }

    public ConcentricShapes() {
        super(false);

        FilterButtonModel reseedAction = paramSet.createReseedAction("", "Reseed Randomness");
        type.setupEnableOtherIf(sides, ConcentricShapeType::hasSides);
        type.setupEnableOtherIf(tuning, ConcentricShapeType::hasTuning);

        setParams(
            type,
            sides,
            tuning,
            distanceParam,
            colorsParam,
            new DialogParam("Transform", center, scale, rotate),
            randomnessParam.withAction(reseedAction)
        ).withAction(new FilterButtonModel("Export SVG...", this::exportSVG,
            null, "Export the current image to an SVG file",
            null, false));

        randomnessParam.setupEnableOtherIfNotZero(reseedAction);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        Random random = paramSet.getLastSeedRandom();
        List<ColoredShape> shapes = createShapes(dest.getWidth(), dest.getHeight(),
            random, rotate.getValueInRadians(), tuning.getPercentage());
        for (ColoredShape shape : shapes) {
            g.setColor(shape.color());
            g.fill(shape.shape());
        }

        g.dispose();
        return dest;
    }

    private List<ColoredShape> createShapes(int width, int height, Random rng, double angle, double tuning) {
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        ConcentricShapeType shapeType = type.getSelected();
        double maxDist = calcMaxDistance(cx, cy, width, height);

        int randomness = randomnessParam.getValue();
        double rndMultiplier = randomness / 400.0;
        maxDist += rndMultiplier * maxDist;

        double distance = distanceParam.getValueAsDouble();
        int numRings = (int) (shapeType.getRingsMultiplier() * (1 + (maxDist / distance)));

        distance *= shapeType.getDistMultiplier();

        Color[] colors = colorsParam.getColors();
        int numColors = colors.length;
        List<ColoredShape> retVal = new ArrayList<>(numRings);

        int numSides = sides.getValue();

        double scaleX = scale.getPercentage(0);
        double scaleY = scale.getPercentage(1);

        AffineTransform at = null;
        if (angle != 0 || scaleX != 1.0 || scaleY != 1.0) {
            at = new AffineTransform();
            // this will first scale, and then rotate!
            at.rotate(angle, cx, cy);
            at.translate(cx, cy);
            at.scale(scaleX, scaleY);
            at.translate(-cx, -cy);
        }

        for (int ring = numRings; ring > 0; ring--) {
            double r = ring * distance;
            Color color = colorsParam.getColor((ring - 1) % numColors);
            Shape shape = shapeType.createShape(cx, cy, r, numSides, tuning);
            if (randomness > 0) {
                shape = Shapes.randomize(shape, rng, rndMultiplier * r);
            }
            if (at != null) {
                shape = at.createTransformedShape(shape);
            }
            retVal.add(new ColoredShape(color, shape));
        }
        return retVal;
    }

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        StringBuilder content = new StringBuilder();
        content.append(canvas.createSVGElement());
        content.append("\n");

        List<ColoredShape> coloredShapes = createShapes(canvas.getWidth(), canvas.getHeight(), paramSet.getLastSeedRandom(), rotate.getValueInRadians(), tuning.getPercentage());
        for (ColoredShape coloredShape : coloredShapes) {
            String svgPath = Shapes.toSVGPath(coloredShape.shape());
            String svgColor = Colors.toHTMLHex(coloredShape.color(), false);
            content.append("<path d=\"%s\" fill=\"#%s\"/>\n".formatted(svgPath, svgColor));
        }
        content.append("</svg>");

        FileIO.saveSVG(content.toString(), "concentric.svg");
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