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
import pixelitor.utils.debug.DebugNode;

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
import static pixelitor.tools.transform.Direction.*;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Cursors.MOVE;

/**
 * A widget that calculates an {@link AffineTransform}
 * based on the interactive movement of handles
 */
public class TransformBox implements ToolWidget {
    // the distance (in component space) between
    // the rotate handle and the NW-NE line
    public static final int ROT_HANDLE_DISTANCE = 28;

    private final CornerHandle nw;
    private final CornerHandle ne;
    private final CornerHandle se;
    private final CornerHandle sw;
    private final RotationHandle rot;

    private final DraggablePoint[] handles;
    private final CornerHandle[] corners;
    private final PositionHandle[] positions;
    private final EdgeHandle[] edges;

    private Consumer<AffineTransform> transformListener;

    private final View view;

    // the starting position of the box in image space,
    // corresponding to the default size of the transformed object
    private final Rectangle2D origImRect;

    // the current width/height of the rotated rectangle in image space
    private final DoubleDim2D rotatedImDim;

    private double angle = 0.0;
    private double sin = 0.0;
    private double cos = 1.0;
    private int angleDegrees = 0;

    // the angle-dependent cursor offset used to
    // determine the cursor for a given corner
    private int cursorOffset = 0;

    // the box shape in component coordinates
    private GeneralPath boxShape;

    private boolean globalDrag = false;
    private double globalDragStartX;
    private double globalDragStartY;

    private Memento beforeMovement;

    public TransformBox(Rectangle2D origCoRect, View view,
                        Consumer<AffineTransform> transformListener) {
        origCoRect = Shapes.toPositiveRect(origCoRect);
        origImRect = view.componentToImageSpace(origCoRect);
        rotatedImDim = new DoubleDim2D(origImRect);
        this.view = view;

        double westX = origCoRect.getX();
        double eastX = westX + origCoRect.getWidth();
        double northY = origCoRect.getY();
        double southY = northY + origCoRect.getHeight();

        Point2D.Double nwLoc = new Point2D.Double(westX, northY);
        Point2D.Double neLoc = new Point2D.Double(eastX, northY);
        Point2D.Double seLoc = new Point2D.Double(eastX, southY);
        Point2D.Double swLoc = new Point2D.Double(westX, southY);

        nw = new CornerHandle("NW", this, true,
                nwLoc, view, Color.WHITE, NW_OFFSET, NW_OFFSET_IO);

        ne = new CornerHandle("NE", this, true,
                neLoc, view, Color.WHITE, NE_OFFSET, NE_OFFSET_IO);

        se = new CornerHandle("SE", this, false,
                seLoc, view, Color.WHITE, SE_OFFSET, SE_OFFSET_IO);

        sw = new CornerHandle("SW", this, false,
                swLoc, view, Color.WHITE, SW_OFFSET, SW_OFFSET_IO);

        Point2D center = Shapes.calcCenter(ne, sw);

        rot = new RotationHandle("rot", this,
                new Point2D.Double(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE),
                view);

        this.transformListener = transformListener;

        EdgeHandle n = new EdgeHandle("N", this, nw, ne,
                Color.WHITE, true, N_OFFSET, N_OFFSET_IO);
        EdgeHandle e = new EdgeHandle("E", this, ne, se,
                Color.WHITE, false, E_OFFSET, E_OFFSET_IO);
        EdgeHandle w = new EdgeHandle("W", this, nw, sw,
                Color.WHITE, false, W_OFFSET, W_OFFSET_IO);
        EdgeHandle s = new EdgeHandle("S", this, sw, se,
                Color.WHITE, true, S_OFFSET, S_OFFSET_IO);

        nw.setVerNeighbor(sw, w, true);
        nw.setHorNeighbor(ne, n, true);

        se.setHorNeighbor(sw, s, true);
        se.setVerNeighbor(ne, e, true);

        handles = new DraggablePoint[]{nw, ne, se, sw, rot, n, e, w, s};
        corners = new CornerHandle[]{nw, ne, se, sw};
        edges = new EdgeHandle[]{n, e, w, s};
        positions = new PositionHandle[]{nw, ne, se, sw, n, e, w, s};

        updateBoxShape();
        updateDirections(false);
    }

    public void setTransformListener(Consumer<AffineTransform> transformListener) {
        this.transformListener = transformListener;
    }

    public void transform(AffineTransform at) {
        at.transform(beforeMovement.nw, nw);
        at.transform(beforeMovement.ne, ne);
        at.transform(beforeMovement.se, se);
        at.transform(beforeMovement.sw, sw);

        cornerHandlesMoved();
        view.repaint();
    }

