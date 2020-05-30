/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.OpenImages;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.GlobalEvents;
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
import static pixelitor.guitest.AppRunner.ROBOT_DELAY_DEFAULT;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * Keyboard input for AssertJ-Swing based tests
 */
public class Keyboard {
    // on some Linux environments it can happen that robot key events are
    // generated multiple times
    private static final boolean osLevelKeyEvents = !JVM.isLinux;

    private final FrameFixture pw;
    private final Robot robot;
    private final AppRunner runner;

    private boolean ctrlDown = false;
    private boolean altDown = false;
    private boolean shiftDown = false;

    public Keyboard(FrameFixture pw, Robot robot, AppRunner runner) {
        this.pw = pw;
        this.robot = robot;
        this.runner = runner;
    }

    void undo(String edit) {
        EDT.assertEditToBeUndoneNameIs(edit);
        if (osLevelKeyEvents) {
            // press Ctrl-Z
            pw.pressKey(VK_CONTROL).pressKey(VK_Z)
                .releaseKey(VK_Z).releaseKey(VK_CONTROL);

//        KeyPressInfo info = KeyPressInfo.keyCode(VK_Z).modifiers(ctrlOrCommand);
//        pw.pressAndReleaseKey(info);
        } else {
            Utils.sleep(ROBOT_DELAY_DEFAULT, MILLISECONDS);
            EDT.undo(edit);
        }

        robot.waitForIdle();
    }

    // undo without expected edit name for random tests
    void undo() {
        if (osLevelKeyEvents) {
            // press Ctrl-Z
            pw.pressKey(VK_CONTROL).pressKey(VK_Z)
                .releaseKey(VK_Z).releaseKey(VK_CONTROL);
        } else {
            EDT.undo();
        }
    }

    void redo(String edit) {
        EDT.assertEditToBeRedoneNameIs(edit);
        if (osLevelKeyEvents) {
            // press Ctrl-Shift-Z
            pw.pressKey(VK_CONTROL).pressKey(VK_SHIFT).pressKey(VK_Z)
                .releaseKey(VK_Z).releaseKey(VK_SHIFT).releaseKey(VK_CONTROL);

//        KeyPressInfo info = KeyPressInfo.keyCode(VK_Z).modifiers(VK_CONTROL, VK_SHIFT);
//        pw.pressAndReleaseKey(info);
        } else {
            Utils.sleep(ROBOT_DELAY_DEFAULT, MILLISECONDS);
            EDT.redo(edit);
        }

        robot.waitForIdle();
    }

