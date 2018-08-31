/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.filters.curves;

import pixelitor.filters.Filter;
import pixelitor.filters.curves.lx.LxCurveType;
import pixelitor.filters.curves.lx.LxCurvesPanel;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.levels.LevelsModel;
import pixelitor.filters.levels.OneChannelLevelsModel;
import pixelitor.filters.levels.gui.OneChannelLevelsPanel;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * The GUI for the levels filter
 */
public class CurvesGUI extends FilterGUI implements ItemListener {
//    private final DefaultComboBoxModel<String> selectorModel;

//    private final JPanel cardPanel;
//    private final JCheckBox showOriginalCB;

    public CurvesGUI(Filter filter, Drawable dr) {
        super(filter, dr);

        LxCurvesPanel curvesPanel = new LxCurvesPanel();
        curvesPanel.addActionListener(e -> {
//            System.out.println("curve changed: " + e.paramString());
            ((Curves) filter).setCurves(curvesPanel.lxCurves);
            runFilterPreview();
        });

        JPanel chartPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chartPanel.add(curvesPanel);

        JComboBox curveTypeSelect = new JComboBox<>(LxCurveType.values());
        curveTypeSelect.setMaximumRowCount(curveTypeSelect.getItemCount());
        curveTypeSelect.setSelectedItem(LxCurveType.RGB);
        curveTypeSelect.addActionListener(e -> {
            curvesPanel.setActiveCurve((LxCurveType) curveTypeSelect.getSelectedItem());
        });

        JButton resetChannel = new JButton("Reset channel");
        resetChannel.addActionListener(e -> curvesPanel.resetActiveCurve());

        JButton resetAllBtn = new JButton("Reset All");
        resetAllBtn.addActionListener(e -> curvesPanel.reset());

        JPanel channelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelPanel.add(new JLabel("Channel:"));
        channelPanel.add(curveTypeSelect);
        channelPanel.add(resetChannel);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> dr.setShowOriginal(showOriginalCB.isSelected()));
        buttonsPanel.add(showOriginalCB);
        buttonsPanel.add(resetAllBtn);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(channelPanel);
        mainPanel.add(chartPanel);
        mainPanel.add(buttonsPanel);

        add(mainPanel);
//        runFilterPreview()




//        model.setExecutor(this);

//        setLayout(new BorderLayout());
//
//        selectorModel = new DefaultComboBoxModel();
//        JComboBox<String> selector = new JComboBox(selectorModel);
//        selector.addItemListener(this);
//
//        JPanel northPanel = new JPanel();
//        northPanel.setLayout(new FlowLayout());
//        northPanel.add(selector);
//        JButton resetAllButton = new JButton("Reset all");
//        resetAllButton.addActionListener(e -> model.resetToDefaultSettings());
//        northPanel.add(resetAllButton);
//        add(northPanel, BorderLayout.NORTH);
//
//        cardPanel = new JPanel();
//        cardPanel.setLayout(new CardLayout());
//
//        OneChannelLevelsModel[] models = model.getSubModels();
//        for (OneChannelLevelsModel m : models) {
//            OneChannelLevelsPanel p = new OneChannelLevelsPanel(m);
//            addNewCard(p);
//        }
//
//        add(cardPanel, BorderLayout.CENTER);
//
//        showOriginalCB = new JCheckBox("Show Original");
//        showOriginalCB.setName("show original");
//        showOriginalCB.addActionListener(e -> dr.setShowOriginal(showOriginalCB.isSelected()));
//        add(showOriginalCB, BorderLayout.SOUTH);
    }

//    private void addNewCard(OneChannelLevelsPanel chPanel) {
//        String channelName = chPanel.getCardName();
//        cardPanel.add(chPanel, channelName);
//        selectorModel.addElement(channelName);
//    }

    @Override
    public void itemStateChanged(ItemEvent e) {
//        CardLayout cl = (CardLayout) (cardPanel.getLayout());
//        cl.show(cardPanel, (String) e.getItem());
    }
}
