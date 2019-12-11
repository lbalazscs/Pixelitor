/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.tools.Tools;
import pixelitor.tools.gui.ToolButton;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.KeyListener;
import pixelitor.utils.Keys;
import pixelitor.utils.Utils;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.test.Events;

import javax.swing.*;
import java.awt.AWTEvent;
import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
import static java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;

/**
 * A global listener for AWT/Swing events
 */
public class GlobalEvents {
    private static boolean spaceDown = false;

    // dialogs can be inside dialogs, and this keeps track of the nesting
    private static int numNestedDialogs = 0;

    private static KeyListener keyListener;

    private static final Action INCREASE_ACTIVE_BRUSH_SIZE_ACTION = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Tools.increaseActiveBrushSize();
        }
    };

    private static final Action DECREASE_ACTIVE_BRUSH_SIZE_ACTION = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Tools.decreaseActiveBrushSize();
        }
    };

    private static final Map<KeyStroke, Action> hotKeyMap = new HashMap<>();

    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getID() != KeyEvent.KEY_PRESSED) {
                // we are only interested in key pressed events
                return;
            }
            if (keyEvent.getSource() instanceof JTextField) {
                // hotkeys should be inactive while editing text
                return;
            }

            KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(keyEvent);
            Action action = hotKeyMap.get(keyStroke);
            if (action != null) {
                action.actionPerformed(null);
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    private GlobalEvents() {
        // do not instantiate: only static utility methods
    }

    public static void addHotKey(char key, Action action) {
        addHotKey(key, action, false);
    }

    private static void addHotKey(char key, Action action, boolean caseSensitive) {
        if (caseSensitive) {
            hotKeyMap.put(KeyStroke.getKeyStroke(key, 0), action);
        } else {
            assert Character.isUpperCase(key);

            // see issue #31 for why key codes and not key characters are used here
            hotKeyMap.put(KeyStroke.getKeyStroke(key, InputEvent.SHIFT_MASK), action);
            hotKeyMap.put(KeyStroke.getKeyStroke(key, 0), action);
        }
    }

    public static void init() {
        // we want to use the tab key as "hide all", but
        // tab is the focus traversal key, it must be
        // handled before it gets consumed
        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addKeyEventDispatcher(e -> {
            int id = e.getID();
            if (id == KeyEvent.KEY_PRESSED) {
                keyPressed(e);
            } else if (id == KeyEvent.KEY_RELEASED) {
                keyReleased(e);
            }
            return false;
        });

        // Remove Ctrl-Tab and Ctrl-Shift-Tab as focus traversal keys
        // so that they can be used to switch between tabs/internal frames.
        // Also remove Tab so that is works as Show/Hide All
        Set<AWTKeyStroke> forwardKeys = keyboardFocusManager
                .getDefaultFocusTraversalKeys(FORWARD_TRAVERSAL_KEYS);
        forwardKeys = new HashSet<>(forwardKeys); // make modifiable
        forwardKeys.remove(Keys.CTRL_TAB);
        forwardKeys.remove(Keys.TAB);
        keyboardFocusManager.setDefaultFocusTraversalKeys(FORWARD_TRAVERSAL_KEYS, forwardKeys);

        Set<AWTKeyStroke> backwardKeys = keyboardFocusManager
                .getDefaultFocusTraversalKeys(BACKWARD_TRAVERSAL_KEYS);
        backwardKeys = new HashSet<>(backwardKeys); // make modifiable
        backwardKeys.remove(Keys.CTRL_SHIFT_TAB);
        keyboardFocusManager.setDefaultFocusTraversalKeys(BACKWARD_TRAVERSAL_KEYS, backwardKeys);
    }

    private static void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                // Alt-space is not treated as space-down because on Windows
                // this opens the system menu, and we get the space-pressed
                // event, but not the space released-event, and the app gets
                // stuck in Hand mode, which looks like a freeze when there
                // are no scrollbars. See issue #29
                if (numNestedDialogs == 0 && !e.isAltDown()) {
                    keyListener.spacePressed();
                    spaceDown = true;
                    e.consume();
                }

                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                // checking for VK_KP_RIGHT and other KP keys does not seem to be necessary
                // because at least on windows actually VK_RIGHT is sent by the keypad keys
                // but let's check them in order to be on the safe side
                if (numNestedDialogs == 0 && keyListener.arrowKeyPressed(ArrowKey.right(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                if (numNestedDialogs == 0 && keyListener.arrowKeyPressed(ArrowKey.left(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
                if (numNestedDialogs == 0 && keyListener.arrowKeyPressed(ArrowKey.up(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
                if (numNestedDialogs == 0 && keyListener.arrowKeyPressed(ArrowKey.down(e.isShiftDown()))) {
                    e.consume();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                if (numNestedDialogs == 0) {
                    keyListener.escPressed();
                }
                break;
            case KeyEvent.VK_ALT:
                if (numNestedDialogs == 0) {
                    keyListener.altPressed();
                }
                break;
            default:
                keyListener.otherKeyPressed(e);
        }
    }

    private static void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                keyListener.spaceReleased();
                spaceDown = false;
                break;
            case KeyEvent.VK_ALT:
                if (numNestedDialogs == 0) {
                    keyListener.altReleased();
                }
                break;
        }
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    @VisibleForTesting
    public static void setSpaceDown(boolean spaceDown) {
        GlobalEvents.spaceDown = spaceDown;
    }

    /**
     * The idea is that when we are in a dialog, we want to use the Tab
     * key for navigating the UI, and not for "Hide All".
     */
    public static void dialogOpened(String title) {
//        System.out.println("--- dialog opened: " + title
//                + ", nesting: " + numNestedDialogs
//                + " => " + (numNestedDialogs + 1));
        assert EventQueue.isDispatchThread();
        numNestedDialogs++;
        if(numNestedDialogs == 1) {
            Tools.firstModalDialogShown();
        }
    }

    public static void dialogClosed(String title) {
//        System.out.println("--- dialog closed: " + title
//                + ", nesting: " + numNestedDialogs
//                + " => " + (numNestedDialogs - 1));
        assert EventQueue.isDispatchThread();
        numNestedDialogs--;
        assert numNestedDialogs >= 0;
        if(numNestedDialogs == 0) {
            Tools.firstModalDialogHidden();
        }
    }

    @VisibleForTesting
    public static void assertDialogNestingIs(int expected) {
        if (numNestedDialogs != expected) {
            throw new AssertionError("numNestedDialogs = " + numNestedDialogs
                    + ", expected = " + expected);
        }
    }

    @VisibleForTesting
    public static int getNumNestedDialogs() {
        return numNestedDialogs;
    }

    public static void addBrushSizeActions() {
        addHotKey(']', INCREASE_ACTIVE_BRUSH_SIZE_ACTION, true);
        addHotKey('[', DECREASE_ACTIVE_BRUSH_SIZE_ACTION, true);
    }

    public static void registerDebugMouseWatching(boolean postEvents) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            MouseEvent e = (MouseEvent) event;
            String componentDescr = getComponentDescription(e);
            String msg = null;
            if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                msg = "CLICKED"
                        + " at (" + e.getX() + ", " + e.getY()
                        + "), click count = " + e.getClickCount()
                        + ", comp = " + componentDescr;
            } else if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                msg = "DRAGGED"
                        + " at (" + e.getX() + ", " + e.getY()
                        + "), comp = " + componentDescr;
            } else if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                msg = "PRESSED"
                        + " at (" + e.getX() + ", " + e.getY()
                        + "), comp = " + componentDescr;
            } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                msg = "RELEASED"
                        + " at (" + e.getX() + ", " + e.getY()
                        + "), comp = " + componentDescr;
            } else if (e.getID() == MouseEvent.MOUSE_WHEEL) {
                msg = "WHEEL"
                        + " at (" + e.getX() + ", " + e.getY()
                        + "), comp = " + componentDescr;
            }
            if (msg != null) {
                msg = Tools.getCurrent().getName() + " Tool: "
                        + Utils.debugMouseModifiers(e)
                        + msg;
                if (postEvents) {
                    Events.postMouseEvent(msg);
                } else {
                    System.out.println(msg);
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK
                | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    private static String getComponentDescription(MouseEvent e) {
        Component c = e.getComponent();
        String descr = c.getClass().getSimpleName();
        if (c instanceof View) {
            descr += "(name = " + c.getName() + ")";
        } else if (c instanceof ToolButton) {
            ToolButton b = (ToolButton) c;
            descr += "(name = " + b.getTool().getName() + ")";
        }

        return descr;
    }

    public static void setKeyListener(KeyListener keyListener) {
        GlobalEvents.keyListener = keyListener;
    }

    /**
     * Reports all events which take more than the given time to complete.
     *
     * See https://stackoverflow.com/questions/5541493/how-do-i-profile-the-edt-in-swing
     */
    public static void showEventsSlowerThan(long threshold, TimeUnit unit) {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
            final long thresholdNanos = unit.toNanos(threshold);

            @Override
            protected void dispatchEvent(AWTEvent event) {
                long startTime = System.nanoTime();
                super.dispatchEvent(event);
                long endTime = System.nanoTime();

                if (endTime - startTime > thresholdNanos) {
                    System.out.println(((endTime - startTime) / 1_000_000) + " ms: " + event);
                }
            }
        });
    }
}
