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

package pixelitor.guitest;

import com.bric.util.JVM;
import org.assertj.swing.core.Robot;
import org.assertj.swing.driver.WindowDriver;
import org.assertj.swing.fixture.AbstractWindowFixture;
import org.assertj.swing.fixture.FrameFixture;
import pixelitor.Views;
import pixelitor.colors.FgBgColors;
import pixelitor.history.HistoryChecker;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Threads;
import pixelitor.utils.Utils;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pixelitor.guitest.AppRunner.DEFAULT_ROBOT_DELAY;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * Keyboard input for AssertJ-Swing based tests
 */
public class Keyboard {
    // on some Linux environments robot key events could be
    // generated multiple times
    private static final boolean USE_OS_LEVEL_EVENTS = !JVM.isLinux;

    private final FrameFixture pw;
    private final Robot robot;
    private final AppRunner app;
    private final HistoryChecker historyChecker;

    private boolean ctrlDown = false;
    private boolean altDown = false;
    private boolean shiftDown = false;

    public Keyboard(FrameFixture pw, Robot robot, AppRunner app, HistoryChecker historyChecker) {
        this.pw = pw;
        this.robot = robot;
        this.app = app;
        this.historyChecker = historyChecker;
    }

    void undo(String editName) {
        if (historyChecker != null) {
            historyChecker.registerUndo(editName);
        }
        EDT.assertEditToBeUndoneNameIs(editName);

        if (USE_OS_LEVEL_EVENTS) {
            pressKeys(VK_CONTROL, VK_Z); // press Ctrl-Z
        } else {
            Utils.sleep(DEFAULT_ROBOT_DELAY, MILLISECONDS);
            EDT.undo(editName);
        }

        robot.waitForIdle();
    }

    void redo(String editName) {
        if (historyChecker != null) {
            historyChecker.registerRedo(editName);
        }
        EDT.assertEditToBeRedoneNameIs(editName);
        if (USE_OS_LEVEL_EVENTS) {
            pressKeys(VK_CONTROL, VK_SHIFT, VK_Z); // press Ctrl-Shift-Z
        } else {
            Utils.sleep(DEFAULT_ROBOT_DELAY, MILLISECONDS);
            EDT.redo(editName);
        }

        robot.waitForIdle();
    }

    void undoRedo(String edit) {
        undo(edit);
        redo(edit);
    }

    void undoRedoUndo(String edit) {
        undo(edit);
        redo(edit);
        undo(edit);
    }

    void undoRedo(String firstEdit, String secondEdit) {
        undo(secondEdit);
        undo(firstEdit);
        redo(firstEdit);
        redo(secondEdit);
    }

    void invert() {
        if (USE_OS_LEVEL_EVENTS) {
            // press Ctrl-I
            pressKeys(VK_CONTROL, VK_I);
        } else {
            app.runMenuCommand("Invert");
        }
    }

    void deselect() {
        if (USE_OS_LEVEL_EVENTS) {
            // press Ctrl-D
            pressKeys(VK_CONTROL, VK_D);
            robot.waitForIdle();
        } else {
            // runMenuCommand("Deselect");
            EDT.run(() -> Views.getActiveComp().deselect(true));
        }
    }

    void fgBgDefaults() {
        if (USE_OS_LEVEL_EVENTS) {
            // press D
            pressKeys(VK_D);
        } else {
            EDT.run(FgBgColors::setDefaultColors);
        }
    }

    void randomizeColors() {
        if (USE_OS_LEVEL_EVENTS) {
            pressKeys(VK_R);
        } else {
            EDT.run(FgBgColors::randomizeColors);
        }
    }

    void actualPixels() {
        if (USE_OS_LEVEL_EVENTS) {
            // press Ctrl-0
            pressKeys(VK_CONTROL, VK_0);
        } else {
            app.runMenuCommand("Actual Pixels");
        }
    }

    void nudge() {
        nudge(ArrowKey.SHIFT_RIGHT);
    }

    void nudge(ArrowKey key) {
        int keyCode = key.getKeyCode();

        if (USE_OS_LEVEL_EVENTS) {
            if (key.isShiftDown()) {
                // TODO for some reason the shift is not detected
                pressKeys(VK_SHIFT, keyCode);
            } else {
                pressKeys(keyCode);
            }
        } else {
            if (key.isShiftDown()) {
                postKeyToEventQueue(SHIFT_DOWN_MASK, keyCode);
            } else {
                postKeyToEventQueue(0, keyCode);
            }
        }
    }

