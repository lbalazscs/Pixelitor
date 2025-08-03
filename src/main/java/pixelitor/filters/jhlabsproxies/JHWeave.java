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

package pixelitor.filters.jhlabsproxies;

import com.jhlabs.image.WeaveFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.gui.*;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.List;

import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * Weave filter based on the JHLabs WeaveFilter
 */
public class JHWeave extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = 6730512545695784681L;

    public static final String NAME = Texts.i18n("weave");

    public static final List<GridParam.Preset> WEAVE_PRESETS = List.of(
        new GridParam.Preset("Plain", new int[][]{
            {0, 1, 0, 1},
            {1, 0, 1, 0},
            {0, 1, 0, 1},
            {1, 0, 1, 0}
        }),
        new GridParam.Preset("Basket", new int[][]{
            {1, 1, 0, 0},
            {1, 1, 0, 0},
            {0, 0, 1, 1},
            {0, 0, 1, 1}
        }),
        new GridParam.Preset("Twill", new int[][]{
            {0, 0, 0, 1},
            {0, 0, 1, 0},
            {0, 1, 0, 0},
            {1, 0, 0, 0}
        }),
        new GridParam.Preset("Crowfoot", new int[][]{
            {0, 1, 1, 1},
            {1, 0, 1, 1},
            {1, 1, 0, 1},
            {1, 1, 1, 0}
        })
    );

    private final GridParam pattern = new GridParam("Pattern",
        WEAVE_PRESETS, GridCellPainter.createForWeave());

    private final GroupedRangeParam size = new GroupedRangeParam("Size", "Width", "Height", 0, 16, 100, true);
    private final GroupedRangeParam gap = new GroupedRangeParam("Gap", 0, 6, 100);
    private final AngleParam angle = new AngleParam("Rotate", 0);

    private final BooleanParam roundThreads = new BooleanParam("Round Threads");
    private final BooleanParam shadeCrossings = new BooleanParam("Shade Crossings", true);
    private final BooleanParam useImageColors = new BooleanParam("Use Image Colors", true, IGNORE_RANDOMIZE);

    private WeaveFilter filter;

    public JHWeave() {
        super(true);

        initParams(
            pattern,
            size.withAdjustedRange(0.4),
            gap.withAdjustedRange(0.4),
            angle,
            roundThreads,
            shadeCrossings,
            useImageColors
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        if (filter == null) {
            filter = new WeaveFilter(NAME);
        }

        filter.setPattern(pattern.getData());
        filter.setXWidth(size.getValue(0));
        filter.setYWidth(size.getValue(1));
        filter.setXGap(gap.getValue(0));
        filter.setYGap(gap.getValue(1));
        filter.setAngle(angle.getValueInIntuitiveRadians());
        filter.setUseImageColors(useImageColors.isChecked());
        filter.setRoundThreads(roundThreads.isChecked());
        filter.setShadeCrossings(shadeCrossings.isChecked());

        return filter.filter(src, dest);
    }
}