/*
 * Copyright 2017 Laszlo Balazs-Csiki
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
import pixelitor.selection.IgnoreSelection;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A kind of compound edit used when an image
 * and its mask are changed together.
 * It extends ImageEdit so that it can be used
 * as a replacement.
 */
public class ImageAndMaskEdit extends ImageEdit {
    private final boolean canRepeat;
    private final ImageEdit maskImageEdit;

    public ImageAndMaskEdit(Composition comp, String name, ImageLayer layer,
                            BufferedImage backupImage,
                            BufferedImage maskBackupImage,
                            boolean canRepeat) {
        super(comp, name, layer, backupImage, IgnoreSelection.YES, canRepeat);
        this.canRepeat = canRepeat;

        assert layer.hasMask();

        maskImageEdit = new ImageEdit(comp, name,
                layer.getMask(), maskBackupImage, IgnoreSelection.YES, canRepeat);

        fadeable = false;
        embedded = true;
        maskImageEdit.setEmbedded(true);
    }

    @Override
    public boolean canRepeat() {
        return canRepeat;
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
            comp.imageChanged(FULL);
            dr.updateIconImage();
            ((ImageLayer) dr).getMask().updateIconImage();
            History.notifyMenus(this);
        }
    }
}
