/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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

import java.awt.geom.AffineTransform;

/**
 * Flips (mirrors) all content layers of a composition
 * either horizontally or vertically.
 */
public class Flip extends SimpleCompAction {
    private final FlipDirection direction;

    public Flip(FlipDirection direction) {
        super(direction.getDisplayName(), false);
        this.direction = direction;
    }

    @Override
    public boolean disableForSmartObjects() {
        return false;
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
        contentLayer.flip(direction, false);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return direction.createCanvasTransform(canvas);
    }

    @Override
    protected Guides createTransformedGuides(Guides srcGuides, View view, Canvas srcCanvas) {
        return srcGuides.copyFlipped(direction, view);
    }

    @Override
    protected String getStatusBarMessage() {
        return direction.getStatusBarMessage();
    }
}
