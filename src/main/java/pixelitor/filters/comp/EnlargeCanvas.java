package pixelitor.filters.comp;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponents;
import pixelitor.filters.gui.RangeParam;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.Selection;
import pixelitor.utils.OKCancelDialog;
import pixelitor.utils.SliderSpinner;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;

import static pixelitor.Composition.ImageChangeActions.REPAINT;
import static pixelitor.utils.SliderSpinner.TextPosition.BORDER;

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

        Selection selection = comp.getSelectionOrNull();
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
                new EnlargeCanvas(panel.getNorth(), panel.getEast(), panel.getSouth(), panel.getWest()).process(comp);
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
