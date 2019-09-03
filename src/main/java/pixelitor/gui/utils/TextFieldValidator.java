/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

/**
 * An object that checks the contents of a text field
 */
public interface TextFieldValidator {
    // the only non-static method in this interface
    ValidationResult check(JTextField textField);

    static ValidationResult hasValidDouble(JTextField textField) {
        String text = textField.getText().trim();
        try {
            //noinspection ResultOfMethodCallIgnored
            Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return ValidationResult.error(text + " is not a valid number.");
        }
        return ValidationResult.ok();
    }

    static ValidationResult hasValidPositiveInt(JTextField textField, boolean allowZero) {
        String text = textField.getText().trim();
        try {
            //noinspection ResultOfMethodCallIgnored
            int value = Integer.parseInt(text);
            if (value == 0 && !allowZero) {
                return ValidationResult.error("0 is not allowed here.");
            }
            if (value < 0) {
                return ValidationResult.error("Negative numbers are not allowed here.");
            }
        } catch (NumberFormatException ex) {
            return ValidationResult.error(text + " is not a valid number.");
        }
        return ValidationResult.ok();
    }

    static JLayer<JTextField> createPositiveIntLayerFor(JTextField tf, boolean allowZero) {
        TFValidationLayerUI layerUI = new TFValidationLayerUI(
            textField1 -> hasValidPositiveInt(textField1, allowZero));
        return new JLayer<>(tf, layerUI);
    }
}
