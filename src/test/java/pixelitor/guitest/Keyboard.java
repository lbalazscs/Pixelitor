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

package pixelitor.guitest;

import com.bric.util.JVM;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.AbstractWindowFixture;
import org.assertj.swing.fixture.FrameFixture;
import pixelitor.colors.FgBgColors;
import pixelitor.gui.OpenComps;
import pixelitor.tools.util.ArrowKey;
import pixelitor.utils.Utils;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pixelitor.guitest.AppRunner.ROBOT_DELAY_DEFAULT;

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

    public Keyboard(FrameFixture pw, Robot robot, AppRunner runner) {
        this.pw = pw;
        this.robot = robot;
        this.runner = runner;
    }

    void undo(String edit) {
//        Utils.debugCall(edit);

        if (osLevelKeyEvents) {
            EDT.assertEditToBeUndoneNameIs(edit);

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
//        Utils.debugCall(edit);

        if (osLevelKeyEvents) {
            EDT.assertEditToBeRedoneNameIs(edit);

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
            GuiActionRunner.execute(() ->
                OpenComps.getActiveCompOrNull().deselect(true));
        }
    }

    void fgBgDefaults() {
        if (osLevelKeyEvents) {
            // press D
            pw.pressKey(VK_D).releaseKey(VK_D);
        } else {
            GuiActionRunner.execute(FgBgColors::setDefaultColors);
        }
    }

    void randomizeColors() {
        if (osLevelKeyEvents) {
            pw.pressAndReleaseKeys(KeyEvent.VK_R);
        } else {
            GuiActionRunner.execute(FgBgColors::randomize);
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
        boolean shiftDown = key.isShiftDown();

        if (osLevelKeyEvents) {
            if (shiftDown) {
                // TODO for some reason the shift is not detected
                pw.pressKey(VK_SHIFT).pressKey(keyCode)
                        .releaseKey(VK_RIGHT).releaseKey(keyCode);

            } else {
                pw.pressKey(VK_RIGHT).releaseKey(VK_RIGHT);
            }
        } else {
            if (shiftDown) {
                postKeyEventToEventQueue(KeyEvent.SHIFT_MASK, keyCode);
            } else {
                postKeyEventToEventQueue(0, keyCode);
            }
        }
    }

    static void pressCtrlPlus(AbstractWindowFixture window, int times) {
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

    static void pressCtrlMinus(AbstractWindowFixture window, int times) {
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
        if (osLevelKeyEvents) {
            pw.pressKey(VK_ENTER).releaseKey(VK_ENTER);
        } else {
            postKeyEventToEventQueue(0, VK_ENTER);
        }
    }

    void pressEsc() {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_ESCAPE).releaseKey(VK_ESCAPE);
        } else {
            postKeyEventToEventQueue(0, VK_ESCAPE);
        }
    }

    public void pressCtrlOne() {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_CONTROL).pressKey(VK_1)
                    .releaseKey(VK_1).releaseKey(VK_CONTROL);
        } else {
            postKeyEventToEventQueue(KeyEvent.CTRL_MASK, VK_1);
        }
    }

    public void pressCtrlTwo() {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_CONTROL).pressKey(VK_2)
                    .releaseKey(KeyEvent.VK_2).releaseKey(VK_CONTROL);
        } else {
            postKeyEventToEventQueue(KeyEvent.CTRL_MASK, VK_2);
        }
    }

    public void pressCtrlThree() {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_CONTROL).pressKey(VK_3)
                    .releaseKey(VK_3).releaseKey(VK_CONTROL);
        } else {
            postKeyEventToEventQueue(KeyEvent.CTRL_MASK, VK_3);
        }
    }

    public void pressCtrlFour() {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_CONTROL).pressKey(VK_4)
                    .releaseKey(VK_4).releaseKey(VK_CONTROL);
        } else {
            postKeyEventToEventQueue(KeyEvent.CTRL_MASK, VK_4);
        }
    }

    private void postKeyEventToEventQueue(int modifiers, int keyCode) {
        EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        Frame eventSource = pw.target();
        queue.postEvent(new KeyEvent(eventSource, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), modifiers,
                keyCode, Character.MIN_VALUE));
        queue.postEvent(new KeyEvent(eventSource, KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(), modifiers,
                keyCode, Character.MIN_VALUE));
    }

    public void pressCtrl() {
        pw.pressKey(VK_CONTROL);
    }

    public void releaseCtrl() {
        pw.releaseKey(VK_CONTROL);
    }

    public void pressAlt() {
        pw.pressKey(VK_ALT);
    }

    public void releaseAlt() {
        pw.releaseKey(VK_ALT);
    }

    public void pressShift() {
        pw.pressKey(VK_SHIFT);
    }

    public void releaseShift() {
        pw.releaseKey(VK_SHIFT);
    }
}
