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

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * A JTextfield that allows only input consisting of numbers and nothing else.
 */
public class IntTextField extends JTextField implements KeyListener {
    private int minValue;
    private int maxValue;
    private boolean limitRange;

    public IntTextField(String text, int minValue, int maxValue, boolean limitRange, int columns) {
        super(text, columns);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.limitRange = limitRange;
        init();
    }

    public IntTextField(String text) {
        super(text);
        init();
    }

    public IntTextField(int columns) {
        super(columns);
        init();
    }

    private void init() {
        addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char c = e.getKeyChar();
        if (!((Character.isDigit(c) ||
                (c == KeyEvent.VK_BACK_SPACE) ||
                (c == KeyEvent.VK_ENTER) ||
                (c == KeyEvent.VK_DELETE)))) {

            if ((int) c != 26) { // 26 occurs while undoing a change, should not beep
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
        String s = getText();
        int intValue = Integer.parseInt(s);
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

    public void setRange(int min, int max) {
        this.minValue = min;
        this.maxValue = max;
        this.limitRange = true;
    }

    @Override
    public void fireActionPerformed() {
        // makes public this method that is protected in JTextField
        super.fireActionPerformed();
    }
}
