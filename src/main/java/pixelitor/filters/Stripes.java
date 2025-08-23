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
import static pixelitor.filters.gui.TransparencyMode.MANUAL_ALPHA_ONLY;

/**
 * A filter that generates a pattern of parallel stripes.
 */
public class Stripes extends ParametrizedFilter {
    public static final String NAME = "Stripes";

    @Serial
    private static final long serialVersionUID = 1L;

    // an approximation of a sine-like arc using a cubic Bezier curve
    private static final double BEZIER_CONTROL_FACTOR = 0.552284749831;

    enum Type {
        STRAIGHT("Straight") {
            @Override
            public double calcPeriod(int thickness, int gap, double wavelength, double amplitude) {
                return thickness + gap;
            }

            @Override
            public Shape createPrototypeShape(double diagonal, int thickness, double wavelength, double amplitude) {
                // for straight stripes, the prototype is a simple horizontal rectangle
                return new Rectangle2D.Double(
                    -diagonal / 2.0,
                    -thickness / 2.0,
                    diagonal,
                    thickness
                );
            }
        },
        CHEVRON("Chevron") {
            @Override
            public double calcPeriod(int thickness, int gap, double wavelength, double amplitude) {
                return calculateWavyPeriod(thickness, gap, wavelength, amplitude);
            }

            @Override
            public Shape createPrototypeShape(double diagonal, int thickness, double wavelength, double amplitude) {
                Path2D centerline = createChevronCenterline(diagonal, wavelength, amplitude);
                return createStrokedShape(centerline, thickness);
            }
        },
        CURVED("Curved") {
            @Override
            public double calcPeriod(int thickness, int gap, double wavelength, double amplitude) {
                return calculateWavyPeriod(thickness, gap, wavelength, amplitude);
            }

            @Override
            public Shape createPrototypeShape(double diagonal, int thickness, double wavelength, double amplitude) {
                Path2D centerline = createCurvedCenterline(diagonal, wavelength, amplitude);
                return createStrokedShape(centerline, thickness);
            }
        },
        SQUARE_WAVE("Square Wave") {
            @Override
            public double calcPeriod(int thickness, int gap, double wavelength, double amplitude) {
                // for square waves, the vertical period includes the wave's full height
                return thickness + gap + 2 * amplitude;
            }

            @Override
            public Shape createPrototypeShape(double diagonal, int thickness, double wavelength, double amplitude) {
                Path2D centerline = createSquareWaveCenterline(diagonal, wavelength, amplitude);
                return createStrokedShape(centerline, thickness);
            }
        };

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Calculates the period (vertical distance between stripe centerlines).
         */
        public abstract double calcPeriod(int thickness, int gap, double wavelength, double amplitude);

        /**
         * Creates the prototype shape for a single stripe.
         */
        public abstract Shape createPrototypeShape(double diagonal, int thickness, double wavelength, double amplitude);

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final EnumParam<Type> type = new EnumParam<>("Type", Type.class);
    private final RangeParam thicknessParam = new RangeParam("Thickness", 2, 20, 200);
    private final RangeParam gapParam = new RangeParam("Gap", 2, 20, 200);
    private final RangeParam wavelengthParam = new RangeParam("Wavelength", 10, 80, 500);
    private final RangeParam amplitudeParam = new RangeParam("Amplitude", 10, 20, 500);
    private final ColorParam bgColor = new ColorParam("Background Color", BLACK, MANUAL_ALPHA_ONLY);
    private final ColorListParam colorsParam = new ColorListParam("Colors",
        1, 1, WHITE, Colors.CW_GREEN, Colors.CW_RED, Colors.CW_BLUE,
        Colors.CW_ORANGE, Colors.CW_TEAL, Colors.CW_VIOLET, Colors.CW_YELLOW);
    private final AngleParam rotate = new AngleParam("Rotate", 0);