    // can be sent to a dialog or to the main frame
    static <S, C extends Window, D extends WindowDriver>
    void pressCtrlPlus(AbstractWindowFixture<S, C, D> window, int times) {
        for (int i = 0; i < times; i++) {
            if (USE_OS_LEVEL_EVENTS) {
                window.pressKey(VK_CONTROL);
                window.pressKey(VK_ADD);
                window.releaseKey(VK_ADD);
                window.releaseKey(VK_CONTROL);
            } else {
                EDT.zoomIn();
            }
        }
    }

    // can be sent to a dialog or to the main frame
    static <S, C extends Window, D extends WindowDriver>
    void pressCtrlMinus(AbstractWindowFixture<S, C, D> window, int times) {
        for (int i = 0; i < times; i++) {
            if (USE_OS_LEVEL_EVENTS) {
                window.pressKey(VK_CONTROL);
                window.pressKey(VK_SUBTRACT);
                window.releaseKey(VK_SUBTRACT);
                window.releaseKey(VK_CONTROL);
            } else {
                EDT.zoomOut();
            }
        }
    }

    void pressEnter() {
        press(VK_ENTER);
    }

    void pressEsc() {
        press(VK_ESCAPE);
    }

    public void pressChar(char c) {
        press(c);
    }

    void press(int keyCode) {
        if (USE_OS_LEVEL_EVENTS) {
            pw.pressKey(keyCode);
            pw.releaseKey(keyCode);
        } else {
            postKeyToEventQueue(0, keyCode);
        }
        Utils.sleep(100, MILLISECONDS);
    }

    void ctrlPress(int keyCode) {
        if (USE_OS_LEVEL_EVENTS) {
            pressKeys(VK_CONTROL, keyCode);
        } else {
            postKeyToEventQueue(CTRL_DOWN_MASK, keyCode);
        }
        Utils.sleep(100, MILLISECONDS);
    }

    void ctrlAltPress(int keyCode) {
        if (USE_OS_LEVEL_EVENTS) {
            pressKeys(VK_CONTROL, VK_ALT, keyCode);
        } else {
            postKeyToEventQueue(CTRL_DOWN_MASK | ALT_DOWN_MASK, keyCode);
        }
        Utils.sleep(100, MILLISECONDS);
    }

    public void pressCtrlOne() {
        ctrlPress(VK_1);
    }

    public void pressCtrlTwo() {
        ctrlPress(VK_2);
    }

    public void pressCtrlThree() {
        ctrlPress(VK_3);
    }

    public void pressCtrlFour() {
        ctrlPress(VK_4);
    }

    public void pressTab() {
        press('\t');
    }

    public void pressCtrlTab() {
        ctrlPress('\t');
    }

    private void postKeyToEventQueue(int modifiers, int keyCode) {
        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        Frame eventSource = pw.target();
        queue.postEvent(new KeyEvent(eventSource, KEY_PRESSED,
            System.currentTimeMillis(), modifiers,
            keyCode, Character.MIN_VALUE));
        queue.postEvent(new KeyEvent(eventSource, KEY_RELEASED,
            System.currentTimeMillis(), modifiers,
            keyCode, Character.MIN_VALUE));
    }

    public void pressCtrl() {
        robot.pressKey(VK_CONTROL);
        ctrlDown = true;
    }

    public void releaseCtrl() {
        robot.releaseKey(VK_CONTROL);
        ctrlDown = false;
    }

    public void pressAlt() {
        robot.pressKey(VK_ALT);
        altDown = true;
    }

    public void releaseAlt() {
        robot.releaseKey(VK_ALT);
        altDown = false;
    }

    public void pressShift() {
        robot.pressKey(VK_SHIFT);
        shiftDown = true;
    }

    public void releaseShift() {
        robot.releaseKey(VK_SHIFT);
        shiftDown = false;
    }

    public void assertModifiersReleased() {
        assert !ctrlDown : "Control key is still pressed";
        assert !shiftDown : "Shift key is still pressed";
        assert !altDown : "Alt key is still pressed";
    }

    // make sure that the modifier keys don't remain
    // pressed after an exception or a forced pause/exit
    public void releaseModifierKeys() {
        assert calledOutsideEDT() : "on EDT";

        releaseCtrl();
        releaseAlt();
        releaseShift();
    }

    public void releaseModifierKeysFromAnyThread() {
        if (Threads.calledOnEDT()) {
            Thread thread = new Thread(this::releaseModifierKeys);
            thread.start();
        } else {
            releaseModifierKeys();
        }
    }

    /**
     * Simulates a keyboard shortcut with multiple modifier keys.
     */
    private void pressKeys(int... keys) {
        for (int key : keys) {
            pw.pressKey(key);
        }

        for (int i = keys.length - 1; i >= 0; i--) {
            pw.releaseKey(keys[i]);
        }
    }
}
