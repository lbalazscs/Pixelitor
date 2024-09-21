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
import javax.swing.text.*;

/**
 * A DocumentFilter that restricts text input to integer values within a range.
 */
public class IntDocumentFilter extends DocumentFilter {
    private final int min;
    private final int max;
    private final boolean isRangeConstrained;

    public IntDocumentFilter() {
        this.min = -1;
        this.max = -1;
        this.isRangeConstrained = false;
    }

    public IntDocumentFilter(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        this.min = min;
        this.max = max;
        isRangeConstrained = true;
    }

    public void applyOn(JTextField tf) {
        ((AbstractDocument) tf.getDocument()).setDocumentFilter(this);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        Document doc = fb.getDocument();
        int currentLength = doc.getLength();
        String currentText = doc.getText(0, currentLength);
        String before = currentText.substring(0, offset);
        String after = currentText.substring(length + offset, currentLength);
        String newString = before + (text == null ? "" : text) + after;
        validateInteger(newString, offset);
        fb.replace(offset, length, text, attrs);
    }

    private void validateInteger(String newString, int offset) throws BadLocationException {
        int newInt;
        try {
            newInt = Integer.parseInt(newString);
        } catch (NumberFormatException e) {
            throw new BadLocationException(newString, offset);
        }

        if (isRangeConstrained && (newInt < min || newInt > max)) {
            throw new BadLocationException(newString, offset);
        }
    }
}
