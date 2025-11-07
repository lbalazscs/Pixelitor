/*
 * Copyright 2025 Laszlo Balazs-Csiki and Contributors
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
    private final LayerMask mask;
    private BufferedImage prevLayerImage;
    private final ImageLayer layer;
    private final MaskViewMode prevMaskViewMode;

    public ApplyLayerMaskEdit(ImageLayer layer, LayerMask mask,
                              BufferedImage prevLayerImage,
                              MaskViewMode prevMaskViewMode) {
        super("Apply Layer Mask", layer.getComp());

        this.prevMaskViewMode = prevMaskViewMode;
        this.prevLayerImage = prevLayerImage;
        this.layer = layer;
        this.mask = mask;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setImage(prevLayerImage);
        layer.addConfiguredMask(mask);
        if (layer.isActive()) {
            comp.setMaskViewMode(prevMaskViewMode, layer);
        }
        layer.updateIconImage();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // the mask view mode is automatically set to normal
        prevLayerImage = layer.applyLayerMask(false);
    }

    @Override
    public void die() {
        super.die();

        if (prevLayerImage != null) {
            prevLayerImage.flush();
        }
    }

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.add(layer.createDebugNode());
        node.add(mask.createDebugNode("mask"));
        node.add(DebugNodes.createBufferedImageNode("previous layer image", prevLayerImage));
        node.addAsString("previous mask view mode", prevMaskViewMode);

        return node;
    }
}
