/*
 * Copyright 2026 Laszlo Balazs-Csiki and Contributors
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
import pixelitor.compactions.FlipDirection;
import pixelitor.compactions.Outsets;
import pixelitor.compactions.QuadrantAngle;
import pixelitor.history.ContentLayerMoveEdit;
import pixelitor.history.MultiEdit;
import pixelitor.history.PixelitorEdit;
import pixelitor.utils.debug.DebugNode;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.List;
import java.util.ListIterator;

/**
 * Base class for layers with content that can be manipulated
 * spatially, supporting transformations like move and rotate.
 */
public abstract class ContentLayer extends Layer {
    @Serial
    private static final long serialVersionUID = 2L;

    // The translation relative to the default position. For image
    // layers, this is always negative or zero, because the layer
    // image is automatically enlarged if the layer is moved inward.
    private int translationX = 0;
    private int translationY = 0;

    // the relative movement of the layer content while
    // the user is actively dragging it with the Move Tool
    private transient int dragOffsetX = 0;
    private transient int dragOffsetY = 0;

    protected ContentLayer(Composition comp, String name) {
        super(comp, name);
    }

    /**
     * Finds the first opaque layer at the given image-space point,
     * in the given list, searching from top to bottom.
     */
    public static ContentLayer findOpaqueInList(List<Layer> layers, Point p) {
        ListIterator<Layer> li = layers.listIterator(layers.size());
        while (li.hasPrevious()) {
            Layer layer = li.previous();
            if (layer instanceof ContentLayer contentLayer) {
                ContentLayer found = contentLayer.findOpaqueLayerAtPoint(p);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        // defaults for transient fields
        dragOffsetX = 0;
        dragOffsetY = 0;
    }

    /**
     * Returns the X translation of the content relative to its default position.
     * For image-based layers this is the X translation of the image relative to
     * the canvas and it's always zero or negative because the image must cover the canvas.
     */
    public int getTx() {
        return translationX + dragOffsetX;
    }

    /**
     * Similar to {@link  #getTx()}, but for the Y translation.
     */
    public int getTy() {
        return translationY + dragOffsetY;
    }

    /**
     * Returns the rectangle representing the content, relative to the canvas.
     */
    public Rectangle getContentBounds() {
        return getContentBounds(true);
    }

    public abstract Rectangle getContentBounds(boolean includeTransparent);

    /**
     * Returns the pixel value at the given image-space point, or
     * zero if the point is outside the content or transparent.
     */
    public abstract int getPixelAtPoint(Point p);

    /**
     * Finds the topmost opaque layer at a given image-space point.
     * For simple content layers, this method checks the layer itself.
     * For layer groups, this method can descend into child layers.
     */
    public ContentLayer findOpaqueLayerAtPoint(Point p) {
        // a small opacity makes the layer effectively invisible for hit-testing
        if (!isVisible() || getOpacity() < 0.05f) {
            return null;
        }

        int pixel = getPixelAtPoint(p);

        int pixelAlphaThreshold = 30;
        if (((pixel >> 24) & 0xFF) > pixelAlphaThreshold) {
            return this;
        }

        return null;
    }

    @Override
    public void prepareMovement() {
        dragOffsetX = 0;
        dragOffsetY = 0;
        super.prepareMovement();
    }

    @Override
    public void moveWhileDragging(double imDx, double imDy) {
        dragOffsetX = (int) imDx;
        dragOffsetY = (int) imDy;
        super.moveWhileDragging(imDx, imDy);
    }

    @Override
    public PixelitorEdit finalizeMovement() {
        int prevTx = translationX;
        int prevTy = translationY;

        // while dragging only the drag offsets were updated,
        // and now they can be committed to the final value
        translationX += dragOffsetX;
        translationY += dragOffsetY;
        dragOffsetX = 0;
        dragOffsetY = 0;

        // possibly null if there is no linked mask
        PixelitorEdit linkedEdit = createLinkedMovementEdit();

        // can be null for empty shape or gradient fill layers
        PixelitorEdit ownEdit = createMovementEdit(prevTx, prevTy);

        return MultiEdit.combine(ownEdit, linkedEdit, ContentLayerMoveEdit.NAME);
    }

    /**
     * Creates a history edit for the movement of this content layer.
     */
    abstract PixelitorEdit createMovementEdit(int prevTx, int prevTy);

    /**
     * Programmatically sets the translation, bypassing layer
     * enlargement checks and without affecting linked layers.
     */
    public void setTranslation(int x, int y) {
        translationX = x;
        translationY = y;
    }

    @Override
    public void crop(Rectangle cropRect, boolean deleteCropped, boolean allowGrowing) {
        if (!deleteCropped) {
            // only adjusts the layer translation (subclasses can
            // do more or something completely different)
            setTranslation(
                translationX - cropRect.x,
                translationY - cropRect.y);
        }
    }

    /**
     * Flips the layer content horizontally or vertically.
     */
    public abstract void flip(FlipDirection direction, boolean layerTransform);

    /**
     * Rotates the layer content by a multiple of 90 degrees.
     */
    public abstract void rotate(QuadrantAngle angle, boolean layerTransform);

    /**
     * Adjusts the layer content in response to canvas enlargement.
     */
    public abstract void enlargeCanvas(Outsets out);

    @Override
    public DebugNode createDebugNode(String key) {
        DebugNode node = super.createDebugNode(key);

        node.addInt("translation X", getTx());
        node.addInt("translation Y", getTy());

        return node;
    }
}
