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

import pixelitor.gui.utils.GUIUtils;

import javax.swing.*;
import java.awt.FlowLayout;

import static java.awt.FlowLayout.LEFT;
import static javax.swing.BoxLayout.X_AXIS;

/**
 * Displays a JComboBox as the GUI for subclasses of {@link ListParam}.
 */
public class ComboBoxParamGUI<E> extends JPanel implements ParamGUI {
    private final JComboBox<E> comboBox;
    private final ResetButton resetButton;

    public ComboBoxParamGUI(ListParam<E> model, FilterButtonModel action) {
        resetButton = new ResetButton(model);
        comboBox = GUIUtils.createComboBox(model, e -> resetButton.updateState());

        if (action != null) {
            JPanel left = new JPanel(new FlowLayout(LEFT));
            left.add(comboBox);
            left.add(resetButton);

            setLayout(new BoxLayout(this, X_AXIS));
            add(left);
            add(Box.createGlue());
            add(action.createGUI());
        } else {
            setLayout(new FlowLayout(LEFT));
            add(comboBox);
            add(resetButton);
        }
    }

    @Override
    public void updateGUI() {
        // can be empty
    }

    @Override
    public void setEnabled(boolean enabled) {
        comboBox.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void setToolTip(String tip) {
        comboBox.setToolTipText(tip);
    }

    @Override
    public void setName(String name) {
        comboBox.setName(name);
    }

    @Override
    public int getNumLayoutColumns() {
        return 2;
    }
}