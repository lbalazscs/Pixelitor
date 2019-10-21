/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.SelectionActions;

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
        Composition comp = OpenComps.getActiveCompOrNull();

        process(comp);
    }

    @Override
    public void process(Composition comp) {
//        MultiLayerBackup backup = new MultiLayerBackup(comp,
//                getEditName(), changesCanvasDimensions);
//        Canvas oldCanvas = comp.getCanvas();
        View view = comp.getView();
        Composition newComp = comp.createCopy(true, true);
        Canvas newCanvas = newComp.getCanvas();

        AffineTransform canvasTX = createCanvasTX(newCanvas);
        newComp.imCoordsChanged(canvasTX, false);

        newComp.forEachLayer(this::processLayer);

        if (changesCanvasDimensions) {
            changeCanvas(newCanvas, view);
        }

//        MultiLayerEdit edit = new MultiLayerEdit(getEditName(), comp, backup, canvasTX);
        History.addEdit(new CompositionReplacedEdit(
                getEditName(), view, comp, newComp));
        view.replaceComp(newComp);
        SelectionActions.setEnabled(newComp.hasSelection(), newComp);

        Guides guides = comp.getGuides();
        if (guides != null) {
            Guides newGuides = createGuidesCopy(guides, view);
            newComp.setGuides(newGuides);
        }


        // Only after the shared canvas size was updated
        newComp.updateAllIconImages();

        newComp.imageChanged(REPAINT, true);
        if (changesCanvasDimensions) {
            view.revalidate(); // make sure the scrollbars are OK
        }
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

    protected abstract void changeCanvas(Canvas canvas, View view);

    protected abstract String getEditName();

    /**
     * Applies the transformation to the given content layer.
     */
    protected abstract void applyTx(ContentLayer contentLayer);

    /**
     * Used to transform things that have coordinates relative to
     * the canvas, like the selection, paths, tool widgets.
     */
    protected abstract AffineTransform createCanvasTX(Canvas canvas);

    protected abstract Guides createGuidesCopy(Guides guides, View view);
}
