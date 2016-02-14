/*
 * Copyright 2016 Laszlo Balazs-Csiki
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

package pixelitor.filters.comp;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Rotates an image
 */
public class Rotate extends SimpleCompAction {
    private final SpecialAngle angle;

    public Rotate(SpecialAngle angle) {
        super(angle.getName(), true);
        this.angle = angle;
    }

    @Override
    protected void changeCanvas(Composition comp) {
        Canvas canvas = comp.getCanvas();
        angle.changeCanvas(canvas);
//
//        rotateCanvas(canvas);
//        canvas.updateSize(newCanvasWidth, newCanvasHeight);
    }

    @Override
    protected String getEditName() {
        return angle.getName();
    }

    @Override
    protected void applyTx(ContentLayer contentLayer) {
        contentLayer.rotate(angle);
    }

    @Override
    protected AffineTransform createTransform(Canvas canvas) {
        return angle.getCanvasTX(canvas);
    }

    public enum SpecialAngle {
        ANGLE_90(90, "Rotate 90\u00B0 CW") {
            @SuppressWarnings("SuspiciousNameCombination")
            @Override
            public void changeCanvas(Canvas canvas) {
                int canvasWidth = canvas.getWidth();
                int canvasHeight = canvas.getHeight();
                int newCanvasWidth = canvasHeight;
                int newCanvasHeight = canvasWidth;
                canvas.updateSize(newCanvasWidth, newCanvasHeight);
            }

            @Override
            public AffineTransform getCanvasTX(Canvas canvas) {
                // rotate, then translate to compensate
                AffineTransform rotTx = AffineTransform.getTranslateInstance(
                        canvas.getHeight(), 0);
                rotTx.quadrantRotate(1);
                return rotTx;
            }

            @Override
            public BufferedImage createDestImage(BufferedImage image) {
                // switch width and height
                int newImageWidth = image.getHeight();
                int newImageHeight = image.getWidth();

                return ImageUtils.createImageWithSameColorModel(image, newImageWidth, newImageHeight);
            }

            @Override
            public AffineTransform getImageTX(ImageLayer layer) {
                // rotate, then translate to compensate
                AffineTransform rotTx = AffineTransform.getTranslateInstance(
                        layer.getImage().getHeight(), 0);
                rotTx.quadrantRotate(1);
                return rotTx;
            }
        }, ANGLE_180(180, "Rotate 180\u00B0") {
            @Override
            public void changeCanvas(Canvas canvas) {
                // do nothing
            }

            @Override
            public AffineTransform getCanvasTX(Canvas canvas) {
                // rotate, then translate to compensate
                AffineTransform rotTx = AffineTransform.getTranslateInstance(
                        canvas.getWidth(), canvas.getHeight());
                rotTx.quadrantRotate(2);
                return rotTx;
            }

            @Override
            public BufferedImage createDestImage(BufferedImage image) {
                // no change in width and height
                int newImageWidth = image.getWidth();
                int newImageHeight = image.getHeight();

                return ImageUtils.createImageWithSameColorModel(image, newImageWidth, newImageHeight);
            }

            @Override
            public AffineTransform getImageTX(ImageLayer layer) {
                Canvas canvas = layer.getComp().getCanvas();
                AffineTransform transform = getCanvasTX(canvas);
                int tx = layer.getTX();
                int ty = layer.getTY();
                transform.translate(tx, ty);
                return transform;
            }
        }, ANGLE_270(270, "Rotate 90\u00B0 CCW") {
            @Override
            public void changeCanvas(Canvas canvas) {
                // same as for 90
                ANGLE_90.changeCanvas(canvas);
            }

            @Override
            public AffineTransform getCanvasTX(Canvas canvas) {
                // rotate, then translate to compensate
                AffineTransform rotTx = AffineTransform.getTranslateInstance(
                        0, canvas.getWidth());
                rotTx.quadrantRotate(3);
                return rotTx;
            }

            @Override
            public BufferedImage createDestImage(BufferedImage image) {
                // switch width and height
                int newImageWidth = image.getHeight();
                int newImageHeight = image.getWidth();

                return ImageUtils.createImageWithSameColorModel(image, newImageWidth, newImageHeight);
            }

            @Override
            public AffineTransform getImageTX(ImageLayer layer) {
                // rotate, then translate to compensate
                AffineTransform rotTx = AffineTransform.getTranslateInstance(
                        0, layer.getImage().getWidth());
                rotTx.quadrantRotate(3);
                return rotTx;
            }
        };

        protected final int angleDegree;
        private final String name;

        SpecialAngle(int angleDegree, String name) {
            this.angleDegree = angleDegree;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract void changeCanvas(Canvas canvas);

        /**
         * Returns the transformation in canvas space.
         * Needed for transforming the selection.
         */
        public abstract AffineTransform getCanvasTX(Canvas canvas);

        /**
         * Return the transformation of the image
         */
        public abstract AffineTransform getImageTX(ImageLayer layer);

        public int getAngleDegree() {
            return angleDegree;
        }

        public abstract BufferedImage createDestImage(BufferedImage image);
    }
}
