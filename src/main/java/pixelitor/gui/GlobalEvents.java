/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.utils.TaskAction;
import pixelitor.tools.Tool;
import pixelitor.tools.Tools;
import pixelitor.tools.util.ArrowKey;
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
import static java.awt.event.KeyEvent.KEY_PRESSED;
import static java.awt.event.KeyEvent.KEY_RELEASED;
import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_KP_DOWN;
import static java.awt.event.KeyEvent.VK_KP_LEFT;
import static java.awt.event.KeyEvent.VK_KP_RIGHT;
import static java.awt.event.KeyEvent.VK_KP_UP;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * Manages global keyboard and mouse event handling.
 */
public class GlobalEvents {
    private static boolean spaceDown = false;

    // Dialogs can be inside dialogs, and this keeps track of the nesting
    private static int modalDialogCount = 0;

    private static Tool activeTool;

    private static final Action INCREASE_BRUSH_SIZE_ACTION =
        new TaskAction(Tools::increaseBrushSize);
    private static final Action DECREASE_BRUSH_SIZE_ACTION =
        new TaskAction(Tools::decreaseBrushSize);

    private static final Map<KeyStroke, Action> hotKeyMap = new HashMap<>();

    static {
        initGlobalKeyListener();
    }

    private static void initGlobalKeyListener() {
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
            if (modalDialogCount > 0) {
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
        // Private constructor to prevent instantiation of utility class
    }

    public static void registerHotkey(char key, Action action) {
        registerHotkey(key, action, false);
    }

    private static void registerHotkey(char key, Action action, boolean caseSensitive) {
        if (!caseSensitive) {
            assert Character.isUpperCase(key) : "Non-case-sensitive keys must be uppercase";

            // see issue #31 for why key codes and not key characters are used here
            hotKeyMap.put(KeyStroke.getKeyStroke(key, InputEvent.SHIFT_DOWN_MASK), action);
        }
        hotKeyMap.put(KeyStroke.getKeyStroke(key, 0), action);
    }

    public static void init() {
        var keyboardFocusManager = configureKeyboardManager();
        configureFocusTraversal(keyboardFocusManager);
        registerBrushSizeShortcuts();
    }

    private static KeyboardFocusManager configureKeyboardManager() {
        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addKeyEventDispatcher(e -> {
            if (modalDialogCount > 0) {
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
        return keyboardFocusManager;
    }

    private static void configureFocusTraversal(KeyboardFocusManager keyboardFocusManager) {
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

    private static void registerBrushSizeShortcuts() {
        registerHotkey(']', INCREASE_BRUSH_SIZE_ACTION, true);
        registerHotkey('[', DECREASE_BRUSH_SIZE_ACTION, true);
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
            default -> activeTool.otherKeyPressed(e);
        }
    }

    private static void spacePressed(KeyEvent e) {
        // Alt-space isn't treated as space-down because on Windows,
        // this opens the system menu, and we get the space-pressed
        // event, but not the space released-event, and the app gets
        // stuck in Hand mode. This looks like a freeze when there
        // are no scrollbars. See issue #29.
        if (modalDialogCount == 0 && !e.isAltDown()) {
            activeTool.spacePressed();
            spaceDown = true;
            e.consume();
        }
    }

    private static void arrowKeyPressed(KeyEvent e, ArrowKey key) {
        if (modalDialogCount == 0 && activeTool.arrowKeyPressed(key)) {
            e.consume();
        }
    }

    private static void escPressed() {
        if (modalDialogCount == 0) {
            activeTool.escPressed();
        }
    }

    private static void altPressed() {
        if (modalDialogCount == 0) {
            activeTool.altPressed();
        }
    }

    private static void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_SPACE -> spaceReleased();
            case VK_ALT -> altReleased();
        }
    }

    private static void spaceReleased() {
        activeTool.spaceReleased();
        spaceDown = false;
    }

    private static void altReleased() {
        if (modalDialogCount == 0) {
            activeTool.altReleased();
        }
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    public static void setSpaceDown(boolean spaceDown) {
        GlobalEvents.spaceDown = spaceDown;
    }

    // keeps track of dialog nesting
    public static void dialogOpened(String dialogTitle) {
        assert calledOnEDT() : threadInfo();

        modalDialogCount++;
        if (modalDialogCount == 1) {
            Tools.firstModalDialogShown();
        }
    }

    // keeps track of dialog nesting
    public static void dialogClosed(String dialogTitle) {
        assert calledOnEDT() : threadInfo();

        modalDialogCount--;
        assert modalDialogCount >= 0;
        if (modalDialogCount == 0) {
            Tools.firstModalDialogHidden();
        }
    }

    public static void assertDialogNestingIs(int expectedCount) {
        if (modalDialogCount != expectedCount) {
            throw new AssertionError("numNestedDialogs = " + modalDialogCount
                + ", expectedCount = " + expectedCount);
        }
    }

    public static int getModalDialogCount() {
        return modalDialogCount;
    }

    public static void enableMouseEventDebugging(boolean postEvents) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            MouseEvent e = (MouseEvent) event;
            String msg = Tools.getActive().getName() + ": " + Debug.mouseEventAsString(e);
            if (postEvents) {
                Events.postMouseEvent(msg);
            } else {
                System.out.println(msg);
            }
        }, AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK
            | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    }

    public static void setActiveTool(Tool activeTool) {
        GlobalEvents.activeTool = activeTool;
    }

    /**
     * Reports all events which take more than the given time to complete.
     *
     * See https://stackoverflow.com/questions/5541493/how-do-i-profile-the-edt-in-swing
     */
    public static void monitorSlowEvents(long threshold, TimeUnit unit) {
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
