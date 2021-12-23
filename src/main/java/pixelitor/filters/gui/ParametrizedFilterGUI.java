/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.OpenImages;
import pixelitor.filters.ParametrizedFilter;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;


/**
 * An automatically built GUI for {@link ParametrizedFilter} filters.
 */
public class ParametrizedFilterGUI extends FilterGUI implements ParamAdjustmentListener {
    private ShowOriginalCB showOriginalCB;

    public ParametrizedFilterGUI(ParametrizedFilter filter, Drawable dr,
                                 boolean addShowOriginal, boolean reset) {
        this(filter, dr, addShowOriginal, reset, null);
    }

    public ParametrizedFilterGUI(ParametrizedFilter filter,
                                 Drawable dr,
                                 boolean addShowOriginal,
                                 boolean reset,
                                 Object otherInfo) {
        super(filter, dr);

        ParamSet paramSet = filter.getParamSet();
        if (reset) {
            paramSet.reset();

            // if the filter is not reset, then the ranges should
            // not be updated, even if the canvas size changed
            paramSet.updateOptions(dr, true);
        }

        paramSet.setAdjustmentListener(this);

        setupGUI(paramSet, addShowOriginal, otherInfo);

        paramAdjusted(); // force running the first filter preview
    }

    protected void setupGUI(ParamSet paramSet,
                            boolean addShowOriginal,
                            Object otherInfo) {
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
            showOriginalCB = new ShowOriginalCB("Show Original");
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

    @Override
    public void paramAdjusted() {
        if (hasShowOriginal()) {
            // if any parameter was changed, the "show original"
            // mode should be automatically stopped
            showOriginalCB.deselectWithoutTriggering();
        }
        runFilterPreview();
    }

    private boolean hasShowOriginal() {
        return showOriginalCB != null;
    }

    private static class ShowOriginalCB extends JCheckBox {
        private boolean trigger = true;

        public ShowOriginalCB(String text) {
            super(text);
            addActionListener(e -> {
                if (trigger) {
                    OpenImages.getActiveDrawableOrThrow().setShowOriginal(isSelected());
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