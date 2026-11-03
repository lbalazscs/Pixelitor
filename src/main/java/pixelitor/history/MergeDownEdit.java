/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.gui.GUIText;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerHolder;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * Represents a "merge down" operation.
 */
public class MergeDownEdit extends PixelitorEdit {
    private final ImageEdit imageEdit;
    private final DeleteLayerEdit deleteLayerEdit;
    private final Layer layer;
    private final ImageLayer belowLayer;

    public MergeDownEdit(LayerHolder holder,
                         Layer layer,
                         ImageLayer belowLayer,
                         BufferedImage backupImage,
                         Layer activeLayerBefore,
                         MaskViewMode maskViewModeBefore,
                         int layerIndex) {
        super(GUIText.MERGE_DOWN, holder.getComp(), true);

        this.layer = layer;
        this.belowLayer = belowLayer;

        imageEdit = new ImageEdit("", comp, belowLayer, backupImage, true);
        imageEdit.setEmbedded(true);
        // the embedded edit restores the active layer and its mask view mode
        deleteLayerEdit = new DeleteLayerEdit(holder, layer, layerIndex,
            activeLayerBefore, maskViewModeBefore);
        deleteLayerEdit.setEmbedded(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        imageEdit.undo();
        deleteLayerEdit.undo();

        refreshBelowLayer();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        imageEdit.redo();
        deleteLayerEdit.redo();

        refreshBelowLayer();
    }

    private void refreshBelowLayer() {
        belowLayer.invalidateMaskedImageCache();
        belowLayer.updateIconImage();
    }

    @Override
    public void die() {
        super.die();

        imageEdit.die();
        deleteLayerEdit.die();
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.add(belowLayer.createDebugNode());
        node.add(imageEdit.createDebugNode("image edit"));
        node.add(deleteLayerEdit.createDebugNode("delete layer edit"));

        return node;
    }
}
