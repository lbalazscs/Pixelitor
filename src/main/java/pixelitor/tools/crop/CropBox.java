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

package pixelitor.tools.crop;

import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

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
 * The cropping widget with draggable handles for resizing the crop area.
 */
public class CropBox implements ToolWidget, Debuggable {
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
    // list of handles for easy iteration
    private final List<CropHandle> handles;

    // the crop area in both coordinate spaces
    private final PRectangle cropRect;

    // state captured when a mouse drag starts
    private int dragStartCursor;
    private Rectangle coDragStartRect;
    private Point coDragStartPos;

    // the width/height ratio maintained when shift is held during resize
    private double aspectRatio = 0;

    public CropBox(PRectangle cropRect, View view) {
        this.cropRect = cropRect;

        // create handles with specific cursors
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
        // draw the outline of the crop rectangle
        Shapes.drawVisibly(g, getSelectedCoRect());

        // draw the handles
        for (CropHandle handle : handles) {
            handle.paintHandle(g);
        }
    }

    /**
     * Returns the component-space rectangle defined by the corner handles.
     */
    private Rectangle getSelectedCoRect() {
        int x1 = (int) topLeft.getX();
        int x2 = (int) topRight.getX();
        int y1 = (int) topLeft.getY();
        int y2 = (int) bottomLeft.getY();
        return Shapes.toPositiveRect(x1, x2, y1, y2);
    }

    /**
     * Sets the cursor to a resize cursor if the mouse is over a handle.
     *
     * @return true if the cursor was set, false otherwise
     */
    private boolean setHandleCursor(double x, double y, View view) {
        CropHandle handle = findHandleAt(x, y);
        if (handle != null) {
            view.setCursor(handle.getCursor());
            return true;
        }
        return false; // no handle found at this location
    }

    @Override
    public CropHandle findHandleAt(double coX, double coY) {
        for (CropHandle handle : handles) {
            if (handle.contains(coX, coY)) {
                return handle;
            }
        }
        return null;
    }

    /**
     * Updates the positions of all handles based on the current crop rectangle.
     */
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

    /**
     * Records the initial state when the mouse button is pressed over the crop box or its handles.
     */
    public void mousePressed(PMouseEvent e) {
        View view = e.getView();
        coDragStartPos = new Point((int) e.getCoX(), (int) e.getCoY()); // snapped
        coDragStartRect = new Rectangle(cropRect.getCo());
        dragStartCursor = view.getCursor().getType();
        aspectRatio = calcAspectRatio(cropRect.getCo());

        if (isResizeCursor(dragStartCursor)) {
            // if the user clicked on the handle, allow resizing
            transformMode = MODE_RESIZE;
        } else if (cropRect.containsCo(e.getPoint())) {
            // if the user clicked inside the rectangle, allow moving
            transformMode = MODE_MOVE;
        } else {
            transformMode = MODE_NONE; // clicked outside
        }
    }

    /**
     * Updates the crop box position or size based on mouse movement while dragging.
     */
    public void mouseDragged(PMouseEvent e) {
        if (transformMode == MODE_NONE) {
            return; // nothing to do if not moving or resizing
        }

        // start from the original rectangle each time
        Rectangle coRect = new Rectangle(coDragStartRect);

        // mouse movement delta in component space
        Point mouseOffset = new Point(
            (int) (e.getCoX() - coDragStartPos.x),
            (int) (e.getCoY() - coDragStartPos.y));

        // apply transformation based on mode
        if (transformMode == MODE_RESIZE) {
            resize(coRect, dragStartCursor, mouseOffset);

            // maintain aspect ratio if shift is pressed and ratio is valid
            if (e.isShiftDown() && aspectRatio > 0) {
                keepAspectRatio(coRect, dragStartCursor, aspectRatio, e.getView());
            }
        } else if (transformMode == MODE_MOVE) {
            // simply translate the rectangle
            coRect.translate(mouseOffset.x, mouseOffset.y);
        }

        // update the actual crop rectangle with the modified temporary one
        cropRect.getCo().setRect(coRect);
        // ensure width/height are positive after potential resize inversions
        cropRect.ensureCoPositive();
        // recalculate image-space rectangle based on new component-space rect
        cropRect.recalcIm(e.getView());
        // update handle positions to match the new rectangle
        updateHandlePositions();
    }

    /**
     * Finalizes the crop box adjustment after the mouse button is released.
     */
    public void mouseReleased(PMouseEvent e) {
        if (transformMode == MODE_NONE) {
            return; // nothing to finalize if we weren't transforming
        }

        // ensure final rectangle dimensions are positive
        cropRect.ensureCoPositive();

        updateHandlePositions();

        // set cursor based on final position
        setHandleCursor(e.getCoX(), e.getCoY(), e.getView());

        // reset interaction mode
        transformMode = MODE_NONE;
    }

    public void mouseMoved(MouseEvent e, View view) {
        // only change cursor if not currently adjusting the box
        if (isAdjusting()) {
            return;
        }

        // try setting a resize cursor if over a handle
        boolean handleCursorSet = setHandleCursor(e.getX(), e.getY(), view);
        if (!handleCursorSet) {
            // if not over a handle, set move cursor if inside the box, otherwise default
            view.setCursor(cropRect.containsCo(e.getX(), e.getY())
                ? Cursors.MOVE
                : Cursors.DEFAULT);
        }
    }

    /**
     * Returns the {@link PRectangle} representing the crop area.
     */
    public PRectangle getCropRect() {
        return cropRect;
    }

