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

import com.jhlabs.image.PixelUtils;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ColorSpaces;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.StatusBarProgressTracker;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * XYZTest filter
 */
public class XYZTest extends ParametrizedFilter {
    public static final String NAME = "XYZ Test";

    @Serial
    private static final long serialVersionUID = -7871330761287217919L;

    private final RangeParam x = new RangeParam("X", -20, 0, 20);
    private final RangeParam y = new RangeParam("Y", -20, 0, 20);
    private final RangeParam z = new RangeParam("Z", -20, 0, 20);
    private final BooleanParam linRGB = new BooleanParam("Linearize");

//    private static final ColorSpace XYZ_CS = ColorSpace.getInstance(ColorSpace.CS_CIEXYZ);

    public XYZTest() {
        super(true);

        initParams(
            x,
            y,
            z,
            linRGB
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        dest = ImageUtils.copyImage(src);
        int[] pixels = ImageUtils.getPixels(dest);

        double[] rgb = {0.0, 0.0, 0.0};
        double[] xyz = {0.0, 0.0, 0.0};

        double xVal = x.getPercentage();
        double yVal = y.getPercentage();
        double zVal = z.getPercentage();
        boolean linearize = linRGB.isChecked();

        int numPixels = pixels.length;
        var pt = new StatusBarProgressTracker(NAME, 100);
        int workUnit = numPixels / 100;
        int workDone = 0;

        for (int i = 0; i < numPixels; i++) {
            int pixel = pixels[i];
            int a = (pixel >>> 24) & 0xFF;

            int r = (pixel >>> 16) & 0xFF;
            int g = (pixel >>> 8) & 0xFF;
            int b = pixel & 0xFF;

            if (linearize) {
                rgb[0] = ColorSpaces.SRGB_TO_LINEAR_LUT[r];
                rgb[1] = ColorSpaces.SRGB_TO_LINEAR_LUT[g];
                rgb[2] = ColorSpaces.SRGB_TO_LINEAR_LUT[b];
            } else {
                rgb[0] = r / 255.0;
                rgb[1] = g / 255.0;
                rgb[2] = b / 255.0;
            }

            //float[] xyz = XYZ_CS.fromRGB(rgb);
            rgb2xyz(rgb, xyz);

            xyz[0] += xVal;
            xyz[1] += yVal;
            xyz[2] += zVal;

            xyz[0] = Math.clamp(xyz[0], 0.0, 1.0);
            xyz[1] = Math.clamp(xyz[1], 0.0, 1.0);
            xyz[2] = Math.clamp(xyz[2], 0.0, 1.0);

//            rgb = XYZ_CS.toRGB(xyz);
            xyz2rgb(xyz, rgb);

            if (linearize) {
                r = ColorSpaces.linearToSRGBInt(rgb[0]);
                g = ColorSpaces.linearToSRGBInt(rgb[1]);
                b = ColorSpaces.linearToSRGBInt(rgb[2]);
            } else {
                r = (int) (rgb[0] * 255);
                g = (int) (rgb[1] * 255);
                b = (int) (rgb[2] * 255);
            }

            r = PixelUtils.clamp(r);
            g = PixelUtils.clamp(g);
            b = PixelUtils.clamp(b);

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

    @Override
    public boolean supportsGray() {
        return false;
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

    public static double[] rgb2xyz(double[] rgb, double[] xyz) {
        double[] rgbLin = rgb;

        // Convert linear sRGB with D50 white point to CIE XYZ
        for (int i = 0; i < 3; i++) {
            xyz[i] = MATRIX_SRGB2XYZ_D50[i * 3] * rgbLin[0] +
                     MATRIX_SRGB2XYZ_D50[i * 3 + 1] * rgbLin[1] +
                     MATRIX_SRGB2XYZ_D50[i * 3 + 2] * rgbLin[2];
        }

        return xyz;
    }

    public static double[] xyz2rgb(double[] xyz, double[] rgb) {
        // XYZ to linear sRGB with D50 white point
        for (int i = 0; i < 3; i++) {
            rgb[i] = MATRIX_XYZ2SRGB_D50[i * 3] * xyz[0] +
                     MATRIX_XYZ2SRGB_D50[i * 3 + 1] * xyz[1] +
                     MATRIX_XYZ2SRGB_D50[i * 3 + 2] * xyz[2];
        }

        return rgb;
    }
}
