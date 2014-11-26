/*
 * Copyright 2010-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor;

import pixelitor.menus.view.ShowHideAllAction;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 */
public class GlobalKeyboardWatch {
    private static boolean spaceDown = false;
    private static boolean showHideAllForTab = true;
    private static JComponent alwaysVisibleComponent;

    private GlobalKeyboardWatch() {
    }

    public static void init() {
        // tab is the focus traversal key, it must be handled before it gets consumed
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                int id = e.getID();
                if (id == KeyEvent.KEY_PRESSED) {
                    int keyCode = e.getKeyCode();
                    if (showHideAllForTab && keyCode == KeyEvent.VK_TAB) {
                        ShowHideAllAction.INSTANCE.actionPerformed(null);
                    } else if (keyCode == KeyEvent.VK_SPACE) {
                        Tools.getCurrentTool().spacePressed();
                        spaceDown = true;
                    }
                } else if (id == KeyEvent.KEY_RELEASED) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == KeyEvent.VK_SPACE) {
                        Tools.getCurrentTool().spaceReleased();
                        spaceDown = false;
                    }
                }
                return false;
            }
        });
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    public static void setShowHideAllForTab(boolean showHideAllForTab) {
        GlobalKeyboardWatch.showHideAllForTab = showHideAllForTab;
    }

    public static void setAlwaysVisibleComponent(JComponent alwaysVisibleComponent) {
        GlobalKeyboardWatch.alwaysVisibleComponent = alwaysVisibleComponent;
    }

    public static void addKeyboardShortCut(char activationChar, boolean caseInsensitive, String actionMapKey, Action action) {
        InputMap inputMap = alwaysVisibleComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        if (caseInsensitive) {
            char activationLC = Character.toLowerCase(activationChar);
            char activationUC = Character.toUpperCase(activationChar);

            inputMap.put(KeyStroke.getKeyStroke(activationLC), actionMapKey);
            inputMap.put(KeyStroke.getKeyStroke(activationUC), actionMapKey);
        } else {
            inputMap.put(KeyStroke.getKeyStroke(activationChar), actionMapKey);
        }

        alwaysVisibleComponent.getActionMap().put(actionMapKey, action);
    }

    public static void addKeyboardShortCut(KeyStroke keyStroke, String actionMapKey, Action action) {
        InputMap inputMap = alwaysVisibleComponent.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(keyStroke, actionMapKey);
        alwaysVisibleComponent.getActionMap().put(actionMapKey, action);
    }

    public static void registerBrushSizeActions() {
        Action increaseActiveBrushSizeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Tools.increaseActiveBrushSize();
            }
        };


        Action decreaseActiveBrushSizeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Tools.decreaseActiveBrushSize();
            }
        };

        GlobalKeyboardWatch.addKeyboardShortCut(']', false, "increment", increaseActiveBrushSizeAction);
        GlobalKeyboardWatch.addKeyboardShortCut('[', false, "decrement", decreaseActiveBrushSizeAction);
    }
}
