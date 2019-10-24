/*
 * Copyright 2019 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

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
     * For image layers, this is always negative or zero,
     * because the layer image is automatically enlarged
     * if the layer is moved inward.
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

    /**
     * Returns the layer bounding box relative to the canvas
     * It must return rect trimmed from transparent pixels
     */
    public abstract Rectangle getEffectiveBoundingBox();

    /**
     * Returns the bounding box relative to the canvas used for snapping
     * It must return rect that can help user position the layer
     * It don't have to be the same as effective bounding box
     */
    public abstract Rectangle getSnappingBoundingBox();

    /**
     * Returns pixel (sRGB) at point or zero if point is out of layer
     * Zero means black transparent pixel (omitted from hit detection)
     */
    public abstract int getMouseHitPixelAtPoint(Point p);

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

        // possibly null if there is no linked mask
        PixelitorEdit linkedEdit = super.endMovement();

        ContentLayerMoveEdit ownEdit = createMovementEdit(oldTX, oldTY);
        assert ownEdit != null;

        return MultiEdit.combine(ownEdit, linkedEdit, ContentLayerMoveEdit.NAME);
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

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCroppedPixels, boolean allowGrowing) {
        if (!deleteCroppedPixels) {
            // relative to the canvas
            int cropX = (int) cropRect.getX();
            int cropY = (int) cropRect.getY();

            setTranslation(
                    translationX - cropX,
                    translationY - cropY);
        }
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
