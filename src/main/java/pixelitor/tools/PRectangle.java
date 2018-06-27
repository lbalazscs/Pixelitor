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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Similar in spirit to {@link PPoint}, this represents
 * a rectangle both in component and image space.
 */
public class PRectangle {
    private Rectangle coRect;
    private Rectangle2D imRect;

    private PRectangle(Rectangle coRect, Rectangle2D imRect) {
        this.coRect = coRect;
        this.imRect = imRect;
    }

    /**
     * Creates a {@link PRectangle} from a component-space input
     */
    public static PRectangle fromCo(Rectangle coRect, ImageComponent ic) {
        Rectangle2D imRect = ic.fromComponentToImageSpace(coRect);
        return new PRectangle(coRect, imRect);
    }

    /**
     * Creates a positive {@link PRectangle} from a component-space input
     */
    public static PRectangle positiveFromCo(Rectangle coRect, ImageComponent ic) {
        coRect = Shapes.toPositiveRect(coRect);
        return fromCo(coRect, ic);
    }

    /**
     * Creates a {@link PRectangle} from an image-space input
     */
    public static PRectangle fromIm(Rectangle2D imRect, ImageComponent ic) {
        Rectangle coRect = ic.fromImageToComponentSpace(imRect);
        return new PRectangle(coRect, imRect);
    }

    /**
     * Creates a positive {@link PRectangle} from an image-space input
     */
    public static PRectangle positiveFromIm(Rectangle2D imRect, ImageComponent ic) {
        imRect = Shapes.toPositiveRect(imRect);
        return fromIm(imRect, ic);
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

    public void makeCoPositive() {
        coRect = Shapes.toPositiveRect(coRect);
    }

    public void icSizeChanged(ImageComponent ic) {
        recalcCo(ic);
    }

    public void recalcCo(ImageComponent ic) {
        coRect = ic.fromImageToComponentSpace(imRect);
    }

    public void recalcIm(ImageComponent ic) {
        imRect = ic.fromComponentToImageSpace(coRect);
    }
}
