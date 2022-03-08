/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.tools.DragTool;
import pixelitor.utils.Rnd;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.io.Serializable;

import static java.lang.String.format;
import static pixelitor.tools.util.DragDisplay.MOUSE_DISPLAY_DISTANCE;

/**
 * Represents the mouse drag on the image made
 * by the user while using a {@link DragTool}.
 * Only the start and end points are relevant.
 */
public class Drag implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // The coordinates in the image space.
    private double imStartX;
    private double imStartY;
    private double imEndX;
    private double imEndY;

    // transient variables from here

    // The coordinates in the component (mouse) space.
    private transient double coStartX;
    private transient double coEndX;
    private transient double coStartY;
    private transient double coEndY;
    private transient boolean hasCoCoords;

    private transient double prevCoEndX;
    private transient double prevCoEndY;

    private transient boolean dragging;
    private transient boolean canceled;
    private transient boolean startAdjusted;
    private transient boolean constrained;
    private transient boolean startFromCenter;
    private transient boolean equallySized;

    public Drag() {
        hasCoCoords = false;
    }

    public Drag(double imStartX, double imStartY, double imEndX, double imEndY) {
        this.imStartX = imStartX;
        this.imStartY = imStartY;
        this.imEndX = imEndX;
        this.imEndY = imEndY;

        hasCoCoords = false;
    }

    public Drag(PPoint start, PPoint end) {
        imStartX = start.getImX();
        imStartY = start.getImY();
        imEndX = end.getImX();
        imEndY = end.getImY();

        hasCoCoords = false;
    }

    public Drag(Rectangle2D r) {
        imStartX = r.getX();
        imStartY = r.getY();
        imEndX = imStartX + r.getWidth();
        imEndY = imStartY + r.getHeight();

        hasCoCoords = false;
    }

    public Drag copy() {
        return new Drag(imStartX, imStartY, imEndX, imEndY);
    }

    public static Drag createRandom(int width, int height, int minDist) {
        int minDist2 = minDist * minDist;
        Drag drag;

        while (true) {
            int x1 = Rnd.intInRange(-width, 2 * width);
            int x2 = Rnd.intInRange(-width, 2 * width);
            int y1 = Rnd.intInRange(-height, 2 * height);
            int y2 = Rnd.intInRange(-height, 2 * height);

            int dx = x2 - x1;
            int dy = y2 - y1;
            if (dx * dx + dy * dy > minDist2) {
                drag = new Drag(x1, y1, x2, y2);
                break;
            }
        }
        return drag;
    }

    public Drag imTransformedCopy(AffineTransform at) {
        Point2D start = new Point2D.Double(imStartX, imStartY);
        Point2D end = new Point2D.Double(imEndX, imEndY);
        at.transform(start, start);
        at.transform(end, end);
        return new Drag(start.getX(), start.getY(), end.getX(), end.getY());
    }

    public Drag translatedCopy(double tx, double ty) {
        return new Drag(
            imStartX + tx, imStartY + ty,
            imEndX + tx, imEndY + ty);
    }

    public void setStart(PPoint e) {
        assert e.getView() != null;

        View view = e.getView();

        coStartX = e.getCoX();
        coStartY = e.getCoY();
        imStartX = view.componentXToImageSpace(coStartX);
        imStartY = view.componentYToImageSpace(coStartY);

        hasCoCoords = true;
    }

    public void setEnd(PPoint e) {
        coEndX = e.getCoX();
        coEndY = e.getCoY();

        if (constrained) {
            Point2D newEnd = Utils.constrainEndPoint(coStartX, coStartY, coEndX, coEndY);
            coEndX = newEnd.getX();
            coEndY = newEnd.getY();
        } else if (equallySized) { // the two special cases are not used at the same time
            double width = Math.abs(coEndX - coStartX);
            double height = Math.abs(coEndY - coStartY);
            double max = Math.max(width, height);
            if (coEndX > coStartX) {
                coEndX = coStartX + max;
            } else {
                coEndX = coStartX - max;
            }
            if (coEndY > coStartY) {
                coEndY = coStartY + max;
            } else {
                coEndY = coStartY - max;
            }
        }

        View view = e.getView();
        imEndX = view.componentXToImageSpace(coEndX);
        imEndY = view.componentYToImageSpace(coEndY);

        dragging = true;
        startAdjusted = false;
        hasCoCoords = true;
    }

    public PPoint getStart(View view) {
        assert hasCoCoords;
        return PPoint.from(coStartX, coStartY, imStartX, imStartY, view);
    }

    public PPoint getEnd(View view) {
        assert hasCoCoords;
        return PPoint.from(coEndX, coEndY, imEndX, imEndY, view);
    }

    // returns the start x coordinate in component space
    public double getCoStartX(boolean centerAdjust) {
        assert hasCoCoords;
        if (centerAdjust && startFromCenter) {
            return coStartX - (coEndX - coStartX);
        } else {
            return coStartX;
        }
    }

    // returns the start y coordinate in component space
    public double getCoStartY(boolean centerAdjust) {
        assert hasCoCoords;
        if (centerAdjust && startFromCenter) {
            return coStartY - (coEndY - coStartY);
        } else {
            return coStartY;
        }
    }

    // returns the end x coordinate in component space
    public double getCoEndX() {
        assert hasCoCoords;
        return coEndX;
    }

    // returns the end y coordinate in component space
    public double getCoEndY() {
        assert hasCoCoords;
        return coEndY;
    }

    public double getStartX() {
        return imStartX;
    }

    public double getStartY() {
        return imStartY;
    }

    public double getStartXFromCenter() {
        if (startFromCenter) {
            return imStartX - (imEndX - imStartX);
        } else {
            return imStartX;
        }
    }

    public double getStartYFromCenter() {
        if (startFromCenter) {
            return imStartY - (imEndY - imStartY);
        } else {
            return imStartY;
        }
    }

    public double getEndX() {
        return imEndX;
    }

    public double getEndY() {
        return imEndY;
    }

    public Point2D getStartPoint() {
        return new Point2D.Double(imStartX, imStartY);
    }

    public Point2D getEndPoint() {
        return new Point2D.Double(imEndX, imEndY);
    }

    public Point2D getCenterPoint() {
        double cx = (imStartX + imEndX) / 2.0;
        double cy = (imStartY + imEndY) / 2.0;

        return new Point2D.Double(cx, cy);
    }

    public Drag getCenterDrag() {
        Point2D center = getCenterPoint();
        return new Drag(center.getX(), center.getY(), getEndX(), getEndY());
    }

    public double getDX() {
        return imEndX - imStartX;
    }

    public double getDY() {
        return imEndY - imStartY;
    }

    /**
     * Return the horizontal line that runs through the center in image space
     */
    public Drag getCenterHorizontalDrag() {
        double centerY;
        if (startFromCenter) {
            centerY = imStartY;
            return new Drag(imStartX - getDX(), centerY, imEndX, centerY);
        } else {
            centerY = imStartY + getDY() / 2.0;
            return new Drag(imStartX, centerY, imEndX, centerY);
        }
    }

    public boolean isClick() {
        assert hasCoCoords;
        return coStartX == coEndX && coStartY == coEndY;
    }

    public boolean isImClick() {
        return imStartX == imEndX && imStartY == imEndY;
    }

    public boolean hasZeroWidth() {
        assert hasCoCoords;
        return coStartX == coEndX;
    }

    public boolean hasZeroHeight() {
        assert hasCoCoords;
        return coStartY == coEndY;
    }

    public boolean hasZeroImWidth() {
        return imStartX == imEndX;
    }

    public boolean hasZeroImHeight() {
        return imStartY == imEndY;
    }

    public void setConstrained(boolean constrained) {
        this.constrained = constrained;
    }

    /**
     * Draws an arrow corresponding to this drag on the given component-space graphics.
     */
    public void drawCoDirectionArrow(Graphics2D g) {
        assert hasCoCoords;
        Shapes.drawDirectionArrow(g, coStartX, coStartY, coEndX, coEndY);
    }

    /**
     * Draws an arrow corresponding to this drag on the given image-space graphics.
     * Arrows should be drawn in component space in order to be zoom-independent.
     * However, this is good enough for debugging.
     */
    public void drawImDirectionArrow(Graphics2D g) {
        Shapes.drawDirectionArrow(g, imStartX, imStartY, imEndX, imEndY);
    }

    public void saveEndValues() {
        prevCoEndX = coEndX;
        prevCoEndY = coEndY;
    }

    public void adjustStartForSpaceDownDrag(View view) {
        assert hasCoCoords;
        double dx = coEndX - prevCoEndX;
        double dy = coEndY - prevCoEndY;

        coStartX += dx;
        coStartY += dy;

        imStartX = view.componentXToImageSpace(coStartX);
        imStartY = view.componentYToImageSpace(coStartY);

        startAdjusted = true;
    }

    public void setStartFromCenter(boolean startFromCenter) {
        this.startFromCenter = startFromCenter;
    }

    public boolean isStartFromCenter() {
        return startFromCenter;
    }

    public void setEquallySized(boolean equallySized) {
        this.equallySized = equallySized;
    }

    public Line2D asLine() {
        return new Line2D.Double(imStartX, imStartY, imEndX, imEndY);
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        canceled = true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public Rectangle toCoRect() {
        assert hasCoCoords;
        int x;
        int y;
        int width;
        int height;

        if (startFromCenter) {
            double halfWidth = coEndX - coStartX; // can be negative
            double halfHeight = coEndY - coStartY; // can be negative

            x = (int) (coStartX - halfWidth);
            y = (int) (coStartY - halfHeight);
            width = (int) (2 * halfWidth);
            height = (int) (2 * halfHeight);
        } else {
            x = (int) coStartX;
            y = (int) coStartY;
            width = (int) (coEndX - coStartX);
            height = (int) (coEndY - coStartY);
        }

        return new Rectangle(x, y, width, height);
    }

    public Rectangle2D toImRect() {
        double x;
        double y;
        double width;
        double height;

        if (startFromCenter) {
            double halfWidth = imEndX - imStartX; // can be negative
            double halfHeight = imEndY - imStartY; // can be negative

            x = imStartX - halfWidth;
            y = imStartY - halfHeight;
            width = 2 * halfWidth;
            height = 2 * halfHeight;
        } else {
            x = imStartX;
            y = imStartY;
            width = imEndX - imStartX;
            height = imEndY - imStartY;
        }

        return new Rectangle2D.Double(x, y, width, height);
    }

    public Rectangle toPosCoRect() {
        return Shapes.toPositiveRect(toCoRect());
    }

    public Rectangle2D toPosImRect() {
        return Shapes.toPositiveRect(toImRect());
    }

    public PRectangle toPosPRect(View view) {
        return PRectangle.positiveFromCo(toCoRect(), view);
    }


    /**
     * Creates a Rectangle where the sign of with/height indicate the direction of drawing
     *
     * @return a Rectangle where the width and height can be < 0
     */
    public Rectangle2D createPossiblyEmptyImRect() {
        double x;
        double y;
        double width;
        double height;

        if (startFromCenter) {
            double halfWidth = imEndX - imStartX; // can be negative
            double halfHeight = imEndY - imStartY; // can be negative

            x = imStartX - halfWidth;
            y = imStartY - halfHeight;

            width = 2 * halfWidth;
            height = 2 * halfHeight;
        } else {
            x = imStartX;
            y = imStartY;
            width = imEndX - imStartX;
            height = imEndY - imStartY;
        }

        return new Rectangle2D.Double(x, y, width, height);
    }

    /**
     * Creates a Rectangle where the width/height are >=0 independently of the direction of the drawing
     *
     * @return a Rectangle where the width and height are >= 0
     */
    public Rectangle2D createPositiveImRect() {
        double x;
        double y;
        double width;
        double height;

        if (startFromCenter) {
            double halfWidth;  // positive or zero
            if (imEndX > imStartX) {
                halfWidth = imEndX - imStartX;
                x = imStartX - halfWidth;
            } else {
                halfWidth = imStartX - imEndX;
                x = imEndX;
            }

            double halfHeight; // positive or zero
            if (imEndY > imStartY) {
                halfHeight = imEndY - imStartY;
                y = imStartY - halfHeight;
            } else {
                halfHeight = imStartY - imEndY;
                y = imEndY;
            }

            width = 2 * halfWidth;
            height = 2 * halfHeight;
        } else {
            double tmpEndX;
            if (imEndX > imStartX) {
                x = imStartX;
                tmpEndX = imEndX;
            } else {
                x = imEndX;
                tmpEndX = imStartX;
            }

            double tmpEndY;
            if (imEndY > imStartY) {
                y = imStartY;
                tmpEndY = imEndY;
            } else {
                y = imEndY;
                tmpEndY = imStartY;
            }

            width = tmpEndX - x;
            height = tmpEndY - y;
        }
        return new Rectangle2D.Double(x, y, width, height);
    }

    public double calcCoDist() {
        assert hasCoCoords;
        double dx = coEndX - coStartX;
        double dy = coEndY - coStartY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (startFromCenter) {
            dist *= 2;
        }
        return dist;
    }

    public double calcImDist() {
        double dx = imEndX - imStartX;
        double dy = imEndY - imStartY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (startFromCenter) {
            dist *= 2.0;
        }
        return dist;
    }

    public double taxiCabMetric(int x, int y) {
        return Math.abs(x - imStartX) + Math.abs(y - imStartY);
    }

    public double getStartDistanceFrom(double x, double y) {
        double dx = imStartX - x;
        double dy = imStartY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double calcIntuitiveAngle() {
        double angle = calcAngle();
        return Utils.atan2AngleToIntuitive(angle);
    }

    public double calcAngle() {
        return Math.atan2(imEndY - imStartY, imEndX - imStartX);
    }

    public double calcAngleBetween(Drag other) {
        // TODO there should be a faster formula
        return other.calcAngle() - calcAngle();
    }

    protected double calcReversedAngle() {
        assert hasCoCoords;
        return Math.atan2(coStartY - coEndY, coStartX - coEndX);
    }

    public double getDrawAngle() {
        return Math.atan2(imEndX - imStartX, imEndY - imStartY); //  between -PI and PI
    }

    public double getAngleFromStartTo(double x, double y) {
        return Math.atan2(x - imStartX, y - imStartY);
    }

    public void displayWidthHeight(Graphics2D g) {
        assert hasCoCoords;
        double imWidth = imEndX - imStartX;
        double imHeight = imEndY - imStartY;
        String widthInfo = DragDisplay.getWidthDisplayString(imWidth);
        String heightInfo = DragDisplay.getHeightDisplayString(imHeight);

        int displayBgWidth = DragDisplay.BG_WIDTH_PIXEL;
        DragDisplay dd = new DragDisplay(g, displayBgWidth);

        // draw the width display
        float widthY;
        if (imHeight >= 0) {
            // display the width info bellow the mouse
            widthY = (float) (coEndY + MOUSE_DISPLAY_DISTANCE + DragDisplay.ONE_LINER_BG_HEIGHT);
        } else {
            // display the width info above the mouse
            widthY = (float) (coEndY - MOUSE_DISPLAY_DISTANCE);
        }
        float widthX = (float) (coStartX + (coEndX - coStartX) / 2.0f - displayBgWidth / 2);
        dd.drawOneLine(widthInfo, widthX, widthY);

        // draw the height display
        float heightX;
        if (imWidth >= 0) {
            // display the height info on the right side of the mouse
            heightX = (float) (coEndX + MOUSE_DISPLAY_DISTANCE);
        } else {
            // display the height info on the left side of the mouse
            heightX = (float) (coEndX - displayBgWidth - MOUSE_DISPLAY_DISTANCE);
        }
        float heightY = (float) (coStartY + (coEndY - coStartY) / 2.0f + DragDisplay.ONE_LINER_BG_HEIGHT / 2.0f);
        dd.drawOneLine(heightInfo, heightX, heightY);

        if (startAdjusted) {
            String xInfo = "x = " + (int) imStartX + " px";
            String yInfo = "y = " + (int) imStartY + " px";
            float startInfoX;
            // can be smaller because of the rounded rectangle
            // and because it is at a distance in both dimensions
            int mouseDist = MOUSE_DISPLAY_DISTANCE / 2;
            if (imWidth >= 0) {
                // display the start info to the left of the start
                startInfoX = (float) (coStartX - displayBgWidth - mouseDist);
            } else {
                // display the start info to the right of the start
                startInfoX = (float) (coStartX + mouseDist);
            }

            float startInfoY;
            if (imHeight >= 0) {
                // display the start info above the start
                startInfoY = (float) (coStartY - mouseDist);
            } else {
                // display the start info bellow the start
                startInfoY = (float) (coStartY + mouseDist + DragDisplay.TWO_LINER_BG_HEIGHT);
            }

            dd.drawTwoLines(xInfo, yInfo, startInfoX, startInfoY);
        }

        dd.finish();
    }

    public void displayRelativeMovement(Graphics2D g) {
        assert hasCoCoords;
        int dx = (int) (imEndX - imStartX);
        int dy = (int) (imEndY - imStartY);
        // TODO mixing co and im coordinates?
        DragDisplay.displayRelativeMovement(g, dx, dy, (float) (coEndX + 30), (float) (coEndY - 20));
    }

    public void displayAngleAndDist(Graphics2D g) {
        assert hasCoCoords;
        int displayBgWidth = DragDisplay.BG_WIDTH_PIXEL;

        double coDx = coEndX - coStartX;
        double coDy = coEndY - coStartY;

        double x;
        boolean xDistIsSmall = false;
        if (coDx >= displayBgWidth) {
            // display it on the right side of the mouse
            x = coEndX + MOUSE_DISPLAY_DISTANCE;
        } else if (coDx <= -displayBgWidth) {
            // display it on the left side of the mouse
            x = coEndX - MOUSE_DISPLAY_DISTANCE - displayBgWidth;
        } else {
            xDistIsSmall = true;
            // display it so that it has no sudden jumps
            x = coEndX - displayBgWidth / 2.0
                + ((displayBgWidth / 2.0 + MOUSE_DISPLAY_DISTANCE)
                   * coDx / displayBgWidth);
        }
        int yInterpolationLimit = DragDisplay.TWO_LINER_BG_HEIGHT;
        if (xDistIsSmall) {
            // if the x distance is small, don't try to smoothly interpolate
            // the y position, because the drag display might cover the shape
            yInterpolationLimit = 0;
        }

        double y;
        if (coDy <= -yInterpolationLimit) {
            // display it above the mouse
            y = coEndY - MOUSE_DISPLAY_DISTANCE;
        } else if (coDy >= yInterpolationLimit) {
            // display it bellow the mouse
            y = coEndY + MOUSE_DISPLAY_DISTANCE + DragDisplay.TWO_LINER_BG_HEIGHT;
        } else {
            // display it so that it has no sudden jumps
            y = coEndY + DragDisplay.TWO_LINER_BG_HEIGHT / 2.0
                + ((DragDisplay.TWO_LINER_BG_HEIGHT / 2.0 + MOUSE_DISPLAY_DISTANCE)
                   * coDy / DragDisplay.TWO_LINER_BG_HEIGHT);
        }

        int dragAngle = (int) Math.toDegrees(calcIntuitiveAngle());
        int dragDistance = (int) calcImDist();

        String angleInfo = "\u2221 = " + dragAngle + " \u00b0";
        String distInfo = "d = " + dragDistance + " px";
        DragDisplay dd = new DragDisplay(g, displayBgWidth);
        dd.drawTwoLines(angleInfo, distInfo, (float) x, (float) y);
        dd.finish();
    }

    public void ensureCoCoords() {
        if (hasCoCoords) {
            return;
        }
        calcCoCoords(Views.getActive());
    }

    public void calcCoCoords(View view) {
        coStartX = view.imageXToComponentSpace(imStartX);
        coStartY = view.imageYToComponentSpace(imStartY);
        coEndX = view.imageXToComponentSpace(imEndX);
        coEndY = view.imageYToComponentSpace(imEndY);
        hasCoCoords = true;
    }

    public void debug(Graphics2D g, Color c) {
        var line = new Line2D.Double(imStartX, imStartY, imEndX, imEndY);
        Shape circle = Shapes.createCircle(imStartX, imStartY, 10);
        Shapes.debug(g, c, line);
        Shapes.debug(g, c, circle);
    }

    @Override
    public String toString() {
        return format("(%.2f, %.2f) => (%.2f, %.2f), center start = %s",
            imStartX, imStartY, imEndX, imEndY, startFromCenter);
    }
}
