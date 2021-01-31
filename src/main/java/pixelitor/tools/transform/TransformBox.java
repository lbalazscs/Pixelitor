/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.gui.utils.DDimension;
import pixelitor.history.History;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.transform.history.TransformBoxChangedEdit;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.AngleUnit;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.*;

import static java.lang.String.format;
import static pixelitor.tools.transform.Direction.*;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Cursors.MOVE;

/**
 * A widget that manipulates a {@link Transformable} by
 * calculating an {@link AffineTransform}
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

    private Transformable owner;

    private final View view;

    // the starting position of the box in image space,
    // corresponding to the initial size of the transformed object
    private final Rectangle2D origImRect;

    // the current width/height of the rotated rectangle in image space
    private final DDimension rotatedImSize;

    private double angle = 0.0;
    private double sin = 0.0;
    private double cos = 1.0;
    private int angleDegrees = 0;

    // the angle-dependent cursor offset used to
    // determine the cursor for a given corner
    private int cursorOffset = 0;

    // the box shape in component coordinates
    private GeneralPath boxShape;

    private boolean wholeBoxDrag = false;
    private double wholeBoxDragStartX;
    private double wholeBoxDragStartY;

    private Memento beforeMovement;

    public TransformBox(Rectangle2D origCoRect, View view,
                        Transformable owner) {
        // it must be transformed to positive rectangle before calling this
        assert !origCoRect.isEmpty();

        origImRect = view.componentToImageSpace(origCoRect);
        rotatedImSize = new DDimension(origImRect);
        this.view = view;
        this.owner = owner;

        double westX = origCoRect.getX();
        double eastX = westX + origCoRect.getWidth();
        double northY = origCoRect.getY();
        double southY = northY + origCoRect.getHeight();

        Point2D.Double nwLoc = new Point2D.Double(westX, northY);
        Point2D.Double neLoc = new Point2D.Double(eastX, northY);
        Point2D.Double seLoc = new Point2D.Double(eastX, southY);
        Point2D.Double swLoc = new Point2D.Double(westX, southY);

        // initialize the corner handles
        nw = new CornerHandle("NW", this, true,
            nwLoc, view, Color.WHITE, NW_OFFSET, NW_OFFSET_IO);
        ne = new CornerHandle("NE", this, true,
            neLoc, view, Color.WHITE, NE_OFFSET, NE_OFFSET_IO);
        se = new CornerHandle("SE", this, false,
            seLoc, view, Color.WHITE, SE_OFFSET, SE_OFFSET_IO);
        sw = new CornerHandle("SW", this, false,
            swLoc, view, Color.WHITE, SW_OFFSET, SW_OFFSET_IO);

        // initialize the rotation handle
        Point2D center = Shapes.calcCenter(ne, sw);
        rot = new RotationHandle("rot", this,
            new Point2D.Double(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE),
            view);

        // initialize the edge handles
        EdgeHandle n = new EdgeHandle("N", this, nw, ne,
            Color.WHITE, true, N_OFFSET, N_OFFSET_IO);
        EdgeHandle e = new EdgeHandle("E", this, ne, se,
            Color.WHITE, false, E_OFFSET, E_OFFSET_IO);
        EdgeHandle w = new EdgeHandle("W", this, nw, sw,
            Color.WHITE, false, W_OFFSET, W_OFFSET_IO);
        EdgeHandle s = new EdgeHandle("S", this, sw, se,
            Color.WHITE, true, S_OFFSET, S_OFFSET_IO);

        // set up the neighboring relations between the corner handles
        nw.setVerNeighbor(sw, w, true);
        nw.setHorNeighbor(ne, n, true);
        se.setHorNeighbor(sw, s, true);
        se.setVerNeighbor(ne, e, true);

        // define some point sets for convenience
        handles = new DraggablePoint[]{nw, ne, se, sw, rot, n, e, w, s};
        corners = new CornerHandle[]{nw, ne, se, sw};
        edges = new EdgeHandle[]{n, e, w, s};
        positions = new PositionHandle[]{nw, ne, se, sw, n, e, w, s};

        updateBoxShape();
        updateDirections(false);
    }

    public void replaceOwner(Transformable owner) {
        this.owner = owner;
    }

    /**
     * Transforms the box geometry with the given component-space transformation
     */
    public void transform(AffineTransform at) {
        at.transform(beforeMovement.nw, nw);
        at.transform(beforeMovement.ne, ne);
        at.transform(beforeMovement.se, se);
        at.transform(beforeMovement.sw, sw);

        cornerHandlesMoved();
        view.repaint();
    }

    // rotates the box to the given angle
    @VisibleForTesting
    public void rotateTo(double angle, AngleUnit unit) {
        saveState(); // so that transform works
        double rad = unit.toAtan2Radians(angle);
        double angleBefore = this.angle;
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

        at.translate(-origImRect.getX(), -origImRect.getY());

        return at;
    }

    private double calcScaleY() {
        return rotatedImSize.getHeight() / origImRect.getHeight();
    }

    private double calcScaleX() {
        return rotatedImSize.getWidth() / origImRect.getWidth();
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

        rotatedImSize.setSize(width, height);
    }

    public Dimension2D getRotatedImSize() {
        return rotatedImSize;
    }

    public void cornerHandlesMoved() {
        updateEdgePositions();

        boolean wasInsideOut = rotatedImSize.isInsideOut();
        updateRotatedDimensions();
        updateRotLocation();
        updateBoxShape();
        applyTransform();

        boolean isInsideOut = rotatedImSize.isInsideOut();
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

    public void applyTransform() {
        owner.transform(calcImTransform());
    }

    private void updateRotLocation() {
        Point2D northCenter = Shapes.calcCenter(nw, ne);

        double rotDistX = ROT_HANDLE_DISTANCE * sin;
        double rotDistY = ROT_HANDLE_DISTANCE * cos;
        if (rotatedImSize.getHeight() < 0) {
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
        var line = new Line2D.Double(Shapes.calcCenter(nw, ne), rot);
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

    /**
     * Returns true if the transform box handles the given mouse pressed event
     */
    public boolean processMousePressed(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();
        DraggablePoint hit = handleWasHit(x, y);
        if (hit != null) {
            handleHitWhenPressed(hit, x, y);
            return true;
        } else {
            activePoint = null;
            if (contains(x, y)) {
                boxAreaHitWhenPressed(x, y);
                return true;
            }
        }

        return false;
    }

    public void handleHitWhenPressed(DraggablePoint handle, double x, double y) {
        handle.setActive(true);
        saveState();
        handle.mousePressed(x, y);
        view.repaint();
    }

    public void boxAreaHitWhenPressed(double x, double y) {
        wholeBoxDrag = true;
        wholeBoxDragStartX = x;
        wholeBoxDragStartY = y;
        saveState();
    }

    /**
     * Returns true if the transform box handles the given mouse dragged event
     */
    public boolean processMouseDragged(PMouseEvent e) {
        if (activePoint != null) {
            activePoint.mouseDragged(e.getCoX(), e.getCoY());
            owner.updateUI(view);
            return true;
        } else if (wholeBoxDrag) {
            dragBox(e);
            return true;
        }
        return false;
    }

    /**
     * Returns true if the transform box handles the given mouse released event
     */
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
            owner.updateUI(view);
            updateDirections(); // necessary only if dragged through the opposite corner
            addMovementToHistory(e.getComp(), "Change Transform Box");
            return true;
        } else if (wholeBoxDrag) {
            dragBox(e);
            wholeBoxDrag = false;
            addMovementToHistory(e.getComp(), "Drag Transform Box");
            return true;
        }
        // we shouldn't get here
        return false;
    }

    /**
     * Used when there can be only one transform box
     */
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

            if (contains(x, y)) {
                view.setCursor(MOVE);
            } else {
                view.setCursor(DEFAULT);
            }
        }
    }

    /**
     * Used when there can be more than one transform boxes.
     * Returns true if this particular transform box handles
     * the given mouse moved event.
     */
    public boolean processMouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        DraggablePoint hit = handleWasHit(x, y);
        if (hit != null) {
            hit.setActive(true);
            view.repaint();
            view.setCursor(hit.getCursor());
            return true;
        }
        return false;
    }

    public boolean contains(double x, double y) {
        return boxShape.contains(x, y);
    }

    private void addMovementToHistory(Composition comp, String editName) {
        assert editName != null;
        Memento afterMovement = copyState();
        History.add(new TransformBoxChangedEdit(editName, comp,
            this, beforeMovement, afterMovement));
    }

    void updateDirections() {
        updateDirections(rotatedImSize.isInsideOut());
    }

    private void updateDirections(boolean isInsideOut) {
        for (PositionHandle p : positions) {
            p.recalcDirection(isInsideOut, cursorOffset);
            if (p.isActive()) {
                view.setCursor(p.getCursor());
            }
        }
    }

    private void dragBox(PMouseEvent e) {
        double x = e.getCoX();
        double y = e.getCoY();

        double dx = x - wholeBoxDragStartX;
        double dy = y - wholeBoxDragStartY;

        var comp = e.getComp();

        moveWholeBoxBy(dx, dy, comp);
    }

    private void moveWholeBoxBy(double dx, double dy, Composition comp) {
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

        owner.updateUI(view);
    }

    public Point2D getCenter() {
        return Shapes.calcCenter(nw, se);
    }

    @Override
    public void coCoordsChanged(View view) {
        for (CornerHandle corner : corners) {
            corner.restoreCoordsFromImSpace(view);
        }
        updateEdgePositions();
        updateRotLocation();
        updateBoxShape();
    }

    @Override
    public void imCoordsChanged(AffineTransform at, Composition comp) {
        // rotate the corners
        for (CornerHandle corner : corners) {
            corner.imTransformOnlyThis(at, false);
        }

        // rotate the rotation handle
        rot.imTransformOnlyThis(at, false);
        recalcAngle();

        updateEdgePositions();
        updateRotatedDimensions();
        updateBoxShape();
        applyTransform();
        updateDirections();
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
        saveState();

        double dx = key.getMoveX();
        double dy = key.getMoveY();
        var comp = view.getComp();
        moveWholeBoxBy(dx, dy, comp);

        String editName = key.isShiftDown()
            ? "Shift-nudge Transform Box"
            : "Nudge Transform Box";
        addMovementToHistory(comp, editName);
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

    public DebugNode getDebugNode() {
        var node = new DebugNode("transform box", this);

        node.add(nw.getDebugNode());
        node.add(ne.getDebugNode());
        node.add(se.getDebugNode());
        node.add(sw.getDebugNode());
        node.add(rot.getDebugNode());

        node.addDouble("rotated width", rotatedImSize.getWidth());
        node.addDouble("rotated height", rotatedImSize.getHeight());
        node.addInt("angle (degrees)", getAngleDegrees());
        node.addDouble("scale X", calcScaleX());
        node.addDouble("scale Y", calcScaleY());

        AffineTransform at = calcImTransform();
        DebugNode transformNode = new DebugNode("transform", at);
        transformNode.addDouble("scaleX (m00)", at.getScaleX());
        transformNode.addDouble("scaleY (m11)", at.getScaleY());
        transformNode.addDouble("shearX (m01)", at.getShearX());
        transformNode.addDouble("shearY (m10)", at.getShearY());
        transformNode.addDouble("translateX (m02)", at.getTranslateX());
        transformNode.addDouble("translateY (m12)", at.getTranslateY());
        node.add(transformNode);

        return node;
    }

    @Override
    public String toString() {
        return format("Transform Box, corners = (%s, %s, %s, %s)",
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

        owner.updateUI(view);
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
