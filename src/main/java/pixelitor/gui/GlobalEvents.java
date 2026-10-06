/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.AWTKeyStroke;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.awt.KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS;
import static java.awt.KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS;
import static java.awt.event.KeyEvent.*;
import static pixelitor.tools.Tools.activeTool;
import static pixelitor.utils.Threads.callInfo;
import static pixelitor.utils.Threads.calledOnEDT;

/**
 * Central place for global keyboard handling: tracks the Space/Alt/Shift
 * state, runs hotkeys, forwards keys to the active tool, and tracks modal
 * dialog nesting. All state is accessed on the EDT only.
 */
public class GlobalEvents {
    private static boolean spaceDown = false;
    private static boolean altDown = false;
    private static boolean shiftDown = false;

    // keeps track of the nesting level since modal dialogs can open other modal dialogs
    private static int modalDialogNesting = 0;

    private static final Action INCREASE_BRUSH_RADIUS_ACTION =
        new TaskAction(Tools::increaseBrushRadius);
    private static final Action DECREASE_BRUSH_RADIUS_ACTION =
        new TaskAction(Tools::decreaseBrushRadius);

    private static final Map<KeyStroke, Action> hotkeyMap = new HashMap<>();

    private GlobalEvents() {
        // prevents instantiation of this utility class
    }

    /**
     * Registers a hotkey for a letter key, with and without Shift.
     */
    public static void registerHotkey(char key, Action action) {
        assert Character.isUpperCase(key) : "Expected an uppercase letter, got " + key;
        assert calledOnEDT() : callInfo(); // hotkeyMap is a plain HashMap read on the EDT
        putHotkey(key, 0, action);
        putHotkey(key, InputEvent.SHIFT_DOWN_MASK, action);
    }

    /**
     * Registers a hotkey that works only without modifiers (e.g. '[' and ']').
     */
    private static void registerPlainHotkey(char key, Action action) {
        putHotkey(key, 0, action);
    }

    private static void putHotkey(char key, int modifiers, Action action) {
        // deliberately a key code, not a key char (see issue #31)
        KeyStroke keyStroke = KeyStroke.getKeyStroke((int) key, modifiers);
        Action previous = hotkeyMap.put(keyStroke, action);
        assert previous == null : "duplicate hotkey " + keyStroke;
    }

    public static void init() {
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher(GlobalEvents::dispatchGlobalKeyEvent);

        // prevent stuck modifier keys when the application loses focus (e.g. alt-tabbing)
        kfm.addPropertyChangeListener("activeWindow", evt -> {
            if (evt.getNewValue() == null) { // the app lost focus
                releaseStuckModifiers();
            }
        });
        removeCtrlTabFromFocusTraversal(kfm);
        registerBrushSizeHotkeys();
    }

    // prevents stuck modifier keys when the app loses focus (e.g. alt-tabbing)
    private static void releaseStuckModifiers() {
        altReleased(); // still sees the last known Shift state
        spaceReleased();
        shiftDown = false; // a Shift release can't be observed while in the background
    }

    private static boolean dispatchGlobalKeyEvent(KeyEvent e) {
        int id = e.getID();

        // must be tracked even if a modal dialog is open, otherwise
        // a Shift press/release in the dialog would leave a stale state
        updateShiftState(e, id);

        if (id == KEY_PRESSED) {
            if (modalDialogNesting > 0) {
                return false;
            }
            // hotkeys should be inactive while editing text
            if (!(e.getSource() instanceof JTextComponent)) {
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                Action action = hotkeyMap.get(keyStroke);
                if (action != null) {
                    action.actionPerformed(null);
                    // hotkey was handled, so consume the event by returning true
                    return true;
                }
            }
            keyPressed(e);
        } else if (id == KEY_RELEASED) {
            // key releases must be tracked even if a modal dialog is open so
            // that Alt/Space released inside a dialog do not leave stale state
            keyReleased(e);
        }
        // let the event be processed by other dispatchers and the focused component
        return false;
    }

    private static void updateShiftState(KeyEvent e, int id) {
        if (e.getKeyCode() == VK_SHIFT) {
            if (id == KEY_PRESSED) {
                shiftDown = true;
            } else if (id == KEY_RELEASED) {
                shiftDown = false;
            }
        }
    }

    // remove Ctrl-Tab and Ctrl-Shift-Tab as focus traversal keys
    // so that they can be used to switch between tabs/internal frames
    private static void removeCtrlTabFromFocusTraversal(KeyboardFocusManager kfm) {
        removeFocusTraversalKey(kfm, FORWARD_TRAVERSAL_KEYS, Keys.CTRL_TAB);
        removeFocusTraversalKey(kfm, BACKWARD_TRAVERSAL_KEYS, Keys.CTRL_SHIFT_TAB);
    }

    private static void removeFocusTraversalKey(KeyboardFocusManager kfm, int traversalId, AWTKeyStroke key) {
        Set<AWTKeyStroke> keys = new HashSet<>(kfm.getDefaultFocusTraversalKeys(traversalId));
        keys.remove(key);
        kfm.setDefaultFocusTraversalKeys(traversalId, keys);
    }

    private static void registerBrushSizeHotkeys() {
        registerPlainHotkey(']', INCREASE_BRUSH_RADIUS_ACTION);
        registerPlainHotkey('[', DECREASE_BRUSH_RADIUS_ACTION);
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
            case VK_ALT -> altPressed();
            default -> activeTool.otherKeyPressed(e);
        }
    }

    private static void spacePressed(KeyEvent e) {
        assert modalDialogNesting == 0;

        // Alt-space isn't treated as space-down because on Windows,
        // this opens the system menu, and we get the space-pressed
        // event, but not the space-released event, and the app gets
        // stuck in Hand mode. This looks like a freeze when there
        // are no scrollbars. See issue #29.
        if (e.isAltDown()) {
            return;
        }
        if (!spaceDown) { // auto-repeat sends repeated pressed events
            spaceDown = true;
            activeTool.spacePressed();
        }
        e.consume();
    }

    private static void altPressed() {
        // tools should only receive a single pressed and a single released call
        if (!altDown) {
            altDown = true;
            activeTool.altPressed(shiftDown);
        }
    }

    private static void altReleased() {
        if (altDown) {
            altDown = false;
            activeTool.altReleased(shiftDown);
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
            case VK_ALT -> altReleased();
        }
    }

    private static void spaceReleased() {
        if (spaceDown) {
            spaceDown = false;
            activeTool.spaceReleased();
        }
    }

    public static boolean isSpaceDown() {
        return spaceDown;
    }

    public static boolean isAltDown() {
        return altDown;
    }

    public static boolean isShiftDown() {
        return shiftDown;
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
        assert modalDialogNesting > 0;

        modalDialogNesting--;
        if (modalDialogNesting == 0) {
            Tools.modalDialogHidden();
        }
    }

    public static void assertModalDialogNestingIs(int expectedCount) {
        if (modalDialogNesting != expectedCount) {
            throw new AssertionError("modalDialogNesting = " + modalDialogNesting
                + ", expectedCount = " + expectedCount);
        }
    }

    /**
     * Returns the number of currently open modal dialogs.
     */
    public static int getModalDialogNesting() {
        return modalDialogNesting;
    }
}
