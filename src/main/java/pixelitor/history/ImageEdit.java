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

import pixelitor.Composition;
import pixelitor.layers.Drawable;
import pixelitor.utils.debug.DebugNode;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

/**
 * A PixelitorEdit that represents the changes made to an image.
 */
public class ImageEdit extends FadeableEdit {
    // selections are ignored for example when the image is enlarged by the move tool
    private final boolean ignoreSelection;

    private SoftReference<BufferedImage> imgRef;
    protected Drawable dr;

    public ImageEdit(String name, Composition comp, Drawable dr,
                     BufferedImage backupImage,
                     boolean ignoreSelection) {
        super(name, comp, dr);
        this.ignoreSelection = ignoreSelection;

        assert dr != null;
        assert backupImage != null;

//        Utils.debugImage(backupImage, "Backup for " + name);

        // the backup image is stored in an SoftReference
        imgRef = new SoftReference<>(backupImage);
        this.dr = dr;

        checkBackupDifferentFromActive();
    }

    // the backup should never be identical to the active image
    // otherwise the backup might be also edited
    private void checkBackupDifferentFromActive() {
        BufferedImage layerImage = dr.getImage();
        if (layerImage == imgRef.get()) {
            throw new IllegalStateException("backup image is identical to the active one");
        }
    }

    public static ImageEdit createEmbedded(Drawable dr) {
        // If there is a selection, only the bounds of the selected area is saved.
        BufferedImage backup = dr.getSelectedSubImage(true);

        ImageEdit edit = new ImageEdit("", dr.getComp(),
            dr, backup, false);

        edit.setEmbedded(true);

        return edit;
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
    private boolean swapImages() {
        BufferedImage backupImage = imgRef.get();
        if (backupImage == null) {
            return false;
        }

        BufferedImage tmp;
        if (ignoreSelection) {
            tmp = dr.getImage();
        } else {
            tmp = dr.getSelectedSubImage(false);
        }
        dr.changeImageForUndoRedo(backupImage, ignoreSelection);

        // create new backup image from tmp
        imgRef = new SoftReference<>(tmp);

        if (!embedded) {
            comp.update();
            dr.updateIconImage();
        }

        checkBackupDifferentFromActive();
        return true;
    }

    @Override
    public void die() {
        super.die();

        BufferedImage backupImage = imgRef.get();
        if (backupImage != null) {
            backupImage.flush();
        }
    }

    @Override
    public BufferedImage getBackupImage() {
        if (imgRef != null) {
            // this still could be null
            return imgRef.get();
        }
        return null;
    }

    @Override
    public DebugNode createDebugNode(String key) {
        var node = super.createDebugNode(key);

        BufferedImage img = imgRef.get();
        if (img != null) {
            node.addInt("backup image width", img.getWidth());
            node.addInt("backup image height", img.getHeight());
        }

        node.addBoolean("ignoreSelection", ignoreSelection);

        return node;
    }
}
