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
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.history.AddToHistory;
import pixelitor.history.CanvasChangeEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.Selection;

import javax.swing.*;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.REPAINT;

/**
 * An action that acts on a Composition
 */
public abstract class CompAction extends AbstractAction {
    private final boolean changesCanvasDimensions;

    CompAction(String name, boolean changesCanvasDimensions) {
        this.changesCanvasDimensions = changesCanvasDimensions;
        assert name != null;
        putValue(Action.NAME, name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveComp().get();

        CanvasChangeEdit canvasChangeEdit = null;
        if (changesCanvasDimensions) {
            canvasChangeEdit = new CanvasChangeEdit("", comp);
        }

        Canvas canvas = comp.getCanvas();
        AffineTransform tx = createTransform(canvas);

        Shape backupShape = null;
        if (comp.hasSelection()) {
            Selection selection = comp.getSelectionOrNull();
            backupShape = selection.getShape();

            selection.transform(tx, AddToHistory.NO);
        }

        // Saved before the change, but the edit is
        // created after the change.
        // This way no image copy is necessary.
        BufferedImage backupImage = null;

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                if (layer instanceof ImageLayer) {
                    backupImage = ((ImageLayer) layer).getImage();
                }
                ContentLayer contentLayer = (ContentLayer) layer;
                applyTx(contentLayer, tx);
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                applyTx(mask, tx);
            }
        }
        assert backupImage != comp.getAnyImageLayer().getImage();

        MultiLayerEdit edit = new MultiLayerEdit(comp, getUndoName(), backupImage,
                canvasChangeEdit);
        if (backupShape != null) {
            SelectionChangeEdit selectionChangeEdit = new SelectionChangeEdit(comp, backupShape, "");
            edit.setSelectionChangeEdit(selectionChangeEdit);
        }
        History.addEdit(edit);

        if (changesCanvasDimensions) {
            changeCanvas(comp);
            AppLogic.activeCompositionDimensionsChanged(comp);
        }

        // Only after the shared canvas size was updated
        comp.updateAllIconImages();

        comp.setDirty(true);
        comp.imageChanged(REPAINT);
    }

    protected abstract void changeCanvas(Composition comp);

    protected abstract String getUndoName();

    protected abstract void applyTx(ContentLayer contentLayer, AffineTransform tx);

    protected abstract AffineTransform createTransform(Canvas canvas);
}
