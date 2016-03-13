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

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.filters.gui.AddDefaultButton;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.ImageComponents;
import pixelitor.gui.utils.OKCancelDialog;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.Selection;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.BORDER;

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
    public void process(Composition comp) {
        String editName = "Enlarge Canvas";
        MultiLayerBackup backup = new MultiLayerBackup(comp, editName, true);

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                ContentLayer contentLayer = (ContentLayer) layer;
                contentLayer.enlargeCanvas(north, east, south, west);
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.enlargeCanvas(north, east, south, west);
            }
        }

        Selection selection = comp.getSelection();
        if (selection != null && (north > 0 || west > 0)) {
            selection.transform(
                    AffineTransform.getTranslateInstance(west, north));
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, editName, backup);
        History.addEdit(edit);

        Canvas canvas = comp.getCanvas();
        int newCanvasWidth = canvas.getWidth() + east + west;
        int newCanvasHeight = canvas.getHeight() + north + south;
        canvas.updateSize(newCanvasWidth, newCanvasHeight);

        // update the icon images only after the shared canvas size was
        // enlarged, because they are based on the canvas-sized subimage
        comp.updateAllIconImages();

        comp.imageChanged(REPAINT, true);

        Messages.showStatusMessage("Canvas enlarged to "
                + newCanvasWidth + " x " + newCanvasHeight + " pixels.");
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
        EnlargeCanvasPanel panel = new EnlargeCanvasPanel();
        OKCancelDialog d = new OKCancelDialog(panel, "Enlarge Canvas") {
            @Override
            protected void dialogAccepted() {
                Composition comp = ImageComponents.getActiveCompOrNull();
                new EnlargeCanvas(panel.getNorth(), panel.getEast(), panel.getSouth(), panel.getWest()).process(comp);
                close();
            }
        };
        d.setVisible(true);
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
            SliderSpinner s = new SliderSpinner(range, BORDER, AddDefaultButton.NO);
            s.setSliderName(sliderName);
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
