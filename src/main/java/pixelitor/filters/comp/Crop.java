package pixelitor.filters.comp;

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.ImageDisplay;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.Layer;

import java.awt.Dimension;
import java.awt.Rectangle;

import static pixelitor.Composition.ImageChangeActions.FULL;

public class Crop implements CompAction {
    private Rectangle cropRect;
    private final boolean selectionCrop;
    private final boolean allowGrowing;

    public Crop(Rectangle cropRect, boolean selectionCrop, boolean allowGrowing) {
        this.cropRect = cropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
    }

    public void process(Composition comp) {
        Canvas canvas = comp.getCanvas();
        if (!allowGrowing) {
            cropRect = cropRect.intersection(canvas.getBounds());
        }

        if (cropRect.isEmpty()) {
            // empty selection, can't do anything useful
            return;
        }

        MultiLayerBackup backup = new MultiLayerBackup(comp, "Crop", true);

        if (selectionCrop) {
            assert comp.hasSelection();
            comp.deselect(AddToHistory.NO);
        } else {
            // if this is a crop started from the crop tool
            // we still could have a selection that needs to be
            // cropped
            comp.cropSelection(cropRect);
        }

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            layer.crop(cropRect);
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, "Crop", backup);
        History.addEdit(edit);

        canvas.updateSize(cropRect.width, cropRect.height);
        comp.updateAllIconImages();
        comp.setDirty(true);

        ImageDisplay display = comp.getIC();
        if (display instanceof ImageComponent) { // not in a test
            ImageComponent ic = (ImageComponent) display;

            ic.setPreferredSize(new Dimension(cropRect.width, cropRect.height));
            ic.revalidate();
            ic.makeSureItIsVisible();

            ic.updateDrawStart();
        }
        comp.imageChanged(FULL);

        AppLogic.activeCompSizeChanged(comp);
    }
}
