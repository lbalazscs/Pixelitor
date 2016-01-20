package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.selection.IgnoreSelection;
import pixelitor.selection.Selection;

import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * Encapsulates the state needed by a MultiLayerEdit
 */
public class MultiLayerBackup {
    private final Composition comp;
    private final String editName;
    private ImageLayer layer;
    private CanvasChangeEdit canvasChangeEdit;
    private TranslationEdit translationEdit;
    private Shape backupShape;

    // Saved before the change, but the edit is
    // created after the change.
    // This way no image copy is necessary.
    private BufferedImage backupImage;
    private BufferedImage backupMaskImage;

    /**
     * This object needs to be created before the translations,
     * canvas changes or selection changes take place
     */
    public MultiLayerBackup(Composition comp, String editName, boolean changesCanvasDimensions) {
        this.comp = comp;
        this.editName = editName;

        // save canvas dimensions
        if (changesCanvasDimensions) {
            canvasChangeEdit = new CanvasChangeEdit(comp, editName);
        }

        // save translation
        ContentLayer contentLayer = comp.getAnyContentLayer();
        if (contentLayer != null) { // could be null, if there are only text layers
            translationEdit = new TranslationEdit(comp, contentLayer, true);
        }

        // save selection
        if (comp.hasSelection()) {
            Selection selection = comp.getSelectionOrNull();
            backupShape = selection.getShape();
            assert backupShape != null;
        }

        // save backup images
        int nrLayers = comp.getNrLayers();
        for (int i = 0; i < nrLayers; i++) {
            Layer compLayer = comp.getLayer(i);
            if (compLayer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) compLayer;
                this.layer = imageLayer;
                backupImage = imageLayer.getImage();
                if (imageLayer.hasMask()) {
                    backupMaskImage = imageLayer.getMask().getImage();
                }
                break;
            }
        }
    }

    public CanvasChangeEdit getCanvasChangeEdit() {
        return canvasChangeEdit;
    }

    public TranslationEdit getTranslationEdit() {
        return translationEdit;
    }

    public boolean hasSavedSelection() {
        return backupShape != null;
    }

    public ImageEdit createImageEdit(BufferedImage currentImage) {
        assert backupImage != null;
        if(currentImage == backupImage) {
            // for enlarge canvas with big layer it can happen that
            // the image does not need to be changed at all
            return null;
        }

        ImageEdit edit;
        if (backupMaskImage != null) {
            edit = new ImageAndMaskEdit(comp, editName, layer,
                    backupImage, backupMaskImage, false);
        } else {
            edit = new ImageEdit(comp, editName, layer,
                    backupImage, IgnoreSelection.YES, false);
        }
        edit.setEmbedded(true);
        return edit;
    }

    public SelectionChangeEdit createSelectionChangeEdit() {
        assert backupShape != null;
        SelectionChangeEdit edit = new SelectionChangeEdit(comp, backupShape, editName);
        edit.setEmbedded(true);
        return edit;
    }

    public DeselectEdit createDeselectEdit() {
        assert backupShape != null;
        DeselectEdit edit = new DeselectEdit(comp, backupShape, editName);
        edit.setEmbedded(true);
        return edit;
    }
}
