/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.gui.ActionSetting;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ReseedNoiseActionSetting;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.BasicProgressTracker;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.ProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.Future;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.ColorParam.OpacitySetting.USER_ONLY_OPACITY;

/**
 * Clouds filter based on multiple Perlin noise iterations, inspired by the Paint.net clouds
 */
public class Clouds extends FilterWithParametrizedGUI {
    public static final String NAME = "Clouds";

    private static int[] p;

    static {
        reseed();
    }

    private final RangeParam scale = new RangeParam("Zoom", 3, 100, 300);
    private final RangeParam roughness = new RangeParam("Roughness (%)", 1, 50, 100);

    private final ColorParam color1 = new ColorParam("Color 1", BLACK, USER_ONLY_OPACITY);
    private final ColorParam color2 = new ColorParam("Color 2", WHITE, USER_ONLY_OPACITY);

    private final ActionSetting reseedAction = new ReseedNoiseActionSetting(e -> {
        reseed();
    });

    public Clouds() {
        super(ShowOriginal.NO);

        setParamSet(new ParamSet(
                scale.adjustRangeToImageSize(0.3),
                roughness,
                color1,
                color2
        ).withAction(reseedAction));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        ProgressTracker pt = new BasicProgressTracker(NAME, src.getHeight());

        renderClouds(dest,
                scale.getValueAsFloat(),
                roughness.getValueAsPercentage(),
                color1.getColor(),
                color2.getColor(),
                pt);

        pt.finish();
        return dest;
    }

    public static void renderClouds(BufferedImage dest, float scaleValue, float roughnessValue, Color c1, Color c2, ProgressTracker pt) {
        int width = dest.getWidth();
        int height = dest.getHeight();
        int[] destData = ImageUtils.getPixelsAsArray(dest);
        int[] color1 = {c1.getAlpha(), c1.getRed(), c1.getGreen(), c1.getBlue()};
        int[] color2 = {c2.getAlpha(), c2.getRed(), c2.getGreen(), c2.getBlue()};

        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable lineTask = () -> calculateLine(scaleValue, roughnessValue, width, destData, color1, color2, finalY);
            futures[y] = ThreadPool.submit(lineTask);
        }
        ThreadPool.waitForFutures(futures, pt);
    }

    private static void calculateLine(float scaleValue, float roughnessValue, int width, int[] destData, int[] color1, int[] color2, int y) {
        for (int x = 0; x < width; x++) {
            float scale = scaleValue;
            float noiseValue = 0.0f;
            float contribution = 1.0f;

            for (int i = 0; (i < 8) && (contribution > 0.03f) && (scale > 0); i++) {
                float scaledX = x / scale;
                float scaledY = y / scale;
                float n = perlinNoise2D(scaledX, scaledY);
                noiseValue += contribution * n;
                scale /= 2;
                contribution *= roughnessValue;
            }

            noiseValue = (1.0f + noiseValue) / 2.0f;
            if (noiseValue < 0.0f) {
                noiseValue = 0.0f;
            } else if (noiseValue > 1.0f) {
                noiseValue = 1.0f;
            }

            destData[x + y * width] = ImageUtils.lerpAndPremultiplyColorWithAlpha(noiseValue, color1, color2);
        }
    }

    /**
     * A 2D version of the algorithm from http://mrl.nyu.edu/~perlin/noise/
     */
    private static float perlinNoise2D(float x, float y) {
        // find unit grid cell containing point + wrap the integer cells at 255
        int gridX = ((int) x) & 255;
        int gridY = ((int) y) & 255;

        // get relative coordinates of point within cell
        x -= ((int) x);
        y -= ((int) y);

        // compute the fade curves for x and y
        float u = fade(x);
        float v = fade(y);

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

    // a smooth interpolation between 0 and 1
    // see http://en.wikipedia.org/wiki/Smoothstep
    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

//    // permutation table that contains the integers from 0 to 255 in random order
//    static final int permutation[] = {151, 160, 137, 91, 90, 15,
//            131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23,
//            190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33,
//            88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166,
//            77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244,
//            102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196,
//            135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123,
//            5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42,
//            223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
//            129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228,
//            251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107,
//            49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254,
//            138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180
//    };
//
//    // permutation table doubled
//    static int p[] = new int[512];
//        static {
//        for (int i = 0; i < 256; i++) {
//            p[i] = permutation[i];
//            p[256 + i] = permutation[i];
//        }
//    }

    private static float grad2D(int hash, float x, float y) {
        int h = hash & 15;
        float u = h < 8 ? x : y;
        float v = h < 4 ? y : x;

        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /**
     * Fill the permutation table is with all the values with between 1 to 256,
     * in random order, and duplicate that
     */
    public static void reseed() {
        p = new int[512];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        Random random = new Random();

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