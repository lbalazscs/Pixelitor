/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.ImageComponents;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;

/**
 * A {@link CompAction} where the processing can be simplified
 * by using the template method pattern.
 */
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
        MultiLayerBackup backup = new MultiLayerBackup(comp,
                getEditName(), changesCanvasDimensions);

        Canvas canvas = comp.getCanvas();
        comp.transformSelection(() -> createCanvasTX(canvas));

        comp.forEachLayer(this::processLayer);

        MultiLayerEdit edit = new MultiLayerEdit(getEditName(), comp, backup);
        History.addEdit(edit);

        if (changesCanvasDimensions) {
            changeCanvas(comp);
        }

        // Only after the shared canvas size was updated
        comp.updateAllIconImages();

        comp.imageChanged(REPAINT, true);
    }

    private void processLayer(Layer layer) {
        if (layer instanceof ContentLayer) {
            ContentLayer contentLayer = (ContentLayer) layer;
            applyTx(contentLayer);
        }
        if (layer.hasMask()) {
            LayerMask mask = layer.getMask();
            applyTx(mask);
        }
    }

    protected abstract void changeCanvas(Composition comp);

    protected abstract String getEditName();

    protected abstract void applyTx(ContentLayer contentLayer);

    protected abstract AffineTransform createCanvasTX(Canvas canvas);
}
