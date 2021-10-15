/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.colors.FgBgColors;
import pixelitor.layers.LayerButtonLayout;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Window;

public class Themes {
    private Themes() {
        // do not instantiate
    }

    public static final Color DISABLED_TEXT_COLOR = new ColorUIResource(133, 133, 133);

    // this theme will be used for the unit tests, otherwise it's overwritten at startup
    private static Theme currentTheme = Theme.NIMBUS;

    public static void install(Theme theme, boolean updateGUI, boolean force) {
        if (theme != currentTheme || force) {
            setLookAndFeel(theme.getLAFClassName());
            currentTheme = theme;
            if (updateGUI) {
                LayerButtonLayout.themeChanged(theme);
                FgBgColors.getGUI().themeChanged();
                updateAllUI();
            }
        }
    }

    public static void updateAllUI() {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    private static void setLookAndFeel(String lafClassName) {
        try {
            UIManager.setLookAndFeel(lafClassName);
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    public static Theme getCurrent() {
        return currentTheme;
    }
}