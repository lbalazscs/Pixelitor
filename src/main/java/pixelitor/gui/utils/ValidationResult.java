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

import java.awt.Component;

import static java.lang.String.format;

/**
 * Represents the result of a validation operation as an immutable objct.
 * It supports chaining multiple validations and combining their results.
 */
public class ValidationResult {
    private final boolean isValid;
    private final String errorMsg;

    // The OK result has no error message, therefore it can be shared
    private static final ValidationResult VALID_RESULT
        = new ValidationResult(true, null);

    /**
     * Private constructor to enforce factory method usage.
     */
    private ValidationResult(boolean isValid, String errorMsg) {
        if (!isValid && errorMsg == null) {
            throw new IllegalArgumentException("missing error message");
        }

        this.isValid = isValid;
        this.errorMsg = errorMsg;
    }

    /**
     * Factory method for the OK result
     */
    public static ValidationResult valid() {
        return VALID_RESULT;
    }

    /**
     * Factory method for an error
     */
    public static ValidationResult invalid(String errorMsg) {
        return new ValidationResult(false, errorMsg);
    }

    public boolean isValid() {
        return isValid;
    }

    /**
     * Combines this validation result with another using logical AND.
     */
    public ValidationResult and(ValidationResult other) {
        if (isValid) {
            assert this == VALID_RESULT;
            assert !other.isValid() || other == VALID_RESULT;

            return other;
        } else {
            if (other.isValid()) {
                return this; // with our error message
            } else {
                return invalid(combineErrorMessages(errorMsg, other.errorMsg));
            }
        }
    }

    /**
     * Adds a new error message to this validation result.
     */
    public ValidationResult withError(String newErrorMsg) {
        if (isValid) {
            assert this == VALID_RESULT;
            return invalid(newErrorMsg);
        } else {
            return invalid(combineErrorMessages(this.errorMsg, newErrorMsg));
        }
    }

    /**
     * Conditionally adds an error message.
     */
    public ValidationResult withErrorIf(boolean condition, String newErrorMsg) {
        return condition ? withError(newErrorMsg) : this;
    }

    /**
     * Validates that a numeric value is not zero.
     */
    public ValidationResult validateNonZero(int value, String fieldName) {
        return withErrorIf(value == 0,
            format("<b>%s</b> cannot be zero", fieldName));
    }

    /**
     * Validates that a numeric value is positive.
     */
    public ValidationResult validatePositive(int value, String fieldName) {
        return withErrorIf(value < 0,
            format("<b>%s</b> must be positive", fieldName));
    }

    /**
     * Combines two error messages with HTML line break.
     */
    private static String combineErrorMessages(String first, String second) {
        return first + "<br>" + second;
    }

    public void showErrorDialog(Component dialogParent) {
        if (isValid) {
            throw new IllegalStateException("is valid");
        }
        assert !errorMsg.startsWith("<html>") : "errorMsg = " + errorMsg;
        Dialogs.showErrorDialog(dialogParent, "Error", "<html>" + errorMsg);
    }
}
