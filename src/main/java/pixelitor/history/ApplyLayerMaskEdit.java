/*
 * Copyright 2015 Laszlo Balazs-Csiki
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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * A PixelitorEdit that represents the application of a layer mask
 * (The layer mask is deleted, but its effect is transferred
 * to the transparency of the layer)
 */
public class ApplyLayerMaskEdit extends PixelitorEdit {
    private final LayerMask oldMask;
    private final BufferedImage oldImage;
    private ImageLayer layer;

    public ApplyLayerMaskEdit(Composition comp, ImageLayer layer, LayerMask oldMask, BufferedImage oldImage) {
        super(comp, "Apply Layer Mask");
        comp.setDirty(true);
        this.oldImage = oldImage;
        this.layer = layer;
        this.oldMask = oldMask;
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        layer.setImage(oldImage);
        layer.addMaskBack(oldMask);

        History.notifyMenus(this);
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        layer.applyLayerMask(AddToHistory.NO);

        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();

        layer = null;
    }

    @Override
    public boolean canRepeat() {
        return false;
    }
}