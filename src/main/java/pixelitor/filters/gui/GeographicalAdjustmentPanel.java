/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

package pixelitor.filters.gui;

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * An adjustment panel, where the components can be added in "geographical" places (north-west etc.)
 */
public class GeographicalAdjustmentPanel extends ParametrizedAdjustPanel {
    private final boolean addLabels;

    public GeographicalAdjustmentPanel(FilterWithParametrizedGUI filter, boolean addLabels, boolean showOriginal) {
        super(filter, showOriginal);
        this.addLabels = addLabels;
    }

    @Override
    protected void setupGUI(ParamSet params, Object otherInfo, boolean addShowOriginal) {
        setLayout(new BorderLayout(5, 5));
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // a panel for parameters like "Edge Action", "Interpolation"
        JPanel nonGeoPanel = new JPanel();

        // the central panel, with max 4 controls
        JPanel geoPanel = createGeoPanel();

        // A panel for global actions like "Randomize Settings", "Reset All"
        JPanel buttonsPanel = createButtonsPanel(addShowOriginal);

        addParams(params, geoPanel, nonGeoPanel, buttonsPanel);

        controlPanel.add(geoPanel, BorderLayout.CENTER);
        controlPanel.add(nonGeoPanel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void addParams(ParamSet params, JPanel geoPanel, JPanel nonGeoPanel, JPanel buttonsPanel) {
        int added = 0;
        for(GUIParam param : params) {
            JComponent control = param.createGUI();

            if(control instanceof JButton) {
                buttonsPanel.add(control);
            } else {
                String labelText = param.getName() + ':';
                if (added < 4) { // the first 4 are added into the 4 "geographical" positions...
                    if(addLabels) {
                        geoPanel.add(new JLabel(labelText));
                    }
                    geoPanel.add(control);
                } else { // ...and the rest into "non geographical" positions.
                    nonGeoPanel.add(new JLabel(labelText)); // these always need a label
                    nonGeoPanel.add(control);
                }
            }
            added++;
        }
    }

    private JPanel createGeoPanel() {
        GridLayout layout;
        if (addLabels) {
            layout = new GridLayout(2, 4, 5, 5);
        } else {
            layout = new GridLayout(2, 2, 5, 5);
        }
        return new JPanel(layout);
    }

    private static JPanel createButtonsPanel(boolean showOriginal) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (showOriginal) {
            JCheckBox showOriginalCB = new JCheckBox("Show Original");
            showOriginalCB.addActionListener(e -> Utils.setShowOriginal(showOriginalCB.isSelected()));
            buttonsPanel.add(showOriginalCB);
        }
        return buttonsPanel;
    }
}
