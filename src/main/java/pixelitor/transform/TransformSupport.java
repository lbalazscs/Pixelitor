/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.utils.Utils;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

/**
 * Helps with interactive manipulation of selections/transforms
 */
public class TransformSupport {
    private final Handles handles;
    private Rectangle compSpaceRect;
    private Rectangle2D imageSpaceRect;
    private int dragStartX;
    private int dragStartY;
    private int dragStartRectWidth;
    private int dragStartRectHeight;
    private Point dragStartLocation;

    // true while the user is adjusting the handles
    private boolean adjusting;

    // true if user can relocate selected area
    private boolean canRelocate;

    public TransformSupport(Rectangle compSpaceRect, Rectangle2D imageSpaceRect) {
        this.compSpaceRect = compSpaceRect;
        this.imageSpaceRect = imageSpaceRect;
        handles = new Handles(compSpaceRect);
    }

    public void paintHandles(Graphics2D g) {
        handles.paint(g);
    }

    public void mousePressed(MouseEvent e) {
        dragStartX = e.getX();
        dragStartY = e.getY();
        dragStartRectWidth = (int) compSpaceRect.getWidth();
        dragStartRectHeight = (int) compSpaceRect.getHeight();
        dragStartLocation = compSpaceRect.getLocation();

        // if user clicked inside selection allow relocate it
        if (compSpaceRect.contains(e.getPoint())) {
            canRelocate = true;
        }
    }

    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        int cursorType = ic.getCursor().getType();
        int mouseX = e.getX();
        int mouseY = e.getY();
        switch (cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
                compSpaceRect.setLocation(mouseX, mouseY);
                compSpaceRect.setSize(dragStartRectWidth + (dragStartX - mouseX), dragStartRectHeight + (dragStartY) - mouseY);
                break;
            case Cursor.SE_RESIZE_CURSOR:
                compSpaceRect.setSize(dragStartRectWidth + (mouseX - dragStartX), dragStartRectHeight + (mouseY - dragStartY));
                break;
            case Cursor.SW_RESIZE_CURSOR:
                compSpaceRect.setLocation(mouseX, compSpaceRect.getLocation().y);
                compSpaceRect.setSize(dragStartRectWidth + (dragStartX - mouseX), dragStartRectHeight + (mouseY - dragStartY));
                break;
            case Cursor.NE_RESIZE_CURSOR:
                compSpaceRect.setLocation(compSpaceRect.getLocation().x, mouseY);
                compSpaceRect.setSize(dragStartRectWidth + (mouseX - dragStartX), dragStartRectHeight + (dragStartY - mouseY));
                break;
            case Cursor.N_RESIZE_CURSOR:
                compSpaceRect.setLocation(compSpaceRect.getLocation().x, mouseY);
                compSpaceRect.setSize(compSpaceRect.width, dragStartRectHeight + (dragStartY - mouseY));
                break;
            case Cursor.S_RESIZE_CURSOR:
                compSpaceRect.setSize(dragStartRectWidth, dragStartRectHeight + (mouseY - dragStartY));
                break;
            case Cursor.E_RESIZE_CURSOR:
                compSpaceRect.setSize(dragStartRectWidth + (mouseX - dragStartX), compSpaceRect.height);
                break;
            case Cursor.W_RESIZE_CURSOR:
                compSpaceRect.setLocation(mouseX, compSpaceRect.y);
                compSpaceRect.setSize(dragStartRectWidth + (dragStartX - mouseX), compSpaceRect.height);
                break;
            default:
                if (canRelocate) {
                    compSpaceRect.setLocation(
                        (dragStartLocation.x - (dragStartX - mouseX)),
                        (dragStartLocation.y - (dragStartY - mouseY))
                    );
                } else {
                    return;
                }
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
        canRelocate = false;
    }

    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        handles.setCursorForPoint(e.getX(), e.getY(), ic);
    }

    public Rectangle2D getImageSpaceRect() {
        return imageSpaceRect;
    }

    @Override
    public String toString() {
        return "TransformSupport{" +
                "handles=" + handles +
                ", compSpaceRect=" + compSpaceRect +
                ", dragStartX=" + dragStartX +
                ", dragStartY=" + dragStartY +
                ", dragStartRectWidth=" + dragStartRectWidth +
                ", dragStartRectHeight=" + dragStartRectHeight +
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

        double viewScale = ic.getZoomLevel().getViewScale();
        this.compSpaceRect.setSize((int) (width * viewScale), (int) (height * viewScale));
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
        double viewScale = ic.getZoomLevel().getViewScale();
        compSpaceRect.translate((int) (key.getMoveX() * viewScale), (int) (key.getMoveY() * viewScale));
        handles.updateRect(compSpaceRect);
        recalculateImageSpaceRect(ic);
        ic.repaint();
    }
}

