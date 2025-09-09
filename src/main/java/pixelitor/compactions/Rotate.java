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
 * Rotates all content layers of a composition by 90, 180 or 270 degrees.
 */
public class Rotate extends SimpleCompAction {
    private final QuadrantAngle angle;

    public Rotate(QuadrantAngle angle) {
        super(angle.getDisplayName(), angle != QuadrantAngle.ANGLE_180);
        this.angle = angle;
    }

    @Override
    protected void updateCanvasSize(Canvas newCanvas, View view) {
        angle.resizeNewCanvas(newCanvas, view);
    }

    @Override
    protected String getEditName() {
        return angle.getDisplayName();
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.rotate(angle, false);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        return angle.createCanvasTransform(canvas);
    }

    @Override
    protected Guides createTransformedGuides(Guides srcGuides, View view, Canvas srcCanvas) {
        return srcGuides.copyRotated(angle, view);
    }

    @Override
    protected String getStatusBarMessage() {
        return "Image rotated by " + angle.asString();
    }
}
