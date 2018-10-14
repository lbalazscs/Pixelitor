/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.ImageComponents;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import static java.awt.event.KeyEvent.VK_0;
import static java.awt.event.KeyEvent.VK_ADD;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_D;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_SUBTRACT;
import static java.awt.event.KeyEvent.VK_Z;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pixelitor.guitest.AssertJSwingTest.ROBOT_DELAY_DEFAULT;

/**
 * Keyboard input for {@link AssertJSwingTest}
 */
public class Keyboard {
    // on some Linux environments it can happen that robot key events are
    // generated multiple times
    private static final boolean osLevelKeyEvents = !JVM.isLinux;

    private final FrameFixture pw;
    private final Robot robot;
    private final AssertJSwingTest tester;

    public Keyboard(FrameFixture pw, Robot robot, AssertJSwingTest tester) {
        this.pw = pw;
        this.robot = robot;
        this.tester = tester;
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
            tester.runMenuCommand("Invert");
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
                    ImageComponents.getActiveCompOrNull().deselect(true));
        }
    }

    void fgBgDefaults() {
        if (osLevelKeyEvents) {
            // press D
            pw.pressKey(VK_D).releaseKey(VK_D);
        } else {
            GuiActionRunner.execute(() -> {
                FgBgColors.setFGColor(Color.BLACK);
                FgBgColors.setBGColor(Color.WHITE);
            });
        }
    }

    void actualPixels() {
        if (osLevelKeyEvents) {
            // press Ctrl-0
            pw.pressKey(VK_CONTROL).pressKey(VK_0).releaseKey(VK_0).releaseKey(VK_CONTROL);
        } else {
            tester.runMenuCommand("Actual Pixels");
        }
    }

    void nudge() {
        if (osLevelKeyEvents) {
            // TODO for some reason the shift is not detected
            pw.pressKey(VK_SHIFT).pressKey(VK_RIGHT)
                    .releaseKey(VK_RIGHT).releaseKey(VK_SHIFT);
        } else {
            postKeyEventToEventQueue(KeyEvent.SHIFT_MASK, KeyEvent.VK_RIGHT);
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

    void pressEsc() {
        if (osLevelKeyEvents) {
            pw.pressKey(VK_ESCAPE).releaseKey(VK_ESCAPE);
        } else {
            postKeyEventToEventQueue(0, VK_ESCAPE);
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
}
