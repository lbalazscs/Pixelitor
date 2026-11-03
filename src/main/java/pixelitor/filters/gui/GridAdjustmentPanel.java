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

package pixelitor.filters.gui;

import pixelitor.filters.ParametrizedFilter;
import pixelitor.filters.jhlabsproxies.JHFourColorGradient;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;

/**
 * A {@link ParametrizedFilterGUI}, where the {@link ParamGUI}s (typically
 * representing the four corners of the image) are added in a 2*2 grid.
 * Additional parameters are added below the grid in a separate row.
 */
public class GridAdjustmentPanel extends ParametrizedFilterGUI {
    private static final int MAX_GRID_PARAMS = 4;
    private final boolean addGridLabels;

    public GridAdjustmentPanel(ParametrizedFilter filter, Filterable layer,
                               boolean addGridLabels, boolean showOriginal,
                               boolean resetSettings) {
        // assigned before super(...) so createFilterParamsPanel() sees the value
        this.addGridLabels = addGridLabels;
        super(filter, layer, showOriginal, resetSettings);
    }

    @Override
    public JPanel createFilterParamsPanel(ParamSet paramSet) {
        List<FilterParam> params = paramSet.getParams();
        int numGridParams = Math.min(params.size(), MAX_GRID_PARAMS);

        // each param takes 1 cell, or 2 if it has a label
        int numCols = addGridLabels ? 4 : 2;
        JPanel gridPanel = new JPanel(new GridLayout(2, numCols, 5, 5));
        for (FilterParam param : params.subList(0, numGridParams)) {
            if (addGridLabels) {
                gridPanel.add(new JLabel(param.getName() + ':'));
            }
            gridPanel.add(param.createGUI());
        }

        if (params.size() <= MAX_GRID_PARAMS) {
            return gridPanel;
        }

        List<FilterParam> extraParams = params.subList(MAX_GRID_PARAMS, params.size());
        JPanel extraParamsPanel = filter instanceof JHFourColorGradient
            ? GUIUtils.createVerticalPanel(extraParams)
            : GUIUtils.createHorizontalPanel(extraParams);

        JPanel combinedPanel = new JPanel(new BorderLayout());
        combinedPanel.add(gridPanel, CENTER);
        combinedPanel.add(extraParamsPanel, SOUTH);
        return combinedPanel;
    }
}
