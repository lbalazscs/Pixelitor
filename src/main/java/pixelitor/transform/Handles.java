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
import pixelitor.utils.Utils;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.List;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;

/**
 * The eight handles of the TransformSupport
 */
public class Handles {
    private final Handle upperLeft = new Handle(Cursor.NW_RESIZE_CURSOR);
    private final Handle upper = new Handle(Cursor.N_RESIZE_CURSOR);
    private final Handle upperRight = new Handle(Cursor.NE_RESIZE_CURSOR);
    private final Handle right = new Handle(Cursor.E_RESIZE_CURSOR);
    private final Handle left = new Handle(Cursor.W_RESIZE_CURSOR);
    private final Handle lowerLeft = new Handle(Cursor.SW_RESIZE_CURSOR);
    private final Handle lower = new Handle(Cursor.S_RESIZE_CURSOR);
    private final Handle lowerRight = new Handle(Cursor.SE_RESIZE_CURSOR);

    private final List<Handle> handles = Arrays.asList(upperLeft, upperRight, lowerRight, lowerLeft,
            right, upper, lower, left);

    private final Stroke bigStroke;
    private final Stroke smallStroke;

    public Handles(Rectangle compSpaceRect) {
        updateRect(compSpaceRect);
        bigStroke = new BasicStroke(3);
        smallStroke = new BasicStroke(1);
    }

    public void paint(Graphics2D g) {
        drawRect(g);
        drawHandles(g);
    }

    private void drawRect(Graphics2D g) {
        Rectangle rect = getSelectedRectInComponentSpace();

        g.setColor(BLACK);
        g.setStroke(bigStroke);
        g.draw(rect);
        g.setColor(WHITE);
        g.setStroke(smallStroke);
        g.draw(rect);
    }

    private void drawHandles(Graphics2D g) {
        for (Handle handle : handles) {
            g.setStroke(bigStroke);
            g.setColor(BLACK);
            g.draw(handle.getShape());
            g.setStroke(smallStroke);
            g.setColor(WHITE);
            g.fill(handle.getShape());
        }
    }

    private Rectangle getSelectedRectInComponentSpace() {
        int upperLeftX = upperLeft.getX();
        int upperRightX = upperRight.getX();
        int upperLeftY = upperLeft.getY();
        int lowerLeftY = lowerLeft.getY();

        return Utils.toPositiveRect(upperLeftX, upperRightX, upperLeftY, lowerLeftY);
    }

    /**
     * Iterates over all the handles and if finds one that is over the point, its cursor is set
     * The coordinates are in image space
     */
    public void setCursorForPoint(int x, int y, ImageComponent c) {
        boolean handleFound = false;
        for (Handle handle : handles) {
            if(handle.isOver(x, y)) {
                c.setCursor(handle.getCursor());
                handleFound = true;
                break;
            }
        }
        if(!handleFound) {
            c.setCursor(Cursor.getDefaultCursor());
        }
    }

    // here rect must be given in component-space ("mouse") coordinates
    public void updateRect(Rectangle compSpaceRect) {
        int horizontalMidPointX = compSpaceRect.x + (compSpaceRect.width / 2);
        int verticalMidPointY = compSpaceRect.y + (compSpaceRect.height / 2);
        int horizontalEndPointX = compSpaceRect.x + compSpaceRect.width;
        int verticalEndPointY = compSpaceRect.y + compSpaceRect.height;
        upperLeft.setPosition(compSpaceRect.x, compSpaceRect.y);
        upper.setPosition(horizontalMidPointX, compSpaceRect.y);
        upperRight.setPosition(horizontalEndPointX, compSpaceRect.y);
        right.setPosition(horizontalEndPointX, verticalMidPointY);
        left.setPosition(compSpaceRect.x, verticalMidPointY);
        lowerLeft.setPosition(compSpaceRect.x, verticalEndPointY);
        lower.setPosition(horizontalMidPointX, verticalEndPointY);
        lowerRight.setPosition(horizontalEndPointX, verticalEndPointY);
    }

    @Override
    public String toString() {
        return "Handles{" +
                "upperLeft=" + upperLeft +
                ", upper=" + upper +
                ", upperRight=" + upperRight +
                ", right=" + right +
                ", left=" + left +
                ", lowerLeft=" + lowerLeft +
                ", lower=" + lower +
                ", lowerRight=" + lowerRight +
                '}';
    }
}
