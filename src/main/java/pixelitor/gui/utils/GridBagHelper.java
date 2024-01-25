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
import static javax.swing.SwingConstants.RIGHT;

/**
 * Helper object for GridBagLayout
 */
public class GridBagHelper {
    private static final Insets insets = new Insets(2, 2, 2, 2);
    private static final GridBagConstraints labelConstraint = new GridBagConstraints(
        0, 0, 1, 1,
        0.0, 1.0, EAST, NONE, insets, 0, 0);
    private static final GridBagConstraints controlConstraint = new GridBagConstraints(
        0, 0, 1, 1,
        1.0, 1.0, WEST, HORIZONTAL, insets, 0, 0);
    private static final GridBagConstraints lastControlConstraint = new GridBagConstraints(
        0, 0, REMAINDER, 1,
        1.0, 1.0, WEST, HORIZONTAL, insets, 0, 0);

    private final Container container;
    private int autoIncrementedGridY = 0;

    public GridBagHelper(Container container) {
        this.container = container;

        labelConstraint.gridy = 0;
        controlConstraint.gridy = 0;
        lastControlConstraint.gridy = 0;
    }

    public void addLabel(String labelText, int gridX, int gridY) {
        JLabel label = new JLabel(labelText, RIGHT);
        addLabel(label, gridX, gridY);
    }

    private void addLabel(JLabel label, int gridX, int gridY) {
        labelConstraint.gridx = gridX;
        labelConstraint.gridy = gridY;
        container.add(label, labelConstraint);
    }

    /**
     * Adds the specified control to the right of the last label
     */
    public void addControl(Component c) {
        controlConstraint.gridx = labelConstraint.gridx + 1;
        controlConstraint.gridy = labelConstraint.gridy;
        container.add(c, controlConstraint);
    }

    public void addLabelAndTwoControls(String labelText, Component c1, Component c2) {
        addLabelAndControl(labelText, c1);
        addNextControl(c2);
    }

    public void addLabelAndControlNoStretch(String labelText, Component c) {
        addLabel(labelText, 0, autoIncrementedGridY);
        addControlNoStretch(c);
        autoIncrementedGridY++;
    }

    public void addTwoLabels(String text1, String text2) {
        addLabelAndControl(text1, new JLabel(text2));
    }

    public void addLabelAndControl(FilterSetting setting) {
        addLabelAndControl(setting.getName() + ":", setting.createGUI());
    }

    public void addLabelAndControl(FilterSetting setting, String controlName) {
        addLabelAndControl(setting.getName() + ":",
            setting.createGUI(controlName));
    }

    public void addLabelAndControl(String labelText, Component c) {
        addLabelAndControl(labelText, c, autoIncrementedGridY);
        autoIncrementedGridY++;
    }

    public void addLabelAndControlVerticalStretch(String labelText, Component c, double weightY) {
        JLabel label = new JLabel(labelText, RIGHT);
        labelConstraint.gridx = 0;
        labelConstraint.gridy = autoIncrementedGridY;
        container.add(label, labelConstraint);

        controlConstraint.gridx = 1;
        controlConstraint.gridy = autoIncrementedGridY;
        GridBagConstraints controlConstraints = (GridBagConstraints) controlConstraint.clone();
        controlConstraints.fill = BOTH;
        controlConstraints.weighty = weightY;
        container.add(c, controlConstraints);

        autoIncrementedGridY++;
    }

    public void addParam(FilterParam param) {
        addLabelAndControl(param.getName() + ":", param.createGUI());
    }

    public void addParam(FilterParam param, String guiName) {
        addLabelAndControl(param.getName() + ":", param.createGUI(guiName));
    }

    public void addLabelAndControl(String labelText, Component c, int gridY) {
        JLabel label = new JLabel(labelText, RIGHT);
        addTwoControls(label, c, gridY);
    }

    public void addTwoControls(Component c1, Component c2) {
        addTwoControls(c1, c2, autoIncrementedGridY);
        autoIncrementedGridY++;
    }

    public void addTwoControlsNoStretch(Component c1, Component c2) {
        controlConstraint.fill = NONE;
        addTwoControls(c1, c2, autoIncrementedGridY);
        autoIncrementedGridY++;
    }

    private void addTwoControls(Component c1, Component c2, int gridY) {
        labelConstraint.gridx = 0;
        labelConstraint.gridy = gridY;
        container.add(c1, labelConstraint);

        controlConstraint.gridx = 1;
        controlConstraint.gridy = gridY;

        container.add(c2, controlConstraint);
    }

    /**
     * Adds the specified control to the right of the
     * last label without stretching
     */
    public void addControlNoStretch(Component c) {
        controlConstraint.gridx = labelConstraint.gridx + 1;
        controlConstraint.gridy = labelConstraint.gridy;
        controlConstraint.fill = NONE;
        container.add(c, controlConstraint);

        controlConstraint.fill = HORIZONTAL; // reset
    }

    /**
     * Adds the specified control to the right of the last control
     */
    public void addNextControl(Component c) {
        controlConstraint.gridx++;
        container.add(c, controlConstraint);
    }

    public void addLabelAndLastControl(FilterSetting setting) {
        addLabelAndLastControl(setting.getName() + ":", setting.createGUI());
    }

    public void addLabelAndLastControl(String name, Component c) {
        addLabel(name, 0, autoIncrementedGridY);
        addLastControl(c);
        autoIncrementedGridY++;
    }

    public void addLastControl(Component c) {
        lastControlConstraint.gridx = labelConstraint.gridx + 1;
        lastControlConstraint.gridy = labelConstraint.gridy;
        container.add(c, lastControlConstraint);
    }

    public void addOnlyControl(Component c) {
        lastControlConstraint.gridx = 0;
        lastControlConstraint.gridy = autoIncrementedGridY;
        autoIncrementedGridY++;

        container.add(c, lastControlConstraint);
    }

    public void arrangeVertically(Iterable<? extends FilterSetting> settings) {
        for (FilterSetting setting : settings) {
            JComponent control = setting.createGUI();

            // so that assertj-swing can find it easily
            control.setName(setting.getName());

            int numColumns = ((ParamGUI) control).getNumLayoutColumns();
            if (numColumns == 1) {
                addOnlyControl(control);
            } else if (numColumns == 2) {
                addLabelAndLastControl(setting.getName() + ':', control);
            } else {
                throw new IllegalStateException("numColumns = " + numColumns);
            }
        }
    }
}
