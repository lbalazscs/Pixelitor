/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.utils.input.Modifiers;

import javax.swing.*;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Random;

import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.swing.core.MouseButton.LEFT_BUTTON;

/**
 * Mouse input for AssertJ-Swing based tests
 */
public class Mouse {
    private final Robot robot;
    private Rectangle canvasBounds; // relative to the screen
    private static final int CANVAS_SAFETY_MARGIN = 20;
    private final FrameFixture pw;
    private final Random random = new Random();

    public Mouse(FrameFixture pw, Robot robot) {
        this.robot = robot;
        this.pw = pw;
    }

    /**
     * Moves the mouse to absolute screen coordinates.
     */
    void moveToScreen(int screenX, int screenY) {
        robot.moveMouse(screenX, screenY);
    }

    /**
     * Moves the mouse to coordinates relative to the canvas.
     */
    void moveToCanvas(int canvasX, int canvasY) {
        moveToScreen(
            canvasBounds.x + canvasX,
            canvasBounds.y + canvasY);
    }

    // drag relative to the screen
    void dragToScreen(int targetX, int targetY) {
        Point currentPos = MouseInfo.getPointerInfo().getLocation();

        robot.pressMouse(LEFT_BUTTON);

        // Move to intermediate point
        Utils.sleep(50, MILLISECONDS);
        moveToScreen(
            (targetX + currentPos.x) / 2,
            (targetY + currentPos.y) / 2);

        // Move to final position
        Utils.sleep(50, MILLISECONDS);
        moveToScreen(targetX, targetY);

        Utils.sleep(50, MILLISECONDS);
        robot.releaseMouse(LEFT_BUTTON);
        robot.waitForIdle();
    }

    // drag relative to the canvas
    void dragToCanvas(int canvasX, int canvasY) {
        dragToScreen(
            canvasBounds.x + canvasX,
            canvasBounds.y + canvasY);
    }

    public void drag(CanvasDrag drag) {
        moveToCanvas(drag.getStartX(), drag.getStartY());
        dragToCanvas(drag.getEndX(), drag.getEndY());
    }

    // move relative to the given dialog
    void moveTo(DialogFixture dialog, int dialogX, int dialogY) {
        robot.moveMouse(dialog.target(), dialogX, dialogY);
    }

    // drag relative to the given dialog
    void dragTo(DialogFixture dialog, int dialogX, int dialogY) {
        robot.pressMouse(LEFT_BUTTON);
        robot.moveMouse(dialog.target(), dialogX, dialogY);
        robot.releaseMouse(LEFT_BUTTON);
    }

    void altDragToScreen(int screenX, int screenY) {
        robot.pressKey(VK_ALT);
        dragToScreen(screenX, screenY);
        robot.releaseKey(VK_ALT);
        robot.waitForIdle();
    }

    void altDragToCanvas(int canvasX, int canvasY) {
        altDragToScreen(
            canvasBounds.x + canvasX,
            canvasBounds.y + canvasY);
    }

    Point moveRandomlyWithinCanvas() {
        int randomX = generateRandomScreenXWithinCanvas();
        int randomY = generateRandomScreenYWithinCanvas();

        Point randomPoint = new Point(randomX, randomY);
        assert canvasBounds.contains(randomPoint);
        moveToScreen(randomX, randomY);

        return randomPoint;
    }

    Point dragRandomlyWithinCanvas() {
        int randomX = generateRandomScreenXWithinCanvas();
        int randomY = generateRandomScreenYWithinCanvas();

        Point randomPoint = new Point(randomX, randomY);
        assert canvasBounds.contains(randomPoint);
        dragToScreen(randomX, randomY);

        return randomPoint;
    }

    private int generateRandomScreenXWithinCanvas() {
        return canvasBounds.x + CANVAS_SAFETY_MARGIN
            + random.nextInt(canvasBounds.width - CANVAS_SAFETY_MARGIN * 2);
    }

    private int generateRandomScreenYWithinCanvas() {
        return canvasBounds.y + CANVAS_SAFETY_MARGIN
            + random.nextInt(canvasBounds.height - CANVAS_SAFETY_MARGIN * 2);
    }

    void shiftMoveClickRandom() {
        int randomX = generateRandomScreenXWithinCanvas();
        int randomY = generateRandomScreenYWithinCanvas();
        pw.pressKey(VK_SHIFT);
        moveToScreen(randomX, randomY);
        click();
        pw.releaseKey(VK_SHIFT);
    }

    void moveToActiveCanvasCenter() {
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
    JPopupMenuFixture showPopupAtCanvas(int canvasX, int canvasY) {
        moveToCanvas(canvasX, canvasY);
        rightClick();
        JPopupMenu popup = robot.findActivePopupMenu();
        assert popup != null : "no popup at (" + canvasX + ", " + canvasY + ")";
        return new JPopupMenuFixture(robot, popup);
    }

    void clickScreen(int screenX, int screenY) {
        moveToScreen(screenX, screenY);
        click();
    }

    void clickCanvas(int canvasX, int canvasY) {
        clickScreen(
            canvasBounds.x + canvasX,
            canvasBounds.y + canvasY);
    }

    void randomClick() {
        moveRandomlyWithinCanvas();
        click();
    }

    void randomDoubleClick() {
        moveRandomlyWithinCanvas();
        doubleClick();
    }

    void randomClick(Modifiers modifiers) {
        moveRandomlyWithinCanvas();
        click(modifiers);
    }

    void click(Modifiers modifiers) {
        if (modifiers.ctrl().isDown()) {
            robot.pressKey(VK_CONTROL);
        }
        if (modifiers.alt().isDown()) {
            robot.pressKey(VK_ALT);
        }
        if (modifiers.shift().isDown()) {
            robot.pressKey(VK_SHIFT);
        }

        if (modifiers.button().isRight()) {
            rightClick();
        } else {
            click();
        }

        if (modifiers.shift().isDown()) {
            robot.releaseKey(VK_SHIFT);
        }
        if (modifiers.alt().isDown()) {
            robot.releaseKey(VK_ALT);
        }
        if (modifiers.ctrl().isDown()) {
            robot.releaseKey(VK_CONTROL);
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

    void ctrlClickScreen(int screenX, int screenY) {
        moveToScreen(screenX, screenY);
        ctrlClick();
    }

    void ctrlClickCanvas(int canvasX, int canvasY) {
        ctrlClickScreen(
            canvasBounds.x + canvasX,
            canvasBounds.y + canvasY);
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

    void updateCanvasBounds() {
        canvasBounds = EDT.call(() ->
            Views.getActive().getVisibleCanvasBoundsOnScreen());
    }

    private void debugCanvasBounds() {
        JFrame frame = new JFrame("debug bounds");
        frame.setBounds(canvasBounds);
        frame.setVisible(true);
        Utils.sleep(3, SECONDS);
        frame.setVisible(false);
    }
}