    // redo without expected edit name for random tests
    void redo() {
        if (osLevelKeyEvents) {
            // press Ctrl-Shift-Z
            pw.pressKey(VK_CONTROL).pressKey(VK_SHIFT).pressKey(VK_Z)
                .releaseKey(VK_Z).releaseKey(VK_SHIFT).releaseKey(VK_CONTROL);
        } else {
            EDT.redo();
        }
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

    void invert() {
        if (osLevelKeyEvents) {
            // press Ctrl-I
            pw.pressKey(VK_CONTROL).pressKey(VK_I).releaseKey(VK_I).releaseKey(VK_CONTROL);
        } else {
            runner.runMenuCommand("Invert");
        }
    }

    void deselect() {
        if (osLevelKeyEvents) {
            // press Ctrl-D
            pw.pressKey(VK_CONTROL).pressKey(VK_D).releaseKey(VK_D).releaseKey(VK_CONTROL);
            robot.waitForIdle();
        } else {
            // runMenuCommand("Deselect");
            EDT.run(() -> OpenImages.getActiveComp().deselect(true));
        }
    }

    void fgBgDefaults() {
        if (osLevelKeyEvents) {
            // press D
            pw.pressKey(VK_D).releaseKey(VK_D);
        } else {
            EDT.run(FgBgColors::setDefaultColors);
        }
    }

    void randomizeColors() {
        if (osLevelKeyEvents) {
            pw.pressAndReleaseKeys(VK_R);
        } else {
            EDT.run(FgBgColors::randomizeColors);
        }
    }

    void actualPixels() {
        if (osLevelKeyEvents) {
            // press Ctrl-0
            pw.pressKey(VK_CONTROL).pressKey(VK_0).releaseKey(VK_0).releaseKey(VK_CONTROL);
        } else {
            runner.runMenuCommand("Actual Pixels");
        }
    }

    void nudge() {
        nudge(ArrowKey.SHIFT_RIGHT);
    }

    void nudge(ArrowKey key) {
        int keyCode = key.getKeyCode();

        if (osLevelKeyEvents) {
            if (key.isShiftDown()) {
                // TODO for some reason the shift is not detected
                pw.pressKey(VK_SHIFT).pressKey(keyCode)
                    .releaseKey(keyCode).releaseKey(VK_SHIFT);
            } else {
                pw.pressKey(VK_RIGHT).releaseKey(VK_RIGHT);
            }
        } else {
            if (key.isShiftDown()) {
                postKeyEventToEventQueue(SHIFT_DOWN_MASK, keyCode);
            } else {
                postKeyEventToEventQueue(0, keyCode);
            }
        }
    }

    static <S, C extends Window, D extends WindowDriver>
    void pressCtrlPlus(AbstractWindowFixture<S, C, D> window, int times) {
        for (int i = 0; i < times; i++) {
            if (osLevelKeyEvents) {
                window.pressKey(VK_CONTROL);
                window.pressKey(VK_ADD);
                window.releaseKey(VK_ADD);
                window.releaseKey(VK_CONTROL);
            } else {
                EDT.increaseZoom();
            }
        }
    }

    static <S, C extends Window, D extends WindowDriver>
    void pressCtrlMinus(AbstractWindowFixture<S, C, D> window, int times) {
        for (int i = 0; i < times; i++) {
            if (osLevelKeyEvents) {
                window.pressKey(VK_CONTROL);
                window.pressKey(VK_SUBTRACT);
                window.releaseKey(VK_SUBTRACT);
                window.releaseKey(VK_CONTROL);
            } else {
                EDT.decreaseZoom();
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
        if (osLevelKeyEvents) {
            pw.pressKey(keyCode).releaseKey(keyCode);
        } else {
            postKeyEventToEventQueue(0, keyCode);
        }
        Utils.sleep(100, MILLISECONDS);
    }

    void ctrlPress(int keyCode) {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_CONTROL).pressKey(keyCode)
                .releaseKey(keyCode).releaseKey(VK_CONTROL);
        } else {
            postKeyEventToEventQueue(CTRL_DOWN_MASK, keyCode);
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

    private void postKeyEventToEventQueue(int modifiers, int keyCode) {
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
        pw.pressKey(VK_CONTROL);
        ctrlDown = true;
    }

    public void releaseCtrl() {
        pw.releaseKey(VK_CONTROL);
        ctrlDown = false;
    }

    public void pressAlt() {
        pw.pressKey(VK_ALT);
        altDown = true;
    }

    public void releaseAlt() {
        pw.releaseKey(VK_ALT);
        altDown = false;
    }

    public void pressShift() {
        pw.pressKey(VK_SHIFT);
        shiftDown = true;
    }

    public void releaseShift() {
        pw.releaseKey(VK_SHIFT);
        shiftDown = false;
    }

    public void assertModifiersAreReleased() {
        assert !ctrlDown;
        assert !shiftDown;
        assert !altDown;
    }

    // make sure that the modifier keys don't remain
    // pressed after an exception or a forced pause/exit
    public void releaseModifierKeys() {
        assert calledOutsideEDT() : "on EDT";

        // TODO it can release them only on the main window
        if (EDT.call(() -> GlobalEvents.getNumNestedDialogs() == 0)) {
            releaseCtrl();
            releaseAlt();
            releaseShift();
        }
    }

    public void releaseModifierKeysFromAnyThread() {
        if (Threads.calledOnEDT()) {
            Thread thread = new Thread(this::releaseModifierKeys);
            thread.start();
        } else {
            releaseModifierKeys();
        }
    }
}
