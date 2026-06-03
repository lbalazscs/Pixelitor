/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.filters.gui.BooleanParam;
import pixelitor.filters.gui.GroupedRangeParam;
import pixelitor.filters.gui.ParamAdjustmentListener;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.util.List;

/**
 * The GUI for adding a single (horizontal or vertical) guide
 */
public class AddSingleGuidePanel extends JPanel {
    private final Guides.Builder builder;
    private final boolean horizontal; // horizontal or vertical

    private final RangeParam percent;

    private AddSingleGuidePanel(Guides.Builder builder, boolean horizontal) {
        this.builder = builder;
        this.horizontal = horizontal;

        var canvas = builder.getCanvas();
        int maxSize = horizontal ? canvas.getWidth() : canvas.getHeight();
        percent = new RangeParam("Percent", 0, 50, 100);
        var pixels = new RangeParam("Pixels", 0, maxSize / 2.0, maxSize);
        percent.scaledLinkWith(pixels, maxSize / 100.0);

        var groupedSliders = new GroupedRangeParam("Position",
            new RangeParam[]{pixels, percent}, false).notLinkable();

        BooleanParam clearExisting = builder.getClearExisting();
        GUIUtils.arrangeVertically(this, List.of(groupedSliders, clearExisting));

        ParamAdjustmentListener previewUpdater = () -> createGuides(true);
        percent.setAdjustmentListener(previewUpdater);
        pixels.setAdjustmentListener(previewUpdater);
        builder.setAdjustmentListener(previewUpdater);
        previewUpdater.paramAdjusted(); // trigger initial preview
    }

    private void createGuides(boolean preview) {
        builder.build(preview, this::configure);
    }

    private void configure(Guides guides) {
        double value = percent.getPercentage();
        if (horizontal) {
            guides.addHorizontal(value);
        } else {
            guides.addVertical(value);
        }
    }

    public static void showDialog(View view,
                                  boolean horizontal,
                                  String dialogTitle) {
        Guides.Builder builder = new Guides.Builder(view, false);
        AddSingleGuidePanel panel = new AddSingleGuidePanel(builder, horizontal);
        new DialogBuilder()
            .title(dialogTitle)
            .content(panel)
            .withScrollbars()
            .okAction(() -> panel.createGuides(false))
            .cancelAction(builder::resetOrigGuides)
            .show();
    }
}
