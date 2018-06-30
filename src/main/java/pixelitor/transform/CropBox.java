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

package pixelitor.transform;

import pixelitor.gui.ImageComponent;
import pixelitor.gui.View;
import pixelitor.tools.ArrowKey;
import pixelitor.tools.PMouseEvent;
import pixelitor.tools.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.List;

/**
 * The cropping rectangle
 */
public class CropBox {

    private static final int MODE_NONE = 0;
    private static final int MODE_RELOCATE = 1;
    private static final int MODE_RESIZE = 2;

    private final CropHandle upperLeft;
    private final CropHandle upper;
    private final CropHandle upperRight;
    private final CropHandle right;
    private final CropHandle left;
    private final CropHandle lowerLeft;
    private final CropHandle lower;
    private final CropHandle lowerRight;

    private final List<CropHandle> handles;

    private final PRectangle rect;
    private int dragStartCursorType;
    private Rectangle dragStartRect;
    private Point dragStart;

    // true while the user is adjusting the handles
    private boolean adjusting;

    // type of user transform
    private int transformMode = MODE_NONE;

    // keep the aspect ratio of the selected area
    private boolean useAspectRatio = false;

    // ratio width/height of the selected area
    private double aspectRatio = 0;

    public CropBox(PRectangle rect, ImageComponent ic) {
        this.rect = rect;

        upperLeft = new CropHandle("NW", Cursor.NW_RESIZE_CURSOR, ic);
        upper = new CropHandle("N", Cursor.N_RESIZE_CURSOR, ic);
        upperRight = new CropHandle("NE", Cursor.NE_RESIZE_CURSOR, ic);
        right = new CropHandle("E", Cursor.E_RESIZE_CURSOR, ic);
        left = new CropHandle("W", Cursor.W_RESIZE_CURSOR, ic);
        lowerLeft = new CropHandle("SW", Cursor.SW_RESIZE_CURSOR, ic);
        lower = new CropHandle("S", Cursor.S_RESIZE_CURSOR, ic);
        lowerRight = new CropHandle("SE", Cursor.SE_RESIZE_CURSOR, ic);

        handles = Arrays.asList(upperLeft, upperRight, lowerRight, lowerLeft,
                right, upper, lower, left);
        update(rect);

    }

    public void setUseAspectRatio(boolean useAspectRatio) {
        this.useAspectRatio = useAspectRatio;
    }

    public void paintHandles(Graphics2D g) {
        drawRect(g);
        drawHandles(g);
    }

    private void drawRect(Graphics2D g) {
        Rectangle rect = getSelectedRectInComponentSpace();

        Shapes.drawVisible(g, rect);
    }

    private void drawHandles(Graphics2D g) {
        for (CropHandle handle : handles) {
            handle.paintHandle(g);
        }
    }

    private Rectangle getSelectedRectInComponentSpace() {
        int upperLeftX = (int) upperLeft.getX();
        int upperRightX = (int) upperRight.getX();
        int upperLeftY = (int) upperLeft.getY();
        int lowerLeftY = (int) lowerLeft.getY();

        return Shapes.toPositiveRect(upperLeftX, upperRightX, upperLeftY, lowerLeftY);
    }

    /**
     * Iterates over all the handles and if finds one that is over the point, its cursor is set
     * The coordinates are in image space
     *
     * @return true if cursor was set on any handle, false otherwise
     */
    public boolean setCursorForPoint(int x, int y, ImageComponent c) {
        for (CropHandle handle : handles) {
            if (handle.handleContains(x, y)) {
                c.setCursor(handle.getCursor());
                return true;
            }
        }

        return false;
    }

    public void update(PRectangle rect) {
        Rectangle r = rect.getCo();
        int horMidX = r.x + (r.width / 2);
        int verMidY = r.y + (r.height / 2);
        int horEndX = r.x + r.width;
        int verEndY = r.y + r.height;

        upperLeft.setLocation(r.x, r.y);
        upper.setLocation(horMidX, r.y);
        upperRight.setLocation(horEndX, r.y);
        right.setLocation(horEndX, verMidY);
        left.setLocation(r.x, verMidY);
        lowerLeft.setLocation(r.x, verEndY);
        lower.setLocation(horMidX, verEndY);
        lowerRight.setLocation(horEndX, verEndY);
    }

