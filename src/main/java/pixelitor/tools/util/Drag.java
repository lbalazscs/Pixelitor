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

package pixelitor.tools.util;

import pixelitor.Views;
import pixelitor.gui.View;
import pixelitor.tools.DragTool;
import pixelitor.utils.Geometry;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.Debuggable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

import static java.lang.String.format;
import static java.util.Objects.*;

/**
 * Represents a mouse drag performed by the user while using
 * a {@link DragTool}. Only the start and end points are relevant.
 */
public class Drag implements Serializable, Debuggable {
    @Serial
    private static final long serialVersionUID = 1L;

    // image-space coordinates (relative to the canvas, adjusted for zooming)
    private double imStartX;
    private double imStartY;
    private double imEndX;
    private double imEndY;

    // transient variables from here

    // component-space coordinates (relative to the View component)
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
    private transient boolean angleConstrained;
    private transient boolean expandFromCenter;
    private transient boolean enforceEqualDimensions;

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
        this(start.getImX(), start.getImY(),
            end.getImX(), end.getImY());
    }

    public Drag(Rectangle2D bounds) {
        this(bounds.getX(),
            bounds.getY(),
            bounds.getX() + bounds.getWidth(),
            bounds.getY() + bounds.getHeight());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // Initializes all transient fields to their default values
        hasCoCoords = false;
        coStartX = 0;
        coEndX = 0;
        coStartY = 0;
        coEndY = 0;
        prevCoEndX = 0;
        prevCoEndY = 0;

        dragging = false;
        canceled = false;
        startAdjusted = false;
        angleConstrained = false;
        expandFromCenter = false;
        enforceEqualDimensions = false;
    }

    public Drag copy() {
        return new Drag(imStartX, imStartY, imEndX, imEndY);
    }

    public Drag imTransformedCopy(AffineTransform at) {
        Point2D start = new Point2D.Double(imStartX, imStartY);
        Point2D end = new Point2D.Double(imEndX, imEndY);
        at.transform(start, start);
        at.transform(end, end);
        return new Drag(start.getX(), start.getY(), end.getX(), end.getY());
    }

    public Drag imTranslatedCopy(double tx, double ty) {
        return new Drag(
            imStartX + tx, imStartY + ty,
            imEndX + tx, imEndY + ty);
    }

    public void setStart(PPoint e) {
        assert e.getView() != null;

        coStartX = e.getCoX();
        coStartY = e.getCoY();

        imStartX = e.getImX();
        imStartY = e.getImY();

        hasCoCoords = true;
    }

    public void setEnd(PPoint e) {
        coEndX = e.getCoX();
        coEndY = e.getCoY();

        if (angleConstrained) {
            Point2D newEnd = Utils.constrainToNearestAngle(coStartX, coStartY, coEndX, coEndY);
            coEndX = newEnd.getX();
            coEndY = newEnd.getY();
        } else if (enforceEqualDimensions) { // the two special cases are not used at the same time
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
        return new PPoint(coStartX, coStartY, imStartX, imStartY, view);
    }

    public PPoint getEnd(View view) {
        assert hasCoCoords;
        return new PPoint(coEndX, coEndY, imEndX, imEndY, view);
    }

    // returns the start x coordinate in component space
    public double getCoStartX(boolean centerAdjust) {
        assert hasCoCoords;
        if (centerAdjust && expandFromCenter) {
            return coStartX - (coEndX - coStartX);
        } else {
            return coStartX;
        }
    }

    // returns the start y coordinate in component space
    public double getCoStartY(boolean centerAdjust) {
        assert hasCoCoords;
        if (centerAdjust && expandFromCenter) {
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

    public double getOriginX() {
        if (expandFromCenter) {
            return imStartX - (imEndX - imStartX);
        } else {
            return imStartX;
        }
    }

    public double getOriginY() {
        if (expandFromCenter) {
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

    public Drag createDragFromCenterToEnd() {
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
        if (expandFromCenter) {
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

    public boolean isEmptyRect() {
        assert hasCoCoords;
        return coStartX == coEndX || coStartY == coEndY;
    }

    public boolean isEmptyImRect() {
        return imStartX == imEndX || imStartY == imEndY;
    }

    public void setAngleConstrained(boolean angleConstrained) {
        this.angleConstrained = angleConstrained;
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

    public void panStartPoint(View view) {
        assert hasCoCoords;
        double dx = coEndX - prevCoEndX;
        double dy = coEndY - prevCoEndY;

        coStartX += dx;
        coStartY += dy;

        imStartX = view.componentXToImageSpace(coStartX);
        imStartY = view.componentYToImageSpace(coStartY);

        startAdjusted = true;
    }

    public void setExpandFromCenter(boolean expandFromCenter) {
        this.expandFromCenter = expandFromCenter;
    }

    public boolean isExpandingFromCenter() {
        return expandFromCenter;
    }

    public void setEnforceEqualDimensions(boolean enforceEqualDimensions) {
        this.enforceEqualDimensions = enforceEqualDimensions;
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

        if (expandFromCenter) {
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

        if (expandFromCenter) {
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
    public Rectangle2D createSignedImRect() {
        double x;
        double y;
        double width;
        double height;

        if (expandFromCenter) {
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

        if (expandFromCenter) {
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

    public double calcCoLength() {
        assert hasCoCoords;
        double dx = coEndX - coStartX;
        double dy = coEndY - coStartY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (expandFromCenter) {
            length *= 2;
        }
        return length;
    }

    public double calcImLength() {
        double dx = imEndX - imStartX;
        double dy = imEndY - imStartY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (expandFromCenter) {
            length *= 2.0;
        }
        return length;
    }

    public double taxiCabMetric(int x, int y) {
        return Math.abs(x - imStartX) + Math.abs(y - imStartY);
    }

    public double calcStartDistanceFrom(double x, double y) {
        double dx = imStartX - x;
        double dy = imStartY - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double calcIntuitiveAngle() {
        return Geometry.atan2ToIntuitive(calcAngle());
    }

    public double calcAngle() {
        return Math.atan2(imEndY - imStartY, imEndX - imStartX);
    }

    protected double calcReversedAngle() {
        assert hasCoCoords;
        return Math.atan2(coStartY - coEndY, coStartX - coEndX);
    }

    public double calcDrawAngle() {
        return Math.atan2(imEndX - imStartX, imEndY - imStartY); //  between -PI and PI
    }

    public double calcAngleFromStartTo(double x, double y) {
        return Math.atan2(x - imStartX, y - imStartY);
    }

    public void drawWidthHeightOverlay(Graphics2D g) {
        assert hasCoCoords;

        double imWidth = imEndX - imStartX;
        double imHeight = imEndY - imStartY;
        MeasurementOverlay overlay = new MeasurementOverlay(g, MeasurementOverlay.BG_WIDTH_PIXELS);

        drawWidth(overlay, imWidth, imHeight);
        drawHeight(overlay, imWidth, imHeight);

        if (startAdjusted) {
            // if the entire thing is moved while it is being created
            // (the user is holding down space while dragging the mouse),
            // then also show the coordinates of the repositioned origin
            drawStartInfo(overlay, imWidth, imHeight);
        }

        overlay.cleanup();
    }

    // draw the width overlay
    private void drawWidth(MeasurementOverlay overlay, double imWidth, double imHeight) {
        double posY;
        if (imHeight >= 0) {
            // display the width info below the mouse
            posY = coEndY + MeasurementOverlay.OFFSET_FROM_MOUSE + MeasurementOverlay.SINGLE_LINE_HEIGHT;
        } else {
            // display the width info above the mouse
            posY = coEndY - MeasurementOverlay.OFFSET_FROM_MOUSE;
        }
        double posX = coStartX + (coEndX - coStartX) / 2.0 - MeasurementOverlay.BG_WIDTH_PIXELS / 2.0f;
        String widthInfo = MeasurementOverlay.formatWidthString(imWidth);
        overlay.drawOneLine(widthInfo, new Point2D.Double(posX, posY));
    }

    // draw the height overlay
    private void drawHeight(MeasurementOverlay overlay, double imWidth, double imHeight) {
        double posX;
        if (imWidth >= 0) {
            // display the height info on the right side of the mouse
            posX = coEndX + MeasurementOverlay.OFFSET_FROM_MOUSE;
        } else {
            // display the height info on the left side of the mouse
            posX = coEndX - MeasurementOverlay.BG_WIDTH_PIXELS - MeasurementOverlay.OFFSET_FROM_MOUSE;
        }
        double posY = coStartY + (coEndY - coStartY) / 2.0 + MeasurementOverlay.SINGLE_LINE_HEIGHT / 2.0f;
        String heightInfo = MeasurementOverlay.formatHeightString(imHeight);
        overlay.drawOneLine(heightInfo, new Point2D.Double(posX, posY));
    }

    private void drawStartInfo(MeasurementOverlay overlay, double imWidth, double imHeight) {
        // can be smaller because of the rounded rectangle
        // and because it is at a distance in both dimensions
        int mouseDist = MeasurementOverlay.OFFSET_FROM_MOUSE / 2;
        double posX;
        if (imWidth >= 0) {
            // display the start info to the left of the start
            posX = coStartX - MeasurementOverlay.BG_WIDTH_PIXELS - mouseDist;
        } else {
            // display the start info to the right of the start
            posX = coStartX + mouseDist;
        }

        double posY;
        if (imHeight >= 0) {
            // display the start info above the start
            posY = coStartY - mouseDist;
        } else {
            // display the start info below the start
            posY = coStartY + mouseDist + MeasurementOverlay.DOUBLE_LINE_HEIGHT;
        }

        String xInfo = "x = " + (int) imStartX + " px";
        String yInfo = "y = " + (int) imStartY + " px";
        overlay.drawTwoLines(xInfo, yInfo, new Point2D.Double(posX, posY));
    }

    public void drawRelativeMovementOverlay(Graphics2D g) {
        assert hasCoCoords;
        int imDx = (int) (imEndX - imStartX);
        int imDy = (int) (imEndY - imStartY);
        // it's OK to mix co and im coordinates here, the co coordinates are
        // for the display's position, and im values are for the displayed value
        MeasurementOverlay.displayRelativeMovement(g, imDx, imDy,
            new Point2D.Double(coEndX + 30, coEndY - 20));
    }

    /**
     * Draws an angle and distance measurement overlay based on this {@link Drag}.
     */
    public void drawAngleDistanceOverlay(Graphics2D g) {
        assert hasCoCoords;

        int dragAngle = (int) Math.toDegrees(calcIntuitiveAngle());
        String angleInfo = "∡ = " + dragAngle + " °";

        int dragLength = (int) calcImLength();
        String distInfo = "d = " + dragLength + " px";

        MeasurementOverlay overlay = new MeasurementOverlay(g, MeasurementOverlay.BG_WIDTH_PIXELS);
        overlay.drawTwoLines(angleInfo, distInfo, calcAngleDistOverlayPosition());
        overlay.cleanup();
    }

    private Point2D.Double calcAngleDistOverlayPosition() {
        double coDx = coEndX - coStartX;
        double coDy = coEndY - coStartY;

        double posX;
        boolean xDistIsSmall = false;
        if (coDx >= MeasurementOverlay.BG_WIDTH_PIXELS) {
            // display it on the right side of the mouse
            posX = coEndX + MeasurementOverlay.OFFSET_FROM_MOUSE;
        } else if (coDx <= -MeasurementOverlay.BG_WIDTH_PIXELS) {
            // display it on the left side of the mouse
            posX = coEndX - MeasurementOverlay.OFFSET_FROM_MOUSE - MeasurementOverlay.BG_WIDTH_PIXELS;
        } else {
            xDistIsSmall = true;
            // display it so that it has no sudden jumps
            posX = coEndX - MeasurementOverlay.BG_WIDTH_PIXELS / 2.0
                + ((MeasurementOverlay.BG_WIDTH_PIXELS / 2.0 + MeasurementOverlay.OFFSET_FROM_MOUSE)
                * coDx / MeasurementOverlay.BG_WIDTH_PIXELS);
        }
        int yInterpolationLimit = MeasurementOverlay.DOUBLE_LINE_HEIGHT;
        if (xDistIsSmall) {
            // if the x distance is small, don't try to smoothly interpolate
            // the y position, because the measurement overlay might cover the shape
            yInterpolationLimit = 0;
        }

        double posY;
        if (coDy <= -yInterpolationLimit) {
            // display it above the mouse
            posY = coEndY - MeasurementOverlay.OFFSET_FROM_MOUSE;
        } else if (coDy >= yInterpolationLimit) {
            // display it below the mouse
            posY = coEndY + MeasurementOverlay.OFFSET_FROM_MOUSE + MeasurementOverlay.DOUBLE_LINE_HEIGHT;
        } else {
            // display it so that it has no sudden jumps
            posY = coEndY + MeasurementOverlay.DOUBLE_LINE_HEIGHT / 2.0
                + ((MeasurementOverlay.DOUBLE_LINE_HEIGHT / 2.0 + MeasurementOverlay.OFFSET_FROM_MOUSE)
                * coDy / MeasurementOverlay.DOUBLE_LINE_HEIGHT);
        }

        return new Point2D.Double(posX, posY);
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
        Line2D line = new Line2D.Double(imStartX, imStartY, imEndX, imEndY);
        Shape circle = Shapes.createCircle(imStartX, imStartY, 10);
        Shapes.debug(g, c, line);
        Shapes.debug(g, c, circle);
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = new DebugNode(key, this);

        node.addDouble("im start x", imStartX);
        node.addDouble("im start y", imStartY);
        node.addDouble("im end x", imEndX);
        node.addDouble("im end y", imEndY);

        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Drag that = (Drag) o;
        return Double.compare(that.imStartX, imStartX) == 0 &&
            Double.compare(that.imStartY, imStartY) == 0 &&
            Double.compare(that.imEndX, imEndX) == 0 &&
            Double.compare(that.imEndY, imEndY) == 0;
    }

    @Override
    public int hashCode() {
        return hash(imStartX, imStartY, imEndX, imEndY);
    }

    @Override
    public String toString() {
        return format("(%.2f, %.2f) => (%.2f, %.2f), expanded = %s",
            imStartX, imStartY, imEndX, imEndY, expandFromCenter);
    }
}
