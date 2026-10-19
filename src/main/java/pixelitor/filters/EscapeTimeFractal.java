/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.impl.ComplexPlaneFilter;
import pixelitor.filters.util.ComplexBounds;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static pixelitor.filters.impl.EscapeTimeFilter.*;

/**
 * Common superclass for the Julia and Mandelbrot set UIs.
 */
public abstract class EscapeTimeFractal extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 9185505916174567657L;

    private static final int COLORS_CONTRASTING = 1;
    private static final int COLORS_CONTINUOUS = 2;
    private static final int COLORS_BLUES = 3;

    private static final int SUPERSAMPLING_NONE = 1;
    private static final int SUPERSAMPLING_2X2 = 2;

    // color cache fields
    private static int[] cachedColors = null;
    private static int cachedColorsStyle = -1;
    private static int cachedMaxIterations = -1;

    /**
     * The available iteration algorithms.
     */
    protected enum IterationType {
        // plain powers: z^d + c
        MANDELBROT("Mandelbrot", new MandelbrotStrategy()),
        MULTIBROT_3("Multibrot d=3", new MultibrotStrategy3()),
        MULTIBROT_4("Multibrot d=4", new MultibrotStrategy4()),
        MULTIBROT_5("Multibrot d=5", new MultibrotStrategy5()),

        // conjugated powers: conj(z)^d + c
        TRICORN("Tricorn", new TricornStrategy()),
        MULTICORN_3("Multicorn d=3", new MulticornStrategy3()),
        MULTICORN_4("Multicorn d=4", new MulticornStrategy4()),

        // absolute values of both components of z before the power
        BURNING_SHIP("Burning Ship", new BurningShipStrategy()),
        BURNING_SHIP_3("Burning Ship d=3", new BurningShipStrategy3()),
        BURNING_SHIP_4("Burning Ship d=4", new BurningShipStrategy4()),

        // absolute values applied to the components of z²
        CELTIC("Celtic", new CelticStrategy()),
        CELTIC_MANDELBAR("Celtic Mandelbar", new CelticMandelbarStrategy()),
        BUFFALO("Buffalo", new BuffaloStrategy()),

        // absolute value of a single component of z before squaring
        HEART_MANDELBROT("Heart Mandelbrot", new HeartMandelbrotStrategy()),
        PERP_MANDELBROT("Perpendicular Mandelbrot", new PerpendicularMandelbrotStrategy()),
        PERP_BURNING_SHIP("Perpendicular Burning Ship", new PerpendicularBurningShipStrategy()),

        // rational and transcendental maps, which have their own exit tests
        MAGNET_1("Magnet Type I", new Magnet1Strategy()),
        MAGNET_2("Magnet Type II", new Magnet2Strategy()),
        SINE("Sine", new SineStrategy()),
        EXPONENTIAL("Exponential", new ExponentialStrategy());

        private final String displayName;
        private final IterationStrategy strategy; // stateless, so it can be shared
        private final ComplexBounds bounds;

        IterationType(String displayName, IterationStrategy strategy) {
            this.displayName = displayName;
            this.strategy = strategy;
            this.bounds = strategy.getBounds();
        }

        public IterationStrategy strategy() {
            return strategy;
        }

        public ComplexBounds bounds() {
            return bounds;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    protected final EnumParam<IterationType> iterationTypeParam =
        new EnumParam<>("Iteration Type", IterationType.class);

    protected final BooleanParam insideOutParam = new BooleanParam("Inside Out", false);
    protected final LogZoomParam zoomParam = new LogZoomParam(GUIText.ZOOM, 200, 200, 1000);
    protected final ImagePositionParam zoomCenterParam;
    protected final RangeParam iterationsParam;
    protected final IntChoiceParam colorsParam = new IntChoiceParam("Colors", new Item[]{
        new Item("Contrasting", COLORS_CONTRASTING),
        new Item("Continuous", COLORS_CONTINUOUS),
        new Item("Blues", COLORS_BLUES),
    });
    private final IntChoiceParam supersamplingParam = new IntChoiceParam("Supersampling", new Item[]{
        new Item("None (Faster)", SUPERSAMPLING_NONE),
        new Item("2x2 (Better, Slower)", SUPERSAMPLING_2X2),
    }, RandomizeMode.IGNORE);

    protected EscapeTimeFractal(int defaultIterations, float defaultZoomCenterX) {
        super(false);

        iterationsParam = new RangeParam.Builder("Iterations")
            .min(2)
            .def(defaultIterations)
            .max(998)
            .randomizeMode(RandomizeMode.IGNORE)
            .build();

        zoomParam.setPresetKey("Zoom");
        insideOutParam.setToolTip("Inverts the starting coordinates: f(z) = 1/z");

        zoomCenterParam = new ImagePositionParam("Zoom Center", defaultZoomCenterX, 0.5f);
        initParams(
            iterationTypeParam,
            insideOutParam,
            zoomParam,
            zoomCenterParam.withDecimalPlaces(2),
            iterationsParam,
            colorsParam,
            supersamplingParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        return switch (supersamplingParam.getValue()) {
            case SUPERSAMPLING_NONE -> renderFractal(src, dest);
            case SUPERSAMPLING_2X2 -> {
                // render at double resolution, then scale down for 2x2 supersampling
                BufferedImage big = new BufferedImage(src.getWidth() * 2, src.getHeight() * 2, src.getType());
                renderFractal(big, big); // processPixel ignores the input, so in-place is safe
                Graphics2D g = dest.createGraphics();
                g.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                g.scale(0.5, 0.5);
                g.drawImage(big, 0, 0, null);
                g.dispose();
                big.flush();
                yield dest;
            }
            default -> throw new IllegalStateException("supersampling = " + supersamplingParam.getValue());
        };
    }

    protected abstract BufferedImage renderFractal(BufferedImage src, BufferedImage dest);

    protected static int[] getColors(int colorsStyle, int maxIterations) {
        // check if we can use the cached colors
        if (cachedColors != null &&
            cachedColorsStyle == colorsStyle &&
            cachedMaxIterations == maxIterations) {
            return cachedColors;
        }

        int[] colors = generateColors(colorsStyle, maxIterations);

        // cache the generated colors
        cachedColors = colors;
        cachedColorsStyle = colorsStyle;
        cachedMaxIterations = maxIterations;

        return colors;
    }

    /**
     * Creates a lookup table mapping iteration counts to RGB colors based on the given style.
     */
    private static int[] generateColors(int colorsStyle, int maxIterations) {
        int[] colors = new int[maxIterations + 1];
        double normalizer = Math.log(maxIterations + 1);

        colors[0] = ComplexPlaneFilter.IN_SET_COLOR; // 0 remaining iterations => point belongs to the set

        for (int it = 1; it <= maxIterations; it++) {
            float bri = (float) (1 + Math.log(maxIterations - it + 1) / normalizer) / 2;
            colors[it] = switch (colorsStyle) {
                case COLORS_CONTRASTING -> Color.HSBtoRGB(maxIterations / (float) it, 0.9f, bri);
                case COLORS_CONTINUOUS -> Color.HSBtoRGB((float) it / maxIterations, 0.9f, bri);
                case COLORS_BLUES ->
                    Color.HSBtoRGB(0.5f + (float) it / (maxIterations * 10), (float) it / maxIterations, bri);
                default -> throw new IllegalStateException("Unknown color style: " + colorsStyle);
            };
        }
        return colors;
    }
}
