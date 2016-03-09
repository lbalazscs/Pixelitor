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

package pixelitor.gui;

import pixelitor.menus.view.ShowHideAllAction;
import pixelitor.tools.ArrowKey;
import pixelitor.tools.Tools;

import javax.swing.*;
import java.awt.AWTEvent;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * A global listener for keyboard events
 */
public class GlobalKeyboardWatch {
    private static boolean spaceDown = false;
    private static boolean dialogActive = false;
    private static JComponent alwaysVisibleComponent;

    private GlobalKeyboardWatch() {
    }

    public static void init() {
        // tab is the focus traversal key, it must be handled before it gets consumed
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            int id = e.getID();
            if (id == KeyEvent.KEY_PRESSED) {
                keyPressed(e);
            } else if (id == KeyEvent.KEY_RELEASED) {
                keyReleased(e);
            }
            return false;
        });
    }

    private static void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_TAB:
                if (!dialogActive) {
                    ShowHideAllAction.INSTANCE.actionPerformed(null);
                }
                break;
            case KeyEvent.VK_SPACE:
                if (!dialogActive) {
                    Tools.getCurrentTool().spacePressed();
                    spaceDown = true;
                    e.consume();
                }

                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                // checking for VK_KP_RIGHT and other KP keys does not seem to be necessary
                // because at least on windows actually VK_RIGHT is sent by the keypad keys
                // but let's check them in order to be on the safe side
                if (!dialogActive && Tools.getCurrentTool().arrowKeyPressed(new ArrowKey.RIGHT(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                if (!dialogActive && Tools.getCurrentTool().arrowKeyPressed(new ArrowKey.LEFT(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                if (!dialogActive && Tools.getCurrentTool().arrowKeyPressed(new ArrowKey.UP(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                if (!dialogActive && Tools.getCurrentTool().arrowKeyPressed(new ArrowKey.DOWN(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                if (!dialogActive) {
                    Tools.getCurrentTool().escPressed();
                }
                break;
            case KeyEvent.VK_ALT:
                if (!dialogActive) {
                    Tools.getCurrentTool().altPressed();
                }
                break;
        }
    }

    private static void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                Tools.getCurrentTool().spaceReleased();
                spaceDown = false;
                break;
            case KeyEvent.VK_ALT:
                if (!dialogActive) {
                    Tools.getCurrentTool().altReleased();
                }
                break;
        }
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    /**
     * The idea is that when we are in a dialog, we want to use the Tab
     * key for navigating the UI, and not for "Hide All"
     */
    public static void setDialogActive(boolean dialogActive) {
        GlobalKeyboardWatch.dialogActive = dialogActive;
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

    public static void registerDebugMouseWatching() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            MouseEvent m = (MouseEvent) event;
            String compClass = m.getComponent().getClass().getName();
            if (m.getID() == MouseEvent.MOUSE_CLICKED) {
                System.out.println("GlobalKeyboardWatch:MOUSE_CLICKED x = " + m.getX() + ", y = " + m.getY() + ", click count = " + m.getClickCount() + ", comp class = " + compClass);
            } else if (m.getID() == MouseEvent.MOUSE_DRAGGED) {
                System.out.println("GlobalKeyboardWatch:MOUSE_DRAGGED x = " + m.getX() + ", y = " + m.getY() + ", comp class = " + compClass);
            } else if (m.getID() == MouseEvent.MOUSE_PRESSED) {
                System.out.println("GlobalKeyboardWatch:MOUSE_PRESSED x = " + m.getX() + ", y = " + m.getY() + ", comp class = " + compClass);
            } else if (m.getID() == MouseEvent.MOUSE_RELEASED) {
                System.out.println("GlobalKeyboardWatch:MOUSE_RELEASED x = " + m.getX() + ", y = " + m.getY() + ", comp class = " + compClass);
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    // TODO this kind of global listening might be better
//    public static void registerMouseWheelWatching() {
//        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
//            @Override
//            public void eventDispatched(AWTEvent e) {
//            }
//        }, AWTEvent.MOUSE_WHEEL_EVENT_MASK);
//    }
}
