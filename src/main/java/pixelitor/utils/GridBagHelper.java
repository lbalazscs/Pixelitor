/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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
package pixelitor.utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Helper object for GridBagLayout
 */
public class GridBagHelper {
    private static final Insets insets = new Insets(2, 2, 2, 2);
    private static final GridBagConstraints labelConstraint = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.EAST, GridBagConstraints.NONE, insets, 0, 0);
    private static final GridBagConstraints nextControlConstraint = new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0);
    private static final GridBagConstraints nextLastControlConstraint = new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insets, 0, 0);
    private Container container;

    public GridBagHelper(Container container) {
        this.container = container;
    }

    public void addLabel(String labelText, int gridX, int gridY) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        addLabel(label, gridX, gridY);
    }

    public void addLabel(JLabel label, int gridX, int gridY) {
        labelConstraint.gridx = gridX;
        labelConstraint.gridy = gridY;
        container.add(label, labelConstraint);
    }

    /**
     * Adds the specified control to the right of the last label
     */
    public void addControl(Component component) {
        nextControlConstraint.gridx = labelConstraint.gridx + 1;
        nextControlConstraint.gridy = labelConstraint.gridy;
        container.add(component, nextControlConstraint);
    }

    public void addLabelWithControl(String labelText, Component component, int gridY) {
        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        labelConstraint.gridx = 0;
        labelConstraint.gridy = gridY;
        container.add(label, labelConstraint);

        nextControlConstraint.gridx = 1;
        nextControlConstraint.gridy = gridY;

        container.add(component, nextControlConstraint);
    }

    /**
     * Adds the specified control to the right of the last label without stretching
     */
    public void addControlNoFill(Component component) {
        nextControlConstraint.gridx = labelConstraint.gridx + 1;
        nextControlConstraint.gridy = labelConstraint.gridy;
        nextControlConstraint.fill = GridBagConstraints.NONE;
        container.add(component, nextControlConstraint);

        nextControlConstraint.fill = GridBagConstraints.HORIZONTAL; // reset
    }


    /**
     * Adds the specified control to the right of the last control
     */
    public void addNextControl(Component component) {
        nextControlConstraint.gridx++;
        container.add(component, nextControlConstraint);
    }

    public void addLastControl(Component component) {
        nextLastControlConstraint.gridx = labelConstraint.gridx + 1;
        nextLastControlConstraint.gridy = labelConstraint.gridy;
        container.add(component, nextLastControlConstraint);
    }

    public void addOnlyControlToRow(Component component, int row) {
        nextLastControlConstraint.gridx = 0;
        nextLastControlConstraint.gridy = row;
        container.add(component, nextLastControlConstraint);
    }
}
