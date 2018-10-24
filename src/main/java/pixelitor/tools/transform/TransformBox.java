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

package pixelitor.tools.transform;

import pixelitor.gui.View;
import pixelitor.gui.utils.DoubleDim2D;
import pixelitor.history.History;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.transform.history.TransformBoxChangedEdit;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.AngleUnit;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.VisibleForTesting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Cursors.MOVE;
import static pixelitor.utils.Cursors.NE_OFFSET;
import static pixelitor.utils.Cursors.NE_OFFSET_IO;
import static pixelitor.utils.Cursors.NW_OFFSET;
import static pixelitor.utils.Cursors.NW_OFFSET_IO;
import static pixelitor.utils.Cursors.SE_OFFSET;
import static pixelitor.utils.Cursors.SE_OFFSET_IO;
import static pixelitor.utils.Cursors.SW_OFFSET;
import static pixelitor.utils.Cursors.SW_OFFSET_IO;

/**
 * A widget that calculates an {@link AffineTransform}
 * based on the interactive movement of handles
 */
public class TransformBox implements ToolWidget {
    // the distance (in component space) between
    // the rotate handle and the NW-NE line
    private static final int ROT_HANDLE_DISTANCE = 20;

    private final TransformHandle nw;
    private final TransformHandle ne;
    private final TransformHandle se;
    private final TransformHandle sw;
    private final RotationHandle rot;
    private final Consumer<AffineTransform> transformListener;
    private final DraggablePoint[] handles;
    private final TransformHandle[] trHandles;
    private final View view;

    // the starting position of the box, corresponding to
    // the default size of the transformed object
    private final Rectangle2D origImRect;

    // the width/height of the rotated rectangle in image space
    private final Dimension2D rotatedImDim;

    private double angle = 0.0;
    private int angleDegrees = 0;

    // the angle-dependent cursor offset used to
    // determine the cursor for a given corner
    private int cursorOffset = 0;

    private double sin = 0.0;
    private double cos = 1.0;

    // the box shape in component coordinates
    private GeneralPath boxShape;

    private boolean globalDrag = false;
    private double globalDragStartX;
    private double globalDragStartY;

    private Memento beforeMovement;

    public TransformBox(Rectangle origCoRect, View view,
                        Consumer<AffineTransform> transformListener) {
        origCoRect = Shapes.toPositiveRect(origCoRect);
        origImRect = view.componentToImageSpace(origCoRect);
        rotatedImDim = new DoubleDim2D(origImRect);
        this.view = view;

        int eastX = origCoRect.x + origCoRect.width;
        int southY = origCoRect.y + origCoRect.height;

        Point2D.Double nwLoc = new Point2D.Double(origCoRect.x, origCoRect.y);
        Point2D.Double neLoc = new Point2D.Double(eastX, origCoRect.y);
        Point2D.Double seLoc = new Point2D.Double(eastX, southY);
        Point2D.Double swLoc = new Point2D.Double(origCoRect.x, southY);

        nw = new TransformHandle("NW", this,
                nwLoc, view, Color.WHITE, NW_OFFSET, NW_OFFSET_IO);

        ne = new TransformHandle("NE", this,
                neLoc, view, Color.WHITE, NE_OFFSET, NE_OFFSET_IO);

        se = new TransformHandle("SE", this,
                seLoc, view, Color.WHITE, SE_OFFSET, SE_OFFSET_IO);

        sw = new TransformHandle("SW", this,
                swLoc, view, Color.WHITE, SW_OFFSET, SW_OFFSET_IO);

        Point2D center = Shapes.calcCenter(ne, sw);

        rot = new RotationHandle("rot", this,
                new Point2D.Double(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE),
                view);

        this.transformListener = transformListener;

        nw.setVerNeighbor(sw, true);
        nw.setHorNeighbor(ne, true);

        se.setHorNeighbor(sw, true);
        se.setVerNeighbor(ne, true);

        handles = new DraggablePoint[]{nw, ne, se, sw, rot};
        trHandles = new TransformHandle[]{nw, ne, se, sw};

        updateBoxShape();
        updateCursors();
    }

    public void rotate(AffineTransform rotate) {
        rotateHandlePositions(rotate);
        view.repaint();
    }

    // rotates the box to the given "intuitive" angles given in degrees
    @VisibleForTesting
    public void rotateTo(double value, AngleUnit unit) {
        saveMemento(); // so that rotate works
        double rad = unit.toAtan2Radians(value);
        double angleBefore = angle;
        setAngle(rad);
        Point2D c = getCenter();
        double cx = c.getX();
        double cy = c.getY();
        rotate(AffineTransform.getRotateInstance(rad - angleBefore, cx, cy));
    }

