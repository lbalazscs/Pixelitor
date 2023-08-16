/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.PAction;
import pixelitor.tools.Tools;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.KeyListener;
import pixelitor.utils.Keys;
import pixelitor.utils.debug.Debug;
import pixelitor.utils.test.Events;

import javax.swing.*;
import java.awt.*;
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
import static java.awt.event.KeyEvent.*;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * A global listener for AWT/Swing events
 */
public class GlobalEvents {
    private static boolean spaceDown = false;

    // Dialogs can be inside dialogs, and this keeps track of the nesting
    private static int numModalDialogs = 0;

    private static KeyListener keyListener;

    private static final Action INCREASE_ACTIVE_BRUSH_SIZE_ACTION =
        new PAction(Tools::increaseActiveBrushSize);
    private static final Action DECREASE_ACTIVE_BRUSH_SIZE_ACTION =
        new PAction(Tools::decreaseActiveBrushSize);

    private static final Map<KeyStroke, Action> hotKeyMap = new HashMap<>();

    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            KeyEvent keyEvent = (KeyEvent) event;
            if (keyEvent.getID() != KEY_PRESSED) {
                // we are only interested in key pressed events
                return;
            }
            if (keyEvent.getSource() instanceof JTextField) {
                // hotkeys should be inactive while editing text
                return;
            }
            if (numModalDialogs > 0) {
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
        // only static utility methods
    }

    public static void addHotKey(char key, Action action) {
        addHotKey(key, action, false);
    }

    private static void addHotKey(char key, Action action, boolean caseSensitive) {
        if (!caseSensitive) {
            assert Character.isUpperCase(key);

            // see issue #31 for why key codes and not key characters are used here
            hotKeyMap.put(KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK), action);
        }
        hotKeyMap.put(KeyStroke.getKeyStroke(key, 0), action);
    }

    public static void init() {
        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addKeyEventDispatcher(e -> {
            if (numModalDialogs > 0) {
                return false;
            }
            int id = e.getID();
            if (id == KEY_PRESSED) {
                keyPressed(e);
            } else if (id == KEY_RELEASED) {
                keyReleased(e);
            }
            return false;
        });

        // Remove Ctrl-Tab and Ctrl-Shift-Tab as focus traversal keys
        // so that they can be used to switch between tabs/internal frames.
        Set<AWTKeyStroke> forwardKeys = keyboardFocusManager
            .getDefaultFocusTraversalKeys(FORWARD_TRAVERSAL_KEYS);
        forwardKeys = new HashSet<>(forwardKeys); // make modifiable
        forwardKeys.remove(Keys.CTRL_TAB);
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
            case VK_SPACE -> spacePressed(e);
            case VK_RIGHT, VK_KP_RIGHT -> arrowKeyPressed(e, ArrowKey.right(e.isShiftDown()));
            case VK_LEFT, VK_KP_LEFT -> arrowKeyPressed(e, ArrowKey.left(e.isShiftDown()));
            case VK_UP, VK_KP_UP -> arrowKeyPressed(e, ArrowKey.up(e.isShiftDown()));
            case VK_DOWN, VK_KP_DOWN -> arrowKeyPressed(e, ArrowKey.down(e.isShiftDown()));
            case VK_ESCAPE -> escPressed();
            case VK_ALT -> altPressed();
            default -> keyListener.otherKeyPressed(e);
        }
    }

    private static void spacePressed(KeyEvent e) {
        // Alt-space is not treated as space-down because on Windows
        // this opens the system menu, and we get the space-pressed
        // event, but not the space released-event, and the app gets
        // stuck in Hand mode, which looks like a freeze when there
        // are no scrollbars. See issue #29
        if (numModalDialogs == 0 && !e.isAltDown()) {
            keyListener.spacePressed();
            spaceDown = true;
            e.consume();
        }
    }

    private static void arrowKeyPressed(KeyEvent e, ArrowKey key) {
        if (numModalDialogs == 0 && keyListener.arrowKeyPressed(key)) {
            e.consume();
        }
    }

    private static void escPressed() {
        if (numModalDialogs == 0) {
            keyListener.escPressed();
        }
    }

    private static void altPressed() {
        if (numModalDialogs == 0) {
            keyListener.altPressed();
        }
    }

    private static void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case VK_SPACE -> spaceReleased();
            case VK_ALT -> altReleased();
        }
    }

    private static void spaceReleased() {
        keyListener.spaceReleased();
        spaceDown = false;
    }

    private static void altReleased() {
        if (numModalDialogs == 0) {
            keyListener.altReleased();
        }
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    public static void setSpaceDown(boolean spaceDown) {
        GlobalEvents.spaceDown = spaceDown;
    }

    // keeps track of dialog nesting
    public static void dialogOpened(String title) {
        assert calledOnEDT() : threadInfo();

        numModalDialogs++;
        if (numModalDialogs == 1) {
            Tools.firstModalDialogShown();
        }

//        System.out.printf("dialog '%s' opened, numModalDialogs = %d%n", title, numModalDialogs);
    }

    // keeps track of dialog nesting
    public static void dialogClosed(String title) {
        assert calledOnEDT() : threadInfo();

        numModalDialogs--;
        assert numModalDialogs >= 0;
        if (numModalDialogs == 0) {
            Tools.firstModalDialogHidden();
        }

//        System.out.printf("dialog '%s' closed, numModalDialogs = %d%n", title, numModalDialogs);
    }

    public static void assertDialogNestingIs(int expected) {
        if (numModalDialogs != expected) {
            throw new AssertionError("numNestedDialogs = " + numModalDialogs
                + ", expected = " + expected);
        }
    }

    public static int getNumModalDialogs() {
        return numModalDialogs;
    }

    public static void addBrushSizeActions() {
        addHotKey(']', INCREASE_ACTIVE_BRUSH_SIZE_ACTION, true);
        addHotKey('[', DECREASE_ACTIVE_BRUSH_SIZE_ACTION, true);
    }

    public static void registerDebugMouseWatching(boolean postEvents) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            MouseEvent e = (MouseEvent) event;
            String msg = Tools.getCurrent().getName() + ": " + Debug.debugMouseEvent(e);
            if (postEvents) {
                Events.postMouseEvent(msg);
            } else {
                System.out.println(msg);
            }
        }, AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK
            | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
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
