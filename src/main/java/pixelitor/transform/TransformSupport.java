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
import pixelitor.tools.ArrowKey;
import pixelitor.utils.Cursors;
import pixelitor.utils.Utils;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * Helps with interactive manipulation of selections/transforms
 */
public class TransformSupport {

    private static final int MODE_NONE = 0;
    private static final int MODE_RELOCATE = 1;
    private static final int MODE_RESIZE = 2;

    private final Handles handles;
    private Rectangle compSpaceRect;
    private Rectangle2D imageSpaceRect;
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

    public TransformSupport(Rectangle compSpaceRect, Rectangle2D imageSpaceRect) {
        this.compSpaceRect = compSpaceRect;
        this.imageSpaceRect = imageSpaceRect;
        handles = new Handles(compSpaceRect);
    }

    public void setUseAspectRatio(boolean useAspectRatio) {
        this.useAspectRatio = useAspectRatio;
    }

    public void paintHandles(Graphics2D g) {
        handles.paint(g);
    }

    public void mousePressed(MouseEvent e, ImageComponent ic) {
        dragStart = e.getPoint();
        dragStartRect = new Rectangle(compSpaceRect);
        dragStartCursorType = ic.getCursor().getType();
        aspectRatio = TransformHelper.calcAspectRatio(dragStartRect);

        if (TransformHelper.isResizeMode(dragStartCursorType)) {
            // if user clicked on the handle allow resize it
            transformMode = MODE_RESIZE;
        } else if (compSpaceRect.contains(e.getPoint())) {
            // if user clicked inside selection allow relocate it
            transformMode = MODE_RELOCATE;
        } else {
            transformMode = MODE_NONE;
        }
    }

    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        if (transformMode == MODE_NONE) {
            return;
        }

        // reset rect
        compSpaceRect.setRect(dragStartRect);
        Point mouseOffset = new Point(e.getX() - dragStart.x, e.getY() - dragStart.y);

        if (transformMode == MODE_RESIZE) {
            TransformHelper.resize(compSpaceRect, dragStartCursorType, mouseOffset);

            if (useAspectRatio && aspectRatio > 0) {
                TransformHelper.keepAspectRatio(compSpaceRect, dragStartCursorType, aspectRatio);
            }
        } else if (transformMode == MODE_RELOCATE) {
            compSpaceRect.translate(mouseOffset.x, mouseOffset.y);
        }

        adjusting = true;
        handles.updateRect(compSpaceRect);
        recalculateImageSpaceRect(ic);
    }

    public void mouseReleased(MouseEvent e, ImageComponent ic) {
        // we need to ensure that after resize rectangle has
        // positive width and height (required for Rectangle.contain testing)
        compSpaceRect = Utils.toPositiveRect(compSpaceRect);
        handles.updateRect(compSpaceRect);
        handles.setCursorForPoint(e.getX(), e.getY(), ic);

        adjusting = false;
        transformMode = MODE_NONE;
    }

    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        boolean isCursorSet = handles.setCursorForPoint(e.getX(), e.getY(), ic);
        if (!isCursorSet) {
            if (compSpaceRect.contains(e.getX(), e.getY())) {
                ic.setCursor(Cursors.MOVE);
            } else {
                ic.setCursor(Cursors.DEFAULT);
            }
        }
    }

    public Rectangle2D getImageSpaceRect() {
        return imageSpaceRect;
    }

    public Rectangle getComponentSpaceRect() {
        return compSpaceRect;
    }

    @Override
    public String toString() {
        return "TransformSupport{" +
                "handles=" + handles +
                ", compSpaceRect=" + compSpaceRect +
                ", dragStart=" + dragStart +
                ", dragStartRect=" + dragStartRect +
                '}';
    }

    /**
     * Used only while the image component is resized
     */
    public void setComponentSpaceRect(Rectangle compSpaceRect) {
        this.compSpaceRect = compSpaceRect;
        handles.updateRect(compSpaceRect);
    }

    /**
     * Set size of selection in image space
     */
    public void setSize(int width, int height, ImageComponent ic) {

        this.imageSpaceRect.setRect(this.imageSpaceRect.getX(), this.imageSpaceRect.getY(), width, height);

        compSpaceRect = ic.fromImageToComponentSpace(imageSpaceRect);
        handles.updateRect(compSpaceRect);
        ic.repaint();
    }

    /**
     * Return true while the user is adjusting the handles
     */
    public boolean isAdjusting() {
        return adjusting;
    }

    private void recalculateImageSpaceRect(ImageComponent ic) {
        Rectangle2D possiblyNegativeRect = ic.fromComponentToImageSpace(compSpaceRect);
        this.imageSpaceRect = Utils.toPositiveRect(possiblyNegativeRect);
    }

    public void arrowKeyPressed(ArrowKey key, ImageComponent ic) {

        // two situation we need to take into consideration
        // 1. user zoom level is >= 100% then we always move rect by 1px
        //    user is in pixel perfect precision mode
        // 2. user zoom level is < 100% then we scale up to ensure that user always see
        //    rect movement

        double viewScale = ic.getZoomLevel().getViewScale();
        int moveScale = viewScale >= 1 ? 1: (int) Math.ceil(1/viewScale);

        imageSpaceRect.setRect(
            this.imageSpaceRect.getX() + (key.getMoveX() * moveScale),
            this.imageSpaceRect.getY() + (key.getMoveY() * moveScale),
            this.imageSpaceRect.getWidth(),
            this.imageSpaceRect.getHeight()
        );

        compSpaceRect = ic.fromImageToComponentSpace(imageSpaceRect);
        handles.updateRect(compSpaceRect);
        ic.repaint();
    }
}

