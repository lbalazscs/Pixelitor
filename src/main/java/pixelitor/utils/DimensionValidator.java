/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.utils;

import pixelitor.Composition;
import pixelitor.gui.utils.DimensionHelper;
import pixelitor.gui.utils.TextFieldValidator;
import pixelitor.gui.utils.ValidationResult;

import javax.swing.*;
import java.text.ParseException;

/**
 * Validates text fields for positive numeric values based on the current unit.
 */
public class DimensionValidator implements TextFieldValidator {
    private final String label;
    private final int max;
    private final int originalSize;

    private ResizeUnit unit = ResizeUnit.PIXELS;
    private int dpi = Composition.DEFAULT_DPI;

    public DimensionValidator(String label, int max, int originalSize) {
        this.label = label;
        this.max = max;
        this.originalSize = originalSize;
    }

    public void updateContext(ResizeUnit unit, int dpi) {
        this.unit = unit;
        this.dpi = dpi;
    }

    @Override
    public ValidationResult check(JTextField textField) {
        String text = textField.getText().trim();
        if (text.isEmpty()) {
            return ValidationResult.invalidEmpty(label);
        }

        double value;
        try {
            value = unit.parse(text);
        } catch (ParseException | NumberFormatException ex) {
            if (unit == ResizeUnit.PIXELS) {
                return ValidationResult.invalid("<b>" + label + "</b> must be an integer.");
            } else {
                return ValidationResult.invalid("<b>" + label + "</b> must be a valid number.");
            }
        }

        if (value <= 0.0) {
            return value == 0.0 ? ValidationResult.invalidZero(label) : ValidationResult.invalidNegative(label);
        }

        // convert the input value to pixels to check against the maximum
        int pixels = unit.toPixels(value, originalSize, dpi);

        if (pixels > max) {
            return ValidationResult.invalid(
                String.format("<b>%s</b> results in a size of %d pixels, which is larger than the maximum of %d.",
                    label, pixels, max));
        }

        return ValidationResult.valid();
    }
}
