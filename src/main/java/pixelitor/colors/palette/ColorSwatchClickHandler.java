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
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static javax.swing.SwingUtilities.isRightMouseButton;
import static pixelitor.colors.FgBgColors.setBgColor;
import static pixelitor.colors.FgBgColors.setFgColor;

/**
 * Handles user clicks on a color swatch.
 */
@FunctionalInterface
public interface ColorSwatchClickHandler {
    /**
     * Processes a click on a color swatch.
     */
    void handle(Color swatchColor, MouseEvent e);

    /**
     * Returns the HTML help text describing what clicking a swatch does.
     */
    default String getHelpText() {
        // Ctrl-click is handled by the swatch itself and never reaches the handler
        return getClickHelp() + ", <b>Ctrl-click</b> to clear the marking.";
    }

    /**
     * Returns the HTML help text for the click action itself.
     */
    default String getClickHelp() {
        return "<b>click</b> to set the foreground color, "
            + "<b>right-click</b> to set the background color";
    }

    // sets the foreground color for left-clicks and the background color for right-clicks
    ColorSwatchClickHandler STANDARD = (swatchColor, e) -> {
        if (isRightMouseButton(e)) {
            setBgColor(swatchColor);
        } else {
            setFgColor(swatchColor);
        }
    };

    /**
     * Creates a click handler for filter swatches.
     */
    static ColorSwatchClickHandler forFilter(Consumer<Color> colorSetter) {
        return new ColorSwatchClickHandler() {
            @Override
            public void handle(Color swatchColor, MouseEvent e) {
                colorSetter.accept(swatchColor);
            }

            @Override
            public String getClickHelp() {
                return "<b>click</b> to set the filter color";
            }
        };
    }
}
