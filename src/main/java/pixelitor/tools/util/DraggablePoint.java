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

package pixelitor.tools.util;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.HandleMovedEdit;
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
 * fractional values, for example when paths are created from selections
 * or when shape handles are rotated.
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

    public DraggablePoint(String name, PPoint p, View view) {
        this.view = view;
        this.name = name;

        setLocationOnlyForThis(p);
    }

    public void copyPositionFrom(DraggablePoint that) {
        x = that.x;
        y = that.y;
        imX = that.imX;
        imY = that.imY;

        assert !isNaN(imX);
        assert !isNaN(imY);
    }

    @Override
    public void setLocation(double x, double y) {
        setLocationOnlyForThis(x, y);
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
     * This implementation constrains it relative to its former position.
     * Subclasses can choose a different pivot point by overriding this method.
     */
    public void setConstrainedLocation(double mouseX, double mouseY) {
        Point2D p = Utils.constrainToNearestAngle(dragStartX, dragStartY, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    public void translate(double dx, double dy) {
        setLocation(x + dx, y + dy);
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
     * for future affine transformations
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

    public boolean handleContains(double x, double y) {
        return x > this.x - HANDLE_RADIUS
            && x < this.x + HANDLE_RADIUS
            && y > this.y - HANDLE_RADIUS
            && y < this.y + HANDLE_RADIUS;
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
        translate(key.getDeltaX(), key.getDeltaY());
    }

    public void setActive(boolean activate) {
        if (activate) {
            activePoint = this;
            lastActive = this;
        } else {
            activePoint = null;
        }
    }

    public boolean isActive() {
        return this == activePoint;
    }

    public double distanceFrom(DraggablePoint other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DraggablePoint that)) {
            return false;
        }
        return sameImPositionAs(that, 1.0);
    }

    public boolean sameImPositionAs(DraggablePoint that, double epsilon) {
        return Math.abs(imX - that.imX) < epsilon
            && Math.abs(imY - that.imY) < epsilon;
    }

    public Point2D getCoLocationCopy() {
        return new Point2D.Double(x, y);
    }

    public Point2D getImLocationCopy() {
        return new Point2D.Double(imX, imY);
    }

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

    // this is supposed to be called after a mouse released event
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

    // used only after deserializing from a pxc file
    public void setView(View view) {
        assert view != null;
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
    public int hashCode() {
        return (int) (31 * x + y);
    }

    public String toColoredString() {
        return format("\u001B[32m%s\u001B[0m " +
                "{x = \u001B[33m%.2f\u001B[0m, " +
                "y = \u001B[33m%.2f\u001B[0m}" +
                "{imX = \u001B[36m%.1f\u001B[0m, " +
                "imY = \u001B[36m%.1f\u001B[0m}",
            name, x, y, imX, imY);
    }

    private String toCoordsString() {
        return format("co: (%.1f, %.1f) im:(%.1f, %.1f)", x, y, imX, imY);
    }

    @Override
    public String toString() {
        return format("%s {%s}", name, toCoordsString());
    }

    public DebugNode createDebugNode() {
        var node = new DebugNode(name, this);
        node.addString("coords", toCoordsString());
        if (imTransformRefPoint != null) {
            node.addDouble("ref X", imTransformRefPoint.getX());
            node.addDouble("ref Y", imTransformRefPoint.getY());
        }
        return node;
    }
}

