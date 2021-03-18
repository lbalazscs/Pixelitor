/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static java.lang.Integer.parseInt;

/**
 * A JTextfield that allows only input consisting
 * of numbers and nothing else.
 */
public class IntTextField extends JTextField implements KeyListener {
    private final int defaultValue;
    private int minValue;
    private int maxValue;
    private boolean limitRange;

    public IntTextField(int defaultValue, int minValue, int maxValue, boolean limitRange, int columns) {
        super(String.valueOf(defaultValue), columns);

        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.limitRange = limitRange;

        init();
    }

    public IntTextField(int defaultValue, int columns) {
        super(String.valueOf(defaultValue), columns);

        this.defaultValue = defaultValue;

        init();
    }

    private void init() {
        addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (!(Character.isDigit(c) ||
            c == KeyEvent.VK_BACK_SPACE ||
            c == KeyEvent.VK_ENTER ||
            c == KeyEvent.VK_DELETE)) {

            if (c != 26) { // 26 occurs while undoing a change, should not beep
                getToolkit().beep();
            }

            e.consume();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public int getIntValue() {
        String s = getText().trim();

        int intValue;
        try {
            intValue = parseInt(s);
        } catch (NumberFormatException e) {
            // still can happen if the value is
            // too large for an int, like 9999999999999
            intValue = defaultValue;
            setText(String.valueOf(intValue));
        }

        if (limitRange) {
            if (intValue > maxValue) {
                intValue = maxValue;
                setText(String.valueOf(intValue));
            } else if (intValue < minValue) {
                intValue = minValue;
                setText(String.valueOf(intValue));
            }
        }
        return intValue;
    }

    @Override
    public void fireActionPerformed() {
        // makes public this method that is protected in JTextField
        super.fireActionPerformed();
    }
}
