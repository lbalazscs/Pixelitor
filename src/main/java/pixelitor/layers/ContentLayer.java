/*
 * Copyright 2016 Laszlo Balazs-Csiki
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
import pixelitor.filters.comp.Rotate;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.LinkedEdit;
import pixelitor.history.PixelitorEdit;

/**
 * A layer with a content (text or image layer) that
 * can be moved/rotated.
 */
public abstract class ContentLayer extends Layer {
    private static final long serialVersionUID = 2L;

    // used only while dragging
    private transient int tmpTX = 0;
    private transient int tmpTY = 0;

    /**
     * The translation relative to the default position.
     * For image layers, this is always negative or zero.
     */
    int translationX = 0;
    int translationY = 0;

    protected ContentLayer(Composition comp, String name, Layer parent) {
        super(comp, name, parent);
    }

    /**
     * Returns the X translation of the content
     * relative to its default position
     */
    public int getTX() {
        return translationX + tmpTX;
    }

    /**
     * Returns the Y translation of the content
     * relative to its default position
     */
    public int getTY() {
        return translationY + tmpTY;
    }

    @Override
    public void startMovement() {
        tmpTX = 0;
        tmpTY = 0;
        super.startMovement();
    }

    @Override
    public void moveWhileDragging(double x, double y) {
        tmpTX = (int) x;
        tmpTY = (int) y;
        super.moveWhileDragging(x, y);
    }

    @Override
    public PixelitorEdit endMovement() {
        int oldTX = translationX;
        int oldTY = translationY;

        // while dragging only the temporary values were updated
        // and now they can be committed to the final value
        translationX += tmpTX;
        translationY += tmpTY;
        tmpTX = 0;
        tmpTY = 0;

        PixelitorEdit linkedEdit = super.endMovement();

        ContentLayerMoveEdit ownEdit = createMovementEdit(oldTX, oldTY);
        if (linkedEdit == null) {
            return ownEdit;
        } else {
            return new LinkedEdit(comp, ContentLayerMoveEdit.NAME, ownEdit, linkedEdit);
        }
    }

    abstract ContentLayerMoveEdit createMovementEdit(int oldTX, int oldTY);

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

    public abstract void rotate(Rotate.SpecialAngle angle);

    public abstract void enlargeCanvas(int north, int east, int south, int west);

    @Override
    public String toString() {
        return "{tx=" + translationX
                + ", ty=" + translationY
                + ", super=" + super.toString() + '}';
    }
}
