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

package pixelitor.tools.util;

import pixelitor.gui.ImageComponent;
import pixelitor.gui.ImageComponents;
import pixelitor.tools.DragTool;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * Represents the mouse drag on the image made
 * by the user while using a {@link DragTool}.
 * Only the start and end points are relevant.
 */
public class UserDrag {
    private boolean dragging;

    // The coordinates in the component (mouse) space.
    private int coStartX;
    private int coEndX;
    private int coStartY;
    private int coEndY;

    // The coordinates in the image space.
    private double imStartX;
    private double imStartY;
    private double imEndX;
    private double imEndY;

    private int prevCoEndX;
    private int prevCoEndY;

    private boolean startAdjusted = false;

    private ImageComponent ic;

    private boolean constrainPoints = false;
    private boolean startFromCenter = false;
    private static final int MOUSE_DISPLAY_DISTANCE = 10;

    public UserDrag() {
    }

    public void setStart(PMouseEvent e) {
        assert e.getIC() != null;
        assert ImageComponents.isActive(e.getIC());

        this.ic = e.getIC();

        coStartX = e.getCoX();
        coStartY = e.getCoY();
        imStartX = ic.componentXToImageSpace(coStartX);
        imStartY = ic.componentYToImageSpace(coStartY);
    }

    public void setEnd(PMouseEvent e) {
        if (this.ic != e.getIC()) {
            // in some exceptional situations it can happen that the
            // ic changes without a mousePressed event, so simulate one
            setStart(e);
        }

        coEndX = e.getCoX();
        coEndY = e.getCoY();

        if (constrainPoints) {
            Point2D constrainedEnd = Utils.constrainEndPoint(coStartX, coStartY, coEndX, coEndY);
            coEndX = (int) constrainedEnd.getX();
            coEndY = (int) constrainedEnd.getY();
        }

        imEndX = ic.componentXToImageSpace(coEndX);
        imEndY = ic.componentYToImageSpace(coEndY);

        dragging = true;
        startAdjusted = false;
    }

    // returns the start x coordinate in component space
    public int getCoStartX() {
        return coStartX;
    }

    // returns the start y coordinate in component space
    public int getCoStartY() {
        return coStartY;
    }

    // returns the end x coordinate in component space
    public int getCoEndX() {
        return coEndX;
    }

    // returns the end y coordinate in component space
    public int getCoEndY() {
        return coEndY;
    }

    public boolean isClick() {
        return ((coStartX == coEndX) && (coStartY == coEndY));
    }

    public ImDrag toImDrag() {
        ImDrag d = new ImDrag(imStartX, imStartY, imEndX, imEndY);
        d.setStartFromCenter(startFromCenter);
        return d;
    }

    public void setConstrainPoints(boolean constrainPoints) {
        this.constrainPoints = constrainPoints;
    }

    public void drawGradientArrow(Graphics2D g) {
        Shapes.drawGradientArrow(g, coStartX, coStartY, coEndX, coEndY);
    }

    public void saveEndValues() {
        prevCoEndX = coEndX;
        prevCoEndY = coEndY;
    }

    public void adjustStartForSpaceDownDrag() {
        int dx = coEndX - prevCoEndX;
        int dy = coEndY - prevCoEndY;

        coStartX += dx;
        coStartY += dy;

        imStartX = ic.componentXToImageSpace(coStartX);
        imStartY = ic.componentYToImageSpace(coStartY);

        startAdjusted = true;
    }

