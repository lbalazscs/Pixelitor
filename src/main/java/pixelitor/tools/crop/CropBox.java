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

import static java.awt.Cursor.E_RESIZE_CURSOR;
import static java.awt.Cursor.NE_RESIZE_CURSOR;
import static java.awt.Cursor.NW_RESIZE_CURSOR;
import static java.awt.Cursor.N_RESIZE_CURSOR;
import static java.awt.Cursor.SE_RESIZE_CURSOR;
import static java.awt.Cursor.SW_RESIZE_CURSOR;
import static java.awt.Cursor.S_RESIZE_CURSOR;
import static java.awt.Cursor.W_RESIZE_CURSOR;

/**
 * The cropping widget with draggable handles for resizing it.
 */
public class CropBox implements ToolWidget {
    // transformation modes for user interactions
    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;
    private static final int MODE_RESIZE = 2;
    private int transformMode = MODE_NONE;

    // crop handles for resizing the crop box
    private final CropHandle topLeft;
    private final CropHandle top;
    private final CropHandle topRight;
    private final CropHandle right;
    private final CropHandle left;
    private final CropHandle bottomLeft;
    private final CropHandle bottom;
    private final CropHandle bottomRight;
    private final List<CropHandle> handles;

    private final PRectangle cropRect;
    private int dragStartCursor;
    private Rectangle dragStartRect;
    private Point dragStartPos;

    // the width/height ratio of the selected area
    private double aspectRatio = 0;

    public CropBox(PRectangle cropRect, View view) {
        this.cropRect = cropRect;

        topLeft = new CropHandle("NW", NW_RESIZE_CURSOR, view);
        top = new CropHandle("N", N_RESIZE_CURSOR, view);
        topRight = new CropHandle("NE", NE_RESIZE_CURSOR, view);
        right = new CropHandle("E", E_RESIZE_CURSOR, view);
        left = new CropHandle("W", W_RESIZE_CURSOR, view);
        bottomLeft = new CropHandle("SW", SW_RESIZE_CURSOR, view);
        bottom = new CropHandle("S", S_RESIZE_CURSOR, view);
        bottomRight = new CropHandle("SE", SE_RESIZE_CURSOR, view);

        handles = List.of(topLeft, topRight, bottomRight, bottomLeft,
            right, top, bottom, left);
        updateHandlePositions();
    }

    @Override
    public void paint(Graphics2D g) {
        drawCropRect(g);
        drawHandles(g);
    }

    private void drawCropRect(Graphics2D g) {
        Shapes.drawVisibly(g, getSelectedCoRect());
    }

    private void drawHandles(Graphics2D g) {
        for (CropHandle handle : handles) {
            handle.paintHandle(g);
        }
    }

    private Rectangle getSelectedCoRect() {
        int x1 = (int) topLeft.getX();
        int x2 = (int) topRight.getX();
        int y1 = (int) topLeft.getY();
        int y2 = (int) bottomLeft.getY();
        return Shapes.toPositiveRect(x1, x2, y1, y2);
    }

    /**
     * Sets the cursor to the appropriate type if the mouse is over a handle.
     *
     * @return true if the cursor was set, false otherwise
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

    private void updateHandlePositions() {
        Rectangle bounds = cropRect.getCo();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;
        int rightX = bounds.x + bounds.width;
        int bottomY = bounds.y + bounds.height;

        topLeft.setLocation(bounds.x, bounds.y);
        top.setLocation(centerX, bounds.y);
        topRight.setLocation(rightX, bounds.y);
        right.setLocation(rightX, centerY);
        left.setLocation(bounds.x, centerY);
        bottomLeft.setLocation(bounds.x, bottomY);
        bottom.setLocation(centerX, bottomY);
        bottomRight.setLocation(rightX, bottomY);
    }

    public void mousePressed(PMouseEvent e) {
        View view = e.getView();
        dragStartPos = new Point((int) e.getCoX(), (int) e.getCoY()); // snapped
        dragStartRect = new Rectangle(cropRect.getCo());
        dragStartCursor = view.getCursor().getType();
        aspectRatio = calcAspectRatio(cropRect.getCo());

        if (isResizeCursor(dragStartCursor)) {
            // if the user clicked on the handle, allow resizing
            transformMode = MODE_RESIZE;
        } else if (cropRect.containsCo(e.getPoint())) {
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

        Rectangle coRect = cropRect.getCo();
        coRect.setRect(dragStartRect);

        Point mouseOffset = new Point(
            (int) (e.getCoX() - dragStartPos.x),
            (int) (e.getCoY() - dragStartPos.y));

        if (transformMode == MODE_RESIZE) {
            resize(coRect, dragStartCursor, mouseOffset);

            if (e.isShiftDown() && aspectRatio > 0) {
                keepAspectRatio(coRect, dragStartCursor, aspectRatio, e.getView());
            }
        } else if (transformMode == MODE_MOVE) {
            coRect.translate(mouseOffset.x, mouseOffset.y);
        }

        updateHandlePositions();
        cropRect.ensureCoPositive();
        cropRect.recalcIm(e.getView());
    }

    public void mouseReleased(PMouseEvent e) {
        // ensure that after resize rectangle has positive
        // width and height (required for Rectangle.contain testing)
        cropRect.ensureCoPositive();

        updateHandlePositions();
        setCursorForPoint(e.getCoX(), e.getCoY(), e.getView());

        transformMode = MODE_NONE;
    }

    public void mouseMoved(MouseEvent e, View view) {
        boolean cursorSet = setCursorForPoint(e.getX(), e.getY(), view);
        if (!cursorSet) {
            if (cropRect.containsCo(e.getX(), e.getY())) {
                view.setCursor(Cursors.MOVE);
            } else {
                view.setCursor(Cursors.DEFAULT);
            }
        }
    }

    public PRectangle getCropRect() {
        return cropRect;
    }

    @Override
    public void coCoordsChanged(View view) {
        cropRect.coCoordsChanged(view);
        updateHandlePositions();
    }

    @Override
    public void imCoordsChanged(AffineTransform at, View view) {
        cropRect.imCoordsChanged(at, view);
        updateHandlePositions();
    }

    /**
     * Sets the size of the crop box in image space.
     */
    public void setImSize(int width, int height, View view) {
        Rectangle2D imRect = cropRect.getIm();
        imRect.setRect(imRect.getX(), imRect.getY(), width, height);

        cropRect.recalcCo(view);

        updateHandlePositions();
        view.repaint();
    }