    /**
     * Returns an AffineTransform in image space that would transform
     * the box from its original position into the current position
     */
    public AffineTransform calcImTransform() {
        AffineTransform at = new AffineTransform();

        if (angle != 0) {
            // rotate with origin at NW
            at.rotate(angle, nw.imX, nw.imY);
        }

        // scale with origin at NW
        at.translate(nw.imX, nw.imY);
        double scaleX = rotatedImDim.getWidth() / origImRect.getWidth();
        double scaleY = rotatedImDim.getHeight() / origImRect.getHeight();
        at.scale(scaleX, scaleY);
//        at.translate(-nw.imX, -nw.imY);
//
//        // translate
//        double tx = nw.imX - origImRect.getX();
//        double ty = nw.imY - origImRect.getY();
//        at.translate(tx, ty);

        // the two commented out translates above merged into one
        at.translate(-origImRect.getX(), -origImRect.getY());

        return at;
    }

    private void updateRotatedDimensions() {
        double width, height;
        if (Math.abs(cos) > Math.abs(sin)) {
            width = (ne.imX - nw.imX) / cos;
            height = (sw.imY - nw.imY) / cos;
        } else {
            // the precision is better when divided by sin
            width = (ne.imY - nw.imY) / sin;
            height = (nw.imX - sw.imX) / sin;
        }

        rotatedImDim.setSize(width, height);
    }

    public Dimension2D getRotatedImSize() {
        return rotatedImDim;
    }

    private void saveMemento() {
        beforeMovement = saveState();
    }

    private void rotateHandlePositions(AffineTransform at) {
        at.transform(beforeMovement.nw, nw);
        at.transform(beforeMovement.ne, ne);
        at.transform(beforeMovement.se, se);
        at.transform(beforeMovement.sw, sw);

        handlePositionsChanged();
    }

    public void handlePositionsChanged() {
        updateRotatedDimensions();
        updateRotLocation();
        updateBoxShape();
        transformListener.accept(calcImTransform());
    }

    private void updateRotLocation() {
        Point2D northCenter = Shapes.calcCenter(nw, ne);

        double rotDistX = ROT_HANDLE_DISTANCE * sin;
        double rotDistY = ROT_HANDLE_DISTANCE * cos;
        if (rotatedImDim.getHeight() < 0) {
            rotDistX *= -1;
            rotDistY *= -1;
        }

        double rotX = northCenter.getX() + rotDistX;
        double rotY = northCenter.getY() - rotDistY;
        rot.setLocation(new Point2D.Double(rotX, rotY));
    }

    @Override
    public void paint(Graphics2D g) {
        // paint the lines
        Shapes.drawVisible(g, boxShape);
        Line2D line = new Line2D.Double(Shapes.calcCenter(nw, ne), rot);
        Shapes.drawVisible(g, line);

        // paint the handles
        for (DraggablePoint handle : handles) {
            handle.paintHandle(g);
        }
    }

    private void updateBoxShape() {
        boxShape = new GeneralPath();
        boxShape.moveTo(nw.getX(), nw.getY());
        boxShape.lineTo(ne.getX(), ne.getY());
        boxShape.lineTo(se.getX(), se.getY());
        boxShape.lineTo(sw.getX(), sw.getY());
        boxShape.lineTo(nw.getX(), nw.getY());
        boxShape.closePath();
    }

    @Override
    public DraggablePoint handleWasHit(double x, double y) {
        for (DraggablePoint handle : handles) {
            if (handle.handleContains(x, y)) {
                return handle;
            }
        }
        return null;
    }

    public boolean handleMousePressed(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();
        DraggablePoint handle = handleWasHit(x, y);
        if (handle != null) {
            handle.setActive(true);
            saveMemento();
            handle.mousePressed(x, y);
            view.repaint();
            return true;
        } else {
            activePoint = null;
            if (boxShape.contains(x, y)) {
                globalDrag = true;
                globalDragStartX = x;
                globalDragStartY = y;
                saveMemento();
                return true;
            }
        }

        return false;
    }

    public boolean handleMouseDragged(PMouseEvent e) {
        if (activePoint != null) {
            activePoint.mouseDragged(e.getCoX(), e.getCoY());
            e.imageChanged(REPAINT);
            return true;
        } else if (globalDrag) {
            dragAll(e);
            return true;
        }
        return false;
    }

    public boolean handleMouseReleased(PMouseEvent e) {
        if (activePoint != null) {
            double x = e.getCoX();
            double y = e.getCoY();
            activePoint.mouseReleased(x, y);
            if (!activePoint.handleContains(x, y)) {
                // we can get here if the handle has a
                // constrained position
                activePoint = null;
            }
            e.imageChanged(REPAINT);
            updateCursors();
            addToHistory(e);
            return true;
        } else if (globalDrag) {
            dragAll(e);
            globalDrag = false;
            addToHistory(e);
            return true;
        }
        // we shouldn't get here
        return false;
    }

