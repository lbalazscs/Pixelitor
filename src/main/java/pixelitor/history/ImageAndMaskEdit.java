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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

/**
 * A kind of compound edit used when an image
 * and its mask are changed together.
 * It extends ImageEdit so that it can be used
 * as a replacement.
 */
public class ImageAndMaskEdit extends ImageEdit {
    private final ImageEdit maskImageEdit;

    public ImageAndMaskEdit(String name, Composition comp, ImageLayer layer,
                            BufferedImage backupImage,
                            BufferedImage maskBackupImage) {
        super(name, comp, layer, backupImage, true);

        assert layer.hasMask();

        maskImageEdit = new ImageEdit(name, comp,
            layer.getMask(), maskBackupImage, true);

        fadeable = false;
        embedded = true;
        maskImageEdit.setEmbedded(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();
        maskImageEdit.undo();
        updateGUI();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();
        maskImageEdit.redo();
        updateGUI();
    }

    @Override
    public void die() {
        super.die();
        maskImageEdit.die();
    }

    private void updateGUI() {
        // the two edits are set to embedded, so we update - except
        // if this edit is also embedded
        if (!embedded) {
            comp.imageChanged();
            dr.updateIconImage();
            ((ImageLayer) dr).getMask().updateIconImage();
        }
    }
}
