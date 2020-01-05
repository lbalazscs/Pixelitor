/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.filters.gui.FilterGUI;
import pixelitor.layers.Drawable;
import pixelitor.utils.Icons;

import javax.swing.*;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BoxLayout.PAGE_AXIS;

/**
 * The GUI for the tone curve filter
 *
 * @author Łukasz Kurzaj lukaszkurzaj@gmail.com
 */
public class ToneCurvesGUI extends FilterGUI {
    public ToneCurvesGUI(Filter filter, Drawable dr) {
        super(filter, dr);

        // listen for any change in curves to run filter preview
        var curvesPanel = new ToneCurvesPanel();
        curvesPanel.addActionListener(e -> {
            ((ToneCurvesFilter) filter).setCurves(curvesPanel.toneCurves);
            runFilterPreview();
        });

        JPanel chartPanel = new JPanel(new FlowLayout(LEFT));
        chartPanel.add(curvesPanel);

        var curveTypeCB = new JComboBox<ToneCurveType>(ToneCurveType.values());
        curveTypeCB.setMaximumRowCount(curveTypeCB.getItemCount());
        curveTypeCB.setSelectedItem(ToneCurveType.RGB);
        curveTypeCB.addActionListener(e -> curvesPanel.setActiveCurve(
                (ToneCurveType) curveTypeCB.getSelectedItem()));

        JButton resetChannel = new JButton("Reset channel", Icons.getWestArrowIcon());
        resetChannel.addActionListener(e -> curvesPanel.resetActiveCurve());

        JButton resetAllBtn = new JButton("Reset All", Icons.getWestArrowIcon());
        resetAllBtn.addActionListener(e -> curvesPanel.reset());

        JPanel channelPanel = new JPanel(new FlowLayout(LEFT));
        channelPanel.add(new JLabel("Channel:"));
        channelPanel.add(curveTypeCB);
        channelPanel.add(resetChannel);

        JPanel buttonsPanel = new JPanel(new FlowLayout(LEFT));
        JCheckBox showOriginalCB = new JCheckBox("Show Original");
        showOriginalCB.setName("show original");
        showOriginalCB.addActionListener(e -> dr.setShowOriginal(showOriginalCB.isSelected()));
        buttonsPanel.add(showOriginalCB);
        buttonsPanel.add(resetAllBtn);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, PAGE_AXIS));
        mainPanel.add(channelPanel);
        mainPanel.add(chartPanel);
        mainPanel.add(buttonsPanel);

        add(mainPanel);
    }
}
