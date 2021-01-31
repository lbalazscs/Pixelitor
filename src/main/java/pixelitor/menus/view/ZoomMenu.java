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

package pixelitor.menus.view;

import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.menus.PMenu;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static java.lang.String.format;
import static pixelitor.gui.AutoZoom.*;
import static pixelitor.utils.Keys.*;
import static pixelitor.utils.Texts.i18n;

/**
 * The zoom menu
 */
public class ZoomMenu extends PMenu {
    public static final String ACTUAL_PIXELS_TOOLTIP = format(
            "Set the zoom level to 100%% (%s)",
            Utils.keystrokeAsText(ACTUAL_PIXELS_KEY));

    public static final String FIT_SPACE_TOOLTIP = format(
            "Display the image at the largest zoom that can fit in the available space (%s)",
            Utils.keystrokeAsText(FIT_SPACE_KEY));

    private static final ButtonGroup radioGroup = new ButtonGroup();

    private static final String ACTION_MAP_KEY_INCREASE = "increase";
    private static final String ACTION_MAP_KEY_DECREASE = "decrease";
    private static final String ACTION_MAP_KEY_ACTUAL_PIXELS = "actual pixels";
    private static final String ACTION_MAP_KEY_FIT_SPACE = "fit space";

    private static final Action ZOOM_IN_ACTION = new AbstractAction(i18n("zoom_in")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            OpenImages.onActiveView(View::increaseZoom);
        }
    };
    private static final Action ZOOM_OUT_ACTION = new AbstractAction(i18n("zoom_out")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            OpenImages.onActiveView(View::decreaseZoom);
        }
    };

    // it is important to initialize this after other static fields
    public static final ZoomMenu INSTANCE = new ZoomMenu();

    private ZoomMenu() {
        super(i18n("zoom"));

        setupZoomKeys(this);

        addAction(ZOOM_IN_ACTION, CTRL_PLUS);
        addAction(ZOOM_OUT_ACTION, CTRL_MINUS);

        addAction(ACTUAL_PIXELS_ACTION, ACTUAL_PIXELS_KEY);
        addAction(FIT_SPACE_ACTION, FIT_SPACE_KEY);
        addAction(FIT_WIDTH_ACTION);
        addAction(FIT_HEIGHT_ACTION);

        addSeparator();

        ZoomLevel[] zoomLevels = ZoomLevel.values();
        for (ZoomLevel level : zoomLevels) {
            ZoomMenuItem menuItem = level.getMenuItem();
            if (level == ZoomLevel.Z100) {
                menuItem.setSelected(true);
            }
            add(menuItem);
            radioGroup.add(menuItem);
        }
    }

    public static void setupZoomKeys(JComponent c) {
        // add other key bindings - see http://stackoverflow.com/questions/15605109/java-keybinding-plus-key
        InputMap inputMap = c.getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = c.getActionMap();
        inputMap.put(CTRL_SHIFT_EQUALS, ACTION_MAP_KEY_INCREASE);  // + key in English keyboards
        inputMap.put(CTRL_NUMPAD_PLUS, ACTION_MAP_KEY_INCREASE);  // + key on the numpad
        inputMap.put(CTRL_NUMPAD_MINUS, ACTION_MAP_KEY_DECREASE); // - key on the numpad
        actionMap.put(ACTION_MAP_KEY_INCREASE, ZOOM_IN_ACTION);
        actionMap.put(ACTION_MAP_KEY_DECREASE, ZOOM_OUT_ACTION);

        // ctrl + numpad 0 = actual pixels
        inputMap.put(CTRL_NUMPAD_0, ACTION_MAP_KEY_ACTUAL_PIXELS);
        actionMap.put(ACTION_MAP_KEY_ACTUAL_PIXELS, ACTUAL_PIXELS_ACTION);

        // ctrl + alt + numpad 0 = fit screen
        inputMap.put(CTRL_ALT_NUMPAD_0, ACTION_MAP_KEY_FIT_SPACE);
        actionMap.put(ACTION_MAP_KEY_FIT_SPACE, FIT_SPACE_ACTION);
    }

    /**
     * Called when the active image has changed
     */
    public static void zoomChanged(ZoomLevel zoomLevel) {
        zoomLevel.getMenuItem().setSelected(true);
    }
}
