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
 * Renders value noise
 */
public class ValueNoise extends FilterWithParametrizedGUI {
    public static final String NAME = "Value Noise";

    private static final Random rand = new Random();
    private static int r1;
    private static int r2;
    private static int r3;

    static {
        reseed();
    }

    private final RangeParam scale = new RangeParam("Zoom", 5, 100, 300);
    private final RangeParam details = new RangeParam("Octaves (Details)", 1, 5, 8);

    private final ActionSetting reseedAction = new ReseedNoiseActionSetting(e -> {
        reseed();
    });

    private final ColorParam color1 = new ColorParam("Color 1", BLACK, USER_ONLY_OPACITY);
    private final ColorParam color2 = new ColorParam("Color 2", WHITE, USER_ONLY_OPACITY);

    public ValueNoise() {
        super(ShowOriginal.NO);

        setParamSet(new ParamSet(
                scale.adjustRangeToImageSize(0.3),
                details,
                color1,
                color2
        ).withAction(reseedAction));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int[] lookupTable = new int[256];
        Color c1 = color1.getColor();
        Color c2 = color2.getColor();
        int[] colorArray1 = {c1.getAlpha(), c1.getRed(), c1.getGreen(), c1.getBlue()};
        int[] colorArray2 = {c2.getAlpha(), c2.getRed(), c2.getGreen(), c2.getBlue()};

        for (int i = 0, lookupTableLength = lookupTable.length; i < lookupTableLength; i++) {
            lookupTable[i] = ImageUtils.lerpAndPremultiplyColorWithAlpha(i / 255.0f, colorArray1, colorArray2);
        }

        int[] destData = ImageUtils.getPixelsAsArray(dest);
        int width = dest.getWidth();
        int height = dest.getHeight();
        float frequency = 1.0f / scale.getValueAsFloat();

        float persistence = 0.6f;
        float amplitude = 1.0f;

        ProgressTracker pt = new BasicProgressTracker(NAME, height);

        Future<?>[] futures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable lineTask = () -> calculateLine(lookupTable, destData, width, frequency, persistence, amplitude, finalY);
            futures[y] = ThreadPool.submit(lineTask);
        }
        ThreadPool.waitForFutures(futures, pt);

        pt.finish();

        return dest;
    }

    private void calculateLine(int[] lookupTable, int[] destData, int width, float frequency, float persistence, float amplitude, int y) {
        for (int x = 0; x < width; x++) {
            int octaves = details.getValue();

            int noise = (int) (255 * generateValueNoise(x, y, octaves, frequency, persistence, amplitude));

            int value = lookupTable[noise];
            destData[x + y * width] = value;
        }
    }

    /**
     * Returns a float between 0 and 1
     */
    @SuppressWarnings("WeakerAccess")
    public static float generateValueNoise(int x, int y, int octaves, float frequency, float persistence, float amplitude) {
        float total = 0.0f;

        for (int lcv = 0; lcv < octaves; lcv++) {
            total += smooth(x * frequency, y * frequency) * amplitude;
            frequency *= 2;
            amplitude *= persistence;
        }

        if (total < 0) {
            total = 0.0f;
        }
        if (total > 1) {
            total = 1.0f;
        }

        return total;
    }

    private static float smooth(float x, float y) {
        float n1 = noise((int) x, (int) y);
        float n2 = noise((int) x + 1, (int) y);
        float n3 = noise((int) x, (int) y + 1);
        float n4 = noise((int) x + 1, (int) y + 1);

        float i1 = interpolate(n1, n2, x - (int) x);
        float i2 = interpolate(n3, n4, x - (int) x);

        return interpolate(i1, i2, y - (int) y);
    }

    public static void reseed() {
        r1 = 1000 + rand.nextInt(90000);
        r2 = 10000 + rand.nextInt(900000);
        r3 = 100000 + rand.nextInt(1000000000);
    }

    private static float noise(int x, int y) {
        int n = x + y * 57;
        n = (n << 13) ^ n;

        return (1.0f - ((n * (n * n * r1 + r2) + r3) & 0x7fffffff) / 1073741824.0f);
    }

    private static float interpolate(float x, float y, float a) {
//        float val = (float) ((1 - FastMath.cos(a * Math.PI)) * 0.5);

        // the smooth step is very similar but much faster than the cosine interpolation
        // http://en.wikipedia.org/wiki/Smoothstep
        // http://www.wolframalpha.com/input/?i=Plot[{3+*+a+*+a+-+2+*+a+*+a+*a%2C+%281+-+Cos[a+*+Pi]%29+*+0.5}%2C+{a%2C+0%2C+1}]
        float val = a * a * (3 - 2 * a);

        return x * (1 - val) + y * val;
    }

    public void setDetails(int newDetails) {
        details.setValue(newDetails);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}