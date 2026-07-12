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
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static pixelitor.filters.impl.ComplexFractalFilter.*;

/**
 * Common superclass for the Julia and Mandelbrot sets.
 */
public abstract class ComplexFractal extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 9185505916174567657L;

    private static final int COLORS_CONTRASTING = 1;
    private static final int COLORS_CONTINUOUS = 2;
    private static final int COLORS_BLUES = 3;

    private static final int SUPERSAMPLING_NONE = 1;
    private static final int SUPERSAMPLING_2X2 = 2;

    private static final int ITERATION_MANDELBROT = 0;
    private static final int ITERATION_BURNING_SHIP = 1;
    private static final int ITERATION_TRICORN = 2;
    private static final int ITERATION_MULTIBROT_3 = 3;
    private static final int ITERATION_MULTIBROT_4 = 4;
    private static final int ITERATION_MULTIBROT_5 = 5;

    // color cache fields
    private static int[] cachedColors = null;
    private static int cachedColorsStyle = -1;
    private static int cachedMaxIterations = -1;

    private final IntChoiceParam iterationTypeParam = new IntChoiceParam("Iteration Type", new Item[]{
        new Item("Mandelbrot", ITERATION_MANDELBROT),
        new Item("Burning Ship", ITERATION_BURNING_SHIP),
        new Item("Tricorn", ITERATION_TRICORN),
        new Item("Multibrot d=3", ITERATION_MULTIBROT_3),
        new Item("Multibrot d=4", ITERATION_MULTIBROT_4),
        new Item("Multibrot d=5", ITERATION_MULTIBROT_5),
    });
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

    protected ComplexFractal(int defaultIterations, float defaultZoomCenterX) {
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
                BufferedImage bigSrc = new BufferedImage(
                    src.getWidth() * 2, src.getHeight() * 2, src.getType());
                BufferedImage bigDest = renderFractal(bigSrc, null);
                bigSrc.flush();
                Graphics2D g2 = dest.createGraphics();
                g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
                g2.scale(0.5, 0.5);
                g2.drawImage(bigDest, 0, 0, null);
                g2.dispose();
                bigDest.flush();
                yield dest;
            }
            default -> throw new IllegalStateException("aa = " + supersamplingParam.getValue());
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
     * Points belonging to the set (0 iterations) are always colored black.
     */
    private static int[] generateColors(int colorsStyle, int maxIterations) {
        int[] colors = new int[maxIterations + 1];
        double normalizer = Math.log(maxIterations + 1);
        for (int it = 0; it <= maxIterations; it++) {
            float bri = (float) (1 + Math.log(maxIterations - it + 1) / normalizer) / 2;
            colors[it] = switch (colorsStyle) {
                case COLORS_CONTRASTING -> Color.HSBtoRGB(
                    it > 0 ? maxIterations / (float) it : 0,
                    0.9f, it > 0 ? bri : 0);
                case COLORS_CONTINUOUS -> Color.HSBtoRGB(
                    (float) it / maxIterations,
                    0.9f, it > 0 ? bri : 0);
                case COLORS_BLUES -> Color.HSBtoRGB(
                    0.5f + (float) it / (maxIterations * 10),
                    (float) it / maxIterations,
                    it > 0 ? bri : 0);
                default -> throw new IllegalStateException("value = " + colorsStyle);
            };
        }
        return colors;
    }

    /**
     * Creates an iteration strategy based on the user's selection.
     */
    protected IterationStrategy createIterator() {
        int type = iterationTypeParam.getValue();
        return switch (type) {
            case ITERATION_MANDELBROT -> new MandelbrotStrategy();
            case ITERATION_BURNING_SHIP -> new BurningShipStrategy();
            case ITERATION_TRICORN -> new TricornStrategy();
            case ITERATION_MULTIBROT_3 -> new MultibrotStrategy3();
            case ITERATION_MULTIBROT_4 -> new MultibrotStrategy4();
            case ITERATION_MULTIBROT_5 -> new MultibrotStrategy5();
            default -> throw new IllegalStateException("Unknown iteration type: " + type);
        };
    }
}
