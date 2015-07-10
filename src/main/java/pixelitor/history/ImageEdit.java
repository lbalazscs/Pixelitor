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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A PixelitorEdit that represents the changes made to an image.
 */
public class ImageEdit extends FadeableEdit {
    private SoftReference<BufferedImage> imgRef;
    private ImageLayer layer;

    private final boolean canRepeat;

    public ImageEdit(String name, Composition comp, ImageLayer layer, BufferedImage backupImage, boolean canRepeat) {
        super(comp, name);

        assert layer != null;
        assert backupImage != null;

        // the backup image is stored in an SoftReference
        this.imgRef = new SoftReference<>(backupImage);
        this.layer = layer;
        this.canRepeat = canRepeat;

        comp.setDirty(true);
        sanityCheck();
    }

    private void sanityCheck() {
        // post condition: the backup should never be identical to the active image
        // otherwise the backup might be also edited
        BufferedImage layerImage = layer.getImage();
        if (layerImage == imgRef.get()) {
            throw new IllegalStateException("backup BufferedImage is identical to the active one");
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        if (!swapImages()) {
            throw new CannotUndoException();
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        if (!swapImages()) {
            throw new CannotRedoException();
        }
    }

    /**
     * Returns true if successful
     */
    private boolean swapImages()  {
        BufferedImage backupImage = imgRef.get();
        if(backupImage == null) {
            return false;
        }

        BufferedImage tmp = layer.getImageOrSubImageIfSelected(false, true);
        layer.changeImageUndoRedo(backupImage);
        comp.imageChanged(FULL);
        layer.updateIconImage();

        // create new backup image from tmp
        imgRef = new SoftReference<>(tmp);
        History.notifyMenus(this);

        sanityCheck();
        return true;
    }

    @Override
    public void die() {
        super.die();

        BufferedImage backupImage = imgRef.get();
        if(backupImage != null) {
            backupImage.flush();
        }

        imgRef = null;
        layer = null;
    }

    @Override
    public BufferedImage getBackupImage() {
        if(imgRef != null) {
            BufferedImage backupImage = imgRef.get();
            // this still could be null
            return backupImage;
        }
        return null;
    }

    @Override
    public boolean canRepeat() {
        return canRepeat;
    }
}
