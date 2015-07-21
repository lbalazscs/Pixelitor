package pixelitor.filters.comp;

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.history.AddToHistory;
import pixelitor.history.History;
import pixelitor.history.MultiLayerBackup;
import pixelitor.history.MultiLayerEdit;
import pixelitor.layers.Layer;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import static pixelitor.Composition.ImageChangeActions.FULL;

public class Crop extends AbstractAction {
    private Composition comp;
    private Rectangle cropRect;
    private final boolean selectionCrop;
    private final boolean allowGrowing;

    public Crop(Composition comp, Rectangle cropRect, boolean selectionCrop, boolean allowGrowing) {
        this.comp = comp;
        this.cropRect = cropRect;
        this.selectionCrop = selectionCrop;
        this.allowGrowing = allowGrowing;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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

        ImageComponent ic = comp.getIC();

        ic.setPreferredSize(new Dimension(cropRect.width, cropRect.height));
        ic.revalidate();
        ic.makeSureItIsVisible();

        ic.updateDrawStart();
        comp.imageChanged(FULL);

        AppLogic.activeCompositionDimensionsChanged(comp);
    }
}
