/*
 * Copyright 2021 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.layers.ImageLayer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.MaskViewMode;
import pixelitor.utils.debug.DebugNode;
import pixelitor.utils.debug.DebugNodes;
import pixelitor.utils.debug.LayerMaskNode;
import pixelitor.utils.debug.LayerNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * A PixelitorEdit that represents the application of a layer mask.
 * (The layer mask is deleted, but its effect is transferred
 * to the transparency of the layer)
 */
public class ApplyLayerMaskEdit extends PixelitorEdit {
    private LayerMask oldMask;
    private BufferedImage oldImage;
    private ImageLayer layer;
    private final MaskViewMode oldMode;

    public ApplyLayerMaskEdit(Composition comp, ImageLayer layer,
                              LayerMask oldMask, BufferedImage oldImage,
                              MaskViewMode oldMode) {
        super("Apply Layer Mask", comp);

        this.oldMode = oldMode;
        this.oldImage = oldImage;
        this.layer = layer;
        this.oldMask = oldMask;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setImage(oldImage);
        layer.addConfiguredMask(oldMask);
        if (layer.isActive()) {
            oldMode.activate(comp, layer);
        }
        layer.updateIconImage();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        // the mask view mode is automatically set to normal
        oldImage = layer.applyLayerMask(false);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
        oldMask = null;
        if (oldImage != null) {
            oldImage.flush();
            oldImage = null;
        }
    }

    @Override
    public DebugNode createDebugNode() {
        DebugNode node = super.createDebugNode();

        node.add(new LayerNode(layer));
        node.add(new LayerMaskNode("old mask", oldMask));
        node.add(DebugNodes.createBufferedImageNode("old image", oldImage));
        node.addString("old mode", oldMode.toString());

        return node;
    }
}