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
import pixelitor.colors.Colors;
import pixelitor.filters.gui.*;
import pixelitor.filters.util.ShapeWithColor;
import pixelitor.io.FileIO;
import pixelitor.utils.ImageUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static pixelitor.filters.gui.IntChoiceParam.Item;
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * A filter that generates a pattern of parallel stripes.
 */
public class Stripes extends ParametrizedFilter {
    public static final String NAME = "Stripes";

    private static final int TYPE_STRAIGHT = 0;
    private static final int TYPE_CHEVRON = 1;
    private static final int TYPE_CURVED = 2;

    @Serial
    private static final long serialVersionUID = 1L;

    // an approximation of a sine-like arc using a cubic Bezier curve
    private static final double BEZIER_CONTROL_FACTOR = 0.552284749831;

    private final IntChoiceParam type = new IntChoiceParam("Type", new Item[]{
        new Item("Straight", TYPE_STRAIGHT),
        new Item("Chevron", TYPE_CHEVRON),
        new Item("Curved", TYPE_CURVED),
    });
    private final RangeParam thicknessParam = new RangeParam("Thickness", 2, 20, 200);
    private final RangeParam wavelengthParam = new RangeParam("Wavelength", 10, 50, 500);
    private final RangeParam amplitudeParam = new RangeParam("Amplitude", 10, 20, 500);
    private final ColorParam bgColor = new ColorParam("Background Color", BLACK, MANUAL_ALPHA_ONLY);
    private final ColorListParam colorsParam = new ColorListParam("Colors",
        1, 1, WHITE, Colors.CW_GREEN, Colors.CW_RED, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET, Colors.CW_YELLOW);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public Stripes() {
        super(false);

        // enable wavelength and amplitude only when the selected type is not straight
        Predicate<Item> notStraight = item -> item.valueIsNot(TYPE_STRAIGHT);
        type.setupEnableOtherIf(wavelengthParam, notStraight);
        type.setupEnableOtherIf(amplitudeParam, notStraight);

        initParams(
            type,
            thicknessParam,
            new GroupedRangeParam("Wave", new RangeParam[]{
                wavelengthParam, amplitudeParam
            }, false).notLinkable(),
            bgColor,
            colorsParam,
            rotate
        ).withAction(FilterButtonModel.createExportSvg(this::exportSVG));
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        Color selectedBg = bgColor.getColor();
        if (selectedBg.getAlpha() == 255) {
            dest = ImageUtils.createImageWithSameCM(src);
        } else {
            dest = ImageUtils.copyImage(src);
        }

        int width = dest.getWidth();
        int height = dest.getHeight();

        Graphics2D g = dest.createGraphics();
        Colors.fillWith(selectedBg, g, width, height);
        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);

        List<ShapeWithColor> shapes = createShapes(width, height);
        for (ShapeWithColor shapeWithColor : shapes) {
            g.setColor(shapeWithColor.color());
            g.fill(shapeWithColor.shape());
        }

