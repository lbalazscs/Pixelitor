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

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.FlowLayout.CENTER;
import static javax.swing.SwingConstants.HORIZONTAL;

/**
 * A special combo box that shows a slider in the popup window.
 */
public class DropDownSlider extends JComboBox<String> {
    private final JPopupMenu popupMenu;
    private final JSlider slider;
    private Dimension preferredSize;

    public DropDownSlider(int minValue, int value, int maxValue) {
        super(new String[]{String.valueOf(value)});
        recalcPreferredSize();
        setEditable(true);

        new IntDocumentFilter(minValue, maxValue).applyOn(getEditorComponent());

        slider = new JSlider(HORIZONTAL, minValue, maxValue, value);
        popupMenu = new JPopupMenu();
        popupMenu.setLayout(new FlowLayout(CENTER, 5, 1));
        popupMenu.add(slider);

        slider.addChangeListener(e ->
            setSelectedItem(String.valueOf(slider.getValue())));

        replaceMouseListeners();
    }

    @Override
    protected void fireActionEvent() {
        // Fire action events only when the slider is not adjusting.
        // It would be user-friendly to fire even when it's adjusting,
        // but at the moment this would generate a lot of undo events.
        if (slider.getValueIsAdjusting()) {
            return;
        }
        super.fireActionEvent();
    }

    private void replaceMouseListeners() {
        MouseAdapter newMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setPopupVisible(true);
            }
        };

        // it this necessary?
        GUIUtils.replaceMouseListeners(this, newMouseListener);

        for (Component c : getComponents()) {
            if (c instanceof JButton b) {
                GUIUtils.replaceMouseListeners(b, newMouseListener);
                break;
            }
        }
    }

    @Override
    public void setPopupVisible(boolean visible) {
        if (visible) {
            int popupX = getWidth() - popupMenu.getPreferredSize().width;
            int popupY = getHeight();
            popupMenu.show(this, popupX, popupY);
        } else {
            popupMenu.setVisible(false);
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        replaceMouseListeners();
        if (popupMenu != null) {
            SwingUtilities.updateComponentTreeUI(popupMenu);
        }
        recalcPreferredSize();
    }

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    // The default preferred size is larger than required,
    // and it would increase the width of the whole layer panel.
    private void recalcPreferredSize() {
        JTextField tf = getEditorComponent();
        int stringWidth = tf.getFontMetrics(tf.getFont()).stringWidth("100");
        preferredSize = super.getPreferredSize();
        if (Themes.getCurrent().isFlat()) {
            preferredSize.width = 20 + (int) (stringWidth * 2.6);
        } else {
            preferredSize.width = stringWidth + 40;
        }
    }

    public int getValue() {
        try {
            return Integer.parseInt((String) getSelectedItem());
        } catch (NumberFormatException e) {
            setSelectedItem("100");
            return 100;
        }
    }

    public void setValue(int newValue) {
        setSelectedItem(String.valueOf(newValue));
        slider.setValue(newValue);
    }

    private JTextField getEditorComponent() {
        return (JTextField) getEditor().getEditorComponent();
    }
}
