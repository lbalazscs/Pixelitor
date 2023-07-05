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

package pixelitor.guitest;

import org.assertj.swing.core.MouseButton;
import org.assertj.swing.core.Robot;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPopupMenuFixture;
import pixelitor.Views;
import pixelitor.utils.Utils;

import javax.swing.*;
import java.awt.Dialog;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Random;

import static java.awt.event.KeyEvent.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.swing.core.MouseButton.LEFT_BUTTON;

/**
 * Mouse input for AssertJ-Swing based tests
 */
public class Mouse {
    private final Robot robot;
    private Rectangle canvasBounds;
    private static final int CANVAS_SAFETY_DIST = 20;
    private final FrameFixture pw;
    private final Random random = new Random();

    public Mouse(FrameFixture pw, Robot robot) {
        this.robot = robot;
        this.pw = pw;
    }

    // move relative to the screen
    void moveToScreen(int x, int y) {
        robot.moveMouse(x, y);
    }

    // move relative to the canvas
    void moveToCanvas(int x, int y) {
        moveToScreen(x + canvasBounds.x, y + canvasBounds.y);
    }

    // drag relative to the screen
    void dragToScreen(int x, int y) {
        Point currentLoc = MouseInfo.getPointerInfo().getLocation();

        robot.pressMouse(LEFT_BUTTON);

        Utils.sleep(50, MILLISECONDS);
        moveToScreen((x + currentLoc.x) / 2, (y + currentLoc.y) / 2);

        Utils.sleep(50, MILLISECONDS);
        moveToScreen(x, y);

        Utils.sleep(50, MILLISECONDS);
        robot.releaseMouse(LEFT_BUTTON);
        robot.waitForIdle();
    }

    // drag relative to the canvas
    void dragToCanvas(int x, int y) {
        dragToScreen(x + canvasBounds.x, y + canvasBounds.y);
    }

    public void drag(CanvasDrag drag) {
        moveToCanvas(drag.getStartX(), drag.getStartY());
        dragToCanvas(drag.getEndX(), drag.getEndY());
    }

    // move relative to the given dialog
    void moveTo(DialogFixture dialog, int x, int y) {
        Dialog c = dialog.target();

        robot.moveMouse(c, x, y);
    }

    // drag relative to the given dialog
    void dragTo(DialogFixture dialog, int x, int y) {
        Dialog c = dialog.target();

        robot.pressMouse(LEFT_BUTTON);
        robot.moveMouse(c, x, y);
        robot.releaseMouse(LEFT_BUTTON);
    }

    void altDragToScreen(int x, int y) {
        robot.pressKey(VK_ALT);
        dragToScreen(x, y);
        robot.releaseKey(VK_ALT);
        robot.waitForIdle();
    }

    void altDragToCanvas(int x, int y) {
        altDragToScreen(x + canvasBounds.x, y + canvasBounds.y);
    }

    Point moveRandomlyWithinCanvas() {
        int x = createRandomScreenXWithinCanvas();
        int y = createRandomScreenYWithinCanvas();

        Point p = new Point(x, y);
        assert canvasBounds.contains(p);
        moveToScreen(x, y);

        return p;
    }

    Point dragRandomlyWithinCanvas() {
        int x = createRandomScreenXWithinCanvas();
        int y = createRandomScreenYWithinCanvas();

        Point p = new Point(x, y);
        assert canvasBounds.contains(p);
        dragToScreen(x, y);

        return p;
    }

    private int createRandomScreenXWithinCanvas() {
        return canvasBounds.x + CANVAS_SAFETY_DIST
            + random.nextInt(canvasBounds.width - CANVAS_SAFETY_DIST * 2);
    }

    private int createRandomScreenYWithinCanvas() {
        return canvasBounds.y + CANVAS_SAFETY_DIST
            + random.nextInt(canvasBounds.height - CANVAS_SAFETY_DIST * 2);
    }

    void shiftMoveClickRandom() {
        int x = createRandomScreenXWithinCanvas();
        int y = createRandomScreenYWithinCanvas();
        pw.pressKey(VK_SHIFT);
        moveToScreen(x, y);
        click();
        pw.releaseKey(VK_SHIFT);
    }

