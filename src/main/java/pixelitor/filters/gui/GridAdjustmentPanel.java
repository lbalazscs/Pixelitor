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
import java.util.List;

import static java.awt.FlowLayout.CENTER;

/**
 * An adjustment panel, where the components (typically representing
 * the four corners of the image) are added in a 2*2 grid
 */
public class GridAdjustmentPanel extends ParametrizedAdjustPanel {
    private final boolean addLabels;

    public GridAdjustmentPanel(FilterWithParametrizedGUI filter, boolean addLabels, ShowOriginal showOriginal) {
        super(filter, showOriginal);
        this.addLabels = addLabels;
    }

    @Override
    protected void setupGUI(ParamSet params, Object otherInfo, ShowOriginal addShowOriginal) {
        setLayout(new BorderLayout(5, 5));
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));

        // a panel for parameters like "Edge Action", "Interpolation"
        JPanel nonGridPanel = new JPanel();

        // the central panel, with max 4 controls
        JPanel gridPanel = createGridPanel();

        // A panel for global actions like "Randomize Settings", "Reset All"
        JPanel buttonsPanel = createButtonsPanel(addShowOriginal);

        addParams(params, gridPanel, nonGridPanel, buttonsPanel);

        controlPanel.add(gridPanel, BorderLayout.CENTER);
        controlPanel.add(nonGridPanel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void addParams(ParamSet params, JPanel gridPanel, JPanel nonGridPanel, JPanel buttonsPanel) {
        int added = 0;

        List<FilterParam> paramList = params.getParamList();

        for (FilterParam param : paramList) {
            JComponent control = param.createGUI();

            String labelText = param.getName() + ':';
            if (added < 4) { // the first 4 are added into the 4 grid positions...
                if (addLabels) {
                    gridPanel.add(new JLabel(labelText));
                }
                gridPanel.add(control);
            } else { // ...and the rest into "non grid" positions.
                nonGridPanel.add(new JLabel(labelText)); // these always need a label
                nonGridPanel.add(control);
            }

            added++;
        }

        List<ActionSetting> actionList = params.getActionList();
        for (ActionSetting action : actionList) {
            JButton control = (JButton) action.createGUI();
            buttonsPanel.add(control);
        }
    }

    private JPanel createGridPanel() {
        GridLayout layout;
        if (addLabels) {
            layout = new GridLayout(2, 4, 5, 5);
        } else {
            layout = new GridLayout(2, 2, 5, 5);
        }
        return new JPanel(layout);
    }

    private static JPanel createButtonsPanel(ShowOriginal showOriginal) {
        JPanel buttonsPanel = new JPanel(new FlowLayout(CENTER));
        if (showOriginal.isYes()) {
            JCheckBox showOriginalCB = new JCheckBox("Show Original");
            showOriginalCB.setName("show original");
            showOriginalCB.addActionListener(e -> Utils.setShowOriginal(showOriginalCB.isSelected()));
            buttonsPanel.add(showOriginalCB);
        }
        return buttonsPanel;
    }
}
