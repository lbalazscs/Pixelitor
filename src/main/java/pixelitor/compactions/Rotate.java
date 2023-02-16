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
import pixelitor.utils.QuadrantAngle;

import java.awt.geom.AffineTransform;

/**
 * Rotates all content layers of a composition by 90, 180 or 270 degrees
 */
public class Rotate extends SimpleCompAction {
    private final QuadrantAngle angle;

    public Rotate(QuadrantAngle angle) {
        super(angle.getGUIName(), true);
        this.angle = angle;
    }

    @Override
    protected void resizeNewCanvas(Canvas newCanvas, View view) {
        angle.resizeNewCanvas(newCanvas, view);
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
}
