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

import pixelitor.filters.gui.Help;
import pixelitor.filters.impl.ComplexFractalImpl;
import pixelitor.filters.impl.ComplexFractalImpl.IterationStrategy;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Renders a Mandelbrot-type set, where the constant c is varied across pixels and z starts at 0.
 */
public class MandelbrotSet extends ComplexFractal {
    public static final String NAME = "Mandelbrot Set";

    @Serial
    private static final long serialVersionUID = 6726131928523590000L;

    public MandelbrotSet() {
        super(100, 0.2028f);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Mandelbrot_set");
    }

    @Override
    public BufferedImage renderFractal(BufferedImage src, BufferedImage dest) {
        IterationStrategy iterator = createIterator();
        Rectangle2D view = iterator.getComplexView();
        MandelbrotSetImpl filter = new MandelbrotSetImpl(view, insideOutParam.isChecked());

        filter.setIterator(iterator);
        filter.setZoom(zoomParam.getZoomRatio());
        filter.setZoomCenter(zoomCenter.getRelativeX(), zoomCenter.getRelativeY());

        int iterations = iterationsParam.getValue();
        filter.setColors(getColors(colorsParam.getValue(), iterations));
        filter.setMaxIterations(iterations);

        return filter.filter(src, dest);
    }
}

class MandelbrotSetImpl extends ComplexFractalImpl {
    private final boolean insideOut;

    protected MandelbrotSetImpl(Rectangle2D view, boolean insideOut) {
        super(MandelbrotSet.NAME,
            view.getX(), view.getX() + view.getWidth(),
            view.getY(), view.getY() + view.getHeight());
        this.insideOut = insideOut;
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // for Mandelbrot-type sets, z starts at 0
        double zx = 0;
        double zy = 0;

        // the complex constant c is mapped from the pixel's image coordinates
        double cx = cxStart + x * xMultiplier;
        double cy = cyStart + y * yMultiplier;

        if (insideOut) {
            // invert c using f(c) = 1/c
            double d = cx * cx + cy * cy;
            if (d == 0) {
                // c is at the origin, so 1/c is at infinity => escape immediately
                return colors[colors.length - 1];
            }
            // use the inverted c' as c
            cx = cx / d;
            cy = -cy / d;
        }

        // check for known regions of the set for a quick exit
        if (iterator.checkShortcuts(cx, cy)) {
            return 0xFF_00_00_00; // black, indicating the point is in the set
        }
        return calcIteratedColor(zx, zy, cx, cy);
    }
}
