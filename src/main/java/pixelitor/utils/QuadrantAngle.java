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

package pixelitor.utils;

import pixelitor.Canvas;
import pixelitor.gui.View;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * A rotation by a given number of quadrants.
 */
public enum QuadrantAngle {
    ANGLE_90(90, "90\u00B0 CW") {
        @Override
        public void changeCanvasSize(Canvas canvas, View view) {
            // switch width and height
            int newWidth = canvas.getHeight();
            int newHeight = canvas.getWidth();
            canvas.changeSize(newWidth, newHeight, view, false);
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            // rotate, then translate to compensate
            var at = AffineTransform.getTranslateInstance(height, 0);
            at.quadrantRotate(1);
            return at;
        }

        @Override
        public BufferedImage createDestImage(BufferedImage img) {
            // switch width and height
            int newWidth = img.getHeight();
            int newHeight = img.getWidth();

            return ImageUtils.createImageWithSameCM(img, newWidth, newHeight);
        }
    }, ANGLE_180(180, "180\u00B0") {
        @Override
        public void changeCanvasSize(Canvas canvas, View view) {
            // do nothing
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            // rotate, then translate to compensate
            var at = AffineTransform.getTranslateInstance(width, height);
            at.quadrantRotate(2);
            return at;
        }

        @Override
        public BufferedImage createDestImage(BufferedImage img) {
            // no change in width and height
            int newWidth = img.getWidth();
            int newHeight = img.getHeight();

            return ImageUtils.createImageWithSameCM(img, newWidth, newHeight);
        }
    }, ANGLE_270(270, "90\u00B0 CCW") {
        @Override
        public void changeCanvasSize(Canvas canvas, View view) {
            // same as for 90
            ANGLE_90.changeCanvasSize(canvas, view);
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            // rotate, then translate to compensate
            var at = AffineTransform.getTranslateInstance(0, width);
            at.quadrantRotate(3);
            return at;
        }

        @Override
        public BufferedImage createDestImage(BufferedImage img) {
            // switch width and height
            int newWidth = img.getHeight();
            int newHeight = img.getWidth();

            return ImageUtils.createImageWithSameCM(img, newWidth, newHeight);
        }
    };

    private final int angleDegree;
    private final String guiName;
    private final String asString;

    QuadrantAngle(int angleDegree, String asString) {
        this.angleDegree = angleDegree;
        this.guiName = "Rotate " + asString;
        this.asString = asString;
    }

    public String getGUIName() {
        return guiName;
    }

    public abstract void changeCanvasSize(Canvas canvas, View view);

    /**
     * Returns the rotation as a transform in
     * image-space coordinates relative to the canvas
     */
    public AffineTransform createCanvasTransform(Canvas canvas) {
        return createTransform(canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Returns the transformation of the image,
     * ignoring the canvas and the translation
     */
    public AffineTransform createImageTransform(BufferedImage image) {
        return createTransform(image.getWidth(), image.getHeight());
    }

    public abstract AffineTransform createTransform(int width, int height);

    public int getAngleDegree() {
        return angleDegree;
    }

    public String asString() {
        return asString;
    }

    /**
     * Creates a new image with the appropriate size
     */
    public abstract BufferedImage createDestImage(BufferedImage img);
}
