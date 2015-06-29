/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
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
package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.OilFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.ResizingHelper;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;

import java.awt.image.BufferedImage;

/**
 * Oil Painting based on the JHLabs OilFilter
 */
public class JHOilPainting extends FilterWithParametrizedGUI {
    private final GroupedRangeParam brushSize = new GroupedRangeParam("Brush Size", 0, 10, 1, false);
    private final RangeParam coarseness = new RangeParam("Coarseness", 2, 255, 25);
    private final IntChoiceParam detailQuality = ResizingHelper.createQualityParam();

    private OilFilter filter;

    public JHOilPainting() {
        super("Oil Painting", true, false);
        setParamSet(new ParamSet(
                brushSize.adjustRangeToImageSize(0.04),
                coarseness,
                detailQuality
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        int brushX = brushSize.getValue(0);
        int brushY = brushSize.getValue(1);
        if (brushX == 0 && brushY == 0) {
            return src;
        }

        if (filter == null) {
            filter = new OilFilter();
        }

        filter.setLevels(coarseness.getValue());

        ResizingHelper r = new ResizingHelper(src);
        if (r.shouldResize()) {
            double resizeFactor = r.getResizeFactor();
            filter.setRangeX((int) (brushX / resizeFactor));
            filter.setRangeY((int) (brushY / resizeFactor));
            dest = r.invoke(detailQuality.getValue(), filter);
        } else {
            // normal case, no resizing
            filter.setRangeX(brushX);
            filter.setRangeY(brushY);
            dest = filter.filter(src, dest);
        }

        return dest;
    }

    @Override
    public boolean excludeFromAnimation() {
        return true;
    }

}