    // rotates the box to the given angles
    @VisibleForTesting
    public void rotateTo(double value, AngleUnit unit) {
        saveState(); // so that transform works
        double rad = unit.toAtan2Radians(value);
        double angleBefore = angle;
        setAngle(rad);
        Point2D c = getCenter();
        double cx = c.getX();
        double cy = c.getY();
        transform(AffineTransform.getRotateInstance(rad - angleBefore, cx, cy));
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
        double scaleX = calcScaleX();
        double scaleY = calcScaleY();
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

    private double calcScaleY() {
        return rotatedImDim.getHeight() / origImRect.getHeight();
    }

    private double calcScaleX() {
        return rotatedImDim.getWidth() / origImRect.getWidth();
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

    public void cornerHandlesMoved() {
        updateEdgePositions();

        boolean wasInsideOut = rotatedImDim.isInsideOut();
        updateRotatedDimensions();
        updateRotLocation();
        updateBoxShape();
        applyTransformation();

        boolean isInsideOut = rotatedImDim.isInsideOut();
        if (isInsideOut != wasInsideOut) {
            recalcAngle();
            updateDirections(isInsideOut);
        }
    }

    private void updateEdgePositions() {
        for (EdgeHandle edge : edges) {
            edge.updatePosition();
        }
    }

    public void applyTransformation() {
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
        rot.setLocation(rotX, rotY);
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

    public boolean processMousePressed(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();
        DraggablePoint handle = handleWasHit(x, y);
        if (handle != null) {
            handle.setActive(true);
            saveState();
            handle.mousePressed(x, y);
            view.repaint();
            return true;
        } else {
            activePoint = null;
            if (boxShape.contains(x, y)) {
                globalDrag = true;
                globalDragStartX = x;
                globalDragStartY = y;
                saveState();
                return true;
            }
        }

        return false;
    }

    public boolean processMouseDragged(PMouseEvent e) {
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

    public boolean processMouseReleased(PMouseEvent e) {
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
            updateDirections(); // necessary only if dragged through the opposite corner
            addMovementToHistory(e);
            return true;
        } else if (globalDrag) {
            dragAll(e);
            globalDrag = false;
            addMovementToHistory(e);
            return true;
        }
        // we shouldn't get here
        return false;
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

    private void addMovementToHistory(PMouseEvent e) {
        Memento afterMovement = copyState();
        History.addEdit(new TransformBoxChangedEdit(e.getComp(),
                this, beforeMovement, afterMovement, false));
    }

    void updateDirections() {
        updateDirections(rotatedImDim.isInsideOut());
    }

    private void updateDirections(boolean isInsideOut) {
        for (PositionHandle p : positions) {
            p.recalcDirection(isInsideOut, cursorOffset);
            if (p.isActive()) {
                view.setCursor(p.getCursor());
            }
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

        cornerHandlesMoved();

        e.imageChanged(REPAINT);
    }

    public Point2D getCenter() {
        return Shapes.calcCenter(nw, se);
    }

    @Override
    public void coCoordsChanged(View view) {
        for (CornerHandle corner : corners) {
            corner.restoreCoordsFromImSpace(view);
        }
        updateRotLocation();
        updateBoxShape();
    }

    public void setAngle(double angle) {
        if (angle == this.angle) {
            return;
        }

        this.angle = angle;
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        angleDegrees = (int) Math.toDegrees(
                Utils.atan2AngleToIntuitive(angle));
        cursorOffset = calcCursorOffset(angleDegrees);
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

    /**
     * Should be called only when the corners
     * and the rotation handle are in sync!
     */
    public void recalcAngle() {
        rot.reCalcAngle(rot.x, rot.y, true);
    }

    @VisibleForTesting
    public CornerHandle getNW() {
        return nw;
    }

    @VisibleForTesting
    public CornerHandle getNE() {
        return ne;
    }

    @VisibleForTesting
    public CornerHandle getSE() {
        return se;
    }

    @VisibleForTesting
    public CornerHandle getSW() {
        return sw;
    }

    @VisibleForTesting
    public RotationHandle getRot() {
        return rot;
    }

    // used for debugging
    public Rectangle getImBounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (CornerHandle corner : corners) {
            if (corner.imX < minX) {
                minX = (int) corner.imX;
            }
            if (corner.imY < minY) {
                minY = (int) corner.imY;
            }
            if (corner.imX > maxX) {
                maxX = (int) corner.imX;
            }
            if (corner.imY > maxY) {
                maxY = (int) corner.imY;
            }
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public DebugNode getDebugNode() {
        DebugNode node = new DebugNode("TransformBox", this);

        node.add(nw.getDebugNode());
        node.add(ne.getDebugNode());
        node.add(se.getDebugNode());
        node.add(sw.getDebugNode());
        node.add(rot.getDebugNode());

        node.addDouble("rotated width", rotatedImDim.getWidth());
        node.addDouble("rotated height", rotatedImDim.getHeight());
        node.addInt("angle", getAngleDegrees());
        node.addDouble("scale X", calcScaleX());
        node.addDouble("scale Y", calcScaleY());
        node.addString("transform", calcImTransform().toString());

        return node;
    }

    @Override
    public String toString() {
        return String.format("Transform Box, corners = (%s, %s, %s, %s)",
                nw, ne, se, sw);
    }

    public void saveState() {
        beforeMovement = copyState();
    }

    private Memento copyState() {
        Memento m = new Memento();
        m.nw = nw.getLocationCopy();
        m.ne = ne.getLocationCopy();
        m.se = se.getLocationCopy();
        m.sw = sw.getLocationCopy();
        m.angle = angle;
        return m;
    }

    public void restoreFrom(Memento m) {
        nw.setLocation(m.nw);
        ne.setLocation(m.ne);
        se.setLocation(m.se);
        sw.setLocation(m.sw);
        setAngle(m.angle);

        cornerHandlesMoved();

        // just to be sure
        updateDirections();
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
    }
}
