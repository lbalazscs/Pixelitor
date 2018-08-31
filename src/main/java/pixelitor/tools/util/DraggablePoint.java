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

package pixelitor.tools.util;

import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.history.HandleMovedEdit;
import pixelitor.utils.Shapes;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.Optional;

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
    private static final long serialVersionUID = 1L;

    private static final int HANDLE_RADIUS = 5;
    private static final int HANDLE_DIAMETER = 2 * HANDLE_RADIUS;
    private static final int SHADOW_OFFSET = 1;

    private final String name; // used only for debugging

    // Coordinates in image space
    public double imX;
    public double imY;

    protected int dragStartX;
    protected int dragStartY;

    protected double origX;
    protected double origY;

    private final Color color;
    private final Color activeColor;

    //    private boolean active = false;
    private static DraggablePoint activePoint = null;

    // transient: it has to be reset after deserialization
    protected transient View view;

    private Shape shape;
    private Shape shadow;
    private static final Composite shadowComposite = AlphaComposite.SrcOver.derive(0.7f);

    public DraggablePoint(String name, double x, double y, View view, Color color, Color activeColor) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.view = view;

        this.color = color;
        this.activeColor = activeColor;
        setShapes();
        calcImCoords();
    }

    @Override
    public void setLocation(double x, double y) {
        setLocationOnlyForThis(x, y);
    }

    // setLocation is overridden in subclasses to also move related
    // points, but we also need a pure version, which is final
    public final void setLocationOnlyForThis(double x, double y) {
        this.x = x;
        this.y = y;

        setShapes();
    }

    public final void setLocationOnlyForThis(Point2D p) {
        setLocationOnlyForThis(p.getX(), p.getY());
    }

    private void setShapes() {
        double shapeStartX = this.x - HANDLE_RADIUS;
        double shapeStartY = this.y - HANDLE_RADIUS;
        double shadowStartX = shapeStartX + SHADOW_OFFSET;
        double shadowStartY = shapeStartY + SHADOW_OFFSET;
        shape = createShape(shapeStartX, shapeStartY, HANDLE_DIAMETER);
        shadow = createShape(shadowStartX, shadowStartY, HANDLE_DIAMETER);
    }

    protected Shape createShape(double startX, double startY, double size) {
        return new Rectangle.Double(startX, startY, size, size);
    }

    public void setConstrainedLocation(double mouseX, double mouseY) {
        // can be implemented only in subclasses,
        // but we don't want to make this class abstract
        throw new UnsupportedOperationException();
    }

    public void translate(double dx, double dy) {
        setLocation(x + dx, y + dy);
    }

    public final void translateOnlyThis(double dx, double dy) {
        setLocationOnlyForThis(x + dx, y + dy);
    }

    public boolean handleContains(double x, double y) {
        return (x > (this.x - HANDLE_RADIUS))
                && (x < (this.x + HANDLE_RADIUS))
                && (y > (this.y - HANDLE_RADIUS))
                && (y < (this.y + HANDLE_RADIUS));
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

    public void mousePressed(int x, int y) {
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

    public void mouseDragged(int x, int y, boolean constrained) {
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

    public void mouseReleased(int x, int y) {
        mouseDragged(x, y);
        afterMouseReleasedActions();
    }

    public void mouseReleased(int x, int y, boolean constrained) {
        mouseDragged(x, y, constrained);
        afterMouseReleasedActions();
    }

    protected void afterMouseReleasedActions() {
        calcImCoords();
    }

    public void calcImCoords() {
        imX = view.componentXToImageSpace(x);
        imY = view.componentYToImageSpace(y);
    }

    /**
     * This should be called when the view size or zooming changes: the
     * component space coordinates are recalculated based on the
     * image space coordinates so that they adapt to the view.
     */
    public void restoreCoordsFromImSpace(View view) {
        assert this.view == view;
        double newX = view.imageXToComponentSpace(imX);
        double newY = view.imageYToComponentSpace(imY);
        setLocationOnlyForThis(newX, newY);
    }

    public void setActive(boolean active) {
//        this.active = active;
        if (active) {
            DraggablePoint.activePoint = this;
        } else {
            DraggablePoint.activePoint = null;
        }
    }

    public boolean isActive() {
        return this == activePoint;
    }

    public double distanceFrom(DraggablePoint other) {
        double dx = other.x - this.x;
        double dy = other.y - this.y;
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
        return this.x == that.x && this.y == that.y;
    }

    public String getName() {
        return name;
    }

    public boolean samePositionAs(DraggablePoint that, double epsilon) {
        return Math.abs(this.x - that.x) < epsilon
                && Math.abs(this.y - that.y) < epsilon;
    }

    public void copyPositionFrom(DraggablePoint that) {
        setLocationOnlyForThis(that.x, that.y);
        this.imX = that.imX;
        this.imY = that.imY;
    }

    public Point2D getLocationCopy() {
        return new Point2D.Double(x, y);
    }

    // this is supposed to be called after a mouse released event
    public Optional<HandleMovedEdit> createMovedEdit(Composition comp) {
        if (x == origX && y == origY) {
            return Optional.empty();
        }

        Point2D before = new Point2D.Double(origX, origY);
        HandleMovedEdit edit = new HandleMovedEdit(
                "Handle Moved", this, before, comp);
        return Optional.of(edit);
    }

    // used only after deserializing from a pxc file
    public void setView(View view) {
        this.view = view;
    }

    @Override
    public int hashCode() {
        return (int) (31 * x + y);
    }

    public String toColoredString() {
        String sb = String.format("\u001B[32m%s\u001B[0m " +
                        "{x = \u001B[33m%.2f\u001B[0m, " +
                        "y = \u001B[33m%.2f\u001B[0m}" +
                        "{imX = \u001B[36m%.1f\u001B[0m, " +
                        "imY = \u001B[36m%.1f\u001B[0m}",
                        name, x, y, imX, imY);
        return sb;
    }

    @Override
    public String toString() {
        String sb = String
                .format("%s {x = %.2f, y = %.2f}{imX = %.1f, imY = %.1f}",
                        name, x, y, imX, imY);
        return sb;
    }
}

