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

package pixelitor.tools.transform;

import pixelitor.Composition;
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.gui.View;
import pixelitor.gui.utils.DDimension;
import pixelitor.history.History;
import pixelitor.tools.ToolWidget;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.SubPath;
import pixelitor.tools.transform.history.TransformBoxChangedEdit;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DraggablePoint;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PPoint;
import pixelitor.utils.AngleUnit;
import pixelitor.utils.Geometry;
import pixelitor.utils.Lazy;
import pixelitor.utils.Shapes;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.Debuggable;

import javax.swing.*;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

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

    // collections of handles for convenient iteration
    private DraggablePoint[] handles;
    private CornerHandle[] corners;
    private PositionHandle[] positions;
    private EdgeHandle[] edges;

    private transient Transformable target;

    private transient View view;

    // the starting bounds of the box in image space,
    // corresponding to the initial size of the transformed object
    private final Rectangle2D origImRect;

    // the current width/height of the box in image space, as if it were unrotated
    // (the name is misleading, but it can be changed only after we stop serializing)
    private final DDimension rotatedImSize;

    // the current rotation angle of the box relative to
    // its original, unrotated state
    private double angle = 0.0;
    private int angleDegrees = 0;
    private double sin = 0.0;
    private double cos = 1.0;

    // the angle-dependent cursor offset used to
    // determine the cursor for a given corner
    private int cursorOffset = 0;

    // the box shape in component coordinates
    private Path2D coBoxShape;

    private transient boolean wholeBoxDrag = false;
    private transient double wholeBoxDragStartCoX;
    private transient double wholeBoxDragStartCoY;

    private transient Memento beforeMovement;

    // this is always false for the Move Tool, but other tools still use it
    private transient boolean useLegacyHistory = true;

    private transient Lazy<JPopupMenu> contextMenu = Lazy.of(this::createContextMenu);

    public TransformBox(Rectangle2D origImRect, View view, Transformable target) {
        // it must be a positive rectangle
        assert !origImRect.isEmpty();
        this.origImRect = origImRect;
        rotatedImSize = new DDimension(origImRect);

        this.view = view;
        this.target = target;

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
        double rotX = (nw.getX() + ne.getX()) / 2;
        double rotY = ne.getY() - ROT_HANDLE_DISTANCE;
        PPoint rotPos = new PPoint(rotX, rotY, view);
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

    public TransformBox copy(Transformable newTarget) {
        TransformBox box = new TransformBox(this);
        box.setTarget(newTarget);
        return box;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // initialize transient fields
        wholeBoxDrag = false;
        wholeBoxDragStartCoX = 0;
        wholeBoxDragStartCoY = 0;
        contextMenu = Lazy.of(this::createContextMenu);

        // these transient fields will be set when they are first needed
        target = null;
        view = null;
        beforeMovement = null;
    }

    public void setTarget(Transformable target) {
        this.target = target;
    }

    /**
     * Initialize transient variables after deserialization.
     */
    public void reInitialize(View view, Transformable target) {
        // a box needs reinitialization if the view is null after deserialization
        // or if it's the old view after the duplication of a composition
        if (this.view == view) {
            return;
        }
        setView(view);
        setTarget(target);
    }

    public void setView(View view) {
        this.view = view;
        for (DraggablePoint handle : handles) {
            handle.setView(view);
        }
    }

    /**
     * Programmatically rotates the box to the given angle.
     */
    public void rotateTo(double angle, AngleUnit unit) {
        saveState(); // so that transform works
        double rad = unit.toRadians(angle);
        double angleBefore = this.angle;
        setAngle(rad);
        Point2D pivot = getPivot();
        coTransform(AffineTransform.getRotateInstance(rad - angleBefore,
            pivot.getX(), pivot.getY()));
    }

    /**
     * Returns the AffineTransform in image space that is needed to map
     * the box from its original position into the current position.
     */
    public AffineTransform calcImTransform() {
        // The position of the pivot point is irrelevant here, because
        // we don't care how the box reached its current state.
        // The pivot point is used only for the rotation logic.

        AffineTransform at = new AffineTransform();

        // the transformation is built in reverse order, using the
        // current top-left (NW) corner as a fixed reference point
        if (angle != 0) {
            // 4. rotate around the current NW corner
            at.rotate(angle, nw.imX, nw.imY);
        }

        // 3. translate to the current top-left (NW) corner's position
        at.translate(nw.imX, nw.imY);

        // 2. scale from the origin
        double scaleX = calcScaleX();
        double scaleY = calcScaleY();
        at.scale(scaleX, scaleY);

        // 1. translate so the original top-left corner is at the origin
        at.translate(-origImRect.getX(), -origImRect.getY());

        return at;
    }

    private double calcScaleY() {
        return rotatedImSize.getHeight() / origImRect.getHeight();
    }

    private double calcScaleX() {
        return rotatedImSize.getWidth() / origImRect.getWidth();
    }

    /**
     * Recalculates the unrotated width and height from the corner positions.
     */
    private void updateUnrotatedDimensions() {
        double width, height;
        // avoid dividing by a number close to zero to maintain precision
        if (Math.abs(cos) > Math.abs(sin)) {
            width = (ne.imX - nw.imX) / cos;
            height = (sw.imY - nw.imY) / cos;
        } else {
            width = (ne.imY - nw.imY) / sin;
            height = (nw.imX - sw.imX) / sin;
        }

        rotatedImSize.setSize(width, height);
    }

    public Dimension2D getRotatedImSize() {
        return rotatedImSize;
    }

    /**
     * Upates everything else after the corner handles have been moved/updated.
     */
    public void cornerHandlesMoved() {
        updateEdgePositions();

        boolean wasInsideOut = rotatedImSize.isInsideOut();
        updateUnrotatedDimensions();
        updateRotHandleLocation();
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
        target.imTransform(calcImTransform());
    }

    /**
     * Ensures that the rotation handle is attached correctly
     * above the (possibly rotated) top edge.
     */
    private void updateRotHandleLocation() {
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
        coBoxShape = new Path2D.Double();
        coBoxShape.moveTo(nw.getX(), nw.getY());
        coBoxShape.lineTo(ne.getX(), ne.getY());
        coBoxShape.lineTo(se.getX(), se.getY());
        coBoxShape.lineTo(sw.getX(), sw.getY());
        coBoxShape.lineTo(nw.getX(), nw.getY());
        coBoxShape.closePath();
    }

    @Override
    public DraggablePoint findHandleAt(double coX, double coY) {
        for (DraggablePoint handle : handles) {
            if (handle.contains(coX, coY)) {
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
            // a new handle drag action is starting
            mousePressedOn(hit, x, y);
            return true;
        } else {
            activePoint = null;
            if (contains(x, y)) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                } else {
                    // a new whole-box drag is starting
                    prepareWholeBoxDrag(e.getCoX(), e.getCoY());
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Handles a mouse press event specifically on one of the box's handles.
     */
    public void mousePressedOn(DraggablePoint handle, double x, double y) {
        View.toolSnappingChanged(cos == 1.0 && handle.shouldSnap(), false);

        handle.setActive(true);
        saveState();
        handle.mousePressed(x, y);
        view.repaint();
    }

    public void prepareWholeBoxDrag(double coX, double coY) {
        wholeBoxDrag = true;
        wholeBoxDragStartCoX = coX;
        wholeBoxDragStartCoY = coY;
        saveState();
        View.toolSnappingChanged(cos == 1.0, false);
    }

    /**
     * Returns true if the transform box handles the given mouse dragged event
     */
    public boolean processMouseDragged(PMouseEvent e) {
        if (activePoint != null) {
            activePoint.mouseDragged(e.getCoX(), e.getCoY());
            target.updateUI(view);
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
            if (!activePoint.contains(x, y)) {
                // can happen if the handle has a constrained position
                activePoint = null;
            }
            target.updateUI(view);
            updateDirections(); // necessary if dragged through the opposite corner
            addLegacyEditToHistory(e.getComp(), "Change Transform Box");
            return true;
        } else if (e.isPopupTrigger()) {
            showPopup(e);
        } else if (wholeBoxDrag) {
            dragBox(e.getCoX(), e.getCoY());
            wholeBoxDrag = false;
            addLegacyEditToHistory(e.getComp(), "Drag Transform Box");
            return true;
        }
        return false;
    }

    private void showPopup(PMouseEvent e) {
        contextMenu.get().show(view, (int) e.getCoX(), (int) e.getCoY());
    }

    private JPopupMenu createContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        menu.add(createRotateAction(QuadrantAngle.ANGLE_90));
        menu.add(createRotateAction(QuadrantAngle.ANGLE_180));
        menu.add(createRotateAction(QuadrantAngle.ANGLE_270));

        menu.addSeparator();

        menu.add(createFlipAction(FlipDirection.HORIZONTAL));
        menu.add(createFlipAction(FlipDirection.VERTICAL));

        return menu;
    }

    private AbstractAction createFlipAction(FlipDirection direction) {
        return new AbstractAction(direction.getDisplayName()) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                flip(direction);
            }
        };
    }

    private AbstractAction createRotateAction(QuadrantAngle angle) {
        return new AbstractAction(angle.getDisplayName()) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                rotate(angle);
            }
        };
    }

    private void flip(FlipDirection direction) {
        saveState();

        // get the original image-space locations of the corners
        Point2D nwImLoc = nw.getImLocationCopy();
        Point2D neImLoc = ne.getImLocationCopy();
        Point2D swImLoc = sw.getImLocationCopy();
        Point2D seImLoc = se.getImLocationCopy();

        if (direction == FlipDirection.HORIZONTAL) {
            // swap the east and west corners
            nw.setImLocationOnlyForThis(neImLoc);
            ne.setImLocationOnlyForThis(nwImLoc);
            sw.setImLocationOnlyForThis(seImLoc);
            se.setImLocationOnlyForThis(swImLoc);
        } else {
            // swap the north and south corners
            nw.setImLocationOnlyForThis(swImLoc);
            sw.setImLocationOnlyForThis(nwImLoc);
            ne.setImLocationOnlyForThis(seImLoc);
            se.setImLocationOnlyForThis(neImLoc);
        }

        // update the rest of the box's state and the UI
        cornerHandlesMoved();
        target.updateUI(view);
        addLegacyEditToHistory(view.getComp(), direction.getDisplayName());
    }

    private void rotate(QuadrantAngle rotAngle) {
        double delta = Math.toRadians(rotAngle.getAngleDegree());
        double newAngle = angle + delta;
        if (newAngle >= Math.PI * 2) {
            newAngle -= Math.PI * 2;
        }
        rotateTo(newAngle, RADIANS);

        target.updateUI(view);
        addLegacyEditToHistory(view.getComp(), rotAngle.getDisplayName());
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
        DraggablePoint handle = findHandleAt(x, y);
        if (handle != null) {
            handle.setActive(true);
            view.repaint();
            view.setCursor(handle.getCursor());
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
        nw.relTranslate(beforeMovement.nw, coDX, coDY);
        ne.relTranslate(beforeMovement.ne, coDX, coDY);
        se.relTranslate(beforeMovement.se, coDX, coDY);
        sw.relTranslate(beforeMovement.sw, coDX, coDY);

        cornerHandlesMoved();

        target.updateUI(view);
    }

    /**
     * Returns the pivot point for rotations.
     */
    public Point2D getPivot() {
        // currently the pivot point is always at the center of the box
        return Geometry.midPoint(nw, se);
    }

    @Override
    public void coCoordsChanged(View view) {
        for (CornerHandle corner : corners) {
            corner.restoreCoordsFromImSpace(view);
        }
        updateEdgePositions();
        updateRotHandleLocation();
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
        updateUnrotatedDimensions();
        updateBoxShape();
        applyTransform();
        updateDirections();
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

        moveWholeBox(key.getDeltaX(), key.getDeltaY());

        String editName = key.isShiftDown()
            ? "Shift-nudge Transform Box"
            : "Nudge Transform Box";
        addLegacyEditToHistory(view.getComp(), editName);
    }

    /**
     * Sets the rotation angle and updates derived trigonometric values.
     */
    public void setAngle(double angle) {
        if (angle == this.angle) {
            return;
        }

        this.angle = angle;
        cos = Math.cos(angle);
        sin = Math.sin(angle);

        // update the cursor offset, which depends on the angle
        angleDegrees = (int) RADIANS.toIntuitiveDegrees(angle);
        cursorOffset = calcCursorOffset(angleDegrees);
    }

    public int getAngleDegrees() {
        return angleDegrees;
    }

    /**
     * Calculates the angle-dependent part of the cursor offset,
     * by dividing the 0-360 range of angles into eight equal parts,
     * corresponding to the eight cursors.
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
        rot.recalcAngle(rot.x, rot.y, true);
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

    public Transformable getTarget() {
        return target;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = new DebugNode(key, this);

        node.addNullableDebuggable("target", target);

        node.add(nw.createDebugNode());
        node.add(ne.createDebugNode());
        node.add(se.createDebugNode());
        node.add(sw.createDebugNode());
        node.add(rot.createDebugNode());

        node.addDouble("unrotated width", rotatedImSize.getWidth());
        node.addDouble("unrotated height", rotatedImSize.getHeight());
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

    public Memento getBeforeMovementMemento() {
        return beforeMovement;
    }

    public void saveImRefPoints() {
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

        cornerHandlesMoved();

        // just to be sure
        updateDirections();

        target.updateUI(view);
    }

    private void addLegacyEditToHistory(Composition comp, String editName) {
        if (useLegacyHistory) {
            if (Tools.MOVE.isActive()) {
                // the move tool never uses the legacy history
                throw new IllegalStateException();
            }
            History.add(createLegacyEdit(comp, editName));
        }
    }

    public void setUseLegacyHistory(boolean useLegacyHistory) {
        this.useLegacyHistory = useLegacyHistory;
    }

    public void prepareMovement() {
        prepareWholeBoxDrag(0, 0);
    }

    public void moveWhileDragging(double relImX, double relImY) {
        // since these are deltas, they can't use the normal
        // image space to component space converting methods
        double scaling = view.getZoomScale();
        dragBox(scaling * relImX, scaling * relImY);
    }

    public void finalizeMovement() {
        // no need to record an undo event here, because
        // createMovementEdit() will take care of that
        wholeBoxDrag = false;
    }

    public TransformBoxChangedEdit createLegacyEdit(Composition comp, String editName) {
        assert editName != null;

        if (target instanceof SubPath) {
            comp.pathChanged();
        }

        Memento afterMovement = copyState();
        return new TransformBoxChangedEdit(editName, comp,
            this, beforeMovement, afterMovement);
    }

    public Rectangle2D getOrigImRect() {
        return origImRect;
    }

    public Memento createMemento() {
        return new Memento(this);
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

        public Memento(TransformBox box) {
            this.nw = box.nw.getLocationCopy();
            this.ne = box.ne.getLocationCopy();
            this.se = box.se.getLocationCopy();
            this.sw = box.sw.getLocationCopy();

            this.angle = box.angle;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Memento memento = (Memento) o;
            return Double.compare(memento.angle, angle) == 0 &&
                Objects.equals(nw, memento.nw) &&
                Objects.equals(ne, memento.ne) &&
                Objects.equals(se, memento.se) &&
                Objects.equals(sw, memento.sw);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nw, ne, se, sw, angle);
        }
    }
}
