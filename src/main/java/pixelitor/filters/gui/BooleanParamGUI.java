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

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.event.ItemListener;

import static java.awt.FlowLayout.LEFT;

/**
 * The GUI component for a {@link BooleanParam}.
 */
public class BooleanParamGUI extends JPanel implements ParamGUI {
    private static final int BUTTON_SPACING = 50;
    
    private final BooleanParam model;
    private final JCheckBox checkBox;
    private ResetButton resetButton;
    private FilterButtonModel extraAction;

    public BooleanParamGUI(BooleanParam model, boolean addResetButton, FilterButtonModel extraAction) {
        super(new FlowLayout(LEFT));
        this.model = model;

        checkBox = new JCheckBox();
        checkBox.setSelected(model.isChecked());
        checkBox.addActionListener(e ->
            model.setValue(checkBox.isSelected(), false, true));
        add(checkBox);

        if (addResetButton) {
            add(Box.createHorizontalStrut(BUTTON_SPACING));
            resetButton = new ResetButton(model);
            add(resetButton);

            // It is important to add here a change listener and not an
            // action listener because we want to trigger this when the
            // checkbox is changed by the default button.
            // Swing is not consistent: in the case of JComboBox, action
            // listeners are called when the component is changed indirectly,
            // but not in the case of JCheckBox
            checkBox.addChangeListener(e -> resetButton.updateIcon());
        }

        if (extraAction != null) {
            this.extraAction = extraAction;
            add(Box.createHorizontalStrut(BUTTON_SPACING));
            add(extraAction.createGUI());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        checkBox.setEnabled(enabled);
        if (resetButton != null) {
            resetButton.setEnabled(enabled);
        }
        if (extraAction != null) {
            extraAction.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }

    @Override
    public void setName(String name) {
        super.setName(name);

        // help assertj-swing to find the checkBox
        checkBox.setName(name);
//        checkBox.setName(name + ".checkbox");
    }

    @Override
    public void updateGUI() {
        checkBox.setSelected(model.isChecked());
    }

    @Override
    public void setToolTip(String tip) {
        checkBox.setToolTipText(tip);
    }

    public void addItemListener(ItemListener itemListener) {
        checkBox.addItemListener(itemListener);
    }

    @Override
    public int getNumLayoutColumns() {
        return 2;
    }
}
