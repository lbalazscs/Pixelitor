/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import java.io.File;
import java.text.ParseException;

/**
 * An object that checks the contents of a text field.
 */
public interface TextFieldValidator {
    // the only non-static method in this interface
    ValidationResult check(JTextField textField);

    /**
     * Checks if the text field contains a positive double value.
     */
    static ValidationResult requirePositiveDouble(JTextField textField, String label) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.invalidEmpty(label);
        }

        double value;
        try {
            value = Utils.parseLocalizedDouble(text);
        } catch (ParseException ex) {
            return ValidationResult.invalid("<b>" + label + "</b> must be a valid number.");
        }

        if (value > 0.0) {
            return ValidationResult.valid();
        }
        if (value == 0.0) {
            return ValidationResult.invalidZero(label);
        }
        // value must be negative
        return ValidationResult.invalidNegative(label);
    }

    /**
     * Checks if the text field contains a positive integer value (> 0).
     */
    static ValidationResult requirePositiveInt(JTextField textField, String label) {
        return checkInt(textField, label, false);
    }

    /**
     * Checks if the text field contains a non-negative integer value (>= 0).
     */
    static ValidationResult requireNonNegativeInt(JTextField textField, String label) {
        return checkInt(textField, label, true);
    }

    /**
     * Creates a JLayer that validates its text field for a positive integer value (> 0).
     */
    static JLayer<JTextField> createPositiveIntLayer(JTextField tf, String label) {
        return TFValidationLayerUI.wrapWithValidation(tf, textField -> requirePositiveInt(textField, label));
    }

    /**
     * Creates a JLayer that validates its text field for a non-negative integer value (>= 0).
     */
    static JLayer<JTextField> createNonNegativeIntLayer(JTextField tf, String label) {
        return TFValidationLayerUI.wrapWithValidation(tf, textField -> requireNonNegativeInt(textField, label));
    }

    private static ValidationResult checkInt(JTextField textField, String label, boolean allowZero) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.invalidEmpty(label);
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return ValidationResult.invalid("<b>" + label + "</b> must be an integer.");
        }

        if (value > 0) {
            return ValidationResult.valid();
        }
        if (value == 0) {
            return allowZero ? ValidationResult.valid() : ValidationResult.invalidZero(label);
        }
        // value must be negative
        return ValidationResult.invalidNegative(label);
    }

    /**
     * Checks if the text field contains a valid existing directory.
     */
    static ValidationResult requireExistingDir(JTextField textField, String label) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.invalidEmpty(label);
        }
        return ValidationResult.valid().requireExistingDir(new File(text), label);
    }

    /**
     * Creates a JLayer that validates its text field for an existing directory.
     */
    static JLayer<JTextField> createExistingDirLayer(String label, JTextField tf) {
        return TFValidationLayerUI.wrapWithValidation(tf, textField -> requireExistingDir(textField, label));
    }
}
