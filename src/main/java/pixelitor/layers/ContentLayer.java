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

package pixelitor.layers;

import pixelitor.Composition;
import pixelitor.filters.comp.Flip;
import pixelitor.history.CompoundEdit;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.PixelitorEdit;

/**
 * A layer with a content (text or image layer) that
 * can be moved/rotated.
 */
public abstract class ContentLayer extends Layer {
    private static final long serialVersionUID = 2L;

    // used only while dragging
    private transient int tmpTranslationX = 0;
    private transient int tmpTranslationY = 0;

    int translationX = 0;
    int translationY = 0;

    protected ContentLayer(Composition comp, String name, Layer parent) {
        super(comp, name, parent);
    }

    public int getTranslationX() {
        return translationX + tmpTranslationX;
    }

    public int getTranslationY() {
        return translationY + tmpTranslationY;
    }

    public void startMovement() {
        tmpTranslationX = 0;
        tmpTranslationY = 0;
        super.startMovement();
    }

    public void moveWhileDragging(int x, int y) {
        tmpTranslationX = x;
        tmpTranslationY = y;
        super.moveWhileDragging(x, y);
    }

    public PixelitorEdit endMovement() {

        int oldTranslationX = translationX;
        int oldTranslationY = translationY;

        // while dragging only the temporary values were updated
        // and now they can be committed to the final value
        translationX += tmpTranslationX;
        translationY += tmpTranslationY;
        tmpTranslationX = 0;
        tmpTranslationY = 0;

        PixelitorEdit linkedEdit = super.endMovement();

        ContentLayerMoveEdit ownEdit = createMovementEdit(oldTranslationX, oldTranslationY);
        if (linkedEdit == null) {
            return ownEdit;
        } else {
            return new CompoundEdit(comp, ContentLayerMoveEdit.NAME, ownEdit, linkedEdit);
        }
    }

    abstract ContentLayerMoveEdit createMovementEdit(int oldTranslationX, int oldTranslationY);

    /**
     * Programmatically set the translation.
     * There is no check for layer enlargement.
     * Also the linked layer is NOT translated.
     */
    public void setTranslation(int x, int y) {
        this.translationX = x;
        this.translationY = y;
    }

    public abstract void flip(Flip.Direction direction);

    public abstract void rotate(int angleDegree);

    public abstract void enlargeCanvas(int north, int east, int south, int west);
}
