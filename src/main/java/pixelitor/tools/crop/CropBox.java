/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.tools.crop;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The cropping rectangle
 */
public class CropBox implements ToolWidget {
    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;
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

    // type of user transform
    private int transformMode = MODE_NONE;

    // the width/height ratio of the selected area
    private double aspectRatio = 0;

    public CropBox(PRectangle rect, View view) {
        this.rect = rect;

        upperLeft = new CropHandle("NW", Cursor.NW_RESIZE_CURSOR, view);
        upper = new CropHandle("N", Cursor.N_RESIZE_CURSOR, view);
        upperRight = new CropHandle("NE", Cursor.NE_RESIZE_CURSOR, view);
        right = new CropHandle("E", Cursor.E_RESIZE_CURSOR, view);
        left = new CropHandle("W", Cursor.W_RESIZE_CURSOR, view);
        lowerLeft = new CropHandle("SW", Cursor.SW_RESIZE_CURSOR, view);
        lower = new CropHandle("S", Cursor.S_RESIZE_CURSOR, view);
        lowerRight = new CropHandle("SE", Cursor.SE_RESIZE_CURSOR, view);

        handles = List.of(upperLeft, upperRight, lowerRight, lowerLeft,
            right, upper, lower, left);
        updateHandles();
    }

    @Override
    public void paint(Graphics2D g) {
        drawRect(g);
        drawHandles(g);
    }

    private void drawRect(Graphics2D g) {
        Shapes.drawVisible(g, getSelectedCoRect());
    }

    private void drawHandles(Graphics2D g) {
        for (CropHandle handle : handles) {
            handle.paintHandle(g);
        }
    }

    private Rectangle getSelectedCoRect() {
        int upperLeftX = (int) upperLeft.getX();
        int upperRightX = (int) upperRight.getX();
        int upperLeftY = (int) upperLeft.getY();
        int lowerLeftY = (int) lowerLeft.getY();

        return Shapes.toPositiveRect(upperLeftX, upperRightX, upperLeftY, lowerLeftY);
    }

    /**
     * Iterates over all the handles and if it finds one that is
     * under the given mouse coordinates, sets the cursor accordingly.
     *
     * @return true if cursor was set on any handle, false otherwise
     */
    private boolean setCursorForPoint(double x, double y, View view) {
        CropHandle hit = handleWasHit(x, y);
        if (hit != null) {
            view.setCursor(hit.getCursor());
            return true;
        }

        return false;
    }

    @Override
    public CropHandle handleWasHit(double x, double y) {
        for (CropHandle handle : handles) {
            if (handle.handleContains(x, y)) {
                return handle;
            }
        }
        return null;
    }

    private void updateHandles() {
        Rectangle r = rect.getCo();
        int horMidX = r.x + r.width / 2;
        int verMidY = r.y + r.height / 2;
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
        View view = e.getView();
        dragStart = e.getPoint();
        dragStartRect = new Rectangle(rect.getCo());
        dragStartCursorType = view.getCursor().getType();
        aspectRatio = calcAspectRatio(rect.getCo());

        if (isResizeMode(dragStartCursorType)) {
            // if user clicked on the handle allow resize it
            transformMode = MODE_RESIZE;
        } else if (rect.containsCo(e.getPoint())) {
            // if user clicked inside selection allow relocate it
            transformMode = MODE_MOVE;
        } else {
            transformMode = MODE_NONE;
        }
    }

    public void mouseDragged(PMouseEvent e) {
        if (transformMode == MODE_NONE) {
            return;
        }

        rect.getCo().setRect(dragStartRect);
        Point mouseOffset = new Point(
            (int) (e.getCoX() - dragStart.x),
            (int) (e.getCoY() - dragStart.y));

        if (transformMode == MODE_RESIZE) {
            resize(rect.getCo(), dragStartCursorType, mouseOffset);

            if (e.isShiftDown() && aspectRatio > 0) {
                keepAspectRatio(rect.getCo(), dragStartCursorType, aspectRatio);
            }
        } else if (transformMode == MODE_MOVE) {
            rect.getCo().translate(mouseOffset.x, mouseOffset.y);
        }

        updateHandles();

        rect.makeCoPositive();
        rect.recalcIm(e.getView());
    }

    public void mouseReleased(PMouseEvent e) {
        // ensure that after resize rectangle has positive
        // width and height (required for Rectangle.contain testing)
        rect.makeCoPositive();

        updateHandles();
        setCursorForPoint(e.getCoX(), e.getCoY(), e.getView());

        transformMode = MODE_NONE;
    }

    public void mouseMoved(MouseEvent e, View view) {
        boolean isCursorSet = setCursorForPoint(e.getX(), e.getY(), view);
        if (!isCursorSet) {
            if (rect.containsCo(e.getX(), e.getY())) {
                view.setCursor(Cursors.MOVE);
            } else {
                view.setCursor(Cursors.DEFAULT);
            }
        }
    }

    public PRectangle getRect() {
        return rect;
    }

    @Override
    public void coCoordsChanged(View view) {
        rect.coCoordsChanged(view);
        updateHandles();
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        rect.imCoordsChanged(comp.getView(), at);
        updateHandles();
    }

    /**
     * Sets the size of the crop box in image space.
     */
    public void setImSize(int width, int height, View view) {
        Rectangle2D imRect = rect.getIm();
        imRect.setRect(imRect.getX(), imRect.getY(), width, height);

        rect.recalcCo(view);

        updateHandles();
        view.repaint();
    }

