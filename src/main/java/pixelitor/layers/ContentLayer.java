/*
 * Copyright 2022 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.Flip;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.utils.QuadrantAngle;
import pixelitor.utils.debug.DebugNode;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serial;

/**
 * A layer with a content (text or image layer) that
 * can be moved/rotated.
 */
public abstract class ContentLayer extends Layer {
    @Serial
    private static final long serialVersionUID = 2L;

    // used only while dragging
    private transient int tmpTx = 0;
    private transient int tmpTy = 0;

    /**
     * The translation relative to the default position.
     * For image layers, this is always negative or zero,
     * because the layer image is automatically enlarged
     * if the layer is moved inward.
     */
    private int translationX = 0;
    private int translationY = 0;

    protected ContentLayer(Composition comp, String name) {
        super(comp, name);
    }

    /**
     * Returns the X translation of the content
     * relative to its default position
     */
    public int getTx() {
        return translationX + tmpTx;
    }

    /**
     * Returns the Y translation of the content
     * relative to its default position
     */
    public int getTy() {
        return translationY + tmpTy;
    }

    /**
     * Returns the rectangle representing the content, relative to the canvas.
     */
    public abstract Rectangle getContentBounds();

    /**
     * Returns the pixel value at the given point or zero if
     * the point is outside the contents or it is transparent.
     */
    public abstract int getPixelAtPoint(Point p);

    @Override
    public void startMovement() {
        tmpTx = 0;
        tmpTy = 0;
        super.startMovement();
    }

    @Override
    public void moveWhileDragging(double relImX, double relImY) {
        tmpTx = (int) relImX;
        tmpTy = (int) relImY;
        super.moveWhileDragging(relImX, relImY);
    }

    @Override
    public PixelitorEdit endMovement() {
        int oldTx = translationX;
        int oldTy = translationY;

        // while dragging only the temporary values were updated,
        // and now they can be committed to the final value
        translationX += tmpTx;
        translationY += tmpTy;
        tmpTx = 0;
        tmpTy = 0;

        // possibly null if there is no linked mask
        PixelitorEdit linkedEdit = createLinkedMovementEdit();

        // can be null for empty shape layers
        PixelitorEdit ownEdit = createMovementEdit(oldTx, oldTy);

        return MultiEdit.combine(ownEdit, linkedEdit, ContentLayerMoveEdit.NAME);
    }

    abstract PixelitorEdit createMovementEdit(int oldTx, int oldTy);

    /**
     * Programmatically set the translation.
     * There is no check for layer enlargement.
     * Also the linked layer is NOT translated.
     */
    public void setTranslation(int x, int y) {
        translationX = x;
        translationY = y;
    }

    @Override
    public void crop(Rectangle2D cropRect, boolean deleteCropped, boolean allowGrowing) {
        if (!deleteCropped) {
            // relative to the canvas
            int cropX = (int) cropRect.getX();
            int cropY = (int) cropRect.getY();

            setTranslation(
                translationX - cropX,
                translationY - cropY);
        }
    }

    public abstract void flip(Flip.Direction direction);

    public abstract void rotate(QuadrantAngle angle);

    public abstract void enlargeCanvas(int north, int east, int south, int west);

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("translation X", getTx());
        node.addInt("translation Y", getTy());

        return node;
    }

    @Override
    public String toString() {
        return "{tx=" + translationX
               + ", ty=" + translationY
               + ", super=" + super.toString() + '}';
    }
}
