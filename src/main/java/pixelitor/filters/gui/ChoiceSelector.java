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

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Displays a JComboBox as the GUI for an IntChoiceParam or an EnumParam
 */
public class ChoiceSelector extends JPanel implements ActionListener, ParamGUI {
    private final JComboBox<IntChoiceParam.Value> comboBox;
    private final DefaultButton defaultButton;

    public ChoiceSelector(ComboBoxModel model, ActionSetting action) {
        assert model instanceof Resettable;



        comboBox = new JComboBox<>(model);
        comboBox.addActionListener(this);
        // workaround for nimbus bug
        Dimension comboPreferredSize = comboBox.getPreferredSize();
        comboBox.setPreferredSize(new Dimension(comboPreferredSize.width + 3, comboPreferredSize.height));


        defaultButton = new DefaultButton((Resettable) model);
//        int buttonSize = comboBox.getPreferredSize().height;
//        defaultButton.setPreferredSize(new Dimension(buttonSize, buttonSize));

        if (action != null) {
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
            left.add(comboBox);
            left.add(defaultButton);

            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(left);
            add(Box.createGlue());
            add(action.createGUI());
        } else {
            setLayout(new FlowLayout(FlowLayout.LEFT));
            add(comboBox);
            add(defaultButton);
        }
    }

    @Override
    public void updateGUI() {
        // can be empty
    }

    @Override
    public void setEnabled(boolean enabled) {
        comboBox.setEnabled(enabled);
        defaultButton.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        model.setSelectedItem(comboBox.getSelectedItem());
        defaultButton.updateState();
    }

    @Override
    public void setToolTip(String tip) {
        comboBox.setToolTipText(tip);
    }
}