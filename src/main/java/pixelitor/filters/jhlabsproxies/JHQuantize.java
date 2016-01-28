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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.QuantizeFilter;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.RangeParam;
import pixelitor.filters.gui.ShowOriginal;
import pixelitor.utils.Utils;

import java.awt.image.BufferedImage;

/**
 * Quantize based on the JHLabs QuantizeFilter
 */
public class JHQuantize extends FilterWithParametrizedGUI {
    public static final String NAME = "Quantize";

    private final RangeParam numberOfColors = new RangeParam("Number of Colors", 2, 2, 256);
    private final BooleanParam dither = new BooleanParam("Dither", false);
    private final BooleanParam serpentine = new BooleanParam("Dither with Serpentine", false);

    private QuantizeFilter filter;

    public JHQuantize() {
        super(ShowOriginal.YES);

        // enable serpentine only if dither is checked
        Utils.setupEnableOtherIf(dither, serpentine, checked -> checked);

        setParamSet(new ParamSet(
                numberOfColors,
                dither,
                serpentine
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new QuantizeFilter(NAME);
        }

        filter.setNumColors(numberOfColors.getValue());
        filter.setDither(dither.isChecked());
        filter.setSerpentine(serpentine.isChecked());

        dest = filter.filter(src, dest);
        return dest;
    }

    @Override
    public boolean excludeFromAnimation() {
        return true;
    }
}