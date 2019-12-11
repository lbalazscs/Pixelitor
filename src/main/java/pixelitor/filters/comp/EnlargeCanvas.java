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
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.OpenComps;
import pixelitor.gui.View;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.util.concurrent.CompletableFuture;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

/**
 * Enlarges the canvas for all layers of a composition
 */
public class EnlargeCanvas implements CompAction {
    private final int north;
    private final int east;
    private final int south;
    private final int west;

    public EnlargeCanvas(int north, int east, int south, int west) {
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    @Override
    public CompletableFuture<Composition> process(Composition comp) {
        View view = comp.getView();
        Guides oldGuides = comp.getGuides();
        Guides newGuides = null;
        if (oldGuides != null) {
            newGuides = oldGuides.copyForEnlargedCanvas(north, east, south, west, view);
            comp.setGuides(newGuides);
        }

        Composition newComp = comp.createCopy(true, true);
        Canvas newCanvas = newComp.getCanvas();

        newComp.forEachLayer(this::processLayer);

        AffineTransform canvasTx = null;
        if (north > 0 || west > 0) {
            canvasTx = AffineTransform.getTranslateInstance(west, north);
            newComp.imCoordsChanged(canvasTx, false);
        }

        int newCanvasWidth = newCanvas.getImWidth() + east + west;
        int newCanvasHeight = newCanvas.getImHeight() + north + south;
        newCanvas.changeImSize(newCanvasWidth, newCanvasHeight, view);

        // update the icon images only after the shared canvas size was
        // enlarged, because they are based on the canvas-sized subimage
        comp.updateAllIconImages();

        History.addEdit(new CompositionReplacedEdit(
                "Enlarge Canvas", false, view, comp, newComp, canvasTx));
        view.replaceComp(newComp);
        SelectionActions.setEnabled(newComp.hasSelection(), newComp);

        comp.imageChanged(REPAINT, true);

        Messages.showInStatusBar("Canvas enlarged to "
                + newCanvasWidth + " x " + newCanvasHeight + " pixels.");
        return CompletableFuture.completedFuture(newComp);
    }

    private void processLayer(Layer layer) {
        if (layer instanceof ContentLayer) {
            ContentLayer contentLayer = (ContentLayer) layer;
            contentLayer.enlargeCanvas(north, east, south, west);
        }
        if (layer.hasMask()) {
            LayerMask mask = layer.getMask();
            mask.enlargeCanvas(north, east, south, west);
        }
    }

    public static Action getAction() {
        return new AbstractAction("Enlarge Canvas...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInDialog();
            }
        };
    }

    private static void showInDialog() {
        EnlargeCanvasPanel p = new EnlargeCanvasPanel();
        new DialogBuilder()
                .title("Enlarge Canvas")
                .content(p)
                .okAction(() -> {
                    Composition comp = OpenComps.getActiveCompOrNull();
                    new EnlargeCanvas(p.getNorth(), p.getEast(), p.getSouth(), p.getWest())
                            .process(comp);
                })
                .show();
    }

    static class EnlargeCanvasPanel extends JPanel {
        final RangeParam northRange = new RangeParam("North", 0, 0, 500);
        final RangeParam eastRange = new RangeParam("East", 0, 0, 500);
        final RangeParam southRange = new RangeParam("South", 0, 0, 500);
        final RangeParam westRange = new RangeParam("West", 0, 0, 500);

        private EnlargeCanvasPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            addSliderSpinner(northRange, "north");
            addSliderSpinner(eastRange, "east");
            addSliderSpinner(southRange, "south");
            addSliderSpinner(westRange, "west");
        }

        private void addSliderSpinner(RangeParam range, String sliderName) {
            SliderSpinner s = new SliderSpinner(range, BORDER, false);
            s.setName(sliderName);
            add(s);
        }

        public int getNorth() {
            return northRange.getValue();
        }

        public int getSouth() {
            return southRange.getValue();
        }

        public int getWest() {
            return westRange.getValue();
        }

        public int getEast() {
            return eastRange.getValue();
        }
    }
}
