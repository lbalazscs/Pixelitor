/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Filterable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;


/**
 * An automatically generated GUI for {@link ParametrizedFilter} filters.
 */
public class ParametrizedFilterGUI extends FilterGUI implements ParamAdjustmentListener {
    private ShowOriginalCheckbox showOriginalCB;

    public ParametrizedFilterGUI(ParametrizedFilter filter, Filterable layer,
                                 boolean addShowOriginal, boolean reset) {
        this(filter, layer, addShowOriginal, reset, null);
    }

    public ParametrizedFilterGUI(ParametrizedFilter filter,
                                 Filterable layer,
                                 boolean addShowOriginal,
                                 boolean reset,
                                 Action[] presets) {
        super(filter, layer);

        ParamSet paramSet = filter.getParamSet();
        if (reset) {
            paramSet.reset();

            // if the filter is not reset, then the ranges should
            // not be updated, even if the canvas size changed
            paramSet.adaptToContext(layer, true);
        }

        paramSet.setAdjustmentListener(this);
        setupGUI(paramSet, addShowOriginal, presets);
    }

    protected void setupGUI(ParamSet paramSet,
                            boolean addShowOriginal,
                            Action[] presets) {
        JPanel filterParamsPanel = createFilterParamsPanel(paramSet);
        JPanel filterActionsPanel = createFilterActionsPanel(
            paramSet.getActions(), addShowOriginal, 3);

        setLayout(new BorderLayout());
        add(filterParamsPanel, CENTER);
        add(filterActionsPanel, SOUTH);
    }

    /**
     * This can be overridden if a custom arrangement is necessary
     */
    public JPanel createFilterParamsPanel(ParamSet paramSet) {
        return GUIUtils.arrangeVertically(paramSet);
    }

    protected JPanel createFilterActionsPanel(List<FilterButtonModel> actionList,
                                              boolean addShowOriginal,
                                              int maxControlsInRow) {
        int numControls = actionList.size();
        if (addShowOriginal) {
            numControls++;
            showOriginalCB = new ShowOriginalCheckbox("Show Original");
        }

        JPanel actionsPanel;
        if (numControls <= maxControlsInRow) {
            actionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        } else {
            int cols = (numControls + 1) / 2;
            actionsPanel = new JPanel(new GridLayout(2, cols));
        }

        if (addShowOriginal) {
            actionsPanel.add(showOriginalCB);
        }

        for (FilterButtonModel action : actionList) {
            // all the buttons go in one row
            JButton button = (JButton) action.createGUI();
            actionsPanel.add(button);
        }

        return actionsPanel;
    }

    /**
     * Handles parameter adjustment events by restarting the preview.
     */
    @Override
    public void paramAdjusted() {
        if (hasShowOriginal()) {
            // Stops "Show Original" mode if any parameter changes.
            showOriginalCB.deselectWithoutTriggering();
        }
        startPreview(false);
    }

    private boolean hasShowOriginal() {
        return showOriginalCB != null;
    }

    /**
     * The checkbox that toggles displaying the original image.
     */
    private static class ShowOriginalCheckbox extends JCheckBox {
        private boolean trigger = true;

        public ShowOriginalCheckbox(String text) {
            super(text);
            addActionListener(e -> {
                if (trigger) {
                    Views.getActiveFilterable().setShowOriginal(isSelected());
                }
            });
            setName("show original");
        }

        public void deselectWithoutTriggering() {
            trigger = false;
            setSelected(false);
            trigger = true;
        }
    }
}