    void moveToActiveICCenter() {
        moveToScreen(canvasBounds.x + canvasBounds.width / 2,
            canvasBounds.y + canvasBounds.height / 2);
        robot.waitForIdle();
    }

    void click() {
        robot.pressMouse(LEFT_BUTTON);
        robot.releaseMouse(LEFT_BUTTON);
    }

    void doubleClick() {
        robot.pressMouse(LEFT_BUTTON);
        robot.releaseMouse(LEFT_BUTTON);
        robot.pressMouse(LEFT_BUTTON);
        robot.releaseMouse(LEFT_BUTTON);
    }

    void rightClick() {
        robot.pressMouse(MouseButton.RIGHT_BUTTON);
        robot.releaseMouse(MouseButton.RIGHT_BUTTON);
    }

    // this should be used only in special cases, where the
    // built-in AssertJ-Swing methods don't work
    JPopupMenuFixture showPopupAtCanvas(int x, int y) {
        moveToCanvas(x, y);
        rightClick();
        JPopupMenu popup = robot.findActivePopupMenu();
        assert popup != null : "no popup at (" + x + ", " + y + ")";
        return new JPopupMenuFixture(robot, popup);
    }

    void clickScreen(int x, int y) {
        moveToScreen(x, y);
        click();
    }

    void clickCanvas(int x, int y) {
        clickScreen(x + canvasBounds.x, y + canvasBounds.y);
    }

    void randomClick() {
        moveRandomlyWithinCanvas();
        click();
    }

    void randomClick(boolean ctrl, boolean alt, boolean shift) {
        moveRandomlyWithinCanvas();
        click(ctrl, alt, shift);
    }

    void randomDoubleClick() {
        moveRandomlyWithinCanvas();
        doubleClick();
    }

    void click(boolean ctrl, boolean alt, boolean shift) {
        if (ctrl) {
            robot.pressKey(VK_CONTROL);
        }
        if (alt) {
            robot.pressKey(VK_ALT);
        }
        if (shift) {
            robot.pressKey(VK_SHIFT);
        }

        click();

        if (ctrl) {
            robot.releaseKey(VK_CONTROL);
        }
        if (alt) {
            robot.releaseKey(VK_ALT);
        }
        if (shift) {
            robot.releaseKey(VK_SHIFT);
        }
    }

    void altClick() {
        robot.pressKey(VK_ALT);
        click();
        robot.releaseKey(VK_ALT);
    }

    void ctrlClick() {
        robot.pressKey(VK_CONTROL);
        click();
        robot.releaseKey(VK_CONTROL);
    }

    void shiftClick() {
        robot.pressKey(VK_SHIFT);
        click();
        robot.releaseKey(VK_SHIFT);
    }

    void ctrlClickScreen(int x, int y) {
        moveToScreen(x, y);
        ctrlClick();
    }

    void ctrlClickCanvas(int x, int y) {
        ctrlClickScreen(x + canvasBounds.x, y + canvasBounds.y);
    }

    void randomCtrlClick() {
        moveRandomlyWithinCanvas();
        ctrlClick();
    }

    void randomAltClick() {
        moveRandomlyWithinCanvas();
        altClick();
    }

    void randomShiftClick() {
        moveRandomlyWithinCanvas();
        shiftClick();
    }

    void dragFromCanvasCenterToTheRight() {
        // move to the canvas center
        moveToScreen(canvasBounds.x + canvasBounds.width / 2,
            canvasBounds.y + canvasBounds.height / 2);
        // drag horizontally to the right
        dragToScreen(canvasBounds.x + canvasBounds.width,
            canvasBounds.y + canvasBounds.height / 2);
    }

    void recalcCanvasBounds() {
        canvasBounds = EDT.call(() ->
            Views.getActive().getVisibleCanvasBoundsOnScreen());

//        debugCanvasBounds();
    }

    private void debugCanvasBounds() {
        JFrame frame = new JFrame("debug bounds");
        frame.setBounds(canvasBounds);
        frame.setVisible(true);
        Utils.sleep(3, SECONDS);
        frame.setVisible(false);
    }
}
