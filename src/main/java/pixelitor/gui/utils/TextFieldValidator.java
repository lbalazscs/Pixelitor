/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

/**
 * An object that checks the contents of a text field
 */
public interface TextFieldValidator {
    // the only non-static method in this interface
    ValidationResult check(JTextField textField);

    static ValidationResult hasPositiveDouble(JTextField textField, String label) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.error("<b>" + label + "</b> can't be empty.");
        }
        double value;
        try {
            value = Utils.parseDouble(text);
        } catch (NumberFormatException ex) {
            return ValidationResult.error("" + text + " is not a valid number for <b>" + label + "</b>");
        }
        if (value > 0) {
            return ValidationResult.ok();
        } else if (value == 0) {
            return ValidationResult.error("<b>" + label + "</b> can't be zero.");
        } else {
            return ValidationResult.error("<b>" + label + "</b> must be positive.");
        }
    }

    static ValidationResult hasPositiveInt(JTextField textField, String label, boolean allowZero) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.error("<b>" + label + "</b> can't be empty.");
        }
        try {
            int value = Integer.parseInt(text);
            if (value == 0 && !allowZero) {
                return ValidationResult.error("<b>" + label + "</b> can't be 0.");
            }
            if (value < 0) {
                return ValidationResult.error("<b>" + label + "</b> must be positive.");
            }
        } catch (NumberFormatException ex) {
            return ValidationResult.error("<b>" + label + "</b> must be an integer.");
        }
        return ValidationResult.ok();
    }

    static JLayer<JTextField> createPositiveIntLayer(String label,
                                                     JTextField tf,
                                                     boolean allowZero) {
        TFValidationLayerUI layerUI = TFValidationLayerUI.fromValidator(
            textField1 -> hasPositiveInt(textField1, label, allowZero));
        return new JLayer<>(tf, layerUI);
    }
}
