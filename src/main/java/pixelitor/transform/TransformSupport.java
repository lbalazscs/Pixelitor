/*
 * Copyright 2009-2014 Laszlo Balazs-Csiki
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor.  If not, see <http://www.gnu.org/licenses/>.
 */
package pixelitor.transform;

import pixelitor.ImageComponent;
import pixelitor.menus.view.ZoomLevel;
import pixelitor.utils.Utils;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * Helps with interactive manipulation of selections/transforms
 */
public class TransformSupport {
    private Handles handles;
    private Rectangle rect;
    private TransformToolChangeListener changeListener;
    private int dragStartX;
    private int dragStartY;
    private int dragStartRectWidth;
    private int dragStartRectHeight;

    // compSpaceRectangle must be given in component-space ("mouse") coordinates
    public TransformSupport(Rectangle compSpaceRectangle, TransformToolChangeListener changeListener) {
        this.rect = compSpaceRectangle;
        this.changeListener = changeListener;
        handles = new Handles(compSpaceRectangle);
    }

    public void paintHandles(Graphics2D g, ZoomLevel zoomLevel) {
        handles.paint(g, zoomLevel);
    }

    public void mousePressed(MouseEvent e, ImageComponent ic) {
        dragStartX = e.getX();
        dragStartY = e.getY();
        dragStartRectWidth = (int) rect.getWidth();
        dragStartRectHeight = (int) rect.getHeight();
    }

    public void mouseDragged(MouseEvent e, ImageComponent ic) {
        int cursorType = ic.getCursor().getType();
        int mouseX = e.getX();
        int mouseY = e.getY();
        switch(cursorType) {
            case Cursor.NW_RESIZE_CURSOR:
                rect.setLocation(mouseX, mouseY);
                rect.setSize(dragStartRectWidth + (dragStartX - mouseX), dragStartRectHeight + (dragStartY) - mouseY);
                break;
            case Cursor.SE_RESIZE_CURSOR:
                rect.setSize(dragStartRectWidth + (mouseX - dragStartX), dragStartRectHeight + (mouseY - dragStartY));
                break;
            case Cursor.SW_RESIZE_CURSOR:
                rect.setLocation(mouseX, rect.getLocation().y);
                rect.setSize(dragStartRectWidth + (dragStartX - mouseX), dragStartRectHeight + (mouseY - dragStartY));
                break;
            case Cursor.NE_RESIZE_CURSOR:
                rect.setLocation(rect.getLocation().x, mouseY);
                rect.setSize(dragStartRectWidth + (mouseX - dragStartX), dragStartRectHeight + (dragStartY - mouseY));
                break;
            case Cursor.N_RESIZE_CURSOR:
                rect.setLocation(rect.getLocation().x, mouseY);
                rect.setSize(rect.width, dragStartRectHeight + (dragStartY - mouseY));
                break;
            case Cursor.S_RESIZE_CURSOR:
                rect.setSize(dragStartRectWidth, dragStartRectHeight + (mouseY - dragStartY));
                break;
            case Cursor.E_RESIZE_CURSOR:
                rect.setSize(dragStartRectWidth + (mouseX - dragStartX), rect.height);
                break;
            case Cursor.W_RESIZE_CURSOR:
                rect.setLocation(mouseX, rect.y);
                rect.setSize(dragStartRectWidth + (dragStartX - mouseX), rect.height);
                break;
            default:
                return;
        }
        handles.updateRect(rect);
        changeListener.transformToolChangeHappened();
    }

    public void mouseMoved(MouseEvent e, ImageComponent ic) {
        handles.setCursorForPoint(e.getX(), e.getY(), ic);
    }

    public Rectangle getRectangle(ImageComponent ic) {
//        Rectangle imageSpaceRect = zoomLevel.fromComponentSpaceToImage(rect);
        Rectangle imageSpaceRect = ic.fromComponentToImageSpace(rect);
        return Utils.toPositiveRectangle(imageSpaceRect);
    }

    @Override
    public String toString() {
        return "TransformSupport{" +
                "handles=" + handles +
                ", rect=" + rect +
                ", changeListener=" + changeListener +
                ", dragStartX=" + dragStartX +
                ", dragStartY=" + dragStartY +
                ", dragStartRectWidth=" + dragStartRectWidth +
                ", dragStartRectHeight=" + dragStartRectHeight +
                '}';
    }
}

