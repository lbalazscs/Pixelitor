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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;
import pixelitor.utils.ImageUtils;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Rotates all content layers of a composition by 90, 180 or 270 degrees
 */
public class Rotate extends SimpleCompAction {
    private final SpecialAngle angle;

    public Rotate(SpecialAngle angle) {
        super(angle.getGUIName(), true);
        this.angle = angle;
    }

    @Override
    protected void changeCanvasSize(Canvas newCanvas, View view) {
        angle.changeCanvasSize(newCanvas, view);
    }

    @Override
    protected String getEditName() {
        return angle.getGUIName();
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.rotate(angle);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return angle.createCanvasTransform(canvas);
    }

    @Override
    protected Guides createGuidesCopy(Guides oldGuides, View view, Canvas oldCanvas) {
        return oldGuides.copyForRotate(angle, view);
    }

    @Override
    protected String getStatusBarMessage() {
        return "The image was rotated by " + angle.asString();
    }

    public enum SpecialAngle {
        ANGLE_90(90, "90\u00B0 CW") {
            @Override
            public void changeCanvasSize(Canvas canvas, View view) {
                // switch width and height
                int newWidth = canvas.getHeight();
                int newHeight = canvas.getWidth();
                canvas.changeSize(newWidth, newHeight, view);
            }

            @Override
            public AffineTransform createCanvasTransform(Canvas canvas) {
                // rotate, then translate to compensate
                var at = AffineTransform.getTranslateInstance(
                    canvas.getHeight(), 0);
                at.quadrantRotate(1);
                return at;
            }

            @Override
            public AffineTransform createImageTransform(BufferedImage image) {
                // rotate, then translate to compensate
                var at = AffineTransform.getTranslateInstance(
                    image.getHeight(), 0);
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
            public AffineTransform createCanvasTransform(Canvas canvas) {
                // rotate, then translate to compensate
                var at = AffineTransform.getTranslateInstance(
                    canvas.getWidth(), canvas.getHeight());
                at.quadrantRotate(2);
                return at;
            }

            @Override
            public AffineTransform createImageTransform(BufferedImage image) {
                // rotate, then translate to compensate
                var at = AffineTransform.getTranslateInstance(
                    image.getWidth(), image.getHeight());
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
            public AffineTransform createCanvasTransform(Canvas canvas) {
                // rotate, then translate to compensate
                var at = AffineTransform.getTranslateInstance(
                    0, canvas.getWidth());
                at.quadrantRotate(3);
                return at;
            }

            @Override
            public AffineTransform createImageTransform(BufferedImage image) {
                // rotate, then translate to compensate
                var at = AffineTransform.getTranslateInstance(
                    0, image.getWidth());
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

        protected final int angleDegree;
        private final String guiName;
        private final String asString;

        SpecialAngle(int angleDegree, String asString) {
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
        public abstract AffineTransform createCanvasTransform(Canvas canvas);

        /**
         * Returns the transformation of the image,
         * ignoring the canvas and the translation
         */
        public abstract AffineTransform createImageTransform(BufferedImage image);

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
}