        g.dispose();
        return dest;
    }

    /**
     * Creates a list of shapes with their associated colors based on the selected type.
     */
    private List<ShapeWithColor> createShapes(int width, int height) {
        return switch (type.getValue()) {
            case TYPE_STRAIGHT -> createStraightShapes(width, height);
            case TYPE_CHEVRON -> createChevronShapes(width, height);
            case TYPE_CURVED -> createCurvedShapes(width, height);
            default -> throw new IllegalStateException("Unexpected value: " + type.getValue());
        };
    }

    /**
     * Creates shapes for straight, parallel stripes.
     */
    private List<ShapeWithColor> createStraightShapes(int width, int height) {
        int thickness = thicknessParam.getValue();
        if (thickness <= 0) {
            return new ArrayList<>();
        }

        // a period consists of one stripe and one gap of the same thickness
        double period = 2.0 * thickness;

        // the diagonal is the longest possible line, ensuring stripes cover the entire canvas
        double diagonal = Math.sqrt(width * width + height * height);

        // create a horizontal rectangle centered at (0, 0) that is long enough to span the diagonal
        Rectangle2D.Double prototype = new Rectangle2D.Double(
            -diagonal / 2.0,
            -thickness / 2.0,
            diagonal,
            thickness
        );

        return generateStripes(width, height, period, prototype);
    }

    /**
     * Creates shapes for chevron (zigzag) stripes.
     */
    private List<ShapeWithColor> createChevronShapes(int width, int height) {
        int thickness = thicknessParam.getValue();
        if (thickness <= 0) {
            return new ArrayList<>();
        }

        double wavelength = wavelengthParam.getValue();
        double amplitude = amplitudeParam.getValue();

        double period = calculatePeriod(thickness, wavelength, amplitude);

        double diagonal = Math.sqrt(width * width + height * height);

        // create a prototype centerline path for the chevron
        // it needs to be long enough to span the diagonal, so we add a buffer
        Path2D centerline = new Path2D.Double();
        double startX = -diagonal / 2.0 - wavelength;
        double endX = diagonal / 2.0 + wavelength;
        double currentX = startX;
        boolean goUp = true;

        // start at a trough to create a consistent zigzag
        centerline.moveTo(currentX, -amplitude);
        while (currentX < endX) {
            currentX += wavelength / 2.0;
            double y = goUp ? amplitude : -amplitude; // alternate between peak and trough
            centerline.lineTo(currentX, y);
            goUp = !goUp;
        }

        Shape protoChevron = createStrokedShape(centerline, thickness);

        return generateStripes(width, height, period, protoChevron);
    }

    /**
     * Creates sinusoidal shapes for curved stripes.
     */
    private List<ShapeWithColor> createCurvedShapes(int width, int height) {
        int thickness = thicknessParam.getValue();
        if (thickness <= 0) {
            return new ArrayList<>();
        }

        double wavelength = wavelengthParam.getValue();
        double amplitude = amplitudeParam.getValue();

        double period = calculatePeriod(thickness, wavelength, amplitude);

        double diagonal = Math.sqrt(width * width + height * height);

        // create a prototype centerline path for the wave using Bezier curves
        Path2D centerline = new Path2D.Double();

        double halfWavelength = wavelength / 2.0;
        double controlXOffset = halfWavelength * BEZIER_CONTROL_FACTOR;

        double startX = -diagonal / 2.0 - wavelength;
        double endX = diagonal / 2.0 + wavelength;
        double currentX = startX;
        boolean goUp = true; // start from a trough

        // start at a trough to create a consistent wave
        centerline.moveTo(currentX, -amplitude);
        while (currentX < endX) {
            double startY = goUp ? -amplitude : amplitude;
            double endY = goUp ? amplitude : -amplitude;

            double cp1x = currentX + controlXOffset;
            double cp1y = startY;
            double cp2x = currentX + halfWavelength - controlXOffset;
            double cp2y = endY;
            double endPointX = currentX + halfWavelength;

            centerline.curveTo(cp1x, cp1y, cp2x, cp2y, endPointX, endY);

            currentX = endPointX; // use the exact end point to avoid floating point drift
            goUp = !goUp;
        }

        Shape protoWave = createStrokedShape(centerline, thickness);

        return generateStripes(width, height, period, protoWave);
    }

    // Creates a thick shape from a centerline.
    private static Shape createStrokedShape(Path2D centerPath, int thickness) {
        // use a large miter limit to ensure that Chevron always has proper spikes
        BasicStroke stroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 100.0f);
        return stroke.createStrokedShape(centerPath);
    }

    /**
     * Calculates the vertical period between stripe centerlines.
     */
    private static double calculatePeriod(double thickness, double wavelength, double amplitude) {
        // To maintain equal visual thickness for stripes and gaps, the period
        // (vertical distance between centerlines) must be adjusted based
        // on the angle of the segments.
        // The perpendicular distance between centerlines should be 2 * thickness.
        // period = (2 * thickness) / cos(alpha), where alpha is the angle of the segment.
        // The average slope m = dy/dx = (2 * amplitude) / (wavelength / 2) = 4 * amplitude / wavelength.
        // period = 2 * thickness * sqrt(1 + m^2).
        if (wavelength > 0) {
            double slope = (4.0 * amplitude) / wavelength;
            return 2.0 * thickness * Math.sqrt(1.0 + slope * slope);
        } else {
            // avoid division by zero; treat as horizontal lines
            return 2.0 * thickness;
        }
    }

    /**
     * Generates a list of stripe shapes from a prototype shape.
     */
    private List<ShapeWithColor> generateStripes(int width, int height, double period, Shape prototypeShape) {
        List<ShapeWithColor> shapes = new ArrayList<>();
        Color[] colors = colorsParam.getColors();
        int numColors = colors.length;
        double angle = rotate.getValueInRadians();

        double diagonal = Math.sqrt(width * width + height * height);

        // create a transform to rotate the stripes around the center of the image
        AffineTransform at = new AffineTransform();
        at.translate(width / 2.0, height / 2.0);
        at.rotate(angle);

        // calculate how many stripes are needed to cover the area from the center to a corner
        int numStripesPerSide = (int) Math.ceil((diagonal / 2.0) / period);

        for (int i = -numStripesPerSide; i <= numStripesPerSide; i++) {
            // y-coordinate of the stripe's center in the local, un-rotated coordinate system
            double y = i * period;

            // create a transform for this stripe by translating the prototype
            AffineTransform stripeTransform = AffineTransform.getTranslateInstance(0, y);
            Shape translatedShape = stripeTransform.createTransformedShape(prototypeShape);

            // apply the main rotation and translation transform
            Shape finalShape = at.createTransformedShape(translatedShape);

            // assign color cyclically
            int colorIndex = (i + numStripesPerSide) % numColors;
            shapes.add(new ShapeWithColor(finalShape, colors[colorIndex]));
        }

        return shapes;
    }

    private void exportSVG() {
        Canvas canvas = Views.getActiveComp().getCanvas();
        List<ShapeWithColor> shapes = createShapes(canvas.getWidth(), canvas.getHeight());
        String svgContent = ShapeWithColor.createSvgContent(shapes, canvas, bgColor.getColor());
        FileIO.saveSVG(svgContent, "stripes.svg");
    }

    @Override
    protected boolean createDefaultDestImg() {
        return false;
    }
}
