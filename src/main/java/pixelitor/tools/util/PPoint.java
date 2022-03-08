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

package pixelitor.tools.util;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.View;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import static java.lang.String.format;

/**
 * The "Pixelitor Point" represents an immutable point on an image both in
 * component (mouse) coordinates and image coordinates.
 * <p>
 * Component coordinates are relative to the {@link View},
 * image coordinates are relative to the {@link Canvas} (not necessarily
 * to the BufferedImage, as the image can be bigger than the canvas) and
 * take the position of the {@link Canvas} within the
 * {@link View} and the image zooming into account.
 */
public class PPoint {
    View view;

    // All the coordinates are initialized in subclasses.

    // coordinates in image space
    protected double imX;
    protected double imY;

    // coordinates in component (MouseEvent) space
    protected double coX;
    protected double coY;

    protected PPoint(View view) {
        assert view != null;
        this.view = view;
    }

    public PPoint(double coX, double coY, double imX, double imY, View view) {
        this.coX = coX;
        this.coY = coY;
        this.imX = imX;
        this.imY = imY;
        this.view = view;
    }

    /**
     * Returns the x coordinate in component space
     */
    public double getCoX() {
        return coX;
    }

    /**
     * Returns the y coordinate in component space
     */
    public double getCoY() {
        return coY;
    }

    /**
     * Returns the x coordinate in image space
     */
    public double getImX() {
        return imX;
    }

    /**
     * Returns the y coordinate in image space
     */
    public double getImY() {
        return imY;
    }

    /**
     * Returns the image space coordinates as a Point2D
     */
    public Point2D asImPoint2D() {
        return new Point2D.Double(getImX(), getImY());
    }

    public Point asImPoint() {
        return new Point((int) getImX(), (int) getImY());
    }

    /**
     * Returns the component space coordinates as a Point2D
     */
    public Point2D asCoPoint2D() {
        return new Point2D.Double(getCoX(), getCoY());
    }

    public View getView() {
        return view;
    }

    public PPoint mirrorVertically(int compWidth) {
        return new EagerIm(view, compWidth - getImX(), getImY());
    }

    public PPoint mirrorHorizontally(int compHeight) {
        return new EagerIm(view, getImX(), compHeight - getImY());
    }

    public PPoint mirrorBoth(int compWidth, int compHeight) {
        return new EagerIm(view, compWidth - getImX(), compHeight - getImY());
    }

    public void drawLineTo(PPoint end, Graphics2D g) {
        g.draw(new Line2D.Double(
            getImX(), getImY(), end.getImX(), end.getImY()));
    }

    /**
     * Returns the squared distance in image space
     */
    public double imDistSq(PPoint other) {
        double dx = getImX() - other.getImX();
        double dy = getImY() - other.getImY();
        return dx * dx + dy * dy;
    }

    /**
     * Returns the distance in image space
     */
    public double imDist(PPoint other) {
        return Math.sqrt(imDistSq(other));
    }

    /**
     * Returns the squared distance in component space
     */
    public double coDistSq(PPoint other) {
        double dx = getCoX() - other.getCoX();
        double dy = getCoY() - other.getCoY();
        return dx * dx + dy * dy;
    }

    /**
     * Returns the distance in component space
     */
    public double coDist(PPoint other) {
        return Math.sqrt(coDistSq(other));
    }

    public static PPoint from(double coX, double coY, double imX, double imY, View view) {
        return new PPoint(coX, coY, imX, imY, view);
    }

    public static PPoint lazyFromCo(double x, double y, View view) {
        return new LazyCo(view, x, y);
    }

    public static PPoint lazyFromCo(MouseEvent e, View view) {
        return new LazyCo(view, e.getX(), e.getY());
    }

    public static PPoint eagerFromCo(double x, double y, View view) {
        return new EagerCo(view, x, y);
    }

    public static PPoint eagerFromIm(double imX, double imY, View view) {
        return new EagerIm(view, imX, imY);
    }

    public static PPoint eagerFromIm(Point2D im, View view) {
        return new EagerIm(view, im.getX(), im.getY());
    }

    public static PPoint lazyFromIm(double imX, double imY, View view) {
        return new LazyIm(view, imX, imY);
    }

    public static PPoint halfPointBetween(DraggablePoint p1, DraggablePoint p2) {
        double x = (p1.getImX() + p2.getImX()) / 2.0;
        double y = (p1.getImY() + p2.getImY()) / 2.0;
        return eagerFromIm(x, y, p1.getView());
    }

    public Composition getComp() {
        return view.getComp();
    }

    @Override
    public String toString() {
        return format("[imX = %.1f, imY = %.1f]", getImX(), getImY());
    }

    /**
     * A lazy {@link PPoint}, which converts component
     * space coordinates to image space coordinates only
     * on demand
     */
    public static class LazyCo extends PPoint {
        private boolean xConverted = false;
        private boolean yConverted = false;

        public LazyCo(View view, double x, double y) {
            super(view);
            coX = x;
            coY = y;
            // image space coordinates are not yet initialized
        }

        @Override
        public double getImX() {
            if (!xConverted) {
                imX = view.componentXToImageSpace(coX);
                xConverted = true;
            }
            return imX;
        }

        @Override
        public double getImY() {
            if (!yConverted) {
                imY = view.componentYToImageSpace(coY);
                yConverted = true;
            }
            return imY;
        }
    }

    /**
     * An eager {@link PPoint}, which converts component
     * space coordinates to image space coordinates immediately
     */
    public static class EagerCo extends PPoint {
        public EagerCo(View view, double x, double y) {
            super(view);
            coX = x;
            coY = y;
            imX = view.componentXToImageSpace(coX);
            imY = view.componentYToImageSpace(coY);
        }
    }

    /**
     * A {@link PPoint} eagerly initialized with image-space coordinates
     */
    private static class EagerIm extends PPoint {
        public EagerIm(View view, double imX, double imY) {
            super(view);
            this.imX = imX;
            this.imY = imY;
            coX = (int) view.imageXToComponentSpace(imX);
            coY = (int) view.imageYToComponentSpace(imY);
        }
    }

    /**
     * A {@link PPoint} lazily initialized with image-space coordinates
     */
    private static class LazyIm extends PPoint {
        private boolean xConverted = false;
        private boolean yConverted = false;

        public LazyIm(View view, double imX, double imY) {
            super(view);
            this.imX = imX;
            this.imY = imY;
            // component space coordinates are not yet initialized
        }

        @Override
        public double getCoX() {
            if (!xConverted) {
                coX = view.imageXToComponentSpace(imX);
                xConverted = true;
            }
            return coX;
        }

        @Override
        public double getCoY() {
            if (!yConverted) {
                coY = view.imageYToComponentSpace(imY);
                yConverted = true;
            }
            return coY;
        }
    }
}

