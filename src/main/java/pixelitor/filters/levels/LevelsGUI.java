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
import pixelitor.filters.gui.ChannelSelectorPanel;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;

/**
 * The {@link FilterGUI} for the {@link Levels} filter.
 */
public class LevelsGUI extends FilterGUI {
    private final JPanel cardPanel;
    private final ChannelSelectorPanel channelSelectorPanel;
    private JCheckBox showOriginalCB;

    public LevelsGUI(Filter filter, Filterable layer, LevelsModel model) {
        super(filter, layer);
        model.setLevelsGUI(this);

        setLayout(new BorderLayout());

        // create center panel first, so it exists when the callback
        // is fired from ChannelSelectorPanel's constructor
        cardPanel = createCenterPanel(model);
        add(cardPanel, CENTER);

        channelSelectorPanel = new ChannelSelectorPanel(this::showChannelPanel);
        channelSelectorPanel.addResetButton(e ->
            model.resetChannelToDefault(channelSelectorPanel.getSelectedChannel()));
        channelSelectorPanel.addColorSpaceChangedListener(colorSpace ->
            model.setColorSpace(colorSpace, true));

        add(channelSelectorPanel, NORTH);
        add(createSouthPanel(model, layer), SOUTH);

        // ensure the lookup is initialized for the preview
        model.updateFilterLookup();
    }

    private void showChannelPanel(Channel channel) {
        if (cardPanel != null) {
            var cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, channel.getName());
        }
    }

    private static JPanel createCenterPanel(LevelsModel model) {
        var panel = new JPanel(new CardLayout());
        for (ChannelLevelsModel subModel : model.getSubModels()) {
            JPanel channelPanel = subModel.createPanel();
            String channelName = subModel.getChannel().getName();
            panel.add(channelPanel, channelName);
        }
        return panel;
    }

    private JPanel createSouthPanel(LevelsModel model, Filterable layer) {
        var southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> layer.setShowOriginal(showOriginalCB.isSelected()));
        southPanel.add(showOriginalCB);

        southPanel.add(GUIUtils.createRandomizeSettingsButton(e ->
            model.randomizeAndRun()));

        southPanel.add(GUIUtils.createResetAllButton(e ->
            model.resetAllAndRun()));

        return southPanel;
    }

    public void setColorSpaceUI(ColorSpace colorSpace) {
        channelSelectorPanel.setColorSpaceUI(colorSpace);
    }
}
