/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.AutoZoom;
import pixelitor.gui.ImageComponents;
import pixelitor.menus.PMenu;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static pixelitor.menus.MenuBar.MENU_SHORTCUT_KEY_MASK;

/**
 * The zoom menu
 */
public class ZoomMenu extends PMenu {
    private static final KeyStroke ACTUAL_PIXELS_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_0, MENU_SHORTCUT_KEY_MASK);
    public static final String ACTUAL_PIXELS_TOOLTIP = String.format("Set the zoom level to 100%% (%s)",
            Utils.keystrokeAsText(ACTUAL_PIXELS_KEY));

    private static final KeyStroke FIT_SCREEN_KEY = KeyStroke
            .getKeyStroke(KeyEvent.VK_0, MENU_SHORTCUT_KEY_MASK + InputEvent.ALT_MASK);
    public static final String FIT_SCREEN_TOOLTIP = String
            .format("Display the image at the largest zoom that can fit in the available space (%s)",
                    Utils.keystrokeAsText(FIT_SCREEN_KEY));

    private static final ButtonGroup radioGroup = new ButtonGroup();

    private static final String ACTION_MAP_KEY_INCREASE = "increase";
    private static final String ACTION_MAP_KEY_DECREASE = "decrease";
    private static final String ACTION_MAP_KEY_ACTUAL_PIXELS = "actual pixels";
    private static final String ACTION_MAP_KEY_FIT_SCREEN = "fit screen";

    private static final KeyStroke CTRL_PLUS = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_MINUS = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MENU_SHORTCUT_KEY_MASK);
    private static final KeyStroke CTRL_NUMPAD_PLUS = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke CTRL_NUMPAD_MINUS = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke CTRL_SHIFT_EQUALS = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_NUMPAD_0 = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke CTRL_ALT_NUMPAD_0 = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_MASK);

    private static final Action ZOOM_IN_ACTION = new AbstractAction("Zoom In") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.getActiveIC().increaseZoom();
        }
    };
    private static final Action ZOOM_OUT_ACTION = new AbstractAction("Zoom Out") {
        @Override
        public void actionPerformed(ActionEvent e) {
            ImageComponents.getActiveIC().decreaseZoom();
        }
    };

    // it is important to initialize this after other static fields
    public static final ZoomMenu INSTANCE = new ZoomMenu();

    private ZoomMenu() {
        super("Zoom");

        addActionWithKey(ZOOM_IN_ACTION, CTRL_PLUS);

        addActionWithKey(ZOOM_OUT_ACTION, CTRL_MINUS);

        addActionWithKey(AutoZoom.ACTUAL_PIXELS_ACTION, ACTUAL_PIXELS_KEY);

        addActionWithKey(AutoZoom.FIT_SCREEN_ACTION, FIT_SCREEN_KEY);

        addSeparator();
        setupZoomKeys(this);

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
        InputMap inputMap = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = c.getActionMap();
        inputMap.put(CTRL_SHIFT_EQUALS, ACTION_MAP_KEY_INCREASE);  // + key in English keyboards
        inputMap.put(CTRL_NUMPAD_PLUS, ACTION_MAP_KEY_INCREASE);  // + key on the numpad
        inputMap.put(CTRL_NUMPAD_MINUS, ACTION_MAP_KEY_DECREASE); // - key on the numpad
        actionMap.put(ACTION_MAP_KEY_INCREASE, ZOOM_IN_ACTION);
        actionMap.put(ACTION_MAP_KEY_DECREASE, ZOOM_OUT_ACTION);

        // ctrl + numpad 0 = actual pixels
        inputMap.put(CTRL_NUMPAD_0, ACTION_MAP_KEY_ACTUAL_PIXELS);
        actionMap.put(ACTION_MAP_KEY_ACTUAL_PIXELS, AutoZoom.ACTUAL_PIXELS_ACTION);

        // ctrl + alt + numpad 0 = fit screen
        inputMap.put(CTRL_ALT_NUMPAD_0, ACTION_MAP_KEY_FIT_SCREEN);
        actionMap.put(ACTION_MAP_KEY_FIT_SCREEN, AutoZoom.FIT_SCREEN_ACTION);
    }

    /**
     * Called when the active image has changed
     */
    public static void zoomChanged(ZoomLevel zoomLevel) {
        zoomLevel.getMenuItem().setSelected(true);
    }
}
