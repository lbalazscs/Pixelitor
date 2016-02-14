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

import java.awt.geom.AffineTransform;

/**
 * Flips a ContentLayer horizontally or vertically
 */
public class Flip extends SimpleCompAction {
    private final Flip.Direction direction;

    public Flip(Direction dir) {
        super(dir.getName(), false);
        direction = dir;
    }

    @Override
    protected void changeCanvas(Composition comp) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    protected String getEditName() {
        return direction.getName();
    }

    @Override
    protected void applyTx(ContentLayer contentLayer) {
        contentLayer.flip(direction);
    }

    @Override
    protected AffineTransform createTransform(Canvas canvas) {
        return direction.getCanvasTX(canvas);
    }

    /**
     * The direction of the flip
     */
    public enum Direction {
        HORIZONTAL {
            @Override
            public String getName() {
                return "Flip Horizontal";
            }

            @Override
            public AffineTransform getCanvasTX(Canvas canvas) {
                AffineTransform flipTx = new AffineTransform();
                flipTx.translate(canvas.getWidth(), 0);
                flipTx.scale(-1, 1);
                return flipTx;
            }

            @Override
            public AffineTransform getImageTX(ImageLayer layer) {
                AffineTransform flipTx = new AffineTransform();
                flipTx.translate(layer.getImage().getWidth(), 0);
                flipTx.scale(-1, 1);
                return flipTx;
            }
        }, VERTICAL {
            @Override
            public String getName() {
                return "Flip Vertical";
            }

            @Override
            public AffineTransform getCanvasTX(Canvas canvas) {
                AffineTransform flipTx = new AffineTransform();
                flipTx.translate(0, canvas.getHeight());
                flipTx.scale(1, -1);
                return flipTx;
            }

            @Override
            public AffineTransform getImageTX(ImageLayer layer) {
                AffineTransform flipTx = new AffineTransform();
                flipTx.translate(0, layer.getImage().getHeight());
                flipTx.scale(1, -1);
                return flipTx;
            }
        };

        public abstract String getName();

        /**
         * Returns the transformation in canvas space.
         * Needed for transforming the selection.
         */
        public abstract AffineTransform getCanvasTX(Canvas canvas);

        /**
         * Returns the transformation for the image.
         */
        public abstract AffineTransform getImageTX(ImageLayer layer);
    }
}
