/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

import pixelitor.utils.UpdateGUI;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.FlowLayout;

/**
 * The GUI for a BooleanParam.
 */
public class BooleanSelector extends JPanel implements ParamGUI {
    private final BooleanParam model;
    private final JCheckBox checkBox;
    private DefaultButton defaultButton;

    public BooleanSelector(BooleanParam model, AddDefaultButton addDefaultButton) {
        this.model = model;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        checkBox = new JCheckBox();
        checkBox.setSelected(model.isChecked());
        add(checkBox);

        checkBox.addActionListener(e -> model.setValue(checkBox.isSelected(), UpdateGUI.NO, true));

        if (addDefaultButton.isYes()) {
            add(Box.createHorizontalStrut(50));
            defaultButton = new DefaultButton(model);
            add(defaultButton);

            // It is important to add here a change listener and not an
            // action listener because we want to trigger this when the
            // checkbox is changed by the default button.
            // Swing is not consistent: in the case of JComboBox, action
            // listeners are called when the component is changed indirectly,
            // but not in the case of JCheckBox
            checkBox.addChangeListener(e -> defaultButton.updateState());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        checkBox.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void updateGUI() {
        checkBox.setSelected(model.isChecked());
    }

    @Override
    public void setToolTip(String tip) {
        checkBox.setToolTipText(tip);
    }

    public void addChangeListener(ChangeListener changeListener) {
        checkBox.addChangeListener(changeListener);
    }
}
