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

import com.jhlabs.image.OilFilter;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.ResizingFilterHelper;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.IntChoiceParam;
import pixelitor.filters.gui.IntChoiceParam.Item;
import pixelitor.filters.gui.RangeParam;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.Texts;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static pixelitor.filters.ResizingFilterHelper.ScaleUpQuality;
import static pixelitor.filters.gui.RandomizeMode.IGNORE_RANDOMIZE;

/**
 * Oil Painting filter based on the JHLabs OilFilter
 */
public class JHOilPainting extends ParametrizedFilter {
    @Serial
    private static final long serialVersionUID = -9168810692489222200L;

    public static final String NAME = Texts.i18n("oil_painting");

    private static final int FASTER = 0;
    private static final int BETTER = 1;

    private final GroupedRangeParam brushSize = new GroupedRangeParam(
        "Brush Size", 0, 1, 10, false);
    private final RangeParam coarseness = new RangeParam(
        "Coarseness", 0, 25, 200);
    private final IntChoiceParam detailQuality = new IntChoiceParam("Detail Quality",
        new Item[]{
            new Item("Faster", FASTER),
            new Item("Better", BETTER),
        }, IGNORE_RANDOMIZE);

    public JHOilPainting() {
        super(true);

        initParams(
            brushSize.withAdjustedRange(0.04),
            coarseness,
            detailQuality
        );
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        int brushX = brushSize.getValue(0);
        int brushY = brushSize.getValue(1);
        if (brushX == 0 && brushY == 0) {
            return src;
        }

        // important to re-create because the progress tracker
        // is different for big and small images
        var filter = new OilFilter(NAME);

        filter.setLevels(coarseness.getValue() + 1);

        var helper = new ResizingFilterHelper(src);
        if (helper.shouldResize()) {
            ScaleUpQuality scaleUpQuality = getScaleUpQuality();

            double resizeFactor = helper.getResizeFactor();
            // these will determine the real running time of the filter
            int downScaledBrushX = (int) (brushX / resizeFactor);
            int downScaledBrushY = (int) (brushY / resizeFactor);

            int resizeUnits = helper.getResizeWorkUnits(scaleUpQuality);
            long filterWorkAmount = (long) downScaledBrushX * downScaledBrushY;
            int filterUnits = (int) (filterWorkAmount / 4);
            int workUnits = resizeUnits + filterUnits;

            var pt = new StatusBarProgressTracker(NAME, workUnits);
            ProgressTracker filterTracker = helper.createFilterTracker(pt, filterUnits);

            filter.setProgressTracker(filterTracker);

            filter.setRangeX(downScaledBrushX);
            filter.setRangeY(downScaledBrushY);

            dest = helper.invoke(scaleUpQuality, filter, pt, 0);
            pt.finished();
        } else {
            // normal case, no resizing
            filter.setRangeX(brushX);
            filter.setRangeY(brushY);
            dest = filter.filter(src, dest);
        }

        return dest;
    }

    private ScaleUpQuality getScaleUpQuality() {
        int quality = detailQuality.getValue();
        return switch (quality) {
            case BETTER -> ScaleUpQuality.BILINEAR11;
            case FASTER -> ScaleUpQuality.BILINEAR_FAST;
            default -> throw new IllegalStateException("quality = " + quality);
        };
    }

    @Override
    public boolean supportsTweenAnimation() {
        return false;
    }
}