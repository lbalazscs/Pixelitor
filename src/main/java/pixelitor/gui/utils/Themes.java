/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.LayerGUILayout;
import pixelitor.layers.SelectionState;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Window;

public class Themes {
    private Themes() {
        // do not instantiate
    }

    public static final Color LIGHT_ICON_COLOR = new ColorUIResource(187, 187, 187);
//    public static final Color LIGHTER_ICON_COLOR = new ColorUIResource(217, 217, 217);

    // this theme will be used for the unit tests, otherwise it's overwritten at startup
    private static Theme currentTheme = Theme.NIMBUS;
    public static final Theme DEFAULT = Theme.NIMBUS;

    public static void install(Theme theme, boolean updateGUI, boolean force) {
        if (theme != currentTheme || force) {
            setLookAndFeel(theme);
            currentTheme = theme;
            if (updateGUI) {
                LayerGUILayout.themeChanged(theme);
                SelectionState.setupBorders(theme.isDark());
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

    private static void setLookAndFeel(Theme theme) {
        try {
            // has an effect only for the flat lafs
            UIManager.put("Component.focusWidth", 1);

//            UIManager.put("defaultFont", new Font("Comic Neue", Font.PLAIN, 30) );
            UIManager.setLookAndFeel(theme.getLAFClassName());
        } catch (Exception e) {
            Dialogs.showExceptionDialog(e);
        }
    }

    public static Theme getCurrent() {
        return currentTheme;
    }
}