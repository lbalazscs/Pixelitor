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

package pixelitor.colors.palette;

import java.awt.Color;

/**
 * Tracks the most recently clicked {@link ColorSwatchButton} within
 * a single swatch container (a palette dialog or the color history
 * dialog). Each container owns its own instance, so selections in
 * different dialogs are independent.
 * <p>
 * The owner must call {@link #clear()} before it discards its swatches,
 * so that this object never retains a swatch that is no longer displayed.
 * Must only be used on the EDT.
 */
public final class SwatchSelection {
    private ColorSwatchButton selected;

    /**
     * Makes the given swatch the selected one
     * and repaints the previously selected one.
     */
    public void select(ColorSwatchButton swatch) {
        assert swatch != null;
        if (swatch == selected) {
            return;
        }
        ColorSwatchButton previous = selected;
        selected = swatch;
        if (previous != null) {
            previous.repaint();
        }
        swatch.repaint();
    }

    /**
     * Removes the selection and drops the reference to the swatch.
     */
    public void clear() {
        ColorSwatchButton previous = selected;
        selected = null;
        if (previous != null) {
            previous.repaint();
        }
    }

    public boolean isSelected(ColorSwatchButton swatch) {
        return swatch == selected;
    }

    /**
     * Returns the color of the currently selected swatch,
     * or {@code null} if there is no selection.
     */
    public Color getSelectedColor() {
        return selected == null ? null : selected.getColor();
    }
}
