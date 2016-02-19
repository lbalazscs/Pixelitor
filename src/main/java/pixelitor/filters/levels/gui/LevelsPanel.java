/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.filters.Filter;
import pixelitor.filters.gui.AdjustPanel;
import pixelitor.filters.levels.LevelsModel;
import pixelitor.filters.levels.OneChannelLevelsModel;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class LevelsPanel extends AdjustPanel implements ItemListener {
    private final DefaultComboBoxModel<String> selectorModel;

    private final JPanel cardPanel;
    private final JCheckBox showOriginalCB;

    public LevelsPanel(Filter filter, ImageLayer layer, LevelsModel model) {
        super(filter, layer);

        model.setExecutor(this);

        setLayout(new BorderLayout());

        selectorModel = new DefaultComboBoxModel();
        JComboBox<String> selector = new JComboBox(selectorModel);
        selector.addItemListener(this);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new FlowLayout());
        northPanel.add(selector);
        JButton resetAllButton = new JButton("Reset all");
        resetAllButton.addActionListener(e -> model.resetToDefaultSettings());
        northPanel.add(resetAllButton);
        add(northPanel, BorderLayout.NORTH);

        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());

        OneChannelLevelsModel[] models = model.getSubModels();
        for (OneChannelLevelsModel m : models) {
            OneChannelLevelsPanel p = new OneChannelLevelsPanel(m);
            addNewCard(p);
        }

        add(cardPanel, BorderLayout.CENTER);

        showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> layer.setShowOriginal(showOriginalCB.isSelected()));
        add(showOriginalCB, BorderLayout.SOUTH);
    }

    private void addNewCard(OneChannelLevelsPanel chPanel) {
        String channelName = chPanel.getCardName();
        cardPanel.add(chPanel, channelName);
        selectorModel.addElement(channelName);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        CardLayout cl = (CardLayout) (cardPanel.getLayout());
        cl.show(cardPanel, (String) e.getItem());
    }
}
