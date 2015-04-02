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

import pixelitor.ImageComponents;
import pixelitor.filters.FilterWithParametrizedGUI;
import pixelitor.utils.GridBagHelper;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ParametrizedAdjustPanel extends AdjustPanel implements ParamAdjustmentListener {
    /**
     * Controls whether the params are reset to the default values when a new
     * ParametrizedAdjustPanel is created
     */
    private static boolean resetParams = true;
    private ShowOriginalCB showOriginalCB;

    public ParametrizedAdjustPanel(FilterWithParametrizedGUI filter, boolean addShowOriginal) {
        this(filter, null, addShowOriginal);
    }

    public ParametrizedAdjustPanel(FilterWithParametrizedGUI filter, Object otherInfo, boolean addShowOriginal) {
        super(filter);

        ParamSet params = filter.getParamSet();
        if (resetParams) {
            params.reset();
            params.considerImageSize(ImageComponents.getActiveComp().get().getCanvas().getBounds());
        }
        params.setAdjustmentListener(this);

        setupGUI(params, otherInfo, addShowOriginal);

        paramAdjusted();
    }

    /**
     * This can be overridden if a custom GUI is necessary
     */
    protected void setupGUI(ParamSet params, Object otherInfo, boolean addShowOriginal) {
        setupControlsInColumn(this, params, addShowOriginal);
    }

    public void setupControlsInColumn(JPanel parent, Iterable<GUIParam> params, boolean addShowOriginal) {
        parent.setLayout(new GridBagLayout());

        int row = 0;
        JPanel buttonsPanel = null;

        GridBagHelper gbHelper = new GridBagHelper(parent);

        for (GUIParam param : params) {
            JComponent control = param.createGUI();

            if (param instanceof ActionParam) { // all the buttons go in one row
                if (buttonsPanel == null) {
                    buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    gbHelper.addOnlyControlToRow(buttonsPanel, row);
                }
                buttonsPanel.add(control);
                control.setName(param.getName());
            } else {
                int numColumns = param.getNrOfGridBagCols();
                if (numColumns == 1) {
                    gbHelper.addOnlyControlToRow(control, row);
                } else if (numColumns == 2) {
                    gbHelper.addLabel(param.getName() + ':', 0, row);
                    gbHelper.addLastControl(control);
                }
            }

            row++;
        }
        if (addShowOriginal) {
            gbHelper.addLabel("Show Original:", 0, row);

            showOriginalCB = new ShowOriginalCB();

            gbHelper.addLastControl(showOriginalCB);
        }
    }

    @Override
    public void paramAdjusted() {
        showOriginalCB.deselectWithoutTriggering();
        super.executeFilterPreview();
    }

    public static void setResetParams(boolean resetParams) {
        ParametrizedAdjustPanel.resetParams = resetParams;
    }

    static class ShowOriginalCB extends JCheckBox {
        private boolean trigger = true;

        public ShowOriginalCB() {
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (trigger) {
                        Utils.setShowOriginal(isSelected());
                    }
                }
            });
        }

        public void deselectWithoutTriggering() {
            trigger = false;
            setSelected(false);
            trigger = true;
        }
    }
}