    public Stripes() {
        super(false);

        // enable wavelength and amplitude only when the selected type is not straight
        Predicate<Type> notStraight = selected -> selected != Type.STRAIGHT;
        type.setupEnableOtherIf(wavelengthParam, notStraight);
        type.setupEnableOtherIf(amplitudeParam, notStraight);

        initParams(
            type,
            new GroupedRangeParam("Stripe", new RangeParam[]{
                thicknessParam, gapParam
            }, true),
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
        // if the background is fully opaque, we can create a new blank image;
        // otherwise, we need to draw over the existing image
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
        Type selectedType = type.getSelected();
        int thickness = thicknessParam.getValue();
        int gap = gapParam.getValue();
        double wavelength = wavelengthParam.getValue();
        double amplitude = amplitudeParam.getValue();

        // the diagonal is the longest possible line, ensuring stripes cover the entire image
        double diagonal = Math.sqrt(width * width + height * height);

        double period = selectedType.calcPeriod(thickness, gap, wavelength, amplitude);
        Shape prototype = selectedType.createPrototypeShape(diagonal, thickness, wavelength, amplitude);

        return generateStripes(width, height, period, prototype, diagonal);
    }

    /**
     * Generates a list of stripe shapes from a prototype shape.
     */
    private List<ShapeWithColor> generateStripes(int width, int height, double period, Shape prototypeShape, double diagonal) {
        List<ShapeWithColor> shapes = new ArrayList<>();
        Color[] colors = colorsParam.getColors();
        int numColors = colors.length;
        double angle = rotate.getValueInRadians();

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

            // assign color cyclically; offset i to ensure the index is non-negative
            int colorIndex = (i + numStripesPerSide) % numColors;
            shapes.add(new ShapeWithColor(finalShape, colors[colorIndex]));
        }

        return shapes;
    }

    /**
     * Calculates the vertical period for wavy stripes (Chevron, Curved).
     */
    private static double calculateWavyPeriod(double thickness, double gap, double wavelength, double amplitude) {
        // To maintain a consistent visual thickness, the vertical period
        // between centerlines is adjusted based on the stripe's slope.
        // The period is calculated using the slope of a chevron segment,
        // which is also a good approximation for curved stripes.
        if (wavelength > 0) {
            double slope = (4.0 * amplitude) / wavelength;
            return (thickness + gap) * Math.sqrt(1.0 + slope * slope);
        } else {
            // avoid division by zero; treat as horizontal lines
            return thickness + gap;
        }
    }

    /**
     * Creates the centerline path for a chevron (zigzag) stripe.
     */
    private static Path2D createChevronCenterline(double diagonal, double wavelength, double amplitude) {
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
            double y = goUp ? amplitude : -amplitude;
            centerline.lineTo(currentX, y);
            goUp = !goUp; // alternate between peak and trough
        }
        return centerline;
    }

    /**
     * Creates the centerline path for a curved (sinusoidal) stripe.
     */
    private static Path2D createCurvedCenterline(double diagonal, double wavelength, double amplitude) {
        Path2D centerline = new Path2D.Double();
        double halfWavelength = wavelength / 2.0;
        double controlXOffset = halfWavelength * BEZIER_CONTROL_FACTOR;
        double startX = -diagonal / 2.0 - wavelength;
        double endX = diagonal / 2.0 + wavelength;
        double currentX = startX;
        boolean goUp = true;

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
        return centerline;
    }

    private static Path2D createSquareWaveCenterline(double diagonal, double wavelength, double amplitude) {
        Path2D centerline = new Path2D.Double();
        double halfWavelength = wavelength / 2.0;
        double startX = -diagonal / 2.0 - wavelength;
        double endX = diagonal / 2.0 + wavelength;
        double currentX = startX;
        boolean goUp = true;

        centerline.moveTo(currentX, -amplitude);
        while (currentX < endX) {
            double nextY = goUp ? amplitude : -amplitude;
            centerline.lineTo(currentX, nextY);
            currentX += halfWavelength;
            centerline.lineTo(currentX, nextY);
            goUp = !goUp;
        }
        return centerline;
    }

    private static Shape createStrokedShape(Path2D centerPath, int thickness) {
        BasicStroke stroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 100.0f);
        return stroke.createStrokedShape(centerPath);
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
