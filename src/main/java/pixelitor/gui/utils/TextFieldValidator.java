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

import pixelitor.utils.Utils;

import javax.swing.*;
import java.text.ParseException;

/**
 * An object that checks the contents of a text field
 */
public interface TextFieldValidator {
    // the only non-static method in this interface
    ValidationResult check(JTextField textField);

    static ValidationResult hasPositiveDouble(JTextField textField, String label) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.invalidEmpty(label);
        }
        double value;
        try {
            value = Utils.parseLocalizedDouble(text);
        } catch (ParseException ex) {
            return ValidationResult.invalid(text + " isn't a valid number for <b>" + label + "</b>");
        }
        if (value > 0) {
            return ValidationResult.valid();
        } else if (value == 0) {
            return ValidationResult.invalidZero(label);
        } else {
            return ValidationResult.invalidNegative(label);
        }
    }

    static ValidationResult hasPositiveInt(JTextField textField, String label, boolean allowZero) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.invalidEmpty(label);
        }
        try {
            int value = Integer.parseInt(text);
            if (value == 0 && !allowZero) {
                return ValidationResult.invalidZero(label);
            }
            if (value < 0) {
                return ValidationResult.invalidNegative(label);
            }
        } catch (NumberFormatException ex) {
            return ValidationResult.invalid("<b>" + label + "</b> must be an integer.");
        }
        return ValidationResult.valid();
    }

    static JLayer<JTextField> createPositiveIntLayer(String label,
                                                     JTextField tf,
                                                     boolean allowZero) {
        return TFValidationLayerUI.wrapWithValidation(tf, textField1 -> hasPositiveInt(textField1, label, allowZero));
    }
}
