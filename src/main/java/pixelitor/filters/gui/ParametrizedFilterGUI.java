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

package pixelitor.filters.gui;

import pixelitor.filters.ParametrizedFilter;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.Drawable;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * A GUI for parametrized filters
 */
public class ParametrizedFilterGUI extends FilterGUI implements ParamAdjustmentListener {
    /**
     * Controls whether the params are reset to the default values when a new
     * ParametrizedAdjustPanel is created
     */
    private static boolean resetParams = true;
    private ShowOriginalCB showOriginalCB;

    public ParametrizedFilterGUI(ParametrizedFilter filter, Drawable dr, ShowOriginal addShowOriginal) {
        this(filter, dr, null, addShowOriginal);
    }

    public ParametrizedFilterGUI(ParametrizedFilter filter, Drawable dr, Object otherInfo, ShowOriginal addShowOriginal) {
        super(filter, dr);

        ParamSet params = filter.getParamSet();
        if (resetParams) {
            params.reset();
            params.considerImageSize(dr.getComp().getCanvas().getImBounds());
        }
        params.setAdjustmentListener(this);

        setupGUI(params, otherInfo, addShowOriginal);

        paramAdjusted();
    }

    protected void setupGUI(ParamSet params, Object otherInfo, ShowOriginal addShowOriginal) {
        JPanel filterParamsPanel = createFilterParamsPanel(params.getParams());
        JPanel filterActionsPanel = createFilterActionsPanel(params.getActions(), addShowOriginal, 3);

        this.setLayout(new BorderLayout());
        this.add(filterParamsPanel, BorderLayout.CENTER);
        this.add(filterActionsPanel, BorderLayout.SOUTH);
    }

    /**
     * This can be overridden if a custom arrangement is necessary
     */
    public JPanel createFilterParamsPanel(List<FilterParam> paramList) {
        return GUIUtils.arrangeParamsInVerticalGridBag(paramList);
    }

    protected JPanel createFilterActionsPanel(List<FilterAction> actionList, ShowOriginal addShowOriginal, int maxControlsInRow) {
        int numControls = actionList.size();
        if (addShowOriginal.isYes()) {
            numControls++;
            showOriginalCB = new ShowOriginalCB("Show Original");
        }
        JPanel faPanel;

        if(numControls <= maxControlsInRow) {
            faPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        } else {
            int cols = (numControls + 1) / 2;
            faPanel = new JPanel(new GridLayout(2, cols));
        }
        if (addShowOriginal.isYes()) {
            faPanel.add(showOriginalCB);
        }
        for (FilterAction action : actionList) {
            // all the buttons go in one row
            JButton button = (JButton) action.createGUI();
            faPanel.add(button);
        }

        return faPanel;
    }

    @Override
    public void paramAdjusted() {
        if (hasShowOriginal()) {
            // if any parameter was changed, the "show original"
            // mode should be automatically stopped
            showOriginalCB.deselectWithoutTriggering();
        }
        super.runFilterPreview();
    }

    private boolean hasShowOriginal() {
        return showOriginalCB != null;
    }

    public static void setResetParams(boolean resetParams) {
        ParametrizedFilterGUI.resetParams = resetParams;
    }

    private static class ShowOriginalCB extends JCheckBox {
        private boolean trigger = true;

        public ShowOriginalCB(String text) {
            super(text);
            addActionListener(e -> {
                if (trigger) {
                    ImageComponents.getActiveDrawableOrNull()
                            .setShowOriginal(isSelected());
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