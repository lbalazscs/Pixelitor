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

import pixelitor.layers.ContentLayer;
import pixelitor.layers.ImageLayer;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.image.BufferedImage;

import static pixelitor.Composition.ImageChangeActions.FULL;

/**
 * A PixelitorEdit hat represents the movement of a content layer.
 */
public class ContentLayerMoveEdit extends PixelitorEdit {
    public static final String NAME = "Layer Movement";
    private BufferedImage backupImage;
    private ContentLayer layer;
    private int backupTranslationX = 0;
    private int backupTranslationY = 0;

    /**
     * @param layer
     * @param backupImage     - can be null, if no image enlargement is taking place
     * @param oldTranslationX
     * @param oldTranslationY
     */
    public ContentLayerMoveEdit(ContentLayer layer, BufferedImage backupImage, int oldTranslationX, int oldTranslationY) {
        super(layer.getComp(), NAME);

        this.layer = layer;
        this.backupImage = backupImage;
        this.backupTranslationX = oldTranslationX;
        this.backupTranslationY = oldTranslationY;

        layer.getComp().setDirty(true);
    }

    @Override
    public void undo() throws CannotUndoException {
        super.undo();

        swapTranslation();
    }

    @Override
    public void redo() throws CannotRedoException {
        super.redo();

        swapTranslation();
    }

    private void swapTranslation() {
        int tmpX = layer.getTranslationX();
        int tmpY = layer.getTranslationY();
        BufferedImage tmpBI = null;
        if (backupImage != null) {
            ImageLayer imageLayer = (ImageLayer) layer;
            tmpBI = imageLayer.getImage();
            imageLayer.setImage(backupImage);
        }

        layer.setTranslation(backupTranslationX, backupTranslationY);
        backupTranslationX = tmpX;
        backupTranslationY = tmpY;

        backupImage = tmpBI;

        layer.getComp().imageChanged(FULL);
        History.notifyMenus(this);
    }

    @Override
    public void die() {
        super.die();
        if (backupImage != null) {
            backupImage.flush();
            backupImage = null;
        }
        layer = null;
    }

    @Override
    public boolean canRepeat() {
        return false;
    }
}
