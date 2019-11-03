/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.guides;

import pixelitor.Canvas;
import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

/**
 * The GUI for adding a single (horizontal or vertical) guide
 */
public class AddSingleGuidePanel extends JPanel {
    private final Guides.Builder builder;
    private final boolean horizontal; // horizontal or vertical

    private final RangeParam percents;

    public AddSingleGuidePanel(Guides.Builder builder, boolean horizontal) {
        this.builder = builder;
        this.horizontal = horizontal;

        Canvas canvas = builder.getCanvas();
        int maxSize = horizontal ? canvas.getImWidth() : canvas.getImHeight();
        percents = new RangeParam("Position %", 0, 50, 100);
        RangeParam pixels = new RangeParam("Position Pixels", 0, maxSize / 2, maxSize);
        percents.linkWith(pixels, maxSize / 100.0);

        BooleanParam clearExisting = builder.getClearExisting();
        List<FilterParam> params = Arrays.asList(percents, pixels, clearExisting);
        GUIUtils.arrangeParamsVertically(this, params);

        ParamAdjustmentListener updatePreview = () -> createGuides(true);
        percents.setAdjustmentListener(updatePreview);
        pixels.setAdjustmentListener(updatePreview);
        builder.setAdjustmentListener(updatePreview);
        updatePreview.paramAdjusted(); // set initial preview
    }

    public void createGuides(boolean preview) {
        builder.build(preview, this::setup);
    }

    private void setup(Guides guides) {
        float percentage = percents.getValueAsPercentage();
        if (horizontal) {
            guides.addHorRelative(percentage);
            guides.setName(String.format("horizontal at %.2f", percentage));
        } else {
            guides.addVerRelative(percentage);
            guides.setName(String.format("vertical at %.2f", percentage));
        }
    }
}
