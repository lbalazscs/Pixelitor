/*
 * Copyright 2024 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.tools.pen.SubPath;
import pixelitor.tools.transform.history.TransformBoxChangedEdit;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.AngleUnit;
import pixelitor.utils.Geometry;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

import static java.lang.String.format;
import static pixelitor.tools.transform.Direction.*;
import static pixelitor.tools.util.DraggablePoint.activePoint;
import static pixelitor.utils.AngleUnit.RADIANS;
import static pixelitor.utils.Cursors.DEFAULT;
import static pixelitor.utils.Cursors.MOVE;

/**
 * A widget that manipulates a {@link Transformable} by
 * calculating an {@link AffineTransform}
 * based on the interactive movement of its handles.
 */
public class TransformBox implements ToolWidget, Debuggable, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // the distance (in component space) between
    // the rotation handle and the NW-NE line
    public static final int ROT_HANDLE_DISTANCE = 28;

    private final CornerHandle nw;
    private final CornerHandle ne;
    private final CornerHandle se;
    private final CornerHandle sw;
    private final RotationHandle rot;

    private DraggablePoint[] handles;
    private CornerHandle[] corners;
    private PositionHandle[] positions;
    private EdgeHandle[] edges;

    private transient Transformable owner;

    private transient View view;

    // Keep track of the rotated status because pixel
    // snapping should not work after rotating.
    private boolean rotated;

    // the starting positions of the box in component and image space,
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
    private GeneralPath coBoxShape;

    private boolean wholeBoxDrag = false;
    private double wholeBoxDragStartCoX;
    private double wholeBoxDragStartCoY;

    private transient Memento beforeMovement;

    public TransformBox(Rectangle2D origRect, View view, Transformable owner) {
        this(origRect, view, owner, false);
    }

    public TransformBox(Rectangle2D origRect, View view, Transformable owner, boolean isCo) {
        // it must be transformed to positive rectangle before calling this
        assert !origRect.isEmpty();

        if (isCo) {
            origImRect = view.componentToImageSpace(origRect);
        } else {
            origImRect = origRect;
        }
        rotatedImSize = new DDimension(origImRect);
        this.view = view;
        this.owner = owner;

        double westX = origImRect.getX();
        double eastX = westX + origImRect.getWidth();
        double northY = origImRect.getY();
        double southY = northY + origImRect.getHeight();

        PPoint nwLoc = PPoint.fromIm(westX, northY, view);
        PPoint neLoc = PPoint.fromIm(eastX, northY, view);
        PPoint seLoc = PPoint.fromIm(eastX, southY, view);
        PPoint swLoc = PPoint.fromIm(westX, southY, view);

        // initialize the corner handles
        nw = new CornerHandle("NW", this, true,
            nwLoc, view, NW_OFFSET, NW_OFFSET_IO);
        ne = new CornerHandle("NE", this, true,
            neLoc, view, NE_OFFSET, NE_OFFSET_IO);
        se = new CornerHandle("SE", this, false,
            seLoc, view, SE_OFFSET, SE_OFFSET_IO);
        sw = new CornerHandle("SW", this, false,
            swLoc, view, SW_OFFSET, SW_OFFSET_IO);

        // initialize the rotation handle
        Point2D center = Geometry.midPoint(ne, sw);
        PPoint rotPos = new PPoint(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE, view);
        rot = new RotationHandle("rot", this, rotPos, view);

        initBox();
    }

    private void initBox() {
        // initialize the edge handles
        EdgeHandle n = new EdgeHandle("N", this, nw, ne,
            true, N_OFFSET, N_OFFSET_IO);
        EdgeHandle e = new EdgeHandle("E", this, ne, se,
            false, E_OFFSET, E_OFFSET_IO);
        EdgeHandle w = new EdgeHandle("W", this, nw, sw,
            false, W_OFFSET, W_OFFSET_IO);
        EdgeHandle s = new EdgeHandle("S", this, sw, se,
            true, S_OFFSET, S_OFFSET_IO);

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

    @SuppressWarnings("CopyConstructorMissesField")
    public TransformBox(TransformBox other) {
        this.nw = other.nw.copy(this);
        this.ne = other.ne.copy(this);
        this.se = other.se.copy(this);
        this.sw = other.sw.copy(this);
        this.rot = other.rot.copy(this);

        this.origImRect = new Rectangle2D.Double();
        origImRect.setRect(other.origImRect);

        this.rotatedImSize = new DDimension(other.rotatedImSize);
        this.view = other.view;

        initBox();

        this.angle = other.angle;
        this.sin = other.sin;
        this.cos = other.cos;
        this.angleDegrees = other.angleDegrees;
        this.cursorOffset = other.cursorOffset;
        this.wholeBoxDrag = other.wholeBoxDrag;
        this.wholeBoxDragStartCoX = other.wholeBoxDragStartCoX;
        this.wholeBoxDragStartCoY = other.wholeBoxDragStartCoY;

        if (other.beforeMovement == null) {
            this.beforeMovement = null;
        } else {
            // sharing the references is OK,
            // because memento objects are immutable
            this.beforeMovement = other.beforeMovement;
        }
    }

    public TransformBox copy(Transformable newOwner) {
        TransformBox box = new TransformBox(this);
        box.setOwner(newOwner);
        return box;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // these transient fields will be set when they are first needed
        owner = null;
        view = null;
        beforeMovement = null;
    }

    public void setOwner(Transformable owner) {
        this.owner = owner;
    }

    /**
     * Initialize transient variables after deserialization.
     */
    public void reInitialize(View view, Transformable owner) {
        // A box needs reinitialization if the view is null after deserialization
        // or if it's the old view after the duplication of a composition.
        if (this.view == view) {
            return;
        }
        setView(view);
        setOwner(owner);
    }

    public void setView(View view) {
        this.view = view;
        for (DraggablePoint handle : handles) {
            handle.setView(view);
        }
    }

    // rotates the box to the given angle
    public void rotateTo(double angle, AngleUnit unit) {
        saveState(); // so that transform works
        double rad = unit.toRadians(angle);
        double angleBefore = this.angle;
        setAngle(rad);
        Point2D c = getCenter();
        double cx = c.getX();
        double cy = c.getY();
        coTransform(AffineTransform.getRotateInstance(rad - angleBefore, cx, cy));
        setRotated(true);
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
        owner.imTransform(calcImTransform());
    }

    private void updateRotLocation() {
        Point2D northCenter = Geometry.midPoint(nw, ne);

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
        Shapes.drawVisibly(g, coBoxShape);
        Shapes.drawVisibly(g, new Line2D.Double(Geometry.midPoint(nw, ne), rot));

        // paint the handles
        for (DraggablePoint handle : handles) {
            handle.paintHandle(g);
        }
    }

    private void updateBoxShape() {
        coBoxShape = new GeneralPath();
        coBoxShape.moveTo(nw.getX(), nw.getY());
        coBoxShape.lineTo(ne.getX(), ne.getY());
        coBoxShape.lineTo(se.getX(), se.getY());
        coBoxShape.lineTo(sw.getX(), sw.getY());
        coBoxShape.lineTo(nw.getX(), nw.getY());
        coBoxShape.closePath();
    }

    @Override
    public DraggablePoint findHandleAt(double x, double y) {
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
        double x = e.getOrigCoX();
        double y = e.getOrigCoY();
        DraggablePoint hit = findHandleAt(x, y);
        if (hit != null) {
            mousePressedOn(hit, x, y);
            return true;
        } else {
            activePoint = null;
            if (contains(x, y)) {
                startWholeBoxDrag(e.getCoX(), e.getCoY());
                return true;
            }
        }

        return false;
    }

    public void mousePressedOn(DraggablePoint handle, double x, double y) {
        View.toolSnappingChanged(!rotated && handle.shouldSnap(), false);

        handle.setActive(true);
        saveState();
        handle.mousePressed(x, y);
        view.repaint();
    }

    public void startWholeBoxDrag(double coX, double coY) {
        wholeBoxDrag = true;
        wholeBoxDragStartCoX = coX;
        wholeBoxDragStartCoY = coY;
        saveState();
        View.toolSnappingChanged(!rotated, false);
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
            dragBox(e.getCoX(), e.getCoY());
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
                // can happen if the handle has a constrained position
                activePoint = null;
            }
            owner.updateUI(view);
            updateDirections(); // necessary if dragged through the opposite corner
            addMovementToHistory(e.getComp(), "Change Transform Box");
            return true;
        } else if (wholeBoxDrag) {
            dragBox(e.getCoX(), e.getCoY());
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
        DraggablePoint hit = findHandleAt(x, y);
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
        DraggablePoint hit = findHandleAt(x, y);
        if (hit != null) {
            hit.setActive(true);
            view.repaint();
            view.setCursor(hit.getCursor());
            return true;
        }
        return false;
    }

    public boolean contains(double coX, double coY) {
        return coBoxShape.contains(coX, coY);
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

    private void dragBox(double coX, double coY) {
        double coDX = coX - wholeBoxDragStartCoX;
        double coDY = coY - wholeBoxDragStartCoY;

        moveWholeBox(coDX, coDY);
    }

    private void moveWholeBox(double coDX, double coDY) {
        nw.setLocation(
            beforeMovement.nw.getCoX() + coDX,
            beforeMovement.nw.getCoY() + coDY);
        ne.setLocation(
            beforeMovement.ne.getCoX() + coDX,
            beforeMovement.ne.getCoY() + coDY);
        se.setLocation(
            beforeMovement.se.getCoX() + coDX,
            beforeMovement.se.getCoY() + coDY);
        sw.setLocation(
            beforeMovement.sw.getCoX() + coDX,
            beforeMovement.sw.getCoY() + coDY);

        cornerHandlesMoved();

        owner.updateUI(view);
    }

    public Point2D getCenter() {
        return Geometry.midPoint(nw, se);
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
    public void imCoordsChanged(AffineTransform at, View view) {
        // move the corners
        for (CornerHandle corner : corners) {
            corner.imTransformOnlyThis(at, false);
        }

        // move the rotation handle
        rot.imTransformOnlyThis(at, false);
        recalcAngle();

        updateEdgePositions();
        updateRotatedDimensions();
        updateBoxShape();
        applyTransform();
        updateDirections();
    }

    public void setRotated(boolean rotated) {
        this.rotated = rotated;
    }

    /**
     * Transforms the box geometry with the given component-space transformation
     */
    public void coTransform(AffineTransform at) {
        nw.coTransformOnlyThis(at, beforeMovement.nw);
        ne.coTransformOnlyThis(at, beforeMovement.ne);
        se.coTransformOnlyThis(at, beforeMovement.se);
        sw.coTransformOnlyThis(at, beforeMovement.sw);

        cornerHandlesMoved();
    }

    // TODO is this just a simpler version of imCoordsChanged?
    public void imTransform(AffineTransform at) {
        for (CornerHandle corner : corners) {
            corner.imTransform(at, true);
        }

        cornerHandlesMoved();
    }

    @Override
    public void arrowKeyPressed(ArrowKey key, View view) {
        saveState();

        moveWholeBox(key.getMoveX(), key.getMoveY());

        String editName = key.isShiftDown()
            ? "Shift-nudge Transform Box"
            : "Nudge Transform Box";
        addMovementToHistory(view.getComp(), editName);
    }

    public void setAngle(double angle) {
        if (angle == this.angle) {
            return;
        }

        this.angle = angle;
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        angleDegrees = (int) RADIANS.toIntuitiveDegrees(angle);
        cursorOffset = calcCursorOffset(angleDegrees);
    }

    public int getAngleDegrees() {
        return angleDegrees;
    }

    /**
     * Calculates the angle-dependent part of the cursor offset,
     * by dividing the 0-360 range of angles into eight equal parts,
     * corresponding to the eight cursors
     */
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

    public CornerHandle getNW() {
        return nw;
    }

    public CornerHandle getNE() {
        return ne;
    }

    public CornerHandle getSE() {
        return se;
    }

    public CornerHandle getSW() {
        return sw;
    }

    public RotationHandle getRot() {
        return rot;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

        node.addNullableDebuggable("owner", owner);

        node.add(nw.createDebugNode());
        node.add(ne.createDebugNode());
        node.add(se.createDebugNode());
        node.add(sw.createDebugNode());
        node.add(rot.createDebugNode());

        node.addDouble("rotated width", rotatedImSize.getWidth());
        node.addDouble("rotated height", rotatedImSize.getHeight());
        node.addInt("angle (degrees)", getAngleDegrees());
        node.addDouble("scale X", calcScaleX());
        node.addDouble("scale Y", calcScaleY());

        AffineTransform at = calcImTransform();
        node.add(DebugNodes.createTransformNode("transform", at));

        return node;
    }

    @Override
    public String toString() {
        return format("Transform Box, corners = (%s, %s, %s, %s)", nw, ne, se, sw);
    }

    private void saveState() {
        beforeMovement = copyState();
    }

    public void saveImState() {
        for (CornerHandle corner : corners) {
            corner.saveImTransformRefPoint();
        }
    }

    private Memento copyState() {
        return new Memento(this);
    }

    public void restoreFrom(Memento m) {
        nw.setLocationOnlyForThis(m.nw);
        ne.setLocationOnlyForThis(m.ne);
        se.setLocationOnlyForThis(m.se);
        sw.setLocationOnlyForThis(m.sw);
        setAngle(m.angle);
        rotated = m.rotated;

        cornerHandlesMoved();

        // just to be sure
        updateDirections();

        owner.updateUI(view);
    }

    private void addMovementToHistory(Composition comp, String editName) {
        History.add(createMovementEdit(comp, editName));
    }

    public void startMovement() {
        startWholeBoxDrag(0, 0);
    }

    public void moveWhileDragging(double relImX, double relImY) {
        // Since these are deltas, they can't use the normal
        // image space => component space methods.
        double scaling = view.getScaling();
        dragBox(scaling * relImX, scaling * relImY);
    }

    public void endMovement() {
        // no need to record an undo event here, because
        // createMovementEdit() will take care of that
        wholeBoxDrag = false;
    }

    public TransformBoxChangedEdit createMovementEdit(Composition comp, String editName) {
        assert editName != null;

        if (owner instanceof SubPath) {
            comp.pathChanged();
        }

        Memento afterMovement = copyState();
        return new TransformBoxChangedEdit(editName, comp,
            this, beforeMovement, afterMovement);
    }

    /**
     * Captures the internal state of a {@link TransformBox}
     * so that it can be returned to this state later.
     */
    public static class Memento {
        private final PPoint nw;
        private final PPoint ne;
        private final PPoint se;
        private final PPoint sw;

        private final double angle;
        private final boolean rotated;

        public Memento(TransformBox box) {
            this.nw = box.nw.getLocationCopy();
            this.ne = box.ne.getLocationCopy();
            this.se = box.se.getLocationCopy();
            this.sw = box.sw.getLocationCopy();

            this.angle = box.angle;
            this.rotated = box.rotated;
        }
    }
}
