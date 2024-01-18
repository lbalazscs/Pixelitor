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

import pixelitor.filters.gui.ImagePositionParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.LogZoomParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.GUIText;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Common superclass for the Julia and Mandelbrot sets
 */
public abstract class ComplexFractal extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 9185505916174567657L;

    private static final int COLORS_CONTRASTING = 1;
    private static final int COLORS_CONTINUOUS = 2;
    private static final int COLORS_BLUES = 3;

    private static final int AA_NONE = 1;
    private static final int AA_2x2 = 2;

    protected final LogZoomParam zoomParam = new LogZoomParam(GUIText.ZOOM, 200, 200, 1000);
    protected final ImagePositionParam zoomCenter;
    protected final RangeParam iterationsParam;
    private final IntChoiceParam colorsParam = new IntChoiceParam("Colors", new Item[]{
        new Item("Contrasting", COLORS_CONTRASTING),
        new Item("Continuous", COLORS_CONTINUOUS),
        new Item("Blues", COLORS_BLUES),
    });
    private final IntChoiceParam aaParam = new IntChoiceParam("Supersampling", new Item[]{
        new Item("None (Faster)", AA_NONE),
        new Item("2x2 (Better, Slower)", AA_2x2),
    }, IGNORE_RANDOMIZE);

    protected ComplexFractal(int defaultIterations, float zoomX) {
        super(false);

        iterationsParam = new RangeParam.Builder("Iterations")
            .min(2)
            .def(defaultIterations)
            .max(998)
            .randomizePolicy(IGNORE_RANDOMIZE)
            .build();

        zoomParam.setPresetKey("Zoom");

        zoomCenter = new ImagePositionParam("Zoom Center", zoomX, 0.5f);
        setParams(zoomParam,
            zoomCenter.withDecimalPlaces(2),
            iterationsParam,
            colorsParam,
            aaParam);
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int aa = aaParam.getValue();
        if (aa == AA_NONE) {
            return transformAA(src, dest);
        } else if (aa == AA_2x2) {
            // transform an image with double size, then scale it down
            BufferedImage bigSrc = new BufferedImage(
                src.getWidth() * 2, src.getHeight() * 2, src.getType());
            BufferedImage bigDest = transformAA(bigSrc, null);
            bigSrc.flush();
            Graphics2D g2 = dest.createGraphics();
            g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
            g2.scale(0.5, 0.5);
            g2.drawImage(bigDest, 0, 0, null);
            g2.dispose();
            bigDest.flush();
            return dest;
        } else {
            throw new IllegalStateException("aa = " + aa);
        }
    }

    protected abstract BufferedImage transformAA(BufferedImage src, BufferedImage dest);

    protected int[] createColors(int maxIterations) {
        int[] colors = new int[maxIterations + 1];
        if (colorsParam.getValue() == COLORS_CONTRASTING) {
            double normalizer = Math.log(maxIterations + 1);
            for (int it = 0; it <= maxIterations; it++) {
                float bri = (float) (1 + Math.log(maxIterations - it + 1) / normalizer) / 2;
                colors[it] = Color.HSBtoRGB(
                    maxIterations / (float) it,
                    0.9f,
                    it > 0 ? bri : 0);
            }
        } else if (colorsParam.getValue() == COLORS_CONTINUOUS) {
            double normalizer = Math.log(maxIterations + 1);
            for (int it = 0; it <= maxIterations; it++) {
                float bri = (float) (1 + Math.log(maxIterations - it + 1) / normalizer) / 2;
                colors[it] = Color.HSBtoRGB(
                    (float) it / maxIterations,
                    0.9f,
                    it > 0 ? bri : 0);
            }
        } else if (colorsParam.getValue() == COLORS_BLUES) {
            double normalizer = Math.log(maxIterations + 1);
            for (int it = 0; it <= maxIterations; it++) {
                float bri = (float) (1 + Math.log(maxIterations - it + 1) / normalizer) / 2;
                colors[it] = Color.HSBtoRGB(
                    0.5f + (float) it / (maxIterations * 10),
                    (float) it / maxIterations,
                    it > 0 ? bri : 0);
            }
        } else {
            throw new IllegalStateException("value = " + colorsParam.getValue());
        }
        return colors;
    }
}

