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

package pixelitor.menus.view;

import pixelitor.gui.utils.ViewEnabledAction;
import pixelitor.menus.PMenu;
import pixelitor.utils.Utils;

import javax.swing.*;

import static java.lang.String.format;
import static pixelitor.gui.AutoZoom.ACTUAL_PIXELS_ACTION;
import static pixelitor.gui.AutoZoom.FIT_HEIGHT_ACTION;
import static pixelitor.gui.AutoZoom.FIT_SPACE_ACTION;
import static pixelitor.gui.AutoZoom.FIT_WIDTH_ACTION;
import static pixelitor.utils.Keys.ACTUAL_PIXELS_KEY;
import static pixelitor.utils.Keys.CTRL_ALT_NUMPAD_0;
import static pixelitor.utils.Keys.CTRL_MINUS;
import static pixelitor.utils.Keys.CTRL_NUMPAD_0;
import static pixelitor.utils.Keys.CTRL_NUMPAD_MINUS;
import static pixelitor.utils.Keys.CTRL_NUMPAD_PLUS;
import static pixelitor.utils.Keys.CTRL_PLUS;
import static pixelitor.utils.Keys.CTRL_SHIFT_EQUALS;
import static pixelitor.utils.Keys.FIT_SPACE_KEY;
import static pixelitor.utils.Texts.i18n;

/**
 * The zoom menu
 */
public class ZoomMenu extends PMenu {
    public static final String ACTUAL_PIXELS_TOOLTIP = format(
        "Set the zoom level to 100%% (%s)",
        Utils.keystrokeToText(ACTUAL_PIXELS_KEY));

    public static final String FIT_SPACE_TOOLTIP = format(
        "Display the image at the largest zoom that can fit in the available space (%s)",
        Utils.keystrokeToText(FIT_SPACE_KEY));

    private static final String ACTION_MAP_KEY_INCREASE = "increase";
    private static final String ACTION_MAP_KEY_DECREASE = "decrease";
    private static final String ACTION_MAP_KEY_ACTUAL_PIXELS = "actual pixels";
    private static final String ACTION_MAP_KEY_FIT_SPACE = "fit space";

    private static final Action ZOOM_IN_ACTION = new ViewEnabledAction(
        i18n("zoom_in"),
        comp -> comp.getView().zoomIn());

    private static final Action ZOOM_OUT_ACTION = new ViewEnabledAction(
        i18n("zoom_out"),
        comp -> comp.getView().zoomOut());

    // it is important to initialize this after other static fields
    public static final ZoomMenu INSTANCE = new ZoomMenu();

    private ZoomMenu() {
        super(i18n("zoom"));

        setupZoomKeys(this);

        add(ZOOM_IN_ACTION, CTRL_PLUS);
        add(ZOOM_OUT_ACTION, CTRL_MINUS);

        add(ACTUAL_PIXELS_ACTION, ACTUAL_PIXELS_KEY);
        add(FIT_SPACE_ACTION, FIT_SPACE_KEY);
        add(FIT_WIDTH_ACTION);
        add(FIT_HEIGHT_ACTION);
    }

    public static void setupZoomKeys(JComponent c) {
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
}
