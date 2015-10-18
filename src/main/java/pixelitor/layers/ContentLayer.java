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
import pixelitor.filters.comp.Rotate;
import pixelitor.history.CompoundEdit;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.PixelitorEdit;

import java.awt.geom.AffineTransform;

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

    @Override
    public void startMovement() {
        tmpTranslationX = 0;
        tmpTranslationY = 0;
        super.startMovement();
    }

    @Override
    public void moveWhileDragging(double x, double y) {
        tmpTranslationX = (int) x;
        tmpTranslationY = (int) y;
        super.moveWhileDragging(x, y);
    }

    @Override
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

    public abstract void flip(Flip.Direction direction, AffineTransform flipTx);

    public abstract void rotate(Rotate.SpecialAngle angle);

    public abstract void enlargeCanvas(int north, int east, int south, int west);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("tx=").append(translationX);
        sb.append(", ty=").append(translationY);
        sb.append(", super=").append(super.toString());
        sb.append('}');
        return sb.toString();
    }
}
