package pixelitor.tools.zoom;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.crop.CropHandle;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Shapes;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class ZoomBox implements ToolWidget {

    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;
    private static final int MODE_RESIZE = 2;
    private final PRectangle rect;
    private int upperLeftX = 0;
    private int upperLeftY = 0;
    private int lowerRightX = 0;
    private int lowerRightY = 0;
    private int dragStartCursorType;
    private Rectangle dragStartRect;
    private Point dragStart;

    // type of user transform
    private int transformMode = MODE_NONE;

    // the width/height ratio of the selected area
    private double aspectRatio = 0;

    public ZoomBox(PRectangle rect, View view) {
        this.rect = rect;
        updateHandles();
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
    public void paint(Graphics2D g) {
        drawRect(g);
    }

    private void drawRect(Graphics2D g) {
        Shapes.drawVisibly(g, getSelectedCoRect());
    }

    private Rectangle getSelectedCoRect() {
        return Shapes.toPositiveRect(upperLeftX, lowerRightX, upperLeftY, lowerRightY);
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
        return null;
    }

    private void updateHandles() {
        Rectangle r = rect.getCo();

        upperLeftX = r.x;
        upperLeftY = r.y;

        lowerRightX = r.x + r.width;
        lowerRightY = r.y + r.height;
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

    @Override
    public String toString() {
        return "ZoomBox{ upperLeft=" + upperLeftX + ", " + upperLeftY +
                ", lowerRight=" + lowerRightX + ", " + lowerRightY +
                ", rect=" + rect +
                ", adjusting=" + isAdjusting() +
                ", transformMode=" + transformMode +
                ", aspectRatio=" + aspectRatio +
                '}';
    }
}

