/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.history;

import pixelitor.Composition;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;

import java.awt.Shape;
import java.awt.image.BufferedImage;

/**
 * Encapsulates the state needed by a MultiLayerEdit
 */
public class MultiLayerBackup {
    private final Composition comp;
    private final String editName;
    private Layer layer;
    private CanvasChangeEdit canvasChangeEdit;
    private TranslationEdit translationEdit;
    private final Shape backupShape;

    // Saved before the change, but the edit is
    // created after the change.
    // This way no image copy is necessary.
    private BufferedImage backupImage;
    private BufferedImage backupMaskImage;

    /**
     * This object needs to be created before the translations,
     * canvas changes or selection changes take place
     */
    public MultiLayerBackup(Composition comp, String editName, boolean canvasSizeChange) {
        this.comp = comp;
        this.editName = editName;

        // save canvas dimensions
        if (canvasSizeChange) {
            canvasChangeEdit = new CanvasChangeEdit(editName, comp);
        }

        // save translation
        ContentLayer contentLayer = comp.getAnyContentLayer();
        if (contentLayer != null) { // could be null, if there are only adj layers - TODO allowed?
            translationEdit = new TranslationEdit(comp, contentLayer, true);
        }

        // save selection
        backupShape = comp.getSelectionShape();

        // save backup images
        boolean imageLayerFound = false;
        int numLayers = comp.getNumLayers();
        for (int i = 0; i < numLayers; i++) {
            Layer compLayer = comp.getLayer(i);
            if (compLayer instanceof ImageLayer) {
                imageLayerFound = true;
                ImageLayer imageLayer = (ImageLayer) compLayer;
                this.layer = imageLayer;
                backupImage = imageLayer.getImage();
                if (imageLayer.hasMask()) {
                    backupMaskImage = imageLayer.getMask().getImage();
                }
                break;
            }
        }
        if (!imageLayerFound) {
            if (contentLayer != null) {
                layer = contentLayer;
                if (contentLayer.hasMask()) {
                    backupMaskImage = contentLayer.getMask().getImage();
                }
            } else {
                // we must have a single adj layer
                // TODO why is this allowed??
                layer = comp.getLayer(0);
                backupMaskImage = layer.getMask().getImage();
            }
        }
        assert layer != null;
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

    public ImageEdit createImageEdit(BufferedImage currentImage, BufferedImage currentMaskImage) {
        assert backupImage != null;
        assert layer instanceof ImageLayer;

        ImageLayer imageLayer = (ImageLayer) layer;

        if (currentImage == backupImage && backupMaskImage == currentMaskImage) {

            // for enlarge canvas with big layer it can happen that
            // the image does not need to be changed at all
            return null;
        }

        ImageEdit edit;
        if (backupMaskImage != null) {
            edit = new ImageAndMaskEdit(editName, comp, imageLayer,
                    backupImage, backupMaskImage, false);
        } else {
            edit = new ImageEdit(editName, comp, imageLayer,
                    backupImage, true, false);
        }
        edit.setEmbedded(true);
        return edit;
    }

    public ImageEdit createImageEditForMaskOnly(BufferedImage currentMaskImage) {
        if (backupMaskImage == currentMaskImage) {
            return null;
        }
        ImageEdit edit = new ImageEdit(editName, comp, layer.getMask(),
                backupMaskImage, true, false);
        edit.setEmbedded(true);
        return edit;
    }

    public SelectionChangeEdit createSelectionChangeEdit() {
        assert backupShape != null;
        SelectionChangeEdit edit = new SelectionChangeEdit(editName, comp, backupShape);
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
