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

package pixelitor.gui.utils;

import pixelitor.filters.gui.FilterParam;
import pixelitor.filters.gui.FilterSetting;
import pixelitor.filters.gui.ParamGUI;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import static java.awt.GridBagConstraints.*;

/**
 * Helper object for GridBagLayout.
 */
public class GridBagHelper {
    private static final int DEFAULT_PADDING = 2;
    private static final Insets DEFAULT_INSETS = new Insets(DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING, DEFAULT_PADDING);

    private static final GridBagConstraints labelConstraints = new GridBagConstraints(
        0, 0, 1, 1, 0.0, 1.0,
        EAST, NONE, DEFAULT_INSETS, 0, 0);
    private static final GridBagConstraints componentConstraints = new GridBagConstraints(
        0, 0, 1, 1, 1.0, 1.0,
        WEST, HORIZONTAL, DEFAULT_INSETS, 0, 0);
    private static final GridBagConstraints lastComponentConstraints = new GridBagConstraints(
        0, 0, REMAINDER, 1, 1.0, 1.0,
        WEST, HORIZONTAL, DEFAULT_INSETS, 0, 0);

    private final Container container;
    private int currentRow = 0;

    public GridBagHelper(Container container) {
        this.container = container;

        initConstraints();
    }

    public GridBagHelper(Container container, int padding) {
        this.container = container;

        initConstraints();
        if (padding != DEFAULT_PADDING) {
            Insets newPadding = new Insets(padding, padding, padding, padding);
            labelConstraints.insets = newPadding;
            componentConstraints.insets = newPadding;
            lastComponentConstraints.insets = newPadding;
        }
    }

    private static void initConstraints() {
        labelConstraints.gridy = 0;
        componentConstraints.gridy = 0;
        lastComponentConstraints.gridy = 0;
    }

    public void addLabel(String labelText, int column, int row) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        addLabel(label, column, row);
    }

    private void addLabel(JLabel label, int column, int row) {
        labelConstraints.gridx = column;
        labelConstraints.gridy = row;
        container.add(label, labelConstraints);
    }

    /**
     * Adds the given control to the right of the most recently added label.
     */
    public void addControl(Component c) {
        componentConstraints.gridx = labelConstraints.gridx + 1;
        componentConstraints.gridy = labelConstraints.gridy;
        container.add(c, componentConstraints);
    }

    public void addLabelAndTwoControls(String labelText, Component c1, Component c2) {
        addLabelAndControl(labelText, c1);
        addNextControl(c2);
    }

    public void addLabelAndControlNoStretch(String labelText, Component c) {
        addLabel(labelText, 0, currentRow);
        addControlNoStretch(c);
        currentRow++;
    }

    public void addLabelAndTwoControlsNoStretch(String labelText, Component c1, Component c2) {
        addLabel(labelText, 0, currentRow);
        addTwoControlsNoStretch(c1, c2);
        currentRow++;
    }

    public void addTwoLabels(String leftLabelText, String rightLabelText) {
        addLabelAndControl(leftLabelText, new JLabel(rightLabelText));
    }

    public void addLabelAndControl(FilterSetting setting) {
        addLabelAndControl(setting.getName() + ":", setting.createGUI());
    }

    public void addLabelAndControl(FilterSetting setting, String lookupName) {
        addLabelAndControl(setting.getName() + ":",
            setting.createGUI(lookupName));
    }

    public void addLabelAndControl(String labelText, Component c) {
        addLabelAndControl(labelText, c, currentRow);
        currentRow++;
    }

    public void addVerticallyStretchable(String labelText, Component c, double verticalWeight) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        labelConstraints.gridx = 0;
        labelConstraints.gridy = currentRow;
        container.add(label, labelConstraints);

        componentConstraints.gridx = 1;
        componentConstraints.gridy = currentRow;
        GridBagConstraints controlConstraints = (GridBagConstraints) componentConstraints.clone();
        controlConstraints.fill = BOTH;
        controlConstraints.weighty = verticalWeight;
        container.add(c, controlConstraints);

        currentRow++;
    }

    public void addParam(FilterParam param) {
        addLabelAndControl(param.getName() + ":", param.createGUI());
    }

    public void addParam(FilterParam param, String lookupName) {
        addLabelAndControl(param.getName() + ":", param.createGUI(lookupName));
    }

    public void addLabelAndControl(String labelText, Component c, int row) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        addTwoControls(label, c, row);
    }

    public void addTwoControls(Component c1, Component c2) {
        addTwoControls(c1, c2, currentRow);
        currentRow++;
    }

    public void addTwoControls(Component c1, Component c2, int row) {
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        container.add(c1, labelConstraints);

        componentConstraints.gridx = 1;
        componentConstraints.gridy = row;

        container.add(c2, componentConstraints);
    }

    /**
     * Adds the given control to the right of the last label without stretching.
     */
    public void addControlNoStretch(Component c) {
        componentConstraints.gridx = labelConstraints.gridx + 1;
        componentConstraints.gridy = labelConstraints.gridy;
        componentConstraints.fill = NONE;
        container.add(c, componentConstraints);

        componentConstraints.fill = HORIZONTAL; // reset
    }

    public void addTwoControlsNoStretch(Component c1, Component c2) {
        componentConstraints.gridx = labelConstraints.gridx + 1;
        componentConstraints.gridy = labelConstraints.gridy;
        componentConstraints.fill = NONE;
        container.add(c1, componentConstraints);

        componentConstraints.gridx = labelConstraints.gridx + 2;
        container.add(c2, componentConstraints);

        componentConstraints.fill = HORIZONTAL; // reset
    }

    /**
     * Adds the given control to the right of the last control.
     */
    public void addNextControl(Component c) {
        componentConstraints.gridx++;
        container.add(c, componentConstraints);
    }

    public void addLabelAndLastControl(FilterSetting setting) {
        addLabelAndLastControl(setting.getName() + ":", setting.createGUI());
    }

    public void addLabelAndLastControl(String labelText, Component c) {
        addLabel(labelText, 0, currentRow);
        addLastControl(c);
        currentRow++;
    }

    /**
     * Adds a component that takes up all remaining columns.
     */
    public void addLastControl(Component c) {
        lastComponentConstraints.gridx = labelConstraints.gridx + 1;
        lastComponentConstraints.gridy = labelConstraints.gridy;
        container.add(c, lastComponentConstraints);
    }

    public void addFullRow(FilterParam param) {
        addFullRow(param.createGUI());
    }

    public void addFullRow(Component c) {
        lastComponentConstraints.gridx = 0;
        lastComponentConstraints.gridy = currentRow;
        currentRow++;

        container.add(c, lastComponentConstraints);
    }

    public void arrangeVertically(Iterable<? extends FilterSetting> settings) {
        for (FilterSetting setting : settings) {
            JComponent control = setting.createGUI(setting.getName());

            int numColumns = ((ParamGUI) control).getNumLayoutColumns();
            if (numColumns == 1) {
                addFullRow(control);
            } else if (numColumns == 2) {
                addLabelAndLastControl(setting.getName() + ':', control);
            } else {
                throw new IllegalStateException("numColumns = " + numColumns);
            }
        }
    }

    public void addVerticalSpace(int height) {
        labelConstraints.gridx = 0;
        labelConstraints.gridy = currentRow;

        container.add(Box.createVerticalStrut(height), labelConstraints);

        currentRow++;
    }
}
