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

/**
 * Rotates an image
 */
public class Rotate extends CompAction {
    private final int angleDegree;

    private int newCanvasWidth;
    private int newCanvasHeight;

    public Rotate(int angleDegree, String name) {
        super(name, true);
        this.angleDegree = angleDegree;
    }

    @Override
    protected void changeCanvas(Composition comp) {
        Canvas canvas = comp.getCanvas();
        rotateCanvas(canvas);
        canvas.updateSize(newCanvasWidth, newCanvasHeight);
    }

    @Override
    protected String getUndoName() {
        return "Rotate";
    }

    @Override
    protected void applyTx(ContentLayer contentLayer, AffineTransform tx) {
        contentLayer.rotate(angleDegree, tx);
    }

    @Override
    protected AffineTransform createTransform(Canvas canvas) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        AffineTransform rotTx = new AffineTransform();
        if (angleDegree == 90) {
            rotTx.translate(canvasHeight, 0);
        } else if (angleDegree == 180) {
            rotTx.translate(canvasWidth, canvasHeight);
        } else if (angleDegree == 270) {
            rotTx.translate(0, canvasWidth);
        }
        // TODO rotate with exact transform
        rotTx.rotate(Math.toRadians(angleDegree));
        return rotTx;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void rotateCanvas(Canvas canvas) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        if (angleDegree == 90 || angleDegree == 270) {
            newCanvasWidth = canvasHeight;
            newCanvasHeight = canvasWidth;
        } else {
            newCanvasWidth = canvasWidth;
            newCanvasHeight = canvasHeight;
        }
    }
}
