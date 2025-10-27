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
import pixelitor.tools.Tools;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Keys;
import pixelitor.utils.debug.Debug;

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
import static pixelitor.tools.Tools.activeTool;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * Manages global keyboard and mouse event handling.
 */
public class GlobalEvents {
    private static boolean spaceDown = false;

    // dialogs can be inside dialogs, and this keeps track of the nesting
    private static int modalDialogNesting = 0;

    private static final Action INCREASE_BRUSH_SIZE_ACTION =
        new TaskAction(Tools::increaseBrushSize);
    private static final Action DECREASE_BRUSH_SIZE_ACTION =
        new TaskAction(Tools::decreaseBrushSize);

    private static final Map<KeyStroke, Action> hotKeyMap = new HashMap<>();

    private GlobalEvents() {
        // prevents instantiation of this utility class
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
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(GlobalEvents::dispatchGlobalKeyEvent);
        return focusManager;
    }

    private static boolean dispatchGlobalKeyEvent(KeyEvent e) {
        if (modalDialogNesting > 0) {
            return false;
        }
        int id = e.getID();
        if (id == KEY_PRESSED) {
            // hotkeys should be inactive while editing text
            if (!(e.getSource() instanceof JTextField)) {
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                Action action = hotKeyMap.get(keyStroke);
                if (action != null) {
                    action.actionPerformed(null);
                    // hotkey was handled, so consume the event by returning true
                    return true;
                }
            }
            keyPressed(e);
        } else if (id == KEY_RELEASED) {
            keyReleased(e);
        }
        // let the event be processed by other dispatchers and the focused component
        return false;
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
            case VK_ESCAPE -> activeTool.escPressed();
            case VK_ALT -> activeTool.altPressed();
            default -> activeTool.otherKeyPressed(e);
        }
    }

    private static void spacePressed(KeyEvent e) {
        // Alt-space isn't treated as space-down because on Windows,
        // this opens the system menu, and we get the space-pressed
        // event, but not the space released-event, and the app gets
        // stuck in Hand mode. This looks like a freeze when there
        // are no scrollbars. See issue #29.
        if (modalDialogNesting == 0 && !e.isAltDown()) {
            activeTool.spacePressed();
            spaceDown = true;
            e.consume();
        }
    }

    private static void arrowKeyPressed(KeyEvent e, ArrowKey key) {
        if (activeTool.arrowKeyPressed(key)) {
            e.consume();
        }
    }

    private static void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case VK_SPACE -> spaceReleased();
            case VK_ALT -> activeTool.altReleased();
        }
    }

    private static void spaceReleased() {
        activeTool.spaceReleased();
        spaceDown = false;
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    // used only by unit tests
    public static void setSpaceDown(boolean spaceDown) {
        GlobalEvents.spaceDown = spaceDown;
    }

    // keeps track of modal dialog nesting
    public static void modalDialogOpened() {
        assert calledOnEDT() : callInfo();

        modalDialogNesting++;
        if (modalDialogNesting == 1) {
            Tools.modalDialogShown();
        }
    }

    // keeps track of modal dialog nesting
    public static void modalDialogClosed() {
        assert calledOnEDT() : callInfo();

        modalDialogNesting--;
        assert modalDialogNesting >= 0;
        if (modalDialogNesting == 0) {
            Tools.modalDialogHidden();
        }
    }

    public static void assertDialogNestingIs(int expectedCount) {
        if (modalDialogNesting != expectedCount) {
            throw new AssertionError("numNestedDialogs = " + modalDialogNesting
                + ", expectedCount = " + expectedCount);
        }
    }

    /**
     * Returns the number of currently open modal dialogs.
     */
    public static int getModalDialogNesting() {
        return modalDialogNesting;
    }

    public static void enableMouseEventDebugging(boolean postEvents) {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            MouseEvent e = (MouseEvent) event;
            String msg = Tools.getActive().getName() + ": " + Debug.mouseEventAsString(e);
            System.out.println(msg);
        }, AWTEvent.MOUSE_EVENT_MASK
            | AWTEvent.MOUSE_MOTION_EVENT_MASK
            | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
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
