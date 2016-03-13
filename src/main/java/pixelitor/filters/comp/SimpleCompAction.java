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

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.Selection;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;


public abstract class SimpleCompAction extends AbstractAction implements CompAction {
    private final boolean changesCanvasDimensions;

    SimpleCompAction(String name, boolean changesCanvasDimensions) {
        this.changesCanvasDimensions = changesCanvasDimensions;
        assert name != null;
        putValue(Action.NAME, name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Composition comp = ImageComponents.getActiveCompOrNull();

        process(comp);
    }

    @Override
    public void process(Composition comp) {
        MultiLayerBackup backup = new MultiLayerBackup(comp, getEditName(), changesCanvasDimensions);

        Canvas canvas = comp.getCanvas();
        AffineTransform canvasTX = createTransform(canvas);

        if (comp.hasSelection()) {
            Selection selection = comp.getSelection();
            selection.transform(canvasTX);
        }

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;
                applyTx(contentLayer);
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                applyTx(mask);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, getEditName(), backup);
        History.addEdit(edit);

        if (changesCanvasDimensions) {
            changeCanvas(comp);
            AppLogic.activeCompSizeChanged(comp);
        }

        // Only after the shared canvas size was updated
        comp.updateAllIconImages();

        comp.imageChanged(REPAINT, true);
    }

    protected abstract void changeCanvas(Composition comp);

    protected abstract String getEditName();

    protected abstract void applyTx(ContentLayer contentLayer);

    protected abstract AffineTransform createTransform(Canvas canvas);
}
