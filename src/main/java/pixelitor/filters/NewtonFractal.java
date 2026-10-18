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
import pixelitor.filters.impl.NewtonFractalFilter;
import pixelitor.filters.util.Complex;
import pixelitor.filters.util.ComplexBounds;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static pixelitor.filters.impl.NewtonFractalFilter.METHOD_HALLEY;
import static pixelitor.filters.impl.NewtonFractalFilter.METHOD_NEWTON;

/**
 * The UI for {@link NewtonFractalFilter}.
 */
public class NewtonFractal extends ParametrizedFilter {
    public static final String NAME = "Newton Fractal";

    @Serial
    private static final long serialVersionUID = 4820194827103948512L;

    private static final int SUPERSAMPLING_NONE = 1;
    private static final int SUPERSAMPLING_2X2 = 2;

    // fixed reference for iteration shading to decouple brightness from the iteration budget
    private static final double SHADING_REFERENCE_STEPS = 100.0;
    private static final double SHADING_NORMALIZER = Math.log(SHADING_REFERENCE_STEPS);

    private static final float SATURATION = 0.85f;
    private static final float MAX_DARKENING = 0.75f;

    private static final ComplexBounds PLANE_BOUNDS = new ComplexBounds(-2.0, 2.0, -2.0, 2.0);

    private final IntChoiceParam polynomialParam = new IntChoiceParam("Polynomial", new Item[]{
        new Item("z³ − 1", 3),
        new Item("z⁴ − 1", 4),
        new Item("z⁵ − 1", 5),
        new Item("z⁶ − 1", 6),
        new Item("z⁷ − 1", 7),
        new Item("z⁸ − 1", 8),
    });
    private final IntChoiceParam methodParam = new IntChoiceParam("Method", new Item[]{
        new Item("Newton", METHOD_NEWTON),
        new Item("Halley", METHOD_HALLEY),
    });
    private final RangeParam shadingStrengthParam = new RangeParam("Shading Strength", 0, 50, 100);
    private final LogZoomParam zoomParam = new LogZoomParam(GUIText.ZOOM, 200, 200, 1000);
    private final ImagePositionParam zoomCenterParam;
    private final RangeParam iterationsParam;
    private final IntChoiceParam supersamplingParam = new IntChoiceParam("Supersampling", new Item[]{
        new Item("None (Faster)", SUPERSAMPLING_NONE),
        new Item("2x2 (Better, Slower)", SUPERSAMPLING_2X2),
    }, RandomizeMode.IGNORE);

    private final ComplexNumberParam stepMultiplierParam = new ComplexNumberParam("Step Multiplier",
        new Complex(1.0, 0.0),
        new ComplexBounds(-0.2, 2.2, -1.2, 1.2));

    public NewtonFractal() {
        super(false);

        iterationsParam = new RangeParam.Builder("Iterations")
            .min(1)
            .def(50)
            .max(1000)
            .randomizeMode(RandomizeMode.IGNORE)
            .build();

        zoomParam.setPresetKey("Zoom");
        zoomCenterParam = new ImagePositionParam("Zoom Center", 0.5f, 0.5f);

        initParams(
            polynomialParam,
            methodParam,
            stepMultiplierParam.forNewton(),
            iterationsParam,
            shadingStrengthParam,
            zoomParam,
            zoomCenterParam.withDecimalPlaces(2),
            supersamplingParam
        );

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Newton_fractal");
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

    private BufferedImage renderFractal(BufferedImage src, BufferedImage dest) {
        int n = polynomialParam.getValue();
        int iterations = iterationsParam.getValue();
        int shadingStrength = shadingStrengthParam.getValue();

        NewtonFractalFilter filter = new NewtonFractalFilter(NAME,
            PLANE_BOUNDS,
            n,
            zoomParam.getZoomRatio(),
            zoomCenterParam.getRelativePoint(),
            iterations,
            generateColors(n, iterations, shadingStrength),
            methodParam.getValue(),
            stepMultiplierParam.getValue());

        return filter.filter(src, dest);
    }

    /**
     * Creates a lookup table mapping each root and iteration step to an RGB color.
     */
    private static int[][] generateColors(int n, int maxIterations, int shadingStrength) {
        int[][] colors = new int[n][maxIterations + 1];
        float shadeFactor = (shadingStrength / 100.0f) * MAX_DARKENING;

        float[] hues = new float[n];
        for (int k = 0; k < n; k++) {
            hues[k] = (float) k / n;
        }

        for (int step = 0; step <= maxIterations; step++) {
            // logarithmic shading normalized against a fixed reference step count
            float progress = (float) Math.min(1.0, Math.log(Math.max(1, step)) / SHADING_NORMALIZER);
            float bri = 1.0f - shadeFactor * progress;

            for (int k = 0; k < n; k++) {
                colors[k][step] = Color.HSBtoRGB(hues[k], SATURATION, bri);
            }
        }
        return colors;
    }
}
