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

import static java.awt.BorderLayout.*;

/**
 * The {@link FilterGUI} for the {@link Levels} filter.
 */
public class LevelsGUI extends FilterGUI {
    private final JPanel cardPanel;
    private final ChannelSelectorPanel channelSelectorPanel;
    private final LevelsModel model;
    private JCheckBox showOriginalCB;

    public LevelsGUI(Filter filter, Filterable layer, LevelsModel model) {
        super(filter, layer);
        this.model = model;

        setLayout(new BorderLayout());

        // create center panel first, so it exists when the callback
        // is fired from ChannelSelectorPanel's constructor
        cardPanel = createCenterPanel(model);
        add(cardPanel, CENTER);

        channelSelectorPanel = new ChannelSelectorPanel(this::showChannelPanel);
        channelSelectorPanel.addResetButton(_ ->
            model.resetChannelToDefault(channelSelectorPanel.getSelectedChannel()));
        channelSelectorPanel.addColorSpaceChangedListener(colorSpace ->
            model.setColorSpace(colorSpace, true));

        add(channelSelectorPanel, NORTH);
        add(createSouthPanel(model, layer), SOUTH);

        // The model isn't necessarily at its defaults (smart filters, reused
        // last-used settings), so make the selectors and the visible channel
        // panel match it. This doesn't trigger any callbacks or previews.
        setSelectedColorSpace(model.getColorSpace());

        model.setLevelsGUI(this);
    }

    private void showChannelPanel(Channel channel) {
        if (cardPanel != null) {
            var cl = (CardLayout) cardPanel.getLayout();
            cl.show(cardPanel, channel.getDisplayName());
        }
    }

    private static JPanel createCenterPanel(LevelsModel model) {
        var panel = new JPanel(new CardLayout());
        for (ChannelLevelsModel subModel : model.getChannelModels()) {
            JPanel channelPanel = subModel.createPanel();
            String channelName = subModel.getChannel().getDisplayName();
            panel.add(channelPanel, channelName);
        }
        return panel;
    }

    private JPanel createSouthPanel(LevelsModel model, Filterable layer) {
        var southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(_ -> layer.setShowOriginal(showOriginalCB.isSelected()));
        southPanel.add(showOriginalCB);

        southPanel.add(GUIUtils.createRandomizeSettingsButton(_ ->
            model.randomizeAndRun()));

        southPanel.add(GUIUtils.createResetAllButton(_ ->
            model.resetAllAndRun()));

        return southPanel;
    }

    /**
     * Shows the given color space (with its primary channel) in the selectors
     * and the matching channel panel, without triggering any callbacks.
     * Called when this GUI is created, and by the model when it changes
     * its color space.
     */
    public void setSelectedColorSpace(ColorSpace colorSpace) {
        Channel channel = colorSpace.getPrimaryChannel();
        channelSelectorPanel.setSelection(colorSpace, channel);
        showChannelPanel(channel);
    }

    @Override
    public void dispose() {
        model.setLevelsGUI(null);
        super.dispose();
    }
}
