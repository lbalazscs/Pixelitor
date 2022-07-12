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

package pixelitor.filters.gui;

import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.jhlabsproxies.JHFourColorGradient;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;

/**
 * A {@link ParametrizedFilterGUI}, where the components (typically representing
 * the four corners of the image) are added in a 2*2 grid.
 * Extra parameters are added in a row bellow the grid.
 */
public class GridAdjustmentPanel extends ParametrizedFilterGUI {
    private static final int MAX_GRID_PARAMS = 4;
    private boolean addGridLabels;

    public GridAdjustmentPanel(ParametrizedFilter filter, Filterable layer,
                               boolean addGridLabels, boolean showOriginal, boolean reset) {
        super(filter, layer, showOriginal, reset);
        this.addGridLabels = addGridLabels;
    }

    @Override
    public JPanel createFilterParamsPanel(ParamSet paramSet) {
        // hack, otherwise the setting of the flag in the constructor is too late,
        // because this is called by the superclass constructor
        if (filter instanceof JHFourColorGradient) {
            addGridLabels = true;
        }

        // the central panel, with maximum MAX_GRID_PARAMS controls
        int numCols = addGridLabels ? 4 : 2;
        JPanel gridPanel = new JPanel(new GridLayout(2, numCols, 5, 5));

        List<FilterParam> paramList = paramSet.getParams();
        int numParams = paramList.size();

        JPanel extraParamsPanel = null;
        if (numParams > MAX_GRID_PARAMS) {
            extraParamsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        }

        for (int i = 0; i < numParams; i++) {
            FilterParam param = paramList.get(i);
            JComponent control = param.createGUI();
            String labelText = param.getName() + ':';

            // the first 4 are added into the 4 grid positions...
            if (i < MAX_GRID_PARAMS) {
                if (addGridLabels) {
                    gridPanel.add(new JLabel(labelText));
                }
                gridPanel.add(control);
            } else {
                extraParamsPanel.add(new JLabel(labelText));
                extraParamsPanel.add(control);
            }
        }

        if (numParams <= MAX_GRID_PARAMS) {
            return gridPanel;
        }

        JPanel p = new JPanel(new BorderLayout());
        p.add(gridPanel, CENTER);
        p.add(extraParamsPanel, SOUTH);
        return p;
    }
}
