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

package pixelitor.filters.levels;

import pixelitor.filters.Filter;
import pixelitor.filters.gui.ChannelAdjustmentGUI;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.CardLayout;

/**
 * The {@link FilterGUI} for the {@link Levels} filter.
 */
public class LevelsGUI extends ChannelAdjustmentGUI<LevelsModel> {
    public LevelsGUI(Levels filter, Filterable layer) {
        this(filter, layer, filter.getModel());
    }

    public LevelsGUI(Filter filter, Filterable layer, LevelsModel model) {
        super(filter, layer, model, createCenterPanel(model));
    }

    @Override
    protected void refreshEditor() {
        var cl = (CardLayout) editorComponent.getLayout();
        cl.show(editorComponent, model.getActiveChannel().getDisplayName());
    }

    private static JPanel createCenterPanel(LevelsModel model) {
        var panel = new JPanel(new CardLayout());
        for (ChannelLevelsModel subModel : model.getAllAdjustments()) {
            JPanel channelPanel = subModel.createPanel();
            String channelName = subModel.getChannel().getDisplayName();
            panel.add(channelPanel, channelName);
        }
        return panel;
    }
}
