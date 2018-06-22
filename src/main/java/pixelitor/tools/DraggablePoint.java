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

package pixelitor.tools;

import pixelitor.gui.ImageComponent;
import pixelitor.utils.Shapes;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * A point that can be dragged with the help of a handle.
 * All coordinates are in component space unless otherwise noted.
 * <p>
 * It has an API similar to the {@link Point} methods, but
 * it does not extend it in order to avoid overriding
 * all the setLocation variations in the subclasses.
 */
public class DraggablePoint {
    private static final int HANDLE_RADIUS = 5;
    private static final int HANDLE_DIAMETER = 2 * HANDLE_RADIUS;
    private static final int SHADOW_OFFSET = 1;

    private final String name; // used only for debugging

    // Coordinates in component space.
    // Most of the time they will be ints as they originate from
    // mouse events, but sometimes they can be floating point,
    // for example when paths are created in other ways.
    public double x;
    public double y;

    // Coordinates in image space
    public double imX;
    public double imY;

    protected int dragStartX;
    protected int dragStartY;

    private double origX;
    private double origY;

    private final Color color;
    private final Color activeColor;

    private boolean active = false;

    private final ImageComponent ic;

    private Shape shape;
    private Shape shadow;
    private final static Composite shadowComposite = AlphaComposite.SrcOver.derive(0.7f);

    public DraggablePoint(String name, double x, double y, ImageComponent ic, Color color, Color activeColor) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.ic = ic;

        this.color = color;
        this.activeColor = activeColor;
        setShapes();
        calcImCoords();
    }

    public void setLocation(double x, double y) {
        setLocationOnlyForThis(x, y);
    }

    // setLocation is overridden in subclasses to also move related points,
    // but we also need a pure version
    public final void setLocationOnlyForThis(double x, double y) {
        this.x = x;
        this.y = y;

        setShapes();
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
        return shape.contains(x, y);
    }

    public void paintHandle(Graphics2D g) {
        Composite c = g.getComposite();
        g.setComposite(shadowComposite);
        Shapes.fillVisible(g, shadow, Color.BLACK);
        g.setComposite(c);

        if (active) {
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
        imX = ic.componentXToImageSpace(x);
        imY = ic.componentYToImageSpace(y);
    }

    public void restoreCoordsFromImSpace(ImageComponent ic) {
        assert this.ic == ic;
        double newX = ic.imageXToComponentSpace(imX);
        double newY = ic.imageYToComponentSpace(imY);
        setLocationOnlyForThis(newX, newY);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
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

    @Override
    public int hashCode() {
        return (int) (31 * x + y);
    }

    @Override
    public String toString() {
        String sb = String
                .format("\u001B[32m%s\u001B[0m {x = \u001B[33m%d\u001B[0m, y = \u001B[33m%d\u001B[0m}{imX = \u001B[36m%.1f\u001B[0m, imY = \u001B[36m%.1f\u001B[0m}",
                        name, x, y, imX, imY);
        return sb;
    }
}