    public void mousePressed(PMouseEvent e) {
        ImageComponent ic = e.getIC();
        dragStart = e.getPoint();
        dragStartRect = new Rectangle(rect.getCo());
        dragStartCursorType = ic.getCursor().getType();
        aspectRatio = TransformHelper.calcAspectRatio(rect.getCo());

        if (TransformHelper.isResizeMode(dragStartCursorType)) {
            // if user clicked on the handle allow resize it
            transformMode = MODE_RESIZE;
        } else if (rect.containsCo(e.getPoint())) {
            // if user clicked inside selection allow relocate it
            transformMode = MODE_RELOCATE;
        } else {
            transformMode = MODE_NONE;
        }
    }

    public void mouseDragged(PMouseEvent e) {
        if (transformMode == MODE_NONE) {
            return;
        }

        // reset rect
        // TODO is this necessary?
        rect.getCo().setRect(dragStartRect);
        Point mouseOffset = new Point(e.getCoX() - dragStart.x, e.getCoY() - dragStart.y);

        if (transformMode == MODE_RESIZE) {
            TransformHelper.resize(rect.getCo(), dragStartCursorType, mouseOffset);

            if (useAspectRatio && aspectRatio > 0) {
                TransformHelper.keepAspectRatio(rect.getCo(), dragStartCursorType, aspectRatio);
            }
        } else if (transformMode == MODE_RELOCATE) {
            rect.getCo().translate(mouseOffset.x, mouseOffset.y);
        }

        adjusting = true;
        update(rect);

        rect.makeCoPositive();
        rect.recalcIm(e.getIC());
    }

    public void mouseReleased(PMouseEvent e) {
        // we need to ensure that after resize rectangle has
        // positive width and height (required for Rectangle.contain testing)
        rect.makeCoPositive();
        update(rect);
        setCursorForPoint(e.getCoX(), e.getCoY(), e.getIC());

        adjusting = false;
        transformMode = MODE_NONE;
    }

    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        boolean isCursorSet = setCursorForPoint(e.getX(), e.getY(), ic);
        if (!isCursorSet) {
            if (rect.containsCo(e.getX(), e.getY())) {
                ic.setCursor(Cursors.MOVE);
            } else {
                ic.setCursor(Cursors.DEFAULT);
            }
        }
    }

    public PRectangle getRect() {
        return rect;
    }

    public void viewSizeChanged(View ic) {
        rect.viewSizeChanged(ic);
        update(rect);
    }

    /**
     * Set size of selection in image space
     */
    public void setImSize(int width, int height, ImageComponent ic) {
        Rectangle2D imageSpaceRect = rect.getIm();
        imageSpaceRect.setRect(imageSpaceRect.getX(), imageSpaceRect.getY(), width, height);

        rect.recalcCo(ic);

        update(rect);
        ic.repaint();
    }

    /**
     * Return true while the user is adjusting the handles
     */
    public boolean isAdjusting() {
        return adjusting;
    }

    public void arrowKeyPressed(ArrowKey key, ImageComponent ic) {

        // two situation we need to take into consideration
        // 1. user zoom level is >= 100% then we always move rect by 1px
        //    user is in pixel perfect precision mode
        // 2. user zoom level is < 100% then we scale up to ensure that user always see
        //    rect movement

        double viewScale = ic.getZoomLevel().getViewScale();
        int moveScale = viewScale >= 1 ? 1 : (int) Math.ceil(1 / viewScale);

        Rectangle2D im = rect.getIm();
        im.setRect(
                im.getX() + (key.getMoveX() * moveScale),
                im.getY() + (key.getMoveY() * moveScale),
                im.getWidth(),
                im.getHeight()
        );

        rect.recalcCo(ic);
        update(rect);
        ic.repaint();
    }
}

