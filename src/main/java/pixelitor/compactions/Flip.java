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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;
import pixelitor.utils.Texts;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Flips (mirrors) all content layers of a composition
 * either horizontally or vertically.
 */
public class Flip extends SimpleCompAction {
    private final Flip.Direction direction;

    public Flip(Direction direction) {
        super(direction.getDisplayName(), false);
        this.direction = direction;
    }

    @Override
    protected void updateCanvasSize(Canvas newCanvas, View view) {
        // a flip doesn't change the canvas size
        throw new IllegalStateException("should not be called");
    }

    @Override
    protected String getEditName() {
        return direction.getDisplayName();
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.flip(direction);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return direction.createCanvasTransform(canvas);
    }

    @Override
    protected Guides createTransformedGuides(Guides srcGuides, View view, Canvas srcCanvas) {
        return srcGuides.copyFlipping(direction, view);
    }

    @Override
    protected String getStatusBarMessage() {
        return direction.getStatusBarMessage();
    }

    /**
     * The direction of the flip
     */
    public enum Direction {
        HORIZONTAL("flip_horizontal") {
            @Override
            public String getStatusBarMessage() {
                return "Image flipped horizontally";
            }

            @Override
            public AffineTransform createTransform(int width, int height) {
                var at = new AffineTransform();
                at.translate(width, 0); // keeps the content visible
                at.scale(-1, 1);
                return at;
            }
        }, VERTICAL("flip_vertical") {
            @Override
            public String getStatusBarMessage() {
                return "Image flipped vertically";
            }

            @Override
            public AffineTransform createTransform(int width, int height) {
                var at = new AffineTransform();
                at.translate(0, height); // keeps the content visible
                at.scale(1, -1);
                return at;
            }
        };

        private final String displayName;

        Direction(String uiKey) {
            this.displayName = Texts.i18n(uiKey);
        }

        public String getDisplayName() {
            return displayName;
        }

        public abstract String getStatusBarMessage();

        public abstract AffineTransform createTransform(int width, int height);

        /**
         * Returns the transformation in image space, relative to the canvas.
         * Needed for transforming the selection.
         */
        public AffineTransform createCanvasTransform(Canvas canvas) {
            return createTransform(canvas.getWidth(), canvas.getHeight());
        }

        /**
         * Returns the transformation for the image (image space, relative to the image).
         */
        public AffineTransform createImageTransform(BufferedImage image) {
            return createTransform(image.getWidth(), image.getHeight());
        }
    }
}
