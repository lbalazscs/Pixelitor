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

import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.ComplexFractalImpl;

import java.awt.image.BufferedImage;

/**
 * Renders a Julia Set, see https://en.wikipedia.org/wiki/Julia_set
 */
public class JuliaSet extends ComplexFractal {
    public static final String NAME = "Julia Set";
    private JuliaSetImpl filter;

    private final GroupedRangeParam cParam = new GroupedRangeParam("Complex Constant (*100)",
            new RangeParam[]{
                    new RangeParam("Re", -150, -70, 50),
                    new RangeParam("Im", -150, 27, 50)
            }, false);

    public JuliaSet() {
        super(300, 0.22f);

        insertParamAtIndex(
                cParam.notLinkable().withDecimalPlaces(2),
                3);
    }

    @Override
    public BufferedImage doTransformAA(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new JuliaSetImpl();
        }

        filter.setZoom(zoomParam.getZoomRatio());
        filter.setZoomCenter(zoomCenter.getRelativeX(), zoomCenter.getRelativeY());

        int iterations = iterationsParam.getValue();
        filter.setColors(createColors(iterations));
        filter.setMaxIterations(iterations);

        filter.setCx(cParam.getValueAsDPercentage(0));
        filter.setCy(cParam.getValueAsDPercentage(1));

        return filter.filter(src, dest);
    }
}

class JuliaSetImpl extends ComplexFractalImpl {
    private double cx;
    private double cy;

    protected JuliaSetImpl() {
        super(JuliaSet.NAME, -2.0f,  2.0f, -1.2f, 1.2f);
    }

    @Override
    public int filterRGB(int x, int y, int rgb) {
        double zx = cxStart + x * xMultiplier;
        double zy = cyStart + y * yMultiplier;

        return calcIteratedColor(zx, zy, cx, cy);
    }

    public void setCx(double cx) {
        this.cx = cx;
    }

    public void setCy(double cy) {
        this.cy = cy;
    }
}


