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

package pixelitor.gui.utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.REMAINDER;
import static java.awt.GridBagConstraints.WEST;

/**
 * Helper object for GridBagLayout
 */
public class GridBagHelper {
    private static final Insets insets = new Insets(2, 2, 2, 2);
    private static final GridBagConstraints labelConstraint = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, EAST, NONE, insets, 0, 0);
    private static final GridBagConstraints nextControlConstraint = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, WEST, HORIZONTAL, insets, 0, 0);
    private static final GridBagConstraints nextLastControlConstraint = new GridBagConstraints(0, 0, REMAINDER, 1, 0.0, 1.0, WEST, HORIZONTAL, insets, 0, 0);

    private final Container container;
    private int autoIncrementedGridY = 0;

    public GridBagHelper(Container container) {
        this.container = container;
    }

    public void addLabel(String labelText, int gridX, int gridY) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
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
        nextControlConstraint.gridx = labelConstraint.gridx + 1;
        nextControlConstraint.gridy = labelConstraint.gridy;
        container.add(c, nextControlConstraint);
    }

    public void addLabelWithTwoControls(String labelText, Component c1, Component c2) {
        addLabelWithControl(labelText, c1);
        addNextControl(c2);
    }

    public void addLabelWithControlNoFill(String labelText, Component c) {
        addLabel(labelText, 0, autoIncrementedGridY);
        addControlNoFill(c);
        autoIncrementedGridY++;
    }

    public void addTwoLabels(String text1, String text2) {
        addLabelWithControl(text1, new JLabel(text2));
    }

    public void addTwoControls(Component c1, Component c2) {
        addTwoControls(c1, c2, autoIncrementedGridY);
        autoIncrementedGridY++;
    }

    public void addLabelWithControl(String labelText, Component c) {
        addLabelWithControl(labelText, c, autoIncrementedGridY);
        autoIncrementedGridY++;
    }

    public void addLabelWithControl(String labelText, Component c, int gridY) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        addTwoControls(label, c, gridY);
    }

    private void addTwoControls(Component c1, Component c2, int gridY) {
        labelConstraint.gridx = 0;
        labelConstraint.gridy = gridY;
        container.add(c1, labelConstraint);

        nextControlConstraint.gridx = 1;
        nextControlConstraint.gridy = gridY;

        container.add(c2, nextControlConstraint);
    }

    /**
     * Adds the specified control to the right of the last label without stretching
     */
    public void addControlNoFill(Component c) {
        nextControlConstraint.gridx = labelConstraint.gridx + 1;
        nextControlConstraint.gridy = labelConstraint.gridy;
        nextControlConstraint.fill = NONE;
        container.add(c, nextControlConstraint);

        nextControlConstraint.fill = HORIZONTAL; // reset
    }

    /**
     * Adds the specified control to the right of the last control
     */
    public void addNextControl(Component c) {
        nextControlConstraint.gridx++;
        container.add(c, nextControlConstraint);
    }

    public void addLabelWithLastControl(String name, Component c) {
        addLabel(name, 0, autoIncrementedGridY);
        addLastControl(c);
        autoIncrementedGridY++;
    }

    public void addLastControl(Component c) {
        nextLastControlConstraint.gridx = labelConstraint.gridx + 1;
        nextLastControlConstraint.gridy = labelConstraint.gridy;
        container.add(c, nextLastControlConstraint);
    }

    public void addOnlyControlToRow(Component c, int row) {
        nextLastControlConstraint.gridx = 0;
        nextLastControlConstraint.gridy = row;
        container.add(c, nextLastControlConstraint);
    }
}
