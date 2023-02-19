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

import com.jhlabs.image.ImageMath;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Random;
import java.util.concurrent.Future;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.ColorParam.TransparencyPolicy.USER_ONLY_TRANSPARENCY;
import static pixelitor.gui.GUIText.ZOOM;

/**
 * Clouds filter based on multiple Perlin noise iterations, inspired by the Paint.net clouds
 */
public class Clouds extends ParametrizedFilter {
    public static final String NAME = "Clouds";

    @Serial
    private static final long serialVersionUID = 201867762435136383L;

    private int[] p;

    private final RangeParam scaleParam = new RangeParam(ZOOM, 3, 100, 300);
    private final RangeParam roughnessParam = new RangeParam("Roughness (%)", 0, 50, 100);

    private final ColorParam color1 = new ColorParam("Color 1", BLACK, USER_ONLY_TRANSPARENCY);
    private final ColorParam color2 = new ColorParam("Color 2", WHITE, USER_ONLY_TRANSPARENCY);

    public Clouds() {
        super(false);

        scaleParam.setPresetKey("Zoom");

        setParams(
            scaleParam.withAdjustedRange(0.3),
            roughnessParam,
            color1,
            color2
        ).withAction(paramSet.createReseedAction(this::reseed));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        var pt = new StatusBarProgressTracker(NAME, src.getHeight());

        if (p == null) {
            reseed(paramSet.getLastSeed());
        }

        renderClouds(dest,
            scaleParam.getValueAsFloat(),
            (float) roughnessParam.getPercentage(),
            color1.getColor(),
            color2.getColor(),
            pt);

        pt.finished();
        return dest;
    }

    private void renderClouds(BufferedImage dest,
                              float scale, float roughness,
                              Color c1, Color c2, ProgressTracker pt) {
        int width = dest.getWidth();
        int height = dest.getHeight();
        int[] destData = ImageUtils.getPixelArray(dest);
        int[] c1Arr = {c1.getAlpha(), c1.getRed(), c1.getGreen(), c1.getBlue()};
        int[] c2Arr = {c2.getAlpha(), c2.getRed(), c2.getGreen(), c2.getBlue()};

        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable lineTask = () -> calculateLine(scale, roughness, width, finalY, destData, c1Arr, c2Arr);
            futures[y] = ThreadPool.submit(lineTask);
        }
        ThreadPool.waitFor(futures, pt);
    }

    private void calculateLine(float startingScale, float roughness,
                               int width, int y, int[] destData,
                               int[] color1, int[] color2) {
        for (int x = 0; x < width; x++) {
            float scale = startingScale;
            float noiseValue = 0.0f;
            float contribution = 1.0f;

            for (int i = 0; i < 8 && contribution > 0.03f && scale > 0; i++) {
                float scaledX = x / scale;
                float scaledY = y / scale;
                float n = perlinNoise2D(scaledX, scaledY);
                noiseValue += contribution * n;
                scale /= 2;
                contribution *= roughness;
            }

            noiseValue = (1.0f + noiseValue) / 2.0f;
            if (noiseValue < 0.0f) {
                noiseValue = 0.0f;
            } else if (noiseValue > 1.0f) {
                noiseValue = 1.0f;
            }

            destData[x + y * width] = ImageUtils.lerpAndPremultiply(
                noiseValue, color1, color2);
        }
    }

    /**
     * A 2D version of the algorithm from http://mrl.nyu.edu/~perlin/noise/
     */
    private float perlinNoise2D(float x, float y) {
        // find unit grid cell containing point + wrap the integer cells at 255
        int gridX = ((int) x) & 255;
        int gridY = ((int) y) & 255;

        // get relative coordinates of point within cell
        x -= ((int) x);
        y -= ((int) y);

        // compute the fade curves for x and y
        float u = ImageMath.smootherStep01(x);
        float v = ImageMath.smootherStep01(y);

        // calculate hashed gradient indices
        int a = p[gridX] + gridY;
        int aa = p[a];
        int ab = p[a + 1];
        int b = p[gridX + 1] + gridY;
        int ba = p[b];
        int bb = p[b + 1];

        float noiseSE = grad2D(p[aa], x, y);
        float noiseSW = grad2D(p[ba], x - 1, y);
        float noiseNE = grad2D(p[ab], x, y - 1);
        float noiseNW = grad2D(p[bb], x - 1, y - 1);

        float noiseS = ImageMath.lerp(u, noiseSE, noiseSW);
        float noiseN = ImageMath.lerp(u, noiseNE, noiseNW);

        float noise = ImageMath.lerp(v, noiseS, noiseN);

        // noise is in the range [-1..1]
        return noise;
    }

    private static float grad2D(int hash, float x, float y) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : x;

        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /**
     * Fill the permutation table is with all the values with
     * between 1 and 256, in random order, and duplicate it
     */
    public void reseed(long newSeed) {
        Random random = new Random(newSeed);
        p = new int[512];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;

            // duplicate
            p[i + 256] = p[i];
        }
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}