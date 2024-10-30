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

package pixelitor.utils;

import pixelitor.Canvas;
import pixelitor.gui.View;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Represents rotation angles by a specific number of quadrants (90-degree increments).
 */
public enum QuadrantAngle {
    ANGLE_90(90, "rotate_90") {
        @Override
        public void resizeNewCanvas(Canvas canvas, View view) {
            swapCanvasDimensions(canvas, view);
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            var at = AffineTransform.getTranslateInstance(height, 0);
            at.quadrantRotate(1);
            return at;
        }

        @Override
        public BufferedImage createDestImage(BufferedImage img) {
            return createSwappedDimensionsImage(img);
        }
    }, ANGLE_180(180, "rotate_180") {
        @Override
        public void resizeNewCanvas(Canvas canvas, View view) {
            // no resize needed
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            var at = AffineTransform.getTranslateInstance(width, height);
            at.quadrantRotate(2);
            return at;
        }

        @Override
        public BufferedImage createDestImage(BufferedImage img) {
            return createSameDimensionsImage(img);
        }
    }, ANGLE_270(270, "rotate_270") {
        @Override
        public void resizeNewCanvas(Canvas canvas, View view) {
            swapCanvasDimensions(canvas, view);
        }

        @Override
        public AffineTransform createTransform(int width, int height) {
            var at = AffineTransform.getTranslateInstance(0, width);
            at.quadrantRotate(3);
            return at;
        }

        @Override
        public BufferedImage createDestImage(BufferedImage img) {
            return createSwappedDimensionsImage(img);
        }
    };

    private final int angleDegree;
    private final String guiName;

    QuadrantAngle(int angleDegree, String guiKey) {
        this.angleDegree = angleDegree;
        this.guiName = Texts.i18n(guiKey);
    }

    public String getGUIName() {
        return guiName;
    }

    /**
     * Adjusts the canvas size for the rotation if necessary.
     */
    public abstract void resizeNewCanvas(Canvas canvas, View view);

    /**
     * Returns the rotation as a transformation in
     * image-space coordinates relative to the canvas.
     */
    public AffineTransform createCanvasTransform(Canvas canvas) {
        return createTransform(canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Returns the transformation of the image,
     * ignoring the canvas and the translation.
     */
    public AffineTransform createImageTransform(BufferedImage image) {
        return createTransform(image.getWidth(), image.getHeight());
    }

    public abstract AffineTransform createTransform(int width, int height);

    public int getAngleDegree() {
        return angleDegree;
    }

    /**
     * Creates a new image with dimensions appropriate for this rotation.
     */
    public abstract BufferedImage createDestImage(BufferedImage img);

    public String asString() {
        return angleDegree + "°";
    }

    /**
     * Swaps the width and height of a Canvas.
     */
    private static void swapCanvasDimensions(Canvas canvas, View view) {
        // swap the width and height
        int newWidth = canvas.getHeight();
        int newHeight = canvas.getWidth();
        canvas.resize(newWidth, newHeight, view, false);
    }

    /**
     * Creates a new image with swapped width and height, preserving color model.
     */
    private static BufferedImage createSwappedDimensionsImage(BufferedImage src) {
        return ImageUtils.createImageWithSameCM(
            src, src.getHeight(), src.getWidth());
    }

    /**
     * Creates a new image with the same dimensions, preserving color model.
     */
    private static BufferedImage createSameDimensionsImage(BufferedImage src) {
        return ImageUtils.createImageWithSameCM(
            src, src.getWidth(), src.getHeight());
    }
}
