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

package pixelitor.filters.comp;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.utils.ImageUtils;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Rotates all content layers of a composition by 90, 180 or 270 degrees
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
    protected AffineTransform createCanvasTX(Canvas canvas) {
        return angle.createCanvasTX(canvas);
    }

    public enum SpecialAngle {
        ANGLE_90(90, "Rotate 90\u00B0 CW") {
            @Override
            public void changeCanvas(Canvas canvas) {
                // switch width and height
                int newWidth = canvas.getImHeight();
                int newHeight = canvas.getImWidth();
                canvas.changeImSize(newWidth, newHeight);
            }

            @Override
            public AffineTransform createCanvasTX(Canvas canvas) {
                // rotate, then translate to compensate
                AffineTransform at = AffineTransform.getTranslateInstance(
                        canvas.getImHeight(), 0);
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

            @Override
            public AffineTransform createImageTX(ImageLayer layer) {
                // rotate, then translate to compensate
                AffineTransform at = AffineTransform.getTranslateInstance(
                        layer.getImage().getHeight(), 0);
                at.quadrantRotate(1);
                return at;
            }
        }, ANGLE_180(180, "Rotate 180\u00B0") {
            @Override
            public void changeCanvas(Canvas canvas) {
                // do nothing
            }

            @Override
            public AffineTransform createCanvasTX(Canvas canvas) {
                // rotate, then translate to compensate
                AffineTransform at = AffineTransform.getTranslateInstance(
                        canvas.getImWidth(), canvas.getImHeight());
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

            @Override
            public AffineTransform createImageTX(ImageLayer layer) {
                Canvas canvas = layer.getComp().getCanvas();
                AffineTransform transform = createCanvasTX(canvas);
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
            public AffineTransform createCanvasTX(Canvas canvas) {
                // rotate, then translate to compensate
                AffineTransform at = AffineTransform.getTranslateInstance(
                        0, canvas.getImWidth());
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

            @Override
            public AffineTransform createImageTX(ImageLayer layer) {
                // rotate, then translate to compensate
                AffineTransform at = AffineTransform.getTranslateInstance(
                        0, layer.getImage().getWidth());
                at.quadrantRotate(3);
                return at;
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
        public abstract AffineTransform createCanvasTX(Canvas canvas);

        /**
         * Return the transformation of the image
         */
        public abstract AffineTransform createImageTX(ImageLayer layer);

        public int getAngleDegree() {
            return angleDegree;
        }

        /**
         * Creates a new image with the appropriate size
         */
        public abstract BufferedImage createDestImage(BufferedImage img);
    }
}
