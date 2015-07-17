package pixelitor.filters.comp;

import pixelitor.AppLogic;
import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.ImageComponent;
import pixelitor.history.AddToHistory;
import pixelitor.history.CanvasChangeEdit;
import pixelitor.history.DeselectEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

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

        Shape backupShape = null;
        if (comp.hasSelection()) {
            backupShape = comp.getSelectionOrNull().getShape();
        }

        if (selectionCrop) {
            assert comp.hasSelection();
            comp.deselect(AddToHistory.NO);
        } else {
            // if this is a crop started from the crop tool
            // we still could have a selection that needs to be
            // cropped
            comp.cropSelection(cropRect);
        }

        // needs to be created before the translations or
        // canvas changes take place
        CanvasChangeEdit canvasChangeEdit = new CanvasChangeEdit("", comp);

        int nrLayers = comp.getNrLayers();
        BufferedImage backupImage = null;
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                backupImage = ((ImageLayer) layer).getImage();
            }
            layer.crop(cropRect);
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, "Crop", backupImage, canvasChangeEdit);
        if (comp.hasSelection()) {
            // Selection crops always deselect, so this must be a
            // crop tool crop with cropped selection
            assert !selectionCrop;
            assert backupShape != null;

            SelectionChangeEdit selectionChangeEdit = new SelectionChangeEdit(comp, backupShape, "");
            edit.setSelectionChangeEdit(selectionChangeEdit);
        } else {
            // no selection after the crop
            if (backupShape == null) {
                // there was no selection then we started
                assert !selectionCrop;
            } else {
                // There was a selection but it disappeared:
                // either a selection crop or a crop tool crop without
                // overlap with the existing selection.
                DeselectEdit deselectEdit = new DeselectEdit(comp, backupShape, "");
                edit.setDeselectEdit(deselectEdit);
            }
        }
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
