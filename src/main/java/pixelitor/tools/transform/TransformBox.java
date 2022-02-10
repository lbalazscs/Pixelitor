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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

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
public class TransformBox implements ToolWidget, Serializable {
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
    private GeneralPath boxShape;

    private boolean wholeBoxDrag = false;
    private double wholeBoxDragStartX;
    private double wholeBoxDragStartY;

    private transient Memento beforeMovement;

    public TransformBox(Rectangle2D origCoRect, View view, Transformable owner) {
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

        Point2D nwLoc = new Point2D.Double(westX, northY);
        Point2D neLoc = new Point2D.Double(eastX, northY);
        Point2D seLoc = new Point2D.Double(eastX, southY);
        Point2D swLoc = new Point2D.Double(westX, southY);

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
        Point2D center = Shapes.calcCenter(ne, sw);
        rot = new RotationHandle("rot", this,
            new Point2D.Double(center.getX(), ne.getY() - ROT_HANDLE_DISTANCE), view);

        initBox();
    }

    private void initBox() {
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
        this.wholeBoxDragStartX = other.wholeBoxDragStartX;
        this.wholeBoxDragStartY = other.wholeBoxDragStartY;

        if (other.beforeMovement == null) {
            this.beforeMovement = null;
        } else {
            this.beforeMovement = other.beforeMovement.copy();
        }
    }

    public TransformBox copy(Transformable newOwner, View currentView) {
        if (needsInitialization(currentView)) {
            // can't be copied without a view
            setView(currentView);
        }

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

    public boolean needsInitialization(View currentView) {
        // A box needs reinitialization if the view is null after deserialization
        // or if it's the old view after the duplication of a composition.
        return view != currentView;
    }

    /**
     * Initialize transient variables after deserialization.
     */
    public void reInitialize(View view, Transformable owner) {
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
    @VisibleForTesting
    public void rotateTo(double angle, AngleUnit unit) {
        saveState(); // so that transform works
        double rad = unit.toRadians(angle);
        double angleBefore = this.angle;
        setAngle(rad);
        Point2D c = getCenter();
        double cx = c.getX();
        double cy = c.getY();
        coTransform(AffineTransform.getRotateInstance(rad - angleBefore, cx, cy));
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
        Shapes.drawVisibly(g, boxShape);
        Shapes.drawVisibly(g, new Line2D.Double(Shapes.calcCenter(nw, ne), rot));

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
                startWholeBoxDrag(x, y);
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

    public void startWholeBoxDrag(double x, double y) {
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
        double dx = coX - wholeBoxDragStartX;
        double dy = coY - wholeBoxDragStartY;

        moveWholeBox(dx, dy);
    }

    private void moveWholeBox(double dx, double dy) {
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

    /**
     * Transforms the box geometry with the given component-space transformation
     */
    public void coTransform(AffineTransform at) {
        at.transform(beforeMovement.nw, nw);
        at.transform(beforeMovement.ne, ne);
        at.transform(beforeMovement.se, se);
        at.transform(beforeMovement.sw, sw);

        cornerHandlesMoved();
    }

    // TODO is this just a simpler version of imCoordsChanged?
    public void imTransform(AffineTransform at) {
        for (CornerHandle corner : corners) {
            corner.imTransform(at, false);
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

        angleDegrees = (int) Math.toDegrees(Utils.atan2AngleToIntuitive(angle));
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

    public DebugNode createDebugNode() {
        var node = new DebugNode("transform box", this);

        if (owner == null) {
            node.addString("owner", "null");
        } else {
            node.add(owner.createDebugNode());
        }
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
        return format("Transform Box, corners = (%s, %s, %s, %s)", nw, ne, se, sw);
    }

    public void saveState() {
        beforeMovement = copyState();
    }

    public void saveImState() {
        for (CornerHandle corner : corners) {
            corner.saveImTransformRefPoint();
        }
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

    private void addMovementToHistory(Composition comp, String editName) {
        History.add(createMovementEdit(comp, editName));
    }

    public void startMovement() {
        startWholeBoxDrag(0, 0);
    }

    public void moveWhileDragging(double x, double y) {
        // TODO transform into co space
        dragBox(x, y);
    }

    public void endMovement() {
        // no need to record an undo event here, because
        // createMovementEdit() will take care of that
        wholeBoxDrag = false;
    }

    public TransformBoxChangedEdit createMovementEdit(Composition comp, String editName) {
        assert editName != null;
        Memento afterMovement = copyState();
        TransformBoxChangedEdit edit = new TransformBoxChangedEdit(editName, comp,
            this, beforeMovement, afterMovement);
        return edit;
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

        public Memento copy() {
            Memento copy = new Memento();
            copy.nw = nw;
            copy.ne = ne;
            copy.se = se;
            copy.sw = sw;
            return copy;
        }
    }
}
