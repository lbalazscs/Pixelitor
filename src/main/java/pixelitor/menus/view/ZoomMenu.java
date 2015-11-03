/*
 * Copyright (c) 2015 Laszlo Balazs-Csiki
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

import pixelitor.ImageComponents;
import pixelitor.menus.PMenu;
import pixelitor.tools.AutoZoomButtons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * The zoom menu
 */
public class ZoomMenu extends PMenu {
    private static final ButtonGroup radioGroup = new ButtonGroup();

    private static final String ACTION_MAP_KEY_INCREASE = "increase";
    private static final String ACTION_MAP_KEY_DECREASE = "decrease";
    private static final String ACTION_MAP_KEY_ACTUAL_PIXELS = "actual pixels";
    private static final String ACTION_MAP_KEY_FIT_SCREEN = "fit screen";

    private static final KeyStroke CTRL_PLUS = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_MINUS = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_0 = KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK);
    private static final KeyStroke CTRL_NUMPAD_PLUS = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke CTRL_NUMPAD_MINUS = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke CTRL_ALT_0 = KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_MASK + InputEvent.ALT_MASK);
    private static final KeyStroke CTRL_SHIFT_EQUALS = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK + InputEvent.SHIFT_MASK);
    private static final KeyStroke CTRL_NUMPAD_0 = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK);
    private static final KeyStroke CTRL_ALT_NUMPAD_0 = KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_MASK);

    // it is important to initialize this after the keystroke initializations!
    public static final ZoomMenu INSTANCE = new ZoomMenu();

    private ZoomMenu() {
        super("Zoom");

        Action increaseAction = new AbstractAction("Zoom In") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.increaseZoomForActiveIC();
            }
        };
        addActionWithKey(increaseAction, CTRL_PLUS);

        Action decreaseAction = new AbstractAction("Zoom Out") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ImageComponents.decreaseZoomForActiveIC();
            }
        };
        addActionWithKey(decreaseAction, CTRL_MINUS);

        addActionWithKey(AutoZoomButtons.ACTUAL_PIXELS_ACTION, CTRL_0);

        addActionWithKey(AutoZoomButtons.FIT_SCREEN_ACTION, CTRL_ALT_0);

        addSeparator();

        // add other key bindings - see http://stackoverflow.com/questions/15605109/java-keybinding-plus-key
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();
        inputMap.put(CTRL_SHIFT_EQUALS, ACTION_MAP_KEY_INCREASE);  // + key in English keyboards
        inputMap.put(CTRL_NUMPAD_PLUS, ACTION_MAP_KEY_INCREASE);  // + key on the numpad
        inputMap.put(CTRL_NUMPAD_MINUS, ACTION_MAP_KEY_DECREASE); // - key on the numpad
        actionMap.put(ACTION_MAP_KEY_INCREASE, increaseAction);
        actionMap.put(ACTION_MAP_KEY_DECREASE, decreaseAction);

        // ctrl + numpad 0 = actual pixels
        inputMap.put(CTRL_NUMPAD_0, ACTION_MAP_KEY_ACTUAL_PIXELS);
        actionMap.put(ACTION_MAP_KEY_ACTUAL_PIXELS, AutoZoomButtons.ACTUAL_PIXELS_ACTION);

        // ctrl + alt + numpad 0 = fit screen
        inputMap.put(CTRL_ALT_NUMPAD_0, ACTION_MAP_KEY_FIT_SCREEN);
        actionMap.put(ACTION_MAP_KEY_FIT_SCREEN, AutoZoomButtons.FIT_SCREEN_ACTION);

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

    /**
     * Called when the active image has changed
     */
    public static void zoomChanged(ZoomLevel zoomLevel) {
        zoomLevel.getMenuItem().setSelected(true);
    }
}