    /**
     * Returns true while the user is adjusting the handles.
     */
    public boolean isAdjusting() {
        return transformMode != MODE_NONE;
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
        // Two situations have to be taken into consideration:
        // 1. if the zoom level is >= 100%, then move the rectangle by 1px
        // 2. if the zoom level is < 100%, then scale up to ensure
        //    that the user always sees moving the rectangle
        double viewScale = view.getZoomLevel().getViewScale();
        int moveScale = viewScale >= 1 ? 1 : (int) Math.ceil(1 / viewScale);

        Rectangle2D im = rect.getIm();
        im.setRect(
            im.getX() + key.getMoveX() * moveScale,
            im.getY() + key.getMoveY() * moveScale,
            im.getWidth(),
            im.getHeight()
        );

        rect.recalcCo(view);
        updateHandles();
        view.repaint();
    }

    public static double calcAspectRatio(Rectangle rect) {
        if (rect.height > 0) {
            return (double) rect.width / rect.height;
        } else {
            return 0;
        }
    }

    private static boolean isResizeMode(int cursorType) {
        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
            case Cursor.SE_RESIZE_CURSOR:
            case Cursor.SW_RESIZE_CURSOR:
            case Cursor.NE_RESIZE_CURSOR:
            case Cursor.N_RESIZE_CURSOR:
            case Cursor.S_RESIZE_CURSOR:
            case Cursor.E_RESIZE_CURSOR:
            case Cursor.W_RESIZE_CURSOR:
                return true;
        }

        return false;
    }

    /**
     * Recalculates the box position and size, according to the mouse offset and direction
     *
     * @param rect       rectangle to adjust
     * @param cursorType user modification direction (N,S,W,E,NW,NE,SE,SW)
     * @param moveOffset the offset calculated as: mousePos - mouseStartPos
     */
    public static void resize(Rectangle rect, int cursorType, Point moveOffset) {
        int offsetX = moveOffset.x;
        int offsetY = moveOffset.y;
        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR -> {
                rect.width -= offsetX;
                rect.height -= offsetY;
                rect.x += offsetX;
                rect.y += offsetY;
            }
            case Cursor.SE_RESIZE_CURSOR -> {
                rect.width += offsetX;
                rect.height += offsetY;
            }
            case Cursor.SW_RESIZE_CURSOR -> {
                rect.width -= offsetX;
                rect.height += offsetY;
                rect.x += offsetX;
            }
            case Cursor.NE_RESIZE_CURSOR -> {
                rect.width += offsetX;
                rect.height -= offsetY;
                rect.y += offsetY;
            }
            case Cursor.N_RESIZE_CURSOR -> {
                rect.height -= offsetY;
                rect.y += offsetY;
            }
            case Cursor.S_RESIZE_CURSOR -> rect.height += offsetY;
            case Cursor.E_RESIZE_CURSOR -> rect.width += offsetX;
            case Cursor.W_RESIZE_CURSOR -> {
                rect.width -= offsetX;
                rect.x += offsetX;
            }
        }
    }

    /**
     * Adjusts the given rectangle to keep the aspect ratio.
     * Recalculates the x,y according to the drag direction.
     *
     * @param rect        rectangle to adjust
     * @param cursorType  user modification direction (N,S,W,E,NW,NE,SE,SW)
     * @param aspectRatio aspect ratio of original rectangle. Required > 0
     */
    public static void keepAspectRatio(Rectangle rect, int cursorType, double aspectRatio) {
        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
            case Cursor.NE_RESIZE_CURSOR:
            case Cursor.SE_RESIZE_CURSOR:
            case Cursor.SW_RESIZE_CURSOR:
                // To find out whether to adjust the width
                // or the height, compare the rectangle ratios.
                double aspectRatioNew = calcAspectRatio(rect);
                if (aspectRatioNew > aspectRatio) {
                    int height = (int) (rect.width / aspectRatio);
                    if (cursorType == Cursor.NW_RESIZE_CURSOR ||
                        cursorType == Cursor.NE_RESIZE_CURSOR) {
                        rect.y -= (height - rect.height);
                    }

                    rect.height = height;
                } else {
                    int width = (int) (rect.height * aspectRatio);
                    if (cursorType == Cursor.NW_RESIZE_CURSOR ||
                        cursorType == Cursor.SW_RESIZE_CURSOR) {
                        rect.x -= (width - rect.width);
                    }

                    rect.width = width;
                }
                break;
            case Cursor.N_RESIZE_CURSOR:
            case Cursor.S_RESIZE_CURSOR:
                // adjust width and center horizontally
                int width = (int) (rect.height * aspectRatio);
                rect.x -= (width - rect.width) / 2;
                rect.width = width;
                break;
            case Cursor.E_RESIZE_CURSOR:
            case Cursor.W_RESIZE_CURSOR:
                // adjust height and center vertically
                int height = (int) (rect.width / aspectRatio);
                rect.y -= (height - rect.height) / 2;
                rect.height = height;
                break;
        }
    }

    @Override
    public String toString() {
        return "CropBox{ upperLeft=" + upperLeft +
            ", lowerRight=" + lowerRight +
            ", rect=" + rect +
            ", adjusting=" + isAdjusting() +
            ", transformMode=" + transformMode +
            ", aspectRatio=" + aspectRatio +
            '}';
    }
}

