/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.levels.gui;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.Filter;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.levels.Channel;
import pixelitor.filters.levels.ChannelLevelsModel;
import pixelitor.filters.levels.LevelsModel;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import static java.awt.BorderLayout.*;

/**
 * The GUI for the levels filter
 */
public class LevelsGUI extends FilterGUI implements ItemListener {
    private final EnumComboBoxModel<Channel> channelsModel
        = new EnumComboBoxModel<>(Channel.class);

    private final JPanel cardPanel;
    private final JCheckBox showOriginalCB;

    @SuppressWarnings("unchecked")
    public LevelsGUI(Filter filter, Drawable dr, LevelsModel model) {
        super(filter, dr);

        model.setLastGUI(this);

        setLayout(new BorderLayout());

        JComboBox<Channel> selector = new JComboBox<>(channelsModel);
        selector.addItemListener(this);

        JPanel northPanel = new JPanel(new FlowLayout());
        northPanel.add(new JLabel("Channel:"));
        northPanel.add(selector);

        JButton resetChannelButton = GUIUtils.createResetChannelButton(
            e -> model.resetChannelToDefault(channelsModel.getSelectedItem()));
        northPanel.add(resetChannelButton);

        add(northPanel, NORTH);

        cardPanel = new JPanel(new CardLayout());

        ChannelLevelsModel[] models = model.getSubModels();
        for (ChannelLevelsModel m : models) {
            ChannelLevelsPanel p = new ChannelLevelsPanel(m);
            addNewCard(p);
        }

        add(cardPanel, CENTER);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> dr.setShowOriginal(showOriginalCB.isSelected()));
        southPanel.add(showOriginalCB);

        JButton resetAllButton = GUIUtils.createResetAllButton(
            e -> model.resetAllToDefault());
        southPanel.add(resetAllButton);

        add(southPanel, SOUTH);
    }

    private void addNewCard(ChannelLevelsPanel chPanel) {
        String channelName = chPanel.getCardName();
        cardPanel.add(chPanel, channelName);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        CardLayout cl = (CardLayout) cardPanel.getLayout();
        cl.show(cardPanel, ((Channel) e.getItem()).getName());
    }
}