    /**
     * Returns true if the crop box is being adjusted by the user.
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

        Rectangle2D im = cropRect.getIm();
        im.setRect(
            im.getX() + key.getMoveX() * moveScale,
            im.getY() + key.getMoveY() * moveScale,
            im.getWidth(),
            im.getHeight()
        );

        cropRect.recalcCo(view);
        updateHandlePositions();
        view.repaint();
    }

    public static double calcAspectRatio(Rectangle rect) {
        return (rect.height > 0) ? (double) rect.width / rect.height : 0;
    }

    private static boolean isResizeCursor(int cursor) {
        return switch (cursor) {
            case NW_RESIZE_CURSOR, SE_RESIZE_CURSOR,
                    SW_RESIZE_CURSOR, NE_RESIZE_CURSOR,
                    N_RESIZE_CURSOR, S_RESIZE_CURSOR,
                    E_RESIZE_CURSOR, W_RESIZE_CURSOR -> true;
            default -> false;
        };
    }

    /**
     * Resizes the rectangle based on the cursor position and mouse offset.
     */
    public static void resize(Rectangle rect, int cursor, Point offset) {
        int dx = offset.x;
        int dy = offset.y;
        switch (cursor) {
            case NW_RESIZE_CURSOR -> {
                rect.width -= dx;
                rect.height -= dy;
                rect.x += dx;
                rect.y += dy;
            }
            case SE_RESIZE_CURSOR -> {
                rect.width += dx;
                rect.height += dy;
            }
            case SW_RESIZE_CURSOR -> {
                rect.width -= dx;
                rect.height += dy;
                rect.x += dx;
            }
            case NE_RESIZE_CURSOR -> {
                rect.width += dx;
                rect.height -= dy;
                rect.y += dy;
            }
            case N_RESIZE_CURSOR -> {
                rect.height -= dy;
                rect.y += dy;
            }
            case S_RESIZE_CURSOR -> rect.height += dy;
            case E_RESIZE_CURSOR -> rect.width += dx;
            case W_RESIZE_CURSOR -> {
                rect.width -= dx;
                rect.x += dx;
            }
        }
    }

    /**
     * Adjusts the rectangle to maintain the aspect ratio based on the drag direction.
     */
    public static void keepAspectRatio(Rectangle rect, int cursor, double aspectRatio, View view) {
        switch (cursor) {
            case NW_RESIZE_CURSOR, NE_RESIZE_CURSOR,
                    SE_RESIZE_CURSOR, SW_RESIZE_CURSOR -> {
                // To find out whether to adjust the width
                // or the height, compare the rectangle ratios.
                double currentAspectRatio = calcAspectRatio(rect);
                if (currentAspectRatio > aspectRatio) {
                    int adjustedHeight = (int) (rect.width / aspectRatio);
                    if (cursor == NW_RESIZE_CURSOR || cursor == NE_RESIZE_CURSOR) {
                        rect.y -= (adjustedHeight - rect.height);
                    }
                    rect.height = adjustedHeight;
                } else {
                    int adjustedWidth = (int) (rect.height * aspectRatio);
                    if (cursor == NW_RESIZE_CURSOR || cursor == SW_RESIZE_CURSOR) {
                        rect.x -= (adjustedWidth - rect.width);
                    }

                    rect.width = adjustedWidth;
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
        return "CropBox{ topLeft=" + topLeft +
            ", bottomRight=" + bottomRight +
            ", cropRect=" + cropRect +
            ", adjusting=" + isAdjusting() +
            ", transformMode=" + transformMode +
            ", aspectRatio=" + aspectRatio +
            '}';
    }
}

