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
import java.io.Serial;
import java.util.Optional;

import static java.lang.Double.isNaN;
import static java.lang.String.format;

/**
 * A point that can be dragged with the help of a handle.
 * All coordinates are in component space unless otherwise noted.
 *
 * The x, y coordinates will be integers most of the time as they
 * originate from mouse events, but sometimes they can be
 * fractional values, for example when paths are created from selections
 * or when shape handles are rotated.
 */
public class DraggablePoint extends Point2D.Double {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int HANDLE_RADIUS = 5;
    protected static final int HANDLE_SIZE = 2 * HANDLE_RADIUS;
    private static final int SHADOW_OFFSET = 1;

    private final String name; // used only for debugging

    // Coordinates in image space
    public double imX;
    public double imY;

    protected double dragStartX;
    protected double dragStartY;

    protected double origX;
    protected double origY;

    private final Color color;
    private final Color activeColor;

    // since there can be only one active point at a
    // time, it can be stored in this static variable
    public static DraggablePoint activePoint = null;

    // the point that was active the last time
    public static DraggablePoint lastActive;

    // transient: it has to be reset after deserialization
    @SuppressWarnings("TransientFieldNotInitialized")
    protected transient View view;

    private Shape shape;
    private Shape shadow;
    private static final Composite shadowComposite = AlphaComposite.SrcOver.derive(0.7f);

    protected Cursor cursor;

    // the original positions in image space, saved when
    // a transform box is created to serve as reference points
    private Point2D imTransformRefPoint;

    public DraggablePoint(String name, double x, double y, View view, Color color, Color activeColor) {
        assert view != null;
        this.view = view;

        setLocationOnlyForThis(x, y);

        this.name = name;
        this.color = color;
        this.activeColor = activeColor;
    }

    @Override
    public void setLocation(double x, double y) {
        setLocationOnlyForThis(x, y);
    }

    // setLocation is overridden in subclasses to also move related
    // points, but we also need a pure version, which is final
    public final void setLocationOnlyForThis(double x, double y) {
        assert !isNaN(x) : this.x + " to NaN in " + this;
        assert !isNaN(y) : this.y + " to NaN in " + this;

        this.x = x;
        this.y = y;

        imX = view.componentXToImageSpace(x);
        imY = view.componentYToImageSpace(y);

        assert !isNaN(imX);
        assert !isNaN(imY);

        setShapes();
    }

    /**
     * Stores the current location as a reference point
     * for future affine transformations
     */
    public void storeTransformRefPoint() {
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
        // call setLocation, which is in component space and also can be overridden
        Point2D transformed;
        if (useRefPoint) {
            transformed = at.transform(imTransformRefPoint, null);
        } else {
            transformed = at.transform(getImLocationCopy(), null);
        }

        assert !isNaN(transformed.getX()) : "at = " + at + ", useRefPoint = " + useRefPoint;
        assert !isNaN(transformed.getY()) : "at = " + at + ", useRefPoint = " + useRefPoint;

        setImLocationOnlyForThis(transformed);
    }

    public final void setLocationOnlyForThis(Point2D p) {
        setLocationOnlyForThis(p.getX(), p.getY());
    }

    private void setImLocationOnlyForThis(Point2D p) {
        imX = p.getX();
        imY = p.getY();

        assert !isNaN(imX);
        assert !isNaN(imY);

        restoreCoordsFromImSpace(view);
    }

    private void setShapes() {
        double shapeStartX = x - HANDLE_RADIUS;
        double shapeStartY = y - HANDLE_RADIUS;
        shape = createShape(shapeStartX, shapeStartY);

        double shadowStartX = shapeStartX + SHADOW_OFFSET;
        double shadowStartY = shapeStartY + SHADOW_OFFSET;
        shadow = createShape(shadowStartX, shadowStartY);
    }

    protected Shape createShape(double startX, double startY) {
        return new Rectangle.Double(startX, startY, HANDLE_SIZE, HANDLE_SIZE);
    }

    /**
     * This implementation constrains it relative to its former position.
     * Subclasses can choose a different pivot point by overriding this method.
     */
    public void setConstrainedLocation(double mouseX, double mouseY) {
        Point2D p = Utils.constrainEndPoint(dragStartX, dragStartY, mouseX, mouseY);
        setLocation(p.getX(), p.getY());
    }

    public void translate(double dx, double dy) {
        setLocation(x + dx, y + dy);
    }

    public final void translateOnlyThis(double dx, double dy) {
        setLocationOnlyForThis(x + dx, y + dy);
    }

    public boolean handleContains(double x, double y) {
        return x > this.x - HANDLE_RADIUS
            && x < this.x + HANDLE_RADIUS
            && y > this.y - HANDLE_RADIUS
            && y < this.y + HANDLE_RADIUS;
    }

    public void paintHandle(Graphics2D g) {
        Composite c = g.getComposite();
        g.setComposite(shadowComposite);
        Shapes.fillVisible(g, shadow, Color.BLACK);
        g.setComposite(c);

        if (isActive()) {
            Shapes.fillVisible(g, shape, activeColor);
        } else {
            Shapes.fillVisible(g, shape, color);
        }
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

    /**
     * This should be called when the view size or zooming changes: the
     * component space coordinates are recalculated based on the
     * image space coordinates so that they adapt to the view.
     */
    public void restoreCoordsFromImSpace(View view) {
        if (this.view != view) {
            // this shouldn't happen, but it does very rarely in random GUI tests
            assert this.view != null;
            assert view != null;
//            if (RandomGUITest.isRunning()) {
//                throw new AssertionError("this view = " + this.view.getName()
//                    + ", argument view = " + view.getName());
//            }
            this.view = view; // the new view must be correct
        }

        double newX = view.imageXToComponentSpace(imX);
        double newY = view.imageYToComponentSpace(imY);
        setLocationOnlyForThis(newX, newY);
    }

    public void arrowKeyPressed(ArrowKey key) {
        translate(key.getMoveX(), key.getMoveY());
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
        if (!(o instanceof DraggablePoint)) {
            return false;
        }
        DraggablePoint that = (DraggablePoint) o;
        return samePositionAs(that);
    }

    public boolean samePositionAs(DraggablePoint that) {
        return x == that.x && y == that.y;
    }

    public boolean samePositionAs(DraggablePoint that, double epsilon) {
        return Math.abs(x - that.x) < epsilon
            && Math.abs(y - that.y) < epsilon;
    }

    public void copyPositionFrom(DraggablePoint that) {
        setLocationOnlyForThis(that.x, that.y);
        imX = that.imX;
        imY = that.imY;

        assert !isNaN(imX);
        assert !isNaN(imY);
    }

    public Point2D getLocationCopy() {
        return new Point2D.Double(x, y);
    }

    public Point2D getImLocationCopy() {
        return new Point2D.Double(imX, imY);
    }

    public Point getScreenCoords() {
        Point p = new Point((int) x, (int) y);
        SwingUtilities.convertPointToScreen(p, view);
        return p;
    }

    public PPoint asPPoint() {
        return PPoint.lazyFromCo(x, y, view);
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

    @Override
    public String toString() {
        return format("%s {x = %.2f, y = %.2f}{imX = %.1f, imY = %.1f}",
            name, x, y, imX, imY);
    }

    public DebugNode createDebugNode() {
        var node = new DebugNode(name, this);
        node.addDouble("x", getX());
        node.addDouble("y", getY());
        return node;
    }
}

