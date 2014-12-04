/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.filters;

import com.jhlabs.image.TransformFilter;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.impl.SliceFilter;

import java.awt.image.BufferedImage;

/**
 * Slice based on SliceFilter
 */
public class Slice extends FilterWithParametrizedGUI {
    private RangeParam sizeParam = new RangeParam("Size", 0, 100, 6);
    private RangeParam offsetParam = new RangeParam("Offset", 0, 100, 10);
    private RangeParam shiftHParam = new RangeParam("Shift Effect Horizontal", 0, 100, 0);
    private RangeParam shiftVParam = new RangeParam("Shift Effect Vertical", 0, 100, 0);
    private IntChoiceParam edgeActionParam =  IntChoiceParam.getEdgeActionChoices();

    private SliceFilter filter;

    public Slice() {
        super("Slice", true, false);
        setParamSet(new ParamSet(
                sizeParam.adjustRangeToImageSize(0.25),
                offsetParam.adjustRangeToImageSize(0.25),
                shiftHParam,
                shiftVParam,
                edgeActionParam
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if(filter == null) {
            filter = new SliceFilter();
        }

        filter.setOffset(offsetParam.getValue());
        filter.setShiftHorizontal(shiftHParam.getValueAsPercentage());
        filter.setShiftVertical(shiftVParam.getValueAsPercentage());
        filter.setSize(sizeParam.getValue());
        filter.setEdgeAction(edgeActionParam.getValue());
        filter.setInterpolation(TransformFilter.NEAREST_NEIGHBOUR); // no difference

        dest = filter.filter(src, dest);
        return dest;
    }
}