/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import pixelitor.ImageComponent;
import pixelitor.ImageDisplay;
import pixelitor.tools.ArrowKey;
import pixelitor.utils.Utils;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * Helps with interactive manipulation of selections/transforms
 */
public class TransformSupport {
    private final Handles handles;
    private Rectangle compSpaceRect;
    private Rectangle imageSpaceRect;
    private int dragStartX;
    private int dragStartY;
    private int dragStartRectWidth;
    private int dragStartRectHeight;

    // true while the user is adjusting the handles
    private boolean adjusting;

    public TransformSupport(Rectangle compSpaceRectangle, Rectangle imageSpaceRect) {
        this.compSpaceRect = compSpaceRectangle;
        this.imageSpaceRect = imageSpaceRect;
        handles = new Handles(compSpaceRectangle);
    }

    public void paintHandles(Graphics2D g) {
        handles.paint(g);
    }

    public void mousePressed(MouseEvent e) {
        dragStartX = e.getX();
        dragStartY = e.getY();
        dragStartRectWidth = (int) compSpaceRect.getWidth();
        dragStartRectHeight = (int) compSpaceRect.getHeight();
    }

    public void mouseDragged(MouseEvent e, ImageDisplay ic) {
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
                return;
        }
        adjusting = true;
        handles.updateRect(compSpaceRect);
    }

    public void mouseReleased() {
        adjusting = false;
    }

    public void mouseMoved(MouseEvent e, ImageDisplay ic) {
        handles.setCursorForPoint(e.getX(), e.getY(), ic);
    }

    public Rectangle getImageSpaceRectangle(ImageDisplay ic) {
        if(adjusting) {
            recalculateImageSpaceRect(ic);
        }
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
     * @param lastCropRectangle - the last crop rectangle in component space
     */
    public void setComponentSpaceRect(Rectangle compSpaceRect) {
        this.compSpaceRect = compSpaceRect;
        handles.updateRect(compSpaceRect);
    }

    private void recalculateImageSpaceRect(ImageDisplay ic) {
        imageSpaceRect = Utils.toPositiveRectangle(ic.fromComponentToImageSpace(compSpaceRect));
    }

    public void arrowKeyPressed(ArrowKey key, ImageComponent ic) {
        compSpaceRect.translate(key.getMoveX(), key.getMoveY());
        handles.updateRect(compSpaceRect);
        recalculateImageSpaceRect(ic);
        ic.repaint();
    }
}

