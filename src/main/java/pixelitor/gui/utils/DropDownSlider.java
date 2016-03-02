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

package pixelitor.gui.utils;

import pixelitor.utils.IconUtils;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A textfield with a drop down button that shows a slider in a popup window
 */
public class DropDownSlider extends JComponent {
    private static final Icon downIconEnabled = IconUtils.loadIcon("dropdown_enabled.gif");
    private static final Icon downIconDisabled = IconUtils.loadIcon("dropdown_disabled.gif");

    private final IntTextField textField;
    private JButton dropDownButton;
    private final JPopupMenu popupMenu;
    private boolean dropDownEnabled = true;
    private final JSlider slider;

    public DropDownSlider(int value, int minValue, int maxValue, boolean limitRange) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        textField = new IntTextField(String.valueOf(value), minValue, maxValue, limitRange, 4);
        add(textField);

        initDropDownButton();
        textField.add(dropDownButton);

        slider = new JSlider(JSlider.HORIZONTAL, minValue, maxValue, value);
        popupMenu = new JPopupMenu();
        popupMenu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
        popupMenu.add(slider);

        connectSlideAndTextField();
    }

    private void initDropDownButton() {
        dropDownButton = new JButton(downIconEnabled);
        dropDownButton.setDisabledIcon(downIconDisabled);
        dropDownButton.putClientProperty("JComponent.sizeVariant", "mini");

        dropDownButton.setRequestFocusEnabled(false);
        dropDownButton.setInheritsPopupMenu(true);
        dropDownButton.setBorderPainted(false);
        dropDownButton.setFocusPainted(false);

        dropDownButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dropDown();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                dropDownButton.setBorderPainted(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                dropDownButton.setBorderPainted(false);
            }
        });

        dropDownButton.setIconTextGap(0);
        dropDownButton.setBorder(null);
        dropDownButton.setMargin(new Insets(0, 0, 0, 0));

        setDropDownButtonSize();
    }

    private void setDropDownButtonSize() {
        Dimension preferredSize = textField.getPreferredSize();

        int dropDownButtonWidth = 17;
        int dropDownButtonHeight = 22;

        int boundsX = preferredSize.width - dropDownButtonWidth;
        int boundsY = (preferredSize.height - dropDownButtonHeight) / 2;
        dropDownButton.setBounds(boundsX, boundsY, dropDownButtonWidth, dropDownButtonHeight); // Works only for the Nimbus L&F
    }

    private void connectSlideAndTextField() {
        slider.addChangeListener(e -> {
            int value1 = slider.getValue();
            textField.setText(String.valueOf(value1));
            if (!slider.getValueIsAdjusting()) {
                textField.fireActionPerformed();
            }
        });

        textField.addActionListener(e -> {
            int value1 = textField.getIntValue();
            int sliderValue = slider.getValue();
            if (value1 != sliderValue) {
                slider.setValue(value1);
            }
        });
    }

    public int getValue() {
        return textField.getIntValue();
    }

    public void setValue(int newValue) {
        textField.setText(String.valueOf(newValue));
        slider.setValue(newValue);
    }

    public void addActionListener(ActionListener listener) {
        textField.addActionListener(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        dropDownButton.setEnabled(enabled);
        dropDownEnabled = enabled;
    }

    private void dropDown() {
        if (dropDownEnabled) {
            if (popupMenu.isShowing()) {
                popupMenu.setVisible(false);
            } else {
                Dimension size = getSize();
                Dimension popupSize = popupMenu.getPreferredSize();
                popupMenu.show(this, size.width - popupSize.width, size.height);
            }
        }
    }

    public void setTFName(String name) {
        textField.setName(name);
    }
}
