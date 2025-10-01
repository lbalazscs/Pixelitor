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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.Help;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.ComplexFractalImpl;

import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * Renders a Julia-type set, where z is varied across pixels and the constant c is fixed.
 */
public class JuliaSet extends ComplexFractal {
    public static final String NAME = "Julia Set";

    @Serial
    private static final long serialVersionUID = -3089167245262580096L;

    private JuliaSetImpl filter;

    private final GroupedRangeParam cParam = new GroupedRangeParam("Complex Constant (*100)",
        new RangeParam[]{
            new RangeParam("Re", -150, -70, 50),
            new RangeParam("Im", -150, 27, 50)
        }, false);

    public JuliaSet() {
        super(300, 0.22f);

        insertParam(cParam.notLinkable().withDecimalPlaces(2), 3);

        help = Help.fromWikiURL("https://en.wikipedia.org/wiki/Julia_set");
    }

    @Override
    public BufferedImage renderFractal(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new JuliaSetImpl();
        }

        filter.setIterator(createIterator());
        filter.setZoom(zoomParam.getZoomRatio());
        filter.setZoomCenter(zoomCenter.getRelativeX(), zoomCenter.getRelativeY());

        int iterations = iterationsParam.getValue();
        filter.setColors(getColors(colorsParam.getValue(), iterations));
        filter.setMaxIterations(iterations);

        filter.setCx(cParam.getPercentage(0));
        filter.setCy(cParam.getPercentage(1));
        filter.setInsideOut(insideOutParam.isChecked());

        return filter.filter(src, dest);
    }
}

class JuliaSetImpl extends ComplexFractalImpl {
    private double cx;
    private double cy;
    private boolean insideOut;

    protected JuliaSetImpl() {
        super(JuliaSet.NAME, -2.0f, 2.0f, -1.2f, 1.2f);
    }

    @Override
    public int processPixel(int x, int y, int rgb) {
        // for Julia-type sets, the initial z is mapped from the pixel's image coordinates
        double zx = cxStart + x * xMultiplier;
        double zy = cyStart + y * yMultiplier;

        if (insideOut) {
            // invert the initial z value using f(z) = 1/z
            double d = zx * zx + zy * zy;
            if (d == 0) {
                // z0 is at the origin, so 1/z0 is at infinity => escape immediately
                return colors[colors.length - 1];
            }
            // use the inverted z0' az z0
            zx = zx / d;
            zy = -zy / d;
        }

        // the complex constant c is fixed for the entire image
        return calcIteratedColor(zx, zy, cx, cy);
    }

    public void setCx(double cx) {
        this.cx = cx;
    }

    public void setCy(double cy) {
        this.cy = cy;
    }

    public void setInsideOut(boolean insideOut) {
        this.insideOut = insideOut;
    }
}
