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

package pixelitor.gui.utils;

import com.formdev.flatlaf.FlatLaf;
import pixelitor.tools.gui.ToolButton;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A static utility class for managing the application's Look and Feel (theme).
 */
public class Themes {
    private Themes() {
        // do not instantiate
    }

    private static final List<Consumer<Theme>> themeChangeListeners = new ArrayList<>();

    public static final Color LIGHT_ICON_COLOR = new ColorUIResource(187, 187, 187);

    public static final Theme DEFAULT = Theme.NIMBUS;

    // this theme will be used for the unit tests, otherwise it's overwritten at startup
    private static Theme activeTheme = DEFAULT;

    public static final AccentColor DEFAULT_ACCENT_COLOR = AccentColor.BLUE;
    private static AccentColor activeAccentColor = DEFAULT_ACCENT_COLOR;

    public static void addThemeChangeListener(Consumer<Theme> listener) {
        themeChangeListeners.add(listener);
    }

    public static void apply(Theme theme, boolean notifyComponents, boolean force) {
        if (theme != activeTheme || force) {
            applyLookAndFeel(theme);
            activeTheme = theme;

            if (notifyComponents) {
                // notify all registered listeners
                for (Consumer<Theme> listener : themeChangeListeners) {
                    listener.accept(theme);
                }

                refreshComponentUIs();
            }
        }
    }

    public static void changeAccentColor(AccentColor newColor) {
        if (!activeTheme.isFlat()) {
            throw new IllegalStateException("Active theme is " + activeTheme.getLAFClassName());
        }
        if (newColor == activeAccentColor) {
            return;
        }
        activeAccentColor = newColor;
        useAccentColor(newColor.asColor());
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", newColor.asHexCode()));
        apply(activeTheme, true, true);
    }

    public static void useAccentColor(Color color) {
        ToolButton.setDarkThemeSelectedColor(color);
    }

    /**
     * Updates all UI components to reflect theme changes.
     */
    public static void refreshComponentUIs() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    private static void applyLookAndFeel(Theme theme) {
        try {
            // has an effect only for the flat lafs
            UIManager.put("Component.focusWidth", 1);

            UIManager.setLookAndFeel(theme.getLAFClassName());
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    public static Theme getActive() {
        return activeTheme;
    }

    public static AccentColor getActiveAccentColor() {
        return activeAccentColor;
    }
}
