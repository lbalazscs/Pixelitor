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
 * Flips all content layers of a composition horizontally or vertically
 */
public class Flip extends SimpleCompAction {
    private final Direction direction;

    public Flip(Direction dir) {
        super(dir.getGUIName(), false);
        direction = dir;
    }

    @Override
    protected void resizeNewCanvas(Canvas newCanvas, View view) {
        // a flip doesn't change the canvas size
        throw new IllegalStateException("should not be called");
    }

    @Override
    protected String getEditName() {
        return direction.getGUIName();
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
    protected Guides createGuidesCopy(Guides oldGuides, View view, Canvas oldCanvas) {
        return oldGuides.copyForFlip(direction, view);
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
                return "The image was flipped horizontally";
            }

            @Override
            public AffineTransform createTransform(int width, int height) {
                var at = new AffineTransform();
                at.translate(width, 0);
                at.scale(-1, 1);
                return at;
            }
        }, VERTICAL("flip_vertical") {
            @Override
            public String getStatusBarMessage() {
                return "The image was flipped vertically";
            }

            @Override
            public AffineTransform createTransform(int width, int height) {
                var at = new AffineTransform();
                at.translate(0, height);
                at.scale(1, -1);
                return at;
            }
        };

        private final String guiName;

        Direction(String guiKey) {
            this.guiName = Texts.i18n(guiKey);
        }

        public String getGUIName() {
            return guiName;
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
