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

package pixelitor.tools.crop;

import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;

import static java.awt.Cursor.*;

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
    private int dragStartCursor;
    private Rectangle dragStartRect;
    private Point dragStart;

    // type of user transform
    private int transformMode = MODE_NONE;

    // the width/height ratio of the selected area
    private double aspectRatio = 0;

    public CropBox(PRectangle rect, View view) {
        this.rect = rect;

        upperLeft = new CropHandle("NW", NW_RESIZE_CURSOR, view);
        upper = new CropHandle("N", N_RESIZE_CURSOR, view);
        upperRight = new CropHandle("NE", NE_RESIZE_CURSOR, view);
        right = new CropHandle("E", E_RESIZE_CURSOR, view);
        left = new CropHandle("W", W_RESIZE_CURSOR, view);
        lowerLeft = new CropHandle("SW", SW_RESIZE_CURSOR, view);
        lower = new CropHandle("S", S_RESIZE_CURSOR, view);
        lowerRight = new CropHandle("SE", SE_RESIZE_CURSOR, view);

        handles = List.of(upperLeft, upperRight, lowerRight, lowerLeft,
                right, upper, lower, left);
        updateHandleLocations();
    }

    @Override
    public void paint(Graphics2D g) {
        drawRect(g);
        drawHandles(g);
    }

    private void drawRect(Graphics2D g) {
        Shapes.drawVisibly(g, getSelectedCoRect());
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
     * @return true if the cursor was set on any handle, false otherwise
     */
    private boolean setCursorForPoint(double x, double y, View view) {
        CropHandle handle = findHandleAt(x, y);
        if (handle != null) {
            view.setCursor(handle.getCursor());
            return true;
        }

        return false;
    }

    @Override
    public CropHandle findHandleAt(double x, double y) {
        for (CropHandle handle : handles) {
            if (handle.handleContains(x, y)) {
                return handle;
            }
        }
        return null;
    }

    private void updateHandleLocations() {
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
        dragStart = new Point((int) e.getCoX(), (int) e.getCoY()); // snapped
        dragStartRect = new Rectangle(rect.getCo());
        dragStartCursor = view.getCursor().getType();
        aspectRatio = calcAspectRatio(rect.getCo());

        if (isResizing(dragStartCursor)) {
            // if the user clicked on the handle, allow resizing
            transformMode = MODE_RESIZE;
        } else if (rect.containsCo(e.getPoint())) {
            // if the user clicked inside the rectangle, allow moving
            transformMode = MODE_MOVE;
        } else {
            transformMode = MODE_NONE;
        }
    }

    public void mouseDragged(PMouseEvent e) {
        if (transformMode == MODE_NONE) {
            return;
        }

        Rectangle coRect = rect.getCo();
        coRect.setRect(dragStartRect);
        Point mouseOffset = new Point(
            (int) (e.getCoX() - dragStart.x),
            (int) (e.getCoY() - dragStart.y));

        if (transformMode == MODE_RESIZE) {
            resize(coRect, dragStartCursor, mouseOffset);

            if (e.isShiftDown() && aspectRatio > 0) {
                keepAspectRatio(coRect, dragStartCursor, aspectRatio, e.getView());
            }
        } else if (transformMode == MODE_MOVE) {
            coRect.translate(mouseOffset.x, mouseOffset.y);
        }

        updateHandleLocations();

        rect.makeCoPositive();
        rect.recalcIm(e.getView());
    }

    public void mouseReleased(PMouseEvent e) {
        // ensure that after resize rectangle has positive
        // width and height (required for Rectangle.contain testing)
        rect.makeCoPositive();

        updateHandleLocations();
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
        updateHandleLocations();
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        rect.imCoordsChanged(at, view);
        updateHandleLocations();
    }

    /**
     * Sets the size of the crop box in image space.
     */
    public void setImSize(int width, int height, View view) {
        Rectangle2D imRect = rect.getIm();
        imRect.setRect(imRect.getX(), imRect.getY(), width, height);

        rect.recalcCo(view);

        updateHandleLocations();
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
        updateHandleLocations();
        view.repaint();
    }

    public static double calcAspectRatio(Rectangle rect) {
        if (rect.height > 0) {
            return (double) rect.width / rect.height;
        } else {
            return 0;
        }
    }

    private static boolean isResizing(int cursor) {
        return switch (cursor) {
            case NW_RESIZE_CURSOR, SE_RESIZE_CURSOR,
                    SW_RESIZE_CURSOR, NE_RESIZE_CURSOR,
                    N_RESIZE_CURSOR, S_RESIZE_CURSOR,
                    E_RESIZE_CURSOR, W_RESIZE_CURSOR -> true;
            default -> false;
        };
    }

    /**
     * Recalculates the box position and size, according to the mouse offset and direction
     *
     * @param rect       rectangle to adjust
     * @param cursor     user modification direction (N,S,W,E,NW,NE,SE,SW)
     * @param moveOffset the offset calculated as: mousePos - mouseStartPos
     */
    public static void resize(Rectangle rect, int cursor, Point moveOffset) {
        int offsetX = moveOffset.x;
        int offsetY = moveOffset.y;
        switch (cursor) {
            case NW_RESIZE_CURSOR -> {
                rect.width -= offsetX;
                rect.height -= offsetY;
                rect.x += offsetX;
                rect.y += offsetY;
            }
            case SE_RESIZE_CURSOR -> {
                rect.width += offsetX;
                rect.height += offsetY;
            }
            case SW_RESIZE_CURSOR -> {
                rect.width -= offsetX;
                rect.height += offsetY;
                rect.x += offsetX;
            }
            case NE_RESIZE_CURSOR -> {
                rect.width += offsetX;
                rect.height -= offsetY;
                rect.y += offsetY;
            }
            case N_RESIZE_CURSOR -> {
                rect.height -= offsetY;
                rect.y += offsetY;
            }
            case S_RESIZE_CURSOR -> rect.height += offsetY;
            case E_RESIZE_CURSOR -> rect.width += offsetX;
            case W_RESIZE_CURSOR -> {
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
     * @param cursor      user modification direction (N,S,W,E,NW,NE,SE,SW)
     * @param aspectRatio aspect ratio of original rectangle. Required > 0
     * @param view
     */
    public static void keepAspectRatio(Rectangle rect, int cursor, double aspectRatio, View view) {
        switch (cursor) {
            case NW_RESIZE_CURSOR, NE_RESIZE_CURSOR,
                    SE_RESIZE_CURSOR, SW_RESIZE_CURSOR -> {
                // To find out whether to adjust the width
                // or the height, compare the rectangle ratios.
                double newAspectRatio = calcAspectRatio(rect);
                if (newAspectRatio > aspectRatio) {
                    int height = (int) (rect.width / aspectRatio);
                    if (cursor == NW_RESIZE_CURSOR || cursor == NE_RESIZE_CURSOR) {
                        rect.y -= (height - rect.height);
                    }

                    rect.height = height;
                } else {
                    int width = (int) (rect.height * aspectRatio);
                    if (cursor == NW_RESIZE_CURSOR || cursor == SW_RESIZE_CURSOR) {
                        rect.x -= (width - rect.width);
                    }

                    rect.width = width;
                }
            }
            case N_RESIZE_CURSOR, S_RESIZE_CURSOR -> {
                // adjust width and center horizontally
                int width = (int) (rect.height * aspectRatio);
                rect.x -= (width - rect.width) / 2;
                rect.width = width;
            }
            case E_RESIZE_CURSOR, W_RESIZE_CURSOR -> {
                // adjust height and center vertically
                int height = (int) (rect.width / aspectRatio);
                rect.y -= (height - rect.height) / 2;
                rect.height = height;
            }
        }

        // snap to pixels
        if (view != null) { // not in unit tests
            Rectangle2D im = view.componentToImageSpace(rect);
            Rectangle co = view.imageToComponentSpace(im);
            rect.setRect(co);
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

