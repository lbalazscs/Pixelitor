package pixelitor.filters.comp;

import pixelitor.AppLogic;
import pixelitor.Composition;
import pixelitor.history.AddToHistory;
import pixelitor.history.CanvasChangeEdit;
import pixelitor.history.History;
import pixelitor.history.MultiLayerEdit;
import pixelitor.history.SelectionChangeEdit;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.Selection;

import javax.swing.*;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.INVALIDATE_CACHE;

public class Resize extends AbstractAction {
    private Composition comp;
    private int targetWidth;
    private int targetHeight;

    // if true, resizes an image so that the proportions
    // are kept and the result fits into the given dimensions
    private final boolean resizeInBox;

    public Resize(Composition comp, int targetWidth, int targetHeight, boolean resizeInBox) {
        this.comp = comp;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.resizeInBox = resizeInBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int actualWidth = comp.getCanvasWidth();
        int actualHeight = comp.getCanvasHeight();

        if ((actualWidth == targetWidth) && (actualHeight == targetHeight)) {
            return;
        }

        if (resizeInBox) {
            int maxWidth = targetWidth;
            int maxHeight = targetHeight;

            double heightScale = maxHeight / (double) actualHeight;
            double widthScale = maxWidth / (double) actualWidth;
            double scale = Math.min(heightScale, widthScale);

            targetWidth = (int) (scale * (double) actualWidth);
            targetHeight = (int) (scale * (double) actualHeight);
        }

        boolean progressiveBilinear = false;
        if ((targetWidth < (actualWidth / 2)) || (targetHeight < (actualHeight / 2))) {
            progressiveBilinear = true;
        }

        Shape backupShape = null;
        if (comp.hasSelection()) {
            Selection selection = comp.getSelectionOrNull();
            backupShape = selection.getShape();

            double sx = ((double) targetWidth) / actualWidth;
            double sy = ((double) targetHeight) / actualHeight;
            AffineTransform tx = AffineTransform.getScaleInstance(sx, sy);
            selection.transform(tx, AddToHistory.NO);
        }
        BufferedImage backupImage = null;

        // needs to be created before the translations or
        // canvas changes take place
        CanvasChangeEdit canvasChangeEdit = new CanvasChangeEdit("", comp);

        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer layer = comp.getLayer(i);
            if (layer instanceof ImageLayer) {
                backupImage = ((ImageLayer) layer).getImage();
            }
            layer.resize(targetWidth, targetHeight, progressiveBilinear);
            if (layer.hasMask()) {
                layer.getMask().resize(targetWidth, targetHeight, progressiveBilinear);
            }
        }

        MultiLayerEdit edit = new MultiLayerEdit(comp, "Resize", backupImage, canvasChangeEdit);
        if (backupShape != null) {
            SelectionChangeEdit selectionChangeEdit = new SelectionChangeEdit(comp, backupShape, "");
            edit.setSelectionChangeEdit(selectionChangeEdit);
        }
        History.addEdit(edit);

        comp.getCanvas().updateSize(targetWidth, targetHeight);

        // Only after the shared canvas size was updated
        // The icon image should change if the proportions were
        // changed or if it was resized to a very small size
        comp.updateAllIconImages();

        comp.imageChanged(INVALIDATE_CACHE);

        AppLogic.activeCompositionDimensionsChanged(comp);
    }
}
