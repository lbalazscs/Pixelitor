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

import java.awt.Component;
import java.io.File;

import static java.lang.String.format;

/**
 * Represents the result of a validation as an immutable object.
 * It supports chaining multiple validations and combining their results.
 */
public class ValidationResult {
    private final boolean isValid;
    private final String errorMsg;

    // the OK result has no error message, therefore it can be shared
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
     * Factory method for the OK result.
     */
    public static ValidationResult valid() {
        return VALID_RESULT;
    }

    /**
     * Factory method for an error.
     */
    public static ValidationResult invalid(String errorMsg) {
        return new ValidationResult(false, errorMsg);
    }

    public static ValidationResult invalidZero(String name) {
        return invalid("<b>" + name + "</b> can't be zero.");
    }

    public static ValidationResult invalidNegative(String name) {
        return invalid("<b>" + name + "</b> must be positive.");
    }

    public static ValidationResult invalidEmpty(String name) {
        return invalid("<b>" + name + "</b> can't be empty.");
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

            // if this one is valid, the result is determined by the other one
            return other;
        }
        if (other.isValid) {
            return this;
        }
        // both are invalid, so combine the error messages
        return invalid(combineErrorMessages(errorMsg, other.errorMsg));
    }

    /**
     * Adds a new error message to this validation result.
     */
    public ValidationResult addError(String newErrorMsg) {
        if (isValid) {
            assert this == VALID_RESULT;
            return invalid(newErrorMsg);
        }
        return invalid(combineErrorMessages(this.errorMsg, newErrorMsg));
    }

    /**
     * Conditionally adds an error message.
     */
    public ValidationResult addErrorIf(boolean condition, String newErrorMsg) {
        return condition ? addError(newErrorMsg) : this;
    }

    /**
     * Validates that a numeric value is not negative (0 is allowed).
     */
    public ValidationResult requireNonNegative(int value, String fieldName) {
        return addErrorIf(value < 0,
            format("<b>%s</b> must not be negative", fieldName));
    }

    /**
     * Validates that a numeric value is positive (0 is not allowed).
     */
    public ValidationResult requirePositive(int value, String fieldName) {
        return addErrorIf(value <= 0,
            format("<b>%s</b> must be positive", fieldName));
    }

    /**
     * Validates that a numeric value is not larger than the given maximum value.
     */
    public ValidationResult requireMax(int value, String fieldName, int maxValue) {
        return addErrorIf(value > maxValue,
            format("<b>%s</b> must be smaller than %d", fieldName, maxValue));
    }

    /**
     * Validates that a string represents a positive integer.
     */
    public ValidationResult requirePositiveInt(String text, String fieldName) {
        try {
            int value = Integer.parseInt(text.trim());
            return this.requirePositive(value, fieldName);
        } catch (NumberFormatException e) {
            return this.addError(format("<b>%s</b> must be an integer.", fieldName));
        }
    }

    /**
     * Validates that a string represents a positive integer with a maximum value.
     */
    public ValidationResult requireBoundedPositiveInt(String text, String fieldName, int maxValue) {
        try {
            int value = Integer.parseInt(text.trim());
            return requirePositive(value, fieldName)
                .requireMax(value, fieldName, maxValue);
        } catch (NumberFormatException e) {
            return this.addError(format("<b>%s</b> must be an integer.", fieldName));
        }
    }

    /**
     * Validates a directory path that is allowed to be empty.
     * If the path is not empty, it must point to an existing directory.
     */
    public ValidationResult checkOptionalDir(String path, String description) {
        if (path == null || path.trim().isEmpty()) {
            return this;
        }
        return requireExistingDir(new File(path.trim()), description);
    }

    /**
     * Validates that a given File is a directory and it exists.
     */
    public ValidationResult requireExistingDir(File dir, String description) {
        if (!dir.exists()) {
            return addError(format("The selected %s folder <b>%s</b> doesn't exist.",
                description, dir.getAbsolutePath()));
        }

        if (!dir.isDirectory()) {
            return addError(format("The selected %s <b>%s</b> is not a folder.",
                description, dir.getAbsolutePath()));
        }

        return this;
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
