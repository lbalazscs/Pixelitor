/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A textfield with a drop down button that shows a slider in a popup window
 */
public class DropDownSlider extends JComponent implements MouseListener, ChangeListener, ActionListener {

    private static final Icon downIconEnabled = IconUtils.loadIcon("dropdown_enabled.gif");
    private static final Icon downIconDisabled = IconUtils.loadIcon("dropdown_disabled.gif");

    private final IntTextField textField;
    private final JButton dropDownButton;
    private final JPopupMenu popupMenu;
    private boolean dropDownEnabled = true;
    private final JSlider slider;

    public DropDownSlider(int value, int minValue, int maxValue, boolean limitRange) {

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        textField = new IntTextField(String.valueOf(value), minValue, maxValue, limitRange, 4);
        add(textField);

//        int size = textField.getPreferredSize().height - 6;


        dropDownButton = new JButton(downIconEnabled);
        dropDownButton.setDisabledIcon(downIconDisabled);
        dropDownButton.putClientProperty("JComponent.sizeVariant", "mini");


        dropDownButton.setRequestFocusEnabled(false);
        dropDownButton.setInheritsPopupMenu(true);

        dropDownButton.setBorderPainted(false);
        dropDownButton.setFocusPainted(false);

        dropDownButton.addMouseListener(this);
//        dropDownButton.setMaximumSize(new Dimension(hSize, vSize));
//        dropDownButton.setMinimumSize(new Dimension(hSize, vSize));
//        dropDownButton.setPreferredSize(new Dimension(hSize, vSize));

        dropDownButton.setIconTextGap(0);
        dropDownButton.setBorder(null);
        dropDownButton.setMargin(new Insets(0, 0, 0, 0));

        textField.add(dropDownButton);
        Dimension preferredSize = textField.getPreferredSize();

        int dropDownButtonWidth = 17;
        int dropDownButtonHeight = 22;


        int boundsX = preferredSize.width - dropDownButtonWidth;
        int boundsY = (preferredSize.height - dropDownButtonHeight) / 2;
        dropDownButton.setBounds(boundsX, boundsY, dropDownButtonWidth, dropDownButtonHeight); // Works only for the Nimbus L&F

        slider = new JSlider(JSlider.HORIZONTAL, minValue, maxValue, value);
        popupMenu = new JPopupMenu();
        popupMenu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 1));
        popupMenu.add(slider);

        slider.addChangeListener(this);

        textField.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o == textField) {
            int value = textField.getIntValue();
            int sliderValue = slider.getValue();
            if (value != sliderValue) {
                slider.setValue(value);
            }
        }
    }


    @Override
    public void stateChanged(ChangeEvent e) {
        Object o = e.getSource();
        if (o == slider) {
            int value = slider.getValue();
            textField.setText(String.valueOf(value));
            if (!slider.getValueIsAdjusting()) {
                textField.fireActionPerformed();
            }
        }
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
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (popupMenu.isShowing() && dropDownEnabled) {
            popupMenu.setVisible(false);
        } else if (dropDownEnabled) {
            Dimension size = getSize();
            Dimension popupSize = popupMenu.getPreferredSize();
            popupMenu.show(this, size.width - popupSize.width, size.height);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        dropDownButton.setBorderPainted(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        dropDownButton.setBorderPainted(false);
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        dropDownButton.setEnabled(enabled);
        dropDownEnabled = enabled;
    }


}
