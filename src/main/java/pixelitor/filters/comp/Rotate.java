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

import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.history.OneLayerUndoableEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;

/**
 * Rotates an image
 */
public class Rotate extends CompOperation {
    private final int angleDegree;

    private int newCanvasWidth;
    private int newCanvasHeight;

    public Rotate(int angleDegree, String name) {
        super(name);
        this.angleDegree = angleDegree;
    }

    @Override
    public void transform(Composition comp) {

        OneLayerUndoableEdit.createAndAddToHistory(comp, "Rotate", true, false);
        int nrLayers = comp.getNrLayers();

        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;
                contentLayer.rotate(angleDegree);
            }
        }

        if (!comp.hasSelection()) {
            rotateCanvas(canvasWidth, canvasHeight);
            comp.getCanvas().updateSize(newCanvasWidth, newCanvasHeight);
        }

        comp.setDirty(true);
        comp.imageChanged(true, false);

        AppLogic.activeCompositionDimensionsChanged(comp);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void rotateCanvas(int canvasWidth, int canvasHeight) {
        if (angleDegree == 90 || angleDegree == 270) {
            newCanvasWidth = canvasHeight;
            newCanvasHeight = canvasWidth;
        } else {
            newCanvasWidth = canvasWidth;
            newCanvasHeight = canvasHeight;
        }
    }
}
