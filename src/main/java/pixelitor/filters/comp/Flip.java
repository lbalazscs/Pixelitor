/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import java.awt.geom.AffineTransform;

import static pixelitor.filters.comp.Flip.Direction.HORIZONTAL;
import static pixelitor.filters.comp.Flip.Direction.VERTICAL;

/**
 * Flips a ContentLayer horizontally or vertically
 */
public class Flip extends CompAction {
    private final Flip.Direction direction;

    private static final Flip horizontalFlip = new Flip(HORIZONTAL);
    private static final Flip verticalFlip = new Flip(VERTICAL);

    public static Flip createFlip(Direction dir) {
        if(dir == HORIZONTAL) {
            return horizontalFlip;
        }
        if(dir == VERTICAL) {
            return verticalFlip;
        }
        throw new IllegalStateException("should not get here");
    }

    private Flip(Direction dir) {
        super(dir.getName(), false);
        direction = dir;
    }

    @Override
    protected void changeCanvas(Composition comp) {
        throw new IllegalStateException("should not be called");
    }

    @Override
    protected String getUndoName() {
        return "Flip";
    }

    @Override
    protected void applyTx(ContentLayer contentLayer, AffineTransform tx) {
        contentLayer.flip(direction, tx);
    }

    @Override
    protected AffineTransform createTransform(Canvas canvas) {
        AffineTransform flipTx = new AffineTransform();

        if (direction == HORIZONTAL) {
            flipTx.translate(canvas.getWidth(), 0);
            flipTx.scale(-1, 1);
        } else {
            flipTx.translate(0, canvas.getHeight());
            flipTx.scale(1, -1);
        }
        return flipTx;
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
        }, VERTICAL {
            @Override
            public String getName() {
                return "Flip Vertical";
            }
        };

        public abstract String getName();
    }
}
