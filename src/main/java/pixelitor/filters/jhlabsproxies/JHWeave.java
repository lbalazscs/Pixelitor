/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import com.jhlabs.image.WeaveFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Value;
import pixelitor.filters.gui.ParamSet;
import pixelitor.filters.gui.ShowOriginal;

import java.awt.image.BufferedImage;

import static com.jhlabs.image.WeaveFilter.BASKET_PATTERN;
import static com.jhlabs.image.WeaveFilter.CROWFOOT_PATTERN;
import static com.jhlabs.image.WeaveFilter.PLAIN_PATTERN;
import static com.jhlabs.image.WeaveFilter.TWILL_PATTERN;
import static pixelitor.filters.gui.RandomizePolicy.IGNORE_RANDOMIZE;

/**
 * Weave filter based on the JHLabs WeaveFilter
 */
public class JHWeave extends ParametrizedFilter {
    public static final String NAME = "Weave";

    private final IntChoiceParam pattern = new IntChoiceParam("Pattern", new Value[]{
            new Value("Plain", PLAIN_PATTERN),
            new Value("Basket", BASKET_PATTERN),
            new Value("Twill", TWILL_PATTERN),
            new Value("Crowfoot", CROWFOOT_PATTERN),
    });
    private final GroupedRangeParam size = new GroupedRangeParam("Size", "Width", "Height", 0, 16, 100, true);
    private final GroupedRangeParam gap = new GroupedRangeParam("Gap", 0, 6, 100);
    private final BooleanParam roundThreads = new BooleanParam("Round Threads", false);
    private final BooleanParam shadeCrossings = new BooleanParam("Shade Crossings", true);
    private final BooleanParam useImageColors = new BooleanParam("Use Image Colors", true, IGNORE_RANDOMIZE);

    private WeaveFilter filter;

    public JHWeave() {
        super(ShowOriginal.YES);

        setParamSet(new ParamSet(
                pattern,
                size.withAdjustedRange(0.4),
                gap.withAdjustedRange(0.4),
                roundThreads,
                shadeCrossings,
                useImageColors
        ));
    }

    @Override
    public BufferedImage doTransform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new WeaveFilter(NAME);
        }

        filter.setPattern(pattern.getValue());
        filter.setXWidth(size.getValue(0));
        filter.setYWidth(size.getValue(1));
        filter.setXGap(gap.getValue(0));
        filter.setYGap(gap.getValue(1));
        filter.setUseImageColors(useImageColors.isChecked());
        filter.setRoundThreads(roundThreads.isChecked());
        filter.setShadeCrossings(shadeCrossings.isChecked());

        dest = filter.filter(src, dest);
        return dest;
    }
}