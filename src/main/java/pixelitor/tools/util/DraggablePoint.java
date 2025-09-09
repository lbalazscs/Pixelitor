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

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.HandleMovedEdit;
import pixelitor.tools.pen.AnchorPoint;
import pixelitor.tools.pen.ControlPoint;
import pixelitor.tools.pen.Path;
import pixelitor.utils.Shapes;
import pixelitor.utils.Utils;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serial;
import java.util.Optional;

import static java.lang.Double.isNaN;
import static java.lang.String.format;

/**
 * A point that can be dragged with the help of a handle.
 * It maintains both component-space and image-space coordinates.
 *
 * The x, y coordinates will be integers most of the time as they
 * originate from mouse events, but sometimes they can be
 * fractional values, for example when paths are created from
 * selections or when transform box handles are rotated.
 */
public class DraggablePoint extends Point2D.Double {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final double HANDLE_RADIUS = 5.0;
    protected static final double HANDLE_SIZE = 2.0 * HANDLE_RADIUS;
    private static final double SHADOW_OFFSET = 1.0;

    private static final Color DEFAULT_HANDLE_COLOR = Color.WHITE;
    private static final Color ACTIVE_HANDLE_COLOR = Color.RED;

    protected final String name; // used only for debugging

    // Coordinates in image space (relative to the image, considering zooming).
    // All other coordinates are in component space, relative to the view.
    public double imX;
    public double imY;

    // helper variables used while dragging
    protected double dragStartX;
    protected double dragStartY;
    protected double origX;
    protected double origY;

    // since only one point can be active at a time,
    // it is stored in this static variable
    public static DraggablePoint activePoint = null;

    // the point that was active the last time
    public static DraggablePoint lastActive;

    // transient: it has to be reset after deserialization
    @SuppressWarnings("TransientFieldNotInitialized")
    protected transient View view;

    protected Cursor cursor;
    private Shape shape;
    private Shape shadowShape;
    private static final Composite SHADOW_COMPOSITE = AlphaComposite.SrcOver.derive(0.7f);

    // the original position in image space, saved when
    // a transform box is created to serve as reference points
    private Point2D imTransformRefPoint;

    public DraggablePoint(String name, PPoint pos, View view) {
        this.view = view;
        this.name = name;

        setLocationOnlyForThis(pos);
    }

    public void copyPositionFrom(DraggablePoint that) {
        x = that.x;
        y = that.y;
        imX = that.imX;
        imY = that.imY;

        assert !isNaN(imX);
        assert !isNaN(imY);

        updateShapes();
    }

    @Override
    public void setLocation(double coX, double coY) {
        setLocationOnlyForThis(coX, coY);
    }

    /**
     * Sets the location of this point without affecting related points.
     * (setLocation is overridden in subclasses to also move related points).
     */
    public final void setLocationOnlyForThis(double coX, double coY) {
        assert !isNaN(coX) : this.x + " to NaN in " + this;
        assert !isNaN(coY) : this.y + " to NaN in " + this;

        this.x = coX;
        this.y = coY;

        imX = view.componentXToImageSpace(coX);
        imY = view.componentYToImageSpace(coY);

        assert !isNaN(imX);
        assert !isNaN(imY);

        updateShapes();
    }

    public final void setLocationOnlyForThis(PPoint p) {
        this.x = p.getCoX();
        this.y = p.getCoY();
        this.imX = p.getImX();
        this.imY = p.getImY();

        updateShapes();
    }

    public final void setLocationOnlyForThis(Point2D p) {
        setLocationOnlyForThis(p.getX(), p.getY());
    }

    /**
     * Sets the location based on image space coordinates.
     */
    public void setImLocationOnlyForThis(Point2D p) {
        imX = p.getX();
        imY = p.getY();

        assert !isNaN(imX);
        assert !isNaN(imY);

        restoreCoordsFromImSpace(view);
    }

    /**
     * Recalculates component space coordinates from image space coordinates.
     * This should be called when the view size or zooming changes.
     */
    public void restoreCoordsFromImSpace(View view) {
        if (this.view != view) {
            // this shouldn't happen, but it does very rarely in random GUI tests
            assert this.view != null;
            assert view != null;
            this.view = view; // the new view must be correct
        }

        double newX = view.imageXToComponentSpace(imX);
        double newY = view.imageYToComponentSpace(imY);
        setLocationOnlyForThis(newX, newY);
    }

