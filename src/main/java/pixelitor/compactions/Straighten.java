/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.Composition;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.layers.ContentLayer;

import java.awt.geom.AffineTransform;
import java.util.concurrent.CompletableFuture;

/**
 * Rotates all layers around the canvas center by an arbitrary angle.
 */
public class Straighten extends SimpleCompAction {
    public static final String NAME = "Straighten";

    private final double angleDegrees;
    private final double angleRadians;

    public Straighten(double angleDegrees) {
        super(NAME, false);
        this.angleDegrees = angleDegrees;
        this.angleRadians = Math.toRadians(angleDegrees);
    }

    @Override
    public CompletableFuture<Composition> process(Composition srcComp) {
        if (angleDegrees == 0.0) {
            // don't add an unnecessary history step
            return CompletableFuture.completedFuture(srcComp);
        }
        return super.process(srcComp);
    }

    @Override
    public boolean disableForSmartObjects() {
        return true;
    }

    @Override
    protected boolean disableForTextLayers() {
        return true;
    }

    @Override
    protected void updateCanvasSize(Canvas newCanvas, View view) {
        // no-op, straighten keeps the canvas size unchanged
    }

    @Override
    protected String getEditName() {
        return NAME;
    }

    @Override
    protected void transform(ContentLayer contentLayer) {
        contentLayer.rotate(angleRadians, false);
    }

    @Override
    protected AffineTransform createCanvasTransform(Canvas canvas) {
        var center = canvas.getImCenter();
        return AffineTransform.getRotateInstance(
            angleRadians, center.getX(), center.getY());
    }

    @Override
    protected Guides createTransformedGuides(Guides srcGuides, View view, Canvas srcCanvas) {
        // guides don't support arbitrary-angle guide transforms
        return srcGuides.copyIdentical(view);
    }

    @Override
    protected String getStatusBarMessage() {
        return String.format("Image straightened by %.2f°", angleDegrees);
    }
}
