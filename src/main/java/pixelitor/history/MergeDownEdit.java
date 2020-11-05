/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.gui.GUIText;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.MaskViewMode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * Represents a "merge down" operation
 */
public class MergeDownEdit extends PixelitorEdit {
    private final ImageEdit imageEdit;
    private final DeleteLayerEdit deleteLayerEdit;
    private final MaskViewMode maskViewMode;
    private final Layer layer;
    private final ImageLayer imageLayer;

    public MergeDownEdit(Composition comp,
                         Layer layer,
                         ImageLayer imageLayer,
                         BufferedImage backupImage,
                         MaskViewMode maskViewMode,
                         int activeIndex) {
        super(GUIText.MERGE_DOWN, comp);

        this.layer = layer;
        this.maskViewMode = maskViewMode;
        this.imageLayer = imageLayer;

        imageEdit = new ImageEdit("", comp, imageLayer, backupImage, true, false);
        imageEdit.setEmbedded(true);
        deleteLayerEdit = new DeleteLayerEdit(comp, layer, activeIndex);
        deleteLayerEdit.setEmbedded(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        imageEdit.undo();
        deleteLayerEdit.undo();

        imageLayer.updateIconImage();

        // restore the original mask view mode of the merged layer
        if (layer.hasMask()) {
            maskViewMode.activate(layer);
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        imageEdit.redo();
        deleteLayerEdit.redo();

        imageLayer.updateIconImage();
    }

    @Override
    public void die() {
        super.die();

        imageEdit.die();
        deleteLayerEdit.die();
    }
}
