/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.tools;

import pixelitor.gui.ImageComponents;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static pixelitor.menus.MenuBar.MENU_SHORTCUT_KEY_MASK;

public class AutoZoomButtons {
    public static final KeyStroke ACTUAL_PIXELS_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_0, MENU_SHORTCUT_KEY_MASK);
    public static final KeyStroke FIT_SCREEN_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_0, MENU_SHORTCUT_KEY_MASK + InputEvent.ALT_MASK);

    public static final String FIT_SCREEN_TOOLTIP = String.format("Display the image at the largest zoom that can fit in the window (%s)",
            Utils.keystrokeAsText(FIT_SCREEN_KEY));
    public static final String ACTUAL_PIXELS_TOOLTIP = String.format("Set the zoom level to 100%% (%s)",
            Utils.keystrokeAsText(ACTUAL_PIXELS_KEY));

    private AutoZoomButtons() {
    }

    public static final Action FIT_SCREEN_ACTION = new AbstractAction("Fit Screen") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.fitActiveToScreen();
        }
    };

    public static final Action ACTUAL_PIXELS_ACTION = new AbstractAction("Actual Pixels") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.fitActiveToActualPixels();
        }
    };

    static {
        // setup tool tips
        FIT_SCREEN_ACTION.putValue(Action.SHORT_DESCRIPTION, FIT_SCREEN_TOOLTIP);
        ACTUAL_PIXELS_ACTION.putValue(Action.SHORT_DESCRIPTION, ACTUAL_PIXELS_TOOLTIP);
    }
}
