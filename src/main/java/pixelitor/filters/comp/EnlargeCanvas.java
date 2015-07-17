package pixelitor.filters.comp;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.filters.gui.RangeParam;
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
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.utils.SliderSpinner.TextPosition.BORDER;

public class EnlargeCanvas {
    private final Composition comp;
    private final int north;
    private final int east;
    private final int south;
    private final int west;

    public EnlargeCanvas(Composition comp, int north, int east, int south, int west) {
        this.comp = comp;
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public void invoke() {
        BufferedImage backupImage = null;

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ContentLayer) {
                if (layer instanceof ImageLayer) {
                    backupImage = ((ImageLayer) layer).getImage();
                }

                ContentLayer contentLayer = (ContentLayer) layer;
                contentLayer.enlargeCanvas(north, east, south, west);
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                mask.enlargeCanvas(north, east, south, west);
            }
        }

        SelectionChangeEdit selectionChangeEdit = null;
        Selection selection = comp.getSelectionOrNull();
        if (selection != null && (north > 0 || west > 0)) {
            Shape backupShape = selection.getShape();
            selection.transform(
                    AffineTransform.getTranslateInstance(west, north),
                    AddToHistory.NO);
            selectionChangeEdit = new SelectionChangeEdit(comp, backupShape, "");
            selectionChangeEdit.setEmbedded(true);
        }

        CanvasChangeEdit canvasChangeEdit = new CanvasChangeEdit("", comp);
        MultiLayerEdit edit = new MultiLayerEdit(comp, "Enlarge Canvas", backupImage, canvasChangeEdit);
        edit.setSelectionChangeEdit(selectionChangeEdit);
        History.addEdit(edit);

        Canvas canvas = comp.getCanvas();
        canvas.updateSize(canvas.getWidth() + east + west, canvas.getHeight() + north + south);

        // update the icon images only after the shared canvas size was
        // enlarged, because they are based on the canvas-sized subimage
        comp.updateAllIconImages();

        comp.imageChanged(REPAINT);
        comp.setDirty(true);
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
                Composition comp = ImageComponents.getActiveComp().get();
                new EnlargeCanvas(comp, panel.getNorth(), panel.getEast(), panel.getSouth(), panel.getWest()).invoke();
                close();
            }
        };
        d.setVisible(true);
    }

    static class EnlargeCanvasPanel extends JPanel {
        final RangeParam northRange = new RangeParam("North", 0, 500, 0);
        final RangeParam eastRange = new RangeParam("East", 0, 500, 0);
        final RangeParam southRange = new RangeParam("South", 0, 500, 0);
        final RangeParam westRange = new RangeParam("West", 0, 500, 0);

        private EnlargeCanvasPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            addSliderSpinner(northRange, "north");
            addSliderSpinner(eastRange, "east");
            addSliderSpinner(southRange, "south");
            addSliderSpinner(westRange, "west");
        }

        private void addSliderSpinner(RangeParam range, String sliderName) {
            SliderSpinner s = new SliderSpinner(range, BORDER, false);
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
