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

package pixelitor.filters.levels;

import pixelitor.filters.Filter;
import pixelitor.filters.gui.EnumParam;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.util.Channel;
import pixelitor.filters.util.ColorSpace;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;

/**
 * The {@link FilterGUI} for the {@link Levels} filter.
 */
public class LevelsGUI extends FilterGUI {
    private final EnumParam<Channel> channelsModel = Channel.asParam(ColorSpace.SRGB);

    private final JPanel cardPanel;
    private JCheckBox showOriginalCB;

    public LevelsGUI(Filter filter, Filterable layer, LevelsModel model) {
        super(filter, layer);
        model.setLastGUI(this);

        setLayout(new BorderLayout());

        add(createNorthPanel(model), NORTH);

        cardPanel = createCenterPanel(model);
        add(cardPanel, CENTER);

        add(createSouthPanel(model, layer), SOUTH);

        // ensure the lookup is initialized for the preview
        model.updateFilterLookup();
    }

    private JPanel createNorthPanel(LevelsModel model) {
        var northPanel = new JPanel(new FlowLayout());
        northPanel.add(new JLabel("Channel:"));

        var selector = new JComboBox<Channel>(channelsModel);
        selector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                var cl = (CardLayout) cardPanel.getLayout();
                cl.show(cardPanel, ((Channel) e.getItem()).getName());
            }
        });
        northPanel.add(selector);

        JButton resetChannelButton = GUIUtils.createResetChannelButton(
            e -> model.resetChannelToDefault(channelsModel.getSelected()));
        northPanel.add(resetChannelButton);

        return northPanel;
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
}