    /**
     * Captures the internal state of this {@link CropBox}
     * so that it can be returned to this state later.
     */
    public Rectangle2D getImCropRect() {
        return (Rectangle2D) cropRect.getIm().clone();
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
     * Sets the size of the crop box in image-space coordinates and updates the view.
     */
    public void setImSize(int width, int height, View view) {
        Rectangle2D imRect = cropRect.getIm();

        // the rectangle's top-left position is maintained
        imRect.setRect(imRect.getX(), imRect.getY(), width, height);

        cropRect.recalcCo(view);

        updateHandlePositions();
        view.repaint();
    }

    public void setImSize(Rectangle2D newRect, View view) {
        Rectangle2D imRect = cropRect.getIm();
        imRect.setRect(newRect);

        cropRect.recalcCo(view);

        updateHandlePositions();
        view.repaint();
    }

    /**
     * Returns true if the crop box is currently being moved or resized by the user.
     */
    public boolean isAdjusting() {
        return transformMode != MODE_NONE;
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
        // move by 1 image pixel if zoom is >= 100%, otherwise
        // scale move distance so that the user always sees a change
        double viewScale = view.getZoomLevel().getScale();
        int moveScale = viewScale >= 1 ? 1 : (int) Math.ceil(1 / viewScale);

        Rectangle2D im = cropRect.getIm();
        im.setRect(
            im.getX() + key.getDeltaX() * moveScale,
            im.getY() + key.getDeltaY() * moveScale,
            im.getWidth(),
            im.getHeight()
        );

        cropRect.recalcCo(view);
        updateHandlePositions();
        view.repaint();
    }

    public static double calcAspectRatio(Rectangle rect) {
        // return 0 if height is zero to avoid division
        // by zero and represent undefined aspect ratio
        return (rect.height > 0) ? (double) rect.width / rect.height : 0;
    }

    /**
     * Checks if the given cursor type corresponds to a resize handle.
     */
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
     * Resizes the given rectangle based on the active resize cursor and mouse offset.
     */
    public static void resize(Rectangle rect, int cursor, Point offset) {
        int dx = offset.x;
        int dy = offset.y;

        // adjust rectangle bounds based on which handle is being dragged
        switch (cursor) {
            case NW_RESIZE_CURSOR -> { // top-left
                rect.width -= dx;
                rect.height -= dy;
                rect.x += dx;
                rect.y += dy;
            }
            case SE_RESIZE_CURSOR -> { // bottom-right
                rect.width += dx;
                rect.height += dy;
            }
            case SW_RESIZE_CURSOR -> { // bottom-left
                rect.width -= dx;
                rect.height += dy;
                rect.x += dx;
            }
            case NE_RESIZE_CURSOR -> { // top-right
                rect.width += dx;
                rect.height -= dy;
                rect.y += dy;
            }
            case N_RESIZE_CURSOR -> { // top
                rect.height -= dy;
                rect.y += dy;
            }
            case S_RESIZE_CURSOR -> rect.height += dy; // bottom
            case E_RESIZE_CURSOR -> rect.width += dx; // right
            case W_RESIZE_CURSOR -> { // left
                rect.width -= dx;
                rect.x += dx;
            }
        }
    }

    /**
     * Adjusts the rectangle dimensions to maintain a fixed aspect ratio during resizing.
     */
    public static void keepAspectRatio(Rectangle rect, int cursor,
                                       double targetAspectRatio, View view) {
        assert targetAspectRatio != 0; // should be called only with valid ratios

        switch (cursor) {
            case NW_RESIZE_CURSOR, NE_RESIZE_CURSOR,
                    SE_RESIZE_CURSOR, SW_RESIZE_CURSOR -> {
                // corner handles: adjust the dimension that
                // deviates more from the target ratio
                double currentAspectRatio = calcAspectRatio(rect);
                if (currentAspectRatio > targetAspectRatio) {
                    // rectangle is too wide, adjust height based on width
                    int adjustedHeight = (int) (rect.width / targetAspectRatio);
                    if (cursor == NW_RESIZE_CURSOR || cursor == NE_RESIZE_CURSOR) {
                        // adjust y position for top handles
                        rect.y -= (adjustedHeight - rect.height);
                    }
                    rect.height = adjustedHeight;
                } else {
                    // rectangle is too tall (or aspect ratio is correct),
                    // adjust width based on height
                    int adjustedWidth = (int) (rect.height * targetAspectRatio);
                    if (cursor == NW_RESIZE_CURSOR || cursor == SW_RESIZE_CURSOR) {
                        // adjust x position for left handles
                        rect.x -= (adjustedWidth - rect.width);
                    }

                    rect.width = adjustedWidth;
                }
            }
            case N_RESIZE_CURSOR, S_RESIZE_CURSOR -> {
                // top/bottom handles: adjust width based on height and center horizontally
                int width = (int) (rect.height * targetAspectRatio);
                rect.x -= (width - rect.width) / 2;
                rect.width = width;
            }
            case E_RESIZE_CURSOR, W_RESIZE_CURSOR -> {
                // left/right handles: adjust height based on width and center vertically
                int height = (int) (rect.width / targetAspectRatio);
                rect.y -= (height - rect.height) / 2;
                rect.height = height;
            }
        }

        // snap the resulting component-space rectangle
        // to image pixels if snapping is enabled
        if (view != null) { // not in unit tests
            Rectangle2D im = view.componentToImageSpace(rect); // could snap
            Rectangle co = view.imageToComponentSpace(im);
            rect.setRect(co);
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.add(DebugNodes.createRectangle2DNode("cropRect im", cropRect.getIm()));
        node.add(DebugNodes.createRectangleNode("cropRect co", cropRect.getCo()));

        return node;
    }
}
