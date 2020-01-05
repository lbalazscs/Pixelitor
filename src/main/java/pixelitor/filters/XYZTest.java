/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

import net.jafama.FastMath;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;

/**
 * XYZTest filter
 */
public class XYZTest extends ParametrizedFilter {
    public static final String NAME = "XYZ Test";

    private final RangeParam x = new RangeParam("X", -20, 0, 20);
    private final RangeParam y = new RangeParam("Y", -20, 0, 20);
    private final RangeParam z = new RangeParam("Z", -20, 0, 20);
    private final BooleanParam linRGB = new BooleanParam("Linearize", false);

    private static final ColorSpace XYZ_CS = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

    public XYZTest() {
        super(ShowOriginal.YES);

        setParams(
                x,
                y,
                z,
                linRGB
        );
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);
        int[] pixels = ImageUtils.getPixelsAsArray(dest);

        double[] rgb = {0.0f, 0.0f, 0.0f};
        double[] xyz = {0.0f, 0.0f, 0.0f};

        float xVal = x.getPercentageValF();
        float yVal = y.getPercentageValF();
        float zVal = z.getPercentageValF();
        boolean linearize = linRGB.isChecked();

        int numPixels = pixels.length;
        var pt = new StatusBarProgressTracker(NAME, 100);
        int workUnit = numPixels / 100;
        int workDone = 0;

        for (int i = 0; i < numPixels; i++) {
            int pixel = pixels[i];
            int a = (pixel >>> 24) & 0xFF;

            rgb[0] = (pixel >>> 16) & 0xFF;
            rgb[1] = (pixel >>> 8) & 0xFF;
            rgb[2] = pixel & 0xFF;

            rgb[0] /= 256.0f;
            rgb[1] /= 256.0f;
            rgb[2] /= 256.0f;

            //float[] xyz = XYZ_CS.fromRGB(rgb);
            rgb2xyz(rgb, xyz, linearize);

            xyz[0] += xVal;
            xyz[1] += yVal;
            xyz[2] += zVal;

            if (xyz[0] > 1) {
                xyz[0] = 1;
            }
            if (xyz[1] > 1) {
                xyz[1] = 1;
            }
            if (xyz[2] > 1) {
                xyz[2] = 1;
            }
            if (xyz[0] < 0) {
                xyz[0] = 0;
            }
            if (xyz[1] < 0) {
                xyz[1] = 0;
            }
            if (xyz[2] < 0) {
                xyz[2] = 0;
            }

//            rgb = XYZ_CS.toRGB(xyz);
            xyz2rgb(xyz, rgb, linearize);

            int r = (int) (rgb[0] * 256);
            int g = (int) (rgb[1] * 256);
            int b = (int) (rgb[2] * 256);

            if (r > 255) {
                r = 255;
            }
            if (g > 255) {
                g = 255;
            }
            if (b > 255) {
                b = 255;
            }
            if (r < 0) {
                r = 0;
            }
            if (g < 0) {
                g = 0;
            }
            if (b < 0) {
                b = 0;
            }

            pixels[i] = a << 24 | r << 16 | g << 8 | b;

            workDone++;
            if (workDone > workUnit) {
                workDone = 0;
                pt.unitDone();
            }
        }

        pt.finished();

        return dest;
    }

    /**
     * sRGB to CIE XYZ conversion matrix. See
     * http://www.brucelindbloom.com/index.html?WorkingSpaceInfo.html#Specifications
     **/
    private static final double[] MATRIX_SRGB2XYZ_D50 = {
            0.436052025, 0.385081593, 0.143087414,
            0.222491598, 0.716886060, 0.060621486,
            0.013929122, 0.097097002, 0.714185470
    };

    /**
     * CIE XYZ to sRGB conversion matrix. See
     * http://www.brucelindbloom.com/index.html?WorkingSpaceInfo.html#Specifications
     **/
    private static final double[] MATRIX_XYZ2SRGB_D50 = {
            3.1338561, -1.6168667, -0.4906146,
            -0.9787684, 1.9161415, 0.0334540,
            0.0719453, -0.2289914, 1.4052427
    };

    public static double[] rgb2xyz(double[] rgb, double[] xyz, boolean linearize) {
        double[] rgbLin = rgb;
        if (linearize) {
            // Remove sRGB companding to make RGB components linear
            rgbLin = new double[3];
            for (int i = 0; i < 3; i++) {
                if (rgb[i] <= 0.04045) {
                    rgbLin[i] = rgb[i] / 12.92;
                } else {
                    rgbLin[i] = FastMath.powQuick((rgb[i] + 0.055) / 1.055, 2.4);
                }
            }
        }

        // Convert linear sRGB with D50 white point to CIE XYZ
        for (int i = 0; i < 3; i++) {
            xyz[i] = MATRIX_SRGB2XYZ_D50[i * 3 + 0] * rgbLin[0] +
                    MATRIX_SRGB2XYZ_D50[i * 3 + 1] * rgbLin[1] +
                    MATRIX_SRGB2XYZ_D50[i * 3 + 2] * rgbLin[2];
        }

        return xyz;
    }

    public static double[] xyz2rgb(double[] xyz, double[] rgb, boolean linearize) {
        // XYZ to linear sRGB with D50 white point
        for (int i = 0; i < 3; i++) {
            rgb[i] = MATRIX_XYZ2SRGB_D50[i * 3 + 0] * xyz[0] +
                    MATRIX_XYZ2SRGB_D50[i * 3 + 1] * xyz[1] +
                    MATRIX_XYZ2SRGB_D50[i * 3 + 2] * xyz[2];
        }

        if (linearize) {
            // Apply sRGB companding
            for (int i = 0; i < 3; i++) {
                if (rgb[i] <= 0.0031308) {
                    rgb[i] = 12.92 * rgb[i];
                } else {
                    rgb[i] = 1.055 * FastMath.powQuick(rgb[i], 1.0 / 2.4) - 0.055;
                }
            }
        }

        return rgb;
    }
}