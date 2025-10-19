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

import pixelitor.gui.View;
import pixelitor.utils.Shapes;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The rectangular equivalent of {@link PPoint}, representing
 * a rectangular area in both component space and image space.
 */
public class PRectangle {
    // the rectangle in component-space, with integer coordinates
    private Rectangle coRect;

    // the rectangle in in image space, with sub-pixel precision
    private Rectangle2D imRect;

    private PRectangle(Rectangle coRect, Rectangle2D imRect) {
        this.coRect = coRect;
        this.imRect = imRect;
    }

    /**
     * Creates a {@link PRectangle} from component-space coordinates.
     */
    public static PRectangle fromCo(Rectangle coRect, View view) {
        Rectangle2D imRect = view.componentToImageSpace(coRect);
        return new PRectangle(coRect, imRect);
    }

    /**
     * Creates a positive {@link PRectangle} from component-space coordinates.
     */
    public static PRectangle positiveFromCo(Rectangle coRect, View view) {
        coRect = Shapes.toPositiveRect(coRect);
        return fromCo(coRect, view);
    }

    /**
     * Creates a {@link PRectangle} from image-space coordinates.
     */
    public static PRectangle fromIm(Rectangle2D imRect, View view) {
        Rectangle coRect = view.imageToComponentSpace(imRect);
        return new PRectangle(coRect, imRect);
    }

    /**
     * Creates a {@link PRectangle} from image-space coordinates.
     */
    public static PRectangle fromIm(double x, double y, double w, double h, View view) {
        Rectangle2D rect = new Rectangle2D.Double(x, y, w, h);
        return fromIm(rect, view);
    }

    /**
     * Creates a positive {@link PRectangle} from image-space coordinates.
     */
    public static PRectangle positiveFromIm(Rectangle2D imRect, View view) {
        imRect = Shapes.toPositiveRect(imRect);
        return fromIm(imRect, view);
    }

    public Rectangle getCo() {
        return coRect;
    }

    public Rectangle2D getIm() {
        return imRect;
    }

    public boolean containsCo(Point point) {
        return coRect.contains(point);
    }

    public boolean containsCo(int x, int y) {
        return coRect.contains(x, y);
    }

    public void ensureCoPositive() {
        coRect = Shapes.toPositiveRect(coRect);
    }

    public void coCoordsChanged(View view) {
        recalcCo(view);
    }

    public void recalcCo(View view) {
        coRect = view.imageToComponentSpace(imRect);
    }

    public void imCoordsChanged(AffineTransform at, View view) {
        Point2D upperLeft = new Point2D.Double(
            imRect.getX(), imRect.getY());
        Point2D lowerRight = new Point2D.Double(
            imRect.getX() + imRect.getWidth(),
            imRect.getY() + imRect.getHeight());

        Point2D newUpperLeft = at.transform(upperLeft, null);
        Point2D newLowerRight = at.transform(lowerRight, null);

        imRect = new Rectangle2D.Double(newUpperLeft.getX(), newUpperLeft.getY(),
            newLowerRight.getX() - newUpperLeft.getX(),
            newLowerRight.getY() - newUpperLeft.getY());
        imRect = Shapes.toPositiveRect(imRect);

        recalcCo(view);
    }

    public void recalcIm(View view) {
        imRect = view.componentToImageSpace(coRect);
    }

    public boolean isEmpty() {
        return imRect.isEmpty();
    }

    @Override
    public String toString() {
        return "PRectangle{coRect=" + coRect + ", imRect=" + imRect + '}';
    }
}