    private void addToHistory(PMouseEvent e) {
        Memento afterMovement = saveState();
        History.addEdit(new TransformBoxChangedEdit(e.getComp(),
                this, beforeMovement, afterMovement, false));
    }

    @VisibleForTesting
    void updateCursors() {
        for (TransformHandle handle : trHandles) {
            handle.recalcCursor();
        }
    }

    private void dragAll(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        double dx = x - globalDragStartX;
        double dy = y - globalDragStartY;

        nw.setLocation(
                beforeMovement.nw.getX() + dx,
                beforeMovement.nw.getY() + dy);
        ne.setLocation(
                beforeMovement.ne.getX() + dx,
                beforeMovement.ne.getY() + dy);
        se.setLocation(
                beforeMovement.se.getX() + dx,
                beforeMovement.se.getY() + dy);
        sw.setLocation(
                beforeMovement.sw.getX() + dx,
                beforeMovement.sw.getY() + dy);

        handlePositionsChanged();

        e.imageChanged(REPAINT);
    }

    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        DraggablePoint hit = handleWasHit(x, y);
        if (hit != null) {
            hit.setActive(true);
            view.repaint();
            view.setCursor(hit.getCursor());
        } else {
            if (activePoint != null) {
                activePoint = null;
                view.repaint();
            }

            if (boxShape.contains(e.getPoint())) {
                view.setCursor(MOVE);
            } else {
                view.setCursor(DEFAULT);
            }
        }
    }

    public Point2D getCenter() {
        return Shapes.calcCenter(nw, se);
    }

    @Override
    public void coCoordsChanged(View view) {
        for (TransformHandle handle : trHandles) {
            handle.restoreCoordsFromImSpace(view);
        }
        updateRotLocation();
        updateBoxShape();
    }

    public void setAngle(double angle) {
        this.angle = angle;
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        angleDegrees = (int) Math.toDegrees(
                Utils.atan2AngleToIntuitive(angle));
        cursorOffset = calcCursorOffset(angleDegrees);
    }

    public int getCursorOffset() {
        return cursorOffset;
    }

    public double getAngle() {
        return angle;
    }

    public int getAngleDegrees() {
        return angleDegrees;
    }

    /**
     * Calculates the angle-dependent part of the cursor offset,
     * by dividing the 0-360 range of angles into eight equal parts,
     * corresponding to the eight cursors
     */
    @VisibleForTesting
    static int calcCursorOffset(int angleDeg) {
        if (angleDeg > 338) { // 360 - (45/2) = 338
            return 0;
        }
        return (angleDeg + 22) / 45;
    }

    public double getSin() {
        return sin;
    }

    public double getCos() {
        return cos;
    }

    public void recalcAngle() {
        rot.reCalcAngle(rot.x, rot.y, true);
    }

    public boolean areCornersInDefaultOrder() {
        // if the box is "inside out", then one of them is negative
        return rotatedImDim.getHeight() >= 0 && rotatedImDim.getWidth() >= 0;
    }

    @VisibleForTesting
    public TransformHandle getNW() {
        return nw;
    }

    @VisibleForTesting
    public TransformHandle getNE() {
        return ne;
    }

    @VisibleForTesting
    public TransformHandle getSE() {
        return se;
    }

    @VisibleForTesting
    public TransformHandle getSW() {
        return sw;
    }

    @VisibleForTesting
    public RotationHandle getRot() {
        return rot;
    }

    private Memento saveState() {
        Memento m = new Memento();
        m.nw = nw.getLocationCopy();
        m.ne = ne.getLocationCopy();
        m.se = se.getLocationCopy();
        m.sw = sw.getLocationCopy();
        m.angle = angle;
        m.sin = sin;
        m.cos = cos;
        return m;
    }

    public void restoreFrom(Memento m) {
        nw.setLocation(m.nw);
        ne.setLocation(m.ne);
        se.setLocation(m.se);
        sw.setLocation(m.sw);
        angle = m.angle;
        sin = m.sin;
        cos = m.cos;

        handlePositionsChanged();
    }

    /**
     * Captures the internal state of a {@link TransformBox}
     * so that it can be returned to this state later.
     */
    public static class Memento {
        private Point2D nw;
        private Point2D ne;
        private Point2D se;
        private Point2D sw;

        private double angle = 0.0;
        private double sin = 0.0;
        private double cos = 1.0;
    }
}
