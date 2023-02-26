/*
 * Copyright 2023 Laszlo Balazs-Csiki and Contributors
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

import pixelitor.layers.ImageLayer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * A PixelitorEdit that represents the application of a layer mask.
 * (The layer mask is deleted, but its effect is transferred
 * to the transparency of the layer)
 */
public class ApplyLayerMaskEdit extends PixelitorEdit {
    private LayerMask mask;
    private BufferedImage previousLayerImage;
    private ImageLayer layer;
    private final MaskViewMode previousMaskViewMode;

    public ApplyLayerMaskEdit(ImageLayer layer, LayerMask mask,
                              BufferedImage previousLayerImage,
                              MaskViewMode previousMaskViewMode) {
        super("Apply Layer Mask", layer.getComp());

        this.previousMaskViewMode = previousMaskViewMode;
        this.previousLayerImage = previousLayerImage;
        this.layer = layer;
        this.mask = mask;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setImage(previousLayerImage);
        layer.addConfiguredMask(mask);
        if (layer.isActive()) {
            previousMaskViewMode.activate(comp, layer);
        }
        layer.updateIconImage();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // the mask view mode is automatically set to normal
        previousLayerImage = layer.applyLayerMask(false);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
        mask = null;
        if (previousLayerImage != null) {
            previousLayerImage.flush();
            previousLayerImage = null;
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.add(mask.createDebugNode("mask"));
        node.add(DebugNodes.createBufferedImageNode("previous layer image", previousLayerImage));
        node.addAsString("previous mask view mode", previousMaskViewMode);

        return node;
    }
}