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

    // Coordinates in component space
    public int x;
    public int y;

    // Coordinates in image space
    public double imX;
    public double imY;

    protected int dragStartX;
    protected int dragStartY;

    private int origX;
    private int origY;

    private final Color color;
    private final Color activeColor;

    private boolean active = false;

    private final ImageComponent ic;

    private final Rectangle shape;
    private final Rectangle shadow;
    private final static Composite shadowComposite = AlphaComposite.SrcOver.derive(0.7f);

    public DraggablePoint(String name, int x, int y, ImageComponent ic, Color color, Color activeColor) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.ic = ic;

        this.color = color;
        this.activeColor = activeColor;
        shape = new Rectangle(x - HANDLE_RADIUS, y - HANDLE_RADIUS, HANDLE_DIAMETER, HANDLE_DIAMETER);
        shadow = new Rectangle(x - HANDLE_RADIUS + SHADOW_OFFSET, y - HANDLE_RADIUS + SHADOW_OFFSET, HANDLE_DIAMETER, HANDLE_DIAMETER);
        calcImCoords();
    }

    public void setLocation(int x, int y) {
        setLocationOnlyForThis(x, y);
    }

    // setLocation is overridden in subclasses to also move related points,
    // but we also need a pure version
    public final void setLocationOnlyForThis(int x, int y) {
        this.x = x;
        this.y = y;
        shape.setLocation(x - HANDLE_RADIUS, y - HANDLE_RADIUS);
        shadow.setLocation(x - HANDLE_RADIUS + SHADOW_OFFSET, y - HANDLE_RADIUS + SHADOW_OFFSET);
    }

    public void setConstrainedLocation(int mouseX, int mouseY) {
        // can be implemented only in subclasses,
        // but we don't want to make this class abstract
        throw new UnsupportedOperationException();
    }

    public void translate(int dx, int dy) {
        setLocation(x + dx, y + dy);
    }

    public final void translateOnlyThis(int dx, int dy) {
        setLocationOnlyForThis(x + dx, y + dy);
    }

    public boolean handleContains(int x, int y) {
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

    public void mouseDragged(int x, int y) {
        int dx = x - dragStartX;
        int dy = y - dragStartY;
        int newX = origX + dx;
        int newY = origY + dy;
        setLocation(newX, newY);
    }

    public void mouseDragged(int x, int y, boolean constrained) {
        int dx = x - dragStartX;
        int dy = y - dragStartY;
        int newX = origX + dx;
        int newY = origY + dy;
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
//        System.out.printf("DraggablePoint::calcImCoords: '%s' (%d, %d) => (%.2f, %.2f) %n",
//                name, x, y, imX, imY);
    }

    public void restoreCoordsFromImSpace(ImageComponent ic) {
        assert this.ic == ic;
        int newX = ic.imageXToComponentSpace(imX);
        int newY = ic.imageYToComponentSpace(imY);
        setLocationOnlyForThis(newX, newY);
//        System.out.printf("DraggablePoint::restoreCoordsFromImSpace: '%s' (%.2f, %.2f) => (%d, %d) %n",
//                name, imX, imY, newX, newY);
    }

    public void setActive(boolean active) {
        this.active = active;
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
        return 31 * x + y;
    }

    @Override
    public String toString() {
        String sb = String.format("%s {x = %d, y = %d}{imX = %.2f, imY = %.2f}", name, x, y, imX, imY);
        return sb;
    }
}
