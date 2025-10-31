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

import org.jdesktop.swingx.geom.Star2D;
import pixelitor.Canvas;
import pixelitor.Views;
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.CustomShapes;
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

    private enum Arrangement {
        NESTED("Nested"), RINGED("Ringed");

        private final String displayName;

        Arrangement(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private enum ConcentricShapeType {
        CIRCLES("Circles", false, false, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return CustomShapes.createCircle(cx, cy, r);
            }
        }, POLYGONS("Polygons", true, true, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return CustomShapes.createCircumscribedPolygon(n, cx, cy, r, tuning);
            }
        }, STARS("Stars", true, true, 2.0, 2.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                double innerRadius = (0.5 + tuning * 0.5) * r;
                return new Star2D(cx, cy, innerRadius, r, n);
            }
        }, BATS("Bats", false, false, 2.0, 2.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return CustomShapes.createBat(cx - r, cy - r, 2 * r, 2 * r);
            }
        }, HEARTS("Hearts", false, false, 1.0, 1.5) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return CustomShapes.createHeart(cx - r, cy - r, 2 * r, 2 * r);
            }
        }, FLOWERS("Flowers", true, true, 1.0, 1.0) {
            @Override
            Shape createShape(double cx, double cy, double r, int n, double tuning) {
                return CustomShapes.createFlower(n, cx, cy, r, tuning);
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

    private final EnumParam<ConcentricShapeType> shapeTypeParam = new EnumParam<>("Type", ConcentricShapeType.class);
    private final EnumParam<Arrangement> arrangementParam = new EnumParam<>("Arrangement", Arrangement.class);
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

    public ConcentricShapes() {
        super(false);

        FilterButtonModel reseedAction = paramSet.createReseedAction("", "Reseed Randomness");

        shapeTypeParam.setupEnableOtherIf(sides, ConcentricShapeType::hasSides);
        shapeTypeParam.setupEnableOtherIf(tuning, ConcentricShapeType::hasTuning);
        randomnessParam.setupEnableOtherIfNotZero(reseedAction);

        initParams(
            arrangementParam,
            shapeTypeParam,
            sides,
            tuning,
            distanceParam,
            colorsParam,
            new CompositeParam("Transform", center, scale, rotate),
            randomnessParam.withSideButton(reseedAction)
        ).withAction(FilterButtonModel.createExportSvg(this::exportSVG));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);
        Graphics2D g = dest.createGraphics();

        Random random = paramSet.getLastSeedRandom();

        int width = dest.getWidth();
        int height = dest.getHeight();
        g.setColor(colorsParam.getColor(0));
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        List<ShapeWithColor> shapes = createShapes(width, height, random,
            tuning.getPercentage(), arrangementParam.getSelected());

        for (ShapeWithColor shape : shapes) {
            shape.fill(g);
        }

        g.dispose();
        return dest;
    }

    private List<ShapeWithColor> createShapes(int width, int height, Random rng, double tuning, Arrangement arrangement) {
        return arrangement == Arrangement.NESTED
            ? createNestedShapes(width, height, rng, tuning)
            : createRingedShapes(width, height, rng, tuning);
    }

    private List<ShapeWithColor> createNestedShapes(int width, int height, Random rng, double tuning) {
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        ConcentricShapeType shapeType = shapeTypeParam.getSelected();
        double maxDist = calcMaxDistance(cx, cy, width, height);

        int randomness = randomnessParam.getValue();
        double rndMultiplier = randomness / 400.0;
        maxDist += rndMultiplier * maxDist;

        double distance = distanceParam.getValueAsDouble();
        int numRings = (int) (shapeType.getRingsMultiplier() * (1 + (maxDist / distance)));
        distance *= shapeType.getDistMultiplier();

        Color[] colors = colorsParam.getColors();
        int numSides = sides.getValue();
        AffineTransform at = createTransform(cx, cy);

        List<ShapeWithColor> shapes = new ArrayList<>(numRings);
        for (int ring = numRings; ring > 0; ring--) {
            double r = ring * distance;
            Color color = colorsParam.getColor((ring - 1) % colors.length);
            Shape shape = createShape(shapeType, cx, cy, rng, tuning, r, numSides, randomness, rndMultiplier, at);
            shapes.add(new ShapeWithColor(shape, color));
        }
        return shapes;
    }

    private List<ShapeWithColor> createRingedShapes(int width, int height, Random rng, double tuning) {
        double cx = width * center.getRelativeX();
        double cy = height * center.getRelativeY();

        ConcentricShapeType shapeType = shapeTypeParam.getSelected();
        double r = distanceParam.getValue();
        int numSides = sides.getValue();
        Color[] colors = colorsParam.getColors();

        double maxDist = Math.sqrt(width * width + height + height) / 2.0;
        int randomness = randomnessParam.getValue();
        AffineTransform at = createTransform(cx, cy);
        int numRings = (int) (maxDist / (2 * r));
        List<ShapeWithColor> shapes = new ArrayList<>(numRings);

        // add a shape at the center
        Shape shape = createShape(shapeType, cx, cy, rng, tuning, r, numSides, randomness, randomness / 400.0, at);
        Color color = selectColor(colors, 0);
        shapes.add(new ShapeWithColor(shape, color));

        // add concentric rings of shapes
        int shapeCount = 1;
        for (int ring = 1; ring <= numRings; ring++) {
            int numShapes = ring * 6;
            double ringRadius = ring * 2 * r;

            double startAngle = 3 * Math.PI / 2;
            for (int i = 0; i < numShapes; i++) {
                double angle = startAngle + 2 * Math.PI * i / numShapes;
                double x = cx + ringRadius * Math.cos(angle);
                double y = cy + ringRadius * Math.sin(angle);
                shape = createShape(shapeType, x, y, rng, tuning, r, numSides, randomness, randomness / 400.0, at);
                shapeCount++;
                color = selectColor(colors, shapeCount);
                shapes.add(new ShapeWithColor(shape, color));
            }
        }

        return shapes;
    }

    // the shapes are transformed using this transform, instead of transforming
    // the Graphics, so that they are also tranformed in the SVG export
    private AffineTransform createTransform(double cx, double cy) {
        double scaleX = scale.getPercentage(0);
        double scaleY = scale.getPercentage(1);
        double rotation = rotate.getValueInRadians();

        AffineTransform at = null;
        if (rotation != 0 || scaleX != 1.0 || scaleY != 1.0) {
            at = new AffineTransform();
            // this will first scale, and then rotate!
            at.rotate(rotation, cx, cy);
            at.translate(cx, cy);
            at.scale(scaleX, scaleY);
            at.translate(-cx, -cy);
        }
        return at;
    }

    private static Shape createShape(ConcentricShapeType shapeType, double x, double y,
                                     Random rng, double tuning, double r, int numSides,
                                     int randomness, double rndMultiplier, AffineTransform at) {
        Shape shape = shapeType.createShape(x, y, r, numSides, tuning);
        if (randomness > 0) {
            shape = Shapes.randomize(shape, rng, rndMultiplier * r);
        }
        if (at != null) {
            shape = at.createTransformedShape(shape);
        }
        return shape;
    }

    private Color selectColor(Color[] colors, int shapeCount) {
        // the index for circular iteration, skipping the first color
        int colorIndex = (shapeCount % (colors.length - 1)) + 1;
        return colorsParam.getColor(colorIndex);
    }

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        List<ShapeWithColor> shapes = createShapes(canvas.getWidth(), canvas.getHeight(),
            paramSet.getLastSeedRandom(), tuning.getPercentage(), arrangementParam.getSelected());
        String svgContent = ShapeWithColor.createSvgContent(shapes, canvas, null);
        FileIO.saveSVG(svgContent, this);
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
