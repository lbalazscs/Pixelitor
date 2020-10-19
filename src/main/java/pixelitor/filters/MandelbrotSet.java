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

import pixelitor.filters.impl.ComplexFractalImpl;

import java.awt.image.BufferedImage;

/**
 * Renders a Mandelbrot Set, see https://en.wikipedia.org/wiki/Mandelbrot_set
 */
public class MandelbrotSet extends ComplexFractal {
    public static final String NAME = "Mandelbrot Set";
    private MandelbrotSetImpl filter;

    public MandelbrotSet() {
        super(100, 0.2028f);

        helpURL = "https://en.wikipedia.org/wiki/Mandelbrot_set";
    }

    @Override
    public BufferedImage doTransformAA(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new MandelbrotSetImpl();
        }

        filter.setZoom(zoomParam.getZoomRatio());
        filter.setZoomCenter(zoomCenter.getRelativeX(), zoomCenter.getRelativeY());

        int iterations = iterationsParam.getValue();
        filter.setColors(createColors(iterations));
        filter.setMaxIterations(iterations);

        return filter.filter(src, dest);
    }
}

class MandelbrotSetImpl extends ComplexFractalImpl {
    protected MandelbrotSetImpl() {
        super(MandelbrotSet.NAME, -2.2f, 0.7f, -1.2f, 1.2f);
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        // start with the complex number (0, 0)
        double zx = 0;
        double zy = 0;

        // the complex constant c, mapped from image coordinates
        double cx = cxStart + x * xMultiplier;
        double cy = cyStart + y * yMultiplier;

        // before going into the escape time algorithm,
        // first two possible shortcuts:

        // 1. check if the point is in the period-2 bulb
        if (cx < -0.75 && cx > -1.25 && cy < 0.28 && cy > -0.28) { // approx. check
            if ((cx + 1) * (cx + 1) + cy * cy < 1 / 16.0) { // exact check
                return colors[0];
            }
        }

        // 2. check if the point is inside the main cardioid
        if (cx > -0.75 && cx < 0.37 && cy < 0.65 && cy > -0.65) { // approx. check
            double cm = cx - 1 / 4.0;
            double cy2 = cy * cy;
            double q = cm * cm + cy2;
            if (q * (q + cm) < cy2 / 4.0) { // exact check
                return colors[0];
            }
        }

        return calcIteratedColor(zx, zy, cx, cy);
    }
}


