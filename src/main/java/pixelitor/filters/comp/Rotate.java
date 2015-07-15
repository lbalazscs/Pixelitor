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
import pixelitor.history.CanvasChangeEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.REPAINT;

/**
 * Rotates an image
 */
public class Rotate extends CompAction {
    private final int angleDegree;

    private int newCanvasWidth;
    private int newCanvasHeight;

    public Rotate(int angleDegree, String name) {
        super(name);
        this.angleDegree = angleDegree;
    }

    @Override
    public void transform(Composition comp) {

        int nrLayers = comp.getNrLayers();

        int canvasWidth = comp.getCanvasWidth();
        int canvasHeight = comp.getCanvasHeight();

        BufferedImage backupImage = null;
        CanvasChangeEdit canvasChangeEdit = new CanvasChangeEdit("", comp);

        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                if (layer instanceof ImageLayer) {
                    backupImage = ((ImageLayer) layer).getImage();
                }
                ContentLayer contentLayer = (ContentLayer) layer;
                contentLayer.rotate(angleDegree);
            }
            if (layer.hasMask()) {
                layer.getMask().rotate(angleDegree);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, "Rotate", backupImage,
                canvasChangeEdit);
        History.addEdit(edit);

        if (!comp.hasSelection()) {
            rotateCanvas(canvasWidth, canvasHeight);
            comp.getCanvas().updateSize(newCanvasWidth, newCanvasHeight);
        }

        // Only after the shared canvas size was updated
        comp.updateAllIconImages();

        comp.setDirty(true);
        comp.imageChanged(REPAINT);

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