    /**
     * Sets a new location, constrained to the angle from the starting point.
     * Subclasses can use a different pivot point by overriding this method.
     */
    public void setConstrainedLocation(double mouseX, double mouseY) {
        Point2D p = Utils.constrainToNearestAngle(dragStartX, dragStartY, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    /**
     * Translates the point by the given deltas.
     */
    public void translate(double coDx, double coDy) {
        setLocation(x + coDx, y + coDy);
    }

    public void relTranslate(PPoint origPos, double coDx, double coDy) {
        setLocation(
            origPos.getCoX() + coDx,
            origPos.getCoY() + coDy);
    }

    public final void translateOnlyThis(double dx, double dy) {
        setLocationOnlyForThis(x + dx, y + dy);
    }

    public void imTranslate(double dx, double dy) {
        // Only set the image-space coordinates because this should work without a view
        imX += dx;
        imY += dy;
    }

    /**
     * Stores the current location as a reference point
     * for future affine transformations.
     */
    public void saveImTransformRefPoint() {
        imTransformRefPoint = getImLocationCopy();
    }

    public void imTransform(AffineTransform at, boolean useRefPoint) {
        imTransformOnlyThis(at, useRefPoint);
    }

    /**
     * Transforms the image-space coordinates with the given {@link AffineTransform},
     * and also recalculates the component-space coordinates
     */
    public final void imTransformOnlyThis(AffineTransform at, boolean useRefPoint) {
        // Can't simply use at.transform(refPoint, this) because that would
        // call setLocation, which is in component space and also can be overridden.
        Point2D newImLoc;
        if (useRefPoint) {
            newImLoc = at.transform(imTransformRefPoint, null);
        } else {
            newImLoc = at.transform(getImLocationCopy(), null);
        }

        assert !isNaN(newImLoc.getX()) : "at = " + at + ", useRefPoint = " + useRefPoint;
        assert !isNaN(newImLoc.getY()) : "at = " + at + ", useRefPoint = " + useRefPoint;

        setImLocationOnlyForThis(newImLoc);
    }

    public final void coTransformOnlyThis(AffineTransform at, PPoint refPoint) {
        Point2D transformed = new Point2D.Double();
        Point2D orig = refPoint.toCoPoint2D();
        at.transform(orig, transformed);
        setLocationOnlyForThis(transformed);
    }

    /**
     * Updates the shapes used for drawing the handle.
     */
    private void updateShapes() {
        double shapeStartX = x - HANDLE_RADIUS;
        double shapeStartY = y - HANDLE_RADIUS;
        shape = createShape(shapeStartX, shapeStartY);

        double shadowStartX = shapeStartX + SHADOW_OFFSET;
        double shadowStartY = shapeStartY + SHADOW_OFFSET;
        shadowShape = createShape(shadowStartX, shadowStartY);
    }

    /**
     * Creates the shape used to draw the handle. Returns a
     * square by default, but it can be overriden to customize it.
     */
    protected Shape createShape(double startX, double startY) {
        return new Rectangle2D.Double(startX, startY, HANDLE_SIZE, HANDLE_SIZE);
    }

    /**
     * Checks whether the shape positions are in sync with the coordinates.
     */
    public boolean checkShapeBounds() {
        if (shape.getBounds().contains(x, y)) {
            return true; // OK
        }
        throw new IllegalStateException(name + ": x = " + x + ", y = " + y + ", shape bounds = " + shape.getBounds());
    }

    public void paintHandle(Graphics2D g) {
        Composite origComposite = g.getComposite();

        // paint the shadow first
        g.setComposite(SHADOW_COMPOSITE);
        g.setColor(Color.BLACK);
        g.fill(shadowShape);

        // paint the handle
        g.setComposite(origComposite);
        Shapes.fillVisibly(g, shape, isActive()
            ? ACTIVE_HANDLE_COLOR
            : DEFAULT_HANDLE_COLOR);
    }

    /**
     * Checks if the given component-space coordinates are within the handle's bounds.
     */
    public boolean contains(double coX, double coY) {
        return coX > this.x - HANDLE_RADIUS
            && coX < this.x + HANDLE_RADIUS
            && coY > this.y - HANDLE_RADIUS
            && coY < this.y + HANDLE_RADIUS;
    }

    public void mousePressed(double x, double y) {
        dragStartX = x;
        dragStartY = y;
        // since the handle has a certain size, the point location
        // and the drag start location are not necessarily the same
        origX = this.x;
        origY = this.y;
    }

    public void mouseDragged(double x, double y) {
        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;
        setLocation(newX, newY);
    }

    public void mouseDragged(double x, double y, boolean constrained) {
        double dx = x - dragStartX;
        double dy = y - dragStartY;
        double newX = origX + dx;
        double newY = origY + dy;

        if (constrained) {
            setConstrainedLocation(newX, newY);
        } else {
            setLocation(newX, newY);
        }
    }

    public void mouseReleased(double x, double y) {
        mouseDragged(x, y);
        afterMouseReleasedActions();
    }

    public void mouseReleased(double x, double y, boolean constrained) {
        mouseDragged(x, y, constrained);
        afterMouseReleasedActions();
    }

    protected void afterMouseReleasedActions() {
    }

    public void arrowKeyPressed(ArrowKey key) {
        // calculate the new position in image space
        double newImX = imX + key.getDeltaX();
        double newImY = imY + key.getDeltaY();

        // convert the new image-space position to component-space
        double newCoX = view.imageXToComponentSpace(newImX);
        double newCoY = view.imageYToComponentSpace(newImY);

        // setLocation updates both coordinate systems and is
        // overridable by subclasses to move related points.
        setLocation(newCoX, newCoY);
    }

    // nudge an anchor or control point of a path with undo support
    public boolean nudge(ArrowKey key) {
        assert (this instanceof AnchorPoint) || (this instanceof ControlPoint);

        if (view == null) {
            return false;
        }
        Composition comp = view.getComp();
        Path path = comp.getActivePath();
        if (path == null) {
            return false;
        }

        mousePressed(this.x, this.y); // save current position as drag start
        arrowKeyPressed(key);
        createMovedEdit(comp).ifPresent(path::handleMoved);
        view.repaint();

        return true;
    }

    /**
     * Activates or deactivates this point.
     */
    public void setActive(boolean activate) {
        if (activate) {
            activePoint = this;
            lastActive = this;
        } else {
            activePoint = null;
        }
    }

    /**
     * Checks if this point is currently active.
     */
    public boolean isActive() {
        return this == activePoint;
    }

    public double distanceFrom(DraggablePoint other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Checks if this point has the same image-space position as another {@link DraggablePoint}.
     */
    public boolean hasSameImPosAs(DraggablePoint that, double epsilon) {
        return Math.abs(imX - that.imX) < epsilon
            && Math.abs(imY - that.imY) < epsilon;
    }

    /**
     * Creates a copy of the component-space location.
     */
    public Point2D getCoLocationCopy() {
        return new Point2D.Double(x, y);
    }

    /**
     * Creates a copy of the image-space location.
     */
    public Point2D getImLocationCopy() {
        return new Point2D.Double(imX, imY);
    }

    /**
     * Returns a copy of the location as a {@link PPoint}.
     */
    public PPoint getLocationCopy() {
        return new PPoint(x, y, imX, imY, view);
    }

    public double getImX() {
        return imX;
    }

    public double getImY() {
        return imY;
    }

    public Point getScreenCoords() {
        Point p = new Point((int) x, (int) y);
        SwingUtilities.convertPointToScreen(p, view);
        return p;
    }

    /**
     * Creates an undoable edit for the move, if the point has actually moved.
     * This is supposed to be called after a mouse released event.
     */
    public Optional<HandleMovedEdit> createMovedEdit(Composition comp) {
        if (x == origX && y == origY) {
            return Optional.empty();
        }

        Point2D before = new Point2D.Double(origX, origY);
        HandleMovedEdit edit = new HandleMovedEdit(
            getMoveEditName(), this, before, comp);
        return Optional.of(edit);
    }

    public String getMoveEditName() {
        return "Move Handle";
    }

    public void setView(View view) {
        this.view = view;
    }

    public View getView() {
        return view;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public String getName() {
        return name;
    }

    public boolean shouldSnap() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DraggablePoint that)) {
            return false;
        }
        return hasSameImPosAs(that, 1.0);
    }

    @Override
    public int hashCode() {
        return (int) (31 * x + y);
    }

    @Override
    public String toString() {
        return format("%s {co: (%.1f, %.1f) im:(%.1f, %.1f)}", name, x, y, imX, imY);
    }

    public DebugNode createDebugNode() {
        var node = new DebugNode(name, this);

        node.addBoolean("active", isActive());

        node.addDouble("co X", x);
        node.addDouble("co Y", y);
        node.addDouble("im X", imX);
        node.addDouble("im Y", imY);

        if (imTransformRefPoint != null) {
            node.addDouble("ref X", imTransformRefPoint.getX());
            node.addDouble("ref Y", imTransformRefPoint.getY());
        }
        return node;
    }
}

