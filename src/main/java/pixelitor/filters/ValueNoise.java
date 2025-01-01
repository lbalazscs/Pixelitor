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

import com.jhlabs.image.ImageMath;
import pixelitor.ThreadPool;
import pixelitor.filters.gui.AngleParam;
import pixelitor.filters.gui.ColorParam;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.util.NoiseInterpolation;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.Random;
import java.util.concurrent.Future;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static pixelitor.filters.gui.TransparencyPolicy.USER_ONLY_TRANSPARENCY;
import static pixelitor.gui.GUIText.ZOOM;

/**
 * Renders value noise
 */
public class ValueNoise extends ParametrizedFilter {
    public static final String NAME = "Value Noise";

    @Serial
    private static final long serialVersionUID = -8523545931426941979L;

    private final Random rand = new Random();
    private int r1;
    private int r2;
    private int r3;

    private float cx;
    private float cy;
    private boolean rotate;
    private double cos;
    private double sin;

    private final RangeParam scale = new RangeParam(ZOOM, 5, 100, 300);
    private final AngleParam angleParam = new AngleParam("Rotate", 0);

    private final RangeParam details = new RangeParam("Octaves (Details)", 1, 5, 8);
    private final RangeParam persistenceParam =
        new RangeParam("Roughness (%)", 0, 60, 100);
    private final EnumParam<NoiseInterpolation> interpolation
        = new EnumParam<>("Interpolation", NoiseInterpolation.class);

    private final ColorParam color1 = new ColorParam("Color 1", BLACK, USER_ONLY_TRANSPARENCY);
    private final ColorParam color2 = new ColorParam("Color 2", WHITE, USER_ONLY_TRANSPARENCY);

    public ValueNoise() {
        super(false);

        scale.setPresetKey("Zoom");
        setParams(
            scale.withAdjustedRange(0.3),
            angleParam,
            details,
            persistenceParam,
            interpolation.withDefault(NoiseInterpolation.CUBIC),
            color1,
            color2
        ).withAction(paramSet.createReseedAction(this::reseed));

        helpURL = "https://en.wikipedia.org/wiki/Value_noise";

        reseed(paramSet.getLastSeed());
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int[] lookupTable = new int[256];
        Color c1 = color1.getColor();
        Color c2 = color2.getColor();
        int[] colorArray1 = {c1.getAlpha(), c1.getRed(), c1.getGreen(), c1.getBlue()};
        int[] colorArray2 = {c2.getAlpha(), c2.getRed(), c2.getGreen(), c2.getBlue()};

        for (int i = 0, lookupTableLength = lookupTable.length; i < lookupTableLength; i++) {
            lookupTable[i] = ImageUtils.lerpAndPremultiply(
                i / 255.0f, colorArray1, colorArray2);
        }

        int[] destData = ImageUtils.getPixelArray(dest);
        int width = dest.getWidth();
        int height = dest.getHeight();
        cx = width / 2.0f;
        cy = height / 2.0f;

        double angle = angleParam.getValueInRadians();
        rotate = angle != 0;
        if (rotate) {
            cos = Math.cos(angle);
            sin = Math.sin(angle);
        }

        float frequency = 1.0f / scale.getValueAsFloat();
        float persistence = (float) persistenceParam.getPercentage();

        var pt = new StatusBarProgressTracker(NAME, height);
        NoiseInterpolation interp = interpolation.getSelected();

        Future<?>[] rowFutures = new Future[height];
        for (int y = 0; y < height; y++) {
            int finalY = y;
            Runnable rowTask = () -> processRow(lookupTable, destData,
                width, frequency, persistence, finalY, interp);
            rowFutures[y] = ThreadPool.submit(rowTask);
        }
        ThreadPool.waitFor(rowFutures, pt);

        pt.finished();

        return dest;
    }

    private void processRow(int[] lookupTable, int[] destData,
                            int width, float frequency, float persistence,
                            int y, NoiseInterpolation interp) {
        float outerY = y - cy;
        for (int x = 0; x < width; x++) {
            int octaves = details.getValue();
            float sampleX = x - cx;
            float sampleY = outerY; // must be reset, because rotation modifies it

            if (rotate) {
                double newX = cos * sampleX + sin * sampleY;
                double newY = -sin * sampleX + cos * sampleY;
                sampleX = (float) newX;
                sampleY = (float) newY;
            }

            int noise = (int) (255 * generateValueNoise(sampleX, sampleY,
                octaves, frequency, persistence, interp));

            int value = lookupTable[noise];
            destData[x + y * width] = value;
        }
    }

    /**
     * Returns a float between 0 and 1
     */
    @SuppressWarnings("WeakerAccess")
    public float generateValueNoise(float x, float y,
                                    int octaves,
                                    float frequency,
                                    float persistence,
                                    NoiseInterpolation interp) {
        float total = 0.0f;

        float amplitude = 1.0f;
        for (int i = 0; i < octaves; i++) {
            total += smooth(x * frequency, y * frequency, interp) * amplitude;
            frequency *= 2;
            amplitude *= persistence;
        }

        return ImageMath.clamp01(total);
    }

    private float smooth(float x, float y, NoiseInterpolation interp) {
        int xx = fastFloor(x);
        int yy = fastFloor(y);
        int xxp = xx + 1;
        int yyp = yy + 1;

        float n1 = noise(xx, yy);
        float n2 = noise(xxp, yy);
        float n3 = noise(xx, yyp);
        float n4 = noise(xxp, yyp);

        float i1 = interpolate(n1, n2, x - xx, interp);
        float i2 = interpolate(n3, n4, x - xx, interp);
        return interpolate(i1, i2, y - yy, interp);
    }

    private static int fastFloor(float f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }

    private void reseed(long newSeed) {
        rand.setSeed(newSeed);
        r1 = 1000 + rand.nextInt(90000);
        r2 = 10000 + rand.nextInt(900000);
        r3 = 100000 + rand.nextInt(1000000000);
    }

    // Creates a random number between 0 and 1, and guarantees that
    // it always generates the same number for a given (x, y) pair.
    private float noise(int x, int y) {
        int n = x + y * 57;
        n = (n << 13) ^ n;

        // 0x7F_FF_FF_FF is Integer.MAX_VALUE: ensures that the value is positive.
        // The final division ensures that the result is scaled to the range 0..1.
        return (1.0f - ((n * (n * n * r1 + r2) + r3) & 0x7F_FF_FF_FF) / 1.0737418E+9f);
    }

    private static float interpolate(float x, float y, float a, NoiseInterpolation interp) {
        float t = interp.step(a);
        return ImageMath.lerp(t, x, y);
    }

    public void setDetails(int newDetails) {
        details.setValue(newDetails);
    }

    @Override
    public boolean supportsGray() {
        return false;
    }
}