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

import pixelitor.Canvas;
import pixelitor.gui.ImageComponent;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;

/**
 * The "Pixelitor Point" represents a point on an image both in
 * component (mouse) coordinates and image coordinates.
 * <p>
 * Component coordinates are relative to the {@link ImageComponent},
 * image coordinates are relative to the {@link Canvas} (not necessarily
 * to the BufferedImage, as the image can be bigger than the canvas) and
 * take the position of the {@link Canvas} within the
 * {@link ImageComponent} and the image zooming into account.
 */
public abstract class PPoint {
    ImageComponent ic;

    // All the coordinates are initialized in subclasses
    // coordinates in image space
    protected double imX;
    protected double imY;
    // coordinates in component (MouseEvent) space
    protected int coX;
    protected int coY;

    protected PPoint(ImageComponent ic) {
        assert ic != null;
        this.ic = ic;
    }

    /**
     * Returns the x coordinate in component space
     */
    public int getCoX() {
        return coX;
    }

    /**
     * Returns the y coordinate in component space
     */
    public int getCoY() {
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

    public ImageComponent getIC() {
        return ic;
    }

    public PPoint mirrorVertically(int compWidth) {
        return new PPoint.Image(ic, compWidth - getImX(), getImY());
    }

    public PPoint mirrorHorizontally(int compHeight) {
        return new PPoint.Image(ic, getImX(), compHeight - getImY());
    }

    public PPoint mirrorBoth(int compWidth, int compHeight) {
        return new PPoint.Image(ic, compWidth - getImX(), compHeight - getImY());
    }

    public void drawLineTo(PPoint end, Graphics2D g) {
        Line2D.Double line = new Line2D.Double(getImX(), getImY(), end.getImX(), end.getImY());
        g.draw(line);
    }

    /**
     * A lazy {@link PPoint}, which converts component
     * space coordinates to image space coordinates only
     * on demand
     */
    public static class Lazy extends PPoint {
        private boolean xConverted = false;
        private boolean yConverted = false;

        public Lazy(ImageComponent ic, int x, int y) {
            super(ic);
            coX = x;
            coY = y;
            // image space coordinates are not uet initialized
        }

        @Override
        public double getImX() {
            if (!xConverted) {
                imX = ic.componentXToImageSpace(coX);
                xConverted = true;
            }
            return imX;
        }

        @Override
        public double getImY() {
            if (!yConverted) {
                imY = ic.componentYToImageSpace(coY);
                yConverted = true;
            }
            return imY;
        }
    }

    /**
     * An eager {@link PPoint}, which converts component
     * space coordinates to image space coordinates immediately
     */
    public static class Eager extends PPoint {
        public Eager(ImageComponent ic, int x, int y) {
            super(ic);
            coX = x;
            coY = y;
            imX = ic.componentXToImageSpace(coX);
            imY = ic.componentYToImageSpace(coY);
        }
    }

    /**
     * A {@link PPoint} eagerly initialized with image-space coordinates
     */
    public static class Image extends PPoint {
        public Image(ImageComponent ic, double imX, double imY) {
            super(ic);
            this.imX = imX;
            this.imY = imY;
            this.coX = (int) ic.imageXToComponentSpace(imX);
            this.coY = (int) ic.imageYToComponentSpace(imY);
        }
    }
}
