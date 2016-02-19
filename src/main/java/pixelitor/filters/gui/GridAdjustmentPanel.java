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

package pixelitor.filters.gui;

import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.layers.ImageLayer;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * An adjustment panel, where the components (typically representing
 * the four corners of the image) are added in a 2*2 grid.
 * Extra parameters are added in a row bellow the grid.
 */
public class GridAdjustmentPanel extends ParametrizedAdjustPanel {
    public static final int MAX_GRID_PARAMS = 4;
    private final boolean addGridLabels;

    public GridAdjustmentPanel(FilterWithParametrizedGUI filter, ImageLayer layer, boolean addGridLabels, ShowOriginal showOriginal) {
        super(filter, layer, showOriginal);
        this.addGridLabels = addGridLabels;
    }

    @Override
    public JPanel createFilterParamsPanel(List<FilterParam> paramList) {
        // the central panel, with max 4 controls
        JPanel gridPanel = new JPanel();
        GridLayout layout;
        if (addGridLabels) {
            layout = new GridLayout(2, 4, 5, 5);
        } else {
            layout = new GridLayout(2, 2, 5, 5);
        }
        gridPanel.setLayout(layout);

        int numParams = paramList.size();
        JPanel extraParamsPanel = null;
        if(numParams > MAX_GRID_PARAMS) {
            extraParamsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        }

        for (int i = 0; i < numParams; i++) {
            FilterParam param = paramList.get(i);
            JComponent control = param.createGUI();

            String labelText = param.getName() + ':';

            if (i < MAX_GRID_PARAMS) { // the first 4 are added into the 4 grid positions...
                if (addGridLabels) {
                    gridPanel.add(new JLabel(labelText));
                }
                gridPanel.add(control);
            } else {
                extraParamsPanel.add(new JLabel(labelText));
                extraParamsPanel.add(control);
            }
        }

        if(numParams <= MAX_GRID_PARAMS) {
            return gridPanel;
        }

        JPanel p = new JPanel(new BorderLayout());
        p.add(gridPanel, BorderLayout.CENTER);
        p.add(extraParamsPanel, BorderLayout.SOUTH);
        return p;
    }
}