    public void setStartFromCenter(boolean startFromCenter) {
        this.startFromCenter = startFromCenter;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void mouseReleased() {
        this.dragging = false;
    }

    public Rectangle toCoRect() {
        return new Rectangle(coStartX, coStartY, coEndX - coStartX, coEndY - coStartY);
    }

    public PRectangle toPosPRect() {
        return PRectangle.positiveFromCo(toCoRect(), ic);
    }

    public double calcImDist() {
        double dx = imEndX - imStartX;
        double dy = imEndY - imStartY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double calcIntuitiveAngle() {
        double angle = Math.atan2(coEndY - coStartY, coEndX - coStartX);
        return Utils.atan2AngleToIntuitive(angle);
    }

    public void displayWidthHeight(Graphics2D g) {
        int imWidth = (int) (imEndX - imStartX);
        int imHeight = (int) (imEndY - imStartY);
        String widthInfo = "\u2194 = " + Math.abs(imWidth) + " px";
        String heightInfo = "\u2195 = " + Math.abs(imHeight) + " px";

        DragDisplay dd = new DragDisplay(g);

        int widthY;
        if (imHeight >= 0) {
            // display the width info bellow the mouse
            widthY = coEndY + MOUSE_DISPLAY_DISTANCE + DragDisplay.ONE_LINER_BG_HEIGHT;
        } else {
            // display the width info above the mouse
            widthY = coEndY - MOUSE_DISPLAY_DISTANCE;
        }
        int widthX = coStartX + (coEndX - coStartX) / 2 - DragDisplay.BG_WIDTH / 2;
        dd.drawOneLine(widthInfo, widthX, widthY);

        int heightX;
        if (imWidth >= 0) {
            // display the height info on the right side of the mouse
            heightX = coEndX + MOUSE_DISPLAY_DISTANCE;
        } else {
            // display the height info on the left side of the mouse
            heightX = coEndX - DragDisplay.BG_WIDTH - MOUSE_DISPLAY_DISTANCE;
        }
        int heightY = coStartY + (coEndY - coStartY) / 2 + DragDisplay.ONE_LINER_BG_HEIGHT / 2;
        dd.drawOneLine(heightInfo, heightX, heightY);

        if (startAdjusted) {
            String xInfo = "x = " + (int) imStartX + " px";
            String yInfo = "y = " + (int) imStartY + " px";
            int startInfoX;
            // can be smaller because of the rounded rectangle
            // and because it is at a distance in both dimensions
            int mouseDist = MOUSE_DISPLAY_DISTANCE / 2;
            if (imWidth >= 0) {
                // display the start info to the left of the start
                startInfoX = coStartX - DragDisplay.BG_WIDTH - mouseDist;
            } else {
                // display the start info to the right of the start
                startInfoX = coStartX + mouseDist;
            }

            int startInfoY;
            if (imHeight >= 0) {
                // display the start info info above the start
                startInfoY = this.coStartY - mouseDist;
            } else {
                // display the start info info bellow the start
                startInfoY = this.coStartY + mouseDist + DragDisplay.TWO_LINER_BG_HEIGHT;
            }

            dd.drawTwoLines(xInfo, yInfo, startInfoX, startInfoY);
        }

        dd.finish();
    }

    public void displayRelativeMovement(Graphics2D g) {
        int dx = (int) (imEndX - imStartX);
        int dy = (int) (imEndY - imStartY);
        String dxString;
        if (dx >= 0) {
            dxString = "\u2192 = " + dx + " px";
        } else {
            dxString = "\u2190 = " + (-dx) + " px";
        }
        String dyString;
        if (dy >= 0) {
            dyString = "\u2193 = " + dy + " px";
        } else {
            dyString = "\u2191 = " + (-dy) + " px";
        }

        DragDisplay dd = new DragDisplay(g);
        int x = coEndX + 30;
        int y = coEndY - 20;

        dd.drawTwoLines(dxString, dyString, x, y);

        dd.finish();
    }

    public void displayAngle(Graphics2D g) {
        DragDisplay dd = new DragDisplay(g);

        int dragAngle = (int) Math.toDegrees(calcIntuitiveAngle());
        String angleInfo = "\u2221 = " + dragAngle + " \u00b0";

        int dragDistance = (int) calcImDist();
        String distInfo = "d = " + dragDistance + " px";

        int coDx = coEndX - coStartX;
        int coDy = coEndY - coStartY;

        int x;
        boolean xDistIsSmall = false;
        if (coDx >= DragDisplay.BG_WIDTH) {
            // display it on the right side of the mouse
            x = coEndX + MOUSE_DISPLAY_DISTANCE;
        } else if (coDx <= -DragDisplay.BG_WIDTH) {
            // display it on the left side of the mouse
            x = coEndX - MOUSE_DISPLAY_DISTANCE - DragDisplay.BG_WIDTH;
        } else {
            xDistIsSmall = true;
            // display it so that it has no sudden jumps
            x = coEndX - DragDisplay.BG_WIDTH / 2
                    + (int) ((DragDisplay.BG_WIDTH / 2 + MOUSE_DISPLAY_DISTANCE)
                    * coDx / (double) DragDisplay.BG_WIDTH);
        }
        int y;
        int yInterpolationLimit = DragDisplay.TWO_LINER_BG_HEIGHT;
        if (xDistIsSmall) {
            // if the x distance is small, don't try to smoothly interpolate
            // the y position, because the drag display might cover the shape
            yInterpolationLimit = 0;
        }
        if (coDy <= -yInterpolationLimit) {
            // display it above the mouse
            y = coEndY - MOUSE_DISPLAY_DISTANCE;
        } else if (coDy >= yInterpolationLimit) {
            // display it bellow the mouse
            y = coEndY + MOUSE_DISPLAY_DISTANCE + DragDisplay.TWO_LINER_BG_HEIGHT;
        } else {
            // display it so that it has no sudden jumps
            y = coEndY + DragDisplay.TWO_LINER_BG_HEIGHT / 2
                    + (int) ((DragDisplay.TWO_LINER_BG_HEIGHT / 2 + MOUSE_DISPLAY_DISTANCE)
                    * coDy / (double) DragDisplay.TWO_LINER_BG_HEIGHT);
        }
        dd.drawTwoLines(angleInfo, distInfo, x, y);

        dd.finish();
    }
}
